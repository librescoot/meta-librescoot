FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append:unu-mdb = " file://id-mdb"
SRC_URI:append:unu-mdb = " file://knownhosts-mdb"
SRC_URI:append:unu-mdb = " file://hostkey-mdb"

SRC_URI:append:unu-dbc = " file://authorized-dbc"
SRC_URI:append:unu-dbc = " file://hostkey-dbc"

SRC_URI:append:librescoot-dbc-rpi5 = " file://authorized-dbc"
SRC_URI:append:librescoot-dbc-rpi5 = " file://hostkey-dbc"

FILES:${PN} += " \
    /root/.ssh/* \
"

do_install:append:unu-mdb() {
    install -d ${D}${sysconfdir}/dropbear
    install -d ${D}/root/.ssh

    install -m 0600 ${WORKDIR}/id-mdb ${D}/root/.ssh/id_dropbear
    install -m 0600 ${WORKDIR}/knownhosts-mdb ${D}/root/.ssh/known_hosts
    install -m 0600 ${WORKDIR}/hostkey-mdb ${D}${sysconfdir}/dropbear/dropbear_rsa_host_key
}

do_install:append:unu-dbc() {
    install -d ${D}${sysconfdir}/dropbear
    install -d ${D}/root/.ssh

    install -m 0600 ${WORKDIR}/authorized-dbc ${D}/root/.ssh/authorized_keys
    install -m 0600 ${WORKDIR}/hostkey-dbc ${D}${sysconfdir}/dropbear/dropbear_rsa_host_key
}

do_install:append:librescoot-dbc-rpi5() {
    install -d ${D}${sysconfdir}/dropbear
    install -d ${D}/root/.ssh

    install -m 0600 ${WORKDIR}/authorized-dbc ${D}/root/.ssh/authorized_keys
    install -m 0600 ${WORKDIR}/hostkey-dbc ${D}${sysconfdir}/dropbear/dropbear_rsa_host_key
}
