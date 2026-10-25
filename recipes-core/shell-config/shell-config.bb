DESCRIPTION = "Librescoot shell configuration and aliases"
LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/CC-BY-NC-SA-4.0;md5=277261f4a7c6c099822f8f5cb0180da0"

SRC_URI = " \
    file://librescoot-aliases.sh \
    file://librescoot-aliases-dbc.sh \
    file://librescoot-path.sh \
    file://librescoot-bin.conf \
"

FILES:${PN} = " \
    ${sysconfdir}/profile.d/librescoot-aliases.sh \
    ${sysconfdir}/profile.d/librescoot-path.sh \
    ${libdir}/tmpfiles.d/librescoot-bin.conf \
"

do_install () {
    install -d ${D}${sysconfdir}/profile.d ${D}${libdir}/tmpfiles.d
    install -m 0644 ${UNPACKDIR}/librescoot-path.sh ${D}${sysconfdir}/profile.d/
    install -m 0644 ${UNPACKDIR}/librescoot-bin.conf ${D}${libdir}/tmpfiles.d/
}

do_install:append:unu-mdb () {
    # Install MDB-specific aliases
    install -m 0755 ${UNPACKDIR}/librescoot-aliases.sh ${D}${sysconfdir}/profile.d/
}

do_install:append:unu-dbc () {
    # Install DBC-specific aliases (with -h mdb)
    install -m 0755 ${UNPACKDIR}/librescoot-aliases-dbc.sh ${D}${sysconfdir}/profile.d/librescoot-aliases.sh
}

do_install:append:librescoot-dbc-rpi4 () {
    # Install DBC-specific aliases for RPi4
    install -m 0755 ${UNPACKDIR}/librescoot-aliases-dbc.sh ${D}${sysconfdir}/profile.d/librescoot-aliases.sh
}

RDEPENDS:${PN} += "valkey"
