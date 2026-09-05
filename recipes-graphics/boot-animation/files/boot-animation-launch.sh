#!/bin/sh
# Launch the boot animation. Reads boot.animation=<name> from the kernel
# command line, defaults to "librescoot". The default theme runs once
# (--once); other themes loop until plymouth-quit.
ANIM=$(sed -n 's/.*boot\.animation=\([^ ]*\).*/\1/p' /proc/cmdline)
ANIM=${ANIM:-librescoot}
ONCE=""
SOUND=""
if [ "$ANIM" = "librescoot" ]; then
    ONCE="--once"
    SOUND="--sound /usr/share/boot-animation/scooter-unlock.wav"
fi
exec /usr/bin/boot-animation /usr/share/boot-animation/${ANIM}.json --fps 25 --fade-ms 1000 $ONCE $SOUND
