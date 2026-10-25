SUMMARY = "Default-deny MDB ingress and forwarding firewall"
LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/CC-BY-NC-SA-4.0;md5=277261f4a7c6c099822f8f5cb0180da0"

SRC_URI = " \
    file://librescoot-firewall \
    file://librescoot-firewall.service \
    file://10-firewall.conf \
    file://ipv4.rules \
    file://ipv6.rules \
"

S = "${UNPACKDIR}"

inherit systemd

RDEPENDS:${PN} = "iptables"
SYSTEMD_SERVICE:${PN} = "librescoot-firewall.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"
FILES:${PN} += "${systemd_system_unitdir}/*.service.d"

do_install() {
    install -d ${D}${sbindir} ${D}${sysconfdir}/librescoot/firewall ${D}${systemd_system_unitdir}
    install -m 0755 ${UNPACKDIR}/librescoot-firewall ${D}${sbindir}/
    install -m 0644 ${UNPACKDIR}/ipv4.rules ${UNPACKDIR}/ipv6.rules ${D}${sysconfdir}/librescoot/firewall/
    install -m 0644 ${UNPACKDIR}/librescoot-firewall.service ${D}${systemd_system_unitdir}/

    for unit in systemd-networkd NetworkManager ppp-link librescoot-netconfig; do
        install -d ${D}${systemd_system_unitdir}/$unit.service.d
        install -m 0644 ${UNPACKDIR}/10-firewall.conf ${D}${systemd_system_unitdir}/$unit.service.d/
    done
}
