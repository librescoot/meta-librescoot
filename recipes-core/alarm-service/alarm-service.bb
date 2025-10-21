SUMMARY = "LibreScoot Alarm Service"
HOMEPAGE = "https://github.com/librescoot/alarm-service"
LICENSE = "CC-BY-NC-4.0"
LIC_FILES_CHKSUM = "file://src/alarm-service/LICENSE;md5=f5a53c7ab38ba3772e879f1407d3d412"

SRC_URI = "git://github.com/librescoot/alarm-service.git;protocol=https;branch=main"
SRC_URI += " file://librescoot-alarm.service"

SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

inherit go-mod systemd

GO_IMPORT = "alarm-service"

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

FILES:${PN} += "/usr/lib/systemd/system/librescoot-alarm.service"

SYSTEMD_SERVICE:${PN} = "librescoot-alarm.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}

    install -m 0755 ${B}/bin/linux_arm/alarm-service ${D}${bindir}/
    install -m 0644 ${WORKDIR}/librescoot-alarm.service ${D}${systemd_system_unitdir}
}
