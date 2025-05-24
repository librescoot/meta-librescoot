SUMMARY = "LibreScoot Update Service"
HOMEPAGE = "https://github.com/librescoot/update-service"
LICENSE = "AGPL-3.0-only"
LIC_FILES_CHKSUM = "file://src/github.com/librescoot/update-service/LICENSE;md5=eb1e647870add0502f8f010b19de32af"

SRC_URI = "git://github.com/librescoot/update-service.git;protocol=https;branch=main"

SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

inherit go-mod systemd

GO_IMPORT = "github.com/librescoot/update-service"

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

# MDB-specific service files
FILES:${PN}:librescoot-mdb += "${systemd_system_unitdir}/librescoot-update.service"
FILES:${PN}:librescoot-mdb += "${systemd_system_unitdir}/librescoot-update-fetcher.service"
FILES:${PN}:librescoot-mdb += "${systemd_system_unitdir}/librescoot-update-installer-mdb.service"

# DBC-specific service files
FILES:${PN}:librescoot-dbc += "${systemd_system_unitdir}/librescoot-update-installer-dbc.service"

# Binaries available on both platforms
FILES:${PN} += "${bindir}/update-service"
FILES:${PN} += "${bindir}/update-fetcher"
FILES:${PN} += "${bindir}/update-installer"

# MDB systemd services
SYSTEMD_SERVICE:${PN}:librescoot-mdb = "librescoot-update.service librescoot-update-fetcher.service librescoot-update-installer-mdb.service"
SYSTEMD_AUTO_ENABLE:${PN}:librescoot-mdb = "enable"

# DBC systemd service
SYSTEMD_SERVICE:${PN}:librescoot-dbc = "librescoot-update-installer-dbc.service"
SYSTEMD_AUTO_ENABLE:${PN}:librescoot-dbc = "enable"

do_install:librescoot-mdb() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}

    # Install all three binaries
    install -m 0755 ${B}/bin/linux_arm/update-service ${D}${bindir}/
    install -m 0755 ${B}/bin/linux_arm/update-fetcher ${D}${bindir}/
    install -m 0755 ${B}/bin/linux_arm/update-installer ${D}${bindir}/

    # Install MDB service files
    install -m 0644 ${B}/src/github.com/librescoot/update-service/librescoot-update.service ${D}${systemd_system_unitdir}/
    install -m 0644 ${B}/src/github.com/librescoot/update-service/librescoot-update-fetcher.service ${D}${systemd_system_unitdir}/
    install -m 0644 ${B}/src/github.com/librescoot/update-service/librescoot-update-installer-mdb.service ${D}${systemd_system_unitdir}/
}

do_install:librescoot-dbc() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}

    # Install installer and fetcher binaries on DBC
    install -m 0755 ${B}/bin/linux_arm/update-installer ${D}${bindir}/
    install -m 0755 ${B}/bin/linux_arm/update-fetcher ${D}${bindir}/

    # Install DBC service file
    install -m 0644 ${B}/src/github.com/librescoot/update-service/librescoot-update-installer-dbc.service ${D}${systemd_system_unitdir}/
}

