FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

SRC_URI:append = " \
    file://librescoot-dbc.dts \
    file://logo/logo_linux_clut224.ppm \
"

SRC_URI:append:unu-mdb = " \
    file://librescoot-mdb.dts \
    file://mdb/0001-arm-imx-add-EPIT-timer-API-and-SDMA-engine-extension.patch \
    file://mdb/0002-leds-lp5562-initialize-LED-to-amber-at-boot.patch \
    file://mdb/0003-usb-cdc-wdm-demote-EPIPE-messages-to-debug-level.patch \
    file://mdb/0004-nfc-add-pn5xx-NFC-driver-for-PN544-PN547-PN548.patch \
    file://mdb/0005-nfc-pn5xx-track-supply-state-to-balance-regulator-e.patch \
    file://mdb/config-epit.cfg \
    file://mdb/config-sdma.cfg \
    file://mdb/config-nfc.cfg \
    file://mdb/config-leds.cfg \
    file://mdb/config-wireguard.cfg \
    file://mdb/config-can.cfg \
    file://mdb/config-modem.cfg \
    file://mdb/config-bt-wifi.cfg \
    file://mdb/config-netfilter.cfg \
    file://mdb/config-bmx055.cfg \
    file://mdb/config-ramoops.cfg \
    file://mdb/config-cmdline.cfg \
    file://mdb/config-headless.cfg \
    file://config-iotop.cfg \
    file://config-ppp.cfg \
    file://config-panic.cfg \
    file://config-namespaces.cfg \
"

KERNEL_CONFIG_FRAGMENTS:append:unu-mdb = " \
    ${UNPACKDIR}/mdb/config-epit.cfg \
    ${UNPACKDIR}/mdb/config-sdma.cfg \
    ${UNPACKDIR}/mdb/config-nfc.cfg \
    ${UNPACKDIR}/mdb/config-leds.cfg \
    ${UNPACKDIR}/mdb/config-wireguard.cfg \
    ${UNPACKDIR}/mdb/config-can.cfg \
    ${UNPACKDIR}/mdb/config-modem.cfg \
    ${UNPACKDIR}/mdb/config-bt-wifi.cfg \
    ${UNPACKDIR}/mdb/config-netfilter.cfg \
    ${UNPACKDIR}/mdb/config-bmx055.cfg \
    ${UNPACKDIR}/mdb/config-ramoops.cfg \
    ${UNPACKDIR}/mdb/config-cmdline.cfg \
    ${UNPACKDIR}/mdb/config-headless.cfg \
    ${UNPACKDIR}/config-iotop.cfg \
    ${UNPACKDIR}/config-ppp.cfg \
    ${UNPACKDIR}/config-panic.cfg \
    ${UNPACKDIR}/config-namespaces.cfg \
"

SRC_URI:append:unu-dbc = " \
    file://0001-fbcon-show-boot-logo-regardless-of-loglevel.patch \
    file://0002-arm-apply-cortex-a9-mp-errata-on-imx6dl.patch \
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
