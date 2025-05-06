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
    python3-cbor2 \
    python3-smbus2 \
    python3-typing-extensions \
    dropbear \
    ioctl \
    bash \
    rsync \
    flutter-engine \
    flutter-wayland-client \
    scootui \
    onboot-service \
    version-service \
    smut \
    firmwared \ 
    screen \
    brouter \
    chrony \
    chronyc \
    xdg-user-dirs \
    sqlite3 \
    libsqlite3-dev \
    alsa-utils \
    mc \
    htop \
    vim-vimrc \
    nano \
    ${@bb.utils.contains('DISTRO_FEATURES', 'x11 wayland', \
                         'weston-xwayland xterm', '', d)} \
"

IMAGE_INSTALL:append = " libubootenv-bin firmware-imx-epdc"
IMAGE_INSTALL:append = " plymouth plymouth-animation"
