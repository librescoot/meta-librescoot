FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += "file://dhcpcd.conf"
SRC_URI += "file://dhcpcd-sysctl.conf"

do_install:append() {
    install -d ${D}${sysconfdir}
    install -d ${D}${sysconfdir}/sysctl.d
    install -m 0644 ${UNPACKDIR}/dhcpcd.conf ${D}${sysconfdir}/dhcpcd.conf
    install -m 0644 ${UNPACKDIR}/dhcpcd-sysctl.conf ${D}${sysconfdir}/sysctl.d/dhcpcd-sysctl.conf
}

# dhcpcd is pulled in by packagegroup-core-base-utils (hard RDEPENDS, so the
# package stays), but every interface has a dedicated manager: systemd-networkd
# owns usb0/can0/end0, NetworkManager+ModemManager own wlan0/wwan0 (wifi is
# driven through NM's D-Bus API by settings-service), pppd owns ppp0. Left
# running, dhcpcd fights NM over the QMI raw-ip wwan0 - IPv6 router
# solicitations, route churn and neighbor un/reachable flapping against the
# carrier every ~15s.
SYSTEMD_AUTO_ENABLE:${PN} = "disable"
