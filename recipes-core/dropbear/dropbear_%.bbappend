FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append:librescoot-mdb = " file://id-mdb"
SRC_URI:append:librescoot-mdb = " file://knownhosts-mdb"
SRC_URI:append:librescoot-mdb = " file://hostkey-mdb"

SRC_URI:append:librescoot-dbc = " file://authorized-dbc"
SRC_URI:append:librescoot-dbc = " file://hostkey-dbc"

do_install:append:librescoot-mdb() {
    install -d ${D}${sysconfdir}/dropbear
    install -d ${D}/root/.ssh

    install -m 0600 ${WORKDIR}/id-mdb ${D}/root/.ssh/id_dropbear
    install -m 0600 ${WORKDIR}/knownhosts-mdb ${D}/root/.ssh/known_hosts
    install -m 0600 ${WORKDIR}/hostkey-mdb ${D}${sysconfdir}/dropbear/dropbear_rsa_host_key
}

do_install:append:librescoot-dbc() {
    install -d ${D}${sysconfdir}/dropbear
    install -d ${D}/root/.ssh

    install -m 0600 ${WORKDIR}/authorized-dbc ${D}/root/.ssh/authorized_keys
    install -m 0600 ${WORKDIR}/hostkey-dbc ${D}${sysconfdir}/dropbear/dropbear_rsa_host_key
}
