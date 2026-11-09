# ppp-link

PPP backup link over UART between the MDB and the DBC, plus the routing that
keeps the peer reachable over whichever link is alive.

## What it ships

- `ppp-link.service` and the `uart-link` peers file. Enabled on the DBC;
  explicitly disabled on the MDB, where vehicle-service starts it alongside
  dashboard power.
- `ip-up-backup-routes` / `ip-down-backup-routes`, installed into pppd's hook
  directories. On PPP up they add a route to the peer's service address with
  metric 200, and on the DBC also a metric-200 default route.
- `librescoot-link-route-monitor`. It keeps the route to the peer's service
  address on the USB link with metric 50 while the peer answers there. Enabled
  at boot on the DBC; on the MDB it rides on `librescoot-dbc-link.service`.
- `librescoot-dbc-link.service`, the unit that owns usb0's admin state.
  Disabled everywhere at boot; vehicle-service starts and stops it.

## Why the route monitor exists

Both boards keep a stable service address (192.168.7.1 / 192.168.7.2) that
survives losing the USB link, and each board knows its peer's link-specific
address (192.168.9.1 / 192.168.9.2) only while that link is up.

Metric preference alone cannot choose between the two links. A route whose
device has lost carrier stays in the routing table and still wins against a
higher-metric alternative, so the kernel keeps sending to the dead link and the
PPP backup never takes over. Only withdrawing the USB route moves the peer, and
that is what the monitor does: it probes the peer on the link-specific address
and installs or removes the metric-50 route. It does not probe without carrier,
because such a probe would leave through the default route, on the MDB the
mobile uplink.

## Who owns the link

`librescoot-dbc-link.service` owns usb0's admin state and is never enabled at
boot. vehicle-service starts it whenever the DBC link should be up (an open
usb0 gate, or dashboard power when the gate is closed) and stops it otherwise:

```
systemctl start librescoot-dbc-link.service   # ip link set usb0 up
systemctl stop  librescoot-dbc-link.service   # ip link set usb0 down
```

The route monitor is `PartOf=` that unit and is pulled in by its `Wants=`, so
it runs exactly while the link is supposed to be up. It must not follow
`ppp-link.service`: an open gate keeps usb0 up while the DBC is off, and in
that state ppp is down while the monitor still has to withdraw a stale route.
The DBC has no vehicle-service to switch the link, so there the monitor stays
enabled at boot.

`librescoot-usb0-failsafe` (mdb-netconfig) starts the link unit when
vehicle-service never recorded a gate decision, and falls back to raising usb0
directly on images that carry no ppp-link.

## Regression tests

```
recipes-connectivity/ppp-link/tests/test-link-routes.sh
python3 recipes-connectivity/ppp-link/tests/test-link-routes-kernel.py
recipes-connectivity/ppp-link/tests/test-service-packaging.sh
recipes-connectivity/mdb-netconfig/tests/test-usb0-failsafe.sh
```

The first two check the routing contract: the shell test drives both hook
scripts and the monitor against stubbed `ip` and `ping` (including that no
probe runs without carrier), and the kernel test runs in a temporary network
namespace to assert against the running kernel that a carrier-less route is
still preferred, and that withdrawing it hands the peer to the backup. The
kernel test needs iproute2 and either unprivileged user namespaces or root; it
cleans up after itself and never touches the host routing table.
