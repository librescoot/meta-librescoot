#!/usr/bin/env python3
"""
Applies filesystem-aware delta patches to reconstruct Mender update files.

Usage:
    mender-apply-delta.py <old.mender> <delta.patch> <output.mender> [temp_dir]
"""
import sys
import os
import tarfile
import tempfile
import shutil
import subprocess
import json
import hashlib
import gzip
from pathlib import Path

# Default temp directory (can be overridden via command line)
DEFAULT_TEMP_DIR = '/data/ota/tmp'

# --- Utility Functions ---
def calculate_sha256(filepath):
    sha256_hash = hashlib.sha256()
    with open(filepath, "rb") as f:
        for byte_block in iter(lambda: f.read(4096), b""): sha256_hash.update(byte_block)
    return sha256_hash.hexdigest()

def extract_tar(tar_file, extract_dir):
    with tarfile.open(tar_file, 'r:*') as tar: tar.extractall(extract_dir)

def decompress_gz(gz_file, output_file):
    """Decompress a gzip file and return (input_size, output_size)."""
    input_size = os.path.getsize(gz_file)
    with gzip.open(gz_file, 'rb') as f_in, open(output_file, 'wb') as f_out:
        shutil.copyfileobj(f_in, f_out)
    output_size = os.path.getsize(output_file)
    return input_size, output_size

def compress_gz(input_file, gz_file):
    """Compress a file with gzip and return (input_size, output_size)."""
    # Use compression level 3 for much faster compression on ARM CPUs
    # Benchmarks on i.MX6UL (512MB rootfs):
    #   Level 3: 2m59s, 137MB (6% larger than level 9, 4.6x faster)
    #   Level 6: 5m18s, 130MB (0.6% larger, 2.6x faster)
    #   Level 9: 13m45s, 129MB (baseline)
    input_size = os.path.getsize(input_file)
    with open(input_file, 'rb') as f_in, gzip.open(gz_file, 'wb', compresslevel=3) as f_out:
        shutil.copyfileobj(f_in, f_out)
    output_size = os.path.getsize(gz_file)
    return input_size, output_size

def compress_gz_and_hash(input_file, gz_file):
    """Compress with gzip while stream-hashing in one pass.
    Returns (outer_sha256, input_size, output_size).
    Saves one full read vs calling calculate_sha256 + compress_gz separately.
    """
    hasher = hashlib.sha256()
    input_size = os.path.getsize(input_file)
    with open(input_file, 'rb') as f_in, gzip.open(gz_file, 'wb', compresslevel=3) as f_out:
        while True:
            chunk = f_in.read(65536)
            if not chunk:
                break
            hasher.update(chunk)
            f_out.write(chunk)
    return hasher.hexdigest(), input_size, os.path.getsize(gz_file)

def compress_payload_tar_and_hash(input_file, gz_file):
    """Compress a payload tar with gzip while computing both checksums in one pass:
    - outer_sha256: SHA256 of the raw tar (for decompressed_sha256 verification)
    - inner_sha256: SHA256 of the first file inside the tar (for rootfs-image.checksum)
    Returns (outer_sha256, inner_sha256, input_size, output_size).
    Saves two full reads vs the previous separate verify + compress + stream-hash calls.
    """
    TAR_BLOCK = 512
    CHUNK = 65536

    outer_hasher = hashlib.sha256()
    inner_hasher = hashlib.sha256()
    input_size = os.path.getsize(input_file)

    with open(input_file, 'rb') as f_in, gzip.open(gz_file, 'wb', compresslevel=3) as f_out:
        # Read the first tar header block to extract the inner file's size
        header = f_in.read(TAR_BLOCK)
        if len(header) < TAR_BLOCK:
            raise Exception("Payload tar smaller than one tar block")
        outer_hasher.update(header)
        f_out.write(header)

        # UStar header: file size at bytes 124-135 as null-padded octal ASCII
        size_field = header[124:136].split(b'\x00', 1)[0].strip()
        try:
            inner_bytes_remaining = int(size_field, 8)
        except ValueError:
            inner_bytes_remaining = 0

        while True:
            chunk = f_in.read(CHUNK)
            if not chunk:
                break
            outer_hasher.update(chunk)
            f_out.write(chunk)
            if inner_bytes_remaining > 0:
                usable = min(len(chunk), inner_bytes_remaining)
                inner_hasher.update(chunk[:usable])
                inner_bytes_remaining -= usable

    return outer_hasher.hexdigest(), inner_hasher.hexdigest(), input_size, os.path.getsize(gz_file)

