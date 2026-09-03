#!/bin/bash
# Build a ums-service round script that installs a test kernel on the DBC.
# usage: mkround.sh <label> <zImage> [dtb] [u-boot-dtb.imx]  -> <label>/scripts/dbc.sh next to this script
set -e
LABEL=$1; ZIMAGE=$2; DTB=$3; UBOOT=$4
[ -n "$LABEL" ] && [ -f "$ZIMAGE" ] || { echo "usage: $0 <label> <zImage> [dtb] [u-boot-dtb.imx]"; exit 1; }
HERE=$(cd "$(dirname "$0")" && pwd)
OUT=$HERE/$LABEL/scripts; mkdir -p "$OUT"
STAGE=$(mktemp -d); mkdir -p "$STAGE/boot"
cp "$ZIMAGE" "$STAGE/boot/zImage"
[ -n "$DTB" ] && cp "$DTB" "$STAGE/boot/librescoot-dbc.dtb"
[ -n "$UBOOT" ] && cp "$UBOOT" "$STAGE/boot/u-boot-dtb.imx"
{
cat <<'HEAD'
#!/bin/bash
# Kernel test round for the DBC, delivered through ums-service (scripts/dbc.sh).
# Installs /boot/zImage (and /boot/librescoot-dbc.dtb when the payload carries
# one, and U-Boot to eMMC sector 2 when the payload carries u-boot-dtb.imx),
# keeps the first-ever originals under /data/flashfree/backup. No reboot:
# ums-service powers the DBC off after processing, boot it by hand.
set -e
D=/data/flashfree
B=$D/backup
P=$D/payload-kernel
rm -rf "$P"; mkdir -p "$B" "$P"
log() { echo "dbc.sh: $*"; }

sed -n '/^__PAYLOAD__$/,$p' "$0" | tail -n +2 | base64 -d | tar -C "$P" -xzf -
[ -f "$P/boot/zImage" ] || { log "payload missing"; exit 1; }

[ -f "$B/zImage" ] || cp /boot/zImage "$B/zImage"
[ -f "$B/librescoot-dbc.dtb" ] || cp /boot/librescoot-dbc.dtb "$B/"
[ -f "$B/uboot-sectors.bin" ] || dd if=/dev/mmcblk3 of="$B/uboot-sectors.bin" bs=512 skip=2 count=2048 2>/dev/null
md5sum /boot/zImage /boot/librescoot-dbc.dtb | sed 's/^/before: /'

cp "$P/boot/zImage" /boot/zImage.new && mv /boot/zImage.new /boot/zImage
if [ -f "$P/boot/librescoot-dbc.dtb" ]; then
    cp "$P/boot/librescoot-dbc.dtb" /boot/librescoot-dbc.dtb.new && mv /boot/librescoot-dbc.dtb.new /boot/librescoot-dbc.dtb
fi
if [ -f "$P/boot/u-boot-dtb.imx" ]; then
    # U-Boot lives in the eMMC user area, IVT at 1 KiB (sector 2); boot0 is inert on this board
    dd if="$P/boot/u-boot-dtb.imx" of=/dev/mmcblk3 bs=512 seek=2 conv=fsync 2>/dev/null
    n=$(stat -c %s "$P/boot/u-boot-dtb.imx")
    echo "u-boot: $(dd if=/dev/mmcblk3 bs=512 skip=2 count=$(( (n + 511) / 512 )) 2>/dev/null | head -c $n | md5sum | cut -c1-32) expected $(md5sum "$P/boot/u-boot-dtb.imx" | cut -c1-32)"
fi
md5sum /boot/zImage /boot/librescoot-dbc.dtb | sed 's/^/after:  /'
sync
log "done"
exit 0
__PAYLOAD__
HEAD
tar -C "$STAGE" -czf - . | base64 -w 76
} > "$OUT/dbc.sh"
chmod +x "$OUT/dbc.sh"
rm -rf "$STAGE"
ls -la "$OUT/dbc.sh"
