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
    keycard-service \
    onboot-service \
    ioctl \
    i2c-tools \
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
# onboot-service is the unit that runs /data/onboot.sh, the coordinator the
# installer leaves behind to finish an install after the laptop is gone.
# Without it nothing runs the queued phases on this image, so an aborted run's
# rescue phase and any phase left by an unexpected reboot both sit inert.
#
# ioctl and i2c-tools are how the installer signals progress on the vehicle.
# /dev/pwm_led* speaks custom ioctls and exposes no sysfs or led_classdev, so
# ioctl is the only way to light a blinker; the LP5562 behind the dashboard LED
# hangs off i2c-2 on the MDB, so i2cset is the only way to drive that. Both
# ship in the full image and neither shipped here.
#
# That mattered from the moment the board stopped rebooting into the full image
# before the dashboard work: it now stays on this image for the whole
# autonomous half of an install, which is exactly the stretch where the user
# has unplugged the laptop and the LEDs are the only thing left to look at.
# Every LED call sends stderr to /dev/null, so the absence did not fail, it
# just went dark. Reported from a real flash: "after the replug, the DBC turned
# on but I see no visible progress otherwise".
#
# Together about 30 KB, plus libi2c.

# keycard-service is here for a different reason: to let the installer get the
# interactive work out of the way while it still has the user's attention.
#
# Cards land in /data and a BLE bond lives in the nRF52, so neither is touched
# by a later mender install or the reboot after it. That means both can be done
# against this image and survive into the full one, which is what lets the
# artifact upload and install run underneath the human instead of after them.
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
