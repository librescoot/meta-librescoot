FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

SRC_URI:append = " \
    file://Add-lcdmode.patch \
    file://0001-add-imx-tas5720-machine-driver.patch \
    file://librescoot-dbc.dts \
    file://logo/logo_linux_clut224.ppm \
"

SRC_URI:append:librescoot-dbc = " \
    file://config-logo.cfg \
    file://config-opt3001.cfg \
    file://config-tas5720.cfg \
"

# Override the default KBUILD_DEFCONFIG for librescoot-dbc machine
#KBUILD_DEFCONFIG:librescoot-dbc = ""

KERNEL_CONFIG_FRAGMENTS:append:librescoot-dbc = " \
    ${WORKDIR}/config-opt3001.cfg \
    ${WORKDIR}/config-logo.cfg \
    ${WORKDIR}/config-tas5720.cfg \
"

do_configure:prepend:librescoot-dbc() {
        # Install the logo file to the correct location in the Linux source tree
        if [ -e ${WORKDIR}/logo/logo_linux_clut224.ppm ]; then
            install -d ${S}/drivers/video/logo
            install -m 0644 ${WORKDIR}/logo/logo_linux_clut224.ppm ${S}/drivers/video/logo/
        fi
}

do_compile:prepend() {
    DTS=`basename ${KERNEL_DEVICETREE} .dtb`
    cp ${WORKDIR}/${DTS}.dts ${S}/arch/arm/boot/dts/
}
