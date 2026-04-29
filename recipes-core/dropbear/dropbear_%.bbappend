FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append = " file://dropbear.socket"
SRC_URI:append = " file://authorized"
SRC_URI:append = " file://id"

SRC_URI:append:unu-mdb = " file://knownhosts-mdb"
SRC_URI:append:unu-mdb = " file://hostkey-mdb"

SRC_URI:append:unu-dbc = " file://knownhosts-dbc"
SRC_URI:append:unu-dbc = " file://hostkey-dbc"

SRC_URI:append:librescoot-dbc-rpi4 = " file://knownhosts-dbc"
SRC_URI:append:librescoot-dbc-rpi4 = " file://hostkey-dbc"

FILES:${PN} += " \
    /root/.ssh/* \
"

do_install:append() {
    install -d ${D}/root/.ssh
    install -d ${D}${sysconfdir}/dropbear

    install -m 0644 ${WORKDIR}/dropbear.socket ${D}${systemd_system_unitdir}/dropbear.socket
    install -m 0600 ${WORKDIR}/id ${D}/root/.ssh/id_dropbear
    install -m 0600 ${WORKDIR}/authorized ${D}/root/.ssh/authorized_keys
}

do_install:append:unu-mdb() {
    install -m 0600 ${WORKDIR}/knownhosts-mdb ${D}/root/.ssh/known_hosts
    install -m 0600 ${WORKDIR}/hostkey-mdb ${D}${sysconfdir}/dropbear/dropbear_rsa_host_key
}

do_install:append:unu-dbc() {
    install -m 0600 ${WORKDIR}/knownhosts-dbc ${D}/root/.ssh/known_hosts
    install -m 0600 ${WORKDIR}/hostkey-dbc ${D}${sysconfdir}/dropbear/dropbear_rsa_host_key
}

do_install:append:librescoot-dbc-rpi4() {
    install -m 0600 ${WORKDIR}/knownhosts-dbc ${D}/root/.ssh/known_hosts
    install -m 0600 ${WORKDIR}/hostkey-dbc ${D}${sysconfdir}/dropbear/dropbear_rsa_host_key
}
