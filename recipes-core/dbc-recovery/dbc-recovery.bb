SUMMARY = "DBC bootloader-recovery diagnostics for the MDB"
DESCRIPTION = "dbc-sdp-probe determines whether a DBC with a corrupted \
bootloader can be reached from the MDB in i.MX serial-download mode, by \
flipping the MDB's OTG port to host and watching the bus for the boot ROM. \
Reports only; it writes nothing to the DBC."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://dbc-sdp-probe.sh"

# MDB only. It is the board that would have to host a bricked DBC; a DBC well
# enough to run this is not the one that needs it.
COMPATIBLE_MACHINE = "unu-mdb"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${UNPACKDIR}/dbc-sdp-probe.sh ${D}${bindir}/dbc-sdp-probe
}

# lsc controls DBC power; kmod provides modprobe for the g_ether unload/reload
# around the role switch.
RDEPENDS:${PN} = "lsc kmod"

PACKAGE_ARCH = "${MACHINE_ARCH}"
FILES:${PN} = "${bindir}/dbc-sdp-probe"
