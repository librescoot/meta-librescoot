DESCRIPTION = "LibreScoot DBC image"
LICENSE = "MIT"

inherit core-image

PLATFORM_FLAVOR    = "mx6qsabresd"

IMAGE_FEATURES += " \
    debug-tweaks \
    ssh-server-dropbear \
    splash \
    hwcodecs \
    wayland \
    weston
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
    firmwared
"

IMAGE_INSTALL:append = " libubootenv-bin"
