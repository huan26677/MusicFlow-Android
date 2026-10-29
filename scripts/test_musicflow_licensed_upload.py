"""Offline-only checks for upload policy and two-step admin workflow."""

import contextlib
import hashlib
import io
import tempfile
import unittest
from argparse import Namespace
from pathlib import Path
from uuid import uuid4

from musicflow_licensed_upload import (
    PRIVATE_BUCKET,
    PUBLIC_BUCKET,
    SupabaseAdmin,
    clean_text,
    checked_https_url,
    publish,
    reviewed_rights,
    stage,
    validate_audio,
)


class FakeAdmin:
    def __init__(self):
        self.calls = []
        self.track_id = str(uuid4())
        self.data = b"ID3" + b"\x00" * 48
        self.evidence = {
            "content_sha256": hashlib.sha256(self.data).hexdigest(),
            "file_bytes": len(self.data),
            "original_filename": "music.mp3",
            "mime_type": "audio/mpeg",
            "staging_path": "drafts/test/music.mp3",
            "published_at": None,
        }

    def storage_upload(self, bucket, path, audio, mime):
        self.calls.append(("upload", bucket, path, audio, mime))

    def storage_download(self, bucket, path):
        self.calls.append(("download", bucket, path))
        return self.data

    def json(self, method, path, document=None, representation=False):
        self.calls.append(("json", method, path, document))
        if method == "GET":
            return [self.evidence] if "music_uploads" in path else [
                {"is_published": False}
            ]
        return None


def approved_args(api, **changes):
    data = dict(
        confirm_rights=True,
        track_id=api.track_id,
        license_reference="Artist signed public streaming contract #123",
        reviewer="Catalog owner",
        license_code="CC-BY-4.0",
        license_url="https://creativecommons.org/licenses/by/4.0/",
        source_url="https://example.com/artist/original-track",
        attribution="Artist — Original Track (CC BY 4.0)",
        allow_download=False,
        allow_commercial_use=False,
    )
    data.update(changes)
    return Namespace(**data)


