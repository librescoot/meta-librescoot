DESCRIPTION = "LibreScoot DBC image"
LICENSE = "MIT"

inherit core-image

PLATFORM_FLAVOR    = "mx6qsabresd"

SPLASH = "plymouth"

BAD_RECOMMENDATIONS += "busybox-syslog"

IMAGE_FEATURES += "\
    ${@bb.utils.contains('DISTRO_FEATURES', 'wayland', 'weston', \
       bb.utils.contains('DISTRO_FEATURES',     'x11', 'x11-base x11-sato', \
                                                       '', d), d)} \
"

IMAGE_FEATURES += " \
    debug-tweaks \
    package-management \
    ssh-server-dropbear \
    splash \
    hwcodecs \
"

CORE_IMAGE_EXTRA_INSTALL += " \
    dbc-netconfig \
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
    flutter-engine \
    scootui \
    onboot-service \
    version-service \
    update-service \
    brightness-reader \
    dbc-backlight-service \
    lsc \
    firmwared \
    screen \
    chrony \
    chronyc \
    xdg-user-dirs \
    sqlite3 \
    libsqlite3-dev \
    alsa-utils \
    htop \
    iotop \
    vim-common \
    tzdata \
    xdelta3 \
    valhalla \
    prime-server \
    libspatialite \
    flutter-drm-gbm-backend \
    flutter-video-player-plugin \
    carplay-service \
    ${@bb.utils.contains('DISTRO_FEATURES', 'x11 wayland', \
                         'weston-xwayland xterm', '', d)} \
"

IMAGE_INSTALL:append = " libubootenv-bin"
IMAGE_INSTALL:append = " plymouth plymouth-animation"

PACKAGE_EXCLUDE = "ofono neard"