def apply_xdelta_patch(old_file, patch_file, output_file):
    cmd = ['xdelta3', '-d', '-s', old_file, patch_file, output_file]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0: raise Exception(f"xdelta3 failed: {result.stderr}")

# --- Mender Artifact Manipulation ---
def verify_payload_against_manifest(output_dir, actual_payload_checksum, expected_new_payload_checksum):
    """Verify the reconstructed payload matches both the delta metadata and the
    manifest shipped in the delta.

    The manifest and header.tar.gz come verbatim from the new artifact (the
    delta ships them as 'new' files) and must NOT be regenerated here: mender
    validates each file inside data/NNNN.tar.gz against a manifest entry named
    data/NNNN/<filename> holding the checksum of the uncompressed content.
    """
    if expected_new_payload_checksum and actual_payload_checksum != expected_new_payload_checksum:
        raise Exception("CRITICAL: Final filesystem checksum mismatch!")

    manifest_path = os.path.join(output_dir, 'manifest')
    if not os.path.exists(manifest_path):
        raise Exception("CRITICAL: delta did not ship a manifest file!")
    with open(manifest_path, 'r') as f:
        payload_checksums = [line.split()[0] for line in f
                             if line.strip() and line.split()[1].startswith('data/')]
    if actual_payload_checksum not in payload_checksums:
        raise Exception("CRITICAL: reconstructed payload checksum is not listed in the shipped manifest!")

def format_size(size_bytes):
    """Format bytes as human-readable size."""
    for unit in ['B', 'KB', 'MB', 'GB']:
        if size_bytes < 1024:
            return f"{size_bytes:.1f}{unit}" if unit != 'B' else f"{size_bytes}B"
        size_bytes /= 1024
    return f"{size_bytes:.1f}TB"

def report_progress(percent, message):
    """Report progress in a parseable format to stdout."""
    print(f"PROGRESS:{percent}:{message}", flush=True)

# Phase weights for modified files (must sum to 1.0)
PHASE_WEIGHT_DECOMPRESS = 0.15
PHASE_WEIGHT_XDELTA = 0.35
PHASE_WEIGHT_COMPRESS = 0.50

# Minimum free space required before starting delta application.
# Peak usage: decompressed old rootfs (~1GB) + patched new rootfs (~1GB) + compressed output (~250MB)
REQUIRED_DISK_SPACE = 2_500_000_000

