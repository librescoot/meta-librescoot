SUMMARY = "LibreScoot Control CLI"
HOMEPAGE = "https://github.com/librescoot/lsc"
LICENSE = "CC-BY-NC-4.0"
LIC_FILES_CHKSUM = "file://src/lsc/LICENSE;md5=fb5d051e53001fdff7fec0f368f47190"

SRC_URI = "git://github.com/librescoot/lsc.git;protocol=https;branch=main \
           file://lsc-completion.sh \
"

SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

inherit librescoot-go

GO_IMPORT = "lsc"

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

do_install:prepend:librescoot-dbc-rpi4() {
    mv ${B}/bin/linux_arm64 ${B}/bin/linux_arm || true
}

FILES:${PN} += "${sysconfdir}/profile.d/lsc-completion.sh"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${B}/bin/linux_arm/lsc ${D}${bindir}/

    install -d ${D}${sysconfdir}/profile.d
    install -m 0644 ${WORKDIR}/lsc-completion.sh ${D}${sysconfdir}/profile.d/lsc-completion.sh
}
