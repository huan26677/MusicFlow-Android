-- MusicFlow Creator Studio v1: metadata-only drafts and review.
-- No audio upload and no changes to the licensed publishing pipeline.
begin;

create table if not exists musicflow_private.creator_drafts (
  id uuid primary key default gen_random_uuid(),
  creator_id uuid not null references auth.users(id) on delete cascade,
  title text not null check (char_length(trim(title)) between 1 and 240),
  artist text not null check (char_length(trim(artist)) between 2 and 180),
  album text not null default '' check (char_length(album) <= 180),
  rights_holder text not null check (char_length(trim(rights_holder)) between 2 and 180),
  license_code text not null default 'UNVERIFIED'
    check (license_code in ('UNVERIFIED','CC0-1.0','CC-BY-4.0','DIRECT')),
  streaming_claimed boolean not null default false,
  rights_reference text not null default '' check (char_length(rights_reference) <= 1000),
  credits jsonb not null default '[]'::jsonb check (jsonb_typeof(credits) = 'array'),
  status text not null default 'draft'
    check (status in ('draft','submitted','needs_changes','metadata_approved')),
  review_note text,
  reviewed_by uuid references auth.users(id) on delete set null,
  reviewed_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists musicflow_creator_drafts_owner
  on musicflow_private.creator_drafts(creator_id, updated_at desc);
create index if not exists musicflow_creator_drafts_review
  on musicflow_private.creator_drafts(created_at desc) where status='submitted';

revoke all on musicflow_private.creator_drafts from public, anon, authenticated;
alter table musicflow_private.creator_drafts enable row level security;

create or replace function public.musicflow_creator_draft_save(
  p_id uuid, p_title text, p_artist text, p_album text,
  p_rights_holder text, p_license_code text, p_streaming_claimed boolean,
  p_rights_reference text, p_credits jsonb
) returns uuid language plpgsql security definer set search_path = ''
as $$
declare uid uuid := auth.uid(); draft record; credit jsonb; result_id uuid;
begin
  if uid is null or not exists(
    select 1 from musicflow_private.user_roles
    where user_id=uid and role in ('artist','author')
  ) then raise exception 'Approved creator role required' using errcode='42501'; end if;

  if char_length(trim(coalesce(p_title,''))) not between 1 and 240
    or char_length(trim(coalesce(p_artist,''))) not between 2 and 180
    or char_length(coalesce(p_album,'')) > 180
    or char_length(trim(coalesce(p_rights_holder,''))) not between 2 and 180
    or p_license_code is null
    or p_license_code not in ('UNVERIFIED','CC0-1.0','CC-BY-4.0','DIRECT')
    or p_streaming_claimed is null
    or char_length(coalesce(p_rights_reference,'')) > 1000
    or p_credits is null or jsonb_typeof(p_credits) <> 'array'
    or jsonb_array_length(p_credits) not between 1 and 12 then
    raise exception 'Invalid creator draft fields' using errcode='22023';
  end if;

  for credit in select value from jsonb_array_elements(p_credits)
  loop
    if jsonb_typeof(credit) <> 'object'
      or char_length(trim(coalesce(credit->>'name',''))) not between 2 and 100
      or (credit->>'contribution') not in
        ('performer','composer','lyricist','producer','master_owner') then
      raise exception 'Invalid contributor credit' using errcode='22023';
    end if;
  end loop;

  if p_id is null then
    insert into musicflow_private.creator_drafts
      (creator_id,title,artist,album,rights_holder,license_code,
       streaming_claimed,rights_reference,credits)
    values
      (uid,trim(p_title),trim(p_artist),coalesce(p_album,''),
       trim(p_rights_holder),p_license_code,
       p_streaming_claimed,coalesce(p_rights_reference,''),p_credits)
    returning id into result_id;
  else
    select id, creator_id, status into draft
      from musicflow_private.creator_drafts
      where id=p_id for update;
    if not found or draft.creator_id<>uid or draft.status not in ('draft','needs_changes') then
      raise exception 'Draft unavailable or locked' using errcode='42501';
    end if;
    update musicflow_private.creator_drafts
      set title=trim(p_title),artist=trim(p_artist),album=coalesce(p_album,''),
        rights_holder=trim(p_rights_holder),license_code=p_license_code,
        streaming_claimed=p_streaming_claimed,
        rights_reference=coalesce(p_rights_reference,''),credits=p_credits,
        status='draft',review_note=null,reviewed_by=null,reviewed_at=null,
        updated_at=now()
      where id=p_id;
    result_id := p_id;
  end if;
  return result_id;
end $$;

create or replace function public.musicflow_creator_my_drafts()
returns jsonb language plpgsql stable security definer set search_path = ''
as $$
declare uid uuid := auth.uid();
begin
  if uid is null then raise exception 'Authentication required' using errcode='28000'; end if;
  return coalesce((
    select jsonb_agg(to_jsonb(s) order by s.updated_at desc)
    from (
      select id,title,artist,album,rights_holder,license_code,
        streaming_claimed,rights_reference,credits,status,review_note,
        created_at,updated_at
      from musicflow_private.creator_drafts where creator_id=uid
      order by updated_at desc limit 50
    ) s
  ),'[]'::jsonb);
end $$;

create or replace function public.musicflow_creator_submit_draft(p_id uuid)
returns boolean language plpgsql security definer set search_path = ''
as $$
declare uid uuid := auth.uid(); draft record;
begin
  if uid is null or not exists (
    select 1 from musicflow_private.user_roles
    where user_id=uid and role in ('artist','author')
  ) then raise exception 'Approved creator role required' using errcode='42501'; end if;
  select * into draft from musicflow_private.creator_drafts
    where id=p_id and creator_id=uid for update;
  if not found or draft.status not in ('draft','needs_changes') then
    raise exception 'Draft not available for submission' using errcode='42501'; end if;
  if draft.license_code = 'UNVERIFIED'
    or not draft.streaming_claimed
    or char_length(trim(draft.rights_reference)) < 8 then
    raise exception 'License and streaming evidence required' using errcode='22023'; end if;
  update musicflow_private.creator_drafts
    set status='submitted',review_note=null,reviewed_by=null,reviewed_at=null,updated_at=now()
    where id=p_id;
  insert into musicflow_private.role_audit(actor_id,target_id,action,details)
    values(uid,uid,'creator_draft_submitted',jsonb_build_object('draft_id',p_id));
  return true;
end $$;

create or replace function public.musicflow_pending_drafts()
returns jsonb language plpgsql stable security definer set search_path = ''
as $$
declare uid uuid := auth.uid();
begin
  if uid is null or not (
    exists(select 1 from musicflow_private.user_roles
      where user_id=uid and role='owner')
    or (exists(select 1 from musicflow_private.user_roles
      where user_id=uid and role='platform_admin')
      and exists(select 1 from musicflow_private.admin_permissions
      where user_id=uid and permission='review_creators'))
  ) then raise exception 'Review permission required' using errcode='42501'; end if;

  return coalesce((
    select jsonb_agg(to_jsonb(s) order by s.created_at)
    from (
      select d.id,d.creator_id,u.email as creator_email,
        d.title,d.artist,d.album,d.rights_holder,d.license_code,
        d.streaming_claimed,d.rights_reference,d.credits,d.created_at
      from musicflow_private.creator_drafts d
      join auth.users u on u.id=d.creator_id
      where d.status='submitted'
      order by d.created_at limit 50
    ) s
  ),'[]'::jsonb);
end $$;

create or replace function public.musicflow_review_draft(
  p_id uuid, p_approve boolean, p_reason text
) returns boolean language plpgsql security definer set search_path = ''
as $$
declare uid uuid := auth.uid(); draft record;
begin
  if uid is null or not (
    exists(select 1 from musicflow_private.user_roles
      where user_id=uid and role='owner')
    or (exists(select 1 from musicflow_private.user_roles
      where user_id=uid and role='platform_admin')
      and exists(select 1 from musicflow_private.admin_permissions
      where user_id=uid and permission='review_creators'))
  ) then raise exception 'Review permission required' using errcode='42501'; end if;
  if p_approve is null or char_length(trim(coalesce(p_reason,''))) not between 5 and 300
    then raise exception 'Decision and reason required' using errcode='22023'; end if;
  select * into draft from musicflow_private.creator_drafts
    where id=p_id and status='submitted' for update;
  if not found then raise exception 'Pending draft not found' using errcode='22023'; end if;
  if draft.creator_id=uid then raise exception 'Cannot review your own draft' using errcode='42501'; end if;
  update musicflow_private.creator_drafts
    set status=case when p_approve then 'metadata_approved' else 'needs_changes' end,
      review_note=trim(p_reason),reviewed_by=uid,reviewed_at=now(),updated_at=now()
    where id=p_id;
  insert into musicflow_private.role_audit(actor_id,target_id,action,details)
    values(uid,draft.creator_id,
      case when p_approve then 'creator_draft_metadata_approved'
        else 'creator_draft_needs_changes' end,
      jsonb_build_object('draft_id',p_id,'reason',trim(p_reason)));
  -- Deliberately NO update to public.music_tracks or storage objects.
  return true;
end $$;

revoke all on function public.musicflow_creator_draft_save(uuid,text,text,text,text,text,boolean,text,jsonb) from public, anon;
revoke all on function public.musicflow_creator_my_drafts() from public, anon;
revoke all on function public.musicflow_creator_submit_draft(uuid) from public, anon;
revoke all on function public.musicflow_pending_drafts() from public, anon;
revoke all on function public.musicflow_review_draft(uuid,boolean,text) from public, anon;
grant execute on function public.musicflow_creator_draft_save(uuid,text,text,text,text,text,boolean,text,jsonb) to authenticated;
grant execute on function public.musicflow_creator_my_drafts() to authenticated;
grant execute on function public.musicflow_creator_submit_draft(uuid) to authenticated;
grant execute on function public.musicflow_pending_drafts() to authenticated;
grant execute on function public.musicflow_review_draft(uuid,boolean,text) to authenticated;
commit;
