# Qt's CMake metadata needs the plugin in the sysroot even when not shipped.
PACKAGE_PREPROCESS_FUNCS:append:class-target = " librescoot_prune_geoclue_plugin"
librescoot_prune_geoclue_plugin() {
    if [ "${DISTRO}" = "librescoot-dbc" ] && [ "${LIBRESCOOT_DBC_GEOCLUE_MODEM}" != "1" ]; then
        rm -f ${PKGD}${libdir}/plugins/position/libqtposition_geoclue2.so
    fi
}
