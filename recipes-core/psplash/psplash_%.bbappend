FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += "file://librescoot-splash.png \
            file://psplash-colors.h \
           "

# Override the default Yocto splash image with LibreScoot branding
SPLASH_IMAGES:forcevariable = "file://librescoot-splash.png;outsuffix=default"

# Copy custom color definitions before compilation
do_configure:prepend() {
    if [ -f ${WORKDIR}/psplash-colors.h ]; then
        cp ${WORKDIR}/psplash-colors.h ${S}/
    fi
}
