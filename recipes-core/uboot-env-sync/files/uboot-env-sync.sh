#!/bin/sh
# Sync U-Boot environment variables from /etc/uboot-env.d/*.conf.
# Runs on every boot. Supported directives:
#
#   set-if-missing key value       — set only if unset or empty
#   set-always key value           — always overwrite
#   set-if-matches key old new     — replace old value with new (safe migration)
#   append-if-missing key token    — append a space-delimited token if absent
#   unset key                      — remove unconditionally
#   unset-if-matches key value     — remove only if current value matches
#
# A value containing spaces can only be given to set-if-matches in single
# quotes; quoting is what separates the old value from the new one:
#
#   set-if-matches mender_pre_setup_commands 'setexpr uid_lo *0x021BC410; ...' \
#                                             'setexpr uid_hi *0x021BC420; ...'
#
# Unquoted operands keep the original reading — the first token is the old value
# and everything after it is the new one — which is how a bare token is expanded
# into a full line. A value cannot itself contain a single quote.

# parse_match_operands <key-relative-operands>
#
# Sets MATCH_OLD and MATCH_NEW. Fails on a line that does not carry both.
parse_match_operands() {
    rest="$1"
    MATCH_OLD=
    MATCH_NEW=

    case "$rest" in
        \'*)
            # Drop the opening quote, then cut the value at the closing one and
            # keep the rest as the remainder for the second operand.
            rest="${rest#\'}"
            case "$rest" in
                *\'*) MATCH_OLD="${rest%%\'*}"; rest="${rest#*\'}" ;;
                *) return 1 ;;
            esac
            # Drop the blanks between the two operands.
            while :; do
                case "$rest" in
                    ' '*) rest="${rest# }" ;;
                    "	"*) rest="${rest#	}" ;;
                    *) break ;;
                esac
            done
            case "$rest" in
                \'*)
                    rest="${rest#\'}"
                    case "$rest" in
                        *\'*) MATCH_NEW="${rest%%\'*}" ;;
                        *) return 1 ;;
                    esac
                    ;;
                '') return 1 ;;
                *) MATCH_NEW="$rest" ;;
            esac
            ;;
        *)
            case "$rest" in
                '') return 1 ;;
                *' '*) MATCH_OLD="${rest%% *}"; MATCH_NEW="${rest#* }" ;;
                *) MATCH_OLD="$rest" ;;
            esac
            [ -n "$MATCH_NEW" ] || return 1
            ;;
    esac
    return 0
}

CONF_DIR=${UBOOT_ENV_SYNC_CONF_DIR:-/etc/uboot-env.d}
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
                if ! parse_match_operands "$rest2"; then
                    echo "uboot-env-sync: malformed set-if-matches for '$key' in $conf, skipping"
                    continue
                fi
                current="$(fw_printenv -n "$key" 2>/dev/null)" || true
                if [ "$current" = "$MATCH_OLD" ]; then
                    fw_setenv "$key" "$MATCH_NEW"
                    echo "uboot-env-sync: set-if-matches $key: [$MATCH_OLD] -> [$MATCH_NEW]"
                    changed=1
                fi
                ;;
            append-if-missing)
                # For space-delimited lists such as bootargs, where the exact
                # value depends on what else has been put on the line. A
                # whole-string set-if-matches cannot express a value that
                # contains spaces, so it silently never migrates those.
                key="${rest%% *}"
                value="${rest#* }"
                current="$(fw_printenv -n "$key" 2>/dev/null)" || true
                current="${current% }"
                case " $current " in
                    *" $value "*)
                        ;;
                    *)
                        fw_setenv "$key" "${current:+$current }$value"
                        echo "uboot-env-sync: append-if-missing $key += $value"
                        changed=1
                        ;;
                esac
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
