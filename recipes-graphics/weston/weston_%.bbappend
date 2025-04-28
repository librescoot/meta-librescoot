FILESEXTRAPATHS_prepend := "${THISDIR}/weston:"

SRC_URI += "file://weston-plymouth.conf"

do_install_append() {
    install -d ${D}${systemd_system_unitdir}/weston.service.d
    install -m 0644 ${WORKDIR}/weston-plymouth.conf ${D}${systemd_system_unitdir}/weston.service.d/
}

FILES_${PN} += "${systemd_system_unitdir}/weston.service.d/weston-plymouth.conf"
