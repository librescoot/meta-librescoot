FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += "file://librescoot-splash.png"

# Override the default Yocto splash image with LibreScoot branding
SPLASH_IMAGES:forcevariable = "file://librescoot-splash.png;outsuffix=default"
