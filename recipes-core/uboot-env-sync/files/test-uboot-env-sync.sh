#!/bin/sh
# Exercise the uboot-env-sync directives against a fake U-Boot environment.
# The values here are boot-critical (bootargs), so the migration is worth
# running on every image build rather than only on a board.
set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
sync_script=${UBOOT_ENV_SYNC_SCRIPT:-$script_dir/uboot-env-sync.sh}

# In the source tree the shipped conf sits under files/unu-dbc/; in UNPACKDIR
# the fetcher has flattened it next to this script. It is DBC-only, so a MDB
# build runs the generic directives without it.
shipped=
for candidate in "$script_dir/boot-animation.conf" "$script_dir/unu-dbc/boot-animation.conf"; do
    if [ -f "$candidate" ]; then
        shipped=$candidate
        break
    fi
done

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
store=$tmp/store
conf=$tmp/conf
bin=$tmp/bin
mkdir -p "$store" "$conf" "$bin"

cat > "$bin/fw_printenv" <<'EOF'
#!/bin/sh
[ "${FAIL_READ:-0}" = 0 ] || exit 1
if [ "$#" = 0 ]; then
    for file in "$ENVSTORE"/*; do
        [ -f "$file" ] || continue
        printf '%s=' "${file##*/}"
        cat "$file"
        printf '\n'
    done
    exit 0
