#!/bin/bash
# Re-arm the one-shot test boot of the .test files already on the DBC.
set -e
[ -f /boot/zImage.test ] || { echo "dbc.sh: no zImage.test"; exit 1; }
fw_printenv -n bootcmd | grep -q testboot || { echo "dbc.sh: bootcmd not wrapped"; exit 1; }
VER=$(cat /data/flashfree/test-kernel-version 2>/dev/null || true)
if [ -n "$VER" ] && [ ! -d "/lib/modules/$VER" ]; then
    echo "dbc.sh: /lib/modules/$VER is missing: this test kernel would run without its own modules"
    exit 1
fi
fw_setenv testboot 1; fw_printenv testboot; sync; echo "dbc.sh: re-armed"; exit 0
