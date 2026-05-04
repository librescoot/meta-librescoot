SUMMARY = "Librescoot UMS Service"
HOMEPAGE = "https://github.com/librescoot/ums-service"
LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://src/github.com/librescoot/ums-service/LICENSE;md5=fb5d051e53001fdff7fec0f368f47190"

SRC_URI = "git://github.com/librescoot/ums-service.git;protocol=https;branch=main"
SRC_URI += " file://librescoot-ums.service"

SRCREV = "${AUTOREV}"
PE = "1"

S = "${WORKDIR}/git"

inherit librescoot-go systemd

GO_IMPORT = "github.com/librescoot/ums-service"

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

FILES:${PN} += "/usr/lib/systemd/system/librescoot-ums.service"

SYSTEMD_SERVICE:${PN} = "librescoot-ums.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}

    install -m 0755 ${B}/bin/linux_arm/ums-service ${D}${bindir}/ums-service
    install -m 0644 ${WORKDIR}/librescoot-ums.service ${D}${systemd_system_unitdir}
}
