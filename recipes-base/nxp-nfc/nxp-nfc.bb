# Copyright (C) 2016 NXP Semiconductors

DESCRIPTION = "Linux NFC stack for NCI based NXP NFC Controllers."
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://src/include/linux_nfc_api.h;md5=79206fdb1d51ebc2e7b92b621a303cc6"

SRC_URI = " \
    git://github.com/NXPNFCLinux/linux_libnfc-nci.git;branch=master;protocol=https \
    file://0001-hack-out-i2c-error-counter.patch \
    file://0002-custom-conf.patch \
"
SRCREV = "449538e5e106666e5263afeaddacc5836fc23d3f"

RDEPENDS:${PN} += "bash"

inherit autotools pkgconfig

S = "${WORKDIR}/git"

FILES:${PN} = "/usr/lib/libnfc_nci_linux-1.so.0"
FILES:${PN} += "/usr/lib/libnfc_nci_linux-1.so.0.0.0"
FILES:${PN} += "/etc/libnfc-nxp-init.conf"
FILES:${PN} += "/etc/libnfc-nxp-pn547.conf"
FILES:${PN} += "/etc/libnfc-nxp-pn548.conf"
FILES:${PN} += "/etc/libnfc-nci.conf"
FILES:${PN} += "/usr/sbin/nfcDemoApp"


do_install:append() {
    install -d ${D}/usr/sbin
    install -m 0755 ${WORKDIR}/package/usr/sbin/nfcDemoApp ${D}/usr/sbin/nfcDemoApp
}

INSANE_SKIP:${PN} += "already-stripped"
