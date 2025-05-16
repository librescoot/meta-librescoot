DESCRIPTION = "pc-ble-driver provides C/C++ libraries for Bluetooth Low Energy nRF5 SoftDevice serialization"
HOMEPAGE = "https://github.com/librescoot/pc-ble-driver"
LICENSE = "CLOSED"
LIC_FILES_CHKSUM = "file://LICENSE;md5=772c3f93b8a2f4f2dec94ef7b9f434fb"

SRC_URI = "git://github.com/librescoot/pc-ble-driver;protocol=https"
SRCREV = "fdbf92831badbca016d2bf95da6fab056ef2d931"
S = "${WORKDIR}/git"

inherit cmake

DEPENDS = "asio \
           catch2 \
           spdlog \
           systemd \
           "

EXTRA_OECMAKE = "-DDISABLE_TESTS=True -DDISABLE_EXAMPLES=True -DNRF_BLE_DRIVER_VERSION=${PV}"


