SUMMARY = "HDiffPatch binary delta decoder"
HOMEPAGE = "https://github.com/sisong/HDiffPatch"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE;md5=740f01c4140f37551526ca06eb60ea11"

SRC_URI = "git://github.com/sisong/HDiffPatch.git;protocol=https;branch=master"
SRCREV = "3b9dca715ca492873bf2c49e22e5d5b7d2a78620"
DEPENDS = "zstd"

EXTRA_OEMAKE = "DIR_DIFF=0 MT=0 LDEF=0 ZLIB=0 LZMA=0 ZSTD=2 XXH=0 MD5=0 BSD=0 VCD=0 BZIP2=0"

do_compile() {
    oe_runmake -B hpatchz CC="${CC} ${LDFLAGS}"
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 hpatchz ${D}${bindir}/hpatchz
}
