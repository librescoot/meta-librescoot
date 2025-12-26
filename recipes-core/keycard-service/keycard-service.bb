SUMMARY = "LibreScoot Keycard Service"
HOMEPAGE = "https://github.com/librescoot/keycard-service"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://src/keycard-service/LICENSE;md5=ff1d608f2b44ef24b63d4f8cdbab0ce4"

SRC_URI = "git://github.com/librescoot/keycard-service.git;protocol=https;branch=main"
SRC_URI:append = " file://librescoot-keycard.service"

SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

inherit librescoot-go systemd

GO_IMPORT = "keycard-service"

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

FILES:${PN} += "/usr/lib/systemd/system/librescoot-keycard.service"

SYSTEMD_SERVICE:${PN} = "librescoot-keycard.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}

    install -m 0755 ${B}/bin/linux_arm/keycard-service ${D}${bindir}/
    install -m 0644 ${WORKDIR}/librescoot-keycard.service ${D}${systemd_system_unitdir}
}
