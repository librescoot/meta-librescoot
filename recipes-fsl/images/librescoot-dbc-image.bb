DESCRIPTION = "LibreScoot DBC image"
LICENSE = "MIT"

inherit core-image

PLATFORM_FLAVOR    = "mx6qsabresd"

IMAGE_FEATURES += "\
    ${@bb.utils.contains('DISTRO_FEATURES', 'wayland', 'weston', \
       bb.utils.contains('DISTRO_FEATURES',     'x11', 'x11-base x11-sato', \
                                                       '', d), d)} \
"

IMAGE_FEATURES += " \
    debug-tweaks \
    ssh-server-dropbear \
    splash \
    hwcodecs \
"

CORE_IMAGE_EXTRA_INSTALL += " \
    dbc-netconfig \
    packagegroup-core-base-utils \
    firmwared \
    python3 \
    i2c-tools \
    python3-pyserial \
    python3-systemd \
    python3-dateutil \
    python3-pyyaml \
    python3-redis \
    dropbear \
    ioctl \
    bash \
    rsync \
    flutter-engine \
    flutter-wayland-client \
    scootui \
    firmwared \ 
    ${@bb.utils.contains('DISTRO_FEATURES', 'x11 wayland', \
                         'weston-xwayland xterm', '', d)} \
"


IMAGE_INSTALL:append = " libubootenv-bin firmware-imx-epdc"

