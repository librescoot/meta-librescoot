SUMMARY = "ioctl - IOCTL for Bash"
DESCRIPTION = "The missing tool to call arbitrary ioctl on devices"
HOMEPAGE = "https://github.com/jerome-pouiller/ioctl.git"
LICENSE = "GPL-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=b234ee4d69f5fce4486a80fdaf4a4263"

SRC_URI = "git://github.com/jerome-pouiller/ioctl.git;protocol=https;branch=master"
SRCREV = "d5610a5106df276c226fd0015191c5e9b504b0e4"

DEBUG_PREFIX_MAP:remove = "-fcanon-prefix-map"

S = "${WORKDIR}/git"

EXTRA_OEMAKE = "'CC=${CC}'"

do_compile() {
    oe_runmake IGNORE_IOCTLS_LIST=1
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${S}/ioctl ${D}${bindir}
}
