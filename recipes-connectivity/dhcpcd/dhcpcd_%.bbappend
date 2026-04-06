FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += "file://dhcpcd.conf"
SRC_URI += "file://dhcpcd-sysctl.conf"

do_install:append() {
    install -d ${D}${sysconfdir}
    install -d ${D}${sysconfdir}/sysctl.d
    install -m 0644 ${WORKDIR}/dhcpcd.conf ${D}${sysconfdir}/dhcpcd.conf
    install -m 0644 ${WORKDIR}/dhcpcd-sysctl.conf ${D}${sysconfdir}/sysctl.d/dhcpcd-sysctl.conf
}

# Disable dhcpcd service on DBC - using systemd-networkd with static IP instead
SYSTEMD_AUTO_ENABLE:${PN}:unu-dbc = "disable"
SYSTEMD_AUTO_ENABLE:${PN}:librescoot-dbc-rpi4 = "disable"