def apply_delta_patch(old_mender, delta_patch, output_mender, temp_base_dir):
    usage = shutil.disk_usage(temp_base_dir)
    if usage.free < REQUIRED_DISK_SPACE:
        raise Exception(
            f"Insufficient disk space: {format_size(usage.free)} free in {temp_base_dir}, "
            f"{format_size(REQUIRED_DISK_SPACE)} required"
        )

    with tempfile.TemporaryDirectory(dir=temp_base_dir) as temp_dir:
        old_dir, delta_dir, output_dir, work_dir = [os.path.join(temp_dir, d) for d in ['old', 'delta', 'output', 'work']]
        for d in [old_dir, delta_dir, output_dir, work_dir]: os.makedirs(d)

        report_progress(5, "Extracting old mender file")
        extract_tar(old_mender, old_dir)
        report_progress(10, "Extracting delta patch")
        extract_tar(delta_patch, delta_dir)

        with open(os.path.join(delta_dir, 'metadata.json'), 'r') as f:
            metadata = json.load(f)

        report_progress(12, f"Applying {len(metadata['changes'])} changes")
        newly_created_payload_checksum = None

        all_new_files = set(metadata['changes'].keys())
        all_old_files = {p.relative_to(old_dir).as_posix() for p in Path(old_dir).rglob('*') if p.is_file()}
        unchanged_files = all_old_files - all_new_files

        # Calculate total work for progress tracking
        total_files = len(unchanged_files) + len(metadata['changes'])
        processed_files = 0

        report_progress(15, f"Copying {len(unchanged_files)} unchanged files")
        for rel_path in unchanged_files:
            dest_path = os.path.join(output_dir, rel_path)
            os.makedirs(os.path.dirname(dest_path), exist_ok=True)
            shutil.copy2(os.path.join(old_dir, rel_path), dest_path)
            processed_files += 1
            # Report progress for unchanged files (15% to 30%)
            progress = 15 + int((processed_files / total_files) * 15)
            if processed_files % max(1, len(unchanged_files) // 5) == 0:  # Report every ~20%
                report_progress(progress, f"Copied {processed_files}/{total_files} files")

        # Pre-calculate work weights based on file sizes
        change_weights = {}
        total_weight = 0
        for rel_path, change in metadata['changes'].items():
            if change['type'] == 'new':
                new_file_path = os.path.join(delta_dir, 'new_files', rel_path)
                weight = os.path.getsize(new_file_path)
            elif change['type'] == 'modified':
                old_file_path = os.path.join(old_dir, rel_path)
                patch_file_path = os.path.join(delta_dir, 'patches', change['patch'])
                weight = os.path.getsize(old_file_path) + os.path.getsize(patch_file_path)
            else:
                weight = 1
            change_weights[rel_path] = weight
            total_weight += weight

        report_progress(30, f"Processing {len(metadata['changes'])} changes")
        processed_weight = 0
        for idx, (rel_path, change) in enumerate(metadata['changes'].items(), 1):
            output_file_path = os.path.join(output_dir, rel_path)
            os.makedirs(os.path.dirname(output_file_path), exist_ok=True)
            weight = change_weights[rel_path]

            if change['type'] == 'new':
                new_file_path = os.path.join(delta_dir, 'new_files', rel_path)
                report_progress(30 + int((processed_weight / total_weight) * 50),
                    f"[{idx}/{len(metadata['changes'])}] new: {rel_path} ({format_size(weight)})")
                shutil.copy2(new_file_path, output_file_path)

            elif change['type'] == 'modified':
                old_file_path = os.path.join(old_dir, rel_path)
                patch_file_path = os.path.join(delta_dir, 'patches', change['patch'])
                old_meta, new_meta = change.get('old_meta',{}), change.get('new_meta',{})

                old_size = os.path.getsize(old_file_path)
                patch_size = os.path.getsize(patch_file_path)

                # Calculate base progress for this file
                base_progress = 30 + int((processed_weight / total_weight) * 50)
                file_progress_range = int((weight / total_weight) * 50)

                def file_phase_progress(phase_start, phase_name):
                    """Report progress for a phase within this file's processing."""
                    progress = base_progress + int(file_progress_range * phase_start)
                    report_progress(progress,
                        f"[{idx}/{len(metadata['changes'])}] patch: {rel_path} - {phase_name}")

                # Initial message with file sizes
                report_progress(base_progress,
                    f"[{idx}/{len(metadata['changes'])}] patch: {rel_path} (old: {format_size(old_size)}, patch: {format_size(patch_size)})")

                source_for_patching = old_file_path
                if old_meta.get('compressed'):
                    file_phase_progress(0, "decompressing")
                    source_for_patching = os.path.join(work_dir, f"old_decomp_{rel_path.replace('/', '_')}")
                    in_sz, out_sz = decompress_gz(old_file_path, source_for_patching)
                    report_progress(base_progress + int(file_progress_range * PHASE_WEIGHT_DECOMPRESS * 0.5),
                        f"[{idx}/{len(metadata['changes'])}] patch: {rel_path} - decompressed {format_size(in_sz)} -> {format_size(out_sz)}")

                file_phase_progress(PHASE_WEIGHT_DECOMPRESS, "applying delta")
                new_temp_path = os.path.join(work_dir, f"new_patched_{rel_path.replace('/', '_')}")
                apply_xdelta_patch(source_for_patching, patch_file_path, new_temp_path)
                if old_meta.get('compressed'):
                    os.unlink(source_for_patching)
                delta_out_size = os.path.getsize(new_temp_path)
                report_progress(base_progress + int(file_progress_range * (PHASE_WEIGHT_DECOMPRESS + PHASE_WEIGHT_XDELTA * 0.5)),
                    f"[{idx}/{len(metadata['changes'])}] patch: {rel_path} - delta produced {format_size(delta_out_size)}")

                if new_meta.get('compressed'):
                    file_phase_progress(PHASE_WEIGHT_DECOMPRESS + PHASE_WEIGHT_XDELTA, "compressing + verifying")
                    if rel_path.startswith('data/'):
                        # Single pass: compress + verify outer sha256 + compute inner rootfs sha256
                        outer_sha256, inner_sha256, in_sz, out_sz = compress_payload_tar_and_hash(new_temp_path, output_file_path)
                        if outer_sha256 != new_meta.get('decompressed_sha256', outer_sha256):
                            raise Exception(f"Checksum mismatch on patched content for {rel_path}")
                        newly_created_payload_checksum = inner_sha256
                    else:
                        # Single pass: compress + verify outer sha256
                        outer_sha256, in_sz, out_sz = compress_gz_and_hash(new_temp_path, output_file_path)
                        if outer_sha256 != new_meta.get('decompressed_sha256', outer_sha256):
                            raise Exception(f"Checksum mismatch on patched content for {rel_path}")
                    ratio = (out_sz / in_sz * 100) if in_sz > 0 else 0
                    report_progress(base_progress + int(file_progress_range * 0.95),
                        f"[{idx}/{len(metadata['changes'])}] patch: {rel_path} - compressed {format_size(in_sz)} -> {format_size(out_sz)} ({ratio:.1f}%)")
                else:
                     shutil.move(new_temp_path, output_file_path)

            processed_weight += weight

        report_progress(80, "Finalizing artifact")
        # Progress breakdown for finalization (80-100%):
        # 80-82: Verify payload against shipped manifest
        # 95-100: Create output mender file

        if newly_created_payload_checksum is not None:
            report_progress(80, "Verifying payload against manifest")
            verify_payload_against_manifest(output_dir, newly_created_payload_checksum,
                metadata.get('new_payload_checksum'))

        report_progress(95, "Creating output mender file")
        with tarfile.open(output_mender, 'w') as tar:
            items_to_add = ['version', 'manifest', 'header.tar.gz']
            data_dir = os.path.join(output_dir, 'data')
            if os.path.isdir(data_dir):
                items_to_add.extend([os.path.join('data', f) for f in sorted(os.listdir(data_dir))])

            total_items = len(items_to_add)
            for i, item in enumerate(items_to_add):
                # Progress from 95-100% based on items
                progress = 95 + int((i / total_items) * 5)
                if item.startswith('data/'):
                    item_path = os.path.join(output_dir, item)
                    item_size = os.path.getsize(item_path)
                    report_progress(progress, f"Adding {item} ({format_size(item_size)})")
                    tar.add(item_path, arcname=item)
                else:
                    report_progress(progress, f"Adding {item}")
                    tar.add(os.path.join(output_dir, item), arcname=item)

        report_progress(100, "Complete")

def main():
    if len(sys.argv) < 4 or len(sys.argv) > 5:
        print(f"Usage: python3 {sys.argv[0]} <old.mender> <delta.patch> <output.mender> [temp_dir]")
        sys.exit(1)
    if shutil.which('xdelta3') is None:
        print("Error: xdelta3 is not installed or not in PATH.")
        sys.exit(1)

    # Get temp directory from command line or use default
    temp_dir = sys.argv[4] if len(sys.argv) == 5 else DEFAULT_TEMP_DIR

    # Ensure temp directory exists
    os.makedirs(temp_dir, exist_ok=True)

    try:
        apply_delta_patch(sys.argv[1], sys.argv[2], sys.argv[3], temp_dir)
    except Exception as e:
        print(f"\nError: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == '__main__':
    main()
