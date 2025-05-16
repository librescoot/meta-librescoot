DESCRIPTION = "Heavily simplified version of original nrfutil. Only flash and restart over usb can be expected to work."
HOMEPAGE = "https://github.com/NordicSemiconductor/pc-nrfutil"

LICENSE = "Proprietary"
LIC_FILES_CHKSUM = "file://${S}/../git/LICENSE;md5=1e9df9ce515a549de0523956ebef8304"

SRC_URI = "git://github.com/librescoot/pc-nrfutil;protocol=https;branch=master"
SRCREV = "8646ab62313b7e8127d6fd6151016f22f8ecdc10"

inherit python3-dir

DEPENDS = " \
        python3-wheel \
        swig-native \
        systemd \
        "

RDEPENDS:${PN} += " \
        libudev \
        udev \
        "
do_install() {
    install -d ${D}${libdir}/${PYTHON_DIR}/site-packages/pc_nrfutil

    install -m 644 ${S}/../git/nordicsemi/*.py ${D}${libdir}/${PYTHON_DIR}/site-packages/pc_nrfutil/
}

FILES:${PN} += "\
    ${libdir}/${PYTHON_DIR}/site-packages/* \
"

