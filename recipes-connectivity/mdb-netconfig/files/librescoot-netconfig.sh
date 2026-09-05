#!/bin/sh

# usb0's address comes from 10-usb0.network, and vehicle-service decides
# whether the link is up. On an image that never ships vehicle-service, the
# installer's bootstrap one, nothing ever records a usb0-gate decision, so
# librescoot-usb0-failsafe.timer's 120s deadline becomes the only path to a
# reachable board on every boot. Raise it here instead when there is no owner
# to race. ifconfig is not on that image; ip is.
ip addr add 192.168.7.1/32 dev lo 2>/dev/null

if [ ! -x /usr/bin/vehicle-service ]; then
    ip addr replace 192.168.7.1/24 dev usb0
    ip addr replace 192.168.9.1/24 dev usb0
    ip link set usb0 up
    exit 0
fi

iptables --table nat --append POSTROUTING --out-interface eth0 -j MASQUERADE
iptables --table nat --append POSTROUTING --out-interface wwan0 -j MASQUERADE
iptables --append FORWARD --in-interface usb0 -j ACCEPT
iptables --append FORWARD --in-interface ppp0 -j ACCEPT
iptables --append INPUT --in-interface wwan0 -p tcp --dport 6379 -j DROP
echo 1 > /proc/sys/net/ipv4/ip_forward

ip link set can0 type can bitrate 125000 restart-ms 100
ip link set can0 up
