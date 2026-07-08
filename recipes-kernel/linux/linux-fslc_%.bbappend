FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

SRC_URI:append = " \
    file://librescoot-dbc.dts \
    file://logo/logo_linux_clut224.ppm \
"

SRC_URI:append:unu-dbc = " \
    file://0001-fbcon-show-boot-logo-regardless-of-loglevel.patch \
    file://config-logo.cfg \
    file://config-opt3001.cfg \
    file://config-tas5720.cfg \
    file://config-video.cfg \
    file://config-cmdline.cfg \
    file://config-iotop.cfg \
    file://config-ppp.cfg \
    file://config-panic.cfg \
    file://config-namespaces.cfg \
"

# Override the default KBUILD_DEFCONFIG for librescoot-dbc machine
#KBUILD_DEFCONFIG:unu-dbc = ""

KERNEL_CONFIG_FRAGMENTS:append:unu-dbc = " \
    ${UNPACKDIR}/config-opt3001.cfg \
    ${UNPACKDIR}/config-logo.cfg \
    ${UNPACKDIR}/config-tas5720.cfg \
    ${UNPACKDIR}/config-video.cfg \
    ${UNPACKDIR}/config-cmdline.cfg \
    ${UNPACKDIR}/config-iotop.cfg \
    ${UNPACKDIR}/config-ppp.cfg \
    ${UNPACKDIR}/config-panic.cfg \
    ${UNPACKDIR}/config-namespaces.cfg \
"

do_configure:prepend:unu-dbc() {
        # Install the logo file to the correct location in the Linux source tree
        if [ -e ${UNPACKDIR}/logo/logo_linux_clut224.ppm ]; then
            install -d ${S}/drivers/video/logo
            install -m 0644 ${UNPACKDIR}/logo/logo_linux_clut224.ppm ${S}/drivers/video/logo/
        fi
}

do_compile:prepend() {
    # Kernel 6.5+ keeps ARM dts under arch/arm/boot/dts/<vendor>/ (the flat dir
    # is no longer a build target). Honour the subdir in KERNEL_DEVICETREE.
    DTS=`basename ${KERNEL_DEVICETREE} .dtb`
    DTSDIR=`dirname ${KERNEL_DEVICETREE}`
    install -d ${S}/arch/arm/boot/dts/${DTSDIR}
    cp ${UNPACKDIR}/${DTS}.dts ${S}/arch/arm/boot/dts/${DTSDIR}/
}
