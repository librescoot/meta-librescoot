#!/usr/bin/env python3
"""Pin the routing behaviour the USB/PPP link selection depends on.

Run on a Linux host with iproute2, either unprivileged (user namespaces) or as
root. Everything happens in a temporary network namespace; the host routing
table is never touched.

Why this exists: when both candidate routes for the peer's service address are
in the table, the kernel keeps using the one whose device lost carrier. It does
not fall back to the higher-metric alternative, so removing the USB route is
the only thing that hands the peer to the PPP backup. The monitor's probe is
therefore load bearing, and this test asserts the property against the running
kernel instead of the source comments. It then drives the real monitor script
once per scenario with a stub probe and checks the route table it leaves.
"""
import os
import subprocess
import sys
import tempfile
from pathlib import Path

FILES = Path(__file__).resolve().parents[1] / "files"
MONITOR = str(FILES / "librescoot-link-route-monitor")

IF_USB = "ls-usb0"
IF_USB_PEER = "ls-usb-peer"
IF_BAK = "ls-bak0"
IF_BAK_PEER = "ls-bak-peer"

USB_PEER = "192.168.9.2"
SERVICE_PEER = "192.168.7.2"
BAK_GW = "192.168.8.2"
USB_ROUTE = f"{SERVICE_PEER}/32 via {USB_PEER} dev {IF_USB} metric 50"


def run(*args, check=True, env=None):
    return subprocess.run(args, check=check, text=True, env=env,
                          stdout=subprocess.PIPE, stderr=subprocess.STDOUT)


def ip(*args, check=True):
    return run("ip", *args, check=check)


def route_destination():
    """Device the kernel would use to reach the peer's service address."""
    result = ip("route", "get", SERVICE_PEER, check=False)
    words = result.stdout.split()
    return words[words.index("dev") + 1] if "dev" in words else None


def usb_route_line():
    for line in ip("-o", "route", "show", f"{SERVICE_PEER}/32").stdout.splitlines():
        if f"dev {IF_USB} metric 50" in line:
            return line
    return ""


def expect(condition, label):
    if not condition:
        raise SystemExit(f"FAIL {label}")
    print(f"PASS {label}", flush=True)


def body():
    with tempfile.TemporaryDirectory() as workdir:
        work = Path(workdir)
        stub = work / "bin"
        stub.mkdir()
        (stub / "ping").write_text(
            '#!/bin/sh\nprintf "%s\\n" "$*" >> "$PING_LOG"\n[ "$PING_RESULT" = success ]\n')
        (stub / "ping").chmod(0o755)

        carrier = work / "carrier"
        peer_config = work / "uart-link"
        # MDB role: its peer is the DBC.
        peer_config.write_text("192.168.8.1:192.168.8.2\n")

        def monitor(ping_result, carrier_value):
            (work / "ping.log").write_text("")
            carrier.write_text(f"{carrier_value}\n")
            env = dict(os.environ)
            env.update(
                PATH=f"{stub}:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
                CARRIER_PATH=str(carrier),
                PEER_CONFIG=str(peer_config),
                USB_IF=IF_USB,
                LINK_ROUTE_ONCE="1",
                PING_LOG=str(work / "ping.log"),
                PING_RESULT=ping_result,
            )
            run(MONITOR, env=env)
            return (work / "ping.log").read_text()

        for name, peer in ((IF_USB, IF_USB_PEER), (IF_BAK, IF_BAK_PEER)):
            ip("link", "add", name, "type", "veth", "peer", "name", peer)
            ip("link", "set", name, "up")
            ip("link", "set", peer, "up")

        ip("addr", "add", "192.168.9.1/24", "dev", IF_USB)
        ip("addr", "add", f"{USB_PEER}/24", "dev", IF_USB_PEER)
        ip("addr", "add", "192.168.8.1/24", "dev", IF_BAK)
        ip("addr", "add", f"{BAK_GW}/24", "dev", IF_BAK_PEER)

        # The monitor's route over USB and the one the ip-up hook installs over
        # the PPP backup, both in the table at the same time.
        ip("route", "replace", *USB_ROUTE.split())
        ip("route", "replace", f"{SERVICE_PEER}/32", "via", BAK_GW, "dev", IF_BAK, "metric", "200")
        expect(route_destination() == IF_USB, "prefers the USB route while both links are up")

        # Carrier loss on usb0 with the interface still administratively up,
        # which is what a powered-off or wedged DBC looks like.
        ip("link", "set", IF_USB_PEER, "down")
        expect("linkdown" in usb_route_line(),
               "keeps the USB route in the table with a linkdown flag")
        expect(route_destination() == IF_USB,
               "does not fall back to the PPP route on carrier loss alone")

        # The fix: no carrier means no probe at all, and the route goes away.
        expect(monitor("success", 0) == "",
               "monitor does not probe without carrier")
        expect(route_destination() == IF_BAK,
               "withdrawing the USB route hands the peer to the PPP backup")

        ip("link", "set", IF_USB_PEER, "up")
        monitor("success", 1)
        expect(route_destination() == IF_USB,
               "restores the USB route when the peer answers again")

        monitor("failure", 1)
        expect(route_destination() == IF_BAK,
               "withdraws the USB route when the peer stops answering")

        ip("link", "set", IF_BAK_PEER, "down")
        expect(route_destination() == IF_BAK,
               "keeps using the backup while it is the only candidate")


def main():
    if "--in-namespace" not in sys.argv:
        if os.geteuid() == 0:
            prefix = ["unshare", "-n"]
        else:
            if run("unshare", "-rn", "true", check=False).returncode != 0:
                raise SystemExit("Unprivileged user namespaces are unavailable; run with sudo.")
            prefix = ["unshare", "-rn"]
        return subprocess.run(prefix + [sys.executable, __file__, "--in-namespace"]).returncode

    body()
    return 0


if __name__ == "__main__":
    sys.exit(main())
