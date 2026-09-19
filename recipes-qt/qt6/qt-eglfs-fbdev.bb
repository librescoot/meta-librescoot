SUMMARY = "Qt EGLFS fbdev device integration plugin"
DESCRIPTION = "Renders through the GPU render node and presents to fbdev without DRM modesetting."
LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=fb5d051e53001fdff7fec0f368f47190"

SRC_URI = "git://github.com/librescoot/qt-eglfs-fbdev.git;branch=main;protocol=https \
           file://0001-headless-use-Qt-EGL-no-X11-types.patch \
"
SRCREV = "d2e413141651bdd69314422926f73b87189cc69d"
PV = "1.0.0+git${SRCPV}"
S = "${WORKDIR}/git"

inherit cmake qt6-cmake pkgconfig

DEPENDS = "qtbase virtual/libgbm virtual/egl"

FILES:${PN} = "${libdir}/plugins/egldeviceintegrations/*.so"
