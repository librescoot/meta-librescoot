# MDB firewall

`mdb-netconfig` depends on this package, including on bootstrap images. The
firewall owns the IPv4 and IPv6 filter tables. It uses iptables/ip6tables on the
MDB kernel. NAT, routes and forwarding sysctls remain owned by network setup.

## Trust boundary

- `lo`, `usb0`, `ppp0` and WireGuard interfaces named `wg*` may reach local
  services. USB and PPP are trusted physical links; WireGuard peers are trusted
  administrators. Services reachable through them may provide full vehicle and
  root access. A connected service laptop must therefore be trusted too.
- USB, PPP and WireGuard may initiate forwarded connections. Established/related
  replies are accepted. Unsolicited traffic from every other interface is denied,
  regardless of its source or destination IP, including local USB/VPN addresses.
- Outbound connections are allowed. This package does not configure NAT or
  enable forwarding.
- DHCP client replies and ICMP errors/discovery/echo requests are accepted.
  DHCP service on USB is covered by the trusted-interface allowance.

WireGuard transport is client-initiated: the scooter must initiate toward its
configured peer. Configure `PersistentKeepalive` below the conntrack UDP timeout
(for example 25 seconds with a 30-second timeout) when remote access must remain
available during idle periods. Unsolicited inbound WireGuard handshakes are not
allowed by these rules. A server-style deployment needs an explicit UDP listener
exception; opening the HTTP or Redis port is not a substitute.

A socket bound to a private or WireGuard IP is not restricted to that ingress
interface. The firewall enforces the interface boundary. Application credentials
remain useful defense in depth; send them over WireGuard or TLS, not plaintext
HTTP on an untrusted network.

## Startup and reload

`librescoot-firewall.service` runs before `network-pre.target`. NetworkManager,
systemd-networkd, PPP and network setup require successful firewall activation.
The loader sets default-deny policies before validating and installing both rule
files; a failure is reported to systemd, not ignored. No stop action flushes the
firewall. Reloads replace the managed tables rather than append rules.

Rules live in `/etc/librescoot/firewall/`. Apply edits with
`systemctl reload librescoot-firewall`. They replace the complete managed tables,
so put site-specific exceptions in these files rather than separate append-only
startup scripts. Conntrack is retained during reloads, including established
management connections.

When deploying remotely, save `iptables-save` and `ip6tables-save`, arrange a
local timed restore before applying changes, and verify **new** SSH connections
before cancelling rollback. Keep serial recovery available for the first
hardware validation. Do not treat an existing SSH session as proof that new
connections are permitted.

## Regression test

On a Linux development host with Python 3, iproute2, iptables, wireguard-tools,
and IPv6/WireGuard kernel support:

```
sudo python3 recipes-connectivity/mdb-firewall/tests/test-firewall.py
```

The test creates and cleans up private network namespaces. It exercises private
and VPN destination addresses arriving through WAN, IPv4/IPv6 forwarding,
internal/VPN access, preservation of outbound NAT and replies, PPP matching, alternative
uplinks, reload idempotence and invalid ruleset rejection. It also establishes a
real client-initiated WireGuard tunnel and checks encrypted management ingress.
TCP servers are harmless stand-ins; the test never connects to a vehicle.
