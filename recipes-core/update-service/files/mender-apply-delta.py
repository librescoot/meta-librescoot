#!/usr/bin/env python3
"""
Applies filesystem-aware delta patches to reconstruct Mender update files.

Usage:
    mender-apply-delta.py <old.mender> <delta.patch> <output.mender>
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

# Custom temporary directory location to avoid filling up root partition
TEMP_DIR = '/data/_tmp'

# --- Utility Functions ---

def calculate_sha256(filepath):
    """Calculate SHA256 hash of a file."""
    sha256_hash = hashlib.sha256()
    with open(filepath, "rb") as f:
        for byte_block in iter(lambda: f.read(4096), b""):
            sha256_hash.update(byte_block)
    return sha256_hash.hexdigest()

def extract_tar(tar_file, extract_dir):
    """Extract a tar file to a directory."""
    with tarfile.open(tar_file, 'r:*') as tar:
        tar.extractall(extract_dir)

def decompress_gz(gz_file, output_file):
    """Decompress a .gz file (e.g., .tar.gz -> .tar)."""
    with gzip.open(gz_file, 'rb') as f_in:
        with open(output_file, 'wb') as f_out:
            shutil.copyfileobj(f_in, f_out)

def compress_gz(input_file, gz_file):
    """Compress a file with gzip."""
    with open(input_file, 'rb') as f_in:
        with gzip.open(gz_file, 'wb', compresslevel=9) as f_out:
            shutil.copyfileobj(f_in, f_out)

def apply_xdelta_patch(old_file, patch_file, output_file):
    """Apply an xdelta3 patch. Its success is our implicit verification."""
    cmd = ['xdelta3', '-d', '-s', old_file, patch_file, output_file]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        raise Exception(f"xdelta3 failed: {result.stderr}")

# --- Mender Artifact Manipulation ---

def update_manifest_file(output_dir):
    """Update the manifest file with correct SHA256 checksums."""
    manifest_path = os.path.join(output_dir, 'manifest')
    manifest_lines = []
    files_in_manifest = sorted([p for p in Path(output_dir).rglob('*') if p.is_file() and p.name != 'manifest'])
    for filepath in files_in_manifest:
        rel_path = filepath.relative_to(output_dir).as_posix()
        checksum = calculate_sha256(filepath)
        manifest_lines.append(f"{checksum}  {rel_path}\n")
    with open(manifest_path, 'w') as f:
        f.writelines(manifest_lines)
    print(f"  Updated manifest file with {len(manifest_lines)} entries")

def update_header_with_payload_checksum(output_dir, work_dir, new_payload_tar_path, expected_new_payload_checksum):
    """
    Verifies the final filesystem checksum and updates the header with it.
    """
    header_tar_path = os.path.join(output_dir, 'header.tar.gz')
    if not os.path.exists(header_tar_path):
        return

    print("  Extracting new filesystem to calculate and verify its checksum...")
    fs_extract_dir = os.path.join(work_dir, 'fs_extract_for_checksum')
    if os.path.exists(fs_extract_dir): shutil.rmtree(fs_extract_dir)
    os.makedirs(fs_extract_dir)
    with tarfile.open(new_payload_tar_path, 'r') as tar:
        tar.extractall(fs_extract_dir)
    
    fs_image_files = [p for p in Path(fs_extract_dir).rglob('*') if p.is_file()]
    if not fs_image_files:
        raise Exception("Could not find filesystem image inside the newly generated payload.")
    
    actual_payload_checksum = calculate_sha256(fs_image_files[0])
    print(f"  Calculated new filesystem checksum: {actual_payload_checksum}")

    # *** THE FINAL VERIFICATION IS HERE ***
    if expected_new_payload_checksum and actual_payload_checksum != expected_new_payload_checksum:
        raise Exception(
            f"CRITICAL: Final filesystem checksum mismatch!\n"
            f"  Expected (from delta): {expected_new_payload_checksum}\n"
            f"  Got (from patched file): {actual_payload_checksum}"
        )
    elif expected_new_payload_checksum:
        print("  ✓ Final filesystem checksum verified successfully.")

    header_extract_dir = os.path.join(work_dir, 'header_extract')
    if os.path.exists(header_extract_dir): shutil.rmtree(header_extract_dir)
    extract_tar(header_tar_path, header_extract_dir)
    
    type_info_path = next(Path(header_extract_dir).rglob('type-info'), None)
    if not type_info_path: return

    with open(type_info_path, 'r') as f:
        type_info = json.load(f)
    
    if 'artifact_provides' in type_info and 'rootfs-image.checksum' in type_info['artifact_provides']:
        print(f"  Updating rootfs-image.checksum in type-info")
        type_info['artifact_provides']['rootfs-image.checksum'] = actual_payload_checksum
        with open(type_info_path, 'w') as f:
            json.dump(type_info, f, separators=(',', ':'))
        
        print("  Recreating header.tar.gz...")
        with tarfile.open(header_tar_path, 'w:gz', compresslevel=9) as tar:
            all_files = []
            for root, _, files in os.walk(header_extract_dir):
                for file in files:
                    full_path = os.path.join(root, file)
                    relative_path = os.path.relpath(full_path, header_extract_dir)
                    all_files.append((full_path, relative_path))

            def mender_header_sort_key(item):
                arcname = item[1].replace('\\', '/')
                basename = os.path.basename(arcname)
                if basename == 'header-info': return (0, arcname)
                if basename == 'type-info': return (1, arcname)
                if basename == 'meta-data': return (2, arcname)
                return (9, arcname)

            all_files.sort(key=mender_header_sort_key)
            for full_path, arcname in all_files:
                tar.add(full_path, arcname=arcname)

# --- Main Application Logic ---

def apply_delta_patch(old_mender, delta_patch, output_mender):
    os.makedirs(TEMP_DIR, exist_ok=True)
    with tempfile.TemporaryDirectory(dir=TEMP_DIR) as temp_dir:
        old_dir, delta_dir, output_dir, work_dir = [os.path.join(temp_dir, d) for d in ['old', 'delta', 'output', 'work']]
        for d in [old_dir, delta_dir, output_dir, work_dir]:
            os.makedirs(d)

        print(f"Extracting old Mender file: {old_mender}")
        extract_tar(old_mender, old_dir)
        print(f"Extracting delta patch: {delta_patch}")
        extract_tar(delta_patch, delta_dir)

        with open(os.path.join(delta_dir, 'metadata.json'), 'r') as f:
            metadata = json.load(f)

        cached_decompressed_files = {}
        newly_created_payload_tar = None

        print("\nApplying changes...")
        for rel_path, change_info in metadata['changes'].items():
            change_type = change_info['type']
            output_file = os.path.join(output_dir, rel_path)
            os.makedirs(os.path.dirname(output_file), exist_ok=True)
            if change_type in ['unchanged', 'new', 'deleted']:
                if change_type == 'unchanged': shutil.copy2(os.path.join(old_dir, rel_path), output_file)
                elif change_type == 'new': shutil.copy2(os.path.join(delta_dir, 'new_files', rel_path), output_file)
                continue

            print(f"  Patching: {rel_path}")
            old_file_path = os.path.join(old_dir, rel_path)
            patch_file_path = os.path.join(delta_dir, 'patches', change_info['patch'])
            new_temp_path = os.path.join(work_dir, f"new_decomp_{rel_path.replace('/', '_')}")

            old_meta = change_info.get('old_meta', {})
            new_meta = change_info.get('new_meta', {})

            source_for_patching = old_file_path
            if old_meta.get('compressed'):
                if rel_path in cached_decompressed_files:
                    source_for_patching = cached_decompressed_files[rel_path]
                else:
                    decompressed_path = os.path.join(work_dir, f"old_decomp_{rel_path.replace('/', '_')}")
                    decompress_gz(old_file_path, decompressed_path)
                    source_for_patching = decompressed_path
                    cached_decompressed_files[rel_path] = decompressed_path

            apply_xdelta_patch(source_for_patching, patch_file_path, new_temp_path)

            if new_meta.get('compressed'):
                if calculate_sha256(new_temp_path) != new_meta.get('decompressed_sha256'):
                    raise Exception(f"Patched decompressed file hash mismatch for {rel_path}")
                print(f"    Patched content verified for {rel_path}, compressing now...")
                compress_gz(new_temp_path, output_file)
                if rel_path.startswith('data/'):
                    newly_created_payload_tar = new_temp_path
            else:
                shutil.move(new_temp_path, output_file)

        print("\nUpdating Mender artifact metadata...")
        if newly_created_payload_tar:
            update_header_with_payload_checksum(output_dir, work_dir, newly_created_payload_tar, metadata.get('new_payload_checksum'))
        update_manifest_file(output_dir)

        print(f"\nCreating output Mender file: {output_mender}")
        with tarfile.open(output_mender, 'w') as tar:
            top_level_files = ['version', 'manifest', 'header.tar.gz']
            for item_name in top_level_files:
                item_path = os.path.join(output_dir, item_name)
                if os.path.exists(item_path): tar.add(item_path, arcname=item_name)
            
            data_dir_path = os.path.join(output_dir, 'data')
            if os.path.isdir(data_dir_path):
                for data_file in sorted(os.listdir(data_dir_path)):
                    full_path = os.path.join(data_dir_path, data_file)
                    arcname = os.path.join('data', data_file)
                    tar.add(full_path, arcname=arcname)
        
        print("\n✓ Delta patch applied and Mender artifact created successfully!")

def main():
    if len(sys.argv) != 4:
        print(f"Usage: python3 {sys.argv[0]} <old.mender> <delta.patch> <output.mender>")
        sys.exit(1)
    if shutil.which('xdelta3') is None:
        print("Error: xdelta3 is not installed or not in PATH")
        sys.exit(1)
    try:
        apply_delta_patch(sys.argv[1], sys.argv[2], sys.argv[3])
    except Exception as e:
        print(f"\nError: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == '__main__':
    main()
