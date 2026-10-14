#!/usr/bin/env python3
"""Exercise MDB DNS policy in temporary Linux network namespaces."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import time

CONFIG = Path(__file__).resolve().parents[1] / "files/dnsmasq-mdb.conf"
FIREWALL = CONFIG.parents[2] / "mdb-firewall/files"
NAMES = {role: f"ls-dns-{os.getpid()}-{role}" for role in ("mdb", "usb", "ppp", "vpn", "wan")}
CLIENT = r'''
import socket, struct, sys
host, mode = sys.argv[1:]
family = socket.AF_INET6 if ':' in host else socket.AF_INET
query = struct.pack('!6H', 1234, 0x100, 1, 0, 0, 0) + b'\x03mdb\x04test\0' + struct.pack('!2H', 1, 1)
def exact(s, n):
    data = b''
    while len(data) < n:
        chunk = s.recv(n - len(data))
        if not chunk:
            raise OSError('short DNS response')
        data += chunk
    return data
try:
    with socket.socket(family, socket.SOCK_STREAM if mode == 'tcp' else socket.SOCK_DGRAM) as s:
        s.settimeout(0.5)
        s.connect((host, 53))
        if mode == 'tcp':
            s.sendall(struct.pack('!H', len(query)) + query)
            reply = exact(s, struct.unpack('!H', exact(s, 2))[0])
        else:
            s.send(query)
            reply = s.recv(2048)
        ident, flags, questions, answers, _, _ = struct.unpack('!6H', reply[:12])
        assert ident == 1234 and flags & 0x8000 and flags & 15 == 0 and answers == 1
        assert reply.endswith(socket.inet_aton('192.0.2.42'))
except (OSError, AssertionError):
    sys.exit(1)
'''


def run(*args, check=True):
    return subprocess.run(list(map(str, args)), check=check, text=True, capture_output=True)


def ip(role, *args):
    return run("ip", "-n", NAMES[role], *args)


def inside(role, *args, check=True):
    return run("ip", "netns", "exec", NAMES[role], *args, check=check)


def link(role, name, mdb, peer, subnet, prefix=32):
    ip("mdb", "link", "add", name, "type", "veth", "peer", "name", "link0", "netns", NAMES[role])
    for side, iface, address, other in (("mdb", name, mdb, peer), (role, "link0", peer, mdb)):
        if prefix == 32:
            ip(side, "addr", "add", address + "/32", "peer", other + "/32", "dev", iface)
        else:
            ip(side, "addr", "add", f"{address}/{prefix}", "dev", iface)
        suffix = "1" if side == "mdb" else "2"
        ip(side, "-6", "addr", "add", f"fd00:88:{subnet}::{suffix}/64", "dev", iface, "nodad")
        ip(side, "link", "set", iface, "up")
    ip(role, "route", "add", "default", "via", mdb)
    ip(role, "-6", "route", "add", "default", "via", f"fd00:88:{subnet}::1")


def probe(role, host, allowed, label):
    for mode in ("udp", "tcp"):
        result = inside(role, "python3", "-c", CLIENT, host, mode, check=False)
        assert (result.returncode == 0) == allowed, (label, mode, result.stdout, result.stderr)
        print(f"PASS {label} {mode}", flush=True)


def main():
    binary = shutil.which(os.environ.get("DNSMASQ", "dnsmasq"))
    if os.geteuid() != 0 or not binary:
        raise SystemExit("Run as root with dnsmasq installed, or set DNSMASQ to its path.")
    for tool in ("ip", "iptables", "ip6tables", "iptables-restore", "ip6tables-restore"):
        if not shutil.which(tool):
            raise SystemExit(f"Missing {tool}")
    created = []
    daemon = None
    try:
        for role, name in NAMES.items():
            run("ip", "netns", "add", name)
            created.append(name)
            ip(role, "link", "set", "lo", "up")
        ip("mdb", "addr", "add", "192.168.7.1/32", "dev", "lo")
        ip("mdb", "-6", "addr", "add", "fd00:88::1/128", "dev", "lo", "nodad")
        link("usb", "usb0", "192.168.7.1", "192.168.7.2", "7", prefix=24)
        link("ppp", "ppp0", "192.168.8.1", "192.168.8.2", "8")
        link("vpn", "wg-test", "10.7.0.1", "10.9.0.2", "9")
        link("wan", "wwan0", "198.18.0.1", "198.18.0.2", "10")
        with tempfile.TemporaryFile(mode="w+") as log:
            def start(config):
                p = subprocess.Popen([
                    "ip", "netns", "exec", NAMES["mdb"], binary,
                    "--no-daemon", "--local-service", "--no-resolv", "--no-hosts",
                    "--address=/mdb.test/192.0.2.42", f"--conf-file={config}",
                ], stdout=log, stderr=log)
                for _ in range(30):
                    if p.poll() is not None:
                        log.seek(0)
                        raise AssertionError(log.read())
                    if inside("mdb", "python3", "-c", CLIENT, "127.0.0.1", "udp", check=False).returncode == 0:
                        return p
                    time.sleep(0.1)
                p.terminate()
                p.wait(timeout=5)
                raise AssertionError("dnsmasq did not become ready")

            daemon = start("/dev/null")
            probe("ppp", "192.168.7.1", False, "default subnet policy rejects /32 PPP peer")
            daemon.terminate()
            daemon.wait(timeout=5)
            daemon = start(CONFIG)
            probe("wan", "198.18.0.1", False, "DNS interface policy excludes WAN address")
            inside("mdb", "sh", FIREWALL / "librescoot-firewall", FIREWALL)
            for role in ("mdb", "usb", "ppp", "vpn"):
                for host in ("192.168.7.1", "fd00:88::1"):
                    probe(role, host, True, f"trusted {role} to {host}")
            for name in ("wwan0", "end0", "wlan0"):
                if name != "wwan0":
                    ip("mdb", "link", "set", "wwan0", "name", name)
                for host in ("192.168.7.1", "198.18.0.1", "fd00:88::1", "fd00:88:10::1"):
                    probe("wan", host, False, f"firewall excludes {name} to {host}")
                if name != "wwan0":
                    ip("mdb", "link", "set", name, "name", "wwan0")
            ip("mdb", "link", "del", "ppp0")
            link("ppp", "ppp0", "192.168.8.1", "192.168.8.2", "8")
            time.sleep(1)
            for host in ("192.168.7.1", "fd00:88::1"):
                probe("ppp", host, True, "PPP recreated without DNS restart")
            assert daemon.poll() is None
    finally:
        if daemon is not None and daemon.poll() is None:
            daemon.terminate()
            daemon.wait(timeout=5)
        for name in reversed(created):
            run("ip", "netns", "del", name, check=False)


if __name__ == "__main__":
    main()
