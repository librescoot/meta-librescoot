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

# run_failsafe GATE_VALUE SYSTEMCTL_RESULT [RUN_STATE_CONTENT]
# An empty RUN_STATE_CONTENT leaves no run record at all.
run_failsafe() {
    : > "$TMP/systemctl.log"
    : > "$TMP/ip.log"
    rm -f "$TMP/run-state"
    if [ -n "${3:-}" ]; then
        printf '%s\n' "$3" > "$TMP/run-state"
    fi
    PATH="$TMP/bin:$PATH" SYSTEMCTL_LOG="$TMP/systemctl.log" IP_LOG="$TMP/ip.log" \
        LIBRESCOOT_INSTALLER_RUN_STATE="$TMP/run-state" \
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

# An open install run outranks a recorded gate. The installer learns the
# scooter's keycards over usb0, and registering the second card closes the gate
# on the keycard threshold; honouring that would take usb0 down under the
# installer that still has to verify and finish.
run_failsafe closed success 'run-id: run-abc-1
stage: flash
result: running
finish: pending'
grep -q 'Installer run in progress' "$TMP/output.log"
grep -qx 'start librescoot-dbc-link.service' "$TMP/systemctl.log"

# A run record without a finish field has only just been written: still open.
run_failsafe closed success 'run-id: run-abc-1
stage: flash'
grep -q 'Installer run in progress' "$TMP/output.log"
grep -qx 'start librescoot-dbc-link.service' "$TMP/systemctl.log"

# The finished run hands the decision back to vehicle-service.
run_failsafe closed success 'run-id: run-abc-1
stage: complete
result: success
finish: complete'
grep -q 'vehicle-service has the link' "$TMP/output.log"
[ ! -s "$TMP/systemctl.log" ] || { echo "failsafe raised usb0 after the run finished" >&2; exit 1; }
