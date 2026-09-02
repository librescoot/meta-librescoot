SUMMARY = "Librescoot Control CLI"
HOMEPAGE = "https://github.com/librescoot/lsc"
LICENSE = "CC-BY-NC-4.0"
LIC_FILES_CHKSUM = "file://src/lsc/LICENSE;md5=fb5d051e53001fdff7fec0f368f47190"

SRC_URI = "git://github.com/librescoot/lsc.git;protocol=https;branch=main;destsuffix=${GO_SRCURI_DESTSUFFIX} \
           file://lsc-completion.sh \
"

SRCREV = "${AUTOREV}"


inherit librescoot-go systemd

GO_IMPORT = "lsc"
# The module is named librescoot/lsc, so import-path patterns built from
# GO_IMPORT match nothing and go install silently falls back to the root
# package. A relative pattern builds both main packages, lsc and cmd/lsd.
GO_INSTALL = "./..."

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

do_install:prepend:librescoot-dbc-rpi4() {
    mv ${B}/bin/linux_arm64 ${B}/bin/linux_arm || true
}

FILES:${PN} += "${sysconfdir}/profile.d/lsc-completion.sh"

# lsd, the web management daemon, ships in the same repo and only makes
# sense on the MDB: it binds the usb0 management address and talks to the
# local Valkey. The unit file comes from the repo's deploy/ directory.
FILES:${PN}:append:unu-mdb = " ${systemd_system_unitdir}/librescoot-lsd.service"
SYSTEMD_SERVICE:${PN}:unu-mdb = "librescoot-lsd.service"
SYSTEMD_AUTO_ENABLE:${PN}:unu-mdb = "enable"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${B}/bin/linux_arm/lsc ${D}${bindir}/

    install -d ${D}${sysconfdir}/profile.d
    install -m 0644 ${UNPACKDIR}/lsc-completion.sh ${D}${sysconfdir}/profile.d/lsc-completion.sh
}

do_install:append:unu-mdb() {
    install -m 0755 ${B}/bin/linux_arm/lsd ${D}${bindir}/
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${S}/src/lsc/deploy/librescoot-lsd.service ${D}${systemd_system_unitdir}/
}
