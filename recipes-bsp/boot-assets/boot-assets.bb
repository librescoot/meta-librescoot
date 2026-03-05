SUMMARY = "Boot assets packaged into the rootfs for OTA boot updates"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

COMPATIBLE_MACHINE = "(unu-mdb|unu-dbc)"

DEPENDS = "virtual/kernel u-boot-imx"

do_install[depends] += "virtual/kernel:do_deploy u-boot-imx:do_deploy"

do_install() {
    install -d ${D}${datadir}/boot-assets

    install -m 0644 ${DEPLOY_DIR_IMAGE}/zImage ${D}${datadir}/boot-assets/zImage

    DTB_NAME=$(basename ${KERNEL_DEVICETREE})
    install -m 0644 ${DEPLOY_DIR_IMAGE}/${DTB_NAME} ${D}${datadir}/boot-assets/${DTB_NAME}

    install -m 0644 ${DEPLOY_DIR_IMAGE}/u-boot-dtb.imx ${D}${datadir}/boot-assets/u-boot-dtb.imx

    cd ${D}${datadir}/boot-assets
    sha256sum zImage ${DTB_NAME} u-boot-dtb.imx > manifest.sha256
    sha256sum manifest.sha256 | awk '{print $1}' > version
}

FILES:${PN} = "${datadir}/boot-assets"
ALLOW_EMPTY:${PN}-dev = "0"
PACKAGES = "${PN}"
PACKAGE_ARCH = "${MACHINE_ARCH}"
INSANE_SKIP:${PN} += "already-stripped"
