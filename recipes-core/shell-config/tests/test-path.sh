#!/bin/sh
set -eu

root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
profile="$root/files/librescoot-path.sh"

sh -n "$profile"
for initial in /usr/bin:/bin /data/bin:/usr/bin /usr/bin:/data/bin:/bin ''; do
    PROFILE="$profile" INITIAL="$initial" /bin/sh -c '
        PATH=$INITIAL
        . "$PROFILE"
        expected=${INITIAL:+$INITIAL:}/data/bin
        case ":$INITIAL:" in
            *:/data/bin:*) expected=$INITIAL ;;
        esac
        test "$PATH" = "$expected"
        . "$PROFILE"
        test "$PATH" = "$expected"
        test "$(/usr/bin/printenv PATH)" = "$expected"
    '
done

sandbox=$(mktemp -d)
trap 'rm -rf "$sandbox"' EXIT HUP INT TERM
mkdir -p "$sandbox/etc/tmpfiles.d"
cp "$root/files/librescoot-bin.conf" "$sandbox/etc/tmpfiles.d/"
systemd-tmpfiles --root="$sandbox" --create
test "$(stat -c '%a' "$sandbox/data/bin")" = 755
printf '#!/bin/sh\n' > "$sandbox/data/bin/custom-tool"
systemd-tmpfiles --root="$sandbox" --create
test -f "$sandbox/data/bin/custom-tool"
echo 'Persistent binary PATH tests passed'
