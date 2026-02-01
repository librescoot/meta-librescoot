SUMMARY = "LibreScoot Python packages"
DESCRIPTION = "Python 3 runtime and libraries used by LibreScoot services"
LICENSE = "MIT"

inherit packagegroup

# Common Python packages used on both MDB and DBC
RDEPENDS:${PN} = " \
    python3 \
    python3-pyserial \
    python3-systemd \
    python3-dateutil \
    python3-pyyaml \
    python3-aioredis \
    python3-redis \
    python3-cbor2 \
    python3-smbus2 \
    python3-typing-extensions \
"

# MDB-specific Python packages (CAN bus, NFC tooling, etc.)
RDEPENDS:${PN}:append:unu-mdb = " \
    python3-can \
    python3-numpy \
    python3-click \
    python3-cbor \
    python3-crccheck \
    python3-pc-ble-driver-py \
    python3-nrfutil \
    python3-intelhex \
    python3-ecdsa \
    python3-libusb1 \
    python3-protobuf \
"

# DBC-specific Python packages
RDEPENDS:${PN}:append:unu-dbc = " \
    python3-shapely \
"

RDEPENDS:${PN}:append:librescoot-dbc-rpi4 = " \
    python3-shapely \
"
