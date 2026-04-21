FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"
PACKAGECONFIG:append = " modemmanager ppp"

SRC_URI += "file://20-modemmanager-ordering.conf"

do_install:append() {
    install -d ${D}${systemd_unitdir}/system/NetworkManager.service.d
    install -m 0644 ${WORKDIR}/20-modemmanager-ordering.conf \
        ${D}${systemd_unitdir}/system/NetworkManager.service.d/20-modemmanager-ordering.conf
}

FILES:${PN} += "${systemd_unitdir}/system/NetworkManager.service.d/20-modemmanager-ordering.conf"
