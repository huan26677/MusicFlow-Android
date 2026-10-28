#!/usr/bin/env python3
"""Admin-only, two-step upload for MusicFlow licensed audio.

Requires the Supabase secret/service-role key in the process environment,
NEVER in an Android app, repository, command argument, or log.

stage   => upload to a private Storage bucket and create an unpublished draft.
publish => manually attest streaming rights, verify the staged file hash,
           copy it to a public bucket and publish the catalog row.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import sys
import urllib.error
import urllib.parse
import urllib.request
from uuid import UUID, uuid4

PROJECT_URL = "https://vzvxpbwrftlnyvuhfoqn.supabase.co"
PRIVATE_BUCKET = "musicflow-private-drafts"
PUBLIC_BUCKET = "musicflow-public-audio"
MAX_BYTES = 50 * 1024 * 1024
MIME_TYPES = {
    ".mp3": "audio/mpeg",
    ".m4a": "audio/mp4",
    ".flac": "audio/flac",
    ".ogg": "audio/ogg",
    ".wav": "audio/wav",
}


def validate_audio(path: Path) -> tuple[bytes, str, str]:
    """Validate supported format, magic bytes, and size before network use."""
    if not path.is_file():
        raise ValueError("File nhạc không tồn tại.")
    extension = path.suffix.lower()
    mime = MIME_TYPES.get(extension)
    if mime is None:
        raise ValueError("Chỉ hỗ trợ MP3, M4A, FLAC, OGG hoặc WAV.")
    size = path.stat().st_size
    if not 1 <= size <= MAX_BYTES:
        raise ValueError("File phải có kích thước từ 1 byte đến 50 MiB.")
    data = path.read_bytes()
    header = data[:16]
    matches = {
        ".mp3": header.startswith(b"ID3")
        or (len(header) > 1 and header[0] == 0xFF and (header[1] & 0xE0) == 0xE0),
        ".m4a": len(header) >= 8 and header[4:8] == b"ftyp",
        ".flac": header.startswith(b"fLaC"),
        ".ogg": header.startswith(b"OggS"),
        ".wav": header.startswith(b"RIFF") and header[8:12] == b"WAVE",
    }
    if not matches[extension]:
        raise ValueError("Phần đầu file không khớp định dạng âm thanh đã chọn.")
    return data, mime, hashlib.sha256(data).hexdigest()


def clean_text(text: str, field: str, limit: int) -> str:
    value = text.strip()
    if not 1 <= len(value) <= limit:
        raise ValueError(f"{field} phải có từ 1 đến {limit} ký tự.")
    return value


class SupabaseAdmin:
    def __init__(self, key: str):
        if not key.startswith("sb_secret_"):
            raise ValueError(
                "Chỉ hỗ trợ khóa sb_secret_ của Supabase dành cho admin."
            )
        self.key = key

    def request(
        self,
        method: str,
        path: str,
        payload: bytes | None = None,
        content_type: str | None = None,
        extra_headers: dict[str, str] | None = None,
    ) -> bytes:
        headers = {
            "apikey": self.key,
            "Accept": "application/json",
        }
        if content_type:
            headers["Content-Type"] = content_type
        if extra_headers:
            headers.update(extra_headers)
        request = urllib.request.Request(
            PROJECT_URL + path,
            data=payload,
            headers=headers,
            method=method,
        )
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                return response.read(MAX_BYTES + 1)
        except urllib.error.HTTPError as error:
            # Never print request headers or secrets.
            details = error.read(600).decode("utf-8", errors="replace")
            raise RuntimeError(f"Supabase HTTP {error.code}: {details}") from None
        except urllib.error.URLError as error:
            raise RuntimeError(f"Không kết nối được Supabase: {error.reason}") from None

    def json(
        self,
        method: str,
        path: str,
        document: dict | None = None,
        representation: bool = False,
    ):
        data = None if document is None else json.dumps(document).encode("utf-8")
        headers = {"Prefer": "return=representation"} if representation else None
        raw = self.request(
            method, path, data,
            "application/json" if data is not None else None,
            headers,
        )
        return json.loads(raw) if raw else None

    def storage_upload(self, bucket: str, object_key: str, data: bytes, mime: str) -> None:
        path = "/storage/v1/object/" + urllib.parse.quote(
            f"{bucket}/{object_key}", safe="/"
        )
        self.request(
            "POST", path, data, mime, {"x-upsert": "true"}
        )

    def storage_download(self, bucket: str, object_key: str) -> bytes:
        path = "/storage/v1/object/authenticated/" + urllib.parse.quote(
            f"{bucket}/{object_key}", safe="/"
        )
        data = self.request("GET", path)
        if len(data) > MAX_BYTES:
            raise ValueError("File trong Storage vượt giới hạn 50 MiB.")
        return data


def stage(api: SupabaseAdmin, args: argparse.Namespace) -> None:
    file_path = Path(args.file)
    audio, mime, sha256_hex = validate_audio(file_path)
    title = clean_text(args.title, "Tên bài", 240)
    artist = clean_text(args.artist, "Nghệ sĩ", 180)
    album = clean_text(args.album, "Album", 180) if args.album else None
    if not 0 <= args.duration_ms <= 86_400_000:
        raise ValueError("Thời lượng phải từ 0 đến 86400000 ms.")

    track_id = str(uuid4())
    storage_key = f"drafts/{track_id}/{uuid4().hex}{file_path.suffix.lower()}"
    # The private bucket does not provide public playback URLs.
    api.storage_upload(PRIVATE_BUCKET, storage_key, audio, mime)
    api.json("POST", "/rest/v1/music_tracks", {
        "id": track_id,
        "title": title,
        "artist": artist,
        "album": album,
        "duration_ms": args.duration_ms,
        "is_published": False,
        "rights_confirmed": False,
    })
    api.json("POST", "/rest/v1/music_uploads", {
        "track_id": track_id,
        "staging_path": storage_key,
        "original_filename": file_path.name[:240],
        "mime_type": mime,
        "content_sha256": sha256_hex,
        "file_bytes": len(audio),
    })
    print("TẠO BẢN NHÁP RIÊNG TƯ THÀNH CÔNG")
    print(f"Track ID: {track_id}")
    print("Chưa xuất bản, chưa thể phát công khai.")


def load_one(api: SupabaseAdmin, table: str, track_id: str) -> dict:
    if table not in {"music_tracks", "music_uploads"}:
        raise ValueError("Tên bảng không được hỗ trợ.")
    query = urllib.parse.urlencode({
        "select": "*", "track_id" if table == "music_uploads" else "id":
        f"eq.{track_id}",
    })
    rows = api.json("GET", f"/rest/v1/{table}?{query}")
    if not isinstance(rows, list) or len(rows) != 1:
        raise ValueError(f"Không tìm thấy bản ghi {table} cho Track ID đã nhập.")
    return rows[0]


def publish(api: SupabaseAdmin, args: argparse.Namespace) -> None:
    if not args.confirm_rights:
        raise ValueError(
            "Phải xác nhận --confirm-rights rằng bạn có quyền phát công khai."
        )
    track_id = str(UUID(args.track_id))
    license_reference = clean_text(
        args.license_reference, "Tài liệu tham chiếu bản quyền", 1000
    )
    reviewer = clean_text(args.reviewer, "Người xác nhận", 160)

    track = load_one(api, "music_tracks", track_id)
    evidence = load_one(api, "music_uploads", track_id)
    if track.get("is_published"):
        raise ValueError("Bài đã công khai; không thực hiện xuất bản lần hai.")
    if evidence.get("published_at") is not None:
        raise ValueError("Bản ghi upload đã được đánh dấu xuất bản.")

    content = api.storage_download(PRIVATE_BUCKET, evidence["staging_path"])
    if hashlib.sha256(content).hexdigest() != evidence["content_sha256"]:
        raise ValueError("SHA-256 không trùng: từ chối xuất bản.")
    if len(content) != evidence["file_bytes"]:
        raise ValueError("Kích thước file không trùng: từ chối xuất bản.")

    extension = Path(evidence["original_filename"]).suffix.lower()
    if extension not in MIME_TYPES or MIME_TYPES[extension] != evidence["mime_type"]:
        raise ValueError("Định dạng file trong bản nháp không hợp lệ.")
    public_path = (
        f"tracks/{track_id}/{evidence['content_sha256'][:24]}{extension}"
    )
    # Only publish with explicit rights attestation. Public URLs are permanent.
    api.storage_upload(PUBLIC_BUCKET, public_path, content, evidence["mime_type"])
    url = (
        f"{PROJECT_URL}/storage/v1/object/public/{PUBLIC_BUCKET}/"
        + urllib.parse.quote(public_path, safe="/")
    )
    api.json(
        "POST",
        "/rest/v1/rpc/musicflow_publish_licensed_track",
        {
            "p_track_id": track_id,
            "p_public_path": public_path,
            "p_public_url": url,
            "p_sha256": evidence["content_sha256"],
            "p_rights_reference": license_reference,
            "p_reviewer": reviewer,
        },
    )
    print("XUẤT BẢN BÀI HÁT ĐÃ ĐƯỢC XÁC NHẬN QUYỀN")
    print(f"Track ID: {track_id}")
    print("Bài hát sẽ xuất hiện trong danh mục của MusicFlow.")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    draft = commands.add_parser("stage", help="Tạo bản nháp riêng tư")
    draft.add_argument("--file", required=True)
    draft.add_argument("--title", required=True)
    draft.add_argument("--artist", required=True)
    draft.add_argument("--album", default="")
    draft.add_argument("--duration-ms", type=int, default=0)

    approval = commands.add_parser("publish", help="Xác nhận quyền và xuất bản")
    approval.add_argument("--track-id", required=True)
    approval.add_argument("--license-reference", required=True)
    approval.add_argument("--reviewer", required=True)
    approval.add_argument("--confirm-rights", action="store_true")
    args = parser.parse_args()

    secret = os.environ.get("MUSICFLOW_SUPABASE_SECRET_KEY", "").strip()
    if not secret:
        parser.error(
            "Chưa đặt biến môi trường MUSICFLOW_SUPABASE_SECRET_KEY. "
            "Không dùng publishable key; không nhập khóa vào lệnh hoặc Git."
        )
    try:
        api = SupabaseAdmin(secret)
        if args.command == "stage":
            stage(api, args)
        else:
            publish(api, args)
    except (ValueError, RuntimeError, OSError) as error:
        print(f"LỖI: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    if hasattr(sys.stderr, "reconfigure"):
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    sys.exit(main())
