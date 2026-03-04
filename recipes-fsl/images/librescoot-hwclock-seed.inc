# Install fake-hwclock and pre-seed it with the build timestamp.
#
# On first boot (or after RTC power loss) the system clock will be set to
# approximately the image build time instead of the epoch. fake-hwclock then
# updates the seed file on every shutdown, so subsequent boots use the
# last-known-good time.

CORE_IMAGE_EXTRA_INSTALL += "fake-hwclock"

IMAGE_POSTPROCESS_COMMAND:append = " seed_fake_hwclock; "

python seed_fake_hwclock() {
    import time, os
    rootfs = d.getVar('IMAGE_ROOTFS')
    epoch = int(time.time())

    # Seed fake-hwclock so first boot starts at build time, not epoch.
    hwclock_path = os.path.join(rootfs, 'etc', 'fake-hwclock.data')
    with open(hwclock_path, 'w') as f:
        f.write(time.strftime('%Y-%m-%d %H:%M:%S', time.gmtime(epoch)) + '\n')

    # Write build timestamp (epoch seconds) so userspace can determine
    # whether the current system time is merely plausible (>= build time,
    # set by fake-hwclock) or actually correct (synced by Chrony/GPS).
    ts_path = os.path.join(rootfs, 'etc', 'build-timestamp')
    with open(ts_path, 'w') as f:
        f.write(str(epoch) + '\n')
}
