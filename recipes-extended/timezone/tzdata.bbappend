# Ship ONLY Europe zone files (plus the generic/Universal/Etc scaffolding the
# base tzdata recipe needs), dropping Asia, Africa, the Americas, Australia,
# Antarctica, Atlantic, Arctic and Pacific zones. The MDB/DBC devices only ever
# use Europe/Berlin (DEFAULT_TIMEZONE), so the other ~400 zone files are
# dead weight in the rootfs.
#
# We install the full tree then whitelist-prune (rather than truncating the
# upstream TZONES compile list) because the single `europe` source file also
# emits Russian Asia/* zones, Africa/Ceuta, Greenland America/* and Atlantic/*
# entries; pruning after compile is the precise, robust route and was verified
# against tzdata with the native zic.

ZONEINFO_KEEP = "Europe Etc CET EET MET WET GB W-SU \
                 GMT GMT+0 GMT-0 GMT0 Greenwich UCT UTC Universal Zulu \
                 zone.tab zone1970.tab iso3166.tab leapseconds \
                 leap-seconds.list tzdata.zi"

# Drop the redundant posix and right trees entirely (behind the scenes for
# DST-aware zic variants and leap-second-aware variants respectively); the
# default tree above is what the C library consumes at runtime.
do_install:append() {
    for entry in ${D}${datadir}/zoneinfo/*; do
        [ -e "$entry" ] || continue
        case "$(basename "$entry")" in
            posix|right)
                rm -rf "$entry"
                ;;
            *)
                base=$(basename "$entry")
                case " ${ZONEINFO_KEEP} " in
                    *" $base "*) ;;
                    *) rm -rf "$entry" ;;
                esac
                ;;
        esac
    done
}

# The dropped zone-group packages and the posix/right trees are removed from the
# build; the tzdata metapackage must depend ONLY on packages that still exist.
TZ_PACKAGES = "tzdata-core tzdata-europe"
PACKAGES = "${TZ_PACKAGES} ${PN}"
RDEPENDS:${PN} = "${TZ_PACKAGES}"
ALLOW_EMPTY:${PN} = "1"
