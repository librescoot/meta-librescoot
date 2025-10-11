PACKAGECONFIG:remove:pn-systemd = "timesyncd"

FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

SRC_URI += "file://journald.conf"

do_install:append() {
    install -d ${D}${sysconfdir}/systemd
    install -m 0644 ${WORKDIR}/journald.conf ${D}${sysconfdir}/systemd/journald.conf
}

