DESCRIPTION = "Python bindings for the nRF5 Bluetooth Low Energy GAP/GATT driver"
HOMEPAGE = "https://github.com/NordicSemiconductor/pc-ble-driver-py"

PYPI_NAME = "pc_ble_driver_py"
PYTHON_VERSION = "cp38"

LICENSE = "Proprietary"
LIC_FILES_CHKSUM = "file://${S}/../git/LICENSE;md5=1e9df9ce515a549de0523956ebef8304"

SRC_URI = "git://github.com/librescoot/pc-ble-driver-py;protocol=https;branch=master"
SRCREV = "9ffd5756c750acd17c633f26a7d886c7c7821189"

inherit python3-dir

DEPENDS = " \
        nrf-ble-driver \
        python3-scikit-build \
        python3-wheel \
        swig-native \
        systemd \
        "

RDEPENDS_${PN} += " \
        libudev \
        udev \
        "

do_install() {
    install -d ${D}${libdir}/${PYTHON_DIR}/site-packages/pc_ble_driver_py
    install -d ${D}${libdir}/${PYTHON_DIR}/site-packages/pc_ble_driver_py/hex
    install -d ${D}${libdir}/${PYTHON_DIR}/site-packages/pc_ble_driver_py/hex/sd_api_v2
    install -d ${D}${libdir}/${PYTHON_DIR}/site-packages/pc_ble_driver_py/hex/sd_api_v5

    install -m 644 ${S}/../git/pc_ble_driver_py/*.py ${D}${libdir}/${PYTHON_DIR}/site-packages/pc_ble_driver_py/
    install -m 644 ${S}/../git/pc_ble_driver_py/hex/*.py ${D}${libdir}/${PYTHON_DIR}/site-packages/pc_ble_driver_py/hex/
    install -m 644 ${S}/../git/pc_ble_driver_py/hex/sd_api_v2/* ${D}${libdir}/${PYTHON_DIR}/site-packages/pc_ble_driver_py/hex/sd_api_v2/
    install -m 644 ${S}/../git/pc_ble_driver_py/hex/sd_api_v5/* ${D}${libdir}/${PYTHON_DIR}/site-packages/pc_ble_driver_py/hex/sd_api_v5/
}

FILES:${PN} += "\
    ${libdir}/${PYTHON_DIR}/site-packages/* \
"

