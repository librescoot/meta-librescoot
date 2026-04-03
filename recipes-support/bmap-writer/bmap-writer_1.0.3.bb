SUMMARY = "bmaptool alternative written in C++"
DESCRIPTION = "bmap-writer efficiently writes disk images to storage devices \
using block mapping (BMAP). Lightweight C++ alternative to bmaptool."
HOMEPAGE = "https://github.com/embetrix/bmap-writer"
SECTION = "console/utils"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=e49f4652534af377a713df3d9dec60cb"

SRC_URI = "git://github.com/embetrix/${BPN};branch=master;protocol=https"
SRCREV = "991e2c4264b843f61e502712f497103472a1b6e7"
S = "${WORKDIR}/git"

DEPENDS = "libtinyxml2 libarchive"
inherit cmake pkgconfig

FILES:${PN} = "${bindir}"

BBCLASSEXTEND = "native nativesdk"
