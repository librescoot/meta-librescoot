#!/bin/bash
# Promote the .test kernel and DTB to the real names and unwrap bootcmd.
set -e
log() { echo "dbc.sh: $*"; }
B=/data/flashfree/backup; mkdir -p "$B"
[ -f "$B/zImage" ] || cp /boot/zImage "$B/zImage"
[ -f "$B/librescoot-dbc.dtb" ] || cp /boot/librescoot-dbc.dtb "$B/"
[ -f /boot/zImage.test ] && [ -f /boot/librescoot-dbc.dtb.test ] || { log "no .test files"; exit 1; }
cp /boot/zImage.test /boot/zImage.new && mv /boot/zImage.new /boot/zImage
cp /boot/librescoot-dbc.dtb.test /boot/librescoot-dbc.dtb.new && mv /boot/librescoot-dbc.dtb.new /boot/librescoot-dbc.dtb
orig=$(fw_printenv -n bootcmd_orig 2>/dev/null || true)
[ -n "$orig" ] && fw_setenv bootcmd "$orig" && fw_setenv bootcmd_orig && log "bootcmd restored"
fw_setenv testboot
md5sum /boot/zImage /boot/librescoot-dbc.dtb; fw_printenv bootcmd
sync; log "promoted"; exit 0
