DESCRIPTION = "LibreScoot DBC (Dashboard Controller Board) image"
LICENSE = "MIT"

inherit core-image

require librescoot-hwclock-seed.inc

PLATFORM_FLAVOR = "mx6qsabresd"

BAD_RECOMMENDATIONS += "busybox-syslog"

IMAGE_FEATURES += " \
    debug-tweaks \
    ssh-server-dropbear \
    hwcodecs \
"

CORE_IMAGE_EXTRA_INSTALL += " \
    packagegroup-librescoot-base \
    packagegroup-librescoot-services \
    packagegroup-librescoot-connectivity \
    packagegroup-librescoot-python \
    packagegroup-librescoot-navigation \
    packagegroup-librescoot-debug \
    ioctl \
    sqlite3 \
    ffmpeg \
    nano \
"

# Packages added on scarthgap not yet in a packagegroup
IMAGE_INSTALL:append = " ppp-link dbc-dispatcher data-server machine-id-init uboot-env-sync"
IMAGE_INSTALL:append = " scootui-qt scootui-tui glmark2 hiredis"
IMAGE_INSTALL:append = " boot-animation"
IMAGE_INSTALL:append:unu-dbc = " imx-overlay-alpha"
IMAGE_INSTALL:append:unu-dbc = " drm-holder"
IMAGE_INSTALL:append = " systemd-journal-upload"
IMAGE_INSTALL:append:unu-dbc = " boot-assets"

PACKAGE_EXCLUDE = "ofono neard"
