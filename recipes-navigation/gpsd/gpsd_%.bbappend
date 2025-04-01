FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

SRC_URI += "file://gpsd"

do_install:append() {
    install -d ${D}${sysconfdir}
    install -d ${D}${sysconfdir}/default/

    install -m 0644 ${WORKDIR}/gpsd ${D}${sysconfdir}/default/gpsd
}
