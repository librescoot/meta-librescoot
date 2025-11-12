DESCRIPTION = "LibreScoot shell configuration and aliases"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " \
    file://librescoot-aliases.sh \
    file://librescoot-aliases-dbc.sh \
    file://redis-cli-env-dbc.sh \
    file://hosts-mdb \
    file://hosts-dbc \
"

S = "${WORKDIR}"

FILES:${PN} = " \
    /etc/profile.d/librescoot-aliases.sh \
    /etc/profile.d/redis-cli-env.sh \
    /etc/hosts-extra \
"

do_install () {
    install -d ${D}${sysconfdir}/profile.d
    install -d ${D}${sysconfdir}
}

do_install:append:unu-mdb () {
    # Install MDB-specific aliases
    install -m 0755 ${WORKDIR}/librescoot-aliases.sh ${D}${sysconfdir}/profile.d/

    # Install hosts entry for DBC
    install -m 0644 ${WORKDIR}/hosts-mdb ${D}${sysconfdir}/hosts-extra
}

do_install:append:unu-dbc () {
    # Install DBC-specific aliases (with -h mdb)
    install -m 0755 ${WORKDIR}/librescoot-aliases-dbc.sh ${D}${sysconfdir}/profile.d/librescoot-aliases.sh

    # Set default redis-cli host
    install -m 0755 ${WORKDIR}/redis-cli-env-dbc.sh ${D}${sysconfdir}/profile.d/redis-cli-env.sh

    # Install hosts entry for MDB
    install -m 0644 ${WORKDIR}/hosts-dbc ${D}${sysconfdir}/hosts-extra
}

pkg_postinst:${PN} () {
    #!/bin/sh
    if [ -z "$D" ] && [ -f /etc/hosts-extra ]; then
        # Running on target, append to /etc/hosts if not already present
        if ! grep -qf /etc/hosts-extra /etc/hosts 2>/dev/null; then
            cat /etc/hosts-extra >> /etc/hosts
        fi
    fi
}

RDEPENDS:${PN} += "redis"
