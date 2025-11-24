FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += " \
    file://librescoot.script \
    file://librescoot.plymouth \
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
}

FILES:${PN} += "${sysconfdir}/plymouth/plymouthd.conf"
