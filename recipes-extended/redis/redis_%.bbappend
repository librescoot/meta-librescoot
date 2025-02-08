FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

SRC_URI += "file://redis.conf"
SRC_URI += " file://redis-sysctl.conf"
SRC_URI += " file://redis.service"

do_install:append() {
    install -d ${D}${sysconfdir}
    install -d ${D}${sysconfdir}/sysctl.d/
    install -d ${D}${systemd_system_unitdir}

    install -m 0644 ${WORKDIR}/redis.conf ${D}${sysconfdir}/redis/redis.conf
    install -m 0644 ${WORKDIR}/redis-sysctl.conf ${D}${sysconfdir}/sysctl.d/redis-sysctl.conf
    install -m 0644 ${WORKDIR}/redis.service ${D}${systemd_system_unitdir}
}
