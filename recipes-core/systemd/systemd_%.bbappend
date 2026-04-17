PACKAGECONFIG:remove:pn-systemd = "timesyncd"

# logind is only dropped on DBC — MDB needs it so battery-service (and others)
# can acquire org.freedesktop.login1.Manager.Inhibit suspend-inhibitor locks.
PACKAGECONFIG:remove:pn-systemd:unu-dbc = "logind"
PACKAGECONFIG:remove:pn-systemd:librescoot-dbc-rpi4 = "logind"

FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

SRC_URI += "file://journald.conf"
SRC_URI += "file://00-create-volatile.conf"
SRC_URI:append:unu-dbc = " file://99-etnaviv-no-autosuspend.rules"

do_install:append() {
    install -d ${D}${sysconfdir}/systemd
    install -m 0644 ${WORKDIR}/journald.conf ${D}${sysconfdir}/systemd/journald.conf

    # Override the default 00-create-volatile.conf to avoid duplicate /run/lock
    install -m 0644 ${WORKDIR}/00-create-volatile.conf ${D}${libdir}/tmpfiles.d/00-create-volatile.conf
}

# Drop udev rules that don't match any hardware on unu-dbc. Systemd's udev
# installs its rules under ${nonarch_base_libdir}/udev/rules.d/ — these are
# the same files the (unused) eudev bbappend was trying to clean up.
do_install:append:unu-dbc() {
    install -d ${D}${sysconfdir}/udev/rules.d
    install -m 0644 ${WORKDIR}/99-etnaviv-no-autosuspend.rules \
        ${D}${sysconfdir}/udev/rules.d/

    # Power management (DBC is plugged into MDB, no battery)
    rm -f ${D}${nonarch_base_libdir}/udev/rules.d/60-autosuspend.rules

    # Storage: no CD/DVD, no tape, no btrfs
    rm -f ${D}${nonarch_base_libdir}/udev/rules.d/60-cdrom_id.rules
    rm -f ${D}${nonarch_base_libdir}/udev/rules.d/60-persistent-storage-tape.rules
    rm -f ${D}${nonarch_base_libdir}/udev/rules.d/64-btrfs.rules

    # Storage: 60-persistent-storage.rules runs blkid on every block device to
    # populate /dev/disk/by-uuid/* and friends. DBC's fstab uses /dev/mmcblkNpN
    # paths directly and no image recipe references by-uuid anywhere, so the
    # probe is pure cost on each mmc event.
    rm -f ${D}${nonarch_base_libdir}/udev/rules.d/60-persistent-storage.rules

    # Security: no smart cards, no FIDO tokens
    rm -f ${D}${nonarch_base_libdir}/udev/rules.d/60-fido-id.rules

    # Network: no InfiniBand
    rm -f ${D}${nonarch_base_libdir}/udev/rules.d/60-infiniband.rules

    # Input: touch only, no HID
    rm -f ${D}${nonarch_base_libdir}/udev/rules.d/70-joystick.rules
    rm -f ${D}${nonarch_base_libdir}/udev/rules.d/70-mouse.rules
    rm -f ${D}${nonarch_base_libdir}/udev/rules.d/70-touchpad.rules
    rm -f ${D}${nonarch_base_libdir}/udev/rules.d/80-libinput-device-groups.rules
    rm -f ${D}${nonarch_base_libdir}/udev/rules.d/90-libinput-fuzz-override.rules

    # Memory: no RAM testing hardware
    rm -f ${D}${nonarch_base_libdir}/udev/rules.d/70-memory.rules

    # Camera: CSI ports present but unused
    rm -f ${D}${nonarch_base_libdir}/udev/rules.d/70-camera.rules

    # Audio: ALSA state is not persisted across boots
    rm -f ${D}${nonarch_base_libdir}/udev/rules.d/90-alsa-restore.rules

    # Multi-seat desktop: DBC is single-user embedded
    rm -f ${D}${nonarch_base_libdir}/udev/rules.d/71-seat.rules
    rm -f ${D}${nonarch_base_libdir}/udev/rules.d/73-seat-late.rules

    # Network configuration is done by systemd-networkd directly
    rm -f ${D}${nonarch_base_libdir}/udev/rules.d/75-net-description.rules
    rm -f ${D}${nonarch_base_libdir}/udev/rules.d/81-net-dhcp.rules

    # I/O cost: eMMC doesn't support CFQ iocost scheduling
    rm -f ${D}${nonarch_base_libdir}/udev/rules.d/90-iocost.rules
}
