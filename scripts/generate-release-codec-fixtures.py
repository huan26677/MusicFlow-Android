"""Create original, synthetic MP3/FLAC media for MusicFlow release QA.

No copyrighted music is used. Each file carries predictable artist/album/title
metadata so the MediaStore/Library integration can be tested on an emulator.

Prerequisite: FFmpeg with libmp3lame and FLAC encoders. You can specify
--ffmpeg, put ffmpeg on PATH, or install imageio-ffmpeg into
app/build/rc-qa/python-deps (the generated files stay ignored by Git).
"""

from __future__ import annotations

import argparse
import os
from pathlib import Path
import shutil
import subprocess
import sys


def locate_ffmpeg(explicit: str | None) -> str:
    if explicit:
        if not Path(explicit).is_file():
            raise SystemExit("Specified FFmpeg executable does not exist")
        return explicit
    on_path = shutil.which("ffmpeg")
    if on_path:
        return on_path
    root = Path(__file__).resolve().parents[1]
    local_modules = root / "app" / "build" / "rc-qa" / "python-deps"
    if local_modules.is_dir():
        sys.path.insert(0, str(local_modules))
    try:
        import imageio_ffmpeg
    except ImportError as exc:
        raise SystemExit(
            "FFmpeg unavailable: set --ffmpeg or install imageio-ffmpeg into "
            "app/build/rc-qa/python-deps"
        ) from exc
    return imageio_ffmpeg.get_ffmpeg_exe()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--prefix", required=True)
    parser.add_argument("--ffmpeg", default=None)
    parser.add_argument("--seconds", type=int, default=40)
    args = parser.parse_args()
    if not args.prefix.startswith("AAA_MusicFlow_CODEC_"):
        raise SystemExit("Refusing non-QA prefix")
    if not 20 <= args.seconds <= 120:
        raise SystemExit("Expected 20-120 seconds of synthetic QA sound")
    args.output.mkdir(parents=True, exist_ok=True)
    ffmpeg = locate_ffmpeg(args.ffmpeg)
    common_meta = (
        "-metadata",
        "artist=MusicFlow QA Artist",
        "-metadata",
        "album=MusicFlow QA Album",
        "-metadata",
        "genre=Electronic",
    )
    for letter, frequency, codec, suffix in (
        ("A", 440, "libmp3lame", "mp3"),
        ("B", 554, "flac", "flac"),
    ):
        title = f"{args.prefix}_{letter}"
        target = args.output / f"{title}.{suffix}"
        command = [
            ffmpeg,
            "-hide_banner",
            "-loglevel",
            "error",
            "-nostdin",
            "-y",
            "-f",
            "lavfi",
            "-i",
            f"sine=frequency={frequency}:sample_rate=44100:duration={args.seconds}",
            "-vn",
            "-ac",
            "1",
            *common_meta,
            "-metadata",
            f"title={title}",
            "-c:a",
            codec,
        ]
        if suffix == "mp3":
            command += ["-q:a", "4"]
        else:
            command += ["-compression_level", "5"]
        command.append(str(target))
        subprocess.run(command, check=True)
        if not target.is_file() or target.stat().st_size < 1024:
            raise SystemExit(f"Missing/empty fixture {suffix}")
        print(f"FIXTURE_{suffix.upper()}={target} SIZE={target.stat().st_size}")
    print("FIXTURE_GENERATION=PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
