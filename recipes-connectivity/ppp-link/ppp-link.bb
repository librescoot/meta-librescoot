SUMMARY = "PPP backup link over UART between MDB and DBC"
LICENSE = "CLOSED"
LIC_FILES_CHKSUM = ""

# pppd 2.5.x loads OpenSSL's legacy provider (DES/MD4 for the Microsoft
# auth protocols) unconditionally at startup and exits when it's missing,
# even for a noauth link like ours. oe-core's ppp recipe doesn't pull the
# module in, so name it explicitly.
RDEPENDS:${PN} = "ppp openssl-ossl-module-legacy"

SRC_URI = "file://ppp-link.service \
           file://ip-up-backup-routes \
           file://ip-down-backup-routes \
           file://librescoot-link-route-monitor \
           file://librescoot-link-route-monitor.service \
"
SRC_URI:append:unu-mdb = " file://uart-link-mdb"
SRC_URI:append:unu-dbc = " file://uart-link-dbc"

inherit systemd

PACKAGES += "${PN}-route-monitor"
FILES:${PN}-route-monitor = "${libexecdir}/librescoot-link-route-monitor ${systemd_system_unitdir}/librescoot-link-route-monitor.service"
RDEPENDS:${PN} += "${PN}-route-monitor"

SYSTEMD_PACKAGES = "${PN} ${PN}-route-monitor"
SYSTEMD_SERVICE:${PN} = "ppp-link.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"
SYSTEMD_SERVICE:${PN}-route-monitor = "librescoot-link-route-monitor.service"
SYSTEMD_AUTO_ENABLE:${PN}-route-monitor = "enable"

# On the MDB the link is slaved to DBC power: vehicle-service starts/stops
# this unit alongside the dashboard_power GPIO. Auto-starting it at boot
# would leave pppd holding ttymxc2 open while the DBC is unpowered, whose
# dead TX line reads as a permanent break and triggers periodic imx-uart
# "RX flood" soft resets.
SYSTEMD_AUTO_ENABLE:${PN}:unu-mdb = "disable"

do_install() {
    install -d ${D}${sysconfdir}/ppp/peers
    install -d ${D}${sysconfdir}/ppp/ip-up.d
    install -d ${D}${sysconfdir}/ppp/ip-down.d
    install -d ${D}${systemd_system_unitdir}
    install -d ${D}${libexecdir}

    install -m 0644 ${UNPACKDIR}/ppp-link.service ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/librescoot-link-route-monitor.service ${D}${systemd_system_unitdir}
    install -m 0755 ${UNPACKDIR}/librescoot-link-route-monitor ${D}${libexecdir}
    install -m 0755 ${UNPACKDIR}/ip-up-backup-routes ${D}${sysconfdir}/ppp/ip-up.d/50-backup-routes
    install -m 0755 ${UNPACKDIR}/ip-down-backup-routes ${D}${sysconfdir}/ppp/ip-down.d/50-backup-routes
}

do_install:append:unu-mdb() {
    install -m 0644 ${UNPACKDIR}/uart-link-mdb ${D}${sysconfdir}/ppp/peers/uart-link
}

do_install:append:unu-dbc() {
    install -m 0644 ${UNPACKDIR}/uart-link-dbc ${D}${sysconfdir}/ppp/peers/uart-link
}
