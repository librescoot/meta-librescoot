SUMMARY = "Librescoot Data Server"
HOMEPAGE = "https://github.com/librescoot/data-server"
LICENSE = "AGPL-3.0-or-later"
LIC_FILES_CHKSUM = "file://src/data-server/LICENSE;md5=eb1e647870add0502f8f010b19de32af"

SRC_URI = "git://github.com/librescoot/data-server.git;protocol=https;branch=main;destsuffix=${GO_SRCURI_DESTSUFFIX}"
SRC_URI += " file://librescoot-data-server.service"

SRCREV = "${AUTOREV}"


inherit librescoot-go systemd

GO_IMPORT = "data-server"

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

FILES:${PN} += "/usr/lib/systemd/system/librescoot-data-server.service"

SYSTEMD_SERVICE:${PN} = "librescoot-data-server.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

# The unit contains a board-specific service address, also routed over PPP.
PACKAGE_ARCH = "${MACHINE_ARCH}"
DATA_SERVER_ADDR = "127.0.0.1:8080"
DATA_SERVER_ADDR:unu-mdb = "192.168.7.1:8080"
DATA_SERVER_ADDR:unu-dbc = "192.168.7.2:8080"
DATA_SERVER_ADDR:librescoot-dbc-rpi4 = "192.168.7.2:8080"

RDEPENDS:${PN}:append:unu-mdb = " mdb-netconfig"
RDEPENDS:${PN}:append:unu-dbc = " dbc-netconfig"
RDEPENDS:${PN}:append:librescoot-dbc-rpi4 = " dbc-netconfig"

do_install() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}
    install -m 0755 ${B}/bin/linux_arm/data-server ${D}${bindir}/
    sed -e 's|@DATA_SERVER_ADDR@|${DATA_SERVER_ADDR}|g' \
        ${UNPACKDIR}/librescoot-data-server.service > ${D}${systemd_system_unitdir}/librescoot-data-server.service
    chmod 0644 ${D}${systemd_system_unitdir}/librescoot-data-server.service
}
