SUMMARY = "Sync expected U-Boot env vars from rootfs config files"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

inherit systemd

SRC_URI = "file://uboot-env-sync.sh \
           file://uboot-env-sync.service \
           file://identity.conf"
SRC_URI:append:unu-dbc = " file://boot-animation.conf"

FILESEXTRAPATHS:prepend := "${THISDIR}/files/${MACHINE}:"

SYSTEMD_SERVICE:${PN} = "uboot-env-sync.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

RDEPENDS:${PN} = "u-boot-fw-utils"

do_install() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/uboot-env-sync.service ${D}${systemd_system_unitdir}

    install -d ${D}${libdir}/uboot-env-sync
    install -m 0755 ${WORKDIR}/uboot-env-sync.sh ${D}${libdir}/uboot-env-sync/uboot-env-sync.sh

    install -d ${D}${sysconfdir}/uboot-env.d
    install -m 0644 ${WORKDIR}/identity.conf ${D}${sysconfdir}/uboot-env.d/identity.conf
    if [ -f ${WORKDIR}/boot-animation.conf ]; then
        install -m 0644 ${WORKDIR}/boot-animation.conf ${D}${sysconfdir}/uboot-env.d/boot-animation.conf
    fi
}

PACKAGE_ARCH = "${MACHINE_ARCH}"
FILES:${PN} = "${libdir}/uboot-env-sync ${systemd_system_unitdir} ${sysconfdir}/uboot-env.d"
