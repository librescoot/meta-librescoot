FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

SRC_URI:append = " \
    file://Add-lcdmode.patch \
    file://librescoot-dbc.dts \
    file://logo/logo_linux_clut224.ppm \
"

# Override the default KBUILD_DEFCONFIG for librescoot-dbc machine
#KBUILD_DEFCONFIG:librescoot-dbc = ""

KERNEL_CONFIG_FRAGMENTS:append:librescoot-dbc = " ${WORKDIR}/opt3001.cfg ${WORKDIR}/boot-logo.cfg"

do_configure:prepend() {
    # Only apply these configurations for the DBC
    if [ "${MACHINE}" = "librescoot-dbc" ]; then
        # Install the logo file to the correct location in the Linux source tree
        if [ -e ${WORKDIR}/logo/logo_linux_clut224.ppm ]; then
            install -d ${S}/drivers/video/logo
            install -m 0644 ${WORKDIR}/logo/logo_linux_clut224.ppm ${S}/drivers/video/logo/
        fi

        # TEMPORARY: Disable kernel logo to test Plymouth
        # REVERT AFTER TESTING: Change CONFIG_LOGO back to y and CONFIG_LOGO_LINUX_CLUT224 back to y
        cat > ${WORKDIR}/boot-logo.cfg << EOF
CONFIG_LOGO=n
CONFIG_LOGO_LINUX_MONO=n
CONFIG_LOGO_LINUX_VGA16=n
CONFIG_LOGO_LINUX_CLUT224=n
# Original settings:
# CONFIG_LOGO=y
# CONFIG_LOGO_LINUX_MONO=n
# CONFIG_LOGO_LINUX_VGA16=n
# CONFIG_LOGO_LINUX_CLUT224=y
EOF

        # Create config fragment for OPT3001 light sensor module
        cat > ${WORKDIR}/opt3001.cfg << EOF
CONFIG_OPT3001=m
EOF
    fi
}

do_compile:prepend() {
    DTS=`basename ${KERNEL_DEVICETREE} .dtb`
    cp ${WORKDIR}/${DTS}.dts ${S}/arch/arm/boot/dts/
}
