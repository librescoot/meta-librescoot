SUMMARY = "libqmi is a library for talking to WWAN devices by QMI protocol"
DESCRIPTION = "libqmi is a glib-based library for talking to WWAN modems and \
               devices which speak the Qualcomm MSM Interface (QMI) protocol"
HOMEPAGE = "http://www.freedesktop.org/wiki/Software/libqmi"
LICENSE = "GPL-2.0-or-later & LGPL-2.1-or-later"
LIC_FILES_CHKSUM = " \
    file://COPYING;md5=b234ee4d69f5fce4486a80fdaf4a4263 \
    file://COPYING.LIB;md5=4fbd65380cdd255951079008b364516c \
"

DEPENDS = "glib-2.0 glib-2.0-native"

inherit meson pkgconfig bash-completion gobject-introspection upstream-version-is-even

SRCREV = "5e4c88d1f8a2b438b55180e7d5070c47bd5b73a2"
SRC_URI = "git://gitlab.freedesktop.org/mobile-broadband/libqmi.git;protocol=https;branch=qmi-1-36"

S = "${WORKDIR}/git"

PACKAGECONFIG ??= "udev mbim"
PACKAGECONFIG[udev] = "-Dudev=true,-Dudev=false,libgudev"
PACKAGECONFIG[mbim] = "-Dmbim_qmux=true,-Dmbim_qmux=false,libmbim"
PACKAGECONFIG[qrtr] = "-Dqrtr=true,-Dqrtr=false,libqrtr-glib"

EXTRA_OEMESON = " \
    -Dgtk_doc=false \
    -Dman=false \
"