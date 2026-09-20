#!/bin/sh
# Launch the boot animation. Reads boot.animation=<name> from the kernel
# command line, defaults to "librescoot". Only the stock theme holds its last
# frame (--once); the others loop until the dashboard takes the display over.
ANIM=$(sed -n 's/.*boot\.animation=\([^ ]*\).*/\1/p' /proc/cmdline)
ANIM=${ANIM:-librescoot}
AUDIO_DEVICE=${BOOT_ANIMATION_AUDIO_DEVICE:-auto}
SOUND_DIR=/usr/share/boot-animation

# Each theme has its own startup sound and its own end behaviour.
ONCE=""
case "$ANIM" in
    librescoot)    ONCE="--once"; SOUND_NAME=scooter-unlock.wav ;;
    windowsxp)     SOUND_NAME=windowsxp.wav ;;
    librescoot-xp) SOUND_NAME=librescoot-xp.wav ;;
    coopertino)    SOUND_NAME=coopertino.wav ;;
    *)             SOUND_NAME="" ;;
esac

# The sound switch is read straight from the U-Boot environment rather than the
# command line: this runs before anything is mounted writable, and growing
# bootargs for a cosmetic flag is not worth the boot risk. An unreadable value
# keeps the sound on, so a missing fw_printenv cannot silence a boot that used
# to chime. A missing sound file is likewise not an error; the animation just
# plays silently.
SOUND_ENABLED=1
if [ -n "$SOUND_NAME" ] && command -v fw_printenv >/dev/null 2>&1; then
    BOOT_SOUND=$(fw_printenv -n boot_sound 2>/dev/null) || BOOT_SOUND=""
    case "$BOOT_SOUND" in
        0|off|false|no|OFF|FALSE|NO) SOUND_ENABLED=0 ;;
    esac
fi

SOUND=""
if [ "$SOUND_ENABLED" = 1 ] && [ -n "$SOUND_NAME" ] && [ -f "$SOUND_DIR/$SOUND_NAME" ]; then
    SOUND="--sound $SOUND_DIR/$SOUND_NAME"
fi

exec /usr/bin/boot-animation "$SOUND_DIR/${ANIM}.json" --fps 25 --fade-ms 1000 \
    --audio-device "$AUDIO_DEVICE" $ONCE $SOUND
