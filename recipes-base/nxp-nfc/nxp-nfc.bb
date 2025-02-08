# Copyright (C) 2016 NXP Semiconductors

DESCRIPTION = "Linux NFC stack for NCI based NXP NFC Controllers."
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://src/include/linux_nfc_api.h;md5=66a29263c6e002e28c53ef23a71debee"

SRC_URI = " \
    git://github.com/StarGate01/linux_libnfc-nci.git;branch=master;protocol=https \
"
SRCREV = "7ce9c8aad0e37850a49b6d8dcc22ae5c783268e7"

RDEPENDS:${PN}-bin += "bash"

inherit autotools pkgconfig lib_package

S = "${WORKDIR}/git"

FILES:${PN}-bin = "/usr/sbin/nfcDemoApp"


do_install:append() {
    install -d ${D}${sbindir}
    install -m 0755 ${WORKDIR}/package/usr/sbin/nfcDemoApp ${D}${sbindir}/nfcDemoApp
}

INSANE_SKIP:${PN} += "already-stripped"
