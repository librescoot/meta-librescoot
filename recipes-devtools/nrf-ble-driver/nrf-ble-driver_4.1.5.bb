DESCRIPTION = "pc-ble-driver provides C/C++ libraries for Bluetooth Low Energy nRF5 SoftDevice serialization"
HOMEPAGE = "https://github.com/librescoot/pc-ble-driver"
LICENSE = "CLOSED"
LIC_FILES_CHKSUM = "file://LICENSE;md5=772c3f93b8a2f4f2dec94ef7b9f434fb"

SRC_URI = "git://github.com/librescoot/pc-ble-driver;protocol=https;branch=master"
SRCREV = "fdbf92831badbca016d2bf95da6fab056ef2d931"

inherit cmake

DEPENDS = "asio \
           catch2 \
           spdlog \
           systemd \
           "

FILES:${PN} += "/usr/share/LICENSE"

EXTRA_OECMAKE = "-DDISABLE_TESTS=True -DDISABLE_EXAMPLES=True -DNRF_BLE_DRIVER_VERSION=${PV}"

# Modern ASIO removed asio::io_service (use asio::io_context). This old driver
# already uses the new make_work_guard/restart() API, so only the type differs.
do_configure:prepend() {
    sed -i 's/asio::io_service/asio::io_context/g' ${S}/src/common/transport/uart_transport.cpp
}
