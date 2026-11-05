-- MusicFlow role, staff, creator and moderation foundation. Idempotent setup.
-- Keep all security data in unexposed musicflow_private schema.
-- NEVER insert an Owner from anonymous signup or client-editable metadata.
begin;

create schema if not exists musicflow_private;
revoke all on schema musicflow_private from public, anon, authenticated;

create table if not exists musicflow_private.user_roles (
  user_id uuid not null references auth.users(id) on delete cascade,
  role text not null check (role in ('owner','platform_admin','developer','moderator','artist','author')),
  granted_by uuid references auth.users(id) on delete set null,
  created_at timestamptz not null default now(),
  primary key (user_id, role)
);
create unique index if not exists musicflow_one_owner
  on musicflow_private.user_roles (role) where role = 'owner';

create table if not exists musicflow_private.admin_permissions (
  user_id uuid not null references auth.users(id) on delete cascade,
  permission text not null check (permission in ('manage_users','review_creators','moderate_catalog')),
  granted_by uuid references auth.users(id) on delete set null,
  created_at timestamptz not null default now(),
  primary key (user_id, permission)
);
create table if not exists musicflow_private.creator_applications (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  requested_role text not null check (requested_role in ('artist','author')),
  requested_name text not null check (char_length(trim(requested_name)) between 2 and 80),
  note text not null default '' check (char_length(note) <= 1000),
  status text not null default 'pending' check (status in ('pending','approved','rejected')),
  reviewed_by uuid references auth.users(id) on delete set null,
  reviewed_at timestamptz,
  review_note text,
  created_at timestamptz not null default now()
);
create unique index if not exists musicflow_creator_one_pending
  on musicflow_private.creator_applications(user_id, requested_role) where status='pending';

create table if not exists musicflow_private.creator_profiles (
  user_id uuid primary key references auth.users(id) on delete cascade,
  stage_name text not null check (char_length(trim(stage_name)) between 2 and 80),
  bio text not null default '' check (char_length(bio) <= 1000),
  updated_at timestamptz not null default now()
);
create table if not exists musicflow_private.role_audit (
  id bigint generated always as identity primary key,
  actor_id uuid references auth.users(id) on delete set null,
  target_id uuid references auth.users(id) on delete set null,
  action text not null,
  details jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);
create index if not exists musicflow_role_audit_recent on musicflow_private.role_audit(created_at desc);
create index if not exists musicflow_role_requests_recent on musicflow_private.creator_applications(created_at desc);

revoke all on all tables in schema musicflow_private from public, anon, authenticated;
revoke all on all sequences in schema musicflow_private from public, anon, authenticated;
alter table musicflow_private.user_roles enable row level security;
alter table musicflow_private.admin_permissions enable row level security;
alter table musicflow_private.creator_applications enable row level security;
alter table musicflow_private.creator_profiles enable row level security;
alter table musicflow_private.role_audit enable row level security;

create or replace function public.musicflow_my_access()
returns jsonb language plpgsql stable security definer set search_path = ''
as $$
declare uid uuid := auth.uid();
begin
  if uid is null then raise exception 'Authentication required' using errcode='28000'; end if;
  return jsonb_build_object(
    'roles', coalesce((select jsonb_agg(r.role order by r.role)
      from musicflow_private.user_roles r where r.user_id=uid),'[]'::jsonb),
    'permissions', coalesce((select jsonb_agg(p.permission order by p.permission)
      from musicflow_private.admin_permissions p
      where p.user_id=uid and exists (
        select 1 from musicflow_private.user_roles r
        where r.user_id=uid and r.role='platform_admin')),'[]'::jsonb)
  );
end $$;

