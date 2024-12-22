LICENSE = "CLOSED"
LIC_FILES_CHKSUM = ""

SRC_URI = "file://10-usb0.network"

do_install() {
    install -d ${D}/etc/systemd/network
    install -m 0644 ${WORKDIR}/10-usb0.network ${D}/etc/systemd/network
}
