SUMMARY = "Kernel module for IMX PWM LED driver"
DESCRIPTION = "This module provides a driver for PWM LED control on i.MX platforms"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://LICENSE;md5=b234ee4d69f5fce4486a80fdaf4a4263"

inherit module

SRC_URI = "git://github.com/librescoot/kernel-module-imx-pwm-led.git;protocol=https;branch=master \
           file://imx_pwm_led.conf"

SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

EXTRA_OEMAKE += "KERNELDIR=${STAGING_KERNEL_DIR}"

RPROVIDES:${PN} += "linux-imx-led"

do_compile() {
    oe_runmake
}

do_install:append() {
    install -d ${D}${includedir}/imx/linux
    install -m 0644 ${S}/pwm_led.h ${D}${includedir}/imx/linux
    install -d ${D}/etc/modprobe.d/
    install -m 0644 ${WORKDIR}/imx_pwm_led.conf ${D}/etc/modprobe.d/
}


FILES:${PN} += "${nonarch_base_libdir}/modules/${KERNEL_VERSION}/kernel/drivers/leds/imx-pwm-led.ko"
FILES:${PN} += "${sysconfdir}/modprobe.d/imx_pwm_led.conf"
