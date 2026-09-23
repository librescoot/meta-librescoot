#!/bin/bash
# One-shot test-boot round: kernel and DTB go in as .test files, U-Boot boots
# them exactly once via a bootcmd wrapper; a failed boot falls back to the
# untouched originals on the next power cycle.
# usage: mktest.sh <label> <zImage> <dtb> [modules-dir]
#
# The module tree is required: a kernel booted without its own /lib/modules
# silently loses every driver that is not built in. Installing it is additive —
# the release already on the board keeps its tree — so the fallback boot is
# unaffected.
set -e
LABEL=$1; ZIMAGE=$2; DTB=$3; MODULES=${4:-$MODULES}
[ -n "$LABEL" ] && [ -f "$ZIMAGE" ] && [ -f "$DTB" ] || {
    echo "usage: $0 <label> <zImage> <dtb> [modules-dir]" >&2
    echo "       (modules-dir may also be set as \$MODULES)" >&2
    exit 1
}
[ -n "$MODULES" ] && [ -d "$MODULES" ] || {
    echo "$0: a module tree is required: pass the lib/modules/<release> dir matching the kernel" >&2
    echo "    (or state the release as \$KERNEL_VERSION to install the kernel alone)" >&2
    [ -n "${KERNEL_VERSION:-}" ] || exit 1
}
VER=${KERNEL_VERSION:-}
if [ -n "$MODULES" ]; then
    VER=$(basename "${MODULES%/}")
    case "$VER" in
        ''|*/*|*' '*) echo "$0: cannot derive a release from '$MODULES'" >&2; exit 1 ;;
    esac
fi
[ -n "$VER" ] || { echo "$0: no kernel release to install under" >&2; exit 1; }
HERE=$(cd "$(dirname "$0")" && pwd)
OUT=$HERE/$LABEL/scripts; mkdir -p "$OUT"
STAGE=$(mktemp -d); mkdir -p "$STAGE/boot"
cp "$ZIMAGE" "$STAGE/boot/zImage.test"; cp "$DTB" "$STAGE/boot/librescoot-dbc.dtb.test"
if [ -n "$MODULES" ]; then
    mkdir -p "$STAGE/modules/$VER"
    cp -a "$MODULES/." "$STAGE/modules/$VER/"
fi
printf '%s\n' "$VER" > "$STAGE/KERNEL_VERSION"
{
cat <<'HEAD'
#!/bin/bash
# One-shot kernel test round for the DBC (ums-service scripts/dbc.sh).
# Installs the payload's module tree under /lib/modules (additive), then writes
# /boot/zImage.test and /boot/librescoot-dbc.dtb.test, leaves the originals
# alone, and wraps bootcmd so that with testboot=1 U-Boot clears the flag, saves
# the environment, and only then boots the .test files for this one boot. A panic
# or hang therefore falls back to the originals on the next power cycle, which
# still find their own modules. The release is recorded for promote.sh. No reboot
# here: ums-service powers the DBC off, boot it by hand.
#
# ROOT prefixes the filesystem paths, so the install can be dry-run off-board.
set -e
ROOT=${ROOT:-}
D=$ROOT/data/flashfree; P=$D/payload-test
rm -rf "$P"; mkdir -p "$D" "$P"
log() { echo "dbc.sh: $*"; }
sed -n '/^__PAYLOAD__$/,$p' "$0" | tail -n +2 | base64 -d | tar -C "$P" -xzf -
[ -f "$P/boot/zImage.test" ] && [ -f "$P/boot/librescoot-dbc.dtb.test" ] || { log "payload missing"; exit 1; }
VER=$(cat "$P/KERNEL_VERSION" 2>/dev/null || true)
[ -n "$VER" ] || { log "payload carries no kernel release; refusing"; exit 1; }

if [ -d "$P/modules/$VER" ]; then
    rm -rf "$ROOT/lib/modules/$VER.new"; mkdir -p "$ROOT/lib/modules/$VER.new"
    cp -a "$P/modules/$VER/." "$ROOT/lib/modules/$VER.new/"
    rm -rf "$ROOT/lib/modules/$VER"
    mv "$ROOT/lib/modules/$VER.new" "$ROOT/lib/modules/$VER"
    log "modules: /lib/modules/$VER ($(du -sh "$ROOT/lib/modules/$VER" | cut -f1))"
else
    log "WARNING: no module tree in the payload; $VER runs with whatever /lib/modules holds"
fi
printf '%s\n' "$VER" > "$D/test-kernel-version"

cp "$P/boot/zImage.test" "$ROOT/boot/zImage.test.new" && mv "$ROOT/boot/zImage.test.new" "$ROOT/boot/zImage.test"
cp "$P/boot/librescoot-dbc.dtb.test" "$ROOT/boot/librescoot-dbc.dtb.test.new" && mv "$ROOT/boot/librescoot-dbc.dtb.test.new" "$ROOT/boot/librescoot-dbc.dtb.test"
md5sum "$ROOT/boot/zImage" "$ROOT/boot/librescoot-dbc.dtb" "$ROOT/boot/zImage.test" "$ROOT/boot/librescoot-dbc.dtb.test"
if [ -z "$ROOT" ]; then
    orig=$(fw_printenv -n bootcmd)
    case "$orig" in
      *testboot*) log "bootcmd already wrapped" ;;
      *) fw_setenv bootcmd_orig "$orig"
         fw_setenv bootcmd 'if test "${testboot}" = 1; then setenv testboot 0; saveenv; setenv mender_kernel_name zImage.test; setenv mender_dtb_name librescoot-dbc.dtb.test; fi; run bootcmd_orig'
         log "bootcmd wrapped" ;;
    esac
    fw_setenv testboot 1
    fw_printenv bootcmd bootcmd_orig testboot mender_kernel_name mender_dtb_name 2>&1
fi
sync
log "done: next boot uses the .test kernel once ($VER)"
exit 0
__PAYLOAD__
HEAD
tar -C "$STAGE" -czf - . | base64 -w 76
} > "$OUT/dbc.sh"
chmod +x "$OUT/dbc.sh"; rm -rf "$STAGE"; ls -la "$OUT/dbc.sh"
