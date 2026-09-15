SUMMARY = "Librescoot Trip Service"
HOMEPAGE = "https://github.com/librescoot/trip-service"
LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://src/github.com/librescoot/trip-service/LICENSE;md5=fb5d051e53001fdff7fec0f368f47190"

SRC_URI = "git://github.com/librescoot/trip-service.git;protocol=https;branch=main;destsuffix=${GO_SRCURI_DESTSUFFIX}"
SRC_URI += " file://librescoot-trip.service"

SRCREV = "${AUTOREV}"

inherit librescoot-go systemd

GO_IMPORT = "github.com/librescoot/trip-service"

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

FILES:${PN} += "/usr/lib/systemd/system/librescoot-trip.service"

SYSTEMD_SERVICE:${PN} = "librescoot-trip.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}

    install -m 0755 ${B}/bin/linux_arm/trip-service ${D}${bindir}/
    install -m 0644 ${UNPACKDIR}/librescoot-trip.service ${D}${systemd_system_unitdir}
}
