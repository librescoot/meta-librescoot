SUMMARY = "LibreScoot Go microservices"
DESCRIPTION = "All LibreScoot Go-based microservices. \
    MDB services handle vehicle control, cellular, NFC, etc. \
    DBC services handle display, updates, and backlight."
LICENSE = "MIT"

inherit packagegroup

# Services common to MDB
RDEPENDS:${PN}:append:unu-mdb = " \
    ecu-service \
    vehicle-service \
    keycard-service \
    boot-led-service \
    battery-service \
    modem-service \
    onboot-service \
    bluetooth-service \
    pm-service \
    update-service \
    ums-service \
    settings-service \
    alarm-service \
    uplink-service \
    lsc \
    radio-gaga \
"

# Services for DBC (i.MX6 and RPi4)
RDEPENDS:${PN}:append:unu-dbc = " \
    onboot-service \
    update-service \
    dbc-backlight-service \
    brightness-reader \
    lsc \
"

RDEPENDS:${PN}:append:librescoot-dbc-rpi4 = " \
    onboot-service \
    update-service \
    dbc-backlight-service \
    brightness-reader \
    lsc \
"

# Version service is common to all targets
RDEPENDS:${PN} = " \
    version-service \
"
