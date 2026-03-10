#!/bin/sh
# Persist machine-id across OTA updates by writing it to U-Boot env.
# On first boot after flashing, mender_systemd_machine_id is empty, so
# systemd generates a random machine-id and writes it to /etc/machine-id.
# We read it and store it in U-Boot env so that subsequent boots (including
# post-OTA boots) pass systemd.machine_id= on the kernel cmdline, giving a
# stable machine-id tied to the device rather than the rootfs generation.

NVMEM=/sys/bus/nvmem/devices/imx-ocotp0/nvmem

current=$(fw_printenv mender_systemd_machine_id 2>/dev/null | sed 's/mender_systemd_machine_id=//')
if [ -n "$current" ]; then
    exit 0
fi

if [ ! -r "$NVMEM" ]; then
    echo "machine-id-init: cannot read $NVMEM" >&2
    exit 1
fi

# Read OCOTP CFG0+CFG1 (8 bytes at offset 4): lower 64 bits of hardware UID.
uid=$(od -A n -t x1 -j 4 -N 8 "$NVMEM" | tr -d ' \n')

if [ "${#uid}" -ne 16 ]; then
    echo "machine-id-init: unexpected UID length ${#uid}, expected 16" >&2
    exit 1
fi

# machine-id requires exactly 32 lowercase hex chars; double the 16-char UID.
machine_id="${uid}${uid}"

fw_setenv mender_systemd_machine_id "$machine_id"
echo "machine-id-init: set mender_systemd_machine_id=$machine_id (active from next boot)"
