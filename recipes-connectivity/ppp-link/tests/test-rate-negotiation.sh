#!/bin/sh
set -eu

ROOT=$(mktemp -d)
trap 'jobs -p | xargs kill 2>/dev/null || true; rm -rf "$ROOT"' EXIT
FILES="$PWD/recipes-connectivity/ppp-link/files"
mkdir -p "$ROOT/bin" "$ROOT/redis"

cat > "$ROOT/bin/redis-cli" <<'EOF'
#!/bin/sh
set -eu
[ "$1" = --raw ] && shift
[ "$1" = -h ] && HOST="$2" && shift 2
ROOT="$TEST_ROOT/redis"
key_file() { printf '%s/%s' "$ROOT" "$(printf '%s' "$1" | tr ':/' '__')"; }
case "$1" in
    GET)
        FILE=$(key_file "$2")
        [ -r "$FILE" ] && cat "$FILE"
        ;;
    SET)
        FILE=$(key_file "$2")
        printf '%s\n' "$3" > "$FILE.$$"
        mv "$FILE.$$" "$FILE"
        printf 'OK\n'
        ;;
    DEL)
        shift
        for KEY in "$@"; do rm -f "$(key_file "$KEY")"; done
        printf '1\n'
        ;;
    HGET)
        case "$2:$3" in
            usb:mode) cat "$TEST_ROOT/usb-mode" ;;
            usb:status) cat "$TEST_ROOT/usb-status" ;;
            vehicle:dbc-updating) cat "$TEST_ROOT/dbc-updating" ;;
            ota:status) cat "$TEST_ROOT/ota-status" ;;
        esac
        ;;
    PING)
        [ "${REDIS_FAIL_HIGH:-0}" != 1 ] || exit 1
        [ "$HOST" != 192.168.8.1 ] || {
            [ "$(cat "$TEST_ROOT/mdb-rate")" = 2500000 ]
            [ "$(cat "$TEST_ROOT/dbc-rate")" = 2500000 ]
        }
        printf 'PONG\n'
        ;;
    *) exit 2 ;;
esac
EOF

cat > "$ROOT/bin/ping" <<'EOF'
#!/bin/sh
set -eu
[ "$(cat "$TEST_ROOT/mdb-rate")" = 2500000 ]
[ "$(cat "$TEST_ROOT/dbc-rate")" = 2500000 ]
EOF

cat > "$ROOT/bin/systemctl" <<'EOF'
#!/bin/sh
set -eu
printf '%s\n' "$*" >> "$TEST_ROOT/systemctl.log"
case "$1" in
    stop)
        case "$2" in
            ppp-link-rate-fallback*.timer)
                if [ -r "$PPP_RATE_STATE_DIR/fallback.pid" ]; then
                    kill "$(cat "$PPP_RATE_STATE_DIR/fallback.pid")" 2>/dev/null || true
                    rm -f "$PPP_RATE_STATE_DIR/fallback.pid"
                fi
                ;;
        esac
        ;;
esac
exit 0
EOF

cat > "$ROOT/bin/systemd-run" <<'EOF'
#!/bin/sh
set -eu
UNIT=
while [ "$#" -gt 0 ]; do
    case "$1" in
        --unit=*) UNIT=${1#--unit=} ;;
        --on-active=*) ;;
        --quiet|--collect) ;;
        *) break ;;
    esac
    shift
done
printf '%s\n' "$UNIT" >> "$TEST_ROOT/systemd-run.log"
case "$UNIT" in
ppp-link-rate-fallback-*)
    (sleep "${TEST_ROLLBACK_DELAY:-6}"; "$1" "$2") &
    echo "$!" > "$PPP_RATE_STATE_DIR/fallback.pid"
    ;;
*)
    "$1" "$2"
    ;;
esac
EOF

chmod +x "$ROOT/bin/redis-cli" "$ROOT/bin/ping" "$ROOT/bin/systemctl" "$ROOT/bin/systemd-run"
printf 'normal\n' > "$ROOT/usb-mode"
printf 'idle\n' > "$ROOT/usb-status"
printf 'false\n' > "$ROOT/dbc-updating"
printf 'idle\n' > "$ROOT/ota-status"
printf '192.168.8.1:192.168.8.2\n' > "$ROOT/mdb-peer"
printf '192.168.8.2:192.168.8.1\n' > "$ROOT/dbc-peer"

