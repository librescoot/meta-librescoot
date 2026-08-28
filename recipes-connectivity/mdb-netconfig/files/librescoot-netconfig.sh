#!/bin/sh

# usb0's address comes from 10-usb0.network. Nothing sets it here: an
# `ifconfig usb0 <addr>` raises the link as a side effect, and vehicle-service
# is the only thing that gets to decide whether usb0 is up.
#
# Except on an image that never ships vehicle-service at all — the
# installer's minimal/bootstrap image. There, nothing will ever record a
# usb0-gate decision, so librescoot-usb0-failsafe.timer's 120s deadline is
# not a rare backstop, it is the only path, on every single boot. Bring the
# link up here instead: this is exactly the unconditional behaviour usb0 had
# before vehicle-service owned the policy, restored only where there is no
# policy owner to race.
if [ ! -x /usr/bin/vehicle-service ]; then
    ifconfig usb0 192.168.7.1 2>/dev/null || true
fi

iptables --table nat --append POSTROUTING --out-interface eth0 -j MASQUERADE
iptables --table nat --append POSTROUTING --out-interface wwan0 -j MASQUERADE
iptables --append FORWARD --in-interface usb0 -j ACCEPT
iptables --append INPUT --in-interface wwan0 -p tcp --dport 6379 -j DROP
echo 1 > /proc/sys/net/ipv4/ip_forward

ip link set can0 type can bitrate 125000 restart-ms 100
ifconfig can0 up
