LICENSE = "CLOSED"
LIC_FILES_CHKSUM = ""

inherit systemd

SRC_URI = "file://librescoot-boot-led.service"

SYSTEMD_SERVICE:${PN} = "librescoot-boot-led.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

RDEPENDS:${PN} = "keycard-service"

do_install() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/librescoot-boot-led.service ${D}${systemd_system_unitdir}
}