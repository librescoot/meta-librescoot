#!/usr/bin/env python3
"""Run as root on a Linux host with iproute2, iptables and IPv6 support."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import time

FILES = Path(__file__).resolve().parents[1] / "files"
NAMES = {role: f"ls-fw-{os.getpid()}-{role}" for role in ("mdb", "wan", "dbc", "vpn")}
PROCESSES = []
SERVER = r'''
import socket, threading, time

def serve(port):
    s = socket.socket(socket.AF_INET6)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    s.setsockopt(socket.IPPROTO_IPV6, socket.IPV6_V6ONLY, 0)
    s.bind(('::', port))
    s.listen()
    while True:
        c, address = s.accept()
        with c:
            c.sendall(address[0].encode())

def udp(port):
    s = socket.socket(socket.AF_INET6, socket.SOCK_DGRAM)
    s.setsockopt(socket.IPPROTO_IPV6, socket.IPV6_V6ONLY, 0)
    s.bind(('::', port))
    while True:
        data, address = s.recvfrom(128)
        s.sendto(data, address)

for port in (22, 53, 111, 6379, 8080, 8090):
    threading.Thread(target=serve, args=(port,), daemon=True).start()
for port in (68, 111, 546):
    threading.Thread(target=udp, args=(port,), daemon=True).start()
while True:
    time.sleep(1)
'''
PROBE = r'''
import socket, sys
try:
    with socket.create_connection((sys.argv[1], int(sys.argv[2])), timeout=0.6) as s:
        print(s.recv(128).decode())
except OSError:
    sys.exit(1)
'''


def run(*args, check=True):
    return subprocess.run(args, check=check, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)


def ip(role, *args):
    return run("ip", "-n", NAMES[role], *args)


def inside(role, *args, check=True):
    return run("ip", "netns", "exec", NAMES[role], *map(str, args), check=check)


def probe(role, address, port, allowed, label, source=None):
    result = inside(role, "python3", "-c", PROBE, address, port, check=False)
    assert (result.returncode == 0) == allowed, (label, result.stdout, result.stderr)
    if source:
        assert result.stdout.strip().split("::ffff:")[-1] == source, result.stdout
    print(f"PASS {label}", flush=True)


def rules():
    return [inside("mdb", *args).stdout for args in (
        ("iptables", "-S"), ("iptables", "-t", "nat", "-S"), ("ip6tables", "-S"))]


def main():
    if os.geteuid() != 0:
        raise SystemExit("Run with sudo; all network changes are confined to temporary namespaces.")
    for name in NAMES.values():
        run("ip", "netns", "add", name)
    for role in NAMES:
        ip(role, "link", "set", "lo", "up")
    for peer, interface, mdb4, peer4, mdb6, peer6 in (
        ("wan", "wwan0", "198.18.0.2/30", "198.18.0.1/30", "fd00:77:0::2/64", "fd00:77:0::1/64"),
        ("dbc", "usb0", "192.168.7.1/24", "192.168.7.2/24", "fd00:77:7::1/64", "fd00:77:7::2/64"),
        ("vpn", "wg0", "10.7.0.4/16", "10.7.0.1/16", "fd00:77:10::4/64", "fd00:77:10::1/64"),
    ):
        ip("mdb", "link", "add", interface, "type", "veth", "peer", "name", "link0", "netns", NAMES[peer])
        ip("mdb", "addr", "add", mdb4, "dev", interface)
        ip("mdb", "-6", "addr", "add", mdb6, "dev", interface, "nodad")
        ip(peer, "addr", "add", peer4, "dev", "link0")
        ip(peer, "-6", "addr", "add", peer6, "dev", "link0", "nodad")
        ip("mdb", "link", "set", interface, "up")
        ip(peer, "link", "set", "link0", "up")
        ip(peer, "route", "add", "default", "via", mdb4.split("/")[0])
        ip(peer, "-6", "route", "add", "default", "via", mdb6.split("/")[0])
    ip("mdb", "route", "add", "default", "via", "198.18.0.1")
    ip("mdb", "-6", "route", "add", "default", "via", "fd00:77:0::1")
    ip("mdb", "addr", "add", "192.168.7.1/32", "dev", "lo")
    inside("mdb", "sysctl", "-qw", "net.ipv4.ip_forward=1", "net.ipv6.conf.all.forwarding=1",
           "net.ipv4.conf.all.rp_filter=0", "net.ipv4.conf.wwan0.rp_filter=2")
    for role in ("mdb", "dbc", "wan"):
        PROCESSES.append(subprocess.Popen(["ip", "netns", "exec", NAMES[role], "python3", "-c", SERVER]))
    time.sleep(1)
    probe("wan", "192.168.7.1", 8090, True, "baseline private-address listener reachable")
    probe("wan", "10.7.0.4", 8090, True, "baseline VPN-address listener reachable from WAN")
    probe("wan", "192.168.7.2", 6379, True, "baseline forwarding reachable")
    inside("mdb", "sysctl", "-qw", "net.ipv4.ip_forward=0")
    ip("mdb", "link", "set", "usb0", "down")
    probe("wan", "192.168.7.1", 8090, True, "baseline exposure with USB down and forwarding off")
    ip("mdb", "link", "set", "usb0", "up")
    ip("mdb", "-6", "addr", "replace", "fd00:77:7::1/64", "dev", "usb0", "nodad")
    ip("dbc", "-6", "addr", "replace", "fd00:77:7::2/64", "dev", "link0", "nodad")
    ip("dbc", "-6", "route", "replace", "default", "via", "fd00:77:7::1")
    inside("mdb", "sysctl", "-qw", "net.ipv4.ip_forward=1")
    for uplink in ("wwan0", "eth0"):
        inside("mdb", "iptables", "-t", "nat", "-A", "POSTROUTING", "-o", uplink, "-j", "MASQUERADE")
    nat_before = inside("mdb", "iptables", "-t", "nat", "-S").stdout
    inside("mdb", "sh", FILES / "librescoot-firewall", FILES)
    assert inside("mdb", "iptables", "-t", "nat", "-S").stdout == nat_before, "NAT changed"

    for address in ("192.168.7.1", "10.7.0.4", "198.18.0.2", "fd00:77:7::1", "fd00:77:10::4"):
        for port in (22, 53, 111, 6379, 8080, 8090):
            probe("wan", address, port, False, f"WAN denied {address}:{port}")
    for address in ("192.168.7.2", "fd00:77:7::2"):
        probe("wan", address, 6379, False, f"WAN forwarding denied {address}")
        probe("vpn", address, 6379, True, f"VPN forwarding allowed {address}")
    for role in ("dbc", "vpn"):
        for address in ("192.168.7.1", "fd00:77:7::1"):
            for port in (22, 6379, 8080, 8090):
                probe(role, address, port, True, f"{role} access allowed {address}:{port}")
    probe("dbc", "198.18.0.1", 8080, True, "DBC outbound NAT and replies", source="198.18.0.2")
    probe("dbc", "fd00:77:0::1", 8080, True, "DBC outbound IPv6 and replies")
    probe("mdb", "198.18.0.1", 8080, True, "MDB outbound and replies")
    inside("wan", "ping", "-c", "1", "-W", "2", "198.18.0.2")
    inside("wan", "ping", "-6", "-c", "1", "-W", "2", "fd00:77:0::2")
    print("PASS IPv4/IPv6 diagnostic ICMP", flush=True)
    udp_probe = '''
import socket, sys
address, port, source = sys.argv[1:]
s = socket.socket(socket.AF_INET6 if ':' in address else socket.AF_INET, socket.SOCK_DGRAM)
s.settimeout(0.6)
s.bind(('::' if ':' in address else '0.0.0.0', int(source)))
s.sendto(b'probe', (address, int(port)))
try:
    assert s.recv(128) == b'probe'
except OSError:
    sys.exit(1)
'''
    for address, port, source, allowed in (
        ("198.18.0.2", 68, 67, True), ("fd00:77:0::2", 546, 547, True),
        ("198.18.0.2", 111, 55555, False), ("fd00:77:0::2", 111, 55555, False),
    ):
        result = inside("wan", "python3", "-c", udp_probe, address, port, source, check=False)
        assert (result.returncode == 0) == allowed, (address, port, result.stderr)
        print(f"PASS UDP {address}:{port} {'allowed' if allowed else 'denied'}", flush=True)

    ip("mdb", "link", "set", "usb0", "name", "ppp0")
    probe("dbc", "192.168.7.1", 6379, True, "PPP ingress allowed")
    probe("dbc", "198.18.0.1", 8080, True, "PPP outbound and replies")
    probe("wan", "192.168.7.2", 6379, False, "WAN to PPP denied")
    ip("mdb", "link", "set", "ppp0", "name", "usb0")
    for uplink in ("wlan0", "end0", "eth0"):
        ip("mdb", "link", "set", "wwan0", "name", uplink)
        probe("wan", "192.168.7.1", 8090, False, f"{uplink} ingress denied")
        probe("dbc", "198.18.0.1", 8080, True, f"{uplink} outbound works",
              source="198.18.0.2" if uplink == "eth0" else "192.168.7.2")
        ip("mdb", "link", "set", uplink, "name", "wwan0")

    before = rules()
    inside("mdb", "sh", FILES / "librescoot-firewall", FILES)
    assert before == rules(), "Reload changed or duplicated rules"
    print("PASS idempotent reload", flush=True)
    with tempfile.TemporaryDirectory() as directory:
        shutil.copy(FILES / "ipv4.rules", directory)
        Path(directory, "ipv6.rules").write_text("invalid ruleset\n")
        result = inside("mdb", "sh", FILES / "librescoot-firewall", directory, check=False)
        assert result.returncode != 0, "Invalid ruleset accepted"
        assert before == rules(), "Failed validation altered active rules"
        probe("wan", "192.168.7.1", 8090, False, "failed reload stays protected")
    # A fresh client-initiated handshake must work without an inbound UDP exception.
    ip("mdb", "link", "del", "wg0")
    ip("mdb", "link", "add", "wg0", "type", "wireguard")
    ip("wan", "link", "add", "tunnel0", "type", "wireguard")
    with tempfile.TemporaryDirectory() as directory:
        keys = {}
        for role in ("mdb", "wan"):
            private = run("wg", "genkey").stdout
            public = subprocess.run(["wg", "pubkey"], input=private, text=True,
                                    check=True, stdout=subprocess.PIPE).stdout.strip()
            keyfile = Path(directory, role)
            keyfile.write_text(private)
            keyfile.chmod(0o600)
            keys[role] = (keyfile, public)
        inside("wan", "wg", "set", "tunnel0", "private-key", keys["wan"][0], "listen-port", "51820",
               "peer", keys["mdb"][1], "allowed-ips", "10.7.0.4/32")
        inside("mdb", "wg", "set", "wg0", "private-key", keys["mdb"][0], "peer", keys["wan"][1],
               "allowed-ips", "10.7.0.1/32", "endpoint", "198.18.0.1:51820", "persistent-keepalive", "5")
        ip("mdb", "addr", "add", "10.7.0.4/16", "dev", "wg0")
        ip("wan", "addr", "add", "10.7.0.1/32", "dev", "tunnel0")
        ip("wan", "link", "set", "tunnel0", "up")
        ip("mdb", "link", "set", "wg0", "up")
        ip("wan", "route", "add", "10.7.0.4/32", "dev", "tunnel0")
        inside("mdb", "ping", "-c", "2", "-W", "3", "10.7.0.1")
        probe("wan", "10.7.0.4", 8090, True, "real WireGuard handshake and encrypted ingress")
        probe("wan", "192.168.7.1", 8090, False, "unencrypted WAN ingress remains denied")
    with tempfile.TemporaryDirectory() as directory:
        shutil.copy(FILES / "ipv4.rules", directory)
        Path(directory, "ipv6.rules").write_text("invalid ruleset\n")
        for tool in ("iptables", "ip6tables"):
            inside("mdb", tool, "-F")
            inside("mdb", tool, "-P", "INPUT", "ACCEPT")
            inside("mdb", tool, "-P", "FORWARD", "ACCEPT")
        result = inside("mdb", "sh", FILES / "librescoot-firewall", directory, check=False)
        assert result.returncode != 0, "Invalid initial ruleset accepted"
        for tool in ("iptables", "ip6tables"):
            policies = inside("mdb", tool, "-S").stdout
            assert "-P INPUT DROP" in policies and "-P FORWARD DROP" in policies
        probe("wan", "192.168.7.1", 8090, False, "failed initial load is default-deny")
    print("ALL CHECKS PASSED", flush=True)


try:
    main()
finally:
    for process in PROCESSES:
        process.terminate()
    for process in PROCESSES:
        process.wait()
    for name in NAMES.values():
        run("ip", "netns", "del", name, check=False)
