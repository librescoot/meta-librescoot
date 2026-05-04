DESCRIPTION = "Librescoot DBC image"
LICENSE = "MIT"

inherit core-image

require librescoot-hwclock-seed.inc

PLATFORM_FLAVOR    = "mx6qsabresd"

BAD_RECOMMENDATIONS += "busybox-syslog"

IMAGE_FEATURES += " \
    debug-tweaks \
    ssh-server-dropbear \
    hwcodecs \
"

CORE_IMAGE_EXTRA_INSTALL += " \
    dbc-netconfig \
    ppp-link \
    u-boot-default-env \
    packagegroup-core-base-utils \
    firmwared \
    rpm \
    python3 \
    i2c-tools \
    python3-pyserial \
    python3-systemd \
    python3-dateutil \
    python3-pyyaml \
    python3-aioredis \
    python3-redis \
    redis \
    python3-cbor2 \
    python3-smbus2 \
    python3-typing-extensions \
    python3-shapely \
    dropbear \
    ioctl \
    bash \
    rsync \
    curl \
    dbc-dispatcher \
    onboot-service \
    data-server \
    machine-id-init \
    uboot-env-sync \
    version-service \
    update-service \
    dbc-backlight-service \
    lsc \
    shell-config \
    screen \
    chrony \
    chronyc \
    sqlite3 \
    htop \
    iotop \
    vim-tiny \
    tzdata \
    xdelta3 \
    valhalla \
    prime-server \
    libspatialite \
    ffmpeg \
    scootui-qt \
    scootui-tui \
    glmark2 \
    nano \
    hiredis \
"

IMAGE_INSTALL:append = " libubootenv-bin"
IMAGE_INSTALL:append = " boot-animation"
IMAGE_INSTALL:append:unu-dbc = " imx-overlay-alpha"
IMAGE_INSTALL:append:unu-dbc = " drm-holder"
IMAGE_INSTALL:append = " systemd-journal-upload"
IMAGE_INSTALL:append:unu-dbc = " boot-assets"

PACKAGE_EXCLUDE = "ofono neard"
