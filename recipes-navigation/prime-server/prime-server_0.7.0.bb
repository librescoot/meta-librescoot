SUMMARY = "Non-blocking (web)server API for distributed computing and SOA based on zeromq"
DESCRIPTION = "Prime server is a library for building non-blocking servers based on zeromq."
HOMEPAGE = "https://github.com/kevinkreiser/prime_server"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://COPYING;md5=977e7d29c0769ad85e641aed8ade467f"

SRC_URI = "gitsm://github.com/kevinkreiser/prime_server.git;protocol=https;branch=master"
SRC_URI += "file://m4.patch"
SRCREV = "4508553b2dd29fadfafcc7c766aa6e9b94455fcb"

S = "${WORKDIR}/git"

DEPENDS = " \
    cmake-native \
    pkgconfig-native \
    czmq \
    zeromq \
    curl \
"

inherit cmake pkgconfig

# Use CMake instead of autotools to avoid version detection issues
EXTRA_OECMAKE = " \
    -DCMAKE_BUILD_TYPE=Release \
    -DBUILD_SHARED_LIBS=ON \
"

FILES:${PN} = " \
    ${bindir}/prime_* \
    ${libdir}/lib*.so.* \
"

FILES:${PN}-dev = " \
    ${includedir} \
    ${libdir}/lib*.so \
    ${libdir}/pkgconfig \
"

RDEPENDS:${PN} = " \
    czmq \
    zeromq \
    curl \
"

BBCLASSEXTEND = "native nativesdk"
