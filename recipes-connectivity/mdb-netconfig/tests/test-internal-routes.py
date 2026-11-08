#!/usr/bin/env python3
"""Check that the MDB's internal link subnets never follow the default route.

librescoot-netconfig.sh installs unreachable routes for 192.168.7.0/24,
192.168.8.0/24 and 192.168.9.0/24 (the stable service addresses, the PPP link
and the USB link). Without them a lookup for the DBC's service address falls
through to the default route and leaves through whatever uplink is up, on a
scooter the mobile network, whenever both internal links are down.

The shipped script runs here against a throwaway network namespace with a fake
uplink and a fake usb0, so the policy is exercised instead of being grepped out
of the file. Needs iproute2 and either unprivileged user namespaces or root.
"""
import os
import subprocess
import sys
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / "files" / "librescoot-netconfig.sh"
INTERNAL = ("192.168.7.2", "192.168.8.2", "192.168.9.2")


def run(*args, check=True):
    return subprocess.run(args, check=check, text=True,
                          stdout=subprocess.PIPE, stderr=subprocess.STDOUT)


def ip(*args, check=True):
    return run("ip", *args, check=check)


def route_to(address):
    """(device, output) for a lookup; device is None when it does not resolve."""
    result = ip("route", "get", address, check=False)
    words = result.stdout.split()
    if result.returncode != 0:
        return None, result.stdout.strip()
    return words[words.index("dev") + 1], result.stdout.strip()


def expect(condition, label):
    if not condition:
        raise SystemExit(f"FAIL {label}")
    print(f"PASS {label}", flush=True)


def body():
    if Path("/usr/bin/vehicle-service").exists():
        raise SystemExit("skipping: this host ships vehicle-service, so the script would take "
                         "its full path and touch iptables and CAN")

    ip("link", "add", "up0", "type", "veth", "peer", "name", "up1")
    ip("link", "add", "usb0", "type", "veth", "peer", "name", "usb0-peer")
    ip("link", "set", "up0", "up")
    ip("link", "set", "up1", "up")
    ip("addr", "add", "10.9.9.1/24", "dev", "up0")
    ip("route", "add", "default", "via", "10.9.9.2", "dev", "up0")

    device, output = route_to("192.168.7.2")
    expect(device == "up0",
           "control: without the guard an internal address follows the default route")

    run("sh", str(SCRIPT))

    expect(route_to("192.168.7.2")[0] == "usb0",
           "the stable service address stays reachable over usb0")
    expect(route_to("192.168.9.2")[0] == "usb0",
           "the link-specific address stays reachable over usb0")
    expect("No route to host" in route_to("192.168.8.2")[1],
           "the PPP subnet fails while its link is down")
    unreachable = ip("route", "show", "type", "unreachable").stdout.split()
    for prefix in ("192.168.7.0/24", "192.168.8.0/24", "192.168.9.0/24"):
        expect(prefix in unreachable, f"the guard route for {prefix} is installed")

    # usb0 down is what vehicle-service does when the DBC has no power. The
    # connected routes go with it, and then every internal address has to fail
    # rather than follow the uplink.
    ip("link", "set", "usb0", "down")
    for address in INTERNAL:
        device, output = route_to(address)
        expect("No route to host" in output,
               f"{address} fails instead of leaving through the uplink")
    expect(route_to("10.9.9.7")[0] == "up0",
           "an ordinary address still follows the default route")


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
