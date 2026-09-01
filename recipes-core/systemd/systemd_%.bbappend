PACKAGECONFIG:remove:pn-systemd = "timesyncd"

# logind is only dropped on DBC — MDB needs it so battery-service (and others)
# can acquire org.freedesktop.login1.Manager.Inhibit suspend-inhibitor locks.
PACKAGECONFIG:remove:pn-systemd:unu-dbc = "logind"
PACKAGECONFIG:remove:pn-systemd:librescoot-dbc-rpi4 = "logind"

PACKAGECONFIG:append:pn-systemd = " journal-upload microhttpd openssl pstore"

FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

SRC_URI += "file://journald-librescoot.conf"
SRC_URI += "file://00-create-volatile.conf"
SRC_URI += "file://var.conf"
SRC_URI += "file://tmp.conf"
SRC_URI += "file://journal-upload.conf"
SRC_URI += "file://journal-upload-volatile-state.conf"
SRC_URI += "file://90-journal-upload.preset"
SRC_URI += "file://librescoot-journal-persistent.service"
SRC_URI += "file://90-journal-persistent.preset"
SRC_URI += "file://networkd-wait-online-timeout.conf"
SRC_URI:append:unu-dbc = " file://99-etnaviv-no-autosuspend.rules"
SRC_URI:append:unu-dbc = " file://dbc-data-clean.service"

do_install:append() {
    install -d ${D}${sysconfdir}/systemd
    install -m 0644 ${UNPACKDIR}/journal-upload.conf ${D}${sysconfdir}/systemd/journal-upload.conf

    # Drop-in: our journald settings cannot live in journald.conf. The
    # systemd-conf package ships /usr/lib/systemd/journald.conf.d/00-systemd-conf.conf,
    # and drop-ins beat the main file on every key they share. Drop-ins sort by
    # filename across all directories, so 10- wins over 00-. See the file for why
    # RuntimeMaxUse has to be repeated here.
    install -d ${D}${sysconfdir}/systemd/journald.conf.d
    install -m 0644 ${UNPACKDIR}/journald-librescoot.conf \
        ${D}${sysconfdir}/systemd/journald.conf.d/10-librescoot.conf

    # Override the default 00-create-volatile.conf to avoid duplicate /run/lock
    install -m 0644 ${UNPACKDIR}/00-create-volatile.conf ${D}${libdir}/tmpfiles.d/00-create-volatile.conf

    # Override stock /usr/lib/tmpfiles.d/{var,tmp}.conf via /etc so the 'd /var/log'
    # and 'q /var/tmp' entries don't error out on our volatile/ symlinks.
    install -d ${D}${sysconfdir}/tmpfiles.d
    install -m 0644 ${UNPACKDIR}/var.conf ${D}${sysconfdir}/tmpfiles.d/var.conf
    install -m 0644 ${UNPACKDIR}/tmp.conf ${D}${sysconfdir}/tmpfiles.d/tmp.conf

    # Preset: auto-enable systemd-journal-upload.service at first boot
    install -d ${D}${systemd_unitdir}/system-preset
    install -m 0644 ${UNPACKDIR}/90-journal-upload.preset ${D}${systemd_unitdir}/system-preset/90-journal-upload.preset
    install -m 0644 ${UNPACKDIR}/90-journal-persistent.preset ${D}${systemd_unitdir}/system-preset/90-journal-persistent.preset

    # Drop-in: redirect journal-upload's state directory to tmpfs so the
    # cursor file doesn't wear out the eMMC. Lives under vendor
    # /usr/lib/systemd/system so admins can still override via /etc.
    install -d ${D}${systemd_unitdir}/system/systemd-journal-upload.service.d
    install -m 0644 ${UNPACKDIR}/journal-upload-volatile-state.conf \
        ${D}${systemd_unitdir}/system/systemd-journal-upload.service.d/volatile-state.conf

    # Drop-in: bound systemd-networkd-wait-online so an absent inter-board link
    # slows the boot instead of stopping it. Unbounded, the DBC never reaches
    # multi-user.target when usb0 is missing — no dropbear, no update-service,
    # so a pending mender update can never commit. See the file for detail.
    install -d ${D}${systemd_unitdir}/system/systemd-networkd-wait-online.service.d
    install -m 0644 ${UNPACKDIR}/networkd-wait-online-timeout.conf \
        ${D}${systemd_unitdir}/system/systemd-networkd-wait-online.service.d/timeout.conf

    # Opt-in persistent journal: when /data/settings/journald-persistent exists,
    # symlink /var/log/journal -> /data/journal so journald (Storage=auto)
    # persists logs to the data partition. No marker = volatile, no wear.
    install -d ${D}${systemd_unitdir}/system
    install -m 0644 ${UNPACKDIR}/librescoot-journal-persistent.service \
        ${D}${systemd_unitdir}/system/librescoot-journal-persistent.service
}

# MDB fstab and runtime configuration address every partition by its stable
# /dev/mmcblk1pN name. Nothing consumes /dev/disk/by-* links, so probing every
# block device with blkid only delays /data and the services ordered after it.
# A five-boot hardware trial reduced median /data readiness by 1.61 seconds.
#
# MDB runs a single root session with no desktop graphics, multiseat, or
# non-root interactive user. 71-seat.rules tags input, sound, graphics,
# backlight, and USB devices with seat/uaccess ACLs that are never consumed.
# Removing it saves ~0.3s across valkey, Bluetooth, vehicle, battery, ECU,
# and PM service startups. Five candidate boots passed with all services
# healthy, GPIO input by-path still present, CAN up, modem active, and no
# failed units. U-Boot rollback was exercised before landing.
do_install:append:unu-mdb() {
    rm -f ${D}${nonarch_base_libdir}/udev/rules.d/60-persistent-storage.rules
    rm -f ${D}${nonarch_base_libdir}/udev/rules.d/71-seat.rules
}

# Drop udev rules that don't match any hardware on unu-dbc. Systemd's udev
# installs its rules under ${nonarch_base_libdir}/udev/rules.d/ — these are
# the same files the (unused) eudev bbappend was trying to clean up.
do_install:append:unu-dbc() {
    install -d ${D}${sysconfdir}/udev/rules.d
    install -m 0644 ${UNPACKDIR}/99-etnaviv-no-autosuspend.rules \
        ${D}${sysconfdir}/udev/rules.d/

    # Clean /data unmount at shutdown. journald (when persisting to /data) stays
    # alive through the stop transaction and pins /data, so the data.mount
    # unmount fails and ext4 recovers the journal on every boot. This unit
    # relinquishes journald from /data on stop, before the unmount. Enabled via
    # a static sysinit.target.wants symlink so it runs unconditionally.
    install -m 0644 ${UNPACKDIR}/dbc-data-clean.service \
        ${D}${systemd_unitdir}/system/dbc-data-clean.service
    install -d ${D}${systemd_unitdir}/system/sysinit.target.wants
    ln -sf ../dbc-data-clean.service \
        ${D}${systemd_unitdir}/system/sysinit.target.wants/dbc-data-clean.service

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
