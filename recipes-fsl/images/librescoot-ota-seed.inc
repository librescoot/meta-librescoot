# Seed the image's own .mender artifact into the sdimg data partition at
# /data/ota/${LIBRESCOOT_OTA_COMPONENT}/ so a freshly flashed device already
# carries the artifact matching its running version. update-service resolves
# the delta base by looking the running version up in that directory, so
# factory installs can take delta OTAs without first downloading a full image.
#
# meta-mender builds the data partition (dataimg, embedded into the sdimg)
# from a copy of ${IMAGE_ROOTFS}/data set up by the do_copy_rootfs prefunc;
# this hooks in after it, mirroring meta-mender's own
# do_install_bootstrap_artifact.
#
# The seeded file name must follow the release asset convention
# librescoot-<variant>-<version>.mender that update-service parses.

LIBRESCOOT_OTA_COMPONENT ?= ""
LIBRESCOOT_VERSION ??= "0.0.1"

# Build the .mender artifact before the data partition image that embeds it.
IMAGE_TYPEDEP:dataimg:append = " mender"

fakeroot librescoot_seed_ota_artifact() {
    if [ -z "${LIBRESCOOT_OTA_COMPONENT}" ]; then
        bbfatal "LIBRESCOOT_OTA_COMPONENT must be set (dbc or mdb) before requiring librescoot-ota-seed.inc"
    fi
    seed_dir="${_MENDER_ROOTFS_COPY}/ota/${LIBRESCOOT_OTA_COMPONENT}"
    install -d "$seed_dir"

    # do_delete_copy_rootfs is a postfunc, so an interrupted or failed build
    # leaves the WORKDIR data.copy.* directory behind with its artifact in it. The
    # next run's do_copy_rootfs uses copyhardlinktree, which merges the fresh
    # /data on top instead of replacing it, so the stale artifact survives
    # under its old version-stamped name. Two of them do not fit a 256 MB data
    # partition and mkfs.ext4 fails with "Could not allocate block". Clear any
    # artifact already here before seeding ours.
    for stale in "$seed_dir"/*.mender; do
        [ -e "$stale" ] || continue
        bbwarn "Removing stale seeded artifact from an earlier build: $(basename "$stale")"
        rm -f "$stale"
    done

    install -m 0644 "${IMGDEPLOYDIR}/${IMAGE_LINK_NAME}.mender" \
        "$seed_dir/librescoot-${MACHINE}-${LIBRESCOOT_VERSION}.mender"
}

# mender-dataimg.bbclass comes in via IMAGE_CLASSES (inherit_defer), so it
# parses AFTER this file: a parse-time += here would land our prefunc before
# do_copy_rootfs and _MENDER_ROOTFS_COPY would still be unset when it runs.
# Anonymous python executes at finalise, after all deferred class parsing,
# so this reliably appends after meta-mender's own prefuncs.
python () {
    d.appendVarFlag('do_image_dataimg', 'prefuncs', ' librescoot_seed_ota_artifact')
}
