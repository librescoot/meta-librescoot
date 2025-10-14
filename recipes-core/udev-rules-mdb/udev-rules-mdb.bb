DESCRIPTION = "udev rules for unu-mdb modem"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=b97a012949927931feb7793eee5ed924"

SRC_URI = " file://99-mdb-modem.rules"

S = "${WORKDIR}"

FILES:${PN} = "/etc/udev/rules.d/99-mdb-modem.rules"

do_install () {
	install -d ${D}${sysconfdir}/udev/rules.d
	install -m 0644 ${WORKDIR}/99-mdb-modem.rules ${D}${sysconfdir}/udev/rules.d/
}
