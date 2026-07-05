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
    debug-tweaks \
    ssh-server-dropbear \
"

CORE_IMAGE_EXTRA_INSTALL += " \
    u-boot-default-env \
    dbc-netconfig \
    machine-id-init \
    uboot-env-sync \
    data-server \
    dropbear \
"

IMAGE_INSTALL:append = " libubootenv-bin"

PACKAGE_EXCLUDE = "ofono neard"
