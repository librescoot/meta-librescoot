SUMMARY = "PPP backup link over UART between MDB and DBC"
LICENSE = "CLOSED"
LIC_FILES_CHKSUM = ""

# pppd 2.5.x loads OpenSSL's legacy provider (DES/MD4 for the Microsoft
# auth protocols) unconditionally at startup and exits when it's missing,
# even for a noauth link like ours. oe-core's ppp recipe doesn't pull the
# module in, so name it explicitly.
RDEPENDS:${PN} = "ppp openssl-ossl-module-legacy"

SRC_URI = "file://ppp-link.service"
SRC_URI:append:unu-mdb = " file://uart-link-mdb"
SRC_URI:append:unu-dbc = " file://uart-link-dbc"

inherit systemd

SYSTEMD_SERVICE:${PN} = "ppp-link.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d ${D}${sysconfdir}/ppp/peers
    install -d ${D}${systemd_system_unitdir}

    install -m 0644 ${UNPACKDIR}/ppp-link.service ${D}${systemd_system_unitdir}
}

do_install:append:unu-mdb() {
    install -m 0644 ${UNPACKDIR}/uart-link-mdb ${D}${sysconfdir}/ppp/peers/uart-link
}

do_install:append:unu-dbc() {
    install -m 0644 ${UNPACKDIR}/uart-link-dbc ${D}${sysconfdir}/ppp/peers/uart-link
}
