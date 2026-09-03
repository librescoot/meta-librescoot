#!/bin/bash
# One-shot test-boot round: kernel and DTB go in as .test files, U-Boot boots
# them exactly once via a bootcmd wrapper; a failed boot falls back to the
# untouched originals on the next power cycle.
# usage: mktest.sh <label> <zImage> <dtb>  -> <label>/scripts/dbc.sh next to this script
set -e
LABEL=$1; ZIMAGE=$2; DTB=$3
[ -n "$LABEL" ] && [ -f "$ZIMAGE" ] && [ -f "$DTB" ] || { echo "usage: $0 <label> <zImage> <dtb>"; exit 1; }
HERE=$(cd "$(dirname "$0")" && pwd)
OUT=$HERE/$LABEL/scripts; mkdir -p "$OUT"
STAGE=$(mktemp -d); mkdir -p "$STAGE/boot"
cp "$ZIMAGE" "$STAGE/boot/zImage.test"; cp "$DTB" "$STAGE/boot/librescoot-dbc.dtb.test"
{
cat <<'HEAD'
#!/bin/bash
# One-shot kernel test round for the DBC (ums-service scripts/dbc.sh).
# Writes /boot/zImage.test and /boot/librescoot-dbc.dtb.test, leaves the
# originals alone, and wraps bootcmd so that with testboot=1 U-Boot clears the
# flag, saves the environment, and only then boots the .test files for this one
# boot. A panic or hang therefore falls back to the originals on the next power
# cycle. No reboot here: ums-service powers the DBC off, boot it by hand.
set -e
D=/data/flashfree; P=$D/payload-test
rm -rf "$P"; mkdir -p "$D" "$P"
log() { echo "dbc.sh: $*"; }
sed -n '/^__PAYLOAD__$/,$p' "$0" | tail -n +2 | base64 -d | tar -C "$P" -xzf -
[ -f "$P/boot/zImage.test" ] && [ -f "$P/boot/librescoot-dbc.dtb.test" ] || { log "payload missing"; exit 1; }
cp "$P/boot/zImage.test" /boot/zImage.test.new && mv /boot/zImage.test.new /boot/zImage.test
cp "$P/boot/librescoot-dbc.dtb.test" /boot/librescoot-dbc.dtb.test.new && mv /boot/librescoot-dbc.dtb.test.new /boot/librescoot-dbc.dtb.test
md5sum /boot/zImage /boot/librescoot-dbc.dtb /boot/zImage.test /boot/librescoot-dbc.dtb.test
orig=$(fw_printenv -n bootcmd)
case "$orig" in
  *testboot*) log "bootcmd already wrapped" ;;
  *) fw_setenv bootcmd_orig "$orig"
     fw_setenv bootcmd 'if test "${testboot}" = 1; then setenv testboot 0; saveenv; setenv mender_kernel_name zImage.test; setenv mender_dtb_name librescoot-dbc.dtb.test; fi; run bootcmd_orig'
     log "bootcmd wrapped" ;;
esac
fw_setenv testboot 1
fw_printenv bootcmd bootcmd_orig testboot mender_kernel_name mender_dtb_name 2>&1
sync
log "done: next boot uses the .test kernel once"
exit 0
__PAYLOAD__
HEAD
tar -C "$STAGE" -czf - . | base64 -w 76
} > "$OUT/dbc.sh"
chmod +x "$OUT/dbc.sh"; rm -rf "$STAGE"; ls -la "$OUT/dbc.sh"
