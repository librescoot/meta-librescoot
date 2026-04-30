
# meta-librescoot

Yocto BSP layer for [Librescoot](https://github.com/librescoot), providing machine configurations, distro settings, and service recipes for iMX6-based electric scooter computers. Compatible with **Yocto Scarthgap**.

Part of the [Librescoot](https://librescoot.org/) open-source platform.

## What This Layer Provides

### Machine Configurations

| Machine | Hardware | Role |
|---------|----------|------|
| `unu-mdb` | NXP i.MX6UL (ARMv7) | Main Dashboard Board — vehicle controller |
| `unu-dbc` | NXP i.MX6DL (ARMv7) | Dashboard Computer — touchscreen display |
| `librescoot-dbc-rpi4` | Raspberry Pi 4 (ARM64) | Development/testing target for DBC |

### Images

- **`librescoot-mdb-image`** — Vehicle controller rootfs with vehicle-service, ecu-service, battery-service, bluetooth-service, modem-service, and supporting infrastructure (Redis, NetworkManager, ModemManager).
- **`librescoot-dbc-image`** — Touchscreen dashboard rootfs with Flutter UI (scootui), Valhalla routing engine, Plymouth boot animation, and display management services.

Both images use **Mender** for A/B OTA updates.

### Distros

- `librescoot-mdb` — Cellular connectivity stack (libmbim, libqmi, ModemManager). No display.
- `librescoot-dbc` — Flutter SDK 3.32.5, display stack. No cellular.

Both are based on `fsl-imx-base` with systemd, PAM, and usrmerge enabled.

### Recipe Categories

| Directory | Contents |
|-----------|----------|
| `recipes-core` | Librescoot Go services, systemd units, Plymouth themes |
| `recipes-graphics` | Flutter engine, DRM/FBDEV backends, Wayland protocols |
| `recipes-connectivity` | NetworkManager configs, WiFi, cellular modem support |
| `recipes-devtools` | Go 1.25.7 toolchain (cross-compiler + runtime) |
| `recipes-kernel` | Linux kernel patches, device trees, LED kernel module |
| `recipes-navigation` | Valhalla routing engine, GPS utilities, prime-server |
| `recipes-bsp` | U-Boot patches, boot assets |
| `recipes-extended` | Redis, radio-gaga telemetry, fake-hwclock |
| `recipes-mender` | Mender OTA client configuration |
| `recipes-base` | ioctl utilities, NXP NFC library |
| `recipes-support` | chrony (NTP), vim, xdelta3 |
| `recipes-fsl` | Image definitions (MDB + DBC) |

### Custom Classes

- **`librescoot-go.bbclass`** — Injects version from `git describe --tags` into Go service binaries at build time.

## Building

Use the [librescoot](https://github.com/librescoot/librescoot) build harness:

```bash
git clone https://github.com/librescoot/librescoot.git
cd librescoot
./build.sh mdb    # Build MDB image
./build.sh dbc    # Build DBC image
```

The build harness uses Docker to set up the Yocto environment, clones this layer (and other dependencies), and produces Mender-compatible images.

### Output

Build artifacts are in `yocto/build/tmp/deploy/images/<machine>/`:

- `*.mender` — OTA update artifact
- `*.sdimg` — Full SD card image (for initial provisioning)

### Key local.conf Settings

The build harness configures these automatically, but for manual builds:

```
MACHINE = "unu-mdb"                    # or "unu-dbc", "librescoot-dbc-rpi4"
DISTRO = "librescoot-mdb"             # or "librescoot-dbc"
ACCEPT_FSL_EULA = "1"
```

## Layer Dependencies

- [poky](https://git.yoctoproject.org/poky) (scarthgap)
- [meta-openembedded](https://github.com/openembedded/meta-openembedded) (meta-oe, meta-python, meta-networking, meta-multimedia)
- [meta-freescale](https://github.com/Freescale/meta-freescale)
- [meta-flutter](https://github.com/passy/meta-flutter) (DBC only)
- [meta-mender](https://github.com/mendersoftware/meta-mender)
- [meta-lts-mixins](https://git.yoctoproject.org/meta-lts-mixins) (scarthgap/go branch, for Go toolchain)

## License

This project is dual-licensed. The source code is available under the
[Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License][cc-by-nc-sa].
The maintainers reserve the right to grant separate licenses for commercial distribution; please contact the maintainers to discuss commercial licensing.

[![CC BY-NC-SA 4.0][cc-by-nc-sa-image]][cc-by-nc-sa]

[cc-by-nc-sa]: http://creativecommons.org/licenses/by-nc-sa/4.0/
[cc-by-nc-sa-image]: https://licensebuttons.net/l/by-nc-sa/4.0/88x31.png
