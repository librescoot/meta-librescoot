FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

FLUTTER_SDK_TAG = "3.32.5"

SRC_URI:remove = "file://0004-resolve-unknown-warning-option.patch"

RDEPENDS:${PN} += "harfbuzz liberation-fonts"

do_install:append() {
    install -d ${D}${libdir}
    ln -sf ${FLUTTER_ENGINE_INSTALL_PREFIX}/release/lib/libflutter_engine.so ${D}${libdir}/libflutter_engine.so
}

FILES:${PN} += "${libdir}/libflutter_engine.so"

INSANE_SKIP:${PN} += "dev-so"
