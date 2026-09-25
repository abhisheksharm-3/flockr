-- Drops and recreates the public schema, then the files after this one rebuild it. Apply 00 to 09 in
-- order, as one transaction. Accounts live in auth.users and survive; everything in public does not.

drop schema if exists public cascade;
create schema public authorization postgres;
comment on schema public is 'Flockr application schema. Defined in supabase/schema/.';
