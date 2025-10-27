SUMMARY = "LibreScoot Carplay Service"
HOMEPAGE = "https://github.com/librescoot/carplay-service"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://src/github.com/mzyy94/gocarplay/LICENSE;md5=6560d1f6d5f413db4997dffd12eed3ce"

SRC_URI = "git://github.com/librescoot/carplay-service.git;protocol=https;branch=master"

SRCREV = "${AUTOREV}"

DEPENDS += "libusb"

S = "${WORKDIR}/git"

inherit go-mod systemd

GO_IMPORT = "github.com/mzyy94/gocarplay"

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

do_install:prepend:librescoot-dbc-rpi4() {
    mv ${B}/bin/linux_arm64 ${B}/bin/linux_arm
}

do_install() {
    install -d ${D}${bindir}

    install -m 0755 ${B}/bin/linux_arm/settings-service ${D}${bindir}/settings-service
}
