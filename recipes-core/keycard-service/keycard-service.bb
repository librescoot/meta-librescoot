SUMMARY = "Librescoot Keycard Service"
HOMEPAGE = "https://github.com/librescoot/keycard-service"
LICENSE = "CC-BY-NC-4.0"
LIC_FILES_CHKSUM = "file://src/keycard-service/LICENSE;md5=fb5d051e53001fdff7fec0f368f47190"

SRC_URI = "git://github.com/librescoot/keycard-service.git;protocol=https;branch=main;destsuffix=${GO_SRCURI_DESTSUFFIX}"
SRC_URI:append = " file://librescoot-keycard.service"
SRC_URI:append = " file://ledcontrol.sh"

SRCREV = "${AUTOREV}"


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
    install -m 0644 ${UNPACKDIR}/librescoot-keycard.service ${D}${systemd_system_unitdir}
    install -m 0755 ${UNPACKDIR}/ledcontrol.sh ${D}${bindir}
}
