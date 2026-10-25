SUMMARY = "Stable MAC address for g_ether USB gadget"
DESCRIPTION = "Derives a deterministic MAC address from the device machine-id \
when loading the g_ether kernel module."
LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/CC-BY-NC-SA-4.0;md5=277261f4a7c6c099822f8f5cb0180da0"

SRC_URI = "file://g_ether.conf"

do_install() {
    install -d ${D}${sysconfdir}/modprobe.d
    install -m 0644 ${UNPACKDIR}/g_ether.conf ${D}${sysconfdir}/modprobe.d/
}
