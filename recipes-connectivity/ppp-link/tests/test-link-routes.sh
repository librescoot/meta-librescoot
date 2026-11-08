#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/bin"

cat > "$TMP/bin/ip" <<'EOF'
#!/bin/sh
printf '%s\n' "$*" >> "$IP_LOG"
EOF
cat > "$TMP/bin/ping" <<'EOF'
#!/bin/sh
printf '%s\n' "$*" >> "$PING_LOG"
[ "$PING_RESULT" = success ]
EOF
chmod +x "$TMP/bin/ip" "$TMP/bin/ping"

# run_monitor PING_RESULT PEER_CONFIG CARRIER
run_monitor() {
    : > "$TMP/ip.log"
    : > "$TMP/ping.log"
    printf '%s\n' "$3" > "$TMP/carrier"
    PATH="$TMP/bin:$PATH" IP_LOG="$TMP/ip.log" PING_LOG="$TMP/ping.log" \
        PING_RESULT="$1" PEER_CONFIG="$2" CARRIER_PATH="$TMP/carrier" \
        LINK_ROUTE_ONCE=1 "$ROOT/files/librescoot-link-route-monitor"
}

assert_no_ping() {
    if [ -s "$TMP/ping.log" ]; then
        printf 'probe ran without carrier: %s\n' "$(cat "$TMP/ping.log")" >&2
        exit 1
    fi
}

printf '%s\n' '192.168.8.1:192.168.8.2' > "$TMP/mdb-peer"
run_monitor success "$TMP/mdb-peer" 1
grep -Fqx -- '-c 1 -W 1 192.168.9.2' "$TMP/ping.log"
grep -qx 'route replace 192.168.7.2/32 via 192.168.9.2 dev usb0 metric 50' "$TMP/ip.log"
run_monitor failure "$TMP/mdb-peer" 1
grep -qx 'route del 192.168.7.2/32 via 192.168.9.2 dev usb0 metric 50' "$TMP/ip.log"

# Without carrier there is nothing to probe, and a probe would leave through
# the default route instead of the link.
run_monitor success "$TMP/mdb-peer" 0
assert_no_ping
grep -qx 'route del 192.168.7.2/32 via 192.168.9.2 dev usb0 metric 50' "$TMP/ip.log"

printf '%s\n' '192.168.8.2:192.168.8.1' > "$TMP/dbc-peer"
run_monitor success "$TMP/dbc-peer" 1
grep -Fqx -- '-c 1 -W 1 192.168.9.1' "$TMP/ping.log"
grep -qx 'route replace 192.168.7.1/32 via 192.168.9.1 dev usb0 metric 50' "$TMP/ip.log"
run_monitor success "$TMP/dbc-peer" 0
assert_no_ping
grep -qx 'route del 192.168.7.1/32 via 192.168.9.1 dev usb0 metric 50' "$TMP/ip.log"

: > "$TMP/ip.log"
PATH="$TMP/bin:$PATH" IP_LOG="$TMP/ip.log" PPP_IFACE=ppp0 \
    PPP_LOCAL=192.168.8.1 PPP_REMOTE=192.168.8.2 \
    "$ROOT/files/ip-up-backup-routes"
grep -qx 'route replace 192.168.7.2/32 via 192.168.8.2 dev ppp0 metric 200' "$TMP/ip.log"

: > "$TMP/ip.log"
PATH="$TMP/bin:$PATH" IP_LOG="$TMP/ip.log" PPP_IFACE=ppp0 \
    PPP_LOCAL=192.168.8.2 PPP_REMOTE=192.168.8.1 \
    "$ROOT/files/ip-up-backup-routes"
grep -qx 'route replace 192.168.7.1/32 via 192.168.8.1 dev ppp0 metric 200' "$TMP/ip.log"
grep -qx 'route replace default via 192.168.8.1 dev ppp0 metric 200' "$TMP/ip.log"
