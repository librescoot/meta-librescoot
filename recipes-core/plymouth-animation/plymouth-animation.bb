SUMMARY = "LibreScoot boot animation frames"
LICENSE = "CLOSED"

SRC_URI = "file://frames/"

do_install() {
    install -d ${D}${datadir}/plymouth/themes/librescoot/frames
    install -m 0644 ${WORKDIR}/frames/* ${D}${datadir}/plymouth/themes/librescoot/frames/
}

FILES:${PN} = "${datadir}/plymouth/themes/librescoot \
              ${datadir}/plymouth/themes/librescoot/frames \
              ${datadir}/plymouth/themes/librescoot/frames/*"

RDEPENDS:${PN} = "plymouth"
