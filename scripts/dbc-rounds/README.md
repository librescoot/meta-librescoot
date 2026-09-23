# DBC test rounds over ums-service

Scripts for iterating on the DBC kernel, device tree and U-Boot on a scooter
whose DBC is only reachable through the MDB's USB link. ums-service runs
`scripts/dbc.sh` from the USB drive on the DBC (two minute limit) and powers
the DBC off afterwards; output goes to `journalctl -u librescoot-ums` on the
MDB and `ums_log.txt` on the drive.

Delivery: put the MDB into drive mode (`redis-cli HSET usb mode ums; redis-cli
PUBLISH usb mode`), copy the generated `dbc.sh` to `scripts/` on the drive,
unmount, move the cable to the DBC. The first detach switches the MDB back to
normal mode and starts processing.

- `mktest.sh <label> <zImage> <dtb> [modules-dir]`: one-shot test boot. The files
  go in as `/boot/zImage.test` and `/boot/librescoot-dbc.dtb.test`, the originals
  stay, and `bootcmd` is wrapped so that with `testboot=1` U-Boot clears the flag,
  saves the environment and boots the `.test` files once. A failed boot falls
  back to the originals on the next power cycle, which still find their own
  modules. Boot the DBC by hand after the round, ums-service powers it off.
- `rearm.sh`: set `testboot=1` again for the `.test` files already on the DBC.
- `promote.sh`: install the `.test` kernel as `/boot/zImage-<release>` with
  `/boot/zImage` pointing at it, promote the DTB, and unwrap `bootcmd`.
- `readout.sh`: kernel identity, the kernel/modules pairing, test-boot
  environment, SPI, DRM, backlight, pad and IPU state, dmesg; stored in the
  `flashfree` hash of the MDB redis. Re-arms `testboot` if a wrapped `bootcmd`
  and `zImage.test` exist.
- `mkround.sh <label> <zImage> [dtb] [u-boot-dtb.imx] [modules-dir]`: replaces
  the files in place (and writes U-Boot to eMMC sector 2) with first-run backups
  under `/data/flashfree/backup`. No fallback; U-Boot has no recovery path over
  USB.

## Pass the module tree

Both kernel installers require the `lib/modules/<release>` directory that belongs
to the kernel, as the fifth argument to `mkround.sh` or the fourth to
`mktest.sh` (or `$MODULES`). It is installed under `/lib/modules/<release>`
before the kernel, additively: a release already on the board keeps its tree, so
the fallback boot is unaffected. About 3 MB, 1 MB compressed — it rides in the
payload with the kernel.

Without it a test kernel silently loses every driver that is not built in, which
looks like a driver regression rather than a missing module. Pass
`KERNEL_VERSION=<release>` instead only when the tree is knowingly not needed.

## Kernel shape in /boot

The image ships `/boot/zImage` as a symlink to `zImage-<release>`. The installers
keep that shape: they write the versioned file and point the symlink at it.
Replacing the symlink with a plain file leaves a slot whose kernel no module
tree tracks, which is how a board ends up booting a kernel that cannot load its
own drivers.

## Dry runs

The generated scripts and `promote.sh` take `ROOT=<dir>`, which prefixes every
filesystem path and skips the device and environment operations, so an install
can be exercised off-board:

    ROOT=/tmp/root bash round/scripts/dbc.sh

The `.test` boot relies on `bootcmd` loading `/boot/${mender_kernel_name}` and
`/boot/${mender_dtb_name}`, which the mender integration does. The DBC journal
is volatile, so the dmesg of a boot that never showed anything can only be
read while that boot is still running; ums-service reaches an already running
DBC without rebooting it.
