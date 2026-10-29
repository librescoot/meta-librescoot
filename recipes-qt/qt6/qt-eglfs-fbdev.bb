SUMMARY = "Qt EGLFS framebuffer device integration"
DESCRIPTION = "GPU rendering into fbdev without taking DRM display ownership."
HOMEPAGE = "https://github.com/librescoot/qt-eglfs-fbdev"
LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://LICENSE.CC-BY-NC-SA-4.0;md5=fb5d051e53001fdff7fec0f368f47190"

SRC_URI = "git://github.com/librescoot/qt-eglfs-fbdev.git;branch=main;protocol=https"
SRCREV = "da339e2324996dc525d7c7f9464521854533aa29"
PV = "0.1.0+git${SRCPV}"

inherit cmake qt6-cmake pkgconfig

DEPENDS = "qtbase virtual/libgbm virtual/egl"
EXTRA_OECMAKE = "-DQT6_INSTALL_PLUGINS=${libdir}/plugins"
FILES:${PN} = "${libdir}/plugins/egldeviceintegrations/*.so"
