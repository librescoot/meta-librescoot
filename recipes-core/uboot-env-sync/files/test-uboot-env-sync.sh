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
[ "$1" = "-n" ] || exit 1
[ -f "$ENVSTORE/$2" ] || exit 1
cat "$ENVSTORE/$2"
EOF
cat > "$bin/fw_setenv" <<'EOF'
#!/bin/sh
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
