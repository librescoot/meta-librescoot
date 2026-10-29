SUMMARY = "Overlay alpha control for IMX6 DBC boot transitions"
DESCRIPTION = "Controls the IPU overlay framebuffer alpha for seamless \
crossfade between boot animation and ScootUI"
LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/CC-BY-NC-SA-4.0;md5=277261f4a7c6c099822f8f5cb0180da0"

SRC_URI = "file://imx-overlay-alpha.c"

S = "${UNPACKDIR}"

do_compile() {
    ${CC} ${CFLAGS} ${LDFLAGS} -o ${B}/imx-overlay-alpha ${UNPACKDIR}/imx-overlay-alpha.c
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${B}/imx-overlay-alpha ${D}${bindir}/
}
