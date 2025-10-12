#!/usr/bin/env python3
"""
Applies filesystem-aware delta patches to reconstruct Mender update files.
This version correctly handles chained updates by treating metadata files
(manifest, header.tar.gz) as non-patchable, regenerating them instead.
"""
import sys, os, tarfile, tempfile, shutil, subprocess, json, hashlib, gzip
from pathlib import Path

TEMP_DIR = '/data/_tmp'

# --- (Utility functions are unchanged) ---
def calculate_sha256(filepath):
    sha256_hash = hashlib.sha256()
    with open(filepath, "rb") as f:
        for byte_block in iter(lambda: f.read(4096), b""):
            sha256_hash.update(byte_block)
    return sha256_hash.hexdigest()

def extract_tar(tar_file, extract_dir):
    with tarfile.open(tar_file, 'r:*') as tar:
        tar.extractall(extract_dir)

def decompress_gz(gz_file, output_file):
    with gzip.open(gz_file, 'rb') as f_in:
        with open(output_file, 'wb') as f_out:
            shutil.copyfileobj(f_in, f_out)

def compress_gz(input_file, gz_file):
    with open(input_file, 'rb') as f_in:
        with gzip.open(gz_file, 'wb', compresslevel=9) as f_out:
            shutil.copyfileobj(f_in, f_out)

def apply_xdelta_patch(old_file, patch_file, output_file):
    cmd = ['xdelta3', '-d', '-s', old_file, patch_file, output_file]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        raise Exception(f"xdelta3 failed: {result.stderr}")

# --- (Mender artifact manipulation functions are largely the same, but simplified) ---
def update_manifest_file(output_dir):
    manifest_path = os.path.join(output_dir, 'manifest')
    manifest_lines = []
    # Note: We now create the manifest from scratch.
    files_to_manifest = [p for p in Path(output_dir).rglob('*') if p.is_file() and p.name != 'manifest']
    for filepath in sorted(files_to_manifest):
        rel_path = filepath.relative_to(output_dir).as_posix()
        checksum = calculate_sha256(filepath)
        manifest_lines.append(f"{checksum}  {rel_path}\n")
    with open(manifest_path, 'w') as f: f.writelines(manifest_lines)
    print(f"  Generated new manifest file.")

def update_header_with_payload_checksum(output_dir, work_dir, new_payload_tar_path, expected_new_payload_checksum):
    header_tar_path = os.path.join(output_dir, 'header.tar.gz')
    if not os.path.exists(header_tar_path): return

    print("  Verifying final filesystem and updating header...")
    with tempfile.TemporaryDirectory(dir=work_dir) as fs_extract_dir:
        with tarfile.open(new_payload_tar_path, 'r') as tar:
            tar.extractall(fs_extract_dir)
        fs_image_path = next(Path(fs_extract_dir).rglob('*'), None)
        if not fs_image_path: raise Exception("No filesystem image found in reconstructed payload.")
        
        actual_payload_checksum = calculate_sha256(fs_image_path)
        if expected_new_payload_checksum and actual_payload_checksum != expected_new_payload_checksum:
            raise Exception("CRITICAL: Final filesystem checksum mismatch!")
        print("  ✓ Final filesystem checksum verified.")

    with tempfile.TemporaryDirectory(dir=work_dir) as header_extract_dir:
        extract_tar(header_tar_path, header_extract_dir)
        type_info_path = next(Path(header_extract_dir).rglob('type-info'), None)
        if type_info_path:
            with open(type_info_path, 'r+') as f:
                type_info = json.load(f)
                type_info['artifact_provides']['rootfs-image.checksum'] = actual_payload_checksum
                f.seek(0)
                json.dump(type_info, f, separators=(',', ':'))
                f.truncate()
            
            with tarfile.open(header_tar_path, 'w:gz') as tar:
                all_files = sorted(list(Path(header_extract_dir).rglob('*')), key=lambda p: p.as_posix())
                for path in all_files:
                    if path.is_file():
                        tar.add(path, arcname=path.relative_to(header_extract_dir).as_posix())
            print("  Updated header.tar.gz with new checksum.")

