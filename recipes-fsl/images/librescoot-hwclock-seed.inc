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
    ts = time.strftime('%Y-%m-%d %H:%M:%S', time.gmtime())
    path = os.path.join(rootfs, 'etc', 'fake-hwclock.data')
    with open(path, 'w') as f:
        f.write(ts + '\n')
}
