LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/CC-BY-NC-SA-4.0;md5=277261f4a7c6c099822f8f5cb0180da0"

inherit systemd

SRC_URI = "file://machine-id-init.sh \
           file://machine-id-init.service"

SYSTEMD_SERVICE:${PN} = "machine-id-init.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

RDEPENDS:${PN} = "u-boot-fw-utils"

do_install() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/machine-id-init.service ${D}${systemd_system_unitdir}

    install -d ${D}${libdir}/machine-id-init
    install -m 0755 ${UNPACKDIR}/machine-id-init.sh ${D}${libdir}/machine-id-init/machine-id-init.sh
}
