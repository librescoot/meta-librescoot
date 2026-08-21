#!/bin/sh
# dbc-sdp-probe — does the DBC come up in i.MX serial-download mode?
#
# A DBC with a corrupted bootloader cannot be recovered over UMS, because UMS is
# served by U-Boot and dies with it. The i.MX6 boot ROM falls back to serial
# download (SDP) below U-Boot, enumerating as a USB device — 15A2:0061 for the
# DBC's i.MX6SL. Nothing on the vehicle can host that today: the MDB's OTG port
# is the gadget end of the link and the DBC is the host.
#
# This probe flips the MDB's OTG port to host, powers the DBC, and looks for the
# ROM on the bus. It answers one question: can the MDB see a bricked DBC well
# enough to recover it in the field, without a laptop and without pulling the
# board.
#
# It only ever reports. It writes nothing to the DBC.
#
# Exit: 0 SDP device found, 1 not found, 2 could not run the probe.
#
# Caveats worth knowing before running:
#   - Flipping the role tears down usb0, the primary MDB<->DBC link. A healthy
#     DBC loses Redis for the duration. Refused unless the DBC looks dead or
#     --force is given.
#   - The MDB has no vbus-supply in its device tree, so it cannot power the bus
#     itself. VBUS has to come from the DBC's own rail once lsc powers it. If
#     the probe never sees anything, that is the first thing to suspect, and it
#     means SDP recovery from the MDB needs a hardware change rather than a
#     script.
#   - ci_hdrc.1 carries the modem. Nothing here touches it.

set -u

OTG=/sys/bus/platform/devices/ci_hdrc.0
ROLE="$OTG/role"
DBC_IP=192.168.7.2
SDP_VENDOR=15a2
SDP_PRODUCT=0061
WAIT_SECONDS=25
FORCE=0
LEAVE_ON=0

usage() {
    cat <<EOF
Usage: dbc-sdp-probe [--force] [--leave-on] [--wait SECONDS]

  --force      probe even when the DBC is answering (this WILL drop its usb0 link)
  --leave-on   leave the DBC powered on exit, for a follow-up recovery attempt
  --wait N     how long to watch the bus for the ROM (default ${WAIT_SECONDS}s)
EOF
}

while [ $# -gt 0 ]; do
    case "$1" in
        --force)    FORCE=1 ;;
        --leave-on) LEAVE_ON=1 ;;
        --wait)     shift; WAIT_SECONDS="${1:-25}" ;;
        -h|--help)  usage; exit 0 ;;
        *)          echo "unknown argument: $1" >&2; usage >&2; exit 2 ;;
    esac
    shift
done

log() { echo "[sdp-probe] $*"; }
die() { echo "[sdp-probe] $*" >&2; exit 2; }

[ -w "$ROLE" ] || die "no writable $ROLE — is this an MDB?"
command -v lsc >/dev/null 2>&1 || die "lsc not found; needed to control DBC power"

ORIGINAL_ROLE=$(cat "$ROLE" 2>/dev/null) || die "cannot read $ROLE"
log "OTG port currently in '$ORIGINAL_ROLE' role"

# Refuse to disrupt a working link unless told otherwise. A DBC that answers is
# a DBC that does not need recovering.
if [ "$FORCE" -eq 0 ] && ping -c1 -W2 "$DBC_IP" >/dev/null 2>&1; then
    die "DBC is answering at $DBC_IP — it is not bricked. Use --force to probe anyway (drops its link)."
fi

GADGET_WAS_LOADED=0
lsmod 2>/dev/null | grep -q '^g_ether' && GADGET_WAS_LOADED=1

# Restore runs on every exit path, including Ctrl-C and any failure below.
# Leaving the MDB in host role would leave the vehicle without its DBC link.
restore() {
    log "restoring"
    [ "$LEAVE_ON" -eq 1 ] || lsc dbc off >/dev/null 2>&1
    if [ "$(cat "$ROLE" 2>/dev/null)" != "$ORIGINAL_ROLE" ]; then
        echo "$ORIGINAL_ROLE" > "$ROLE" 2>/dev/null \
            || log "WARNING could not restore role to '$ORIGINAL_ROLE' — usb0 will stay down"
    fi
    if [ "$GADGET_WAS_LOADED" -eq 1 ] && ! lsmod 2>/dev/null | grep -q '^g_ether'; then
        modprobe g_ether >/dev/null 2>&1 || log "WARNING could not reload g_ether"
    fi
    # Give networkd a moment to bring usb0 back before we claim to be done.
    i=0
    while [ "$i" -lt 10 ]; do
        [ -d /sys/class/net/usb0 ] && break
        sleep 1
        i=$((i + 1))
    done
    if [ -d /sys/class/net/usb0 ]; then
        log "usb0 is back"
    else
        log "WARNING usb0 did not return — the DBC link is down, check 'ip link'"
    fi
}
trap restore EXIT INT TERM

log "powering DBC off for a clean start"
lsc dbc off >/dev/null 2>&1
sleep 2

# The gadget holds the UDC; the role cannot flip underneath it.
if [ "$GADGET_WAS_LOADED" -eq 1 ]; then
    log "unloading g_ether"
    modprobe -r g_ether 2>/dev/null || log "WARNING g_ether would not unload; the flip may fail"
fi

log "switching OTG port to host"
echo host > "$ROLE" 2>/dev/null || die "could not write 'host' to $ROLE"
sleep 1
[ "$(cat "$ROLE" 2>/dev/null)" = "host" ] || die "role did not take; still '$(cat "$ROLE" 2>/dev/null)'"

# Host first, then power: the ROM's SDP window opens early, so the bus needs to
# be watching before the DBC starts.
log "powering DBC on"
lsc dbc on >/dev/null 2>&1 || die "lsc dbc on failed"

log "watching the bus for ${SDP_VENDOR}:${SDP_PRODUCT} for ${WAIT_SECONDS}s"
found=""
elapsed=0
while [ "$elapsed" -lt "$WAIT_SECONDS" ]; do
    for d in /sys/bus/usb/devices/*/; do
        [ -r "$d/idVendor" ] || continue
        v=$(cat "$d/idVendor" 2>/dev/null)
        p=$(cat "$d/idProduct" 2>/dev/null)
        if [ "$v" = "$SDP_VENDOR" ] && [ "$p" = "$SDP_PRODUCT" ]; then
            found="$d"
            break
        fi
    done
    [ -n "$found" ] && break
    sleep 1
    elapsed=$((elapsed + 1))
done

if [ -n "$found" ]; then
    log "FOUND: DBC boot ROM in serial-download mode after ${elapsed}s"
    log "  path:    $found"
    log "  vid:pid: $(cat "$found/idVendor" 2>/dev/null):$(cat "$found/idProduct" 2>/dev/null)"
    log "  product: $(cat "$found/product" 2>/dev/null || echo '(none)')"
    log "SDP recovery from the MDB is viable on this hardware."
    exit 0
fi

log "not found after ${WAIT_SECONDS}s"
log "Either the DBC booted normally (so it was never in SDP), or the MDB cannot"
log "see it. Devices currently on the bus:"
for d in /sys/bus/usb/devices/*/; do
    [ -r "$d/idVendor" ] || continue
    log "  $(cat "$d/idVendor" 2>/dev/null):$(cat "$d/idProduct" 2>/dev/null) $(cat "$d/product" 2>/dev/null || echo '')"
done
log "If nothing new appeared at all, suspect VBUS: this port has no vbus-supply,"
log "so it depends on the DBC driving the rail from its own side."
exit 1
