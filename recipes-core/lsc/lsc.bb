SUMMARY = "LibreScoot Control CLI"
HOMEPAGE = "https://github.com/librescoot/lsc"
LICENSE = "CC-BY-NC-4.0"
LIC_FILES_CHKSUM = "file://src/lsc/LICENSE;md5=f5a53c7ab38ba3772e879f1407d3d412"

SRC_URI = "git://github.com/librescoot/lsc.git;protocol=https;branch=main \
           file://lsc-completion.sh \
"

# Pinned 2026-01-28: fix(ota): replace os.Exit with error returns
SRCREV = "1defbb90b966ddc83d0ba5b31486891a1ad65587"

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
