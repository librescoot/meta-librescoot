FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append:librescoot-mdb = " file://chrony-mdb.conf"
SRC_URI:append:librescoot-dbc = " file://chrony-dbc.conf"

do_install:librescoot-mdb:append() {
    install -d ${D}${sysconfdir}

    install -m 0644 ${WORKDIR}/chrony-mdb.conf ${D}${sysconfdir}/chrony.conf
}

do_install:librescoot-dbc:append() {
    install -d ${D}${sysconfdir}

    install -m 0644 ${WORKDIR}/chrony-dbc.conf ${D}${sysconfdir}/chrony.conf
}
