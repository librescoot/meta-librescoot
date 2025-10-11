LICENSE = "CLOSED"
LIC_FILES_CHKSUM = ""

SRC_URI = "file://10-usb0.network"
SRC_URI += "file://10-eth1.network"
SRC_URI += "file://librescoot-netconfig.sh"
SRC_URI += "file://librescoot-netconfig.service"

inherit systemd

SYSTEMD_SERVICE:${PN} = "librescoot-netconfig.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install:unu-dbc() {
    install -d ${D}/etc/systemd/network
    install -d ${D}${sbindir}
    install -d ${D}${systemd_system_unitdir}

    install -m 0644 ${WORKDIR}/10-usb0.network ${D}/etc/systemd/network
    install -m 0755 ${WORKDIR}/librescoot-netconfig.sh ${D}${sbindir}/librescoot-netconfig
    install -m 0644 ${WORKDIR}/librescoot-netconfig.service ${D}${systemd_system_unitdir}
}

do_install:librescoot-dbc-rpi5() {
    install -d ${D}/etc/systemd/network
    install -d ${D}${sbindir}
    install -d ${D}${systemd_system_unitdir}

    install -m 0644 ${WORKDIR}/10-eth1.network ${D}/etc/systemd/network
    install -m 0755 ${WORKDIR}/librescoot-netconfig.sh ${D}${sbindir}/librescoot-netconfig
    install -m 0644 ${WORKDIR}/librescoot-netconfig.service ${D}${systemd_system_unitdir}
}
