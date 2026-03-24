SUMMARY = "Qt EGLFS fbdev device integration plugin"
DESCRIPTION = "Renders via GPU render node, blits to framebuffer. No DRM modesetting."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "git://github.com/librescoot/qt-eglfs-fbdev.git;branch=main;protocol=https"
SRCREV = "${AUTOREV}"
PV = "1.0.0+git${SRCPV}"

S = "${WORKDIR}/git"

inherit cmake qt6-cmake pkgconfig

DEPENDS = " \
    qtbase \
    virtual/libgbm \
    virtual/egl \
"

FILES:${PN} = "${libdir}/qt6/plugins/egldeviceintegrations/*.so"
