FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

SRC_URI:append = " \
    file://Add-lcdmode.patch \
    file://librescoot-dbc.dts \
    file://logo/logo_linux_clut224.ppm \
"

KERNEL_CONFIG_FRAGMENTS += "${WORKDIR}/boot-logo.cfg"

do_configure:prepend() {
    # Install the logo file to the correct location in the Linux source tree
    if [ -e ${WORKDIR}/logo/logo_linux_clut224.ppm ]; then
        install -d ${S}/drivers/video/logo
        install -m 0644 ${WORKDIR}/logo/logo_linux_clut224.ppm ${S}/drivers/video/logo/
    fi

    # Create a kernel config fragment to ensure logo is enabled
    cat > ${WORKDIR}/boot-logo.cfg << EOF
CONFIG_LOGO=y
CONFIG_LOGO_LINUX_MONO=n
CONFIG_LOGO_LINUX_VGA16=n
CONFIG_LOGO_LINUX_CLUT224=y
EOF
}

do_compile:prepend() {
    DTS=`basename ${KERNEL_DEVICETREE} .dtb`
    cp ${WORKDIR}/${DTS}.dts ${S}/arch/arm/boot/dts/
}
