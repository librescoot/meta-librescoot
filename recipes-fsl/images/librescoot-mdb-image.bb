DESCRIPTION = "LibreScoot MDB image"
LICENSE = "MIT"

inherit core-image

PLATFORM_FLAVOR    = "mx6ulevk"

IMAGE_FEATURES += " \
    debug-tweaks \
    ssh-server-dropbear \
"

CORE_IMAGE_EXTRA_INSTALL += " \
    wireguard-tools \
    u-boot-default-env \
    mdb-netconfig \
    modemmanager \
    networkmanager \
    networkmanager-nmcli \
    packagegroup-core-base-utils \
    firmwared \
    rpm \
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
    python3-click \
    redis \
    dropbear \
    nxp-nfc \
    libnfc \
    ioctl \
    bash \
    gpsd \
    gps-utils \
    gps-utils-python \
    gpsd-udev \
    rsync \
    curl \
    lsof \
    screen \
    libgpiod \
    libgpiod-tools \
    chrony \
    chronyc \
    ecu-service \
    vehicle-service \
    keycard-service \
    boot-led-service \
    battery-service \
    modem-service \
    onboot-service \
    bluetooth-service \
    version-service \
    pm-service \
    update-service \
    ums-service \
    settings-service \
    radio-gaga \
    iptables \
    mc \
    htop \
    iotop \
    vim-vimrc \
    nano \
    tzdata \
    xdelta3 \
    nrf-ble-driver \
    python3-pc-ble-driver-py \
    python3-nrfutil \
    python3-intelhex \
    python3-ecdsa \
    python3-libusb1 \
    python3-protobuf \
"

IMAGE_INSTALL:append = " libubootenv-bin"
IMAGE_INSTALL:append = " linux-firmware-imx-sdma-imx6q"

IMAGE_INSTALL:remove = "ofono"
