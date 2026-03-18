SUMMARY = "LibreScoot DBC Display Dispatcher"
HOMEPAGE = "https://github.com/librescoot/dbc-dispatcher"
LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://src/github.com/librescoot/dbc-dispatcher/LICENSE;md5=444cf8f9f11901e2fa0b24a5562ca5fc"

SRC_URI = "git://github.com/librescoot/dbc-dispatcher.git;protocol=https;branch=main"
SRC_URI += " file://dbc-dispatcher.service"
SRC_URI += " file://dbc-dispatcher-rpi4.service"

SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

inherit librescoot-go systemd

GO_IMPORT = "github.com/librescoot/dbc-dispatcher"

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

RDEPENDS:${PN} += "scootui-qt"

FILES:${PN} += "/usr/lib/systemd/system/dbc-dispatcher.service"

SYSTEMD_SERVICE:${PN} = "dbc-dispatcher.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install:prepend:librescoot-dbc-rpi4() {
    mv ${B}/bin/linux_arm64 ${B}/bin/linux_arm || true
}

do_install() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}

    install -m 0755 ${B}/bin/linux_arm/dbc-dispatcher ${D}${bindir}/dbc-dispatcher
    install -m 0644 ${WORKDIR}/dbc-dispatcher.service ${D}${systemd_system_unitdir}
}

do_install:append:librescoot-dbc-rpi4() {
    install -m 0644 ${WORKDIR}/dbc-dispatcher-rpi4.service ${D}${systemd_system_unitdir}/dbc-dispatcher.service
}
