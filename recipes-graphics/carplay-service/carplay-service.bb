SUMMARY = "LibreScoot Carplay Service"
HOMEPAGE = "https://github.com/librescoot/carplay-service"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://src/github.com/mzyy94/gocarplay/LICENSE;md5=6560d1f6d5f413db4997dffd12eed3ce"

SRC_URI = "git://github.com/librescoot/carplay-service.git;protocol=https;branch=master"
SRC_URI += "file://librescoot-carplay.service"

SRCREV = "${AUTOREV}"

DEPENDS += "libusb"

S = "${WORKDIR}/git"

inherit librescoot-go systemd pkgconfig

GO_IMPORT = "github.com/mzyy94/gocarplay"

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

SYSTEMD_SERVICE:${PN}:librescoot-dbc-rpi4 = "librescoot-carplay.service"
SYSTEMD_AUTO_ENABLE:${PN}:librescoot-dbc-rpi4 = "enable"

do_install:prepend:librescoot-dbc-rpi4() {
    mv ${B}/bin/linux_arm64 ${B}/bin/linux_arm || true
}

do_install() {
    install -d ${D}${bindir}

    install -m 0755 ${B}/bin/linux_arm/server ${D}${bindir}/carplay-service
}

do_install:append:librescoot-dbc-rpi4() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/librescoot-carplay.service ${D}${systemd_system_unitdir}/librescoot-carplay.service
}
