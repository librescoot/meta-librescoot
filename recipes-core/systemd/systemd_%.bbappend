PACKAGECONFIG:remove:pn-systemd = "timesyncd"

FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

SRC_URI += "file://journald.conf"
SRC_URI += "file://00-create-volatile.conf"

do_install:append() {
    install -d ${D}${sysconfdir}/systemd
    install -m 0644 ${WORKDIR}/journald.conf ${D}${sysconfdir}/systemd/journald.conf

    # Override the default 00-create-volatile.conf to avoid duplicate /run/lock
    install -m 0644 ${WORKDIR}/00-create-volatile.conf ${D}${libdir}/tmpfiles.d/00-create-volatile.conf
}

