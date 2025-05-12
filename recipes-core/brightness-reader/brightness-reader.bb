LICENSE = "CLOSED"
LIC_FILES_CHKSUM = ""

inherit systemd

SRC_URI = "file://brightness-reader.py"
SRC_URI += "file://librescoot-brightness.service"

SYSTEMD_SERVICE:${PN} = "librescoot-brightness.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

FILES:${PN} = "/opt/brightness-reader/*"

do_install() {
    install -d ${D}/opt/brightness-reader
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/brightness-reader.py ${D}/opt/brightness-reader
    install -m 0644 ${WORKDIR}/librescoot-brightness.service ${D}${systemd_system_unitdir}
}
