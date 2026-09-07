FILESEXTRAPATHS:prepend := "${THISDIR}/qtmultimedia:"

SRC_URI += "file://0001-alsa-let-first-write-start-playback.patch"

PACKAGECONFIG = "alsa"
