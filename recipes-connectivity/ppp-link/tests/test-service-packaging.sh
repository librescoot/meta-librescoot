#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
RECIPE="$ROOT/ppp-link.bb"
UNIT="$ROOT/files/ppp-link.service"
SYSTEMD_FILES="$ROOT/../../recipes-core/systemd/systemd"

assert_line() {
    grep -Fqx "$1" "$2" || {
        printf 'missing expected line in %s: %s\n' "$2" "$1" >&2
        exit 1
    }
}

# The package must ship and enable this exact unit. MDB remains explicitly
# disabled at boot; DBC inherits the enabled default.
grep -Fq 'file://ppp-link.service' "$RECIPE"
assert_line 'inherit systemd' "$RECIPE"
assert_line 'SYSTEMD_SERVICE:${PN} = "ppp-link.service"' "$RECIPE"
assert_line 'SYSTEMD_AUTO_ENABLE:${PN} = "enable"' "$RECIPE"
assert_line 'SYSTEMD_AUTO_ENABLE:${PN}:unu-mdb = "disable"' "$RECIPE"
assert_line '    install -m 0644 ${UNPACKDIR}/ppp-link.service ${D}${systemd_system_unitdir}' "$RECIPE"

# On a cold boot, wait for the standard tmpfiles pass which owns /run/lock.
# The pre-start mkdir is a fallback and must target pppd's actual lock parent,
# not the unused /var/run/pppd/lock path.
assert_line 'After=local-fs.target systemd-tmpfiles-setup.service' "$UNIT"
assert_line 'ExecStartPre=/bin/mkdir -p /run/lock' "$UNIT"
assert_line 'ExecStart=/usr/sbin/pppd call uart-link nodetach' "$UNIT"
if grep -Fq '/var/run/pppd/lock' "$UNIT"; then
    echo 'ppp-link.service still creates the unused pppd lock directory' >&2
    exit 1
fi

# Librescoot deliberately leaves /run/lock to systemd's legacy tmpfiles rule
# instead of installing a duplicate layer-specific rule.
grep -Fq '/run/lock is already created by legacy.conf' \
    "$SYSTEMD_FILES/00-create-volatile.conf"
if grep -Eq '^[[:space:]]*[dD][+!]?[[:space:]]+/run/lock([[:space:]]|$)' \
    "$SYSTEMD_FILES/00-create-volatile.conf"; then
    echo 'duplicate Librescoot /run/lock tmpfiles rule found' >&2
    exit 1
fi
