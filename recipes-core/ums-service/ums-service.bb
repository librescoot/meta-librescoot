SUMMARY = "LibreScoot UMS Service"
HOMEPAGE = "https://github.com/librescoot/ums-service"
LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://src/github.com/librescoot/ums-service/LICENSE;md5=136c671dba2d2f644b882e31c3e289e8"

SRC_URI = "git://github.com/librescoot/ums-service.git;protocol=https;branch=main"
SRC_URI += " file://librescoot-ums.service"

SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

inherit go-mod systemd

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
