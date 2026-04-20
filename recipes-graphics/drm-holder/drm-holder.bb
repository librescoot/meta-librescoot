SUMMARY = "Keeps /dev/dri/card1 open to suppress imx-drm lastclose modeset flash"
DESCRIPTION = "Tiny helper that holds an fd on the primary DRM node so that \
scootui-qt can exit promptly on SIGTERM without triggering a visible modeset \
flash during imx-drm's fb_helper lastclose path."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " \
    file://drm-holder.c \
    file://scootui-drm-holder.service \
"

S = "${WORKDIR}"

COMPATIBLE_MACHINE = "unu-dbc"

inherit systemd

SYSTEMD_SERVICE:${PN} = "scootui-drm-holder.service"
SYSTEMD_AUTO_ENABLE:${PN} = "disable"

do_compile() {
    ${CC} ${CFLAGS} ${LDFLAGS} -Os -o ${B}/drm-holder ${S}/drm-holder.c
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${B}/drm-holder ${D}${bindir}/drm-holder

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/scootui-drm-holder.service ${D}${systemd_system_unitdir}/
}

FILES:${PN} += "${systemd_system_unitdir}/scootui-drm-holder.service"