run_negotiator() {
    ROLE="$1"
    shift
    exec env \
        PPP_RATE_FILE="$ROOT/$ROLE-rate" \
        PPP_RATE_STATE_DIR="$ROOT/$ROLE-state" \
        PPP_PEER_CONFIG="$ROOT/$ROLE-peer" \
        PPP_SET_RATE="$FILES/ppp-link-set-rate" \
        REDIS_CLI="$ROOT/bin/redis-cli" \
        SYSTEMCTL="$ROOT/bin/systemctl" \
        SYSTEMD_RUN="$ROOT/bin/systemd-run" \
        PING="$ROOT/bin/ping" \
        PPP_RATE_NO_RESTART=1 \
        PPP_RATE_POLL_INTERVAL=1 \
        PPP_RATE_WATCH_FAILURES=2 \
        PPP_RATE_STABLE_CHECKS=2 \
        PPP_RATE_MAX_ATTEMPTS=1 \
        PPP_RATE_RETRY_DELAY=1 \
        PPP_RATE_SWITCH_DELAY=0 \
        PPP_RATE_PREPARE_TIMEOUT=4 \
        PPP_RATE_CONFIRM_TIMEOUT=4 \
        PPP_RATE_ROLLBACK_DELAY=15 \
        TEST_ROOT="$ROOT" \
        TEST_ROLLBACK_DELAY=15 \
        "$@" "$FILES/ppp-link-rate-negotiator"
}

wait_for_exit() {
    PID="$1"
    LIMIT="$2"
    while kill -0 "$PID" 2>/dev/null && [ "$LIMIT" -gt 0 ]; do
        sleep 1
        LIMIT=$((LIMIT - 1))
    done
    ! kill -0 "$PID" 2>/dev/null
}

wait_for_file() {
    FILE="$1"
    LIMIT="$2"
    while [ ! -e "$FILE" ] && [ "$LIMIT" -gt 0 ]; do
        sleep 1
        LIMIT=$((LIMIT - 1))
    done
    [ -e "$FILE" ]
}

rm -rf "$ROOT/redis" "$ROOT/mdb-state" "$ROOT/dbc-state"
mkdir -p "$ROOT/redis"
printf '250000\n' > "$ROOT/mdb-rate"
printf '250000\n' > "$ROOT/dbc-rate"
run_negotiator dbc & DBC_PID=$!
sleep 1
run_negotiator mdb & MDB_PID=$!
wait_for_file "$ROOT/mdb-state/ppp-rate-negotiated" 20
wait_for_file "$ROOT/dbc-state/ppp-rate-negotiated" 5
[ "$(cat "$ROOT/mdb-rate")" = 2500000 ]
[ "$(cat "$ROOT/dbc-rate")" = 2500000 ]
grep -Eq '^ppp-link-rate-fallback-.+' "$ROOT/systemd-run.log"
grep -Eq '^ppp-link-rate-switch-.+' "$ROOT/systemd-run.log"
printf '250000\n' > "$ROOT/mdb-rate"
sleep 4
[ "$(cat "$ROOT/dbc-rate")" = 250000 ]
kill "$MDB_PID" "$DBC_PID" 2>/dev/null || true
wait "$MDB_PID" 2>/dev/null || true
wait "$DBC_PID" 2>/dev/null || true

rm -rf "$ROOT/redis" "$ROOT/mdb-state" "$ROOT/dbc-state"
mkdir -p "$ROOT/redis"
printf '250000\n' > "$ROOT/mdb-rate"
printf '250000\n' > "$ROOT/dbc-rate"
run_negotiator mdb & MDB_PID=$!
sleep 2
[ ! -e "$ROOT/mdb-state/ppp-rate-attempts" ]
kill "$MDB_PID"
wait "$MDB_PID" 2>/dev/null || true

rm -rf "$ROOT/redis" "$ROOT/mdb-state" "$ROOT/dbc-state"
mkdir -p "$ROOT/redis"
printf '1\n' > "$ROOT/redis/ppp-link_rate_dbc-capable"
printf '250000\n' > "$ROOT/mdb-rate"
run_negotiator mdb PPP_RATE_MAX_ATTEMPTS=2 PPP_RATE_PREPARE_TIMEOUT=1 & MDB_PID=$!
wait_for_exit "$MDB_PID" 8
wait "$MDB_PID"
[ "$(cat "$ROOT/mdb-state/ppp-rate-attempts")" = 2 ]

rm -rf "$ROOT/redis" "$ROOT/mdb-state" "$ROOT/dbc-state"
mkdir -p "$ROOT/redis"
printf 'ums\n' > "$ROOT/usb-mode"
printf '250000\n' > "$ROOT/mdb-rate"
printf '250000\n' > "$ROOT/dbc-rate"
run_negotiator dbc & DBC_PID=$!
sleep 1
run_negotiator mdb & MDB_PID=$!
sleep 2
[ ! -e "$ROOT/mdb-state/ppp-rate-attempts" ]
kill "$MDB_PID" "$DBC_PID"
wait "$MDB_PID" 2>/dev/null || true
wait "$DBC_PID" 2>/dev/null || true
printf 'normal\n' > "$ROOT/usb-mode"

