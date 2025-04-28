#!/bin/sh

ifconfig usb0 192.168.7.1
iptables --table nat --append POSTROUTING --out-interface eth0 -j MASQUERADE
iptables --table nat --append POSTROUTING --out-interface wwan0 -j MASQUERADE
iptables --append FORWARD --in-interface usb0 -j ACCEPT
echo 1 > /proc/sys/net/ipv4/ip_forward

ip link set can0 type can bitrate 125000 restart-ms 100
ifconfig can0 up
