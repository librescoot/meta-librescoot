FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"
PACKAGECONFIG:append = " ppp"
# Modem support only on the MDB — the DBC has no modem, and the modemmanager
# packageconfig RDEPENDS would drag ModemManager into any image that ships
# NetworkManager.
PACKAGECONFIG:append:librescoot-mdb = " modemmanager wwan"

SRC_URI += "file://20-modemmanager-ordering.conf"

do_install:append() {
    install -d ${D}${systemd_unitdir}/system/NetworkManager.service.d
    install -m 0644 ${UNPACKDIR}/20-modemmanager-ordering.conf \
        ${D}${systemd_unitdir}/system/NetworkManager.service.d/20-modemmanager-ordering.conf
}

FILES:${PN} += "${systemd_unitdir}/system/NetworkManager.service.d/20-modemmanager-ordering.conf"
