FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

do_install:append() {
    install -d ${D}${libdir}
    ln -sf ${FLUTTER_ENGINE_INSTALL_PREFIX}/release/lib/libflutter_engine.so ${D}${libdir}/libflutter_engine.so
}

FILES:${PN} += "${libdir}/libflutter_engine.so"

INSANE_SKIP:${PN} += "dev-so"
