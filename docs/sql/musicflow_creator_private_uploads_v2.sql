-- MusicFlow Creator Studio v2: private, immutable uploads linked to an approved Creator's draft.
-- This is NOT the licensed public catalog publishing pipeline.
begin;

insert into storage.buckets(id, name, public, file_size_limit, allowed_mime_types)
values(
 'musicflow-creator-submissions',
 'musicflow-creator-submissions',
 false,
 6291456,
 array[
   'audio/mpeg','audio/mp4','audio/ogg','audio/flac','audio/wav','audio/x-wav',
   'application/pdf','image/jpeg','image/png'
 ]::text[]
)
on conflict(id) do update set
 public=false,
 file_size_limit=excluded.file_size_limit,
 allowed_mime_types=excluded.allowed_mime_types;

create table if not exists musicflow_private.creator_upload_slots(
 id uuid primary key default gen_random_uuid(),
 draft_id uuid not null references musicflow_private.creator_drafts(id) on delete cascade,
 creator_id uuid not null references auth.users(id) on delete cascade,
 kind text not null check(kind in ('audio','evidence')),
 object_path text not null unique,
 mime_type text not null,
 byte_count bigint not null check(byte_count between 1 and 6291456),
 state text not null default 'reserved' check(state in ('reserved','uploaded')),
 created_at timestamptz not null default now(),
 uploaded_at timestamptz,
 unique(draft_id,kind)
);
create index if not exists musicflow_creator_upload_owner
 on musicflow_private.creator_upload_slots(creator_id,draft_id);
alter table musicflow_private.creator_upload_slots enable row level security;
revoke all on musicflow_private.creator_upload_slots from public,anon,authenticated;

create or replace function public.musicflow_creator_upload_reserve(
 p_draft_id uuid, p_kind text, p_mime_type text, p_byte_count bigint
) returns jsonb language plpgsql security definer set search_path=''
as $$
declare
 uid uuid := auth.uid();
 draft record;
 existing record;
 suffix text;
 slot_id uuid;
 path text;
 bucket constant text := 'musicflow-creator-submissions';
begin
 if uid is null or not exists(
   select 1 from musicflow_private.user_roles where user_id=uid and role in ('artist','author')
 ) then raise exception 'Approved creator required' using errcode='42501'; end if;
 if p_kind not in ('audio','evidence')
   or p_byte_count is null or p_byte_count not between 1 and 6291456
   or p_mime_type is null
   or not (
     (p_kind='audio' and p_mime_type in (
       'audio/mpeg','audio/mp4','audio/ogg','audio/flac','audio/wav','audio/x-wav'))
     or (p_kind='evidence' and p_mime_type in (
       'application/pdf','image/jpeg','image/png'))
   ) then raise exception 'Unsupported upload type or size' using errcode='22023'; end if;

 select id,status into draft from musicflow_private.creator_drafts
 where id=p_draft_id and creator_id=uid for update;
 if not found or draft.status not in ('draft','needs_changes') then
   raise exception 'Draft is locked or unavailable' using errcode='42501'; end if;

 select * into existing from musicflow_private.creator_upload_slots
 where draft_id=p_draft_id and kind=p_kind for update;
 if found then
   if existing.state='uploaded' then raise exception 'Existing uploaded file is immutable' using errcode='23505'; end if;
   if existing.mime_type<>p_mime_type or existing.byte_count<>p_byte_count then
     raise exception 'Upload already reserved for another file; retry the original selection'
       using errcode='23505'; end if;
   return jsonb_build_object('id',existing.id,'bucket',bucket,'path',existing.object_path,
     'mime',existing.mime_type,'size',existing.byte_count,'state',existing.state);
 end if;
 suffix := case p_mime_type
   when 'audio/mpeg' then '.mp3'
   when 'audio/mp4' then '.m4a'
   when 'audio/ogg' then '.ogg'
   when 'audio/flac' then '.flac'
   when 'audio/wav' then '.wav'
   when 'audio/x-wav' then '.wav'
   when 'application/pdf' then '.pdf'
   when 'image/jpeg' then '.jpg'
   when 'image/png' then '.png' end;
 slot_id := gen_random_uuid();
 path := uid::text || '/' || p_draft_id::text || '/' || slot_id::text || suffix;
 insert into musicflow_private.creator_upload_slots
 (id,draft_id,creator_id,kind,object_path,mime_type,byte_count)
 values (slot_id,p_draft_id,uid,p_kind,path,p_mime_type,p_byte_count);
 return jsonb_build_object('id',slot_id,'bucket',bucket,'path',path,
   'mime',p_mime_type,'size',p_byte_count,'state','reserved');
