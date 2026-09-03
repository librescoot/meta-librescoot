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

- `mktest.sh <label> <zImage> <dtb>`: one-shot test boot. The files go in as
  `/boot/zImage.test` and `/boot/librescoot-dbc.dtb.test`, the originals stay,
  and `bootcmd` is wrapped so that with `testboot=1` U-Boot clears the flag,
  saves the environment and boots the `.test` files once. A failed boot falls
  back to the originals on the next power cycle. Boot the DBC by hand after
  the round, ums-service powers it off.
- `rearm.sh`: set `testboot=1` again for the `.test` files already on the DBC.
- `promote.sh`: copy the `.test` files over the originals and unwrap `bootcmd`.
- `readout.sh`: kernel identity, test-boot environment, SPI, DRM, backlight,
  pad and IPU state, dmesg; stored in the `flashfree` hash of the MDB redis.
  Re-arms `testboot` if a wrapped `bootcmd` and `zImage.test` exist.
- `mkround.sh <label> <zImage> [dtb] [u-boot-dtb.imx]`: replaces the files in
  place (and writes U-Boot to eMMC sector 2) with first-run backups under
  `/data/flashfree/backup`. No fallback; U-Boot has no recovery path over USB.

The `.test` boot relies on `bootcmd` loading `/boot/${mender_kernel_name}` and
`/boot/${mender_dtb_name}`, which the mender integration does. The DBC journal
is volatile, so the dmesg of a boot that never showed anything can only be
read while that boot is still running; ums-service reaches an already running
DBC without rebooting it.
