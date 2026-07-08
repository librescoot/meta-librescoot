DESCRIPTION = "Librescoot DBC image"
LICENSE = "MIT"

inherit core-image

require librescoot-hwclock-seed.inc

PLATFORM_FLAVOR    = "mx6qsabresd"

BAD_RECOMMENDATIONS += "busybox-syslog"

IMAGE_FEATURES += " \
    allow-empty-password \
    allow-root-login \
    empty-root-password \
    post-install-logging \
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

# Mask getty on tty1 to prevent a console flash during the boot animation.
# Done as a rootfs postprocess (after package postinsts) rather than shipped in
# a package, because systemd 259's offline `systemctl preset-all` errors out if
# the unit is already masked when it runs, failing do_rootfs.
mask_getty_tty1() {
    ln -sf /dev/null ${IMAGE_ROOTFS}${sysconfdir}/systemd/system/getty@tty1.service
}
# Run as a do_rootfs postfunc so it executes AFTER systemd_handle_machine_id
# (which runs `systemctl preset-all`); preset-all errors on an already-masked
# unit, so the mask must be applied only once preset-all has finished.
do_rootfs[postfuncs] += "mask_getty_tty1"
