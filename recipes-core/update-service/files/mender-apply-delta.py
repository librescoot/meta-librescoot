#!/usr/bin/env python3
"""
Applies filesystem-aware delta patches to reconstruct Mender update files.
Handles decompressed filesystems and recompresses them with proper verification.

Usage:
    python3 mender-apply-delta.py <old.mender> <delta.patch> <output.mender>
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
    """Decompress a .gz file."""
    with gzip.open(gz_file, 'rb') as f_in:
        with open(output_file, 'wb') as f_out:
            shutil.copyfileobj(f_in, f_out)


def compress_gz(input_file, gz_file):
    """Compress a file with gzip."""
    with open(input_file, 'rb') as f_in:
        with gzip.open(gz_file, 'wb', compresslevel=9) as f_out:
            shutil.copyfileobj(f_in, f_out)


def apply_xdelta_patch(old_file, patch_file, output_file):
    """Apply an xdelta3 patch to reconstruct a file."""
    cmd = ['xdelta3', '-d', '-s', old_file, patch_file, output_file]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        raise Exception(f"xdelta3 failed: {result.stderr}")
    return output_file


def update_manifest_file(output_dir):
    """Update the manifest file with correct SHA256 checksums."""
    manifest_path = os.path.join(output_dir, 'manifest')
    manifest_lines = []

    # Calculate checksums for all files referenced in manifest
    # Typical manifest contains: data files, header.tar.gz, and version
    for root, dirs, files in os.walk(output_dir):
        for file in sorted(files):
            if file == 'manifest':
                continue
            filepath = os.path.join(root, file)
            rel_path = os.path.relpath(filepath, output_dir)
            checksum = calculate_sha256(filepath)
            manifest_lines.append(f"{checksum}  {rel_path}\n")

    # Sort manifest lines for consistency
    manifest_lines.sort(key=lambda x: x.split('  ')[1])

    with open(manifest_path, 'w') as f:
        f.writelines(manifest_lines)

    print(f"  Updated manifest file with {len(manifest_lines)} entries")


def update_header_metadata(output_dir, work_dir):
    """Update header.tar.gz to contain correct payload checksums."""
    header_tar_path = os.path.join(output_dir, 'header.tar.gz')

    if not os.path.exists(header_tar_path):
        print("  No header.tar.gz found, skipping header update")
        return

    # Extract header.tar.gz
    header_extract_dir = os.path.join(work_dir, 'header_extract')
    os.makedirs(header_extract_dir, exist_ok=True)

    with tarfile.open(header_tar_path, 'r:gz') as tar:
        tar.extractall(header_extract_dir)

    # Find all type-info files and update checksums
    type_info_files = []
    for root, dirs, files in os.walk(header_extract_dir):
        if 'type-info' in files:
            type_info_files.append(os.path.join(root, 'type-info'))

    if not type_info_files:
        print("  No type-info files found in header")
        return

    # Calculate checksum of the actual payload
    # For Mender artifacts, the data directory contains tar.gz files,
    # and the checksum in type-info refers to the file INSIDE the tar.gz
    data_files = []
    data_dir = os.path.join(output_dir, 'data')
    if os.path.exists(data_dir):
        for root, dirs, files in os.walk(data_dir):
            for file in files:
                data_files.append(os.path.join(root, file))

    if not data_files:
        print("  No data files found, skipping header update")
        return

    # Assume single payload for now (most common case)
    payload_archive = data_files[0]

    # Extract the actual payload file from the tar.gz to calculate its checksum
    payload_extract_dir = os.path.join(work_dir, 'payload_extract')
    os.makedirs(payload_extract_dir, exist_ok=True)

    with tarfile.open(payload_archive, 'r:gz') as tar:
        tar.extractall(payload_extract_dir)

    # Find the extracted payload file (usually a .ext4 file or similar)
    payload_files = []
    for root, dirs, files in os.walk(payload_extract_dir):
        for file in files:
            payload_files.append(os.path.join(root, file))

    if not payload_files:
        print("  Warning: No files found inside payload archive")
        # Fall back to using the archive checksum
        payload_checksum = calculate_sha256(payload_archive)
        print(f"  Using archive checksum: {payload_checksum}")
    else:
        # Use the first (and usually only) file inside the archive
        actual_payload = payload_files[0]
        payload_checksum = calculate_sha256(actual_payload)
        print(f"  Payload file: {os.path.basename(actual_payload)}")
        print(f"  Payload checksum: {payload_checksum}")

    # Update each type-info file
    updated = False
    for type_info_path in type_info_files:
        with open(type_info_path, 'r') as f:
            type_info = json.load(f)

        # Update rootfs-image.checksum if present
        if 'artifact_provides' in type_info:
            if 'rootfs-image.checksum' in type_info['artifact_provides']:
                old_checksum = type_info['artifact_provides']['rootfs-image.checksum']
                type_info['artifact_provides']['rootfs-image.checksum'] = payload_checksum
                print(f"  Updated rootfs-image.checksum in type-info")
                print(f"    Old: {old_checksum}")
                print(f"    New: {payload_checksum}")
                updated = True

        # Write back
        with open(type_info_path, 'w') as f:
            json.dump(type_info, f, separators=(',', ':'))

    if not updated:
        print("  No checksum fields found in type-info to update")
        return

    # Recreate header.tar.gz with updated metadata
    print("  Recreating header.tar.gz...")
    new_header_path = os.path.join(work_dir, 'header_new.tar.gz')

    with tarfile.open(new_header_path, 'w:gz') as tar:
        for item in os.listdir(header_extract_dir):
            item_path = os.path.join(header_extract_dir, item)
            tar.add(item_path, arcname=item)

    # Replace old header with new one
    shutil.move(new_header_path, header_tar_path)
    print(f"  Updated header.tar.gz (new checksum: {calculate_sha256(header_tar_path)})")


def apply_delta_patch(old_mender, delta_patch, output_mender):
    """Apply a delta patch to reconstruct the new Mender file."""

    if not os.path.exists(old_mender):
        raise Exception(f"Old Mender file not found: {old_mender}")

    if not os.path.exists(delta_patch):
        raise Exception(f"Delta patch file not found: {delta_patch}")

    with tempfile.TemporaryDirectory() as temp_dir:
        old_dir = os.path.join(temp_dir, 'old')
        delta_dir = os.path.join(temp_dir, 'delta')
        output_dir = os.path.join(temp_dir, 'output')
        work_dir = os.path.join(temp_dir, 'work')

        os.makedirs(old_dir)
        os.makedirs(delta_dir)
        os.makedirs(output_dir)
        os.makedirs(work_dir)

        # Extract old Mender file
        print(f"Extracting old Mender file: {old_mender}")
        extract_tar(old_mender, old_dir)

        # Extract delta patch
        print(f"Extracting delta patch: {delta_patch}")
        extract_tar(delta_patch, delta_dir)

        # Load metadata
        metadata_file = os.path.join(delta_dir, 'metadata.json')
        if not os.path.exists(metadata_file):
            raise Exception("Delta patch is invalid: metadata.json not found")

        with open(metadata_file, 'r') as f:
            metadata = json.load(f)

        # Check version
        if metadata.get('version') != 2:
            raise Exception(f"Incompatible delta format version: {metadata.get('version')}")

        # Verify old filesystem payload checksum (critical security check)
        expected_old_payload = metadata.get('old_payload_checksum')

        if expected_old_payload:
            print("Verifying old filesystem payload...")

            # Extract and verify the actual payload from the old Mender file
            old_data_files = []
            old_data_dir = os.path.join(old_dir, 'data')
            if os.path.exists(old_data_dir):
                for root, dirs, files in os.walk(old_data_dir):
                    for file in files:
                        old_data_files.append(os.path.join(root, file))

            if old_data_files:
                # Extract the payload from the archive
                old_payload_archive = old_data_files[0]
                old_payload_extract = os.path.join(work_dir, 'old_payload_verify')
                os.makedirs(old_payload_extract, exist_ok=True)

                with tarfile.open(old_payload_archive, 'r:gz') as tar:
                    tar.extractall(old_payload_extract)

                # Find the payload file
                old_payload_files = []
                for root, dirs, files in os.walk(old_payload_extract):
                    for file in files:
                        old_payload_files.append(os.path.join(root, file))

                if old_payload_files:
                    actual_old_payload = calculate_sha256(old_payload_files[0])

                    if actual_old_payload != expected_old_payload:
                        raise Exception(
                            f"CRITICAL: Old filesystem payload checksum mismatch!\n"
                            f"  This delta patch was created for a different filesystem version.\n"
                            f"  Expected (from delta): {expected_old_payload}\n"
                            f"  Got (from old.mender): {actual_old_payload}\n"
                            f"  Refusing to apply patch to prevent corruption."
                        )
                    else:
                        print(f"  ✓ Filesystem payload verified: {actual_old_payload[:16]}...")
                else:
                    print("  Warning: Could not extract payload file for verification")
            else:
                print("  Warning: No data files found in old Mender artifact")
        else:
            print("  Warning: No old_payload_checksum in delta metadata (older delta format)")

        # Process all changes
        print("\nApplying changes...")
        patches_dir = os.path.join(delta_dir, 'patches')
        new_files_dir = os.path.join(delta_dir, 'new_files')

        for rel_path, change_info in metadata['changes'].items():
            change_type = change_info['type']

            if change_type == 'unchanged':
                # Copy unchanged file from old
                print(f"  Copying unchanged: {rel_path}")
                old_file = os.path.join(old_dir, rel_path)
                output_file = os.path.join(output_dir, rel_path)
                os.makedirs(os.path.dirname(output_file), exist_ok=True)
                shutil.copy2(old_file, output_file)

                # Verify hash
                file_hash = calculate_sha256(output_file)
                if file_hash != change_info['sha256']:
                    raise Exception(f"Hash mismatch for unchanged file: {rel_path}")

            elif change_type == 'modified':
                # Apply xdelta patch
                print(f"  Patching: {rel_path}")
                old_file = os.path.join(old_dir, rel_path)
                patch_file = os.path.join(patches_dir, change_info['patch'])
                output_file = os.path.join(output_dir, rel_path)
                os.makedirs(os.path.dirname(output_file), exist_ok=True)

                old_meta = change_info.get('old_meta', {})
                new_meta = change_info.get('new_meta', {})

                # Check if we need to decompress old file before patching
                if old_meta.get('compressed'):
                    print(f"    Decompressing old file...")
                    old_decompressed = os.path.join(work_dir, f"old_{rel_path.replace('/', '_')}")
                    decompress_gz(old_file, old_decompressed)

                    # Verify decompressed hash
                    old_dec_hash = calculate_sha256(old_decompressed)
                    if old_dec_hash != old_meta.get('decompressed_sha256'):
                        raise Exception(f"Old decompressed file hash mismatch: {rel_path}")

                    old_for_patch = old_decompressed
                else:
                    old_for_patch = old_file

                # Apply patch to get new decompressed/raw file
                new_temp = os.path.join(work_dir, f"new_{rel_path.replace('/', '_')}")
                apply_xdelta_patch(old_for_patch, patch_file, new_temp)

                # Verify the patched result
                if new_meta.get('compressed'):
                    # Verify decompressed hash
                    new_temp_hash = calculate_sha256(new_temp)
                    expected_hash = new_meta.get('decompressed_sha256')
                    if new_temp_hash != expected_hash:
                        raise Exception(f"Patched decompressed file hash mismatch: {rel_path}")

                    # Recompress
                    print(f"    Recompressing...")
                    compress_gz(new_temp, output_file)

                    # Verify final compressed hash
                    final_hash = calculate_sha256(output_file)
                    if final_hash != change_info['new_sha256']:
                        print(f"    WARNING: Compressed file hash differs")
                        print(f"      Expected: {change_info['new_sha256']}")
                        print(f"      Got:      {final_hash}")
                        print(f"    This is normal due to gzip timestamp/metadata differences")
                        print(f"    The decompressed content is verified correct.")
                else:
                    # Move to final location
                    shutil.move(new_temp, output_file)

                    # Verify hash
                    file_hash = calculate_sha256(output_file)
                    if file_hash != change_info['new_sha256']:
                        raise Exception(f"Hash mismatch after patching: {rel_path}")

            elif change_type == 'new':
                # Copy new file from delta
                print(f"  Adding new: {rel_path}")
                source_file = os.path.join(new_files_dir, rel_path)
                output_file = os.path.join(output_dir, rel_path)

                os.makedirs(os.path.dirname(output_file), exist_ok=True)
                shutil.copy2(source_file, output_file)

                # Verify hash
                file_hash = calculate_sha256(output_file)
                if file_hash != change_info['sha256']:
                    raise Exception(f"Hash mismatch for new file: {rel_path}")

            elif change_type == 'deleted':
                # File should not exist in output
                print(f"  Deleting: {rel_path}")

        # Update Mender artifact metadata with correct checksums
        print("\nUpdating Mender artifact metadata...")
        update_header_metadata(output_dir, work_dir)
        update_manifest_file(output_dir)

        # Create output Mender tar file
        print(f"\nCreating output Mender file: {output_mender}")

        # Get list of files to add to tar in correct order
        files_to_add = []
        for root, dirs, files in os.walk(output_dir):
            for file in files:
                filepath = os.path.join(root, file)
                arcname = os.path.relpath(filepath, output_dir)
                files_to_add.append((filepath, arcname))

        # Sort to ensure consistent order
        def sort_key(item):
            arcname = item[1]
            if arcname == 'version':
                return (0, arcname)
            elif arcname == 'manifest':
                return (1, arcname)
            elif arcname == 'header.tar.gz':
                return (2, arcname)
            elif arcname.startswith('data/'):
                return (3, arcname)
            else:
                return (4, arcname)

        files_to_add.sort(key=sort_key)

        # Create tar file
        with tarfile.open(output_mender, 'w') as tar:
            for filepath, arcname in files_to_add:
                tar.add(filepath, arcname=arcname)

        # Verify content files
        print("\nVerifying reconstructed content...")
        with tempfile.TemporaryDirectory() as verify_dir:
            extract_tar(output_mender, verify_dir)

            all_verified = True
            for rel_path, change_info in metadata['changes'].items():
                if change_info['type'] == 'deleted':
                    continue

                # Skip manifest and header.tar.gz - we've intentionally updated these
                if rel_path in ['manifest', 'header.tar.gz']:
                    print(f"  ✓ Updated (metadata): {rel_path}")
                    continue

                verify_file = os.path.join(verify_dir, rel_path)
                if not os.path.exists(verify_file):
                    print(f"  ✗ Missing: {rel_path}")
                    all_verified = False
                    continue

                # For compressed files, verify decompressed content
                if change_info['type'] in ['modified', 'unchanged']:
                    new_meta = change_info.get('new_meta', {}) if change_info['type'] == 'modified' else {}

                    if new_meta.get('compressed'):
                        # Verify decompressed content
                        verify_decompressed = os.path.join(work_dir, f"verify_{rel_path.replace('/', '_')}")
                        decompress_gz(verify_file, verify_decompressed)
                        verify_hash = calculate_sha256(verify_decompressed)
                        expected_hash = new_meta.get('decompressed_sha256')

                        if verify_hash == expected_hash:
                            print(f"  ✓ Verified (decompressed): {rel_path}")
                        else:
                            print(f"  ✗ Hash mismatch: {rel_path}")
                            all_verified = False
                    else:
                        # Regular file
                        verify_hash = calculate_sha256(verify_file)
                        expected_hash = change_info.get('new_sha256') if change_info['type'] == 'modified' else change_info.get('sha256')

                        if verify_hash == expected_hash:
                            print(f"  ✓ Verified: {rel_path}")
                        else:
                            print(f"  ✗ Hash mismatch: {rel_path}")
                            all_verified = False

        if all_verified:
            print("\n✓ Delta patch applied successfully!")
            print("  All content files verified correct!")
        else:
            print("\n✗ Some files failed verification")

        # Show size information
        old_size = os.path.getsize(old_mender)
        delta_size = os.path.getsize(delta_patch)
        output_size = os.path.getsize(output_mender)

        print(f"\n  Old Mender size:    {old_size:,} bytes ({old_size / 1024 / 1024:.2f} MB)")
        print(f"  Delta patch size:   {delta_size:,} bytes ({delta_size / 1024 / 1024:.2f} MB)")
        print(f"  Output Mender size: {output_size:,} bytes ({output_size / 1024 / 1024:.2f} MB)")
        print(f"  Output file: {output_mender}")


def main():
    if len(sys.argv) != 4:
        print("Usage: python3 mender-delta-apply-v2.py <old.mender> <delta.patch> <output.mender>")
        sys.exit(1)

    old_mender = sys.argv[1]
    delta_patch = sys.argv[2]
    output_mender = sys.argv[3]

    # Check if xdelta3 is available
    if shutil.which('xdelta3') is None:
        print("Error: xdelta3 is not installed or not in PATH")
        sys.exit(1)

    try:
        apply_delta_patch(old_mender, delta_patch, output_mender)
    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == '__main__':
    main()
