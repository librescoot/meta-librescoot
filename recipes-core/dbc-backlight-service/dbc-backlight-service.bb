SUMMARY = "LibreScoot Dashboard Controller Backlight Service"
HOMEPAGE = "https://github.com/librescoot/dbc-backlight-service"
LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://src/github.com/librescoot/dbc-backlight-service/LICENSE;md5=136c671dba2d2f644b882e31c3e289e8"

SRC_URI = "git://github.com/librescoot/dbc-backlight-service.git;protocol=https;branch=main"
SRC_URI += " file://dbc-backlight.service"

SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

inherit librescoot-go systemd

GO_IMPORT = "github.com/librescoot/dbc-backlight-service"

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

FILES:${PN} += "/usr/lib/systemd/system/dbc-backlight.service"

SYSTEMD_SERVICE:${PN} = "dbc-backlight.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install:prepend:librescoot-dbc-rpi4() {
    mv ${B}/bin/linux_arm64 ${B}/bin/linux_arm || true
}

do_install() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}

    install -m 0755 ${B}/bin/linux_arm/backlight-service ${D}${bindir}/backlight-service
    install -m 0644 ${WORKDIR}/dbc-backlight.service ${D}${systemd_system_unitdir}
}
