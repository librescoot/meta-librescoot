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

SRC_URI:append:mender-uboot = " file://common/0002-Custom-Integration-of-Mender-boot-code-into-U-Boot.patch"
SRC_URI:append:mender-uboot = " file://common/0003-Custom-Disable-CONFIG_BOOTCOMMAND-and-enable-CONFIG_MENDER_.patch"
SRC_URI:append:mender-uboot = " file://common/0004-mender-malloc.patch"

SRC_URI:append = " file://common/0001-helloworld.patch"
SRC_URI:append = " file://common/0003-sanity-check.patch"
SRC_URI:append:unu-dbc = " file://dbc/mx6sabresd.bmp"

SRC_URI:append:unu-mdb = " file://mdb/0004-enable-usb-mass-storage.patch"
SRC_URI:append:unu-mdb = " file://mdb/0006-spi-remove.patch"
SRC_URI:append:unu-mdb = " file://mdb/0007-mass-storage.patch"
SRC_URI:append:unu-mdb = " file://mdb/0008-mmc-and-usb.patch"
SRC_URI:append:unu-mdb = " file://mdb/0009-add-kernel_addr_r-var.patch"

SRC_URI:append:unu-dbc = " file://dbc/0001-add-DBC-memory-configuration.patch"
SRC_URI:append:unu-dbc = " file://dbc/0001-add-kernel_addr_r.patch"
SRC_URI:append:unu-dbc = " file://dbc/0002-remove-LVDS-EPDC-and-ethernet.-add-SPI-LCD-init-and-.patch"
SRC_URI:append:unu-dbc = " file://dbc/0003-add-SPI-and-FIT-compat-to-configs.patch"
SRC_URI:append:unu-dbc = " file://dbc/0004-add-console-and-boot-animation-args.patch"

PR = "r1"
SRCREV = "b76bb1bf9fd21e21006d79552e28855ac43ad43c"


UBOOT_INITIAL_ENV = ""
UBOOT_BINARY:unu-dbc = "u-boot-dtb.imx"
UBOOT_BINARY:unu-mdb = "u-boot-dtb.imx"

inherit fsl-u-boot-localversion dtc-145

LOCALVERSION ?= "-${SRCBRANCH}"

PACKAGE_ARCH = "${MACHINE_ARCH}"
COMPATIBLE_MACHINE = "(mx6|mx7)"

FILES:${PN} += "/uboot/*"

do_compile:prepend() {
    if [ -f ${UNPACKDIR}/dbc/mx6sabresd.bmp ]; then
        cp ${UNPACKDIR}/dbc/mx6sabresd.bmp ${S}/tools/logos/freescale.bmp
    fi
    cp ${S}/include/fdt.h ${S}/lib/libfdt/
    cp ${S}/include/libfdt.h ${S}/lib/libfdt/
    cp ${S}/include/libfdt_env.h ${S}/lib/libfdt/
}

# u-boot's check-config.sh rejects these legacy header-defined CONFIGs as "ad-hoc".
# They are grandfathered the canonical way: by adding them to config_whitelist.txt.
do_configure:append() {
    for c in CONFIG_BOOTCOUNT_ALTBOOTCMD CONFIG_ENV_MMC_DEVICE_INDEX \
             CONFIG_ENV_MMC_EMMC_HW_PARTITION CONFIG_ENV_REDUNDANT; do
        grep -qx "$c" ${S}/scripts/config_whitelist.txt || echo "$c" >> ${S}/scripts/config_whitelist.txt
    done
    # check-config.sh runs under LC_ALL=C and feeds the whitelist to comm, which
    # requires it sorted in that same collation. Re-sort after appending.
    LC_ALL=C sort -u -o ${S}/scripts/config_whitelist.txt ${S}/scripts/config_whitelist.txt
}

# The librescoot DBC stores the U-Boot environment on eMMC device 2
# (MENDER_UBOOT_STORAGE_DEVICE in unu-dbc.conf). The stock mx6sabresd header
# defaults to device 1 (the NXP dev-board SD slot), which trips meta-mender's
# "CONFIG_SYS_MMC_ENV_DEV must equal MENDER_UBOOT_STORAGE_DEVICE" check.
do_configure:prepend:unu-dbc() {
    sed -i 's/\(CONFIG_SYS_MMC_ENV_DEV[[:space:]]\+\)1/\12/' ${S}/include/configs/mx6sabresd.h
}

# u-boot 2017.03 is 8 years old; gcc 15 promotes incompatible-pointer-types,
# int-conversion, implicit-function-declaration, etc. to hard DEFAULT errors
# (permerrors that -Wno-error alone can't downgrade); -fpermissive relaxes them.
export KCFLAGS = "-Wno-error -fpermissive"
