DESCRIPTION = "Librescoot DBC minimal bootstrap image"
LICENSE = "MIT"

inherit core-image

require librescoot-hwclock-seed.inc

BAD_RECOMMENDATIONS += "busybox-syslog"

# Bootstrap image for initial installation: boots fast, brings up the
# MDB link, and receives a full .mender via data-server which the
# installer then applies with mender-update install. Partition layout,
# bootloader and mender client are inherited from the machine config
# and must stay identical to the full image so the .mender fits rootfs
# slot B.

IMAGE_FEATURES += " \
    allow-empty-password \
    allow-root-login \
    empty-root-password \
    post-install-logging \
    ssh-server-dropbear \
"

CORE_IMAGE_EXTRA_INSTALL += " \
    u-boot-default-env \
    dbc-netconfig \
    machine-id-init \
    uboot-env-sync \
    data-server \
    dropbear \
    zstd \
"

# zstd is here for the installer, not for this image's own use: routing tiles
# ship as .tar.zst and are unpacked on the dashboard. Doing that while the
# board still runs this image keeps it working regardless of which firmware
# version is being installed on top; the full image only grew zstd in
# 2026-08-09, so a target older than that cannot unpack them itself.

IMAGE_INSTALL:append = " libubootenv-bin"

PACKAGE_EXCLUDE = "ofono neard"

# Mark this as a bootstrap image, two ways, because two different consumers have
# to tell it apart from the full image of the same release and neither can today.
# Both images share DISTRO and MACHINE, so os-release is built once and shared
# between them, and VERSION_ID and VARIANT_ID are load-bearing elsewhere:
# update-service builds the release asset name from VARIANT_ID, and VERSION_ID is
# compared by update-service, lsc, scootui-qt and the installer. Neither can be
# reused to carry this.
#
# The artifact name is what `mender-update show-artifact` reports. The installer
# keys its bootstrap-vs-full detection on this containing "minimal", and without
# it a post-install check cannot tell a freshly bootstrapped board from one the
# artifact was actually applied to, because both report the same version.
MENDER_ARTIFACT_NAME = "release-${LIBRESCOOT_VERSION}-minimal"

# IMAGE_ID is the os-release(5) field for a specific OS image, unused elsewhere
# in this project. The trampoline reads the DBC's /etc/os-release over ssh and
# has no mender access there, so the artifact name alone never reaches it.
# version-service mirrors every os-release line into redis with no allowlist, so
# this also lands as version:dbc[image_id] at no extra cost.
ROOTFS_POSTPROCESS_COMMAND:append = " librescoot_mark_bootstrap_image;"
librescoot_mark_bootstrap_image() {
    echo "IMAGE_ID=librescoot-dbc-bootstrap" >> ${IMAGE_ROOTFS}/usr/lib/os-release
}
