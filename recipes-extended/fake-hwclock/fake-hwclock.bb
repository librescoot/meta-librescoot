SUMMARY = "Save/restore system clock to/from a file across reboots"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-2.0-only;md5=801f80980d171dd6425610833a22dbe6"

inherit systemd

SRC_URI = " \
    file://fake-hwclock \
    file://fake-hwclock-load.service \
    file://fake-hwclock-save.service \
"

SYSTEMD_SERVICE:${PN} = "fake-hwclock-load.service fake-hwclock-save.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d ${D}${sbindir}
    install -m 0755 ${WORKDIR}/fake-hwclock ${D}${sbindir}/fake-hwclock

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/fake-hwclock-load.service ${D}${systemd_system_unitdir}/
    install -m 0644 ${WORKDIR}/fake-hwclock-save.service ${D}${systemd_system_unitdir}/
}

FILES:${PN} = " \
    ${sbindir}/fake-hwclock \
    ${systemd_system_unitdir}/fake-hwclock-load.service \
    ${systemd_system_unitdir}/fake-hwclock-save.service \
"
