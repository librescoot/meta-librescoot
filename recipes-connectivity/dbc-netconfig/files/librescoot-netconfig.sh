#!/bin/sh
#
# Bring the link to the MDB up as soon as the interface exists.
#
# usb0 is created by the kernel when cdc_ether binds, about 5s into boot, but
# systemd-networkd is ordered after systemd-udevd and neither gets scheduled
# promptly while the dashboard and the boot animation are saturating both cores.
# On deep-blue that left the interface unconfigured until ~10.5s and Redis
# unreachable until ~14.7s, so the dashboard painted its first frame with an
# empty cache. The MDB's gadget has carrier about 1.1s into the DBC's boot, so
# all of that wait was on this side.
#
# 10-usb0.network still describes the interface; networkd reconciles against
# what we set here and finds it already correct.

# 192.168.7.2 also lives on lo so it survives usb0 disappearing. Without it
# the address exists only while the USB link does, and the MDB cannot reach the
# DBC over the PPP backup by any route -- the failover would be one-directional.
# Linux accepts the same address on a second interface; usb0's connected route
# still wins for outbound while it is up.
ip addr add 192.168.7.2/32 dev lo 2>/dev/null

i=0
while [ $i -lt 400 ]; do
    [ -e /sys/class/net/usb0 ] && break
    i=$((i + 1))
    sleep 0.02
done

if [ ! -e /sys/class/net/usb0 ]; then
    echo "usb0 did not appear, leaving the link to networkd" >&2
    exit 0
fi

ip link set usb0 up 2>/dev/null
ip addr add 192.168.7.2/24 dev usb0 2>/dev/null
ip route replace default via 192.168.7.1 dev usb0 2>/dev/null

exit 0
