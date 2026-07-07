SUMMARY = "Librescoot Uplink Service"
HOMEPAGE = "https://github.com/librescoot/uplink-service"
LICENSE = "AGPL-3.0-only"
LIC_FILES_CHKSUM = "file://src/${GO_IMPORT}/LICENSE;md5=eb1e647870add0502f8f010b19de32af"

inherit librescoot-go systemd

SRC_URI = "git://github.com/librescoot/uplink-service.git;protocol=https;branch=main;destsuffix=${GO_SRCURI_DESTSUFFIX}"
SRC_URI += " file://librescoot-uplink.service"
SRC_URI += " file://uplink-service.tmpfiles.conf"

SRCREV = "${AUTOREV}"

GO_IMPORT = "github.com/librescoot/uplink-service"
GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

FILES:${PN} += "/usr/lib/systemd/system/librescoot-uplink.service"
FILES:${PN} += "${sysconfdir}/tmpfiles.d/uplink-service.conf"

SYSTEMD_SERVICE:${PN} = "librescoot-uplink.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}
    install -d ${D}${sysconfdir}/tmpfiles.d

    install -m 0755 ${B}/bin/linux_arm/uplink-service ${D}${bindir}/
    install -m 0644 ${UNPACKDIR}/librescoot-uplink.service ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/uplink-service.tmpfiles.conf ${D}${sysconfdir}/tmpfiles.d/uplink-service.conf
}
