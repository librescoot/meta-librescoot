# Seed the image's own .mender artifact into the sdimg data partition at
# /data/ota/${LIBRESCOOT_OTA_COMPONENT}/ so a freshly flashed device already
# carries the artifact matching its running version. update-service resolves
# the delta base by looking the running version up in that directory, so
# factory installs can take delta OTAs without first downloading a full image.
#
# meta-mender builds the data partition (dataimg, embedded into the sdimg)
# from a copy of ${IMAGE_ROOTFS}/data set up by the do_copy_rootfs prefunc;
# this hooks in after it, mirroring meta-mender's own
# do_install_bootstrap_artifact. Appending here is safe because the mender
# classes come in via INHERIT and register their prefuncs first.
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
    install -d "${_MENDER_ROOTFS_COPY}/ota/${LIBRESCOOT_OTA_COMPONENT}"
    install -m 0644 "${IMGDEPLOYDIR}/${IMAGE_LINK_NAME}.mender" \
        "${_MENDER_ROOTFS_COPY}/ota/${LIBRESCOOT_OTA_COMPONENT}/librescoot-${MACHINE}-${LIBRESCOOT_VERSION}.mender"
}

do_image_dataimg[prefuncs] += " librescoot_seed_ota_artifact"
