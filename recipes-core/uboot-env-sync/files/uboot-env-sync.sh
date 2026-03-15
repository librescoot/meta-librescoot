#!/bin/sh
# Sync expected U-Boot environment variables from /etc/uboot-env.d/*.conf.
# Runs on every boot; only writes variables that are missing or empty.
# Config files use simple key=value format (no quoting needed, value is
# everything after the first '=').

CONF_DIR=/etc/uboot-env.d
changed=0

for conf in "$CONF_DIR"/*.conf; do
    [ -f "$conf" ] || continue
    while IFS= read -r line; do
        case "$line" in
            ''|\#*) continue ;;
        esac
        key="${line%%=*}"
        value="${line#*=}"
        current="$(fw_printenv -n "$key" 2>/dev/null)" || true
        if [ -z "$current" ]; then
            fw_setenv "$key" "$value"
            echo "uboot-env-sync: set $key"
            changed=1
        fi
    done < "$conf"
done

if [ "$changed" -eq 0 ]; then
    echo "uboot-env-sync: all variables present"
fi