def apply_delta_patch(old_mender, delta_patch, output_mender):
    with tempfile.TemporaryDirectory(dir=TEMP_DIR) as temp_dir:
        old_dir, delta_dir, output_dir, work_dir = [os.path.join(temp_dir, d) for d in ['old', 'delta', 'output', 'work']]
        for d in [old_dir, delta_dir, output_dir, work_dir]: os.makedirs(d)

        extract_tar(old_mender, old_dir)
        extract_tar(delta_patch, delta_dir)

        with open(os.path.join(delta_dir, 'metadata.json'), 'r') as f:
            metadata = json.load(f)

        # Reconstruct the new artifact structure first
        print("\nReconstructing artifact structure...")
        new_files_dir = os.path.join(delta_dir, 'new_files')
        # Copy all unchanged files from the old artifact
        old_files = {p.relative_to(old_dir).as_posix() for p in Path(old_dir).rglob('*') if p.is_file()}
        new_files_in_delta = {p.relative_to(new_files_dir).as_posix() for p in Path(new_files_dir).rglob('*') if p.is_file()}
        
        for rel_path in old_files:
            if rel_path not in metadata['changes']: # Unchanged
                dest_path = os.path.join(output_dir, rel_path)
                os.makedirs(os.path.dirname(dest_path), exist_ok=True)
                shutil.copy2(os.path.join(old_dir, rel_path), dest_path)

        # Copy all new files (including metadata templates) from the delta
        for rel_path in new_files_in_delta:
             dest_path = os.path.join(output_dir, rel_path)
             os.makedirs(os.path.dirname(dest_path), exist_ok=True)
             shutil.copy2(os.path.join(new_files_dir, rel_path), dest_path)
        
        # Patch modified files
        print("\nApplying patches...")
        newly_created_payload_tar = None
        for rel_path, change in metadata['changes'].items():
            if change['type'] == 'modified':
                print(f"  Patching: {rel_path}")
                old_file_path = os.path.join(old_dir, rel_path)
                patch_file_path = os.path.join(delta_dir, 'patches', change['patch'])
                output_file_path = os.path.join(output_dir, rel_path)
                
                old_meta, new_meta = change.get('old_meta',{}), change.get('new_meta',{})
                
                with tempfile.TemporaryDirectory(dir=work_dir) as patch_work_dir:
                    source_for_patching = old_file_path
                    if old_meta.get('compressed'):
                        source_for_patching = os.path.join(patch_work_dir, 'old.decomp')
                        decompress_gz(old_file_path, source_for_patching)
                    
                    new_temp_path = os.path.join(patch_work_dir, 'new.patched')
                    apply_xdelta_patch(source_for_patching, patch_file_path, new_temp_path)

                    if new_meta.get('compressed'):
                        if calculate_sha256(new_temp_path) != new_meta['decompressed_sha256']:
                            raise Exception(f"Checksum mismatch on patched content for {rel_path}")
                        compress_gz(new_temp_path, output_file_path)
                        if rel_path.startswith('data/'):
                            newly_created_payload_tar = new_temp_path
                    else: # Not compressed
                         shutil.move(new_temp_path, output_file_path)

        print("\nFinalizing artifact...")
        if newly_created_payload_tar:
            update_header_with_payload_checksum(output_dir, work_dir, newly_created_payload_tar, metadata.get('new_payload_checksum'))
        
        # Always regenerate the manifest last
        update_manifest_file(output_dir)

        print(f"\nCreating output Mender file: {output_mender}")
        with tarfile.open(output_mender, 'w') as tar:
            # Final packaging with correct order and structure
            for item in ['version', 'manifest', 'header.tar.gz']:
                tar.add(os.path.join(output_dir, item), arcname=item)
            data_dir = os.path.join(output_dir, 'data')
            if os.path.isdir(data_dir):
                for f in sorted(os.listdir(data_dir)):
                    tar.add(os.path.join(data_dir, f), arcname=os.path.join('data', f))
        
        print("\n✓ Delta patch applied and Mender artifact created successfully!")


def main():
    if len(sys.argv) != 4:
        print(f"Usage: python3 {sys.argv[0]} <old.mender> <delta.patch> <output.mender>")
        sys.exit(1)
    if shutil.which('xdelta3') is None:
        print("Error: xdelta3 is not installed or not in PATH.")
        sys.exit(1)
    try:
        apply_delta_patch(sys.argv[1], sys.argv[2], sys.argv[3])
    except Exception as e:
        print(f"\nError: {e}")
        sys.exit(1)

if __name__ == '__main__':
    main()
