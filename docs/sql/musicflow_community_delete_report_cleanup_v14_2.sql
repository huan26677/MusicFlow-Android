-- MusicFlow Community v14.2 — deleting a comment resolves any pending reports.
-- The author controls the comment, but cannot perform arbitrary moderation.
-- This prevents pending reports on permanently deleted, blanked comments from
-- remaining in the staff queue with no valid moderation action.
create or replace function public.musicflow_community_comment_delete(
  p_comment uuid
) returns boolean language plpgsql security definer set search_path = ''
as $$
declare uid uuid := auth.uid();
begin
  if uid is null then
    raise exception 'Đăng nhập để xóa bình luận' using errcode='28000';
  end if;
  update public.music_track_comments
    set status='deleted', body='', updated_at=now()
    where id=p_comment and author_id=uid and status='visible';
  if not found then
    raise exception 'Không có quyền xóa bình luận' using errcode='42501';
  end if;

  -- A deleted comment can never be restored by community moderation.
  -- Close pending reports atomically as resolved by the author's deletion.
  update public.music_comment_reports
    set status='handled'
    where comment_id=p_comment and status='pending';
  return true;
end $$;

revoke all on function public.musicflow_community_comment_delete(uuid)
  from public, anon;
grant execute on function public.musicflow_community_comment_delete(uuid)
  to authenticated;
