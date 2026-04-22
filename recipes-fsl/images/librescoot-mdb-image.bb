DESCRIPTION = "LibreScoot MDB (Main Driver Board) image"
LICENSE = "MIT"

inherit core-image

require librescoot-hwclock-seed.inc

PLATFORM_FLAVOR = "mx6ulevk"

BAD_RECOMMENDATIONS += "busybox-syslog"

IMAGE_FEATURES += " \
    debug-tweaks \
    ssh-server-dropbear \
"

CORE_IMAGE_EXTRA_INSTALL += " \
    packagegroup-librescoot-base \
    packagegroup-librescoot-services \
    packagegroup-librescoot-connectivity \
    packagegroup-librescoot-python \
    packagegroup-librescoot-navigation \
    packagegroup-librescoot-debug \
    ioctl \
    udev-rules-mdb \
    g-ether-conf \
    nano \
"

# Packages added on scarthgap not yet in a packagegroup
IMAGE_INSTALL:append = " ppp-link machine-id-init uboot-env-sync data-server bmap-writer"

IMAGE_INSTALL:append = " linux-firmware-imx-sdma-imx6q"
IMAGE_INSTALL:append = " systemd-journal-upload"
## TODO: boot-assets adds ~5.5MB which exceeds MDB partition size.
## Needs MENDER_STORAGE_TOTAL_SIZE_MB increase before enabling.
#IMAGE_INSTALL:append:unu-mdb = " boot-assets"

IMAGE_INSTALL:remove = "ofono"
