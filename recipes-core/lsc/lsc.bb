SUMMARY = "LibreScoot Control CLI"
HOMEPAGE = "https://github.com/librescoot/lsc"
LICENSE = "CC-BY-NC-4.0"
LIC_FILES_CHKSUM = "file://src/lsc/LICENSE;md5=f5a53c7ab38ba3772e879f1407d3d412"

SRC_URI = "git://github.com/librescoot/lsc.git;protocol=https;branch=main"

SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

inherit go-mod

GO_IMPORT = "lsc"

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${B}/bin/linux_arm/lsc ${D}${bindir}/
}
