SUMMARY = "scootui"
DESCRIPTION = "ScootUI"
AUTHOR = "Danylo Storozhev, André Bierlein, Teal Bauer"
HOMEPAGE = "None"
BUGTRACKER = "None"
SECTION = "graphics"

LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=fb5d051e53001fdff7fec0f368f47190"

SRCREV = "${AUTOREV}"
SRC_URI = "git://github.com/librescoot/scootui.git;lfs=0;branch=main;protocol=https;destsuffix=git"
SRC_URI += "file://scootui.service"
SRC_URI += "file://scootui-rpi5.service"

PV = "0.4.15+git"
# PR = "r0"

inherit flutter-app systemd

S = "${WORKDIR}/git"

PUBSPEC_APPNAME = "scooter_cluster"
FLUTTER_APPLICATION_INSTALL_SUFFIX = "scootui"
PUBSPEC_ENFORCE_LOCKFILE = "0"
PUBSPEC_IGNORE_LOCKFILE = "1"
FLUTTER_APPLICATION_PATH = ""

SYSTEMD_SERVICE:${PN} = "scootui.service"

do_install:append:librescoot-dbc() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/scootui.service ${D}${systemd_system_unitdir}/
}

do_install:append:librescoot-rpi5() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/scootui-rpi5.service ${D}${systemd_system_unitdir}/scootui.service
}
