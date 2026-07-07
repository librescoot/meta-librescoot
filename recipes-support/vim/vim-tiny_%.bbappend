FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

# These targets drop vim-common for size and run vim-tiny instead, which then
# has no defaults.vim and throws E1187 at startup. Ship a stripped-down
# defaults.vim on them. (Only safe where vim-common is absent, otherwise it
# conflicts on /usr/share/vim/vimNN/defaults.vim.)
SRC_URI:append:unu-mdb = " file://defaults.vim"
SRC_URI:append:unu-dbc = " file://defaults.vim"
SRC_URI:append:librescoot-dbc-rpi4 = " file://defaults.vim"

FILES:${PN}:append:unu-mdb = " ${datadir}/vim"
FILES:${PN}:append:unu-dbc = " ${datadir}/vim"
FILES:${PN}:append:librescoot-dbc-rpi4 = " ${datadir}/vim"

do_install:append:unu-mdb() {
    install -D -m 0644 ${UNPACKDIR}/defaults.vim ${D}${datadir}/vim/${VIMDIR}/defaults.vim
}

do_install:append:unu-dbc() {
    install -D -m 0644 ${UNPACKDIR}/defaults.vim ${D}${datadir}/vim/${VIMDIR}/defaults.vim
}

do_install:append:librescoot-dbc-rpi4() {
    install -D -m 0644 ${UNPACKDIR}/defaults.vim ${D}${datadir}/vim/${VIMDIR}/defaults.vim
}
