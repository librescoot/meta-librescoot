FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"
SRC_URI += "file://redis.conf"

do_install:append() {
    install -d ${D}${sysconfdir}
    install -m 0644 ${WORKDIR}/redis.conf ${D}${sysconfdir}/redis/redis.conf
}
