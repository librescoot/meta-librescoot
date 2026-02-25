FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += " \
    file://librescoot.script \
    file://librescoot.plymouth \
    file://windowsxp.script \
    file://windowsxp.plymouth \
    file://plymouth-start.service \
    file://plymouth-quit-delay.conf \
"

PLYMOUTH_THEME = "librescoot"

do_install:append() {
    install -d ${D}${datadir}/plymouth/themes/librescoot
    install -m 0644 ${WORKDIR}/librescoot.script ${D}${datadir}/plymouth/themes/librescoot/
    install -m 0644 ${WORKDIR}/librescoot.plymouth ${D}${datadir}/plymouth/themes/librescoot/

    install -d ${D}${datadir}/plymouth/themes/windowsxp
    install -m 0644 ${WORKDIR}/windowsxp.script ${D}${datadir}/plymouth/themes/windowsxp/
    install -m 0644 ${WORKDIR}/windowsxp.plymouth ${D}${datadir}/plymouth/themes/windowsxp/

    install -d ${D}${sysconfdir}/plymouth
    cat > ${D}${sysconfdir}/plymouth/plymouthd.conf <<EOF
[Daemon]
Theme=librescoot
ShowDelay=0
DeviceTimeout=5
IgnoreSerialConsoles=yes
EOF

    # Override plymouth-start.service to remove vconsole-setup and udev-trigger deps.
    # Plymouth uses the fbdev renderer (writes to /dev/fb0 via DRM fbdev emulation),
    # avoiding any DRM KMS mode set. Flutter/scootui is the sole DRM master and
    # performs the only mode initialization. This starts Plymouth ~2.9s into boot.
    #
    # ExecStartPre reads /etc/plymouth/theme-override at runtime; if set, overrides
    # the default theme (e.g. write "windowsxp" to activate the easter egg). Uses a
    # bind-mount into /run/ since the rootfs is read-only during early boot.
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
    ${datadir}/plymouth/themes/windowsxp \
    ${datadir}/plymouth/themes/windowsxp/windowsxp.script \
    ${datadir}/plymouth/themes/windowsxp/windowsxp.plymouth \
"
