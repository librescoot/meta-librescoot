# Keep the plugins in the build sysroot; remove unused styles from target packages.
PACKAGE_PREPROCESS_FUNCS:append:class-target = " librescoot_prune_qt_styles"
librescoot_prune_qt_styles() {
    if [ "${MACHINE}" = "unu-dbc" ]; then
        for style in FluentWinUI3 Imagine Material Universal; do
            rm -rf ${PKGD}${libdir}/qml/QtQuick/Controls/$style
            rm -f ${PKGD}${libdir}/libQt6QuickControls2${style}*.so*
        done
    fi
}
