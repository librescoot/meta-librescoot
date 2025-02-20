SUMMARY = "Rescoot Telemetry Client"
HOMEPAGE = "https://github.com/rescoot/unu-radio-gaga"
LICENSE = "AGPL-3.0-or-later"
LIC_FILES_CHKSUM = "file://src/unu-radio-gaga/LICENSE;md5=4ae09d45eac4aa08d013b5f2e01c67f6"

SRC_URI = "git://github.com/rescoot/unu-radio-gaga.git;protocol=https;branch=main"
SRC_URI += " file://radio-gaga.service"

SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

inherit go-mod systemd

GO_IMPORT = "unu-radio-gaga"

GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"

FILES:${PN} += "/usr/lib/systemd/system/radio-gaga.service"

SYSTEMD_SERVICE:${PN} = "radio-gaga.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d ${D}${bindir}
    install -d ${D}${systemd_system_unitdir}

    install -m 0755 ${B}/bin/linux_arm/radio-gaga ${D}${bindir}/
    install -m 0644 ${WORKDIR}/radio-gaga.service ${D}${systemd_system_unitdir}
}