create or replace function public.musicflow_admin_users(p_search text default '', p_limit integer default 20)
returns jsonb language plpgsql stable security definer set search_path = ''
as $$
declare uid uuid := auth.uid();
begin
  if uid is null or not (
    exists(select 1 from musicflow_private.user_roles r where r.user_id=uid and r.role='owner')
    or (
      exists(select 1 from musicflow_private.user_roles r where r.user_id=uid and r.role='platform_admin')
      and exists(select 1 from musicflow_private.admin_permissions p
        where p.user_id=uid and p.permission='manage_users')
    )
  ) then raise exception 'Access denied' using errcode='42501'; end if;
  if char_length(coalesce(p_search,'')) > 80 then raise exception 'Search is too long'; end if;
  return coalesce((
    select jsonb_agg(to_jsonb(s) order by s.created_at desc) from (
      select u.id, u.email, u.created_at, coalesce(ap.display_name,'') as display_name,
      coalesce(e.tier,'free') as tier,
      coalesce((select jsonb_agg(r.role order by r.role) from musicflow_private.user_roles r
        where r.user_id=u.id),'[]'::jsonb) as roles
      from auth.users u
      left join public.account_profiles ap on ap.id=u.id
      left join public.account_entitlements e on e.user_id=u.id
      where u.email ilike '%' || coalesce(p_search,'') || '%'
      order by u.created_at desc
      limit least(greatest(coalesce(p_limit,20),1),50)
    ) s
  ),'[]'::jsonb);
end $$;

create or replace function public.musicflow_manage_role(
  p_user_id uuid, p_role text, p_grant boolean, p_reason text
) returns jsonb language plpgsql security definer set search_path = ''
as $$
declare uid uuid := auth.uid();
begin
  if uid is null or not exists (
    select 1 from musicflow_private.user_roles where user_id=uid and role='owner'
  ) then raise exception 'Only Owner can manage roles' using errcode='42501'; end if;
  if p_grant is null
    or p_role is null or p_role not in ('platform_admin','developer','moderator','artist','author')
    or p_user_id is null or p_user_id=uid
    or exists(select 1 from musicflow_private.user_roles where user_id=p_user_id and role='owner')
  then raise exception 'Invalid target or role' using errcode='22023'; end if;
  if char_length(trim(coalesce(p_reason,''))) < 5 or char_length(p_reason) > 300
    then raise exception 'A reason of 5-300 characters is required'; end if;
  if not exists(select 1 from auth.users where id=p_user_id) then
    raise exception 'Target user not found'; end if;
  if p_grant then
    insert into musicflow_private.user_roles(user_id,role,granted_by)
      values (p_user_id,p_role,uid)
      on conflict(user_id,role) do nothing;
  else
    delete from musicflow_private.user_roles
      where user_id=p_user_id and role=p_role;
    if p_role='platform_admin' then
      delete from musicflow_private.admin_permissions where user_id=p_user_id;
    end if;
  end if;
  insert into musicflow_private.role_audit(actor_id,target_id,action,details)
    values(uid,p_user_id,case when p_grant then 'role_granted' else 'role_revoked' end,
      jsonb_build_object('role',p_role,'reason',trim(p_reason)));
  return public.musicflow_my_access();
end $$;

create or replace function public.musicflow_set_admin_permission(
 p_user_id uuid, p_permission text, p_grant boolean, p_reason text
) returns boolean language plpgsql security definer set search_path = ''
as $$
declare uid uuid := auth.uid();
begin
  if uid is null or not exists(select 1 from musicflow_private.user_roles where user_id=uid and role='owner')
    then raise exception 'Only Owner can grant permissions' using errcode='42501'; end if;
  if p_grant is null
    or p_permission not in ('manage_users','review_creators','moderate_catalog')
    or p_user_id is null or p_user_id=uid
    or not exists(select 1 from musicflow_private.user_roles where user_id=p_user_id and role='platform_admin')
    then raise exception 'Target must be a PlatformAdmin' using errcode='22023'; end if;
  if char_length(trim(coalesce(p_reason,''))) < 5 or char_length(p_reason) > 300
    then raise exception 'Reason must be 5-300 characters'; end if;
  if p_grant then
    insert into musicflow_private.admin_permissions(user_id,permission,granted_by)
      values(p_user_id,p_permission,uid) on conflict(user_id,permission) do nothing;
  else
    delete from musicflow_private.admin_permissions
      where user_id=p_user_id and permission=p_permission;
  end if;
  insert into musicflow_private.role_audit(actor_id,target_id,action,details)
    values(uid,p_user_id,case when p_grant then 'permission_granted' else 'permission_revoked' end,
      jsonb_build_object('permission',p_permission,'reason',trim(p_reason)));
  return true;
