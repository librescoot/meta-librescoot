SUMMARY = "ThorVG vector graphics library"
DESCRIPTION = "Lightweight portable library for rendering SVG, Lottie, and other vector graphics"
HOMEPAGE = "https://github.com/thorvg/thorvg"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE;md5=e6742929a49c378feb5c09f93b86cdd6"

SRC_URI = "git://github.com/thorvg/thorvg.git;protocol=https;branch=main"
SRCREV = "ccd4690de6480ed964f237b0606c1af121ea104f"

S = "${WORKDIR}/git"

inherit meson pkgconfig

EXTRA_OEMESON = " \
    -Ddefault_library=static \
    -Dengines=sw \
    -Dloaders=lottie,png,webp,jpg \
    -Dbindings=capi \
    -Dthreads=false \
    -Dlog=false \
    -Dstatic=true \
    -Dextra='' \
    -Dtools='' \
    -Dtests=false \
    -Dsavers='' \
"

BBCLASSEXTEND = "native nativesdk"
