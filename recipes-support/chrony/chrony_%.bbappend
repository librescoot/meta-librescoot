FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append:unu-mdb = " file://chrony-mdb.conf"
SRC_URI:append:unu-dbc = " file://chrony-dbc.conf"
SRC_URI:append:librescoot-dbc-rpi5 = " file://chrony-dbc.conf"

do_install:append:unu-mdb() {
    install -d ${D}${sysconfdir}

    install -m 0644 ${WORKDIR}/chrony-mdb.conf ${D}${sysconfdir}/chrony.conf
}

do_install:append:unu-dbc() {
    install -d ${D}${sysconfdir}

    install -m 0644 ${WORKDIR}/chrony-dbc.conf ${D}${sysconfdir}/chrony.conf
}

do_install:append:librescoot-dbc-rpi5() {
    install -d ${D}${sysconfdir}

    install -m 0644 ${WORKDIR}/chrony-dbc.conf ${D}${sysconfdir}/chrony.conf
}
