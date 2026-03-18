FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append:unu-dbc = " file://99-etnaviv-no-autosuspend.rules"

# Remove unnecessary udev rules for unu-dbc (saves ~0.7-1.0s in coldplug time)
# Only applies to unu-dbc, not to rpi4 DBC variant
do_install:append:unu-dbc () {
	# Disable etnaviv GPU autosuspend to prevent display flickering
	install -m 0644 ${WORKDIR}/99-etnaviv-no-autosuspend.rules ${D}${sysconfdir}/udev/rules.d/
	# Power management (DBC plugged into MDB, no battery)
	rm -f ${D}${sysconfdir}/udev/rules.d/60-autosuspend.rules

	# Storage (no CD/DVD, tape, or btrfs)
	rm -f ${D}${sysconfdir}/udev/rules.d/60-cdrom_id.rules
	rm -f ${D}${sysconfdir}/udev/rules.d/60-persistent-storage-tape.rules
	rm -f ${D}${sysconfdir}/udev/rules.d/64-btrfs.rules

	# Security (no smart cards, FIDO tokens)
	rm -f ${D}${sysconfdir}/udev/rules.d/60-fido-id.rules

	# Network (no InfiniBand)
	rm -f ${D}${sysconfdir}/udev/rules.d/60-infiniband.rules

	# Input devices (no joystick, mouse, touchpad)
	rm -f ${D}${sysconfdir}/udev/rules.d/70-joystick.rules
	rm -f ${D}${sysconfdir}/udev/rules.d/70-mouse.rules
	rm -f ${D}${sysconfdir}/udev/rules.d/70-touchpad.rules
	rm -f ${D}${sysconfdir}/udev/rules.d/80-libinput-device-groups.rules
	rm -f ${D}${sysconfdir}/udev/rules.d/90-libinput-fuzz-override.rules

	# Memory (no RAM testing hardware)
	rm -f ${D}${sysconfdir}/udev/rules.d/70-memory.rules

	# Camera (CSI ports present but unused)
	rm -f ${D}${sysconfdir}/udev/rules.d/70-camera.rules

	# Audio restore (audio present but not actively managed via ALSA)
	rm -f ${D}${sysconfdir}/udev/rules.d/90-alsa-restore.rules

	# Multi-seat desktop (DBC is single-user embedded, not desktop)
	rm -f ${D}${sysconfdir}/udev/rules.d/71-seat.rules
	rm -f ${D}${sysconfdir}/udev/rules.d/73-seat-late.rules

	# Network configuration (handled by systemd-networkd, not udev)
	rm -f ${D}${sysconfdir}/udev/rules.d/75-net-description.rules
	rm -f ${D}${sysconfdir}/udev/rules.d/81-net-dhcp.rules

	# I/O cost (eMMC doesn't support CFQ-IO-cost)
	rm -f ${D}${sysconfdir}/udev/rules.d/90-iocost.rules
}
