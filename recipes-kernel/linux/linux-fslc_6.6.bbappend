FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

SRC_URI:append = " \
    file://librescoot-dbc.dts \
    file://logo/logo_linux_clut224.ppm \
"

SRC_URI:append:unu-dbc = " \
    file://0001-fbcon-show-boot-logo-regardless-of-loglevel.patch \
    file://dbc/0006-imx-drm-overlay-fbdev-alpha.patch \
    file://dbc/0007-fix-route-overlay-alpha-sysfs-writes-through-DRM-ato.patch \
    file://dbc/0008-fix-disable-overlay-plane-when-not-in-use-to-prevent.patch \
    file://config-logo.cfg \
    file://config-opt3001.cfg \
    file://config-tas5720.cfg \
    file://config-video.cfg \
    file://config-cmdline.cfg \
    file://config-iotop.cfg \
    file://config-ppp.cfg \
"

# Override the default KBUILD_DEFCONFIG for librescoot-dbc machine
#KBUILD_DEFCONFIG:unu-dbc = ""

KERNEL_CONFIG_FRAGMENTS:append:unu-dbc = " \
    ${WORKDIR}/config-opt3001.cfg \
    ${WORKDIR}/config-logo.cfg \
    ${WORKDIR}/config-tas5720.cfg \
    ${WORKDIR}/config-video.cfg \
    ${WORKDIR}/config-cmdline.cfg \
    ${WORKDIR}/config-iotop.cfg \
    ${WORKDIR}/config-ppp.cfg \
"

do_configure:prepend:unu-dbc() {
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
