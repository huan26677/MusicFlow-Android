-- MusicFlow Owner Demo Readiness v2: additive, owner-only, aggregate counters.
-- No account identifiers, passwords, test credentials, or role mutations.
-- Apply to the existing Supabase project after Owner Dashboard v1.
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
    'creator_files_uploaded', (select count(*) from musicflow_private.creator_upload_slots where state = 'uploaded'),
    'active_premium_count', (select count(*) from public.account_entitlements e
      where e.tier = 'premium' and e.expires_at > now()),
    'non_premium_count', (select count(*) from auth.users u
      where not exists (
        select 1 from public.account_entitlements e
        where e.user_id = u.id and e.tier = 'premium' and e.expires_at > now()
      )),
    'delegated_admin_count', (select count(*) from musicflow_private.user_roles r
      where r.role = 'platform_admin'
      and (select count(distinct p.permission) from musicflow_private.admin_permissions p
        where p.user_id = r.user_id
        and p.permission in ('manage_users', 'review_creators', 'moderate_catalog')) = 3)
  );
end;
$$;

revoke all on function public.musicflow_owner_demo_summary() from public, anon;
grant execute on function public.musicflow_owner_demo_summary() to authenticated;
commit;
