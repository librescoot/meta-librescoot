# Data-server service binding

The packaged unit listens on the stable board service address:

| Machine | Listener |
| --- | --- |
| `unu-mdb` | `192.168.7.1:8080` |
| `unu-dbc` | `192.168.7.2:8080` |
| `librescoot-dbc-rpi4` | `192.168.7.2:8080` |

The recipe defaults to loopback for other machine configurations. It produces
machine-specific packages because the installed unit contains the address.
The server binary's standalone default is unchanged.

The unit requires and starts after `librescoot-netconfig.service`. Network
configuration keeps the service address on loopback as well as USB, so the
listener remains available over the routed PPP backup when USB goes down.
The package depends on the matching network configuration. A failed bind
restarts the process; it never retries using a wildcard address.

The HTTP listener does not accept connections addressed to the board's WAN,
VPN or localhost address. Routed administrators must use the stable service IP,
or forward to it through SSH. Address binding is not an ingress-interface
filter: the MDB firewall still enforces the trusted `usb0`, `ppp0` and `wg*`
boundary, including WAN traffic addressed to the private service IP. The
service remains an unauthenticated read/write/delete endpoint for `/data`.

## Tests

Package rendering tests need Python 3 and standard shell tools:

```sh
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s recipes-core/data-server/tests -v
```

The real-server network test also requires a host data-server binary, root,
iproute2, iptables and IPv6 namespace support:

```sh
sudo env PYTHONDONTWRITEBYTECODE=1 DATA_SERVER_BINARY=/path/to/host/data-server \
    python3 recipes-core/data-server/tests/check-network.py
```

It uses temporary namespaces and directories, never a vehicle or host network
configuration. Both board addresses are exercised with real uploads/downloads
and deletion. It checks missing-address failure, absence of wildcard/WAN/VPN
listeners, routed management, PPP with USB down, PPP recreation without a
server restart, and the MDB firewall's WAN-to-private-address protection.
