#!/bin/sh
# Sync U-Boot environment variables from /etc/uboot-env.d/*.conf.
# Runs on every boot. Supported directives:
#
#   set-if-missing key value       — set only if unset or empty
#   set-always key value           — always overwrite
#   set-if-matches key old new     — replace old value with new (safe migration)
#   unset key                      — remove unconditionally
#   unset-if-matches key value     — remove only if current value matches

CONF_DIR=/etc/uboot-env.d
changed=0

for conf in "$CONF_DIR"/*.conf; do
    [ -f "$conf" ] || continue
    while IFS= read -r line; do
        case "$line" in
            ''|\#*) continue ;;
        esac

        verb="${line%% *}"
        rest="${line#* }"

        case "$verb" in
            set-if-missing)
                key="${rest%% *}"
                value="${rest#* }"
                current="$(fw_printenv -n "$key" 2>/dev/null)" || true
                if [ -z "$current" ]; then
                    fw_setenv "$key" "$value"
                    echo "uboot-env-sync: set-if-missing $key = $value"
                    changed=1
                fi
                ;;
            set-always)
                key="${rest%% *}"
                value="${rest#* }"
                fw_setenv "$key" "$value"
                echo "uboot-env-sync: set-always $key = $value"
                changed=1
                ;;
            set-if-matches)
                key="${rest%% *}"
                rest2="${rest#* }"
                oldval="${rest2%% *}"
                newval="${rest2#* }"
                current="$(fw_printenv -n "$key" 2>/dev/null)" || true
                if [ "$current" = "$oldval" ]; then
                    fw_setenv "$key" "$newval"
                    echo "uboot-env-sync: set-if-matches $key: [$oldval] -> [$newval]"
                    changed=1
                fi
                ;;
            unset)
                key="${rest%% *}"
                current="$(fw_printenv -n "$key" 2>/dev/null)" || true
                if [ -n "$current" ]; then
                    fw_setenv "$key"
                    echo "uboot-env-sync: unset $key (was: $current)"
                    changed=1
                fi
                ;;
            unset-if-matches)
                key="${rest%% *}"
                value="${rest#* }"
                current="$(fw_printenv -n "$key" 2>/dev/null)" || true
                if [ "$current" = "$value" ]; then
                    fw_setenv "$key"
                    echo "uboot-env-sync: unset-if-matches $key (was: $current)"
                    changed=1
                fi
                ;;
            *)
                echo "uboot-env-sync: unknown directive '$verb' in $conf, skipping"
                ;;
        esac
    done < "$conf"
done

if [ "$changed" -eq 0 ]; then
    echo "uboot-env-sync: all variables up to date"
fi
