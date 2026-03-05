FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += " \
    file://librescoot.script \
    file://librescoot.plymouth \
    file://windowsxp.script \
    file://windowsxp.plymouth \
    file://plymouth-start.service \
    file://plymouth-quit.timer \
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

    # Override plymouth-start.service to remove vconsole-setup and udev-trigger deps,
    # starting Plymouth at ~T+2.9s without waiting for udev settle.
    # ExecStartPre reads /etc/plymouth/theme-override at runtime; if set, overrides
    # the default theme (e.g. write "windowsxp" to activate the easter egg). Uses a
    # bind-mount into /run/ since the rootfs is read-only during early boot.
    install -d ${D}${sysconfdir}/systemd/system
    install -m 0644 ${WORKDIR}/plymouth-start.service \
        ${D}${sysconfdir}/systemd/system/plymouth-start.service

    # Timer to quit Plymouth after animation completes (~9s after start)
    install -m 0644 ${WORKDIR}/plymouth-quit.timer \
        ${D}${sysconfdir}/systemd/system/plymouth-quit.timer

    # Activate timer when plymouth-start runs
    install -d ${D}${sysconfdir}/systemd/system/plymouth-start.service.wants
    ln -sf ../plymouth-quit.timer \
        ${D}${sysconfdir}/systemd/system/plymouth-start.service.wants/plymouth-quit.timer

    # Remove upstream multi-user.target trigger for plymouth-quit.service
    rm -f ${D}${systemd_system_unitdir}/multi-user.target.wants/plymouth-quit.service
}

FILES:${PN} += " \
    ${sysconfdir}/plymouth/plymouthd.conf \
    ${sysconfdir}/systemd/system/plymouth-start.service \
    ${sysconfdir}/systemd/system/plymouth-quit.timer \
    ${sysconfdir}/systemd/system/plymouth-start.service.wants/plymouth-quit.timer \
    ${datadir}/plymouth/themes/windowsxp \
    ${datadir}/plymouth/themes/windowsxp/windowsxp.script \
    ${datadir}/plymouth/themes/windowsxp/windowsxp.plymouth \
"
