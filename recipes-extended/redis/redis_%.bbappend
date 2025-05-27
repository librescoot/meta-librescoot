FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

SRC_URI += "file://redis.conf"
SRC_URI += " file://redis-sysctl.conf"
SRC_URI += " file://redis.service"

# For DBC machine, only package redis-cli
PACKAGES:${MACHINE} = "${@'${PN}-cli' if d.getVar('MACHINE') == 'librescoot-dbc' else '${PN} ${PN}-cli ${PN}-server'}"

do_install:append() {
    install -d ${D}${sysconfdir}
    install -d ${D}${sysconfdir}/sysctl.d/
    install -d ${D}${systemd_system_unitdir}

    install -m 0644 ${WORKDIR}/redis.conf ${D}${sysconfdir}/redis/redis.conf
    install -m 0644 ${WORKDIR}/redis-sysctl.conf ${D}${sysconfdir}/sysctl.d/redis-sysctl.conf
    install -m 0644 ${WORKDIR}/redis.service ${D}${systemd_system_unitdir}
}

# Remove server files for DBC machine
do_install:append:librescoot-dbc() {
    rm -f ${D}${bindir}/redis-server
    rm -f ${D}${bindir}/redis-sentinel
    rm -f ${D}${sysconfdir}/redis/redis.conf
    rm -f ${D}${sysconfdir}/sysctl.d/redis-sysctl.conf
    rm -f ${D}${systemd_system_unitdir}/redis.service
}
