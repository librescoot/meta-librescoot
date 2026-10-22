#!/usr/bin/env python3
"""Test the actual data-server binary in isolated USB/PPP/WAN namespaces."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import time

from test_packaging import install_unit, listen_args

RECIPE_DIR = Path(__file__).resolve().parents[1]
FIREWALL = RECIPE_DIR.parents[1] / "recipes-connectivity/mdb-firewall/files"
CLIENT = r'''
import http.client, sys
host, method, path = sys.argv[1:]
try:
    c = http.client.HTTPConnection(host, 8080, timeout=0.6)
    c.request(method, path, body=b'network fixture' if method == 'PUT' else None)
    r = c.getresponse()
    data = r.read()
    assert r.status == (204 if method == 'DELETE' else 200), (r.status, data)
    if method == 'GET' and path == '/probe':
        assert data == b'network fixture', data
    assert r.getheader('Server', '').startswith('librescoot-data-server/'), r.getheaders()
    c.close()
except (OSError, AssertionError) as e:
    print(e, file=sys.stderr)
    sys.exit(1)
'''


def run(*args, check=True):
    return subprocess.run(list(map(str, args)), check=check, text=True, capture_output=True, timeout=15)


def check_board(binary, machine, own, other):
    names = {role: f"ls-data-{os.getpid()}-{role}" for role in ("board", "usb", "ppp", "wan", "vpn")}
    created = []
    process = None

    def ip(role, *args):
        return run("ip", "-n", names[role], *args)

    def inside(role, *args, check=True):
        return run("ip", "netns", "exec", names[role], *args, check=check)

    def link(role, interface, local, remote, prefix):
        ip("board", "link", "add", interface, "type", "veth", "peer", "name", "link0", "netns", names[role])
        for side, dev, address, peer in (("board", interface, local, remote), (role, "link0", remote, local)):
            args = ("addr", "add", f"{address}/{prefix}")
            if prefix == 32:
                args += ("peer", f"{peer}/32")
            ip(side, *args, "dev", dev)
            ip(side, "link", "set", dev, "up")

    def ppp():
        link("ppp", "ppp0", f"192.168.8.{own}", f"192.168.8.{other}", 32)
        ip("ppp", "route", "add", f"192.168.7.{own}/32", "via", f"192.168.8.{own}")

    def probe(role, host, allowed, label, method="GET", path="/"):
        result = inside(role, "python3", "-c", CLIENT, host, method, path, check=False)
        if (result.returncode == 0) != allowed:
            raise AssertionError((machine, label, result.stdout, result.stderr))
        print(f"PASS {machine}: {label}", flush=True)

    try:
        for role in names:
            run("ip", "netns", "add", names[role])
            created.append(names[role])
            ip(role, "link", "set", "lo", "up")
        link("wan", "wwan0", "198.18.0.2", "198.18.0.1", 30)
        ip("board", "-6", "addr", "add", "fd00:99::2/64", "dev", "wwan0", "nodad")
        ip("wan", "-6", "addr", "add", "fd00:99::1/64", "dev", "link0", "nodad")
        ip("wan", "route", "add", f"192.168.7.{own}/32", "via", "198.18.0.2")
        link("vpn", "wg-test", "10.7.0.4", "10.7.0.1", 24)
        ip("vpn", "route", "add", f"192.168.7.{own}/32", "via", "10.7.0.4")
        ppp()
        with tempfile.TemporaryDirectory(prefix="librescoot-data-test-") as tmp:
            root = Path(tmp)
            unit = install_unit(root, machine)
            args = listen_args(unit)[1:]
            data = root / "data"
            data.mkdir()
            # No service address yet: fail closed, never bind another address.
            result = inside("board", binary, *args, "-data", data, check=False)
            if result.returncode == 0 or "cannot assign requested address" not in result.stderr:
                raise AssertionError(("missing-address bind did not fail", result.stdout, result.stderr))
            probe("wan", "198.18.0.2", False, "missing service address has no wildcard fallback")
            ip("board", "addr", "add", f"192.168.7.{own}/32", "dev", "lo")
            link("usb", "usb0", f"192.168.7.{own}", f"192.168.7.{other}", 24)
            with tempfile.TemporaryFile(mode="w+") as log:
                process = subprocess.Popen(["ip", "netns", "exec", names["board"], str(binary), *args,
                                            "-data", str(data)], stdout=log, stderr=log)
                for _ in range(60):
                    if process.poll() is not None:
                        log.seek(0)
                        raise AssertionError(log.read())
                    if inside("board", "python3", "-c", CLIENT, f"192.168.7.{own}", "GET", "/", check=False).returncode == 0:
                        break
                    time.sleep(0.05)
                else:
                    raise AssertionError("server did not start")
                probe("usb", f"192.168.7.{own}", True, "USB upload", "PUT", "/probe")
                probe("usb", f"192.168.7.{own}", True, "USB download", path="/probe")
                probe("ppp", f"192.168.7.{own}", True, "PPP reaches stable service address")
                probe("vpn", f"192.168.7.{own}", True, "routed management reaches service address")
                probe("wan", "198.18.0.2", False, "no listener on WAN IPv4 address")
                probe("wan", "fd00:99::2", False, "no listener on WAN IPv6 address")
                probe("vpn", "10.7.0.4", False, "no listener on VPN address")
                probe("board", "127.0.0.1", False, "listener is not wildcard or localhost")
                # Address binding is not an ingress-interface firewall.
                if machine == "unu-mdb":
                    probe("wan", f"192.168.7.{own}", True, "baseline proves private IP still needs ingress firewall")
                    inside("board", "sh", FIREWALL / "librescoot-firewall", FIREWALL)
                    probe("wan", f"192.168.7.{own}", False, "MDB firewall blocks WAN-to-private-IP ingress")
                    probe("vpn", f"192.168.7.{own}", True, "MDB firewall preserves trusted VPN ingress")
                ip("board", "link", "set", "usb0", "down")
                probe("ppp", f"192.168.7.{own}", True, "PPP works with USB down")
                probe("ppp", f"192.168.7.{own}", True, "PPP downloads uploaded data", path="/probe")
                ip("board", "link", "del", "ppp0")
                ppp()
                probe("ppp", f"192.168.7.{own}", True, "PPP recreated without restarting listener")
                probe("ppp", f"192.168.7.{own}", True, "PPP delete", "DELETE", "/probe")
                if (data / "probe").exists():
                    raise AssertionError("delete did not remove test upload")
                if process.poll() is not None:
                    raise AssertionError("server exited during interface changes")
                process.terminate()
                process.wait(timeout=5)
                process = None
    finally:
        if process is not None:
            process.terminate()
            process.wait(timeout=5)
        for name in reversed(created):
            run("ip", "netns", "del", name, check=False)


def main():
    if os.geteuid() != 0:
        raise SystemExit("Run as root; network changes are confined to temporary namespaces.")
    binary = Path(os.environ.get("DATA_SERVER_BINARY", "")).resolve()
    if not binary.is_file() or not os.access(binary, os.X_OK):
        raise SystemExit("Set DATA_SERVER_BINARY to a host data-server executable.")
    for tool in ("ip", "python3", "iptables", "ip6tables", "iptables-restore", "ip6tables-restore"):
        if not shutil.which(tool):
            raise SystemExit(f"Missing {tool}")
    check_board(binary, "unu-mdb", 1, 2)
    check_board(binary, "unu-dbc", 2, 1)


if __name__ == "__main__":
    main()
