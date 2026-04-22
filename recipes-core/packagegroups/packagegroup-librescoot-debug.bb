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
    nano \
    screen \
    lsof \
    canutils \
    i2c-tools \
    libgpiod \
    libgpiod-tools \
"

RDEPENDS:${PN} = "${LIBRESCOOT_DEBUG_PACKAGES}"

# MDB keeps full vim-common; DBC uses vim-tiny to save space
RDEPENDS:${PN}:append:unu-mdb = " vim-common"
RDEPENDS:${PN}:append:unu-dbc = " vim-tiny"
RDEPENDS:${PN}:append:librescoot-dbc-rpi4 = " vim-tiny"
