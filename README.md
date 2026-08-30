# Librescoot Yocto Layer

Part of the [Librescoot](https://librescoot.org/) open-source platform.

## Overview

`meta-librescoot` is the Yocto/OpenEmbedded layer for Librescoot system images.
It defines Librescoot machines, distros, image recipes, package recipes,
kernel and bootloader customizations, network configuration, and systemd units
for the main board (MDB) and dashboard computer (DBC).

The layer declares compatibility with the `wrynose` Yocto layer series in
`conf/layer.conf`.

## Capabilities

- Defines `unu-mdb` for the i.MX6UL-based main board, `unu-dbc` for the
  i.MX6DL-based dashboard, and `librescoot-dbc-rpi4` for the Raspberry Pi 4
  development target.
- Defines the `librescoot-mdb` and `librescoot-dbc` distros, with systemd,
  PAM, usrmerge, board-specific feature selection, and Europe/Berlin as the
  default timezone.
- Provides full and minimal bootstrap images for MDB and DBC targets.
- Packages Librescoot services and tools, dashboard applications, display
  supervision and boot animation, routing, networking, device support, and
  OTA-related configuration.
- Supplies `librescoot-go.bbclass` for Go module recipes that build and embed
  version information.

## Images and shipped components

The image recipes are:

| Image | Purpose | Major included components |
| --- | --- | --- |
| `librescoot-mdb-image` | Full main-board image | Vehicle, ECU, battery, Bluetooth, keycard, modem, power, update, settings, alarm, motion, and telemetry services; Valkey; networking and modem tooling; GPS; `lsc` |
| `librescoot-dbc-image` | Full dashboard image | `dbc-dispatcher`; ScootUI Qt and TUI packages; Valkey client/runtime; Valhalla and prime-server; boot animation; DBC networking; `lsc` |
| `librescoot-mdb-minimal-image` | MDB bootstrap image | USB networking, datastore, data-server, access/bootstrap support, and selected hardware and provisioning utilities |
| `librescoot-dbc-minimal-image` | DBC bootstrap image | DBC networking, data-server, remote access support, and zstd for provisioning payloads |

The machine configurations enable Mender U-Boot and SD-image features. Full
images additionally seed the matching `.mender` artifact into the persistent
data image for OTA use. The minimal images are bootstrap/provisioning images,
not substitutes for the full runtime images.

Recipes may fetch source from their declared upstream `SRC_URI` values during a
build. This layer does not vendor all third-party or sibling project source;
consult each recipe rather than assuming that an implementation is present in
this checkout.

## Operation and interfaces

Add the layer to a compatible Yocto build configuration, select a machine and
distro, then build an image with BitBake. The layer relies on the base machine,
distro, and class files it includes, so the build configuration must also supply
its required BSP and Yocto/OE layers.

```sh
# After initializing a compatible Yocto build environment and adding this layer
MACHINE=unu-mdb DISTRO=librescoot-mdb bitbake librescoot-mdb-image
MACHINE=unu-dbc DISTRO=librescoot-dbc bitbake librescoot-dbc-image

# Bootstrap images
MACHINE=unu-mdb DISTRO=librescoot-mdb bitbake librescoot-mdb-minimal-image
MACHINE=unu-dbc DISTRO=librescoot-dbc bitbake librescoot-dbc-minimal-image
```

Use `bitbake-layers show-layers` to confirm the active layer set before a
build. Deploy generated artifacts using the procedures appropriate for the
selected board and Mender workflow.

## Configuration

The supported machine and distro identifiers are defined in `conf/machine/` and
`conf/distro/`:

| Target | Machine | Distro |
| --- | --- | --- |
| Main board | `unu-mdb` | `librescoot-mdb` |
| Dashboard computer | `unu-dbc` | `librescoot-dbc` |
| Raspberry Pi 4 dashboard development target | `librescoot-dbc-rpi4` | `librescoot-dbc` |

The machine files define board-specific kernel device trees, bootloader and
Mender storage settings, generated SD-image types, and hardware features. The
image recipes select the installed packages. Keep `MACHINE`, `DISTRO`, and the
image recipe aligned; cross-combining board configuration and image intent is
not a supported deployment configuration.

## Build and test

There is no standalone unit-test harness in this layer. Validate recipe parsing
in the configured Yocto environment:

```sh
bitbake-layers show-layers
bitbake -p librescoot-mdb-image
bitbake -p librescoot-dbc-image
```

Then build the selected image with `bitbake` as shown above. Individual recipe
builds, package QA, and image generation are performed by BitBake and the
inherited classes; exact host tools and external layer revisions are determined
by the Yocto build environment, not by this layer alone.

## Deployment and runtime dependencies

Full images include systemd services, network configuration, and Mender support
as selected by their recipes. MDB runtime services depend on the board's vehicle
hardware and its configured network links. DBC runtime services depend on the
display stack, the MDB datastore link, and, for navigation, routing software and
map data. The Raspberry Pi 4 machine is a development target with its own
kernel, firmware, and display configuration.

The full image recipes enable root login with an empty password and Dropbear
SSH features. Treat produced images as development or controlled provisioning
artifacts until credentials and access controls have been set for the intended
deployment.

## Operational notes

Mender partition layout and bootloader settings are machine-specific and
load-bearing for OTA operation. Do not alter storage sizes, rootfs layout, or
boot configuration without rebuilding and validating the complete update and
recovery path for that machine. Minimal images intentionally contain only the
components needed to establish and complete provisioning; install the matching
full artifact before normal operation.

## License

This project is licensed under the [Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License](LICENSE).

Made with ❤️ by the Librescoot community
