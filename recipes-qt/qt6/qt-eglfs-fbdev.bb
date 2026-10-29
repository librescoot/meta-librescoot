SUMMARY = "Qt EGLFS framebuffer device integration"
DESCRIPTION = "GPU rendering into fbdev without taking DRM display ownership."
HOMEPAGE = "https://github.com/librescoot/qt-eglfs-fbdev"
LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://LICENSE.CC-BY-NC-SA-4.0;md5=fb5d051e53001fdff7fec0f368f47190"

SRC_URI = "git://github.com/librescoot/qt-eglfs-fbdev.git;branch=main;protocol=https"
SRCREV = "8553f62498c5ead9330ac1e41bb5f6fd3b283010"
PV = "0.1.1+git${SRCPV}"

inherit cmake qt6-cmake pkgconfig

DEPENDS = "qtbase virtual/libgbm virtual/egl"
EXTRA_OECMAKE = "-DQT6_INSTALL_PLUGINS=${libdir}/plugins"
FILES:${PN} = "${libdir}/plugins/egldeviceintegrations/*.so"
