SUMMARY = "Librescoot Event Service"
HOMEPAGE = "https://github.com/librescoot/event-service"
LICENSE = "AGPL-3.0-only"
LIC_FILES_CHKSUM = "file://src/github.com/librescoot/event-service/LICENSE;md5=eb1e647870add0502f8f010b19de32af"

SRC_URI = "git://github.com/librescoot/event-service.git;protocol=https;branch=main;destsuffix=${GO_SRCURI_DESTSUFFIX}"
SRC_URI += " file://librescoot-events.service"

SRCREV = "${AUTOREV}"


inherit librescoot-go systemd

GO_IMPORT = "github.com/librescoot/event-service"

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

FILES:${PN} += "/usr/lib/systemd/system/librescoot-events.service"

SYSTEMD_SERVICE:${PN} = "librescoot-events.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}

    install -m 0755 ${B}/bin/linux_arm/event-service ${D}${bindir}/
    install -m 0644 ${UNPACKDIR}/librescoot-events.service ${D}${systemd_system_unitdir}
}
