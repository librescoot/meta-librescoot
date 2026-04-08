#!/bin/sh

route add default gw 192.168.7.1 || true
echo "nameserver 1.1.1.1" >> /etc/resolv.conf
echo "nameserver 1.0.0.1" >> /etc/resolv.conf
