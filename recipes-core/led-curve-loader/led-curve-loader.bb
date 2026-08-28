SUMMARY = "Loads PWM-LED fade/cue curves without vehicle-service"
DESCRIPTION = "Standalone loader for the imx-pwm-led driver's fade and cue \
tables, for images that don't ship vehicle-service"
LICENSE = "CLOSED"
LIC_FILES_CHKSUM = ""

SRC_URI = "file://led-curve-loader.c"
SRC_URI += "file://led-curve-loader.service"

S = "${UNPACKDIR}"

inherit systemd

# Nothing else on an image without vehicle-service loads the curves, so the
# loader runs itself. Ordered like the boot LED: as soon as the driver exists.
SYSTEMD_SERVICE:${PN} = "led-curve-loader.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

RDEPENDS:${PN} += "led-curves"

do_compile() {
    ${CC} ${CFLAGS} ${LDFLAGS} -o ${B}/led-curve-loader ${UNPACKDIR}/led-curve-loader.c
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${B}/led-curve-loader ${D}${bindir}/

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/led-curve-loader.service ${D}${systemd_system_unitdir}/
}
