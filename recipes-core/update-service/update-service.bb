SUMMARY = "LibreScoot Update Service"
HOMEPAGE = "https://github.com/librescoot/update-service"
LICENSE = "AGPL-3.0-only"
LIC_FILES_CHKSUM = "file://src/github.com/librescoot/update-service/LICENSE;md5=eb1e647870add0502f8f010b19de32af"

SRC_URI = "git://github.com/librescoot/update-service.git;protocol=https;branch=main"
SRC_URI += "file://mender-apply-delta.py"

SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

inherit go-mod systemd

GO_IMPORT = "github.com/librescoot/update-service"

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

FILES:${PN} += "/usr/lib/systemd/system/librescoot-update.service"
FILES:${PN} += "/usr/bin/mender-apply-delta.py"

# MDB gets mdb service, DBC gets dbc service
SYSTEMD_SERVICE:${PN} = "librescoot-update.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install:prepend:librescoot-dbc-rpi5() {
    mv ${B}/bin/linux_arm64 ${B}/bin/linux_arm || true
}

do_install() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}

    # Install binary
    install -m 0755 ${B}/bin/linux_arm/update-service ${D}${bindir}/
    install -m 0755 ${WORKDIR}/mender-apply-delta.py ${D}/usr/bin/
    
    # Install appropriate systemd service based on device type
    if [ "${MACHINE}" = "unu-mdb" ]; then
        install -m 0644 ${B}/src/github.com/librescoot/update-service/librescoot-update-mdb.service ${D}${systemd_system_unitdir}/librescoot-update.service
    elif [ "${MACHINE}" = "unu-dbc" ]; then
        install -m 0644 ${B}/src/github.com/librescoot/update-service/librescoot-update-dbc.service ${D}${systemd_system_unitdir}/librescoot-update.service
    elif [ "${MACHINE}" = "librescoot-dbc-rpi5" ]; then
        install -m 0644 ${B}/src/github.com/librescoot/update-service/librescoot-update-dbc.service ${D}${systemd_system_unitdir}/librescoot-update.service
    fi
}
