DESCRIPTION = "Librescoot MDB minimal bootstrap image"
LICENSE = "MIT"

inherit core-image

require librescoot-hwclock-seed.inc

BAD_RECOMMENDATIONS += "busybox-syslog"

# Bootstrap image for initial installation: boots fast, brings up USB
# ethernet and the DBC link, and receives a full .mender via data-server
# which the installer then applies with mender-update install. Partition
# layout, bootloader and mender client are inherited from the machine
# config and must stay identical to the full image so the .mender fits
# rootfs slot B.

IMAGE_FEATURES += " \
    allow-empty-password \
    allow-root-login \
    empty-root-password \
    post-install-logging \
    ssh-server-dropbear \
"

CORE_IMAGE_EXTRA_INSTALL += " \
    u-boot-default-env \
    mdb-netconfig \
    g-ether-conf \
    machine-id-init \
    uboot-env-sync \
    data-server \
    dropbear \
    bmap-writer \
    valkey \
    bluetooth-service \
"

# valkey and bluetooth-service are here for the nRF52, not for Bluetooth.
#
# After any reboot the nRF52 initiates, it arms a second power cycle two
# minutes later unless Linux checks in over USOCK, on the assumption that an
# iMX6 which has not spoken by then never came back. bluetooth-service is what
# speaks. Without it a board that lands on this image gets power-cycled every
# two minutes, forever: observed on a vehicle, USB device number climbing once
# per cycle until the AUX pole came off.
#
# That makes the pair load-bearing for the installer's brake-lever restart and
# for the BLE hard-reboot, both of which go through the same nRF path and both
# of which are how the flow avoids asking the user to open the seatbox and
# unbolt a battery. bluetooth-service exits with FATAL when it cannot reach a
# datastore, before it ever opens the UART, so valkey is not optional here.
#
# Around 10 MB on a 170 MB image, or about five seconds of flash time.

IMAGE_INSTALL:append = " libubootenv-bin"
IMAGE_INSTALL:append = " linux-firmware-imx-sdma-imx6q"

IMAGE_INSTALL:remove = "ofono"

# Mark this as a bootstrap image, two ways, because two different consumers have
# to tell it apart from the full image of the same release and neither can today.
# Both images share DISTRO and MACHINE, so os-release is built once and shared
# between them, and VERSION_ID and VARIANT_ID are load-bearing elsewhere:
# update-service builds the release asset name from VARIANT_ID, and VERSION_ID is
# compared by update-service, lsc, scootui-qt and the installer. Neither can be
# reused to carry this.
#
# The artifact name is what `mender-update show-artifact` reports. The installer
# keys its bootstrap-vs-full detection on this containing "minimal", and without
# it a post-install check cannot tell a freshly bootstrapped board from one the
# artifact was actually applied to, because both report the same version.
MENDER_ARTIFACT_NAME = "release-${LIBRESCOOT_VERSION}-minimal"

# IMAGE_ID is the os-release(5) field for a specific OS image, unused elsewhere
# in this project. The trampoline reads the DBC's /etc/os-release over ssh and
# has no mender access there, so the artifact name alone never reaches it.
# version-service mirrors every os-release line into redis with no allowlist, so
# this also lands as version:mdb[image_id] at no extra cost.
# meta-mender ships /data/mender/bootstrap.mender and expects mender-update
# daemon to install and commit it at startup, which is how a board arrives at a
# datastore that names its own artifact. This image runs no daemon, so the
# first thing that ever touches mender is the installer's own `mender-update
# install`, which does the bootstrap inline: it writes the bootstrap Artifact
# instead of the payload, reports 100%, exits 0, and leaves the transaction
# open. The board then reboots onto the same slot, still running this image,
# and every retry dies with "Update already in progress".
#
# So do what the daemon would, once, before anything else can. commit falls
# back to rollback because ArtifactCommit's state script exits 1 on this
# rootfs; either outcome closes the transaction, which is all that is needed.
ROOTFS_POSTPROCESS_COMMAND:append = " librescoot_seed_mender_bootstrap;"
librescoot_seed_mender_bootstrap() {
    install -d ${IMAGE_ROOTFS}${systemd_system_unitdir}
    cat > ${IMAGE_ROOTFS}${systemd_system_unitdir}/mender-bootstrap-seed.service <<'EOF'
[Unit]
Description=Resolve the mender bootstrap Artifact before anything installs
ConditionPathExists=/data/mender/bootstrap.mender
After=local-fs.target
Before=librescoot-update.service

[Service]
Type=oneshot
RemainAfterExit=yes
ExecStart=/bin/sh -c 'mender-update install /data/mender/bootstrap.mender; mender-update commit || mender-update rollback || true'

[Install]
WantedBy=multi-user.target
EOF
    install -d ${IMAGE_ROOTFS}${systemd_system_unitdir}/multi-user.target.wants
    ln -sf ../mender-bootstrap-seed.service \
        ${IMAGE_ROOTFS}${systemd_system_unitdir}/multi-user.target.wants/mender-bootstrap-seed.service
}

ROOTFS_POSTPROCESS_COMMAND:append = " librescoot_mark_bootstrap_image;"
librescoot_mark_bootstrap_image() {
    echo "IMAGE_ID=librescoot-mdb-bootstrap" >> ${IMAGE_ROOTFS}/usr/lib/os-release
}