end $$;

create or replace function public.musicflow_apply_creator(
 p_role text, p_name text, p_note text default ''
) returns uuid language plpgsql security definer set search_path = ''
as $$
declare uid uuid := auth.uid(); application_id uuid;
begin
  if uid is null then raise exception 'Sign in required' using errcode='28000'; end if;
  if p_role not in ('artist','author')
    or char_length(trim(coalesce(p_name,''))) not between 2 and 80
    or char_length(coalesce(p_note,'')) > 1000
    then raise exception 'Invalid creator application' using errcode='22023'; end if;
  if exists(select 1 from musicflow_private.user_roles
    where user_id=uid and role=p_role) then raise exception 'Role already approved'; end if;
  insert into musicflow_private.creator_applications(user_id,requested_role,requested_name,note)
    values(uid,p_role,trim(p_name),coalesce(p_note,'')) returning id into application_id;
  return application_id;
end $$;

create or replace function public.musicflow_my_creator_requests()
returns jsonb language plpgsql stable security definer set search_path = ''
as $$
declare uid uuid := auth.uid();
begin
  if uid is null then raise exception 'Sign in required' using errcode='28000'; end if;
  return coalesce((select jsonb_agg(to_jsonb(s) order by s.created_at desc) from (
    select id, requested_role, requested_name, status, created_at, review_note
    from musicflow_private.creator_applications
    where user_id=uid order by created_at desc limit 20
  ) s),'[]'::jsonb);
end $$;

create or replace function public.musicflow_pending_creators()
returns jsonb language plpgsql stable security definer set search_path = ''
as $$
declare uid uuid := auth.uid();
begin
  if uid is null or not (
    exists(select 1 from musicflow_private.user_roles where user_id=uid and role='owner')
    or (
      exists(select 1 from musicflow_private.user_roles where user_id=uid and role='platform_admin')
      and exists(select 1 from musicflow_private.admin_permissions
        where user_id=uid and permission='review_creators')
    )
  ) then raise exception 'Access denied' using errcode='42501'; end if;
  return coalesce((select jsonb_agg(to_jsonb(s) order by s.created_at) from (
    select a.id, a.user_id, u.email, a.requested_role, a.requested_name, a.note, a.created_at
      from musicflow_private.creator_applications a
      join auth.users u on u.id=a.user_id
      where a.status='pending'
      order by a.created_at limit 50
  ) s),'[]'::jsonb);
end $$;

create or replace function public.musicflow_review_creator(
 p_application uuid, p_approve boolean, p_reason text
) returns boolean language plpgsql security definer set search_path = ''
as $$
declare uid uuid := auth.uid(); req record;
begin
  if uid is null or not (
    exists(select 1 from musicflow_private.user_roles where user_id=uid and role='owner')
    or (
      exists(select 1 from musicflow_private.user_roles where user_id=uid and role='platform_admin')
      and exists(select 1 from musicflow_private.admin_permissions
        where user_id=uid and permission='review_creators')
    )
  ) then raise exception 'Access denied' using errcode='42501'; end if;
  if char_length(trim(coalesce(p_reason,''))) < 5 or char_length(p_reason)>300
    then raise exception 'Review reason must be 5-300 characters'; end if;
  select * into req from musicflow_private.creator_applications
    where id=p_application and status='pending' for update;
  if not found then raise exception 'Pending application not found'; end if;
  if p_approve is null or req.user_id=uid then
    raise exception 'Reviewer cannot review their own application or omit a decision';
  end if;
  update musicflow_private.creator_applications
    set status=case when p_approve then 'approved' else 'rejected' end,
    review_note=trim(p_reason), reviewed_by=uid, reviewed_at=now()
    where id=p_application;
  if p_approve then
    insert into musicflow_private.user_roles(user_id,role,granted_by)
      values(req.user_id, req.requested_role, uid)
      on conflict (user_id,role) do nothing;
    insert into musicflow_private.creator_profiles(user_id,stage_name)
      values(req.user_id,req.requested_name)
      on conflict(user_id) do nothing;
  end if;
  insert into musicflow_private.role_audit(actor_id,target_id,action,details)
    values(uid,req.user_id,case when p_approve then 'creator_approved' else 'creator_rejected' end,
      jsonb_build_object('application_id',p_application,'role',req.requested_role,'reason',trim(p_reason)));
  return true;
