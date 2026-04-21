FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += "file://defaults.vim"

# vim.inc's FILES patterns key off ${BPN} (vim-tiny here), but the binary
# reads runtime files from the compile-time $VIMRUNTIME (/usr/share/vim/...),
# so we install and ship under that path explicitly.
FILES:${PN} += "${datadir}/vim"

do_install:append() {
    install -D -m 0644 ${WORKDIR}/defaults.vim ${D}${datadir}/vim/${VIMDIR}/defaults.vim
}
