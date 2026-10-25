LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/CC-BY-NC-SA-4.0;md5=277261f4a7c6c099822f8f5cb0180da0"

SRC_URI = "file://10-usb0.network"
SRC_URI += "file://librescoot-netconfig.sh"
SRC_URI += "file://librescoot-netconfig.service"

inherit systemd

SYSTEMD_SERVICE:${PN} = "librescoot-netconfig.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d ${D}/etc/systemd/network
    install -d ${D}${sbindir}
    install -d ${D}${systemd_system_unitdir}

    install -m 0644 ${UNPACKDIR}/10-usb0.network ${D}/etc/systemd/network
    install -m 0755 ${UNPACKDIR}/librescoot-netconfig.sh ${D}${sbindir}/librescoot-netconfig
    install -m 0644 ${UNPACKDIR}/librescoot-netconfig.service ${D}${systemd_system_unitdir}
}

