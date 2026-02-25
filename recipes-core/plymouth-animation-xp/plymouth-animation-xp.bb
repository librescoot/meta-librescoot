SUMMARY = "WindowsXP easter egg Plymouth animation frames"
LICENSE = "CLOSED"

SRC_URI = "file://frames/"

do_install() {
    install -d ${D}${datadir}/plymouth/themes/windowsxp/frames
    install -m 0644 ${WORKDIR}/frames/* ${D}${datadir}/plymouth/themes/windowsxp/frames/
}

FILES:${PN} = "${datadir}/plymouth/themes/windowsxp \
              ${datadir}/plymouth/themes/windowsxp/frames \
              ${datadir}/plymouth/themes/windowsxp/frames/*"

RDEPENDS:${PN} = "plymouth"