fi
[ "$1" = "-n" ] || exit 1
[ "${FAIL_KEY_READ:-0}" = 0 ] || exit 1
[ -f "$ENVSTORE/$2" ] || exit 1
cat "$ENVSTORE/$2"
EOF
cat > "$bin/fw_setenv" <<'EOF'
#!/bin/sh
[ "${FAIL_WRITE:-0}" = 0 ] || exit 1
printf "%s\n" "$1" >> "$ENVSTORE.writes"
if [ $# -ge 2 ]; then
    printf '%s' "$2" > "$ENVSTORE/$1"
else
    rm -f "$ENVSTORE/$1"
fi
EOF
chmod +x "$bin/fw_printenv" "$bin/fw_setenv"

ENVSTORE=$store
UBOOT_ENV_SYNC_CONF_DIR=$conf
PATH=$bin:$PATH
export ENVSTORE UBOOT_ENV_SYNC_CONF_DIR PATH

fail() { echo "FAIL: $*" >&2; exit 1; }
expect() { # key expected
    got=$(cat "$store/$1" 2>/dev/null || true)
    [ "$got" = "$2" ] || fail "$1 = [$got], want [$2]"
}
run() { sh "$sync_script" >/dev/null; }
use_conf() { rm -f "$conf"/*.conf; cp "$1" "$conf/01.conf"; }

# --- append-if-missing -----------------------------------------------------

printf '%s\n' 'append-if-missing bootargs logo.name=${boot_animation}' > "$conf/01.conf"
printf 'console=ttymxc0,115200 quiet' > "$store/bootargs"
run
expect bootargs 'console=ttymxc0,115200 quiet logo.name=${boot_animation}'

# Idempotent, and the reference stays literal for U-Boot to expand.
run
expect bootargs 'console=ttymxc0,115200 quiet logo.name=${boot_animation}'

# A longer token that merely ends with the wanted text is not a match.
printf 'prefix-logo.name=${boot_animation} tail' > "$store/bootargs"
run
expect bootargs 'prefix-logo.name=${boot_animation} tail logo.name=${boot_animation}'

# Appending to an unset variable leaves no leading space.
rm -f "$store/bootargs"
run
expect bootargs 'logo.name=${boot_animation}'

# --- set-if-missing --------------------------------------------------------

printf '%s\n' 'set-if-missing boot_sound 1' > "$conf/01.conf"
run
expect boot_sound '1'
printf '0' > "$store/boot_sound"
run
expect boot_sound '0'

# --- set-if-matches --------------------------------------------------------

# Unquoted operands keep the original reading: the first token is the old value
# and everything after it is the new one. This is how a bare console line is
# expanded into the full bootargs.
printf '%s\n' 'set-if-matches bootargs console=ttymxc0,115200 console=ttymxc0,115200 loglevel=0 quiet' > "$conf/01.conf"
printf '%s' 'console=ttymxc0,115200' > "$store/bootargs"
run
expect bootargs 'console=ttymxc0,115200 loglevel=0 quiet'

# A value containing spaces needs single quotes to separate it from the other
# operand. Without quoting this line compared the current value against the
# literal string "setexpr" and never matched.
cat > "$conf/01.conf" <<'CONF'
set-if-matches mender_pre_setup_commands 'setexpr uid_lo *0x021BC410; setenv _hn systemd.hostname=unu-dbc-${uid_lo}; setenv bootargs ${bootargs} ${_hn}' 'setexpr uid_hi *0x021BC420; setenv _hn systemd.hostname=unu-dbc-${uid_hi}; setenv bootargs ${bootargs} ${_hn}'
CONF
printf '%s' 'setexpr uid_lo *0x021BC410; setenv _hn systemd.hostname=unu-dbc-${uid_lo}; setenv bootargs ${bootargs} ${_hn}' > "$store/mender_pre_setup_commands"
run
expect mender_pre_setup_commands 'setexpr uid_hi *0x021BC420; setenv _hn systemd.hostname=unu-dbc-${uid_hi}; setenv bootargs ${bootargs} ${_hn}'

# Idempotent: the migrated value no longer matches the old one.
run
expect mender_pre_setup_commands 'setexpr uid_hi *0x021BC420; setenv _hn systemd.hostname=unu-dbc-${uid_hi}; setenv bootargs ${bootargs} ${_hn}'

# A value that does not match is left alone.
printf '%s' 'something else entirely' > "$store/mender_pre_setup_commands"
run
expect mender_pre_setup_commands 'something else entirely'

# An unterminated quote, and a line with only one operand, are config errors.
# Both used to be read as an operand pair and reported as applied.
printf '%s\n' "set-if-matches bootargs 'console=ttymxc0,115200 quiet" > "$conf/01.conf"
printf '%s' 'console=ttymxc0,115200 quiet' > "$store/bootargs"
run
expect bootargs 'console=ttymxc0,115200 quiet'
printf '%s\n' 'set-if-matches bootargs console=ttymxc0,115200' > "$conf/01.conf"
run
expect bootargs 'console=ttymxc0,115200 quiet'

# Test the actual shipped identity files, including the flattened build input.
for identity in "$script_dir/identity.conf" "$script_dir/unu-mdb/identity.conf" "$script_dir/unu-dbc/identity.conf"; do
    [ -f "$identity" ] || continue
    case "$(cat "$identity")" in
        *unu-mdb-*) board=unu-mdb ;;
        *unu-dbc-*) board=unu-dbc ;;
        *) fail "unknown identity board: $identity" ;;
    esac
    old='setexpr uid_lo *0x021BC410; setenv _hn systemd.hostname='"$board"'-${uid_lo}; setenv bootargs ${bootargs} ${_hn}'
    new='setexpr uid_hi *0x021BC420; setenv _hn systemd.hostname='"$board"'-${uid_hi}; setenv bootargs ${bootargs} ${_hn}'
    use_conf "$identity"
    printf '%s' 'custom bootargs' > "$store/bootargs"
    printf '%s' 'untouched mender_pre_setup_commands=inside-value' > "$store/other"
    printf '%s' 'untouched' > "$store/prefix_mender_pre_setup_commands"
    rm -f "$store/mender_pre_setup_commands" "$ENVSTORE.writes"
    run
    expect mender_pre_setup_commands "$new"
    run
    [ "$(wc -l < "$ENVSTORE.writes")" -eq 1 ] || fail 'missing initialization wrote twice'
    : > "$store/mender_pre_setup_commands"
    rm -f "$ENVSTORE.writes"
    run
    expect mender_pre_setup_commands "$new"
    [ "$(wc -l < "$ENVSTORE.writes")" -eq 1 ] || fail 'empty initialization failed'
    printf '%s' "$old" > "$store/mender_pre_setup_commands"
    rm -f "$ENVSTORE.writes"
    run
    expect mender_pre_setup_commands "$new"
    run
    [ "$(wc -l < "$ENVSTORE.writes")" -eq 1 ] || fail 'migration wrote twice'
    for custom in "$old; custom" " $old" "${old%_hn}other}" 'custom hostname command'; do
        printf '%s' "$custom" > "$store/mender_pre_setup_commands"
        rm -f "$ENVSTORE.writes"
        run
        expect mender_pre_setup_commands "$custom"
        [ ! -f "$ENVSTORE.writes" ] || fail 'custom command was written'
    done
    expect bootargs 'custom bootargs'
    expect other 'untouched mender_pre_setup_commands=inside-value'
    expect prefix_mender_pre_setup_commands 'untouched'
    rm -f "$store/mender_pre_setup_commands" "$ENVSTORE.writes"
    if FAIL_READ=1 run; then fail 'read failure succeeded'; fi
    [ ! -f "$ENVSTORE.writes" ] || fail 'read failure wrote env'
    if FAIL_WRITE=1 run; then fail 'write failure succeeded'; fi
    [ ! -f "$store/mender_pre_setup_commands" ] || fail 'write failure changed env'
    printf '%s' "$old" > "$store/mender_pre_setup_commands"
    if FAIL_KEY_READ=1 run; then fail 'key read failure succeeded'; fi
    expect mender_pre_setup_commands "$old"
    if FAIL_WRITE=1 run; then fail 'migration write failure succeeded'; fi
    expect mender_pre_setup_commands "$old"
done

# --- the shipped conf ------------------------------------------------------

if [ -z "$shipped" ]; then
    echo 'uboot-env-sync: no shipped conf for this machine, skipping its checks'
else

# A line that already has boot.animation but predates logo.name gains it once.
use_conf "$shipped"
printf 'console=ttymxc0,115200 loglevel=0 quiet systemd.show_status=false vt.global_cursor_default=0 boot.animation=${boot_animation}' > "$store/bootargs"
run
expect bootargs 'console=ttymxc0,115200 loglevel=0 quiet systemd.show_status=false vt.global_cursor_default=0 boot.animation=${boot_animation} logo.name=${boot_animation}'
run
expect bootargs 'console=ttymxc0,115200 loglevel=0 quiet systemd.show_status=false vt.global_cursor_default=0 boot.animation=${boot_animation} logo.name=${boot_animation}'

# A board whose saved env holds nothing but a bare console gets the full line.
printf 'console=ttymxc0,115200' > "$store/bootargs"
run
expect bootargs 'console=ttymxc0,115200 loglevel=0 quiet systemd.show_status=false vt.global_cursor_default=0 boot.animation=${boot_animation} logo.name=${boot_animation}'

# A Mender line that already has both is left alone apart from splashpos.
printf 'console=ttymxc0,115200 loglevel=0 quiet boot.animation=${boot_animation} logo.name=${boot_animation} root=/dev/mmcblk3p2' > "$store/bootargs"
run
expect bootargs 'console=ttymxc0,115200 loglevel=0 quiet boot.animation=${boot_animation} logo.name=${boot_animation} root=/dev/mmcblk3p2'
expect splashpos 'm,395'

# The theme default is only filled in when absent.
rm -f "$store/boot_sound"
run
expect boot_animation 'librescoot'
expect boot_sound '1'

fi # shipped conf

echo 'uboot-env-sync: directive tests passed'