class LicensedUploadTests(unittest.TestCase):
    def test_valid_magic_and_sha(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "sample.mp3"
            path.write_bytes(b"ID3" + b"\x00" * 42)
            audio, mime, sha = validate_audio(path)
            self.assertEqual(mime, "audio/mpeg")
            self.assertEqual(sha, hashlib.sha256(audio).hexdigest())

    def test_disguised_file_is_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "not-a-song.mp3"
            path.write_bytes(b"not-music")
            with self.assertRaises(ValueError):
                validate_audio(path)

    def test_empty_rights_reference_rejected(self):
        with self.assertRaises(ValueError):
            clean_text("  ", "Quyền sử dụng", 1000)

    def test_publish_requires_explicit_attestation(self):
        api = FakeAdmin()
        args = approved_args(api, confirm_rights=False)
        with self.assertRaises(ValueError):
            publish(api, args)
        self.assertEqual(api.calls, [])

    def test_stage_never_publishes(self):
        api = FakeAdmin()
        with tempfile.TemporaryDirectory() as folder:
            file_path = Path(folder) / "sample.mp3"
            file_path.write_bytes(api.data)
            args = Namespace(
                file=str(file_path),
                title="Original track",
                artist="Composer",
                album="",
                duration_ms=1000,
            )
            with contextlib.redirect_stdout(io.StringIO()):
                stage(api, args)
        self.assertEqual(api.calls[0][1], PRIVATE_BUCKET)
        draft = next(
            c for c in api.calls
            if c[0] == "json" and c[2] == "/rest/v1/music_tracks"
        )
        self.assertFalse(draft[3]["is_published"])
        self.assertFalse(draft[3]["rights_confirmed"])
        self.assertIsNone(draft[3].get("stream_url"))
        self.assertFalse(any(
            c[0] == "upload" and c[1] == PUBLIC_BUCKET
            for c in api.calls
        ))

    def test_publish_verifies_hash_and_uses_atomic_rpc(self):
        api = FakeAdmin()
        args = approved_args(api)
        with contextlib.redirect_stdout(io.StringIO()):
            publish(api, args)
        self.assertTrue(any(
            c[0] == "upload" and c[1] == PUBLIC_BUCKET
            for c in api.calls
        ))
        rpc = next(
            c for c in api.calls if c[0] == "json"
            and c[2] == "/rest/v1/rpc/musicflow_publish_licensed_track_v2"
        )
        self.assertEqual(rpc[3]["p_sha256"], api.evidence["content_sha256"])
        self.assertEqual(rpc[3]["p_reviewer"], "Catalog owner")
        self.assertEqual(rpc[3]["p_license_code"], "CC-BY-4.0")
        self.assertEqual(
            rpc[3]["p_attribution_text"], "Artist — Original Track (CC BY 4.0)"
        )
        self.assertTrue(rpc[3]["p_streaming_allowed"])
        self.assertFalse(rpc[3]["p_download_allowed"])
        self.assertFalse(rpc[3]["p_commercial_use_allowed"])

    def test_tampered_staging_blocks_publication(self):
        api = FakeAdmin()
        api.evidence["content_sha256"] = "0" * 64
        args = approved_args(api)
        with self.assertRaises(ValueError):
            publish(api, args)
        self.assertFalse(any(c[0] == "upload" for c in api.calls))

    def test_publishable_key_is_not_admin(self):
        with self.assertRaises(ValueError):
            SupabaseAdmin("sb_publishable_example")

    def test_missing_attribution_blocks_public_upload(self):
        api = FakeAdmin()
        with self.assertRaises(ValueError):
            publish(api, approved_args(api, attribution=" "))
        self.assertEqual(api.calls, [])

    def test_missing_or_bad_source_blocks_public_upload(self):
        for url in ("http://example.com", "file:///Music/a.wav",
                    "https://user:password@example.com/track",
                    "https://example.com/a#fragment", ""):
            with self.subTest(url=url):
                api = FakeAdmin()
                with self.assertRaises(ValueError):
                    publish(api, approved_args(api, source_url=url))
                self.assertEqual(api.calls, [])

    def test_wrong_cc_license_link_is_rejected(self):
        api = FakeAdmin()
        with self.assertRaises(ValueError):
            publish(api, approved_args(api, license_url="https://example.com/claims"))
        self.assertEqual(api.calls, [])

    def test_unapproved_license_is_rejected(self):
        api = FakeAdmin()
        with self.assertRaises(ValueError):
            publish(api, approved_args(api, license_code="CC-BY-NC-4.0"))
        self.assertEqual(api.calls, [])

    def test_direct_license_needs_reference_but_no_public_license_link(self):
        api = FakeAdmin()
        rights = reviewed_rights(approved_args(
            api, license_code="DIRECT", license_url="",
            license_reference="Private signed rights document #42"
        ))
        self.assertIsNone(rights["p_license_url"])
        self.assertTrue(rights["p_streaming_allowed"])

    def test_download_and_commercial_rights_are_opt_in(self):
        api = FakeAdmin()
        rights = reviewed_rights(approved_args(
            api, allow_download=True, allow_commercial_use=True
        ))
        self.assertTrue(rights["p_download_allowed"])
        self.assertTrue(rights["p_commercial_use_allowed"])

    def test_short_rights_reference_rejected(self):
        api = FakeAdmin()
        with self.assertRaises(ValueError):
            publish(api, approved_args(api, license_reference="no"))
        self.assertEqual(api.calls, [])

    def test_existing_published_path_disallows_reupload(self):
        api = FakeAdmin()
        api.evidence["published_path"] = "tracks/already-published/song.mp3"
        with self.assertRaises(ValueError):
            publish(api, approved_args(api))
        self.assertFalse(any(call[0] == "upload" for call in api.calls))

    def test_https_validation_preserves_query_and_rejects_credentials(self):
        valid = "https://example.com/artist?track=a%20b"
        self.assertEqual(checked_https_url(valid, "source"), valid)
        for source in ("https://user@example.com/artist",
                       "https://example.com/artist#",
                       "https://-example.com/track",
                       "https://ví-dụ.example/track"):
            with self.subTest(source=source):
                with self.assertRaises(ValueError):
                    checked_https_url(source, "source")


if __name__ == "__main__":
    unittest.main()
