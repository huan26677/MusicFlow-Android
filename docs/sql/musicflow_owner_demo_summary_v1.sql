-- MusicFlow Owner Readiness Dashboard v1. Read-only, report/demo scope.
-- The OWNER role is created manually by the verified project owner, never here.
begin;

create or replace function public.musicflow_owner_demo_summary()
returns jsonb
language plpgsql stable security definer
set search_path = ''
as $$
declare
  uid uuid := auth.uid();
begin
  if uid is null or not exists (
    select 1 from musicflow_private.user_roles r
    where r.user_id = uid and r.role = 'owner'
  ) then
    raise exception 'Owner access required' using errcode = '42501';
  end if;

  return jsonb_build_object(
    'owner_count', (select count(*) from musicflow_private.user_roles where role = 'owner'),
    'user_count', (select count(*) from auth.users),
    'platform_admin_count', (select count(*) from musicflow_private.user_roles where role = 'platform_admin'),
    'moderator_count', (select count(*) from musicflow_private.user_roles where role = 'moderator'),
    'artist_count', (select count(*) from musicflow_private.user_roles where role = 'artist'),
    'author_count', (select count(*) from musicflow_private.user_roles where role = 'author'),
    'developer_count', (select count(*) from musicflow_private.user_roles where role = 'developer'),
    'creator_requests_pending', (select count(*) from musicflow_private.creator_applications where status = 'pending'),
    'drafts_pending', (select count(*) from musicflow_private.creator_drafts where status = 'submitted'),
    'drafts_metadata_approved', (select count(*) from musicflow_private.creator_drafts where status = 'metadata_approved'),
    'creator_files_uploaded', (select count(*) from musicflow_private.creator_upload_slots where state = 'uploaded')
  );
end;
$$;

revoke all on function public.musicflow_owner_demo_summary() from public, anon;
grant execute on function public.musicflow_owner_demo_summary() to authenticated;
commit;
