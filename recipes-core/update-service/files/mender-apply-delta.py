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

def apply_xdelta_patch(old_file, patch_file, output_file):
    cmd = ['xdelta3', '-d', '-s', old_file, patch_file, output_file]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0: raise Exception(f"xdelta3 failed: {result.stderr}")

# --- Mender Artifact Manipulation ---
def update_manifest_file(output_dir, progress_callback=None):
    manifest_path = os.path.join(output_dir, 'manifest')
    manifest_lines = []
    files_to_manifest = sorted([p for p in Path(output_dir).rglob('*') if p.is_file() and p.name != 'manifest'])
    total_files = len(files_to_manifest)
    for i, filepath in enumerate(files_to_manifest):
        rel_path = filepath.relative_to(output_dir).as_posix()
        if progress_callback:
            progress_callback(f"Hashing {rel_path} ({i+1}/{total_files})")
        manifest_lines.append(f"{calculate_sha256(filepath)}  {rel_path}\n")
    with open(manifest_path, 'w') as f: f.writelines(manifest_lines)

def update_header_with_payload_checksum(output_dir, work_dir, new_payload_tar_path, expected_new_payload_checksum, progress_callback=None):
    header_tar_path = os.path.join(output_dir, 'header.tar.gz')
    if not os.path.exists(header_tar_path): return

    if progress_callback: progress_callback("Extracting payload for verification")
    with tempfile.TemporaryDirectory(dir=work_dir) as fs_extract_dir:
        with tarfile.open(new_payload_tar_path, 'r') as tar: tar.extractall(fs_extract_dir)
        fs_image_path = next(Path(fs_extract_dir).rglob('*'), None)
        if not fs_image_path: raise Exception("No filesystem image in reconstructed payload.")

        if progress_callback: progress_callback(f"Verifying rootfs checksum ({format_size(os.path.getsize(fs_image_path))})")
        actual_payload_checksum = calculate_sha256(fs_image_path)
        if expected_new_payload_checksum and actual_payload_checksum != expected_new_payload_checksum:
            raise Exception("CRITICAL: Final filesystem checksum mismatch!")

    if progress_callback: progress_callback("Updating header metadata")
    with tempfile.TemporaryDirectory(dir=work_dir) as header_extract_dir:
        extract_tar(header_tar_path, header_extract_dir)
        type_info_path = next(Path(header_extract_dir).rglob('type-info'), None)
        if type_info_path:
            with open(type_info_path, 'r+') as f:
                type_info = json.load(f)
                type_info['artifact_provides']['rootfs-image.checksum'] = actual_payload_checksum
                f.seek(0); json.dump(type_info, f, separators=(',', ':')); f.truncate()

            if progress_callback: progress_callback("Recreating header.tar.gz")
            with tarfile.open(header_tar_path, 'w:gz') as tar:
                all_files = []
                for root, _, files in os.walk(header_extract_dir):
                    for file in files:
                        full_path = os.path.join(root, file)
                        relative_path = os.path.relpath(full_path, header_extract_dir)
                        all_files.append((full_path, relative_path))

                def mender_header_sort_key(item):
                    """A multi-level sort key to enforce Mender's strict header order."""
                    arcname = item[1].replace('\\', '/') # Normalize path separator
                    basename = os.path.basename(arcname)

                    if basename == 'header-info': return (0, arcname)
                    if basename == 'type-info': return (1, arcname)
                    if basename == 'meta-data': return (2, arcname)
                    return (9, arcname)

                all_files.sort(key=mender_header_sort_key)

                for full_path, arcname in all_files:
                    tar.add(full_path, arcname=arcname)

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

def apply_delta_patch(old_mender, delta_patch, output_mender, temp_base_dir):
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
        newly_created_payload_tar = None

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
                delta_out_size = os.path.getsize(new_temp_path)
                report_progress(base_progress + int(file_progress_range * (PHASE_WEIGHT_DECOMPRESS + PHASE_WEIGHT_XDELTA * 0.5)),
                    f"[{idx}/{len(metadata['changes'])}] patch: {rel_path} - delta produced {format_size(delta_out_size)}")

                if new_meta.get('compressed'):
                    file_phase_progress(PHASE_WEIGHT_DECOMPRESS + PHASE_WEIGHT_XDELTA, "compressing")
                    if calculate_sha256(new_temp_path) != new_meta['decompressed_sha256']:
                        raise Exception(f"Checksum mismatch on patched content for {rel_path}")
                    in_sz, out_sz = compress_gz(new_temp_path, output_file_path)
                    ratio = (out_sz / in_sz * 100) if in_sz > 0 else 0
                    report_progress(base_progress + int(file_progress_range * 0.95),
                        f"[{idx}/{len(metadata['changes'])}] patch: {rel_path} - compressed {format_size(in_sz)} -> {format_size(out_sz)} ({ratio:.1f}%)")
                    if rel_path.startswith('data/'):
                        newly_created_payload_tar = new_temp_path
                else:
                     shutil.move(new_temp_path, output_file_path)

            processed_weight += weight

        report_progress(80, "Finalizing artifact")
        # Progress breakdown for finalization (80-100%):
        # 80-81: Extract payload for verification
        # 81-87: Verify rootfs checksum (SHA256 of ~1GB)
        # 87-88: Update header metadata
        # 88-89: Recreate header.tar.gz
        # 89-95: Generate manifest (SHA256 of each output file)
        # 95-100: Create output mender file

        header_progress_map = {
            "Extracting payload for verification": 80,
            "Verifying rootfs checksum": 81,
            "Updating header metadata": 87,
            "Recreating header.tar.gz": 88,
        }

        def header_progress_callback(msg):
            # Find matching progress by prefix
            for prefix, pct in header_progress_map.items():
                if msg.startswith(prefix):
                    report_progress(pct, msg)
                    return
            report_progress(80, msg)

        if newly_created_payload_tar:
            update_header_with_payload_checksum(output_dir, work_dir, newly_created_payload_tar,
                metadata.get('new_payload_checksum'), progress_callback=header_progress_callback)

        report_progress(89, "Generating manifest")
        def manifest_progress_callback(msg):
            # Manifest generation is 89-95% range
            report_progress(89, msg)

        update_manifest_file(output_dir, progress_callback=manifest_progress_callback)

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
