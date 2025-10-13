DESCRIPTION = "udev rules for unu-mdb modem"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COREBASE}/LICENSE;md5=4d92cd373abda3937c2bc47fbc49d690"

SRC_URI = " file://99-mdb-modem.rules"

S = "${WORKDIR}"

FILES:${PN} = "/etc/udev/rules.d/99-mdb-modem.rules"

do_install () {
	install -d ${D}${sysconfdir}/udev/rules.d
	install -m 0644 ${WORKDIR}/99-mdb-modem.rules ${D}${sysconfdir}/udev/rules.d/
}
