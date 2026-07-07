LICENSE = "CLOSED"
LIC_FILES_CHKSUM = ""

SRC_URI = "file://10-usb0.network"
SRC_URI += "file://20-can0.network"
SRC_URI += "file://30-end0.network"
SRC_URI += "file://wwan.nmconnection"
SRC_URI += "file://librescoot-netconfig.sh"
SRC_URI += "file://librescoot-netconfig.service"
SRC_URI += "file://90-hostname.conf"

inherit systemd

SYSTEMD_SERVICE:${PN} = "librescoot-netconfig.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d ${D}/etc/systemd/network
    install -d ${D}/etc/NetworkManager/system-connections/
    install -d ${D}${sbindir}
    install -d ${D}${systemd_system_unitdir}

    install -m 0644 ${UNPACKDIR}/10-usb0.network ${D}/etc/systemd/network
    install -m 0644 ${UNPACKDIR}/20-can0.network ${D}/etc/systemd/network
    install -m 0644 ${UNPACKDIR}/30-end0.network ${D}/etc/systemd/network
    install -m 0600 ${UNPACKDIR}/wwan.nmconnection ${D}/etc/NetworkManager/system-connections/
    install -d ${D}/etc/NetworkManager/conf.d
    install -m 0644 ${UNPACKDIR}/90-hostname.conf ${D}/etc/NetworkManager/conf.d/
    install -m 0755 ${UNPACKDIR}/librescoot-netconfig.sh ${D}${sbindir}/librescoot-netconfig
    install -m 0644 ${UNPACKDIR}/librescoot-netconfig.service ${D}${systemd_system_unitdir}
}
