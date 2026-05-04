DESCRIPTION = "Librescoot shell configuration and aliases"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " \
    file://librescoot-aliases.sh \
    file://librescoot-aliases-dbc.sh \
"

S = "${WORKDIR}"

FILES:${PN} = " \
    /etc/profile.d/librescoot-aliases.sh \
"

do_install () {
    install -d ${D}${sysconfdir}/profile.d
}

do_install:append:unu-mdb () {
    # Install MDB-specific aliases
    install -m 0755 ${WORKDIR}/librescoot-aliases.sh ${D}${sysconfdir}/profile.d/
}

do_install:append:unu-dbc () {
    # Install DBC-specific aliases (with -h mdb)
    install -m 0755 ${WORKDIR}/librescoot-aliases-dbc.sh ${D}${sysconfdir}/profile.d/librescoot-aliases.sh
}

do_install:append:librescoot-dbc-rpi4 () {
    # Install DBC-specific aliases for RPi4
    install -m 0755 ${WORKDIR}/librescoot-aliases-dbc.sh ${D}${sysconfdir}/profile.d/librescoot-aliases.sh
}

RDEPENDS:${PN} += "redis"
