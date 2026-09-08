SUMMARY = "Boot assets packaged into the rootfs for OTA boot updates"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

COMPATIBLE_MACHINE = "(unu-mdb|unu-dbc)"

DEPENDS = "virtual/kernel u-boot-imx"

do_install[depends] += "virtual/kernel:do_deploy u-boot-imx:do_deploy"

do_install() {
    install -d ${D}${datadir}/boot-assets

    # MDB boots its kernel and DTB from /boot; its updater only consumes U-Boot here.
    ASSETS="u-boot-dtb.imx"
    if [ "${MACHINE}" != "unu-mdb" ]; then
        DTB_NAME=$(basename ${KERNEL_DEVICETREE})
        ASSETS="zImage ${DTB_NAME} ${ASSETS}"
    fi

    for asset in ${ASSETS}; do
        install -m 0644 ${DEPLOY_DIR_IMAGE}/${asset} ${D}${datadir}/boot-assets/${asset}
    done

    cd ${D}${datadir}/boot-assets
    sha256sum ${ASSETS} > manifest.sha256
    sha256sum manifest.sha256 | awk '{print $1}' > version
}

FILES:${PN} = "${datadir}/boot-assets"
ALLOW_EMPTY:${PN}-dev = "0"
PACKAGES = "${PN}"
PACKAGE_ARCH = "${MACHINE_ARCH}"
INSANE_SKIP:${PN} += "already-stripped"
