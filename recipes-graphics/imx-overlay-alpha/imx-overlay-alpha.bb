SUMMARY = "Overlay alpha control for IMX6 DBC boot transitions"
DESCRIPTION = "Controls the IPU overlay framebuffer alpha for seamless \
crossfade between boot animation and Flutter UI"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://imx-overlay-alpha.c"

S = "${UNPACKDIR}"

do_compile() {
    ${CC} ${CFLAGS} ${LDFLAGS} -o ${B}/imx-overlay-alpha ${UNPACKDIR}/imx-overlay-alpha.c
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${B}/imx-overlay-alpha ${D}${bindir}/
}
