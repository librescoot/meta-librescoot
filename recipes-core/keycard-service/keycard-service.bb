LICENSE = "CLOSED"
LIC_FILES_CHKSUM = ""

inherit systemd

SRC_URI = "file://keycard.sh"
SRC_URI += "file://ledcontrol.sh"
SRC_URI += "file://greenled.sh"
SRC_URI += "file://keycard.py"
SRC_URI += "file://PN7150.py"
SRC_URI += "file://librescoot-keycard.service"

SYSTEMD_SERVICE:${PN} = "librescoot-keycard.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

FILES:${PN} = "/opt/librescoot-keycard/*"
FILES:${PN} += "/usr/bin/ledcontrol.sh"
FILES:${PN} += "/usr/bin/greenled.sh"
FILES:${PN} += "/usr/bin/keycard.sh"

do_install() {
    install -d ${D}/opt/librescoot-keycard
    install -d ${D}/usr/bin
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/keycard.sh ${D}/usr/bin/
    install -m 0644 ${WORKDIR}/ledcontrol.sh ${D}/usr/bin/
    install -m 0644 ${WORKDIR}/greenled.sh ${D}/usr/bin/
    install -m 0644 ${WORKDIR}/PN7150.py ${D}/opt/librescoot-keycard
    install -m 0644 ${WORKDIR}/keycard.py ${D}/opt/librescoot-keycard
    install -m 0644 ${WORKDIR}/librescoot-keycard.service ${D}${systemd_system_unitdir}
}
