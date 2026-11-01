SUMMARY = "Librescoot Power Management Service"
HOMEPAGE = "https://github.com/librescoot/pm-service"
LICENSE = "(AGPL-3.0-only | CC-BY-NC-SA-4.0)"
LIC_FILES_CHKSUM = "file://src/github.com/librescoot/pm-service/LICENSE;md5=eb1e647870add0502f8f010b19de32af \
                    file://${COMMON_LICENSE_DIR}/CC-BY-NC-SA-4.0;md5=277261f4a7c6c099822f8f5cb0180da0"

SRC_URI = "git://github.com/librescoot/pm-service.git;protocol=https;branch=main;destsuffix=${GO_SRCURI_DESTSUFFIX} \
           file://clock-coordinator.service"

SRCREV = "${AUTOREV}"


inherit librescoot-go systemd

GO_IMPORT = "github.com/librescoot/pm-service"
GO_INSTALL = "${GO_IMPORT}/cmd/pm-service ${GO_IMPORT}/cmd/clock-coordinator"

PACKAGES =+ "clock-coordinator"
FILES:clock-coordinator = "${bindir}/clock-coordinator ${systemd_system_unitdir}/clock-coordinator.service"
RDEPENDS:clock-coordinator = "chrony chronyc"
RDEPENDS:${PN} += "clock-coordinator"
SYSTEMD_PACKAGES = "${PN} clock-coordinator"
SYSTEMD_SERVICE:clock-coordinator = "clock-coordinator.service"
SYSTEMD_AUTO_ENABLE:clock-coordinator = "enable"

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
    install -m 0755 ${B}/bin/linux_arm/clock-coordinator ${D}${bindir}/
    install -m 0644 ${UNPACKDIR}/clock-coordinator.service ${D}${systemd_system_unitdir}/
    install -m 0644 ${B}/src/github.com/librescoot/pm-service/librescoot-pm.service ${D}${systemd_system_unitdir}
}
