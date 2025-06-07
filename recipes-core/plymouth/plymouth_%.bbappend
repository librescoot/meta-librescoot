FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += " \
    file://librescoot.script \
    file://librescoot.plymouth \
"

PACKAGECONFIG:append = " drm udev"
PLYMOUTH_THEME = "librescoot"

do_install:append() {
    install -d ${D}${datadir}/plymouth/themes/librescoot
    install -m 0644 ${WORKDIR}/librescoot.script ${D}${datadir}/plymouth/themes/librescoot/
    install -m 0644 ${WORKDIR}/librescoot.plymouth ${D}${datadir}/plymouth/themes/librescoot/
    # Animation frames will be installed separately via plymouth-animation recipe
    
    # Set librescoot as default theme
    install -d ${D}${sysconfdir}/plymouth
    echo "[Daemon]" > ${D}${sysconfdir}/plymouth/plymouthd.conf
    echo "Theme=librescoot" >> ${D}${sysconfdir}/plymouth/plymouthd.conf
    echo "ShowDelay=0" >> ${D}${sysconfdir}/plymouth/plymouthd.conf
}

FILES:${PN} += "${sysconfdir}/plymouth/plymouthd.conf"
