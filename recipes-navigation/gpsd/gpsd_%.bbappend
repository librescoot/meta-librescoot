FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

SRC_URI += "file://gpsd.default file://gpsd.service.conf"

FILES:${PN} += "${systemd_system_unitdir}/gpsd.service.d"

do_install:append() {
    install -d ${D}${sysconfdir}
    install -d ${D}${sysconfdir}/default/

    install -m 0644 ${WORKDIR}/gpsd.default ${D}${sysconfdir}/default/gpsd.default

    # Drop-in to ensure gpsd starts after fake-hwclock seeds the system clock
    install -d ${D}${systemd_system_unitdir}/gpsd.service.d
    install -m 0644 ${WORKDIR}/gpsd.service.conf ${D}${systemd_system_unitdir}/gpsd.service.d/10-after-hwclock.conf
}