end $$;

create or replace function public.musicflow_creator_storage_can_write(p_path text)
returns boolean language sql stable security definer set search_path=''
as $$
 select auth.uid() is not null and exists(
   select 1 from musicflow_private.creator_upload_slots f
   join musicflow_private.creator_drafts d on d.id=f.draft_id
   where f.object_path=p_path and f.creator_id=auth.uid()
     and d.creator_id=auth.uid()
     and d.status in ('draft','needs_changes')
     and f.state='reserved'
 );
$$;

create or replace function public.musicflow_creator_storage_can_read(p_path text)
returns boolean language sql stable security definer set search_path=''
as $$
 select auth.uid() is not null and exists(
   select 1 from musicflow_private.creator_upload_slots f
   join musicflow_private.creator_drafts d on d.id=f.draft_id
   where f.object_path=p_path and f.state='uploaded'
     and (f.creator_id=auth.uid() or (
       d.status in ('submitted','metadata_approved') and (
         exists(select 1 from musicflow_private.user_roles r
           where r.user_id=auth.uid() and r.role='owner')
         or (
           exists(select 1 from musicflow_private.user_roles r
             where r.user_id=auth.uid() and r.role='platform_admin')
           and exists(select 1 from musicflow_private.admin_permissions p
             where p.user_id=auth.uid() and p.permission='review_creators')
         )
       )
     ))
 );
$$;

-- INSERT only: no overwrite, update, delete or public read policies.
drop policy if exists "musicflow_creator_submissions_insert" on storage.objects;
create policy "musicflow_creator_submissions_insert" on storage.objects
for insert to authenticated with check(
 bucket_id='musicflow-creator-submissions'
 and public.musicflow_creator_storage_can_write(name)
);
drop policy if exists "musicflow_creator_submissions_read" on storage.objects;
create policy "musicflow_creator_submissions_read" on storage.objects
for select to authenticated using(
 bucket_id='musicflow-creator-submissions'
 and public.musicflow_creator_storage_can_read(name)
);

create or replace function public.musicflow_creator_upload_confirm(p_id uuid)
returns boolean language plpgsql security definer set search_path=''
as $$
declare uid uuid := auth.uid(); slot record; actual record;
begin
 select f.*,d.status as draft_status into slot
   from musicflow_private.creator_upload_slots f
   join musicflow_private.creator_drafts d on d.id=f.draft_id
   where f.id=p_id and f.creator_id=uid for update;
 if uid is null or not found or slot.draft_status not in ('draft','needs_changes')
 then raise exception 'Upload not available' using errcode='42501'; end if;
 if slot.state='uploaded' then return true; end if;
 select id,owner_id,metadata into actual from storage.objects
 where bucket_id='musicflow-creator-submissions' and name=slot.object_path;
 if not found or actual.owner_id is distinct from uid::text then
   raise exception 'Storage object not found or not owned by creator' using errcode='42501'; end if;
 if coalesce(actual.metadata->>'mimetype','')<>slot.mime_type
   or coalesce((actual.metadata->>'size')::bigint,-1)<>slot.byte_count then
   raise exception 'Storage file metadata mismatch' using errcode='22023'; end if;
 update musicflow_private.creator_upload_slots
 set state='uploaded',uploaded_at=now() where id=p_id;
 return true;
