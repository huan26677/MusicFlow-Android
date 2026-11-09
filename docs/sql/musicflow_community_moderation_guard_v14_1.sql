-- MusicFlow Community v14.1 — staff report dismissal must never unhide a
-- previously hidden comment. Applied additively after musicflow_community_v1.
-- SECURITY DEFINER is retained only because the raw tables are fully denied
-- by grants + RLS. Always check role and caller identity within the function.
create or replace function public.musicflow_community_moderate(
  p_comment uuid, p_hide boolean, p_reason text
) returns boolean language plpgsql security definer set search_path = ''
as $$
declare
  uid uuid := auth.uid();
  why text := btrim(coalesce(p_reason, ''));
begin
  if uid is null or not public.musicflow_community_can_moderate() then
    raise exception 'Không có quyền kiểm duyệt' using errcode='42501';
  end if;
  if p_hide is null or char_length(why) not between 5 and 300 then
    raise exception 'Quyết định và lý do 5–300 ký tự là bắt buộc'
      using errcode='22023';
  end if;
  if not exists (
    select 1 from public.music_track_comments c
    where c.id=p_comment and c.author_id<>uid and c.status<>'deleted'
      and exists (
        select 1 from public.music_comment_reports r
        where r.comment_id=c.id and r.status='pending'
      )
  ) then
    raise exception 'Không có báo cáo hợp lệ để xử lý'
      using errcode='42501';
  end if;

  -- The UI action "Bỏ qua báo cáo" only handles reports.
  -- Restoring previously hidden content requires an explicit, separate action;
  -- it must never occur as a side effect of dismissing a report.
  if p_hide then
    update public.music_track_comments
      set status='hidden', updated_at=now()
      where id=p_comment and status='visible';
  end if;

  update public.music_comment_reports
    set status='handled'
    where comment_id=p_comment and status='pending';
  insert into musicflow_private.role_audit(actor_id, action, details)
    values(uid,
      case when p_hide then 'community_comment_hidden'
           else 'community_report_dismissed' end,
      jsonb_build_object('comment',p_comment,'reason',why));
  return true;
end $$;

revoke all on function public.musicflow_community_moderate(uuid,boolean,text)
  from public, anon;
grant execute on function public.musicflow_community_moderate(uuid,boolean,text)
  to authenticated;
