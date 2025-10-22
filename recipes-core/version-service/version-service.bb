SUMMARY = "LibreScoot Version Service"
HOMEPAGE = "https://github.com/librescoot/version-service"
LICENSE = "AGPL-3.0-or-later"
LIC_FILES_CHKSUM = "file://src/github.com/librescoot/version-service/LICENSE;md5=eb1e647870add0502f8f010b19de32af"

SRC_URI = "git://github.com/librescoot/version-service.git;protocol=https;branch=main"

SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

inherit go-mod systemd

GO_IMPORT = "github.com/librescoot/version-service"

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

FILES:${PN} = "/usr/lib/systemd/system/librescoot-version.service"
FILES:${PN} += "/usr/bin/version-service"

SYSTEMD_SERVICE:${PN} = "librescoot-version.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install:unu-mdb() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}

    install -m 0755 ${B}/bin/linux_arm/version-service ${D}${bindir}/
    install -m 0644 ${B}/src/github.com/librescoot/version-service/version-service-mdb.service ${D}${systemd_system_unitdir}/librescoot-version.service
}

do_install:unu-dbc() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}

    install -m 0755 ${B}/bin/linux_arm/version-service ${D}${bindir}/
    install -m 0644 ${B}/src/github.com/librescoot/version-service/version-service-dbc.service ${D}${systemd_system_unitdir}/librescoot-version.service
}

do_install:librescoot-dbc-rpi4() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}

    mv ${B}/bin/linux_arm64 ${B}/bin/linux_arm || true

    install -m 0755 ${B}/bin/linux_arm/version-service ${D}${bindir}/
    install -m 0644 ${B}/src/github.com/librescoot/version-service/version-service-dbc.service ${D}${systemd_system_unitdir}/librescoot-version.service
}
