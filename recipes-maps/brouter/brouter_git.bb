SUMMARY = "BRouter - OpenStreetMap router"
DESCRIPTION = "BRouter is a configurable routing-engine for OpenStreetMap data"
HOMEPAGE = "https://github.com/abrensch/brouter"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://brouter-1.7.7-all.jar;md5=4e9ac1152420ddd884dfb751921c80b8"
RDEPENDS:${PN} = "openjdk-17-jre"

SRC_URI = "https://github.com/abrensch/brouter/releases/download/v1.7.7/brouter-1.7.7.zip \
           file://server.sh"
SRC_URI[sha256sum] = "6b77f657d19a94d433200dd6fc15e93df862cfcc79b50cab7a84d6215c9d49b4"

S = "${WORKDIR}/brouter-1.7.7"

do_compile[noexec] = "1"

do_install() {
    install -d ${D}/opt/brouter
    install -d ${D}${bindir}
    install -m 0644 ${S}/brouter-1.7.7-all.jar ${D}/opt/brouter/brouter.jar
    install -m 0755 ${WORKDIR}/server.sh ${D}${bindir}/brouter
}

FILES:${PN} = " \
    /opt/brouter/brouter.jar \
    ${bindir}/brouter \
"
