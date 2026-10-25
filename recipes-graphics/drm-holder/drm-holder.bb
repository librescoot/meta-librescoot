SUMMARY = "Keeps /dev/dri/card1 open to suppress imx-drm lastclose modeset flash"
DESCRIPTION = "Tiny helper that holds an fd on the primary DRM node so that \
scootui-qt can exit promptly on SIGTERM without triggering a visible modeset \
flash during imx-drm's fb_helper lastclose path."
LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/CC-BY-NC-SA-4.0;md5=277261f4a7c6c099822f8f5cb0180da0"

SRC_URI = " \
    file://drm-holder.c \
    file://scootui-drm-holder.service \
"

COMPATIBLE_MACHINE = "unu-dbc"

inherit systemd

SYSTEMD_SERVICE:${PN} = "scootui-drm-holder.service"
SYSTEMD_AUTO_ENABLE:${PN} = "disable"

S = "${UNPACKDIR}"

do_compile() {
    ${CC} ${CFLAGS} ${LDFLAGS} -Os -o ${B}/drm-holder ${UNPACKDIR}/drm-holder.c
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${B}/drm-holder ${D}${bindir}/drm-holder

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/scootui-drm-holder.service ${D}${systemd_system_unitdir}/
}

FILES:${PN} += "${systemd_system_unitdir}/scootui-drm-holder.service"
