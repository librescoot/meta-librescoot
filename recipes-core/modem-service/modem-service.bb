SUMMARY = "LibreScoot Modem Service"
HOMEPAGE = "https://github.com/librescoot/modem-service"
LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://src/modem-service/LICENSE;md5=eb1e647870add0502f8f010b19de32af"

SRC_URI = "git://github.com/librescoot/modem-service.git;protocol=https;branch=main"
SRC_URI += " file://librescoot-modem.service"

SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

inherit go-mod systemd

GO_IMPORT = "modem-service"

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

FILES:${PN} += "/usr/lib/systemd/system/librescoot-modem.service"

SYSTEMD_SERVICE:${PN} = "librescoot-modem.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}

    install -m 0755 ${B}/bin/linux_arm/modem-service ${D}${bindir}/
    install -m 0644 ${WORKDIR}/librescoot-modem.service ${D}${systemd_system_unitdir}
}

