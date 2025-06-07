FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

SRC_URI:append = " \
    file://Add-lcdmode.patch \
    file://librescoot-dbc.dts \
    file://logo/logo_linux_clut224.ppm \
"

SRC_URI:append:librescoot-dbc = " \
    file://config-drm.cfg \
    file://config-opt3001.cfg \
    file://config-tas5720.cfg \
"

# Override the default KBUILD_DEFCONFIG for librescoot-dbc machine
#KBUILD_DEFCONFIG:librescoot-dbc = ""

KERNEL_CONFIG_FRAGMENTS:append:librescoot-dbc = " \
    ${WORKDIR}/config-opt3001.cfg \
    ${WORKDIR}/config-drm.cfg \
    ${WORKDIR}/config-tas5720.cfg \
"

do_compile:prepend() {
    DTS=`basename ${KERNEL_DEVICETREE} .dtb`
    cp ${WORKDIR}/${DTS}.dts ${S}/arch/arm/boot/dts/
}
