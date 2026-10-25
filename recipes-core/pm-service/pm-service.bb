SUMMARY = "Librescoot Power Management Service"
HOMEPAGE = "https://github.com/librescoot/pm-service"
LICENSE = "(AGPL-3.0-only | CC-BY-NC-SA-4.0)"
LIC_FILES_CHKSUM = "file://src/github.com/librescoot/pm-service/LICENSE;md5=eb1e647870add0502f8f010b19de32af \
                    file://${COMMON_LICENSE_DIR}/CC-BY-NC-SA-4.0;md5=277261f4a7c6c099822f8f5cb0180da0"

SRC_URI = "git://github.com/librescoot/pm-service.git;protocol=https;branch=main;destsuffix=${GO_SRCURI_DESTSUFFIX}"

SRCREV = "${AUTOREV}"


inherit librescoot-go systemd

GO_IMPORT = "github.com/librescoot/pm-service"

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

FILES:${PN} += "/usr/lib/systemd/system/librescoot-pm.service"

SYSTEMD_SERVICE:${PN} = "librescoot-pm.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install:prepend:librescoot-dbc-rpi4() {
    mv ${B}/bin/linux_arm64 ${B}/bin/linux_arm || true
}

do_install() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}

    install -m 0755 ${B}/bin/linux_arm/pm-service ${D}${bindir}/
    install -m 0644 ${B}/src/github.com/librescoot/pm-service/librescoot-pm.service ${D}${systemd_system_unitdir}
}
