LICENSE = "CLOSED"
LIC_FILES_CHKSUM = ""

SRC_URI = "file://10-usb0.network"
SRC_URI += "file://20-can0.network"
SRC_URI += "file://30-end0.network"
SRC_URI += "file://wwan.nmconnection"
SRC_URI += "file://librescoot-netconfig.sh"
SRC_URI += "file://librescoot-netconfig.service"
SRC_URI += "file://librescoot-usb0-failsafe.sh"
SRC_URI += "file://librescoot-usb0-failsafe.service"
SRC_URI += "file://librescoot-usb0-failsafe.timer"
SRC_URI += "file://90-hostname.conf"
SRC_URI += "file://89-mdb-netconfig.preset"

inherit systemd

SYSTEMD_SERVICE:${PN} = "librescoot-netconfig.service librescoot-usb0-failsafe.timer"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

# The failsafe service is started by its timer's Unit=, never enabled: it is a
# oneshot with no [Install] section, so systemctl enable would fail on it. Only
# units named in SYSTEMD_SERVICE get packaged, so name this one here or
# do_package fails QA with "installed and not shipped".
FILES:${PN} += "${systemd_system_unitdir}/librescoot-usb0-failsafe.service"
FILES:${PN} += "${systemd_unitdir}/system-preset/89-mdb-netconfig.preset"

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

    # Raises usb0 when vehicle-service never recorded a gate decision, so a
    # board that cannot boot far enough to own the link still has a USB way in.
    install -m 0755 ${UNPACKDIR}/librescoot-usb0-failsafe.sh ${D}${sbindir}/librescoot-usb0-failsafe
    install -m 0644 ${UNPACKDIR}/librescoot-usb0-failsafe.service ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/librescoot-usb0-failsafe.timer ${D}${systemd_system_unitdir}

    # Preset: keep systemd-networkd-wait-online off. It cannot answer for this
    # board and blocks forever if asked; see the file for why.
    install -d ${D}${systemd_unitdir}/system-preset
    install -m 0644 ${UNPACKDIR}/89-mdb-netconfig.preset ${D}${systemd_unitdir}/system-preset/89-mdb-netconfig.preset
}
