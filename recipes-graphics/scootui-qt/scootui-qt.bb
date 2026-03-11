SUMMARY = "ScootUI (Qt)"
DESCRIPTION = "ScootUI instrument cluster UI - Qt/QML port"
AUTHOR = "LibreScoot Contributors"
HOMEPAGE = "https://github.com/librescoot/scootui-qt"
SECTION = "graphics"

LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=fb5d051e53001fdff7fec0f368f47190"

SCOOTUI_QT_BRANCH ??= "main"
SCOOTUI_QT_SRCREV ??= "${AUTOREV}"
SRCREV = "${SCOOTUI_QT_SRCREV}"
SRC_URI = "git://github.com/librescoot/scootui-qt.git;branch=${SCOOTUI_QT_BRANCH};protocol=https"
SRC_URI += "file://scootui-qt.service"
SRC_URI += "file://scootui-qt-rpi4.service"
SRC_URI += "file://scootui-qt-kms.json"

PV = "1.0.0+git"

S = "${WORKDIR}/git"

inherit cmake qt6-cmake pkgconfig systemd

DEPENDS = " \
    qtbase \
    qtdeclarative \
    qtdeclarative-native \
    qtshadertools \
    qtsvg \
    qttools \
    qmaplibre \
"

RDEPENDS:${PN} = " \
    qtbase \
    qtbase-plugins \
    qtdeclarative \
    qtdeclarative-qmlplugins \
    qtsvg \
    qtsvg-plugins \
    qtlocation \
    qmaplibre \
"

EXTRA_OECMAKE = "-DCMAKE_BUILD_TYPE=RelWithDebInfo"
INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${B}/bin/scootui ${D}${bindir}/scootui-qt
}

SYSTEMD_SERVICE:${PN} = "scootui-qt.service"
SYSTEMD_AUTO_ENABLE:${PN} = "disable"

do_install:append:unu-dbc() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/scootui-qt.service ${D}${systemd_system_unitdir}/
    install -d ${D}${sysconfdir}
    install -m 0644 ${WORKDIR}/scootui-qt-kms.json ${D}${sysconfdir}/
}

do_install:append:librescoot-dbc-rpi4() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/scootui-qt-rpi4.service ${D}${systemd_system_unitdir}/scootui-qt.service
}

FILES:${PN} = " \
    ${bindir}/scootui-qt \
    ${sysconfdir}/scootui-qt-kms.json \
    ${systemd_system_unitdir}/scootui-qt.service \
"
