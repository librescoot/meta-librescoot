FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += "file://librescoot-splash.png"

# Override the default Yocto splash image with LibreScoot branding
SPLASH_IMAGES:forcevariable = "file://librescoot-splash.png;outsuffix=default"

# Color configuration: black background, light grey foreground/progress bar
EXTRA_OECONF += " \
    --with-background-color=0x000000 \
    --with-bar-background-color=0x333333 \
    --with-bar-color=0xCCCCCC \
    --with-text-color=0xCCCCCC \
"
