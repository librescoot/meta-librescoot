LICENSE = "CLOSED"
LIC_FILES_CHKSUM = ""

inherit systemd

SRC_URI = "file://librescoot-fresh-flash-init.sh \
           file://librescoot-fresh-flash-init.service"

SYSTEMD_SERVICE:${PN} = "librescoot-fresh-flash-init.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

RDEPENDS:${PN} = "redis bash"

do_install() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/librescoot-fresh-flash-init.service ${D}${systemd_system_unitdir}

    install -d ${D}${libdir}/librescoot-fresh-flash-init
    install -m 0755 ${WORKDIR}/librescoot-fresh-flash-init.sh ${D}${libdir}/librescoot-fresh-flash-init/librescoot-fresh-flash-init.sh
}

FILES:${PN} += "${libdir}/librescoot-fresh-flash-init"
