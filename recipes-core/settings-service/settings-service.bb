SUMMARY = "LibreScoot Settings Service"
HOMEPAGE = "https://github.com/librescoot/settings-service"
LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://src/github.com/librescoot/settings-service/LICENSE;md5=fb5d051e53001fdff7fec0f368f47190"

SRC_URI = "git://github.com/librescoot/settings-service.git;protocol=https;branch=main"
SRC_URI += " file://librescoot-settings.service"

SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

inherit librescoot-go systemd

GO_IMPORT = "github.com/librescoot/settings-service"

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

FILES:${PN} += "/usr/lib/systemd/system/librescoot-settings.service /usr/share/settings-service"

SYSTEMD_SERVICE:${PN} = "librescoot-settings.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"


do_install() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}
    install -d ${D}/usr/share/settings-service

    install -m 0755 ${B}/bin/linux_arm/settings-service ${D}${bindir}/settings-service
    install -m 0644 ${WORKDIR}/librescoot-settings.service ${D}${systemd_system_unitdir}
    install -m 0644 ${S}/src/${GO_IMPORT}/settings.schema.json ${D}/usr/share/settings-service/settings.schema.json
}
