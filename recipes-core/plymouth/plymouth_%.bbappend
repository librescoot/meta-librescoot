FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += " \
    file://librescoot.script \
    file://librescoot.plymouth \
    file://windowsxp.script \
    file://windowsxp.plymouth \
    file://plymouth-start.service \
    file://plymouth-set-theme.sh \
    file://plymouth-quit-delay.conf \
"

PACKAGECONFIG:append = " drm"
PLYMOUTH_THEME = "librescoot"

do_install:append() {
    install -d ${D}${datadir}/plymouth/themes/librescoot
    install -m 0644 ${WORKDIR}/librescoot.script ${D}${datadir}/plymouth/themes/librescoot/
    install -m 0644 ${WORKDIR}/librescoot.plymouth ${D}${datadir}/plymouth/themes/librescoot/

    install -d ${D}${datadir}/plymouth/themes/windowsxp
    install -m 0644 ${WORKDIR}/windowsxp.script ${D}${datadir}/plymouth/themes/windowsxp/
    install -m 0644 ${WORKDIR}/windowsxp.plymouth ${D}${datadir}/plymouth/themes/windowsxp/

    # plymouthd.conf: symlink to /run so the theme script can write at early boot
    # (root may still be ro). plymouth-set-theme reads plymouth.theme= from
    # /proc/cmdline (set by U-Boot from ${plymouth_theme} env var, persists
    # across OTA) and writes the config to /run/plymouth/plymouthd.conf.
    install -d ${D}${sysconfdir}/plymouth
    ln -sf /run/plymouth/plymouthd.conf ${D}${sysconfdir}/plymouth/plymouthd.conf

    install -d ${D}${libexecdir}
    install -m 0755 ${WORKDIR}/plymouth-set-theme.sh ${D}${libexecdir}/plymouth-set-theme

    install -d ${D}${sysconfdir}/systemd/system
    install -m 0644 ${WORKDIR}/plymouth-start.service \
        ${D}${sysconfdir}/systemd/system/plymouth-start.service

    install -d ${D}${sysconfdir}/systemd/system/plymouth-quit.service.d
    install -m 0644 ${WORKDIR}/plymouth-quit-delay.conf \
        ${D}${sysconfdir}/systemd/system/plymouth-quit.service.d/delay.conf
}

FILES:${PN} += " \
    ${sysconfdir}/plymouth/plymouthd.conf \
    ${sysconfdir}/systemd/system/plymouth-start.service \
    ${sysconfdir}/systemd/system/plymouth-quit.service.d \
    ${sysconfdir}/systemd/system/plymouth-quit.service.d/delay.conf \
    ${libexecdir}/plymouth-set-theme \
    ${datadir}/plymouth/themes/windowsxp \
    ${datadir}/plymouth/themes/windowsxp/windowsxp.script \
    ${datadir}/plymouth/themes/windowsxp/windowsxp.plymouth \
"
