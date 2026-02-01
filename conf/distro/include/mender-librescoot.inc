# Common Mender OTA configuration for all LibreScoot machines
#
# Machine-specific variables (MENDER_STORAGE_DEVICE, partition sizes, etc.)
# must still be set in the machine configuration files, as they depend on
# the actual hardware and storage layout.
#
# This file contains the common Mender policy shared across all targets.

# Use U-Boot integration (all current LibreScoot boards use U-Boot)
MENDER_FEATURES_ENABLE:append = " mender-uboot mender-image-sd"
MENDER_FEATURES_DISABLE:append = " mender-grub mender-image-uefi"

# Common partition filesystem type
MENDER_DATA_PART_FSTYPE = "ext4"
