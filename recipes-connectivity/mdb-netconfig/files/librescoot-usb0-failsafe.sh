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

# An installer run holds the recovery path open on its own.
#
# The installer registers the scooter's keycards over usb0, and the keycard
# threshold closes the gate the moment the second card is learned. That takes
# usb0 down underneath the installer that still has to verify and finish, so the
# run record is the window: finish: pending while the run is open, finish:
# complete once it is over. A record without the field has only just been
# written, which counts as open.
#
# This also covers the boot where vehicle-service read the keycard counts before
# /data was mounted, recorded "closed" from them, and never looked again.
run_state_path=${LIBRESCOOT_INSTALLER_RUN_STATE:-/data/installer/run-state}
finish=$(sed -n 's/^finish:[[:space:]]*//p' "$run_state_path" 2>/dev/null | head -1 | tr -d '\r')

if [ -e "$run_state_path" ] && [ "$finish" != "complete" ]; then
    echo "Installer run in progress (finish: ${finish:-absent}), raising usb0 for the installer"
    systemctl start librescoot-dbc-link.service 2>/dev/null || ip link set usb0 up
    exit 0
fi

gate=$(redis-cli --raw hget system usb0-gate 2>/dev/null)

if [ -n "$gate" ]; then
    echo "usb0 gate is ${gate}, vehicle-service has the link"
    exit 0
fi

echo "No usb0 gate decision recorded, raising usb0 for recovery"
# Through the link unit when it is there, so the route monitor comes up with
# the link. Bootstrap images ship no ppp-link and keep the direct call.
systemctl start librescoot-dbc-link.service 2>/dev/null || ip link set usb0 up
