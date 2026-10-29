DESCRIPTION = "Librescoot MDB image"
LICENSE = "CC-BY-NC-SA-4.0"

inherit core-image

# Mender supplies the fixed 512 MiB floor; keep sizing headroom within it.
IMAGE_OVERHEAD_FACTOR = "1.15"

# 32768 inodes leave over three times the installed file count without a 32 MiB
# inode table. The separate data image uses MENDER_DATA_PART_FSOPTS instead.
# Image-type options must be set before the image tasks are generated.
EXTRA_IMAGECMD:ext4 = "-i 16384"

python check_mdb_rootfs_headroom() {
    import os
    import subprocess

    image = os.path.join(d.getVar('IMGDEPLOYDIR'), d.getVar('IMAGE_NAME') + '.ext4')
    output = subprocess.check_output(['dumpe2fs', '-h', image], text=True,
                                     env=dict(os.environ, LC_ALL='C'))
    fields = dict(line.split(':', 1) for line in output.splitlines() if ':' in line)
    available = (int(fields['Free blocks']) - int(fields['Reserved block count'])) * int(fields['Block size'])
    minimum = 16 * 1024 * 1024
    if available < minimum:
        bb.fatal('MDB rootfs has %d unreserved bytes free; at least %d required' % (available, minimum))
    bb.note('MDB rootfs has %d unreserved bytes free' % available)
}
do_image_ext4[postfuncs] += "check_mdb_rootfs_headroom"

require librescoot-hwclock-seed.inc

LIBRESCOOT_OTA_COMPONENT = "mdb"
require librescoot-ota-seed.inc

PLATFORM_FLAVOR    = "mx6ulevk"

BAD_RECOMMENDATIONS += "busybox-syslog"

IMAGE_FEATURES += " \
    allow-empty-password \
    allow-root-login \
    empty-root-password \
    post-install-logging \
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
    valkey \
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
    event-service \
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
    trip-service \
    alarm-service \
    motion-service \
    data-server \
    uplink-service \
    lsc \
    shell-config \
    radio-gaga \
    connect-service \
    iptables \
    iproute2-tc \
    htop \
    iotop \
    lsof \
    screen \
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
    vim-tiny \
    nano \
"

IMAGE_INSTALL:append = " libubootenv-bin"
IMAGE_INSTALL:append = " bmap-writer"
IMAGE_INSTALL:append = " systemd-journal-upload"
IMAGE_INSTALL:append:unu-mdb = " boot-assets"

IMAGE_INSTALL:remove = "ofono"

# The unit ships with a baked-in enable that preset processing leaves in
# place; with no scooter.logserver set it crash-loops on the placeholder URL.
strip_journal_upload_enable() {
    rm -f ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/multi-user.target.wants/systemd-journal-upload.service
}
do_rootfs[postfuncs] += "strip_journal_upload_enable"
