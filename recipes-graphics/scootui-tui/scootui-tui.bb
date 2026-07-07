SUMMARY = "ScootUI TUI Dashboard"
DESCRIPTION = "Terminal-based scooter dashboard for 480x480 framebuffer console"
HOMEPAGE = "https://github.com/librescoot/scootui-tui"
LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://src/github.com/librescoot/scootui-tui/LICENSE;md5=444cf8f9f11901e2fa0b24a5562ca5fc"

SCOOTUI_TUI_BRANCH ??= "main"
SCOOTUI_TUI_SRCREV ??= "${AUTOREV}"
SRCREV = "${SCOOTUI_TUI_SRCREV}"
SRC_URI = "git://github.com/librescoot/scootui-tui.git;branch=${SCOOTUI_TUI_BRANCH};protocol=https;destsuffix=${GO_SRCURI_DESTSUFFIX}"
SRC_URI += " file://scootui-tui.service"

PV = "1.0.0+git"


inherit librescoot-go systemd

GO_IMPORT = "github.com/librescoot/scootui-tui"

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

FILES:${PN} += "${systemd_system_unitdir}/scootui-tui.service"

SYSTEMD_SERVICE:${PN} = "scootui-tui.service"
SYSTEMD_AUTO_ENABLE:${PN} = "disable"

do_install:prepend:librescoot-dbc-rpi4() {
    mv ${B}/bin/linux_arm64 ${B}/bin/linux_arm || true
}

do_install() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}

    install -m 0755 ${B}/bin/linux_arm/scootui-tui ${D}${bindir}/scootui-tui
    install -m 0644 ${UNPACKDIR}/scootui-tui.service ${D}${systemd_system_unitdir}
}
