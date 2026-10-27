SUMMARY = "Explicit account enrollment for installed cloud clients"
HOMEPAGE = "https://github.com/librescoot/connect-service"
LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://src/github.com/librescoot/connect-service/LICENSE;md5=fb5d051e53001fdff7fec0f368f47190"

SRC_URI = "git://github.com/librescoot/connect-service.git;protocol=https;branch=main;destsuffix=${GO_SRCURI_DESTSUFFIX}"
SRCREV = "${AUTOREV}"

inherit librescoot-go

GO_IMPORT = "github.com/librescoot/connect-service"
GO_INSTALL = "${GO_IMPORT}/cmd/connect"
GO_LINKSHARED = ""
GOBUILDFLAGS:remove = "-buildmode=pie"
export CGO_ENABLED = "0"

RDEPENDS:${PN} += "radio-gaga ca-certificates"

# Only the explicit CLI belongs on the scooter, not the hosted servers or demos.
do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${B}/bin/linux_arm/connect ${D}${bindir}/connect
}
