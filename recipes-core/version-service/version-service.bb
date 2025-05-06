SUMMARY = "LibreScoot Version Service"
HOMEPAGE = "https://github.com/librescoot/version-service"
LICENSE = "AGPL-3.0-or-later"
LIC_FILES_CHKSUM = "file://src/github.com/librescoot/version-service/LICENSE.md;md5=d41d8cd98f00b204e9800998ecf8427e"

SRC_URI = "git://github.com/librescoot/version-service.git;protocol=https;branch=main"

SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

inherit go-mod systemd

GO_IMPORT = "github.com/librescoot/version-service"

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

FILES:${PN} = "/usr/lib/systemd/system/version-service.service"
FILES:${PN} += "/usr/bin/version-service"

SYSTEMD_SERVICE:${PN} = "version-service.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install:librescoot-mdb() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}

    install -m 0755 ${B}/bin/linux_arm/version-service ${D}${bindir}/
    install -m 0644 ${B}/src/github.com/librescoot/version-service/version-service-mdb.service ${D}${systemd_system_unitdir}/version-service.service
}

do_install:librescoot-dbc() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}

    install -m 0755 ${B}/bin/linux_arm/version-service ${D}${bindir}/
    install -m 0644 ${B}/src/github.com/librescoot/version-service/version-service-dbc.service ${D}${systemd_system_unitdir}/version-service.service
}