rm -rf "$ROOT/redis" "$ROOT/mdb-state" "$ROOT/dbc-state"
mkdir -p "$ROOT/redis"
printf 'downloading-updates\n' > "$ROOT/ota-status"
printf '250000\n' > "$ROOT/mdb-rate"
printf '250000\n' > "$ROOT/dbc-rate"
run_negotiator dbc & DBC_PID=$!
sleep 1
run_negotiator mdb & MDB_PID=$!
sleep 2
[ ! -e "$ROOT/mdb-state/ppp-rate-attempts" ]
kill "$MDB_PID" "$DBC_PID"
wait "$MDB_PID" 2>/dev/null || true
wait "$DBC_PID" 2>/dev/null || true
printf 'idle\n' > "$ROOT/ota-status"

rm -rf "$ROOT/redis" "$ROOT/mdb-state" "$ROOT/dbc-state"
mkdir -p "$ROOT/redis"
printf '250000\n' > "$ROOT/mdb-rate"
printf '250000\n' > "$ROOT/dbc-rate"
run_negotiator dbc REDIS_FAIL_HIGH=1 & DBC_PID=$!
sleep 1
run_negotiator mdb & MDB_PID=$!
wait_for_exit "$MDB_PID" 12
wait "$MDB_PID" 2>/dev/null || true
sleep 16
[ "$(cat "$ROOT/mdb-rate")" = 250000 ]
[ "$(cat "$ROOT/dbc-rate")" = 250000 ]
kill "$DBC_PID" 2>/dev/null || true
wait "$DBC_PID" 2>/dev/null || true

printf '250000\n' > "$ROOT/set-rate"
mkdir -p "$ROOT/set-state"
PPP_RATE_FILE="$ROOT/set-rate" PPP_RATE_STATE_DIR="$ROOT/set-state" \
    PPP_RATE_NO_RESTART=1 "$FILES/ppp-link-set-rate" 2500000
[ "$(cat "$ROOT/set-rate")" = 2500000 ]
touch "$ROOT/set-state/ppp-link-disabled"
PPP_RATE_FILE="$ROOT/set-rate" PPP_RATE_STATE_DIR="$ROOT/set-state" \
    PPP_RATE_NO_RESTART=1 "$FILES/ppp-link-set-rate" 250000
PPP_RATE_FILE="$ROOT/set-rate" PPP_RATE_STATE_DIR="$ROOT/set-state" \
    PPP_RATE_NO_RESTART=1 "$FILES/ppp-link-set-rate" 2500000
[ "$(cat "$ROOT/set-rate")" = 250000 ]
if PPP_RATE_FILE="$ROOT/set-rate" PPP_RATE_STATE_DIR="$ROOT/set-state" \
    PPP_RATE_NO_RESTART=1 "$FILES/ppp-link-set-rate" 5000000 2>/dev/null; then
    exit 1
fi
[ "$(cat "$ROOT/set-rate")" = 250000 ]

printf '2500000\n' > "$ROOT/set-rate"
: > "$ROOT/systemctl.log"
PPP_RATE_FILE="$ROOT/set-rate" PPP_RATE_STATE_DIR="$ROOT/set-state" \
    PPP_SET_RATE="$FILES/ppp-link-set-rate" SYSTEMCTL="$ROOT/bin/systemctl" \
    TEST_ROOT="$ROOT" "$FILES/ppp-link-stop"
[ "$(cat "$ROOT/set-rate")" = 250000 ]
[ -e "$ROOT/set-state/ppp-link-disabled" ]
grep -q '^stop ppp-link-rate-negotiator.service$' "$ROOT/systemctl.log"
grep -q '^stop ppp-link-rate-switch.timer$' "$ROOT/systemctl.log"
grep -q '^stop ppp-link-rate-fallback.timer$' "$ROOT/systemctl.log"

printf '250000\n' > "$ROOT/start-rate"
PPP_RATE_FILE="$ROOT/start-rate" PPP_RATE_STATE_DIR="$ROOT/set-state" \
    PPPD=echo "$FILES/ppp-link-start" > "$ROOT/start.out"
grep -qx 'call uart-link 250000 nodetach' "$ROOT/start.out"
printf 'invalid\n' > "$ROOT/start-rate"
PPP_RATE_FILE="$ROOT/start-rate" PPP_RATE_STATE_DIR="$ROOT/set-state" \
    PPPD=echo "$FILES/ppp-link-start" > "$ROOT/start.out"
grep -qx 'call uart-link 250000 nodetach' "$ROOT/start.out"

printf 'rate negotiation tests passed\n'
