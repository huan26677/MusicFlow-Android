-- Recorded Supabase migration: musicflow_licensed_publish_review_v2
-- Applied 2026-10-29 to project vzvxpbwrftlnyvuhfoqn.
-- This is a reference copy for disaster recovery / code review.
-- DO NOT automatically rerun on an already migrated project.
-- Only catalog-hosted ONLINE audio is subject to this permission workflow.

create or replace function public.musicflow_publish_licensed_track_v2(
  p_track_id uuid,
  p_public_path text,
  p_public_url text,
  p_sha256 text,
  p_rights_reference text,
  p_reviewer text,
  p_license_code text,
  p_license_url text,
  p_attribution_text text,
  p_source_url text,
  p_streaming_allowed boolean,
  p_download_allowed boolean,
  p_commercial_use_allowed boolean
)
returns void
language plpgsql
security invoker
set search_path = ''
as $$
declare
  receipt public.music_uploads%rowtype;
  draft public.music_tracks%rowtype;
  canonical_url text;
begin
  select * into receipt from public.music_uploads
    where track_id = p_track_id for update;
  if not found then raise exception 'Draft receipt missing'; end if;
  if receipt.published_at is not null or receipt.published_path is not null then
    raise exception 'Receipt already published';
  end if;

  select * into draft from public.music_tracks
    where id = p_track_id for update;
  if not found or draft.is_published then
    raise exception 'Missing draft or track already published';
  end if;
  if receipt.content_sha256 is distinct from p_sha256
     or p_sha256 !~ '^[a-f0-9]{64}$' then
    raise exception 'Audio SHA-256 mismatch';
  end if;
  if p_public_path !~ ('^tracks/' || p_track_id::text || '/' ||
                        left(p_sha256,24) || '[.](mp3|m4a|flac|ogg|wav)$') then
    raise exception 'Unexpected public audio path or hash prefix';
  end if;
  canonical_url := 'https://vzvxpbwrftlnyvuhfoqn.supabase.co/storage/v1/object/public/musicflow-public-audio/' || p_public_path;
  if p_public_url is distinct from canonical_url then
    raise exception 'Unexpected public audio URL';
  end if;
  if p_streaming_allowed is distinct from true then
    raise exception 'Public playback needs explicit streaming authorization';
  end if;
  if length(btrim(coalesce(p_rights_reference, ''))) < 8
     or length(p_rights_reference) > 1000
     or length(btrim(coalesce(p_reviewer, ''))) < 2
     or length(p_reviewer) > 160 then
    raise exception 'Review evidence and reviewer required';
  end if;
  if p_license_code not in ('CC0-1.0','CC-BY-4.0','DIRECT') then
    raise exception 'License code not supported';
  end if;
  if length(btrim(coalesce(p_attribution_text,''))) < 3
     or length(p_attribution_text) > 500 then
    raise exception 'Public attribution is required';
  end if;
  if p_source_url is null
     or p_source_url !~ '^https://[A-Za-z0-9][A-Za-z0-9.-]*(:[0-9]{1,5})?([/?][^[:space:]#@]*)?$'
     or length(p_source_url) > 500 then
    raise exception 'Valid public HTTPS source URL is required';
  end if;
  if p_license_code = 'CC0-1.0' and p_license_url is distinct from
     'https://creativecommons.org/publicdomain/zero/1.0/' then
    raise exception 'CC0 license URL must match its canonical link';
  end if;
  if p_license_code = 'CC-BY-4.0' and p_license_url is distinct from
     'https://creativecommons.org/licenses/by/4.0/' then
    raise exception 'CC BY license URL must match its canonical link';
  end if;
  if p_license_code = 'DIRECT' and p_license_url is not null and
     (p_license_url !~ '^https://[A-Za-z0-9][A-Za-z0-9.-]*(:[0-9]{1,5})?([/?][^[:space:]#@]*)?$'
      or length(p_license_url) > 500) then
    raise exception 'Direct license link must be HTTPS if provided';
  end if;

  update public.music_tracks set
    stream_url = p_public_url,
    rights_confirmed = true,
    is_published = true,
    license_code = p_license_code,
    license_url = p_license_url,
    attribution_text = btrim(p_attribution_text),
    source_url = p_source_url,
    streaming_allowed = true,
    download_allowed = coalesce(p_download_allowed,false),
    commercial_use_allowed = coalesce(p_commercial_use_allowed,false),
    rights_verified_at = now()
  where id = p_track_id and is_published = false;
  if not found then raise exception 'Draft already published'; end if;

  update public.music_uploads set
    published_path = p_public_path,
    rights_reference = btrim(p_rights_reference),
    rights_confirmed_by = btrim(p_reviewer),
    rights_reviewed_at = now(),
    published_at = now()
  where track_id = p_track_id;
end;
$$;

revoke all on function public.musicflow_publish_licensed_track_v2(
  uuid,text,text,text,text,text,text,text,text,text,boolean,boolean,boolean
) from public, anon, authenticated;
grant execute on function public.musicflow_publish_licensed_track_v2(
  uuid,text,text,text,text,text,text,text,text,text,boolean,boolean,boolean
) to service_role;

-- Legacy RPC remains in the database for audit, but no application/admin
-- role can call it: it did not set the newer rights metadata.
revoke all on function public.musicflow_publish_licensed_track(
  uuid,text,text,text,text,text
) from service_role, public, anon, authenticated;
