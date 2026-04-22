SUMMARY = "LibreScoot connectivity packages"
DESCRIPTION = "Network, cellular, and wireless connectivity stack"
LICENSE = "MIT"

inherit packagegroup

# MDB connectivity: cellular modem, Wi-Fi, BLE, WireGuard VPN
RDEPENDS:${PN}:append:unu-mdb = " \
    mdb-netconfig \
    modemmanager \
    networkmanager \
    networkmanager-nmcli \
    wireguard-tools \
    nxp-nfc \
    libnfc \
    nrf-ble-driver \
"

# DBC connectivity: local network only
RDEPENDS:${PN}:append:unu-dbc = " \
    dbc-netconfig \
"

RDEPENDS:${PN}:append:librescoot-dbc-rpi4 = " \
    dbc-netconfig \
"
