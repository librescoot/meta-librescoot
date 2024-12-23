DESCRIPTION = "LibreScoot DBC image"
LICENSE = "MIT"

inherit core-image

PLATFORM_FLAVOR    = "mx6qsabresd"

IMAGE_FEATURES += "\
    ${@bb.utils.contains('DISTRO_FEATURES', 'wayland', 'weston', \
       bb.utils.contains('DISTRO_FEATURES',     'x11', 'x11-base', \
                                                       '', d), d)} \
"

IMAGE_FEATURES += " \
    debug-tweaks \
    ssh-server-dropbear \
    splash \
    hwcodecs \
"

CORE_IMAGE_EXTRA_INSTALL += " \
    mdb-netconfig \
    packagegroup-core-base-utils \
    firmwared \
    python3 \
    python3-can \
    python3-numpy \
    canutils \
    i2c-tools \
    python3-pyserial \
    python3-systemd \
    python3-dateutil \
    python3-pyyaml \
    python3-redis \
    redis \
    dropbear \
    nxp-nfc \
    ioctl \
    bash \
    gpsd \
    rsync \
    packagegroup-fsl-tools-gpu \
    flutter-engine \
    flutter-wayland-client \
    firmwared \ 
    ${@bb.utils.contains('DISTRO_FEATURES', 'wayland', \
                         'weston weston-init weston-examples \
                          gtk+3-demo', '', d)} \
    ${@bb.utils.contains('DISTRO_FEATURES', 'x11 wayland', \
                         'weston-xwayland xterm', '', d)} \
"

IMAGE_INSTALL:append = " libubootenv-bin"
