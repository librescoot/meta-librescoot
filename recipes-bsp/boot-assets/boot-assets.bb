SUMMARY = "Boot assets packaged into the rootfs for OTA boot updates"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

COMPATIBLE_MACHINE = "(unu-mdb|unu-dbc)"

DEPENDS = "u-boot-imx"

do_install[depends] += "u-boot-imx:do_deploy"

do_install() {
    install -d ${D}${datadir}/boot-assets

    # U-Boot only. This is the one boot component outside the A/B rootfs, so it
    # is the only thing a bundle has to carry. The kernel and DTB ship inside
    # the rootfs image, together with the modules that belong to them, and U-Boot
    # loads both from /boot in the slot it selected. A bundle carrying a kernel
    # could only ever pair that kernel with another image's modules.
    ASSETS="u-boot-dtb.imx"

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
