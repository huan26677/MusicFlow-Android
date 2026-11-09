-- MusicFlow Community v1 — additive, idempotent, authenticated writing only.
-- Public read is restricted to published/rights-confirmed music_tracks.
-- Use Supabase SQL Editor or a reviewed migration; never ship privileged keys.
begin;

create table if not exists public.music_track_ratings (
  track_id uuid not null references public.music_tracks(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  stars smallint not null check (stars between 1 and 5),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  primary key(track_id,user_id)
);
create index if not exists music_track_ratings_track_idx
  on public.music_track_ratings(track_id);
alter table public.music_track_ratings enable row level security;

create table if not exists public.music_track_comments (
  id uuid primary key default gen_random_uuid(),
  track_id uuid not null references public.music_tracks(id) on delete cascade,
  author_id uuid not null references auth.users(id) on delete cascade,
  body text not null check (char_length(btrim(body)) between 0 and 500),
  status text not null default 'visible'
    check(status in ('visible','hidden','deleted')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check ((status = 'deleted' and body = '') or
         (status in ('visible','hidden') and char_length(btrim(body)) between 1 and 500))
);
create index if not exists music_track_comments_page_idx
  on public.music_track_comments(track_id,created_at desc,id desc)
  where status = 'visible';
alter table public.music_track_comments enable row level security;

create table if not exists public.music_comment_reports (
  id uuid primary key default gen_random_uuid(),
  comment_id uuid not null references public.music_track_comments(id) on delete cascade,
  reporter_id uuid not null references auth.users(id) on delete cascade,
  reason text not null check(char_length(btrim(reason)) between 5 and 300),
  status text not null default 'pending'
    check(status in ('pending','handled')),
  created_at timestamptz not null default now(),
  unique(comment_id,reporter_id)
);
create index if not exists music_comment_reports_queue_idx
  on public.music_comment_reports(status,created_at desc);
alter table public.music_comment_reports enable row level security;

-- Moderator roles are not automatically granted access to community data.
create table if not exists musicflow_private.community_moderator_grants (
  user_id uuid primary key references auth.users(id) on delete cascade,
  granted_by uuid references auth.users(id) on delete set null,
  created_at timestamptz not null default now()
);
alter table musicflow_private.community_moderator_grants enable row level security;

revoke all on public.music_track_ratings, public.music_track_comments,
  public.music_comment_reports from public, anon, authenticated;
revoke all on musicflow_private.community_moderator_grants from public, anon, authenticated;

create or replace function public.musicflow_community_read(
  p_track uuid, p_offset integer default 0
) returns jsonb language plpgsql stable security definer set search_path = ''
as $$
declare result jsonb; viewer uuid := auth.uid();
begin
  if p_track is null or p_offset is null or p_offset < 0 or p_offset > 1000 or
    not exists(select 1 from public.music_tracks where id=p_track
      and is_published and rights_confirmed) then
    raise exception 'Bài hát không có trong danh mục công khai' using errcode='22023';
  end if;
  select jsonb_build_object(
    'rating_count',(select count(*) from public.music_track_ratings where track_id=p_track),
    'average_stars',(select coalesce(round(avg(stars)::numeric,1),0)
                     from public.music_track_ratings where track_id=p_track),
    'my_stars',(select stars from public.music_track_ratings
               where track_id=p_track and user_id=viewer),
    'comment_count',(select count(*) from public.music_track_comments
                     where track_id=p_track and status='visible'),
    'comments',coalesce((
      select jsonb_agg(to_jsonb(page_rows) order by page_rows.created_at desc,page_rows.id desc)
      from (
        select c.id,c.body,c.created_at,
          coalesce(nullif(trim(p.display_name),''),'Người nghe') as display_name,
          (c.author_id=viewer) as mine
        from public.music_track_comments c
        left join public.account_profiles p on p.id=c.author_id
        where c.track_id=p_track and c.status='visible'
        order by c.created_at desc,c.id desc limit 20 offset p_offset
      ) page_rows
    ),'[]'::jsonb)
  ) into result;
  return result;
end $$;

create or replace function public.musicflow_community_rate(
  p_track uuid,p_stars integer
) returns boolean language plpgsql security definer set search_path = ''
as $$
declare uid uuid := auth.uid();
begin
  if uid is null then raise exception 'Đăng nhập để đánh giá' using errcode='28000'; end if;
  if p_stars is null or p_stars not between 1 and 5 then
    raise exception 'Chọn 1 đến 5 sao' using errcode='22023';
  end if;
  if not exists(select 1 from public.music_tracks where id=p_track
    and is_published and rights_confirmed) then
    raise exception 'Bài hát chưa công khai' using errcode='22023';
  end if;
  insert into public.music_track_ratings(track_id,user_id,stars)
    values(p_track,uid,p_stars)
    on conflict (track_id,user_id) do update
      set stars=excluded.stars,updated_at=now();
  return true;
end $$;

create or replace function public.musicflow_community_comment_create(
  p_track uuid,p_body text
) returns uuid language plpgsql security definer set search_path = ''
as $$
declare uid uuid:=auth.uid(); new_id uuid; clean_body text:=btrim(coalesce(p_body,''));
begin
  if uid is null then raise exception 'Đăng nhập để bình luận' using errcode='28000'; end if;
  if char_length(clean_body) not between 1 and 500 then
    raise exception 'Bình luận phải có 1–500 ký tự' using errcode='22023';
  end if;
  if not exists(select 1 from public.music_tracks where id=p_track
    and is_published and rights_confirmed) then
    raise exception 'Bài hát chưa công khai' using errcode='22023';
  end if;
  if exists(select 1 from public.music_track_comments
    where author_id=uid and created_at>now()-interval '10 seconds') then
    raise exception 'Bạn đang gửi quá nhanh, hãy thử lại' using errcode='22023';
  end if;
  insert into public.music_track_comments(track_id,author_id,body)
    values(p_track,uid,clean_body) returning id into new_id;
  return new_id;
end $$;

create or replace function public.musicflow_community_comment_edit(
  p_comment uuid,p_body text
) returns boolean language plpgsql security definer set search_path = ''
as $$
declare uid uuid:=auth.uid(); clean_body text:=btrim(coalesce(p_body,''));
begin
  if uid is null then raise exception 'Đăng nhập để sửa bình luận' using errcode='28000'; end if;
  if char_length(clean_body) not between 1 and 500 then
    raise exception 'Bình luận phải có 1–500 ký tự' using errcode='22023';
  end if;
  update public.music_track_comments set body=clean_body,updated_at=now()
    where id=p_comment and author_id=uid and status='visible'
    and exists(select 1 from public.music_tracks t
      where t.id=track_id and t.is_published and t.rights_confirmed);
  if not found then raise exception 'Không có quyền sửa bình luận' using errcode='42501'; end if;
  return true;
end $$;

create or replace function public.musicflow_community_comment_delete(
  p_comment uuid
) returns boolean language plpgsql security definer set search_path = ''
as $$
declare uid uuid:=auth.uid();
begin
  if uid is null then raise exception 'Đăng nhập để xóa bình luận' using errcode='28000'; end if;
  update public.music_track_comments set status='deleted',body='',updated_at=now()
    where id=p_comment and author_id=uid and status='visible';
  if not found then raise exception 'Không có quyền xóa bình luận' using errcode='42501'; end if;
  return true;
end $$;

create or replace function public.musicflow_community_report(
  p_comment uuid,p_reason text
) returns boolean language plpgsql security definer set search_path = ''
as $$
declare uid uuid:=auth.uid(); clean_reason text:=btrim(coalesce(p_reason,''));
begin
  if uid is null then raise exception 'Đăng nhập để báo cáo' using errcode='28000'; end if;
  if char_length(clean_reason) not between 5 and 300 then
    raise exception 'Lý do cần 5–300 ký tự' using errcode='22023';
  end if;
  if not exists(select 1 from public.music_track_comments c
    join public.music_tracks t on t.id=c.track_id
    where c.id=p_comment and c.status='visible'
      and c.author_id<>uid and t.is_published and t.rights_confirmed) then
    raise exception 'Không thể báo cáo bình luận này' using errcode='42501';
  end if;
  insert into public.music_comment_reports(comment_id,reporter_id,reason)
    values(p_comment,uid,clean_reason)
    on conflict (comment_id,reporter_id) do nothing;
  if not found then raise exception 'Bạn đã báo cáo bình luận này' using errcode='23505'; end if;
  return true;
end $$;

create or replace function public.musicflow_community_can_moderate()
returns boolean language sql stable security definer set search_path = ''
as $$
  select auth.uid() is not null and (
    exists(select 1 from musicflow_private.user_roles r
      where r.user_id=auth.uid() and r.role='owner')
    or (exists(select 1 from musicflow_private.user_roles r
      where r.user_id=auth.uid() and r.role='platform_admin')
      and exists(select 1 from musicflow_private.admin_permissions p
        where p.user_id=auth.uid() and p.permission='moderate_catalog'))
    or (exists(select 1 from musicflow_private.user_roles r
      where r.user_id=auth.uid() and r.role='moderator')
      and exists(select 1 from musicflow_private.community_moderator_grants g
        where g.user_id=auth.uid()))
  );
$$;

create or replace function public.musicflow_community_reports()
returns jsonb language plpgsql stable security definer set search_path = ''
as $$
begin
  if not public.musicflow_community_can_moderate() then
    raise exception 'Không có quyền kiểm duyệt' using errcode='42501';
  end if;
  return coalesce((
    select jsonb_agg(to_jsonb(rows_data) order by rows_data.created_at desc)
    from (
      select r.id,r.comment_id,r.reason,r.created_at,c.body,c.status,
        t.title as track_title
      from public.music_comment_reports r
      join public.music_track_comments c on c.id=r.comment_id
      join public.music_tracks t on t.id=c.track_id
      where r.status='pending' order by r.created_at desc limit 40
    ) rows_data
  ),'[]'::jsonb);
end $$;

create or replace function public.musicflow_community_moderate(
  p_comment uuid,p_hide boolean,p_reason text
) returns boolean language plpgsql security definer set search_path = ''
as $$
declare uid uuid:=auth.uid(); why text:=btrim(coalesce(p_reason,''));
begin
  if not public.musicflow_community_can_moderate() then
    raise exception 'Không có quyền kiểm duyệt' using errcode='42501';
  end if;
  if p_hide is null or char_length(why) not between 5 and 300 then
    raise exception 'Quyết định và lý do 5–300 ký tự là bắt buộc' using errcode='22023';
  end if;
  if not exists(select 1 from public.music_track_comments
    where id=p_comment and author_id<>uid and status<>'deleted') then
    raise exception 'Không thể kiểm duyệt bình luận này' using errcode='42501';
  end if;
  -- Hidden text is private (tables have no client grants); may be restored.
  -- Deletion, in contrast, permanently blanks its body.
  update public.music_track_comments
    set status=case when p_hide then 'hidden' else 'visible' end,
        updated_at=now()
    where id=p_comment;
  update public.music_comment_reports set status='handled' where comment_id=p_comment;
  insert into musicflow_private.role_audit(actor_id,action,details)
    values(uid,case when p_hide then 'community_comment_hidden' else 'community_report_dismissed' end,
      jsonb_build_object('comment',p_comment,'reason',why));
  return true;
end $$;

create or replace function public.musicflow_community_grant_moderator(
  p_user uuid,p_grant boolean,p_reason text
) returns boolean language plpgsql security definer set search_path = ''
as $$
declare uid uuid:=auth.uid(); why text:=btrim(coalesce(p_reason,''));
begin
  if uid is null or not exists(select 1 from musicflow_private.user_roles
    where user_id=uid and role='owner') then
    raise exception 'Chỉ Owner được cấp quyền kiểm duyệt cộng đồng' using errcode='42501';
  end if;
  if p_grant is null or char_length(why) not between 5 and 300 then
    raise exception 'Lý do 5–300 ký tự là bắt buộc' using errcode='22023';
  end if;
  if not exists(select 1 from musicflow_private.user_roles
    where user_id=p_user and role='moderator') then
    raise exception 'Người được cấp phải có vai trò Moderator' using errcode='22023';
  end if;
  if p_grant then
    insert into musicflow_private.community_moderator_grants(user_id,granted_by)
      values(p_user,uid) on conflict(user_id) do update
      set granted_by=excluded.granted_by,created_at=now();
  else
    delete from musicflow_private.community_moderator_grants where user_id=p_user;
  end if;
  insert into musicflow_private.role_audit(actor_id,target_id,action,details)
    values(uid,p_user,'community_moderator_access',
      jsonb_build_object('granted',p_grant,'reason',why));
  return true;
end $$;

revoke all on function public.musicflow_community_read(uuid,integer) from public,anon,authenticated;
revoke all on function public.musicflow_community_rate(uuid,integer) from public,anon,authenticated;
revoke all on function public.musicflow_community_comment_create(uuid,text) from public,anon,authenticated;
revoke all on function public.musicflow_community_comment_edit(uuid,text) from public,anon,authenticated;
revoke all on function public.musicflow_community_comment_delete(uuid) from public,anon,authenticated;
revoke all on function public.musicflow_community_report(uuid,text) from public,anon,authenticated;
revoke all on function public.musicflow_community_can_moderate() from public,anon,authenticated;
revoke all on function public.musicflow_community_reports() from public,anon,authenticated;
revoke all on function public.musicflow_community_moderate(uuid,boolean,text) from public,anon,authenticated;
revoke all on function public.musicflow_community_grant_moderator(uuid,boolean,text) from public,anon,authenticated;

grant execute on function public.musicflow_community_read(uuid,integer) to anon,authenticated;
grant execute on function public.musicflow_community_rate(uuid,integer),
  public.musicflow_community_comment_create(uuid,text),
  public.musicflow_community_comment_edit(uuid,text),
  public.musicflow_community_comment_delete(uuid),
  public.musicflow_community_report(uuid,text),
  public.musicflow_community_reports(),
  public.musicflow_community_moderate(uuid,boolean,text),
  public.musicflow_community_grant_moderator(uuid,boolean,text)
  to authenticated;
commit;
