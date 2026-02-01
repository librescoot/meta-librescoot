SUMMARY = "LibreScoot Vehicle Service"
HOMEPAGE = "https://github.com/librescoot/vehicle-service"
LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://src/vehicle-service/LICENSE;md5=fb5d051e53001fdff7fec0f368f47190"

SRC_URI = "git://github.com/librescoot/vehicle-service.git;protocol=https;branch=main"
SRC_URI:append = " file://librescoot-vehicle.service"

# LED cue files (light state transitions)
SRC_URI:append = " ${@' '.join(['file://cues/cue%d-%s' % (i, n) for i, n in enumerate([ \
    'all_off', \
    'standby_to_parked_brake_off', \
    'standby_to_parked_brake_on', \
    'parked_to_drive', \
    'brake_off_to_brake_on', \
    'brake_on_to_brake_off', \
    'drive_to_parked', \
    'parked_brake_off_to_standby', \
    'parked_brake_on_to_standby', \
    'blink_none', \
    'blink_left', \
    'blink_right', \
    'blink_both', \
])])}"

# LED fade files (brightness transitions)
SRC_URI:append = " ${@' '.join(['file://fades/fade%d-%s' % (i, n) for i, n in enumerate([ \
    'parking-smooth-on', \
    'smooth-off', \
    'brake-linear-on', \
    'brake-linear-off', \
    'brake-dim-on', \
    'brake-half-to-full', \
    'drive-light-on', \
    'brake-full-to-half', \
    'drive-light-off', \
    'brake-dim-off', \
    'blink', \
])])}"

# Pinned 2026-01-28: deps: bump librefsm v0.3.3, redis-ipc v0.10.3
SRCREV = "9e51cea1e2cfc0ad7a631cf7044129d77fc1ccc2"

S = "${WORKDIR}/git"

inherit librescoot-go systemd

GO_IMPORT = "vehicle-service"

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

CURVE_DIR = "/usr/share/led-curves"

FILES:${PN} += "${CURVE_DIR}/fades/* ${CURVE_DIR}/cues/*"
FILES:${PN} += "${systemd_system_unitdir}/librescoot-vehicle.service"

SYSTEMD_SERVICE:${PN} = "librescoot-vehicle.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d ${D}${bindir}
    install -d ${D}${CURVE_DIR}/fades
    install -d ${D}${CURVE_DIR}/cues
    install -d ${D}${systemd_system_unitdir}

    install -m 0755 ${B}/bin/linux_arm/vehicle-service ${D}${bindir}/

    # Install all cue and fade files using loops
    for f in ${WORKDIR}/cues/*; do
        [ -f "$f" ] && install -m 0644 "$f" ${D}${CURVE_DIR}/cues/
    done

    for f in ${WORKDIR}/fades/*; do
        [ -f "$f" ] && install -m 0644 "$f" ${D}${CURVE_DIR}/fades/
    done

    install -m 0644 ${WORKDIR}/librescoot-vehicle.service ${D}${systemd_system_unitdir}
}
