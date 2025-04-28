LICENSE = "CLOSED"
LIC_FILES_CHKSUM = ""

inherit systemd

SRC_URI = "file://librescoot-onboot.service"

SYSTEMD_SERVICE:${PN} = "librescoot-onboot.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/librescoot-onboot.service ${D}${systemd_system_unitdir}
}
