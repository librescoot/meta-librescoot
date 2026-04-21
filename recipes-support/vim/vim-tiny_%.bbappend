FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += "file://defaults.vim"

do_install:append() {
    install -D -m 0644 ${WORKDIR}/defaults.vim ${D}${datadir}/vim/${VIMDIR}/defaults.vim
}
