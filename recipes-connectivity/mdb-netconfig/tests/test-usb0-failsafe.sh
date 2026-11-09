#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/bin"

cat > "$TMP/bin/redis-cli" <<'EOF'
#!/bin/sh
printf '%s\n' "$GATE_VALUE"
EOF
cat > "$TMP/bin/systemctl" <<'EOF'
#!/bin/sh
printf '%s\n' "$*" >> "$SYSTEMCTL_LOG"
[ "$SYSTEMCTL_RESULT" = success ]
EOF
cat > "$TMP/bin/ip" <<'EOF'
#!/bin/sh
printf '%s\n' "$*" >> "$IP_LOG"
EOF
chmod +x "$TMP/bin/redis-cli" "$TMP/bin/systemctl" "$TMP/bin/ip"

# run_failsafe GATE_VALUE SYSTEMCTL_RESULT
run_failsafe() {
    : > "$TMP/systemctl.log"
    : > "$TMP/ip.log"
    PATH="$TMP/bin:$PATH" SYSTEMCTL_LOG="$TMP/systemctl.log" IP_LOG="$TMP/ip.log" \
        GATE_VALUE="$1" SYSTEMCTL_RESULT="$2" \
        sh "$ROOT/files/librescoot-usb0-failsafe.sh" > "$TMP/output.log" 2>&1
}

# vehicle-service decided: the failsafe must not touch anything.
run_failsafe closed success
grep -q 'vehicle-service has the link' "$TMP/output.log"
[ ! -s "$TMP/systemctl.log" ] || { echo "failsafe called systemctl despite a recorded gate" >&2; exit 1; }
[ ! -s "$TMP/ip.log" ] || { echo "failsafe raised usb0 despite a recorded gate" >&2; exit 1; }

# No decision: raise the link through the unit, so the route monitor follows.
run_failsafe '' success
grep -qx 'start librescoot-dbc-link.service' "$TMP/systemctl.log"
[ ! -s "$TMP/ip.log" ] || { echo "failsafe bypassed the link unit" >&2; exit 1; }

# No unit on this image: fall back to raising usb0 directly.
run_failsafe '' failure
grep -qx 'start librescoot-dbc-link.service' "$TMP/systemctl.log"
grep -qx 'link set usb0 up' "$TMP/ip.log"