end $$;

create or replace function public.musicflow_creator_upload_list(p_draft_id uuid)
returns jsonb language plpgsql stable security definer set search_path=''
as $$
declare uid uuid := auth.uid(); draft record;
begin
 if uid is null then raise exception 'Sign in required' using errcode='28000'; end if;
 select creator_id,status into draft from musicflow_private.creator_drafts where id=p_draft_id;
 if not found then raise exception 'Draft not found' using errcode='42501'; end if;
 if uid<>draft.creator_id and not (
   draft.status in ('submitted','metadata_approved')
   and (
    exists(select 1 from musicflow_private.user_roles where user_id=uid and role='owner')
    or (
      exists(select 1 from musicflow_private.user_roles
        where user_id=uid and role='platform_admin')
      and exists(select 1 from musicflow_private.admin_permissions
        where user_id=uid and permission='review_creators')
    )
   )
 ) then raise exception 'Not authorized to view this draft' using errcode='42501'; end if;
 return coalesce((
   select jsonb_agg(to_jsonb(s) order by s.kind)
   from (
     select id,kind,object_path as path,mime_type as mime,byte_count as size,state,
       uploaded_at
     from musicflow_private.creator_upload_slots where draft_id=p_draft_id order by kind
   ) s
 ),'[]'::jsonb);
end $$;

-- Existing v1 submission must require both attachment kinds. The metadata
-- reviewer still CANNOT publish to music_tracks/public Storage.
create or replace function public.musicflow_creator_submit_draft(p_id uuid)
returns boolean language plpgsql security definer set search_path=''
as $$
declare uid uuid := auth.uid(); draft record;
begin
 if uid is null or not exists(select 1 from musicflow_private.user_roles
   where user_id=uid and role in ('artist','author'))
 then raise exception 'Approved Creator role required' using errcode='42501'; end if;
 select * into draft from musicflow_private.creator_drafts
 where id=p_id and creator_id=uid for update;
 if not found or draft.status not in ('draft','needs_changes') then
   raise exception 'Draft not available' using errcode='42501'; end if;
 if draft.license_code='UNVERIFIED' or not draft.streaming_claimed
   or char_length(trim(draft.rights_reference))<8 then
   raise exception 'Streaming rights declaration required' using errcode='22023'; end if;
 if not exists (
     select 1 from musicflow_private.creator_upload_slots
       where draft_id=p_id and kind='audio' and state='uploaded'
   ) or not exists (
     select 1 from musicflow_private.creator_upload_slots
       where draft_id=p_id and kind='evidence' and state='uploaded'
   ) then
   raise exception 'Upload one audio sample and one rights evidence document' using errcode='22023';
 end if;
 update musicflow_private.creator_drafts set status='submitted',review_note=null,
   reviewed_by=null,reviewed_at=null,updated_at=now() where id=p_id;
 insert into musicflow_private.role_audit(actor_id,target_id,action,details)
 values(uid,uid,'creator_draft_submitted',jsonb_build_object('draft_id',p_id));
 return true;
end $$;

revoke all on function public.musicflow_creator_upload_reserve(uuid,text,text,bigint) from public,anon;
revoke all on function public.musicflow_creator_upload_confirm(uuid) from public,anon;
revoke all on function public.musicflow_creator_upload_list(uuid) from public,anon;
revoke all on function public.musicflow_creator_storage_can_write(text) from public,anon;
revoke all on function public.musicflow_creator_storage_can_read(text) from public,anon;
grant execute on function public.musicflow_creator_upload_reserve(uuid,text,text,bigint) to authenticated;
grant execute on function public.musicflow_creator_upload_confirm(uuid) to authenticated;
grant execute on function public.musicflow_creator_upload_list(uuid) to authenticated;
grant execute on function public.musicflow_creator_storage_can_write(text) to authenticated;
grant execute on function public.musicflow_creator_storage_can_read(text) to authenticated;
commit;
