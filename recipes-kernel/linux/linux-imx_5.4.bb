# Copyright 2013-2016 (C) Freescale Semiconductor
# Copyright 2017-2020 (C) NXP
# Copyright 2018 (C) O.S. Systems Software LTDA.
# Released under the MIT license (see COPYING.MIT for the terms)
#
# SPDX-License-Identifier: MIT
#

SUMMARY = "Linux Kernel provided and supported by NXP"
DESCRIPTION = "Linux Kernel provided and supported by NXP with focus on \
i.MX Family Reference Boards. It includes support for many IPs such as GPU, VPU and IPU."

require recipes-kernel/linux/linux-imx.inc

LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=bbea815ee2795b2f4230826c0c6b8814"

DEPENDS += "lzop-native bc-native"

KERNEL_SRC ?= "git://github.com/librescoot/linux-imx.git;protocol=https"

SRCBRANCH = "imx_5.4.24_2.1.0_unu"
LOCALVERSION = "-2.1.0"
SRCREV = "8115d570b280199956a10d5b8ce40ad1c880aa0b"

SRC_URI = " ${KERNEL_SRC};branch=${SRCBRANCH} \
            file://0001-replace-solaris-style-flag.patch \
            file://0002-ata-ahci-fix-enum-constants-for-gcc-13.patch \
            file://0003-querystatus.patch \
          "

SRC_URI:append:librescoot-mdb = " file://mdb_defconfig"
SRC_URI:append:librescoot-mdb = " file://librescoot-mdb.dts"

SRC_URI:append:librescoot-dbc = " file://dbc_defconfig"
SRC_URI:append:librescoot-dbc = " file://librescoot-dbc.dts"

# PV is defined in the base in linux-imx.inc file and uses the LINUX_VERSION definition
# required by kernel-yocto.bbclass.
#
# LINUX_VERSION define should match to the kernel version referenced by SRC_URI and
# should be updated once patchlevel is merged.
LINUX_VERSION = "5.4.24"

DEFAULT_PREFERENCE = "1"

COMPATIBLE_MACHINE = "(mx6|mx7|mx8)"

do_compile:prepend() {
    DTS=`basename ${KERNEL_DEVICETREE} .dtb`
    cp ${WORKDIR}/${DTS}.dts ${S}/arch/${ARCH}/boot/dts/
}
