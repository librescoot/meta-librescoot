FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += " \
    file://librescoot.script \
    file://librescoot.plymouth \
    file://plymouth-start.service \
"

PACKAGECONFIG:append = " drm"
PLYMOUTH_THEME = "librescoot"

do_install:append() {
    install -d ${D}${datadir}/plymouth/themes/librescoot
    install -m 0644 ${WORKDIR}/librescoot.script ${D}${datadir}/plymouth/themes/librescoot/
    install -m 0644 ${WORKDIR}/librescoot.plymouth ${D}${datadir}/plymouth/themes/librescoot/

    # Configure Plymouth for optimal startup with DRM backend
    install -d ${D}${sysconfdir}/plymouth
    cat > ${D}${sysconfdir}/plymouth/plymouthd.conf <<EOF
[Daemon]
Theme=librescoot
ShowDelay=0
DeviceTimeout=5
IgnoreSerialConsoles=yes
EOF

    # Override plymouth-start.service to remove vconsole-setup and udev-trigger deps.
    # Plymouth's DRM renderer needs neither: card1 exists via devtmpfs at T+0.3s and
    # DRM rendering doesn't require VT font/keymap setup. This starts Plymouth ~3.5s
    # into boot instead of ~7s.
    install -d ${D}${sysconfdir}/systemd/system
    install -m 0644 ${WORKDIR}/plymouth-start.service \
        ${D}${sysconfdir}/systemd/system/plymouth-start.service
}

FILES:${PN} += " \
    ${sysconfdir}/plymouth/plymouthd.conf \
    ${sysconfdir}/systemd/system/plymouth-start.service \
"
