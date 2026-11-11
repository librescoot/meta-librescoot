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
assert_line 'RDEPENDS:${PN} += "${PN}-route-monitor ${PN}-dbc-link"' "$RECIPE"
assert_line 'SYSTEMD_AUTO_ENABLE:${PN}:unu-mdb = "disable"' "$RECIPE"
assert_line '    install -m 0644 ${UNPACKDIR}/ppp-link.service ${D}${systemd_system_unitdir}' "$RECIPE"

# On a cold boot, wait for the standard tmpfiles pass which owns /run/lock.
# The pre-start mkdir is a fallback and must target pppd's actual lock parent,
# not the unused /var/run/pppd/lock path.
assert_line 'After=local-fs.target systemd-tmpfiles-setup.service' "$UNIT"
assert_line 'ExecStartPre=/bin/mkdir -p /run/lock' "$UNIT"
assert_line 'ExecStart=/usr/sbin/pppd call uart-link nodetach' "$UNIT"

# Intentional shutdown can preserve pppd's user-request or hangup status. Both
# must report cleanly, while any unexpected daemon exit must still be restarted.
assert_line 'SuccessExitStatus=5 16' "$UNIT"
assert_line 'Restart=always' "$UNIT"
if grep -Fqx 'Restart=on-failure' "$UNIT"; then
    echo 'ppp-link.service can suppress restart for accepted pppd exit codes' >&2
    exit 1
fi
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

# The DBC link unit owns usb0's admin state. It is never enabled at boot:
# vehicle-service starts and stops it, and the route monitor rides along so it
# only runs while the link is supposed to be up.
LINK_UNIT="$ROOT/files/librescoot-dbc-link.service"
MONITOR_UNIT="$ROOT/files/librescoot-link-route-monitor.service"
grep -Fq 'file://librescoot-dbc-link.service' "$RECIPE"
assert_line 'SYSTEMD_SERVICE:${PN}-dbc-link = "librescoot-dbc-link.service"' "$RECIPE"
assert_line 'SYSTEMD_AUTO_ENABLE:${PN}-dbc-link = "disable"' "$RECIPE"
assert_line '    install -m 0644 ${UNPACKDIR}/librescoot-dbc-link.service ${D}${systemd_system_unitdir}' "$RECIPE"
assert_line 'SYSTEMD_AUTO_ENABLE:${PN}-route-monitor:unu-mdb = "disable"' "$RECIPE"
assert_line 'Wants=librescoot-link-route-monitor.service' "$LINK_UNIT"
assert_line 'Type=oneshot' "$LINK_UNIT"
assert_line 'RemainAfterExit=yes' "$LINK_UNIT"
assert_line 'ExecStart=/usr/sbin/ip link set usb0 up' "$LINK_UNIT"
assert_line 'ExecStop=/usr/sbin/ip link set usb0 down' "$LINK_UNIT"
# The monitor follows the link unit, not ppp-link: an open gate keeps usb0 up
# while the DBC is off, and there the monitor still has a route to withdraw.
assert_line 'PartOf=librescoot-dbc-link.service' "$MONITOR_UNIT"
assert_line 'After=librescoot-netconfig.service librescoot-dbc-link.service' "$MONITOR_UNIT"
if grep -Fq 'PartOf=ppp-link.service' "$MONITOR_UNIT"; then
    echo 'the route monitor must not follow ppp-link.service' >&2
    exit 1
fi
