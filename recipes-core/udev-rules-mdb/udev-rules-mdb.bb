DESCRIPTION = "udev rules for unu-mdb modem"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " \
    file://99-mdb-modem.rules \
    file://10-mdb-wwan.link \
"

FILES:${PN} = " \
    /etc/udev/rules.d/99-mdb-modem.rules \
    /etc/systemd/network/10-mdb-wwan.link \
"

do_install () {
	install -d ${D}${sysconfdir}/udev/rules.d
	install -m 0644 ${UNPACKDIR}/99-mdb-modem.rules ${D}${sysconfdir}/udev/rules.d/
	install -d ${D}${sysconfdir}/systemd/network
	install -m 0644 ${UNPACKDIR}/10-mdb-wwan.link ${D}${sysconfdir}/systemd/network/
}
