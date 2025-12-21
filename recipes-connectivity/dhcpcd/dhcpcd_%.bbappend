FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += "file://dhcpcd.conf"

do_install:append() {
    install -d ${D}${sysconfdir}
    install -m 0644 ${WORKDIR}/dhcpcd.conf ${D}${sysconfdir}/dhcpcd.conf
}

# Disable dhcpcd service on DBC - using systemd-networkd with static IP instead
SYSTEMD_AUTO_ENABLE:${PN}:unu-dbc = "disable"
SYSTEMD_AUTO_ENABLE:${PN}:librescoot-dbc-rpi4 = "disable"
