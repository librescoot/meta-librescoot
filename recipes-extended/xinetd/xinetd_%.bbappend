FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += "file://xinetd.sysconfig"

do_install:append() {
    # Stock xinetd.service has: EnvironmentFile=-/etc/sysconfig/xinetd
    # Ship the file (empty EXTRAOPTIONS) so systemd stops warning on every start.
    install -d ${D}${sysconfdir}/sysconfig
    install -m 0644 ${WORKDIR}/xinetd.sysconfig ${D}${sysconfdir}/sysconfig/xinetd
}
