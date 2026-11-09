-- Community SQL integration test. TEST FIXTURES ARE ALWAYS ROLLED BACK.
-- Only run in a trusted QA session; no emails or published tracks persist.
-- This simulates auth.uid() in PostgreSQL. It is NOT a real JWT/HTTP E2E test.
begin;
do $community_qa$
declare
  staff_uid uuid := gen_random_uuid();
  author_uid uuid := gen_random_uuid();
  reporter_uid uuid := gen_random_uuid();
  reporter2_uid uuid := gen_random_uuid();
  track_uuid uuid;
  comment_uuid uuid;
  deleted_uuid uuid;
  rejected boolean := false;
  count_value integer;
begin
  insert into auth.users(id,instance_id,email,aud,role,created_at,updated_at)
  values
    (staff_uid,'00000000-0000-0000-0000-000000000000','community-qa-' || staff_uid || '@example.invalid','authenticated','authenticated',now(),now()),
    (author_uid,'00000000-0000-0000-0000-000000000000','community-qa-' || author_uid || '@example.invalid','authenticated','authenticated',now(),now()),
    (reporter_uid,'00000000-0000-0000-0000-000000000000','community-qa-' || reporter_uid || '@example.invalid','authenticated','authenticated',now(),now()),
    (reporter2_uid,'00000000-0000-0000-0000-000000000000','community-qa-' || reporter2_uid || '@example.invalid','authenticated','authenticated',now(),now());

  -- The real project allows one Owner; use a throwaway scoped PlatformAdmin.
  insert into musicflow_private.user_roles(user_id,role)
    values(staff_uid,'platform_admin');
  insert into musicflow_private.admin_permissions(user_id,permission)
    values(staff_uid,'moderate_catalog');
  insert into public.music_tracks(
    title,artist,duration_ms,is_published,rights_confirmed,stream_url,license_code
  )
    values('__qa_community_v14_1_rollback__','QA artist',1000,true,true,
      'https://example.invalid/qa-not-a-real-audio-file','CC0-1.0')
    returning id into track_uuid;

  perform set_config('request.jwt.claim.sub', author_uid::text, true);
  perform public.musicflow_community_rate(track_uuid,4);
  perform public.musicflow_community_rate(track_uuid,2);
  select count(*) into count_value from public.music_track_ratings
    where track_id=track_uuid and user_id=author_uid and stars=2;
  if count_value<>1 then raise exception 'QA_RATING_UNIQUE_FAILED'; end if;
  comment_uuid := public.musicflow_community_comment_create(track_uuid,'Bình luận QA');
  if (public.musicflow_community_read(track_uuid,0)->>'comment_count')::integer<>1 then
    raise exception 'QA_PUBLIC_READ_FAILED';
  end if;

  perform set_config('request.jwt.claim.sub',reporter_uid::text,true);
  begin
    perform public.musicflow_community_comment_edit(comment_uuid,'Không phải tác giả');
  exception when insufficient_privilege then rejected:=true;
  end;
  if not rejected then raise exception 'QA_FOREIGN_EDIT_AUTH_FAILED'; end if;
  perform public.musicflow_community_report(comment_uuid,'Báo cáo spam QA');
  rejected:=false;
  begin
    perform public.musicflow_community_moderate(comment_uuid,true,'Thử kiểm duyệt');
  exception when insufficient_privilege then rejected:=true;
  end;
  if not rejected then raise exception 'QA_NON_ADMIN_MODERATION_AUTH_FAILED'; end if;

  perform set_config('request.jwt.claim.sub',staff_uid::text,true);
  perform public.musicflow_community_moderate(comment_uuid,true,'Ẩn bình luận QA');
  if not exists(select 1 from public.music_track_comments
    where id=comment_uuid and status='hidden') then
    raise exception 'QA_HIDE_FAILED';
  end if;
  insert into public.music_comment_reports(comment_id,reporter_id,reason,status)
    values(comment_uuid,reporter2_uid,'Báo cáo còn tồn QA','pending');
  perform public.musicflow_community_moderate(comment_uuid,false,'Bỏ qua báo cáo QA');
  if not exists(select 1 from public.music_track_comments
    where id=comment_uuid and status='hidden') then
    raise exception 'QA_DISMISS_UNHID_HIDDEN_COMMENT';
  end if;
  if exists(select 1 from public.music_comment_reports
    where comment_id=comment_uuid and status='pending') then
    raise exception 'QA_DISMISS_DID_NOT_HANDLE_REPORTS';
  end if;
  rejected:=false;
  begin
    perform public.musicflow_community_moderate(comment_uuid,false,'Báo cáo đã xong');
  exception when insufficient_privilege then rejected:=true;
  end;
  if not rejected then raise exception 'QA_PENDING_REPORT_GUARD_FAILED'; end if;

  -- Author removal must not leave an impossible-to-moderate pending report.
  insert into public.music_track_comments(track_id,author_id,body)
    values(track_uuid,author_uid,'Bình luận khác đang có báo cáo')
    returning id into deleted_uuid;
  perform set_config('request.jwt.claim.sub',reporter2_uid::text,true);
  perform public.musicflow_community_report(deleted_uuid,'Báo cáo trước khi xóa');
  perform set_config('request.jwt.claim.sub',author_uid::text,true);
  perform public.musicflow_community_comment_delete(deleted_uuid);
  if not exists(select 1 from public.music_track_comments
    where id=deleted_uuid and status='deleted' and body='') then
    raise exception 'QA_AUTHOR_DELETE_FAILED';
  end if;
  if exists(select 1 from public.music_comment_reports
    where comment_id=deleted_uuid and status='pending') then
    raise exception 'QA_DELETED_COMMENT_REPORT_STILL_PENDING';
  end if;
  raise notice 'COMMUNITY_V14_1_ROLLBACK_QA_ALL_GUARDS_PASS';
end $community_qa$;
rollback;
