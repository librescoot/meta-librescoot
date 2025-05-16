DESCRIPTION = "Python bindings for the nRF5 Bluetooth Low Energy GAP/GATT driver"
HOMEPAGE = "https://github.com/NordicSemiconductor/pc-ble-driver-py"

PYPI_NAME = "pc_ble_driver_py"
PYTHON_VERSION = "cp38"

LICENSE = "Proprietary"
LIC_FILES_CHKSUM = "file:///${S}/${PYPI_NAME}-${PV}.dist-info/LICENSE;md5=1e9df9ce515a549de0523956ebef8304"

SRC_URI = "git://github.com/librescoot/pc-ble-driver-py;protocol=https"

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
    # The lib subdirectory is apparently unnecessary.
    install -d ${D}${libdir}/${PYTHON_DIR}/site-packages/pc_ble_driver_py-${PV}.dist-info
    install -d ${D}${libdir}/${PYTHON_DIR}/site-packages/pc_ble_driver_py
    install -d ${D}${libdir}/${PYTHON_DIR}/site-packages/pc_ble_driver_py/hex
    install -d ${D}${libdir}/${PYTHON_DIR}/site-packages/pc_ble_driver_py/hex/sd_api_v2
    install -d ${D}${libdir}/${PYTHON_DIR}/site-packages/pc_ble_driver_py/hex/sd_api_v5

    install -m 644 ${S}/pc_ble_driver_py/*.py ${D}${libdir}/${PYTHON_DIR}/site-packages/pc_ble_driver_py/
    install -m 644 ${S}/pc_ble_driver_py/hex/*.py ${D}${libdir}/${PYTHON_DIR}/site-packages/pc_ble_driver_py/hex/
    install -m 644 ${S}/pc_ble_driver_py/hex/sd_api_v2/* ${D}${libdir}/${PYTHON_DIR}/site-packages/pc_ble_driver_py/hex/sd_api_v2/
    install -m 644 ${S}/pc_ble_driver_py/hex/sd_api_v5/* ${D}${libdir}/${PYTHON_DIR}/site-packages/pc_ble_driver_py/hex/sd_api_v5/
    install -m 644 ${S}/pc_ble_driver_py-${PV}.dist-info/* ${D}${libdir}/${PYTHON_DIR}/site-packages/pc_ble_driver_py-${PV}.dist-info/
}

FILES_${PN} += "\
    ${libdir}/${PYTHON_DIR}/site-packages/* \
"

