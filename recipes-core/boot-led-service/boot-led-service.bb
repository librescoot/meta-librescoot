LICENSE = "CLOSED"
LIC_FILES_CHKSUM = ""

inherit systemd

SRC_URI = "file://librescoot-boot-led.service \
           file://set-dbc-led \
           "

SYSTEMD_SERVICE:${PN} = "librescoot-boot-led.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"


do_install() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}
    install -m 0755 ${WORKDIR}/set-dbc-led ${D}${bindir}/
    install -m 0644 ${WORKDIR}/librescoot-boot-led.service ${D}${systemd_system_unitdir}
}
