LICENSE = "CLOSED"
LIC_FILES_CHKSUM = ""

inherit systemd

SRC_URI = "file://machine-id-init.sh \
           file://machine-id-init.service"

SYSTEMD_SERVICE:${PN} = "machine-id-init.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

RDEPENDS:${PN} = "u-boot-fw-utils"

do_install() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/machine-id-init.service ${D}${systemd_system_unitdir}

    install -d ${D}${libdir}/machine-id-init
    install -m 0755 ${WORKDIR}/machine-id-init.sh ${D}${libdir}/machine-id-init/machine-id-init.sh
}
