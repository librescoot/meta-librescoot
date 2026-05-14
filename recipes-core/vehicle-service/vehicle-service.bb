SUMMARY = "Librescoot Vehicle Service"
HOMEPAGE = "https://github.com/librescoot/vehicle-service"
LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://src/vehicle-service/LICENSE;md5=fb5d051e53001fdff7fec0f368f47190"

SRC_URI = "git://github.com/librescoot/vehicle-service.git;protocol=https;branch=main"
SRC_URI:append = " file://librescoot-vehicle.service"

# Cue files
SRC_URI:append = " file://cues/cue0-all_off"
SRC_URI:append = " file://cues/cue1-standby_to_parked_brake_off"
SRC_URI:append = " file://cues/cue2-standby_to_parked_brake_on"
SRC_URI:append = " file://cues/cue3-parked_to_drive"
SRC_URI:append = " file://cues/cue4-brake_off_to_brake_on"
SRC_URI:append = " file://cues/cue5-brake_on_to_brake_off"
SRC_URI:append = " file://cues/cue6-drive_to_parked"
SRC_URI:append = " file://cues/cue7-parked_brake_off_to_standby"
SRC_URI:append = " file://cues/cue8-parked_brake_on_to_standby"
SRC_URI:append = " file://cues/cue9-blink_none"
SRC_URI:append = " file://cues/cue10-blink_left"
SRC_URI:append = " file://cues/cue11-blink_right"
SRC_URI:append = " file://cues/cue12-blink_both"

# Fade files
SRC_URI:append = " file://fades/fade0-parking-smooth-on"
SRC_URI:append = " file://fades/fade1-smooth-off"
SRC_URI:append = " file://fades/fade2-brake-linear-on"
SRC_URI:append = " file://fades/fade3-brake-linear-off"
SRC_URI:append = " file://fades/fade4-brake-dim-on"
SRC_URI:append = " file://fades/fade5-brake-half-to-full"
SRC_URI:append = " file://fades/fade6-drive-light-on"
SRC_URI:append = " file://fades/fade7-brake-full-to-half"
SRC_URI:append = " file://fades/fade8-drive-light-off"
SRC_URI:append = " file://fades/fade9-brake-dim-off"
SRC_URI:append = " file://fades/fade10-blink"
SRC_URI:append = " file://fades/fade11-licence-on"
SRC_URI:append = " file://fades/fade12-licence-off"

SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

inherit librescoot-go systemd

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

    # Install cue files
    install -m 0644 ${WORKDIR}/cues/cue0-all_off ${D}${CURVE_DIR}/cues/
    install -m 0644 ${WORKDIR}/cues/cue1-standby_to_parked_brake_off ${D}${CURVE_DIR}/cues/
    install -m 0644 ${WORKDIR}/cues/cue2-standby_to_parked_brake_on ${D}${CURVE_DIR}/cues/
    install -m 0644 ${WORKDIR}/cues/cue3-parked_to_drive ${D}${CURVE_DIR}/cues/
    install -m 0644 ${WORKDIR}/cues/cue4-brake_off_to_brake_on ${D}${CURVE_DIR}/cues/
    install -m 0644 ${WORKDIR}/cues/cue5-brake_on_to_brake_off ${D}${CURVE_DIR}/cues/
    install -m 0644 ${WORKDIR}/cues/cue6-drive_to_parked ${D}${CURVE_DIR}/cues/
    install -m 0644 ${WORKDIR}/cues/cue7-parked_brake_off_to_standby ${D}${CURVE_DIR}/cues/
    install -m 0644 ${WORKDIR}/cues/cue8-parked_brake_on_to_standby ${D}${CURVE_DIR}/cues/
    install -m 0644 ${WORKDIR}/cues/cue9-blink_none ${D}${CURVE_DIR}/cues/
    install -m 0644 ${WORKDIR}/cues/cue10-blink_left ${D}${CURVE_DIR}/cues/
    install -m 0644 ${WORKDIR}/cues/cue11-blink_right ${D}${CURVE_DIR}/cues/
    install -m 0644 ${WORKDIR}/cues/cue12-blink_both ${D}${CURVE_DIR}/cues/

    # Install fade files
    install -m 0644 ${WORKDIR}/fades/fade0-parking-smooth-on ${D}${CURVE_DIR}/fades/
    install -m 0644 ${WORKDIR}/fades/fade1-smooth-off ${D}${CURVE_DIR}/fades/
    install -m 0644 ${WORKDIR}/fades/fade2-brake-linear-on ${D}${CURVE_DIR}/fades/
    install -m 0644 ${WORKDIR}/fades/fade3-brake-linear-off ${D}${CURVE_DIR}/fades/
    install -m 0644 ${WORKDIR}/fades/fade4-brake-dim-on ${D}${CURVE_DIR}/fades/
    install -m 0644 ${WORKDIR}/fades/fade5-brake-half-to-full ${D}${CURVE_DIR}/fades/
    install -m 0644 ${WORKDIR}/fades/fade6-drive-light-on ${D}${CURVE_DIR}/fades/
    install -m 0644 ${WORKDIR}/fades/fade7-brake-full-to-half ${D}${CURVE_DIR}/fades/
    install -m 0644 ${WORKDIR}/fades/fade8-drive-light-off ${D}${CURVE_DIR}/fades/
    install -m 0644 ${WORKDIR}/fades/fade9-brake-dim-off ${D}${CURVE_DIR}/fades/
    install -m 0644 ${WORKDIR}/fades/fade10-blink ${D}${CURVE_DIR}/fades/
    install -m 0644 ${WORKDIR}/fades/fade11-licence-on ${D}${CURVE_DIR}/fades/
    install -m 0644 ${WORKDIR}/fades/fade12-licence-off ${D}${CURVE_DIR}/fades/

    install -m 0644 ${WORKDIR}/librescoot-vehicle.service ${D}${systemd_system_unitdir}
}