end $$;

create or replace function public.musicflow_my_creator_profile()
returns jsonb language plpgsql stable security definer set search_path = ''
as $$
declare uid uuid := auth.uid();
begin
  if uid is null then raise exception 'Sign in required' using errcode='28000'; end if;
  return coalesce((select jsonb_build_object('stage_name',stage_name,'bio',bio)
    from musicflow_private.creator_profiles where user_id=uid),'{}'::jsonb);
end $$;

create or replace function public.musicflow_save_creator_profile(
 p_name text, p_bio text default ''
) returns boolean language plpgsql security definer set search_path = ''
as $$
declare uid uuid := auth.uid();
begin
  if uid is null or not exists(
    select 1 from musicflow_private.user_roles where user_id=uid and role in ('artist','author')
  ) then raise exception 'Approved creator role required' using errcode='42501'; end if;
  if char_length(trim(coalesce(p_name,''))) not between 2 and 80
    or char_length(coalesce(p_bio,'')) > 1000 then raise exception 'Invalid creator profile'; end if;
  insert into musicflow_private.creator_profiles(user_id,stage_name,bio)
    values(uid,trim(p_name),coalesce(p_bio,''))
    on conflict(user_id) do update set stage_name=excluded.stage_name,
      bio=excluded.bio, updated_at=now();
  return true;
end $$;

create or replace function public.musicflow_admin_tracks()
returns jsonb language plpgsql stable security definer set search_path = ''
as $$
declare uid uuid := auth.uid();
begin
  if uid is null or not (
    exists(select 1 from musicflow_private.user_roles where user_id=uid and role='owner')
    or (exists(select 1 from musicflow_private.user_roles where user_id=uid and role='platform_admin')
      and exists(select 1 from musicflow_private.admin_permissions
        where user_id=uid and permission='moderate_catalog'))
  ) then raise exception 'Access denied' using errcode='42501'; end if;
  return coalesce((select jsonb_agg(to_jsonb(s)) from (
    select id, title, artist, license_code, rights_confirmed, streaming_allowed, is_published
    from public.music_tracks order by created_at desc limit 50
  ) s),'[]'::jsonb);
end $$;

create or replace function public.musicflow_review_track(
 p_track uuid, p_publish boolean, p_reason text
) returns boolean language plpgsql security definer set search_path = ''
as $$
declare uid uuid := auth.uid(); row_track record; receipt record;
begin
  if uid is null or not (
    exists(select 1 from musicflow_private.user_roles where user_id=uid and role='owner')
    or (exists(select 1 from musicflow_private.user_roles where user_id=uid and role='platform_admin')
      and exists(select 1 from musicflow_private.admin_permissions
        where user_id=uid and permission='moderate_catalog'))
  ) then raise exception 'Access denied' using errcode='42501'; end if;
  if char_length(trim(coalesce(p_reason,''))) < 5 or char_length(p_reason)>300
    then raise exception 'Reason must be 5-300 characters'; end if;
  if p_publish is null then raise exception 'Publishing decision required'; end if;
  select id, rights_confirmed,streaming_allowed,license_code,stream_url,
    rights_verified_at,attribution_text,source_url into row_track
    from public.music_tracks where id=p_track for update;
  if not found then raise exception 'Track not found'; end if;
  if p_publish and not (row_track.rights_confirmed and row_track.streaming_allowed
    and row_track.license_code <> 'UNVERIFIED') then
    raise exception 'Streaming rights have not been verified' using errcode='42501';
  end if;
  if p_publish then
    select published_at,published_path,rights_reference,rights_confirmed_by,
      rights_reviewed_at into receipt
      from public.music_uploads where track_id=p_track;
    if not found or receipt.published_at is null or receipt.published_path is null
      or char_length(trim(coalesce(receipt.rights_reference,''))) < 8
      or char_length(trim(coalesce(receipt.rights_confirmed_by,''))) < 2
      or receipt.rights_reviewed_at is null
      or row_track.rights_verified_at is null
      or char_length(trim(coalesce(row_track.attribution_text,''))) < 3
      or row_track.source_url is null
      or row_track.stream_url is distinct from
        ('https://vzvxpbwrftlnyvuhfoqn.supabase.co/storage/v1/object/public/musicflow-public-audio/' || receipt.published_path)
    then raise exception 'Publish through the licensed evidence pipeline first'
      using errcode='42501'; end if;
  end if;
  update public.music_tracks set is_published=p_publish where id=p_track;
  insert into musicflow_private.role_audit(actor_id,action,details)
    values(uid,case when p_publish then 'track_published' else 'track_unpublished' end,
      jsonb_build_object('track',p_track,'reason',trim(p_reason)));
  return true;
