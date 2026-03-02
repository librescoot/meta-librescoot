#!/bin/sh
# Read plymouth.theme from kernel cmdline (U-Boot expands ${plymouth_theme})
theme=$(cat /proc/cmdline | tr ' ' '\n' | sed -n 's/^plymouth\.theme=//p' | tr -dc '[:alnum:]_-')
theme=${theme:-librescoot}
mkdir -p /run/plymouth
cat > /run/plymouth/plymouthd.conf <<EOF
[Daemon]
Theme=$theme
ShowDelay=0
DeviceTimeout=5
IgnoreSerialConsoles=yes
EOF
