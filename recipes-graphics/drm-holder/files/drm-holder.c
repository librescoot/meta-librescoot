/*
 * drm-holder — keep /dev/dri/cardN open to suppress lastclose modeset.
 *
 * When the last userspace DRM client closes its fd, the kernel's fb helper
 * runs drm_fb_helper_lastclose(), which does a full modeset back to the
 * fbdev-emulation mode. On the DBC (imx-drm, DPI-1 480x480) that modeset
 * manifests as a visible "no-signal" flash — the panel loses sync during
 * CRTC disable → reprogram → enable.
 *
 * Holding any fd on the primary DRM node keeps dev->open_count above zero,
 * so lastclose never runs. We don't need to be master; a plain open is
 * enough.
 *
 * Usage: drm-holder [/dev/dri/cardN]   (default: /dev/dri/card1)
 */

#include <fcntl.h>
#include <signal.h>
#include <unistd.h>

int main(int argc, char **argv) {
    const char *path = (argc > 1) ? argv[1] : "/dev/dri/card1";
    int fd = open(path, O_RDWR | O_CLOEXEC);
    if (fd < 0) return 1;

    sigset_t mask;
    sigfillset(&mask);
    sigdelset(&mask, SIGTERM);
    sigdelset(&mask, SIGINT);
    sigprocmask(SIG_SETMASK, &mask, NULL);

    for (;;) pause();
    return 0;
}
