#!/bin/sh

route add default gw 192.168.7.1
echo "nameserver 1.1.1.1" >> /etc/resolv.conf
echo "nameserver 1.0.0.1" >> /etc/resolv.conf

sleep 50

route del default gw 192.168.7.1
route add default gw 192.168.7.1
