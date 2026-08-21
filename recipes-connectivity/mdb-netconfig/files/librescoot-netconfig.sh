#!/bin/sh

# usb0's address comes from 10-usb0.network. Nothing sets it here: an
# `ifconfig usb0 <addr>` raises the link as a side effect, and vehicle-service
# is the only thing that gets to decide whether usb0 is up.
iptables --table nat --append POSTROUTING --out-interface eth0 -j MASQUERADE
iptables --table nat --append POSTROUTING --out-interface wwan0 -j MASQUERADE
iptables --append FORWARD --in-interface usb0 -j ACCEPT
iptables --append INPUT --in-interface wwan0 -p tcp --dport 6379 -j DROP
echo 1 > /proc/sys/net/ipv4/ip_forward

ip link set can0 type can bitrate 125000 restart-ms 100
ifconfig can0 up
