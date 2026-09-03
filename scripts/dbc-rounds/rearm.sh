#!/bin/bash
# Re-arm the one-shot test boot of the .test files already on the DBC.
set -e
[ -f /boot/zImage.test ] || { echo "dbc.sh: no zImage.test"; exit 1; }
fw_printenv -n bootcmd | grep -q testboot || { echo "dbc.sh: bootcmd not wrapped"; exit 1; }
fw_setenv testboot 1; fw_printenv testboot; sync; echo "dbc.sh: re-armed"; exit 0
