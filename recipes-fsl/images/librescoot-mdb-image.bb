DESCRIPTION = "LibreScoot MDB image"
LICENSE = "MIT"

inherit core-image

require librescoot-hwclock-seed.inc

PLATFORM_FLAVOR    = "mx6ulevk"

BAD_RECOMMENDATIONS += "busybox-syslog"

IMAGE_FEATURES += " \
    debug-tweaks \
    ssh-server-dropbear \
"

CORE_IMAGE_EXTRA_INSTALL += " \
    wireguard-tools \
    u-boot-default-env \
    mdb-netconfig \
    ppp-link \
    modemmanager \
    networkmanager \
    networkmanager-nmcli \
    packagegroup-core-base-utils \
    firmwared \
    rpm \
    python3 \
    canutils \
    i2c-tools \
    python3-pyserial \
    python3-systemd \
    python3-dateutil \
    python3-pyyaml \
    python3-aioredis \
    python3-redis \
    python3-cbor2 \
    python3-crccheck \
    python3-typing-extensions \
    python3-click \
    redis \
    dropbear \
    ioctl \
    bash \
    gpsd \
    gps-utils \
    gps-utils-python \
    gpsd-udev \
    rsync \
    curl \
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
    machine-id-init \
    uboot-env-sync \
    bluetooth-service \
    version-service \
    pm-service \
    update-service \
    ums-service \
    settings-service \
    alarm-service \
    data-server \
    uplink-service \
    lsc \
    shell-config \
    radio-gaga \
    iptables \
    htop \
    iotop \
    lsof \
    screen \
    vim-common \
    tzdata \
    xdelta3 \
    nrf-ble-driver \
    python3-pc-ble-driver-py \
    python3-nrfutil \
    python3-intelhex \
    python3-ecdsa \
    python3-libusb1 \
    python3-protobuf \
    udev-rules-mdb \
    g-ether-conf \
    nano \
"

IMAGE_INSTALL:append = " libubootenv-bin"
IMAGE_INSTALL:append = " bmap-writer"
IMAGE_INSTALL:append = " linux-firmware-imx-sdma-imx6q"
IMAGE_INSTALL:append = " systemd-journal-upload"
## TODO: boot-assets adds ~5.5MB which exceeds MDB partition size.
## Needs MENDER_STORAGE_TOTAL_SIZE_MB increase before enabling.
#IMAGE_INSTALL:append:unu-mdb = " boot-assets"

IMAGE_INSTALL:remove = "ofono"
