#!/bin/bash
# Build a ums-service round script that installs a test kernel on the DBC.
# usage: mkround.sh <label> <zImage> [dtb] [u-boot-dtb.imx] [modules-dir]
#
# The module tree is required: a kernel whose /lib/modules/<release> is missing
# silently loses every driver that is not built in, which is how a mismatched
# round looks like a driver bug. Pass the tree that belongs to the kernel, e.g.
# the yocto work dir's lib/modules/<release>, or a copy of it.
set -e
LABEL=$1; ZIMAGE=$2; DTB=$3; UBOOT=$4; MODULES=${5:-$MODULES}
[ -n "$LABEL" ] && [ -f "$ZIMAGE" ] || {
    echo "usage: $0 <label> <zImage> [dtb] [u-boot-dtb.imx] [modules-dir]" >&2
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
[ -n "$VER" ] || { echo "$0: no kernel release to name the installed image after" >&2; exit 1; }
HERE=$(cd "$(dirname "$0")" && pwd)
OUT=$HERE/$LABEL/scripts; mkdir -p "$OUT"
STAGE=$(mktemp -d); mkdir -p "$STAGE/boot"
cp "$ZIMAGE" "$STAGE/boot/zImage"
[ -n "$DTB" ] && cp "$DTB" "$STAGE/boot/librescoot-dbc.dtb"
[ -n "$UBOOT" ] && cp "$UBOOT" "$STAGE/boot/u-boot-dtb.imx"
if [ -n "$MODULES" ]; then
    mkdir -p "$STAGE/modules/$VER"
    cp -a "$MODULES/." "$STAGE/modules/$VER/"
fi
printf '%s\n' "$VER" > "$STAGE/KERNEL_VERSION"
{
cat <<'HEAD'
#!/bin/bash
# Kernel test round for the DBC, delivered through ums-service (scripts/dbc.sh).
# Installs the payload's module tree under /lib/modules (additive: another
# release's tree is left alone), the kernel as /boot/zImage-<release> with
# /boot/zImage pointing at it — the shape the image ships, so the slot stays
# consistent — plus /boot/librescoot-dbc.dtb when carried, and U-Boot to eMMC
# sector 2 when carried. Keeps the first-ever originals under
# /data/flashfree/backup. No reboot: ums-service powers the DBC off after
# processing, boot it by hand.
#
# ROOT prefixes the filesystem paths, so the install can be dry-run off-board.
set -e
ROOT=${ROOT:-}
D=$ROOT/data/flashfree
B=$D/backup
P=$D/payload-kernel
rm -rf "$P"; mkdir -p "$B" "$P"
log() { echo "dbc.sh: $*"; }

sed -n '/^__PAYLOAD__$/,$p' "$0" | tail -n +2 | base64 -d | tar -C "$P" -xzf -
[ -f "$P/boot/zImage" ] || { log "payload missing"; exit 1; }
VER=$(cat "$P/KERNEL_VERSION" 2>/dev/null || true)
[ -n "$VER" ] || { log "payload carries no module tree; refusing to install a kernel without one"; exit 1; }

[ -f "$B/zImage" ] || cp "$ROOT/boot/zImage" "$B/zImage"
[ -f "$B/zImage-target" ] || readlink "$ROOT/boot/zImage" > "$B/zImage-target" 2>/dev/null || true
[ -f "$B/librescoot-dbc.dtb" ] || cp "$ROOT/boot/librescoot-dbc.dtb" "$B/"
if [ -z "$ROOT" ] && [ -e /dev/mmcblk3 ] && [ ! -f "$B/uboot-sectors.bin" ]; then
    dd if=/dev/mmcblk3 of="$B/uboot-sectors.bin" bs=512 skip=2 count=2048 2>/dev/null
fi
md5sum "$ROOT/boot/zImage" "$ROOT/boot/librescoot-dbc.dtb" | sed 's/^/before: /'

# Modules first: additive, and the new kernel cannot work without them. The host
# script refuses to build a round without a release, so $P/modules/$VER exists
# unless the caller stated a release and passed no tree.
if [ -d "$P/modules/$VER" ]; then
    rm -rf "$ROOT/lib/modules/$VER.new"; mkdir -p "$ROOT/lib/modules/$VER.new"
    cp -a "$P/modules/$VER/." "$ROOT/lib/modules/$VER.new/"
    rm -rf "$ROOT/lib/modules/$VER"
    mv "$ROOT/lib/modules/$VER.new" "$ROOT/lib/modules/$VER"
    log "modules: /lib/modules/$VER ($(du -sh "$ROOT/lib/modules/$VER" | cut -f1))"
else
    log "WARNING: no module tree in the payload; $VER runs with whatever /lib/modules holds"
fi

# Versioned file plus symlink, as the image ships it.
cp "$P/boot/zImage" "$ROOT/boot/zImage-$VER.new"
mv "$ROOT/boot/zImage-$VER.new" "$ROOT/boot/zImage-$VER"
ln -sfn "zImage-$VER" "$ROOT/boot/zImage"
if [ -f "$P/boot/librescoot-dbc.dtb" ]; then
    cp "$P/boot/librescoot-dbc.dtb" "$ROOT/boot/librescoot-dbc.dtb.new"
    mv "$ROOT/boot/librescoot-dbc.dtb.new" "$ROOT/boot/librescoot-dbc.dtb"
fi
if [ -f "$P/boot/u-boot-dtb.imx" ] && [ -z "$ROOT" ]; then
    # U-Boot lives in the eMMC user area, IVT at 1 KiB (sector 2); boot0 is inert on this board
    dd if="$P/boot/u-boot-dtb.imx" of=/dev/mmcblk3 bs=512 seek=2 conv=fsync 2>/dev/null
    n=$(stat -c %s "$P/boot/u-boot-dtb.imx")
    echo "u-boot: $(dd if=/dev/mmcblk3 bs=512 skip=2 count=$(( (n + 511) / 512 )) 2>/dev/null | head -c $n | md5sum | cut -c1-32) expected $(md5sum "$P/boot/u-boot-dtb.imx" | cut -c1-32)"
fi
md5sum "$ROOT/boot/zImage" "$ROOT/boot/librescoot-dbc.dtb" | sed 's/^/after:  /'
ls -l "$ROOT/boot/zImage"; ls "$ROOT/lib/modules"
sync
log "done: next boot runs $VER"
exit 0
__PAYLOAD__
HEAD
tar -C "$STAGE" -czf - . | base64 -w 76
} > "$OUT/dbc.sh"
chmod +x "$OUT/dbc.sh"
rm -rf "$STAGE"
ls -la "$OUT/dbc.sh"
