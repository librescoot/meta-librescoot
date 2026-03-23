PACKAGECONFIG:append:class-target = " linuxfb sql-sqlite"

FILESEXTRAPATHS:prepend := "${THISDIR}/qtbase:"

SRC_URI:append = " \
    file://0001-eglfs-kms-avoid-modeset-on-first-atomic-commit.patch \
"
