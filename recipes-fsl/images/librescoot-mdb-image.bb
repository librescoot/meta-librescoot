DESCRIPTION = "LibreScoot MDB image"
LICENSE = "MIT"

inherit core-image

PLATFORM_FLAVOR    = "mx6ulevk"

IMAGE_FEATURES += " \
    debug-tweaks \
    ssh-server-dropbear \
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
    python3-aioredis \
    python3-redis \
    python3-cbor2 \
    python3-smbus2 \
    python3-typing-extensions \
    redis \
    dropbear \
    nxp-nfc \
    ioctl \
    bash \
    gpsd \
    rsync \
    lsof \
    screen \
"

IMAGE_INSTALL:append = " libubootenv-bin"
IMAGE_INSTALL:append = " linux-firmware-imx-sdma-imx6q"
