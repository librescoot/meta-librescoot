SUMMARY = "LibreScoot Vehicle Service"
HOMEPAGE = "https://github.com/librescoot/vehicle-service"
LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://src/vehicle-service/LICENSE;md5=fb5d051e53001fdff7fec0f368f47190"

SRC_URI = "git://github.com/librescoot/vehicle-service.git;protocol=https;branch=main"
SRC_URI:append = " file://cues"
SRC_URI:append = " file://fades"
SRC_URI:append = " file://librescoot-vehicle.service"

SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

inherit go-mod systemd

GO_IMPORT = "vehicle-service"

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

CURVE_DIR = "/usr/share/led-curves"

FILES:${PN} += "${CURVE_DIR}/fades/*"
FILES:${PN} += "${CURVE_DIR}/cues/*"
FILES:${PN} += "/usr/lib/systemd/system/librescoot-vehicle.service"

SYSTEMD_SERVICE:${PN} = "librescoot-vehicle.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d ${D}${bindir}
    install -d ${D}${CURVE_DIR}/fades
    install -d ${D}${CURVE_DIR}/cues
    install -d ${D}${systemd_system_unitdir}

    install -m 0755 ${B}/bin/linux_arm/vehicle-service ${D}${bindir}/
    install -m 0644 ${WORKDIR}/fades/* ${D}${CURVE_DIR}/fades/
    install -m 0644 ${WORKDIR}/cues/* ${D}${CURVE_DIR}/cues/
    install -m 0644 ${WORKDIR}/librescoot-vehicle.service ${D}${systemd_system_unitdir}
}

