SUMMARY = "LibreScoot DBC Display Dispatcher"
HOMEPAGE = "https://github.com/librescoot/dbc-dispatcher"
LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=444cf8f9f11901e2fa0b24a5562ca5fc"

SRC_URI = "git://github.com/librescoot/dbc-dispatcher.git;protocol=https;branch=main"
SRC_URI += " file://dbc-dispatcher.service"
SRC_URI += " file://dbc-dispatcher-rpi4.service"

SRCREV = "${AUTOREV}"
PV = "0.3.0+git"

S = "${WORKDIR}/git"

DEPENDS = "systemd hiredis"

inherit systemd pkgconfig

RDEPENDS:${PN} += "scootui-qt"

FILES:${PN} += "/usr/lib/systemd/system/dbc-dispatcher.service"

SYSTEMD_SERVICE:${PN} = "dbc-dispatcher.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_compile() {
    cd ${S}
    GITDIR="${S}"
    VERSION=$(cd $GITDIR && git describe --tags --always --dirty 2>/dev/null || echo "dev")

    ${CC} ${CFLAGS} ${LDFLAGS} \
        $(pkg-config --cflags libsystemd hiredis) \
        -DVERSION=\"${VERSION}\" \
        -o ${B}/dbc-dispatcher ${S}/src/main.c \
        $(pkg-config --libs libsystemd hiredis)
}

do_install() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}

    install -m 0755 ${B}/dbc-dispatcher ${D}${bindir}/dbc-dispatcher
    install -m 0644 ${WORKDIR}/dbc-dispatcher.service ${D}${systemd_system_unitdir}
}

do_install:append:librescoot-dbc-rpi4() {
    install -m 0644 ${WORKDIR}/dbc-dispatcher-rpi4.service ${D}${systemd_system_unitdir}/dbc-dispatcher.service
}
