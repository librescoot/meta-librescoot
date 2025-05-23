SUMMARY = "Open Source Routing Engine for OpenStreetMap"
DESCRIPTION = "Valhalla is an open source routing engine and accompanying libraries for use with OpenStreetMap data."
HOMEPAGE = "https://github.com/valhalla/valhalla"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://COPYING;md5=a6502a7ddd34cba9a24a5a8ef3264e51"

SRC_URI = "gitsm://github.com/valhalla/valhalla.git;protocol=https;branch=master"
SRC_URI += "file://0001.patch"
SRC_URI += "file://0002.patch"

SRCREV = "3.5.1"

S = "${WORKDIR}/git"

DEPENDS = " \
    cmake-native \
    pkgconfig-native \
    boost \
    protobuf \
    protobuf-c \
    protobuf-native \
    sqlite3 \
    curl \
    zlib \
    luajit \
    libspatialite \
    czmq \
    lz4 \
"

# Optional dependencies
PACKAGECONFIG ??= "tools data-tools http python services benchmarks tests"
PACKAGECONFIG[tools] = "-DENABLE_TOOLS=ON,-DENABLE_TOOLS=OFF"
PACKAGECONFIG[data-tools] = "-DENABLE_DATA_TOOLS=ON,-DENABLE_DATA_TOOLS=OFF"
PACKAGECONFIG[http] = "-DENABLE_HTTP=ON,-DENABLE_HTTP=OFF"
PACKAGECONFIG[python] = "-DENABLE_PYTHON_BINDINGS=ON,-DENABLE_PYTHON_BINDINGS=OFF,python3"
PACKAGECONFIG[services] = "-DENABLE_SERVICES=ON,-DENABLE_SERVICES=OFF,prime-server"
PACKAGECONFIG[benchmarks] = "-DENABLE_BENCHMARKS=ON,-DENABLE_BENCHMARKS=OFF"
PACKAGECONFIG[tests] = "-DENABLE_TESTS=ON,-DENABLE_TESTS=OFF"
PACKAGECONFIG[ccache] = "-DENABLE_CCACHE=ON,-DENABLE_CCACHE=OFF,ccache"
PACKAGECONFIG[gdal] = "-DENABLE_GDAL=ON,-DENABLE_GDAL=OFF,gdal"

inherit cmake pkgconfig

EXTRA_OECMAKE = " \
    -DCMAKE_BUILD_TYPE=Release \
    -DENABLE_COMPILER_WARNINGS=OFF \
    -DENABLE_SINGLE_FILES_WERROR=OFF \
    -DENABLE_WERROR=OFF \
    -DENABLE_THREAD_SAFE_TILE_REF_COUNT=OFF \
    -DBUILD_SHARED_LIBS=ON \
    -DPREFER_EXTERNAL_DEPS=ON \
    -DProtobuf_PROTOC_EXECUTABLE=${STAGING_BINDIR_NATIVE}/protoc \
"

# Package files
FILES:${PN} = " \
    ${bindir}/valhalla_* \
    ${libdir}/lib*.so.* \
"

FILES:${PN}-dev = " \
    ${includedir} \
    ${libdir}/lib*.so \
    ${libdir}/pkgconfig \
    ${libdir}/cmake \
"

FILES:${PN}-staticdev = " \
    ${libdir}/lib*.a \
"

# Python bindings if enabled
FILES:${PN}-python = " \
    ${PYTHON_SITEPACKAGES_DIR}/valhalla \
"

PACKAGES += "${PN}-python"

RDEPENDS:${PN} = " \
    libspatialite \
    sqlite3 \
"

RDEPENDS:${PN}-python = " \
    ${PN} \
    python3 \
    python3-shapely \
    python3-requests \
"

# Ensure C++17 support
CXXFLAGS:append = " -std=c++17"

BBCLASSEXTEND = "native nativesdk"
