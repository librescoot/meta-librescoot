SUMMARY = "MapLibre Native Qt bindings for Qt6"
DESCRIPTION = "QMapLibre provides Qt6 bindings for MapLibre GL Native, \
enabling hardware-accelerated vector map rendering with OpenGL ES 2.0. \
Used by ScootUI for the map screen with offline MBTiles support."
HOMEPAGE = "https://github.com/maplibre/maplibre-native-qt"
LICENSE = "BSD-2-Clause"
LIC_FILES_CHKSUM = "file://LICENSES/BSD-2-Clause.txt;md5=272be00ca1ae12eceb040a3946c3c2cc"

DEPENDS = " \
    qtbase \
    qtdeclarative-native \
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

# maplibre-native-qt vendors maplibre-native as a git submodule under vendor/.
# Use gitsm:// to recursively fetch all submodules during do_fetch.
SRC_URI = "gitsm://github.com/maplibre/maplibre-native-qt.git;protocol=https;branch=main \
    file://0001-disable-tests.patch \
"

# Pin to 10c6d828: last commit before the "Drawables Renderer" switch to OpenGL ES 3.0+.
# This version vendors maplibre-native from the opengl-2 branch (b50faeb9a24e)
# which preserves OpenGL ES 2.0 support needed for the i.MX6 Vivante GC880 GPU.
SRCREV = "10c6d828bab661330cf9cb08d4d3bb9defb74582"

# Allow CMake to fetch dependencies during build (maplibre-native uses FetchContent)
do_configure[network] = "1"
do_compile[network] = "1"

S = "${WORKDIR}/git"

inherit cmake qt6-cmake pkgconfig

# Qt6 private headers trigger -Wshadow in constructors; suppress as error
CXXFLAGS:append = " -Wno-error=shadow"

EXTRA_OECMAKE = " \
    -DMLN_QT_WITH_LOCATION=ON \
    -DMLN_QT_WITH_WIDGETS=OFF \
    -DMLN_WITH_OPENGL=ON \
    -DBUILD_TESTING=OFF \
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
    ${libdir}/qt6/plugins/geoservices/* \
    ${prefix}/plugins/geoservices/* \
    ${prefix}/qml/MapLibre/* \
"

FILES:${PN}-dev += " \
    ${libdir}/lib*.so \
    ${libdir}/cmake/* \
    ${includedir}/* \
"

# maplibre-native contains pre-generated shaders; skip QA on these
INSANE_SKIP:${PN} += "already-stripped"
