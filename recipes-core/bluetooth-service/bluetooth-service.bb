SUMMARY = "LibreScoot Bluetooth Service"
HOMEPAGE = "https://github.com/librescoot/bluetooth-service"
LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://src/github.com/librescoot/bluetooth-service/LICENSE;md5=fb5d051e53001fdff7fec0f368f47190"

SRC_URI = "git://github.com/librescoot/bluetooth-service.git;protocol=https;branch=main"
SRC_URI:append = " file://librescoot-bluetooth.service"

SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

inherit go-mod systemd

GO_IMPORT = "github.com/librescoot/bluetooth-service"

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

FILES:${PN} += "/usr/lib/systemd/system/librescoot-bluetooth.service"

SYSTEMD_SERVICE:${PN} = "librescoot-bluetooth.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}

    install -m 0755 ${B}/bin/linux_arm/bluetooth-service ${D}${bindir}/
    install -m 0644 ${WORKDIR}/librescoot-bluetooth.service ${D}${systemd_system_unitdir}
}

