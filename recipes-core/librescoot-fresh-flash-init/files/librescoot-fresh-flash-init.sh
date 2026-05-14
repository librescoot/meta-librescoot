#!/bin/sh
# Apply fresh-flash gating on freshly flashed MDB images. Runs only when
# /data/.fresh-flash is present (the marker is baked into the .sdimg's data
# partition and deleted by the installer as its final step).
#
# Gates applied:
# - Runtime-mask librescoot-keycard so it doesn't auto-start. The mask lives
#   in /run/ and disappears on reboot; once the marker is removed, this script
#   doesn't run, so keycard auto-starts normally on the following boot. The
#   installer can `systemctl unmask --runtime librescoot-keycard && start ...`
#   to teach cards while the marker is still present.
# - Force alarm.enabled=false and scooter.auto-standby-seconds=0 in Redis.
#   Consumer services pick these up via their settings watchers.
#
# scooter.usb0-policy isn't touched here: its schema default is always-on,
# and the installer flips it to "auto" itself at finish.

systemctl mask --runtime librescoot-keycard.service 2>/dev/null || true

redis-cli -n 0 hset settings \
    scooter.auto-standby-seconds 0 \
    alarm.enabled false >/dev/null 2>&1 || true

exit 0
