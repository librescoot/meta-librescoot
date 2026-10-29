#!/bin/sh

# A retained opaque overlay would hide the KMS fallback after a UI restart.
if [ -e /sys/class/graphics/fb1/overlay_alpha ] && ! /usr/bin/imx-overlay-alpha 0; then
    echo "ScootUI display: cannot reset overlay" >&2
    exit 1
fi

if [ -c /dev/fb1 ] && [ -w /sys/class/graphics/fb1/overlay_alpha ]; then
    export QT_QPA_EGLFS_INTEGRATION=eglfs_fbdev
    export EGLFS_FBDEV_FB_DEVICE=/dev/fb1
    export EGLFS_FBDEV_RENDER_DEVICE=/dev/dri/renderD128
    echo "ScootUI display: live framebuffer overlay" >&2
else
    export QT_QPA_EGLFS_INTEGRATION=eglfs_kms
    echo "ScootUI display: DRM/KMS fallback" >&2
fi

exec /usr/bin/scootui-qt "$@"
