SUMMARY = "LibreScoot Uplink Service"
HOMEPAGE = "https://github.com/librescoot/uplink-service"
LICENSE = "AGPL-3.0-only"
LIC_FILES_CHKSUM = "file://src/${GO_IMPORT}/LICENSE;md5=eb1e647870add0502f8f010b19de32af"

inherit librescoot-go systemd

SRC_URI = "git://github.com/librescoot/uplink-service.git;protocol=https;branch=main"
SRC_URI += " file://librescoot-uplink.service"

# Pinned 2026-01-28: fix: use safe type assertions in stats logger
SRCREV = "868c717b0d0202c0392597e4372207c2231dcb1d"
S = "${WORKDIR}/git"

GO_IMPORT = "github.com/librescoot/uplink-service"
GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

FILES:${PN} += "/usr/lib/systemd/system/librescoot-uplink.service"

SYSTEMD_SERVICE:${PN} = "librescoot-uplink.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}

    install -m 0755 ${B}/bin/linux_arm/uplink-service ${D}${bindir}/
    install -m 0644 ${WORKDIR}/librescoot-uplink.service ${D}${systemd_system_unitdir}
}
