#!/bin/bash
# Promote the .test kernel and DTB to the real names and unwrap bootcmd.
#
# The kernel goes in as /boot/zImage-<release> with /boot/zImage pointing at it,
# which is the shape the image ships: replacing the symlink with a plain file
# leaves the slot with a kernel whose module tree nothing tracks. The release is
# the one mktest.sh installed with the .test files, and its module tree is
# already in /lib/modules.
#
# ROOT prefixes the filesystem paths, so this can be dry-run off-board (the
# environment is left alone in that case).
set -e
ROOT=${ROOT:-}
D=$ROOT/data/flashfree
B=$D/backup; mkdir -p "$B"
log() { echo "dbc.sh: $*"; }

VER=$(cat "$D/test-kernel-version" 2>/dev/null || true)
[ -n "$VER" ] || { log "no recorded release: run mktest.sh first (it records the kernel it installed)"; exit 1; }
[ -f "$ROOT/boot/zImage.test" ] && [ -f "$ROOT/boot/librescoot-dbc.dtb.test" ] || { log "no .test files"; exit 1; }
[ -d "$ROOT/lib/modules/$VER" ] || { log "no /lib/modules/$VER: the promoted kernel would run without its modules"; exit 1; }

[ -f "$B/zImage" ] || cp "$ROOT/boot/zImage" "$B/zImage"
[ -f "$B/zImage-target" ] || readlink "$ROOT/boot/zImage" > "$B/zImage-target" 2>/dev/null || true
[ -f "$B/librescoot-dbc.dtb" ] || cp "$ROOT/boot/librescoot-dbc.dtb" "$B/"

cp "$ROOT/boot/zImage.test" "$ROOT/boot/zImage-$VER.new"
mv "$ROOT/boot/zImage-$VER.new" "$ROOT/boot/zImage-$VER"
ln -sfn "zImage-$VER" "$ROOT/boot/zImage"
cp "$ROOT/boot/librescoot-dbc.dtb.test" "$ROOT/boot/librescoot-dbc.dtb.new"
mv "$ROOT/boot/librescoot-dbc.dtb.new" "$ROOT/boot/librescoot-dbc.dtb"

if [ -z "$ROOT" ]; then
    orig=$(fw_printenv -n bootcmd_orig 2>/dev/null || true)
    [ -n "$orig" ] && fw_setenv bootcmd "$orig" && fw_setenv bootcmd_orig && log "bootcmd restored"
    fw_setenv testboot
    fw_printenv bootcmd
fi
ls -l "$ROOT/boot/zImage"; ls "$ROOT/lib/modules"
md5sum "$ROOT/boot/zImage" "$ROOT/boot/librescoot-dbc.dtb"
sync; log "promoted $VER"; exit 0
