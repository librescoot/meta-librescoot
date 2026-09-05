#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/bin" "$TMP/usb"

cat > "$TMP/bin/ip" <<'EOF'
#!/bin/sh
printf '%s\n' "$*" >> "$IP_LOG"
EOF
cat > "$TMP/bin/ping" <<'EOF'
#!/bin/sh
[ "$PING_RESULT" = success ]
EOF
chmod +x "$TMP/bin/ip" "$TMP/bin/ping"

run_monitor() {
    : > "$TMP/ip.log"
    PATH="$TMP/bin:$PATH" IP_LOG="$TMP/ip.log" PING_RESULT="$1" \
        PEER_CONFIG="$2" USB_PATH="$TMP/usb" LINK_ROUTE_ONCE=1 \
        "$ROOT/files/librescoot-link-route-monitor"
}

printf '%s\n' '192.168.8.1:192.168.8.2' > "$TMP/mdb-peer"
run_monitor success "$TMP/mdb-peer"
grep -qx 'route replace 192.168.7.2/32 via 192.168.9.2 dev usb0 metric 50' "$TMP/ip.log"
run_monitor failure "$TMP/mdb-peer"
grep -qx 'route del 192.168.7.2/32 via 192.168.9.2 dev usb0 metric 50' "$TMP/ip.log"

printf '%s\n' '192.168.8.2:192.168.8.1' > "$TMP/dbc-peer"
run_monitor success "$TMP/dbc-peer"
grep -qx 'route replace 192.168.7.1/32 via 192.168.9.1 dev usb0 metric 50' "$TMP/ip.log"

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
