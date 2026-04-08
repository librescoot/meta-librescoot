# Install fake-hwclock and pre-seed it with the build timestamp.
#
# On first boot (or after RTC power loss) the system clock will be set to
# approximately the image build time instead of the epoch. fake-hwclock then
# updates the seed file on every shutdown, so subsequent boots use the
# last-known-good time.
#
# The persistent copy lives at /data/fake-hwclock.data (survives OTA updates).
# /etc/fake-hwclock.data is the build-time seed, used as fallback when /data
# has no saved timestamp yet (e.g. first boot, fresh device).

CORE_IMAGE_EXTRA_INSTALL += "fake-hwclock"

ROOTFS_POSTPROCESS_COMMAND:append = " seed_fake_hwclock;"

seed_fake_hwclock() {
    EPOCH=$(date -u +'%s')

    # Seed fake-hwclock so first boot starts at build time, not epoch.
    date -u +'%Y-%m-%d %H:%M:%S' > ${IMAGE_ROOTFS}/etc/fake-hwclock.data

    # Write build timestamp (epoch seconds) so userspace can determine
    # whether the current system time is merely plausible (>= build time,
    # set by fake-hwclock) or actually correct (synced by Chrony/GPS).
    echo "$EPOCH" > ${IMAGE_ROOTFS}/etc/build-timestamp
}
