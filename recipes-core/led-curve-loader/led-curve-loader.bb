SUMMARY = "Loads PWM-LED fade/cue curves without vehicle-service"
DESCRIPTION = "Standalone loader for the imx-pwm-led driver's fade and cue \
tables, for images that don't ship vehicle-service"
LICENSE = "CLOSED"
LIC_FILES_CHKSUM = ""

SRC_URI = "file://led-curve-loader.c"

S = "${UNPACKDIR}"

RDEPENDS:${PN} += "led-curves"

do_compile() {
    ${CC} ${CFLAGS} ${LDFLAGS} -o ${B}/led-curve-loader ${UNPACKDIR}/led-curve-loader.c
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${B}/led-curve-loader ${D}${bindir}/
}
