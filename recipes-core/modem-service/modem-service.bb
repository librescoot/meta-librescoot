SUMMARY = "Librescoot Modem Service"
HOMEPAGE = "https://github.com/librescoot/modem-service"
LICENSE = "(AGPL-3.0-only | CC-BY-NC-SA-4.0)"
LIC_FILES_CHKSUM = "file://src/modem-service/LICENSE;md5=eb1e647870add0502f8f010b19de32af \
                    file://${COMMON_LICENSE_DIR}/CC-BY-NC-SA-4.0;md5=277261f4a7c6c099822f8f5cb0180da0"

SRC_URI = "git://github.com/librescoot/modem-service.git;protocol=https;branch=main;destsuffix=${GO_SRCURI_DESTSUFFIX}"
SRC_URI += " file://librescoot-modem.service"

SRCREV = "${AUTOREV}"


inherit librescoot-go systemd

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
    install -m 0644 ${UNPACKDIR}/librescoot-modem.service ${D}${systemd_system_unitdir}
}

