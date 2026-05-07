SUMMARY = "LibreScoot Motion Service (BMX055 IMU)"
HOMEPAGE = "https://github.com/librescoot/motion-service"
LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://src/github.com/librescoot/motion-service/LICENSE;md5=fb5d051e53001fdff7fec0f368f47190"

SRC_URI = "git://github.com/librescoot/motion-service.git;protocol=https;branch=main"
SRC_URI += " file://librescoot-motion.service"

SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

inherit librescoot-go systemd

GO_IMPORT = "github.com/librescoot/motion-service"

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

FILES:${PN} += "/usr/lib/systemd/system/librescoot-motion.service"

SYSTEMD_SERVICE:${PN} = "librescoot-motion.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}

    install -m 0755 ${B}/bin/linux_arm/motion-service ${D}${bindir}/
    install -m 0644 ${WORKDIR}/librescoot-motion.service ${D}${systemd_system_unitdir}
}
