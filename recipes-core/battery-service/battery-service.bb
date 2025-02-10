SUMMARY = "LibreScoot Battery Service"
HOMEPAGE = "https://github.com/librescoot/battery-service"
LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://src/battery-service/LICENSE;md5=fb5d051e53001fdff7fec0f368f47190"

SRC_URI = "git://github.com/librescoot/battery-service.git;protocol=https;branch=main"
SRC_URI += " file://librescoot-battery.service"

SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

inherit go-mod systemd

GO_IMPORT = "battery-service"

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

FILES:${PN} += "/usr/lib/systemd/system/librescoot-battery.service"

SYSTEMD_SERVICE:${PN} = "librescoot-battery.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}

    install -m 0755 ${B}/bin/linux_arm/battery-service ${D}${bindir}/
    install -m 0644 ${WORKDIR}/librescoot-battery.service ${D}${systemd_system_unitdir}
}

