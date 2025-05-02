FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

SRC_URI += "file://weston.ini \
            file://librescoot-splash.png \
           "

do_install:append() {
    # install -d ${D}${sysconfdir}/xdg/weston
    # install -m 0644 ${WORKDIR}/weston.ini ${D}${sysconfdir}/xdg/weston/weston.ini

    install -d ${D}${datadir}/weston/backgrounds
    install -m 0644 ${WORKDIR}/librescoot-splash.png ${D}${datadir}/weston/backgrounds/librescoot-splash.png
}

FILES:${PN} += "${datadir}/weston/backgrounds/librescoot-splash.png"
