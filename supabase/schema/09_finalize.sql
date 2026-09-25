-- Work that needs the rest of the schema in place: profiles for accounts that already exist, the
-- daily bill reminder job, and telling the API to reload its view of the schema.

insert into public.profiles (id, email, full_name)
select id, lower(email), coalesce(raw_user_meta_data ->> 'full_name', '')
from auth.users
where email is not null
on conflict (id) do nothing;

create extension if not exists pg_cron with schema pg_catalog;
create extension if not exists pg_net with schema extensions;

select cron.unschedule(jobid) from cron.job where jobname = 'flockr-bill-reminders';
select cron.schedule('flockr-bill-reminders', '0 * * * *', 'select public.send_bill_reminders()');

notify pgrst, 'reload schema';