end $$;

create or replace function public.musicflow_owner_audit()
returns jsonb language plpgsql stable security definer set search_path = ''
as $$
declare uid uuid := auth.uid();
begin
  if uid is null or not exists(select 1 from musicflow_private.user_roles
    where user_id=uid and role='owner') then raise exception 'Owner required' using errcode='42501'; end if;
  return coalesce((select jsonb_agg(to_jsonb(s) order by s.created_at desc) from (
    select actor_id,target_id,action,details,created_at
      from musicflow_private.role_audit order by created_at desc limit 50
  ) s),'[]'::jsonb);
end $$;

revoke all on function public.musicflow_my_access() from public, anon;
revoke all on function public.musicflow_admin_users(text,integer) from public, anon;
revoke all on function public.musicflow_manage_role(uuid,text,boolean,text) from public, anon;
revoke all on function public.musicflow_set_admin_permission(uuid,text,boolean,text) from public, anon;
revoke all on function public.musicflow_apply_creator(text,text,text) from public, anon;
revoke all on function public.musicflow_my_creator_requests() from public, anon;
revoke all on function public.musicflow_pending_creators() from public, anon;
revoke all on function public.musicflow_review_creator(uuid,boolean,text) from public, anon;
revoke all on function public.musicflow_my_creator_profile() from public, anon;
revoke all on function public.musicflow_save_creator_profile(text,text) from public, anon;
revoke all on function public.musicflow_admin_tracks() from public, anon;
revoke all on function public.musicflow_review_track(uuid,boolean,text) from public, anon;
revoke all on function public.musicflow_owner_audit() from public, anon;
grant execute on function public.musicflow_my_access() to authenticated;
grant execute on function public.musicflow_admin_users(text,integer) to authenticated;
grant execute on function public.musicflow_manage_role(uuid,text,boolean,text) to authenticated;
grant execute on function public.musicflow_set_admin_permission(uuid,text,boolean,text) to authenticated;
grant execute on function public.musicflow_apply_creator(text,text,text) to authenticated;
grant execute on function public.musicflow_my_creator_requests() to authenticated;
grant execute on function public.musicflow_pending_creators() to authenticated;
grant execute on function public.musicflow_review_creator(uuid,boolean,text) to authenticated;
grant execute on function public.musicflow_my_creator_profile() to authenticated;
grant execute on function public.musicflow_save_creator_profile(text,text) to authenticated;
grant execute on function public.musicflow_admin_tracks() to authenticated;
grant execute on function public.musicflow_review_track(uuid,boolean,text) to authenticated;
grant execute on function public.musicflow_owner_audit() to authenticated;

-- Owner bootstrap is deliberately NOT in migration. Verify the owner's actual
-- auth.users ID and email in Dashboard, then run the documented manual seed.
commit;
