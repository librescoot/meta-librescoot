#!/bin/sh
#
# Raise usb0 if vehicle-service never decided what to do with it.
#
# vehicle-service owns the link: 10-usb0.network leaves it administratively
# down, and vehicle-service either raises it (the keycard gate is open, so the
# recovery path has to stay reachable) or lets it track dashboard_power (the
# gate is closed). Either way it records the decision in system[usb0-gate].
#
# An empty field this far into the boot means it never got that far: dead,
# crash-looping slowly enough to never reach systemd's start limit, or wedged
# before the gate resolved. That is exactly when usb0 is the way back in, so
# raise it and say so loudly.
#
# A valkey that will not answer lands here too, with the same conclusion: a
# scooter whose Redis is down is not one to lock the recovery port on.

gate=$(redis-cli --raw hget system usb0-gate 2>/dev/null)

if [ -n "$gate" ]; then
    echo "usb0 gate is ${gate}, vehicle-service has the link"
    exit 0
fi

echo "No usb0 gate decision recorded, raising usb0 for recovery"
ip link set usb0 up
