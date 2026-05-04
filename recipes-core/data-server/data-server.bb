SUMMARY = "Librescoot Data Server"
HOMEPAGE = "https://github.com/librescoot/data-server"
LICENSE = "AGPL-3.0-or-later"
LIC_FILES_CHKSUM = "file://src/data-server/LICENSE;md5=eb1e647870add0502f8f010b19de32af"

SRC_URI = "git://github.com/librescoot/data-server.git;protocol=https;branch=main"
SRC_URI += " file://librescoot-data-server.service"

SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

inherit librescoot-go systemd

GO_IMPORT = "data-server"

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

FILES:${PN} += "/usr/lib/systemd/system/librescoot-data-server.service"

SYSTEMD_SERVICE:${PN} = "librescoot-data-server.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}
    install -m 0755 ${B}/bin/linux_arm/data-server ${D}${bindir}/
    install -m 0644 ${WORKDIR}/librescoot-data-server.service ${D}${systemd_system_unitdir}
}
