# Copyright (C) 2013-2016 Freescale Semiconductor
# Copyright 2017 NXP
# Copyright 2018 (C) O.S. Systems Software LTDA.

DESCRIPTION = "i.MX U-Boot suppporting i.MX reference boards."
require recipes-bsp/u-boot/u-boot.inc
require recipes-bsp/u-boot/u-boot-mender.inc

PROVIDES += "u-boot"
DEPENDS:append = " dtc-native"

LICENSE = "GPL-2.0-or-later"
LIC_FILES_CHKSUM = "file://Licenses/gpl-2.0.txt;md5=b234ee4d69f5fce4486a80fdaf4a4263"

SRCBRANCH = "imx_v2017.03_4.9.88_2.0.0_ga"
SRC_URI = "git://github.com/nxp-imx/uboot-imx.git;protocol=https;branch=${SRCBRANCH}"

SRC_URI:remove:mender-uboot = " file://0002-Integration-of-Mender-boot-code-into-U-Boot.patch"
SRC_URI:remove:mender-uboot = " file://0003-Disable-CONFIG_BOOTCOMMAND-and-enable-CONFIG_MENDER_.patch"

SRC_URI:append:mender-uboot = " file://0002-Custom-Integration-of-Mender-boot-code-into-U-Boot.patch"
SRC_URI:append:mender-uboot = " file://0003-Custom-Disable-CONFIG_BOOTCOMMAND-and-enable-CONFIG_MENDER_.patch"
SRC_URI:append:mender-uboot = " file://0004-mender-malloc.patch"

SRC_URI:append = " file://0001-helloworld.patch"
SRC_URI:append = " file://0003-sanity-check.patch"
SRC_URI:append = " file://0004-enable-usb-mass-storage.patch"
SRC_URI:append = " file://0006-spi-remove.patch"
SRC_URI:append = " file://0007-mass-storage.patch"
SRC_URI:append = " file://0008-mmc-and-usb.patch"
SRC_URI:append = " file://0009-add-kernel_addr_r-var.patch"

SRCREV = "b76bb1bf9fd21e21006d79552e28855ac43ad43c"

S = "${WORKDIR}/git"

UBOOT_INITIAL_ENV = ""
UBOOT_BINARY = "u-boot-dtb.imx"

inherit fsl-u-boot-localversion dtc-145

LOCALVERSION ?= "-${SRCBRANCH}"

PACKAGE_ARCH = "${MACHINE_ARCH}"
COMPATIBLE_MACHINE = "(mx6|mx7)"

FILES:${PN} += "/uboot/*"

do_compile:prepend() {
    cp ${S}/include/fdt.h ${S}/lib/libfdt/
    cp ${S}/include/libfdt.h ${S}/lib/libfdt/
    cp ${S}/include/libfdt_env.h ${S}/lib/libfdt/
}

#do_install:append:mender-uboot() {
#    install -d ${D}${sysconfdir}
#    install -d ${D}/uboot
#    
#    install -m 0644 ${WORKDIR}/fw_env.config ${D}/uboot/fw_env.config
#    ln -sf /uboot/fw_env.config ${D}${sysconfdir}/fw_env.config
#}
