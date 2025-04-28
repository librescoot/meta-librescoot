SUMMARY = "Simple Mender Update Tool"
HOMEPAGE = "https://github.com/librescoot/smut"
LICENSE = "AGPL-3.0-or-later"
LIC_FILES_CHKSUM = "file://src/smut/LICENSE.md;md5=e6e0db88a9121d8ac23716cccf48bd3b"

SRC_URI = "git://github.com/librescoot/smut.git;protocol=https;branch=main"

SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

inherit go-mod systemd

GO_IMPORT = "smut"

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

FILES:${PN} = "/usr/lib/systemd/system/smut.service"

SYSTEMD_SERVICE:${PN} = "smut.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install:librescoot-mdb() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}

    install -m 0755 ${B}/bin/linux_arm/smut ${D}${bindir}/
    install -m 0644 ${WORKDIR}/smut@mdb.service ${D}${systemd_system_unitdir}/smut.service
}

do_install:librescoot-dbc() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}

    install -m 0755 ${B}/bin/linux_arm/smut ${D}${bindir}/
    install -m 0644 ${WORKDIR}/smut@dbc.service ${D}${systemd_system_unitdir}/smut.service
}