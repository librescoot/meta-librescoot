SUMMARY = "MapLibre Native Qt bindings for Qt6"
DESCRIPTION = "QMapLibre provides Qt6 bindings for MapLibre GL Native, \
enabling hardware-accelerated vector map rendering with OpenGL ES 2.0. \
Used by ScootUI for the map screen with offline MBTiles support."
HOMEPAGE = "https://github.com/maplibre/maplibre-native-qt"
LICENSE = "BSD-2-Clause"
LIC_FILES_CHKSUM = "file://LICENSE;md5=e3fc50a88d0a364313df4b21ef20c29e"

DEPENDS = " \
    qtbase \
    qtdeclarative \
    qtlocation \
    qtsvg \
    curl \
    icu \
    sqlite3 \
    zlib \
    libpng \
    jpeg \
"

# maplibre-native-qt fetches maplibre-native as a submodule during CMake configure.
# We need network access during configure, or pre-fetch. Using AUTOREV + submodules.
SRC_URI = "git://github.com/maplibre/maplibre-native-qt.git;protocol=https;branch=main"
SRCREV = "${AUTOREV}"

# Allow CMake to fetch dependencies during build (maplibre-native uses FetchContent)
do_configure[network] = "1"
do_compile[network] = "1"

S = "${WORKDIR}/git"

inherit cmake qt6-cmake pkgconfig

EXTRA_OECMAKE = " \
    -DMLN_QT_WITH_LOCATION=ON \
    -DMLN_QT_WITH_WIDGETS=OFF \
    -DCMAKE_BUILD_TYPE=Release \
    -DFETCHCONTENT_FULLY_DISCONNECTED=OFF \
"

# maplibre-native build can be memory-intensive
PARALLEL_MAKE = "-j ${@min(int(d.getVar('BB_NUMBER_THREADS')), 4)}"

# Ensure OpenGL ES 2.0 is available
DEPENDS += "virtual/egl virtual/libgles2"

FILES:${PN} += " \
    ${libdir}/lib*.so.* \
    ${libdir}/qt6/qml/* \
"

FILES:${PN}-dev += " \
    ${libdir}/lib*.so \
    ${libdir}/cmake/* \
    ${includedir}/* \
"

# maplibre-native contains pre-generated shaders; skip QA on these
INSANE_SKIP:${PN} += "already-stripped"
