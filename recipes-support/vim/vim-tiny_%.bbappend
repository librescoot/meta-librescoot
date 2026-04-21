FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

# DBC drops vim-common for size, so vim-tiny has no defaults.vim and throws
# E1187 at startup. Ship a stripped-down defaults.vim only on DBC — MDB
# keeps vim-common and would otherwise conflict on /usr/share/vim/vim91/defaults.vim.
SRC_URI:append:unu-dbc = " file://defaults.vim"
SRC_URI:append:librescoot-dbc-rpi4 = " file://defaults.vim"

FILES:${PN}:append:unu-dbc = " ${datadir}/vim"
FILES:${PN}:append:librescoot-dbc-rpi4 = " ${datadir}/vim"

do_install:append:unu-dbc() {
    install -D -m 0644 ${WORKDIR}/defaults.vim ${D}${datadir}/vim/${VIMDIR}/defaults.vim
}

do_install:append:librescoot-dbc-rpi4() {
    install -D -m 0644 ${WORKDIR}/defaults.vim ${D}${datadir}/vim/${VIMDIR}/defaults.vim
}
