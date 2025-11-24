FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += "file://librescoot-splash.png"

do_install:append() {
    # Install the custom splash image as the psplash image
    install -d ${D}${datadir}/pixmaps
    install -m 0644 ${WORKDIR}/librescoot-splash.png ${D}${datadir}/pixmaps/psplash-${MACHINE}.png
}

FILES:${PN} += "${datadir}/pixmaps/psplash-${MACHINE}.png"
