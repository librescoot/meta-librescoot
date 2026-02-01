SUMMARY = "LibreScoot debug and development tools"
DESCRIPTION = "Diagnostic and development utilities. \
    Exclude this packagegroup for production builds by removing it \
    from IMAGE_INSTALL or setting LIBRESCOOT_DEBUG_PACKAGES = '' in local.conf."
LICENSE = "MIT"

inherit packagegroup

# Set to empty in local.conf to exclude debug tools from production images:
#   LIBRESCOOT_DEBUG_PACKAGES = ""
LIBRESCOOT_DEBUG_PACKAGES ?= " \
    htop \
    iotop \
    mc \
    vim-common \
    nano \
    screen \
    lsof \
    canutils \
    i2c-tools \
    libgpiod \
    libgpiod-tools \
"

RDEPENDS:${PN} = "${LIBRESCOOT_DEBUG_PACKAGES}"
