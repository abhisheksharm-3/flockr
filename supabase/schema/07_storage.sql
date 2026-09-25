-- Storage buckets and their access. Paths start with the owning user or house id, and every policy
-- checks that folder, so a file can only be written where its owner is allowed to write.
--
-- avatars/<user_id>/...                 public read, the user writes their own
-- house_headers/<house_id>/...          public read, the house's admins write
-- house-documents/<house_id>/<user_id>/ members read and upload, the uploader or an admin deletes
-- personal-documents/<user_id>/...      the user alone

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types) values
    ('avatars', 'avatars', true, 5242880, array['image/*']),
    ('house_headers', 'house_headers', true, 5242880, array['image/*']),
    ('house-documents', 'house-documents', false, 10485760, null),
    ('personal-documents', 'personal-documents', false, 10485760, null)
on conflict (id) do update set public = excluded.public, file_size_limit = excluded.file_size_limit;

do $$
declare
    v_policy record;
begin
    for v_policy in select policyname from pg_policies where schemaname = 'storage' and tablename = 'objects' loop
        execute format('drop policy %I on storage.objects', v_policy.policyname);
    end loop;
end;
$$;

create policy "avatars are public" on storage.objects for select
    using (bucket_id = 'avatars');
create policy "users write their own avatar" on storage.objects for insert to authenticated
    with check (bucket_id = 'avatars' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy "users replace their own avatar" on storage.objects for update to authenticated
    using (bucket_id = 'avatars' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy "users delete their own avatar" on storage.objects for delete to authenticated
    using (bucket_id = 'avatars' and (storage.foldername(name))[1] = (select auth.uid())::text);

create policy "house headers are public" on storage.objects for select
    using (bucket_id = 'house_headers');
create policy "admins write house headers" on storage.objects for insert to authenticated
    with check (bucket_id = 'house_headers' and public.auth_is_house_admin(((storage.foldername(name))[1])::uuid));
create policy "admins replace house headers" on storage.objects for update to authenticated
    using (bucket_id = 'house_headers' and public.auth_is_house_admin(((storage.foldername(name))[1])::uuid));
create policy "admins delete house headers" on storage.objects for delete to authenticated
    using (bucket_id = 'house_headers' and public.auth_is_house_admin(((storage.foldername(name))[1])::uuid));

create policy "members read house documents" on storage.objects for select to authenticated
    using (bucket_id = 'house-documents' and public.auth_is_house_member(((storage.foldername(name))[1])::uuid));
create policy "members upload house documents as themselves" on storage.objects for insert to authenticated
    with check (bucket_id = 'house-documents'
                and public.auth_is_house_member(((storage.foldername(name))[1])::uuid)
                and (storage.foldername(name))[2] = (select auth.uid())::text);
create policy "uploaders and admins delete house documents" on storage.objects for delete to authenticated
    using (bucket_id = 'house-documents'
           and ((storage.foldername(name))[2] = (select auth.uid())::text
                or public.auth_is_house_admin(((storage.foldername(name))[1])::uuid)));

create policy "users read their personal documents" on storage.objects for select to authenticated
    using (bucket_id = 'personal-documents' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy "users upload personal documents" on storage.objects for insert to authenticated
    with check (bucket_id = 'personal-documents' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy "users delete personal documents" on storage.objects for delete to authenticated
    using (bucket_id = 'personal-documents' and (storage.foldername(name))[1] = (select auth.uid())::text);
