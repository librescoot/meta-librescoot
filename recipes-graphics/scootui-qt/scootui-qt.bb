SUMMARY = "ScootUI (Qt)"
DESCRIPTION = "ScootUI instrument cluster UI - Qt/QML port"
AUTHOR = "Librescoot Contributors"
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

PV = "1.0.0+git${SRCPV}"
PE = "1"

inherit cmake qt6-cmake pkgconfig systemd

# qmltc writes the C++ it generates under .qmltc/ in the build tree, and that
# code carries absolute build paths. The debug-source package ships those files
# verbatim, which trips the buildpaths QA check. Only -src is affected; nothing
# with an absolute path reaches the target image.
INSANE_SKIP:${PN}-src += "buildpaths"

DEPENDS = " \
    hiredis \
    qtbase \
    qtdeclarative \
    qtdeclarative-native \
    qtshadertools \
    qtsvg \
    qttools \
    qmaplibre \
    zlib \
    zstd \
"

RDEPENDS:${PN} = " \
    hiredis \
    qtbase \
    qtbase-plugins \
    qtdeclarative \
    qtdeclarative-qmlplugins \
    qtsvg \
    qtsvg-plugins \
    qtlocation \
    qmaplibre \
"

EXTRA_OECMAKE = "-DCMAKE_BUILD_TYPE=Release"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${B}/bin/scootui ${D}${bindir}/scootui-qt

    # SDF glyphs for the map's street name labels. The styles reference these
    # as file:// so MapLibre reads them straight off disk; without them the
    # dashboard drops its symbol layers and the map draws no names at all.
    install -d ${D}${datadir}/scootui/glyphs/roboto_regular
    install -m 0644 ${S}/assets/glyphs/roboto_regular/*.pbf \
        ${D}${datadir}/scootui/glyphs/roboto_regular/
}

SYSTEMD_SERVICE:${PN} = "scootui-qt.service"
SYSTEMD_AUTO_ENABLE:${PN} = "disable"

do_install:append:unu-dbc() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/scootui-qt.service ${D}${systemd_system_unitdir}/
    install -d ${D}${sysconfdir}
    install -m 0644 ${UNPACKDIR}/scootui-qt-kms.json ${D}${sysconfdir}/
}

do_install:append:librescoot-dbc-rpi4() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/scootui-qt-rpi4.service ${D}${systemd_system_unitdir}/scootui-qt.service
}

FILES:${PN} = " \
    ${bindir}/scootui-qt \
    ${datadir}/scootui/glyphs \
    ${sysconfdir}/scootui-qt-kms.json \
    ${systemd_system_unitdir}/scootui-qt.service \
"
