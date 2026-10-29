SUMMARY = "Status screen for the DBC bootstrap image"
LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/CC-BY-NC-SA-4.0;md5=277261f4a7c6c099822f8f5cb0180da0"

inherit systemd

SRC_URI = "file://dbc-bootstrap-screen.sh file://dbc-bootstrap-screen.service"

COMPATIBLE_MACHINE = "unu-dbc"

SYSTEMD_SERVICE:${PN} = "dbc-bootstrap-screen.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d ${D}${bindir} ${D}${systemd_system_unitdir}
    install -m 0755 ${UNPACKDIR}/dbc-bootstrap-screen.sh ${D}${bindir}/dbc-bootstrap-screen
    install -m 0644 ${UNPACKDIR}/dbc-bootstrap-screen.service ${D}${systemd_system_unitdir}/
}
