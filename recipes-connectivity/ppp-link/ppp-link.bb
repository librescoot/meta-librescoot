SUMMARY = "PPP backup link over UART between MDB and DBC"
LICENSE = "CLOSED"
LIC_FILES_CHKSUM = ""

RDEPENDS:${PN} = "ppp"

SRC_URI = "file://ppp-link.service"
SRC_URI:append:unu-mdb = " file://uart-link-mdb"
SRC_URI:append:unu-dbc = " file://uart-link-dbc"

inherit systemd

SYSTEMD_SERVICE:${PN} = "ppp-link.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d ${D}${sysconfdir}/ppp/peers
    install -d ${D}${systemd_system_unitdir}

    install -m 0644 ${WORKDIR}/ppp-link.service ${D}${systemd_system_unitdir}
}

do_install:append:unu-mdb() {
    install -m 0644 ${WORKDIR}/uart-link-mdb ${D}${sysconfdir}/ppp/peers/uart-link
}

do_install:append:unu-dbc() {
    install -m 0644 ${WORKDIR}/uart-link-dbc ${D}${sysconfdir}/ppp/peers/uart-link
}
