SUMMARY = "Fade and cue data for the PWM-LED driver"
DESCRIPTION = "Curve data loaded into the imx-pwm-led driver so PLAY_FADE and PLAY_CUE work"
LICENSE = "CLOSED"
LIC_FILES_CHKSUM = ""

CURVE_DIR = "/usr/share/led-curves"

SRC_URI = " \
    file://cues/cue0-all_off \
    file://cues/cue1-standby_to_parked_brake_off \
    file://cues/cue2-standby_to_parked_brake_on \
    file://cues/cue3-parked_to_drive \
    file://cues/cue4-brake_off_to_brake_on \
    file://cues/cue5-brake_on_to_brake_off \
    file://cues/cue6-drive_to_parked \
    file://cues/cue7-parked_brake_off_to_standby \
    file://cues/cue8-parked_brake_on_to_standby \
    file://cues/cue9-blink_none \
    file://cues/cue10-blink_left \
    file://cues/cue11-blink_right \
    file://cues/cue12-blink_both \
    file://fades/fade0-parking-smooth-on \
    file://fades/fade1-smooth-off \
    file://fades/fade2-brake-linear-on \
    file://fades/fade3-brake-linear-off \
    file://fades/fade4-brake-dim-on \
    file://fades/fade5-brake-half-to-full \
    file://fades/fade6-drive-light-on \
    file://fades/fade7-brake-full-to-half \
    file://fades/fade8-drive-light-off \
    file://fades/fade9-brake-dim-off \
    file://fades/fade10-blink \
    file://fades/fade11-licence-on \
    file://fades/fade12-licence-off \
"

FILES:${PN} += "${CURVE_DIR}/fades/*"
FILES:${PN} += "${CURVE_DIR}/cues/*"

do_install() {
    install -d ${D}${CURVE_DIR}/fades
    install -d ${D}${CURVE_DIR}/cues

    install -m 0644 ${UNPACKDIR}/cues/cue0-all_off ${D}${CURVE_DIR}/cues/
    install -m 0644 ${UNPACKDIR}/cues/cue1-standby_to_parked_brake_off ${D}${CURVE_DIR}/cues/
    install -m 0644 ${UNPACKDIR}/cues/cue2-standby_to_parked_brake_on ${D}${CURVE_DIR}/cues/
    install -m 0644 ${UNPACKDIR}/cues/cue3-parked_to_drive ${D}${CURVE_DIR}/cues/
    install -m 0644 ${UNPACKDIR}/cues/cue4-brake_off_to_brake_on ${D}${CURVE_DIR}/cues/
    install -m 0644 ${UNPACKDIR}/cues/cue5-brake_on_to_brake_off ${D}${CURVE_DIR}/cues/
    install -m 0644 ${UNPACKDIR}/cues/cue6-drive_to_parked ${D}${CURVE_DIR}/cues/
    install -m 0644 ${UNPACKDIR}/cues/cue7-parked_brake_off_to_standby ${D}${CURVE_DIR}/cues/
    install -m 0644 ${UNPACKDIR}/cues/cue8-parked_brake_on_to_standby ${D}${CURVE_DIR}/cues/
    install -m 0644 ${UNPACKDIR}/cues/cue9-blink_none ${D}${CURVE_DIR}/cues/
    install -m 0644 ${UNPACKDIR}/cues/cue10-blink_left ${D}${CURVE_DIR}/cues/
    install -m 0644 ${UNPACKDIR}/cues/cue11-blink_right ${D}${CURVE_DIR}/cues/
    install -m 0644 ${UNPACKDIR}/cues/cue12-blink_both ${D}${CURVE_DIR}/cues/

    install -m 0644 ${UNPACKDIR}/fades/fade0-parking-smooth-on ${D}${CURVE_DIR}/fades/
    install -m 0644 ${UNPACKDIR}/fades/fade1-smooth-off ${D}${CURVE_DIR}/fades/
    install -m 0644 ${UNPACKDIR}/fades/fade2-brake-linear-on ${D}${CURVE_DIR}/fades/
    install -m 0644 ${UNPACKDIR}/fades/fade3-brake-linear-off ${D}${CURVE_DIR}/fades/
    install -m 0644 ${UNPACKDIR}/fades/fade4-brake-dim-on ${D}${CURVE_DIR}/fades/
    install -m 0644 ${UNPACKDIR}/fades/fade5-brake-half-to-full ${D}${CURVE_DIR}/fades/
    install -m 0644 ${UNPACKDIR}/fades/fade6-drive-light-on ${D}${CURVE_DIR}/fades/
    install -m 0644 ${UNPACKDIR}/fades/fade7-brake-full-to-half ${D}${CURVE_DIR}/fades/
    install -m 0644 ${UNPACKDIR}/fades/fade8-drive-light-off ${D}${CURVE_DIR}/fades/
    install -m 0644 ${UNPACKDIR}/fades/fade9-brake-dim-off ${D}${CURVE_DIR}/fades/
    install -m 0644 ${UNPACKDIR}/fades/fade10-blink ${D}${CURVE_DIR}/fades/
    install -m 0644 ${UNPACKDIR}/fades/fade11-licence-on ${D}${CURVE_DIR}/fades/
    install -m 0644 ${UNPACKDIR}/fades/fade12-licence-off ${D}${CURVE_DIR}/fades/
}
