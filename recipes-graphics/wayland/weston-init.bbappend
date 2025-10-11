FILESEXTRAPATHS:prepend:unu-dbc := "${THISDIR}/${PN}/unu-dbc:"
FILESEXTRAPATHS:prepend:librescoot-dbc-rpi5 := "${THISDIR}/${PN}/librescoot-dbc-rpi5:"

SRC_URI:append:unu-dbc = " file://weston.ini file://librescoot-splash.png"
SRC_URI:append:librescoot-dbc-rpi5 = " file://weston.ini file://librescoot-splash.png"

do_install:append:unu-dbc() {
    install -d ${D}${datadir}/weston/backgrounds
    install -m 0644 ${WORKDIR}/librescoot-splash.png ${D}${datadir}/weston/backgrounds/librescoot-splash.png
}

do_install:append:librescoot-dbc-rpi5() {
    install -d ${D}${datadir}/weston/backgrounds
    install -m 0644 ${WORKDIR}/librescoot-splash.png ${D}${datadir}/weston/backgrounds/librescoot-splash.png
}

FILES:${PN}:append:unu-dbc = " ${datadir}/weston/backgrounds/librescoot-splash.png"
FILES:${PN}:append:librescoot-dbc-rpi5 = " ${datadir}/weston/backgrounds/librescoot-splash.png"
