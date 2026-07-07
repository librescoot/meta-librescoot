DESCRIPTION = "Librescoot MDB minimal bootstrap image"
LICENSE = "MIT"

inherit core-image

require librescoot-hwclock-seed.inc

BAD_RECOMMENDATIONS += "busybox-syslog"

# Bootstrap image for initial installation: boots fast, brings up USB
# ethernet and the DBC link, and receives a full .mender via data-server
# which the installer then applies with mender-update install. Partition
# layout, bootloader and mender client are inherited from the machine
# config and must stay identical to the full image so the .mender fits
# rootfs slot B.

IMAGE_FEATURES += " \
    allow-empty-password \
    allow-root-login \
    empty-root-password \
    post-install-logging \
    ssh-server-dropbear \
"

CORE_IMAGE_EXTRA_INSTALL += " \
    u-boot-default-env \
    mdb-netconfig \
    g-ether-conf \
    machine-id-init \
    uboot-env-sync \
    data-server \
    dropbear \
    bmap-writer \
"

IMAGE_INSTALL:append = " libubootenv-bin"
IMAGE_INSTALL:append = " linux-firmware-imx-sdma-imx6q"

IMAGE_INSTALL:remove = "ofono"
