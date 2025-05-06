SUMMARY = "LibreScoot Update Service"
HOMEPAGE = "https://github.com/librescoot/update-service"
LICENSE = "AGPL-3.0"
LIC_FILES_CHKSUM = "file://src/github.com/librescoot/update-service/LICENSE;md5=eb1e647870add0502f8f010b19de32af"

SRC_URI = "git://github.com/librescoot/update-service.git;protocol=https;branch=main"

SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

inherit go-mod systemd

GO_IMPORT = "github.com/librescoot/update-service"

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

FILES:${PN} += "/usr/lib/systemd/system/librescoot-update.service"

SYSTEMD_SERVICE:${PN} = "librescoot-update.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}

    install -m 0755 ${B}/bin/linux_arm/update-service ${D}${bindir}/
    install -m 0644 ${B}/src/github.com/librescoot/update-service/librescoot-update.service ${D}${systemd_system_unitdir}
}

