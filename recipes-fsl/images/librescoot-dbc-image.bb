DESCRIPTION = "LibreScoot DBC image"
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
IMAGE_INSTALL:append = " imx-overlay-alpha"
IMAGE_INSTALL:append:unu-dbc = " boot-assets"

PACKAGE_EXCLUDE = "ofono neard rpcbind xinetd"
BAD_RECOMMENDATIONS += "ofono neard rpcbind xinetd"

# ---------------------------------------------------------------------------
# Boot-time optimisations
# ---------------------------------------------------------------------------
ROOTFS_POSTPROCESS_COMMAND:append = " mask_ldconfig_service; optimize_fstab_for_boot;"

# The ld.so.cache is pre-built during image creation.  Running ldconfig on
# every boot wastes ~1.8 s on the i.MX6 and is unnecessary because shared
# libraries never change at runtime on this image.
mask_ldconfig_service() {
    ln -sf /dev/null ${IMAGE_ROOTFS}${systemd_system_unitdir}/ldconfig.service
}

# /uboot is only accessed during OTA updates — lazy-mount it via automount so
# it never blocks the boot path.  /data is needed early but should not stall
# the rest of boot if device discovery is slow; nofail lets local-fs.target
# proceed without waiting.  fsck pass is set to 0 for both (ext4 journal
# recovery and vfat integrity are sufficient for this use-case).
optimize_fstab_for_boot() {
    local fstab="${IMAGE_ROOTFS}${sysconfdir}/fstab"
    [ -f "$fstab" ] || return 0
    sed -i '/\/uboot/{
        s/\bdefaults\b/noauto,x-systemd.automount,nofail/
    }' "$fstab"
    sed -i '/[[:space:]]\/data[[:space:]]/{
        s/\bdefaults\b/defaults,nofail/
    }' "$fstab"
    # Set fsck pass (last field) to 0 for both
    sed -i -E '/(\/uboot|[[:space:]]\/data[[:space:]])/s/[0-9]+[[:space:]]*$/0/' "$fstab"
}
