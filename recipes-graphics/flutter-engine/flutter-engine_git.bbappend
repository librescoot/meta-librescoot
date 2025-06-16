FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

do_install:append() {
    install -d ${D}${libdir}
    
    # Create symlinks for both debug and release engines
    ln -sf ${FLUTTER_ENGINE_INSTALL_PREFIX}/release/lib/libflutter_engine.so ${D}${libdir}/libflutter_engine.so
    ln -sf ${FLUTTER_ENGINE_INSTALL_PREFIX}/debug/lib/libflutter_engine.so ${D}${libdir}/libflutter_engine_debug.so
    
    # Create runtime switching script
    install -d ${D}${bindir}
    cat > ${D}${bindir}/flutter-engine-switch << EOF
#!/bin/bash
# Flutter Engine Runtime Mode Switcher
# Usage: flutter-engine-switch [debug|release]

FLUTTER_ENGINE_PREFIX="${FLUTTER_ENGINE_INSTALL_PREFIX}"

case "\$1" in
    debug)
        export LD_LIBRARY_PATH="\$FLUTTER_ENGINE_PREFIX/debug/lib:\$LD_LIBRARY_PATH"
        echo "Switched to Flutter debug engine"
        ;;
    release)
        export LD_LIBRARY_PATH="\$FLUTTER_ENGINE_PREFIX/release/lib:\$LD_LIBRARY_PATH"
        echo "Switched to Flutter release engine"
        ;;
    *)
        echo "Usage: \$0 [debug|release]"
        echo "Current engine paths:"
        echo "  Debug:   \$FLUTTER_ENGINE_PREFIX/debug/lib"
        echo "  Release: \$FLUTTER_ENGINE_PREFIX/release/lib"
        exit 1
        ;;
esac
EOF
    chmod +x ${D}${bindir}/flutter-engine-switch
}

FILES:${PN} += "${libdir}/libflutter_engine.so ${libdir}/libflutter_engine_debug.so ${bindir}/flutter-engine-switch"

INSANE_SKIP:${PN} += "dev-so"
