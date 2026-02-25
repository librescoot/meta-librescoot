#!/usr/bin/env python3
"""
Convert a Lottie animation file to numbered PNG frames for Plymouth boot splash.

Usage:
  python3 convert-lottie.py <input.json> <output-dir> [--fps 15] [--size 480]

Output files: frame-1.png, frame-2.png, ..., frame-N.png
Frames are composited on a black background.

After conversion, quantize to 8-bit for faster Plymouth decode on ARM (~30ms vs ~54ms):
  pngquant --quality 80-100 --strip --force --ext .png <output-dir>/frame-*.png

Plymouth script notes (Cortex-A9 @ 800MHz, 59Hz display):
  - Load frames lazily (one per refresh window) to avoid blocking startup
  - frame_duration=8 → 7.5fps (good balance of smoothness vs decode time)
  - 60 frames at 7.5fps = 8 seconds
"""

import argparse
import io
import os
import sys

try:
    from lottie import parsers
    from lottie.exporters.cairo import PngRenderer
except ImportError:
    print("Error: lottie library not found. Install with: pip install lottie", file=sys.stderr)
    sys.exit(1)

try:
    from PIL import Image
except ImportError:
    print("Error: Pillow not found. Install with: pip install Pillow", file=sys.stderr)
    sys.exit(1)


def convert(input_file, output_dir, fps=15, size=480):
    print(f"Loading {input_file}...")
    animation = parsers.tgs.parse_tgs(input_file)

    src_fps = float(animation.frame_rate)
    src_frames = int(animation.out_point - animation.in_point)
    duration_s = src_frames / src_fps
    src_w = int(animation.width)
    src_h = int(animation.height)

    n_frames = round(duration_s * fps)
    frame_step = src_frames / n_frames

    print(f"  Source: {src_frames} frames @ {src_fps}fps = {duration_s:.2f}s ({src_w}x{src_h})")
    print(f"  Output: {n_frames} frames @ {fps}fps -> {output_dir} ({size}x{size})")

    os.makedirs(output_dir, exist_ok=True)

    with PngRenderer(animation, 96) as renderer:
        for i in range(n_frames):
            src_frame = int(animation.in_point + i * frame_step)

            buf = io.BytesIO()
            renderer.serialize(src_frame, buf)
            buf.seek(0)

            frame = Image.open(buf).convert("RGBA")

            if frame.width != size or frame.height != size:
                frame = frame.resize((size, size), Image.LANCZOS)

            # Composite on solid black
            bg = Image.new("RGB", (size, size), (0, 0, 0))
            bg.paste(frame, mask=frame.split()[3])

            out_path = os.path.join(output_dir, f"frame-{i + 1}.png")
            bg.save(out_path, "PNG", optimize=False)

            if (i + 1) % 10 == 0 or i + 1 == n_frames:
                print(f"  {i + 1}/{n_frames}")

    print(f"Done. {n_frames} frames written to {output_dir}/")
    return n_frames


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("input", help="Input Lottie JSON file")
    parser.add_argument("output", help="Output directory for PNG frames")
    parser.add_argument("--fps", type=int, default=15, help="Target frame rate (default: 15)")
    parser.add_argument("--size", type=int, default=480, help="Output image size in pixels (default: 480)")
    args = parser.parse_args()

    n = convert(args.input, args.output, fps=args.fps, size=args.size)
    print(f"\nFor Plymouth script, set: num_frames = {n}; frame_duration = {round(60 / args.fps)};")


if __name__ == "__main__":
    main()
