FILESEXTRAPATHS:prepend := "${THISDIR}/linux-imx:"

# Add custom boot logo patch and PPM file
SRC_URI += " \
    file://0004-custom-boot-logo.patch \
    file://logo/logo_custom_clut224.ppm \
"

# Enable boot logo in kernel config
KERNEL_CONFIG_FRAGMENTS += "${WORKDIR}/boot-logo.cfg"

do_configure:prepend() {
    # Install the logo file to the correct location in the Linux source tree
    if [ -e ${WORKDIR}/logo/logo_custom_clut224.ppm ]; then
        install -d ${S}/drivers/video/logo
        install -m 0644 ${WORKDIR}/logo/logo_custom_clut224.ppm ${S}/drivers/video/logo/
    fi

    # Create a kernel config fragment to ensure logo is enabled
    cat > ${WORKDIR}/boot-logo.cfg << EOF
CONFIG_LOGO=y
CONFIG_LOGO_CUSTOM_CLUT224=y
# Disable other logos
CONFIG_LOGO_LINUX_MONO=n
CONFIG_LOGO_LINUX_VGA16=n
CONFIG_LOGO_LINUX_CLUT224=n
EOF
}
