# MDB network configuration

## DNS service

`/etc/dnsmasq.d/librescoot.conf` serves DNS on `lo`, `usb0`, `ppp0` and
`wg*`, matching the MDB firewall's trusted interfaces. PPP and routed VPN
clients need not belong to a local interface's subnet. Explicit interface
selectors supersede dnsmasq's default `--local-service` subnet restriction.

Dnsmasq uses its default wildcard binding so interfaces and addresses can
appear after startup. The MDB firewall remains the ingress boundary, including
for WAN packets addressed to a trusted interface's local IP. Upstream resolver
selection is unchanged; systemd-networkd owns USB DHCP.

See the [dnsmasq manual](https://thekelleys.org.uk/dnsmasq/docs/dnsmasq-man.html)
for `--interface`, `--local-service` and the default binding behavior.

Run the isolated DNS regression test on a Linux host with dnsmasq, Python 3,
iproute2 and iptables (IPv4/IPv6):

```sh
sudo python3 tests/test-dns.py
```

Set `DNSMASQ` to use an alternative dnsmasq binary. The test reproduces the
subnet-policy rejection, verifies UDP/TCP over IPv4/IPv6, recreates the PPP
interface without restarting dnsmasq, and checks WAN denial with the packaged
firewall. It uses temporary network namespaces and no external DNS servers.
