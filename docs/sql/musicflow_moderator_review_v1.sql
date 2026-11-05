-- MusicFlow Moderator Metadata Review v1, report/demo scope only.
-- Least privilege: Moderator can review submitted creator *metadata*, never
-- grant creator roles, read arbitrary users, manage entitlements or publish streams.
begin;

create or replace function public.musicflow_can_review_metadata()
returns boolean language sql stable security definer set search_path = ''
as $$
 select auth.uid() is not null and (
   exists(
     select 1 from musicflow_private.user_roles
     where user_id=auth.uid() and role='owner'
   )
   or (
     exists(select 1 from musicflow_private.user_roles
       where user_id=auth.uid() and role='platform_admin')
     and exists(select 1 from musicflow_private.admin_permissions
       where user_id=auth.uid() and permission='review_creators')
   )
   or exists(
     select 1 from musicflow_private.user_roles
     where user_id=auth.uid() and role='moderator'
   )
 );
$$;

create or replace function public.musicflow_pending_drafts()
returns jsonb language plpgsql stable security definer set search_path = ''
as $$
begin
 if not public.musicflow_can_review_metadata() then
   raise exception 'Metadata review permission required' using errcode='42501';
 end if;
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
 ), '[]'::jsonb);
end $$;

create or replace function public.musicflow_review_draft(
 p_id uuid, p_approve boolean, p_reason text
) returns boolean language plpgsql security definer set search_path = ''
as $$
declare uid uuid := auth.uid(); draft record;
begin
 if not public.musicflow_can_review_metadata() then
   raise exception 'Metadata review permission required' using errcode='42501';
 end if;
 if p_approve is null or char_length(trim(coalesce(p_reason,''))) not between 5 and 300
   then raise exception 'Decision and reason required' using errcode='22023'; end if;
 select * into draft from musicflow_private.creator_drafts
   where id=p_id and status='submitted' for update;
 if not found then raise exception 'Pending draft not found' using errcode='22023'; end if;
 if draft.creator_id=uid then
   raise exception 'Cannot review your own draft' using errcode='42501';
 end if;
 update musicflow_private.creator_drafts
   set status=case when p_approve then 'metadata_approved' else 'needs_changes' end,
     review_note=trim(p_reason),reviewed_by=uid,reviewed_at=now(),updated_at=now()
   where id=p_id;
 insert into musicflow_private.role_audit(actor_id,target_id,action,details)
   values(uid,draft.creator_id,
     case when p_approve then 'creator_draft_metadata_approved'
       else 'creator_draft_needs_changes' end,
     jsonb_build_object('draft_id',p_id,'reason',trim(p_reason)));
 -- NEVER modify music_tracks or storage/public catalog from metadata review.
 return true;
end $$;

create or replace function public.musicflow_creator_upload_list(p_draft_id uuid)
returns jsonb language plpgsql stable security definer set search_path = ''
as $$
declare uid uuid := auth.uid(); draft record;
begin
 if uid is null then raise exception 'Sign in required' using errcode='28000'; end if;
 select creator_id,status into draft
   from musicflow_private.creator_drafts where id=p_draft_id;
 if not found then raise exception 'Draft not found' using errcode='42501'; end if;
 if uid<>draft.creator_id and not (
    draft.status in ('submitted','metadata_approved')
    and public.musicflow_can_review_metadata()
 ) then raise exception 'Not authorized to view this draft' using errcode='42501'; end if;
 return coalesce((
   select jsonb_agg(to_jsonb(s) order by s.kind)
   from (
     select id,kind,object_path as path,mime_type as mime,
       byte_count as size,state,uploaded_at
     from musicflow_private.creator_upload_slots
     where draft_id=p_draft_id order by kind
   ) s
 ),'[]'::jsonb);
end $$;

create or replace function public.musicflow_creator_storage_can_read(p_path text)
returns boolean language sql stable security definer set search_path = ''
as $$
 select auth.uid() is not null and exists(
   select 1 from musicflow_private.creator_upload_slots f
   join musicflow_private.creator_drafts d on d.id=f.draft_id
   where f.object_path=p_path and f.state='uploaded'
     and (
       f.creator_id=auth.uid()
       or (
         d.status in ('submitted','metadata_approved')
         and public.musicflow_can_review_metadata()
       )
     )
 );
$$;

-- Authenticated only; SQL functions still verify auth.uid() on every call.
revoke all on function public.musicflow_can_review_metadata() from public,anon;
revoke all on function public.musicflow_pending_drafts() from public,anon;
revoke all on function public.musicflow_review_draft(uuid,boolean,text) from public,anon;
revoke all on function public.musicflow_creator_upload_list(uuid) from public,anon;
revoke all on function public.musicflow_creator_storage_can_read(text) from public,anon;
grant execute on function public.musicflow_can_review_metadata() to authenticated;
grant execute on function public.musicflow_pending_drafts() to authenticated;
grant execute on function public.musicflow_review_draft(uuid,boolean,text) to authenticated;
grant execute on function public.musicflow_creator_upload_list(uuid) to authenticated;
grant execute on function public.musicflow_creator_storage_can_read(text) to authenticated;

commit;
