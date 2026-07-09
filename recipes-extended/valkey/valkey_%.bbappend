FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

# valkey.conf and valkey.service are already in the upstream SRC_URI; the
# copies in this directory take precedence via FILESEXTRAPATHS and carry the
# configuration ported from our former redis setup.
SRC_URI += "file://valkey-sysctl.conf"

do_install:append() {
    install -d ${D}${sysconfdir}/sysctl.d
    install -m 0644 ${UNPACKDIR}/valkey-sysctl.conf ${D}${sysconfdir}/sysctl.d/valkey-sysctl.conf

    # Shell aliases and tooling call redis-cli. valkey's make install already
    # creates the compat symlink (USE_REDIS_SYMLINKS defaults to yes); pin it
    # here so it survives an upstream default change.
    ln -sf valkey-cli ${D}${bindir}/redis-cli
}

# The DBC talks to the valkey instance on the MDB; it only needs the CLI.
SYSTEMD_AUTO_ENABLE:${PN}:unu-dbc = "disable"
SYSTEMD_AUTO_ENABLE:${PN}:librescoot-dbc-rpi4 = "disable"
