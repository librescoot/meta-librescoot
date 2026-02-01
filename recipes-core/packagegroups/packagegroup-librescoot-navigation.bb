SUMMARY = "LibreScoot navigation packages"
DESCRIPTION = "GPS, routing (Valhalla/BRouter), and map support for the DBC"
LICENSE = "MIT"

inherit packagegroup

# Navigation is only used on the DBC (dashboard with display)
RDEPENDS:${PN} = ""

RDEPENDS:${PN}:unu-dbc = " \
    valhalla \
    prime-server \
    libspatialite \
"

RDEPENDS:${PN}:librescoot-dbc-rpi4 = " \
    valhalla \
    prime-server \
    libspatialite \
"

# GPS is used on MDB for location tracking
RDEPENDS:${PN}:unu-mdb = " \
    gpsd \
    gps-utils \
    gps-utils-python \
    gpsd-udev \
"
