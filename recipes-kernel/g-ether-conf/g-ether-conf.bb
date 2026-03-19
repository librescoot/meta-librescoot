SUMMARY = "Stable MAC address for g_ether USB gadget"
DESCRIPTION = "Derives a deterministic MAC address from the device machine-id \
when loading the g_ether kernel module."
LICENSE = "CLOSED"

SRC_URI = "file://g_ether.conf"

do_install() {
    install -d ${D}${sysconfdir}/modprobe.d
    install -m 0644 ${WORKDIR}/g_ether.conf ${D}${sysconfdir}/modprobe.d/
}
