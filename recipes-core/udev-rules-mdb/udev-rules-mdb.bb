DESCRIPTION = "udev rules for unu-mdb modem"
LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/CC-BY-NC-SA-4.0;md5=277261f4a7c6c099822f8f5cb0180da0"

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
