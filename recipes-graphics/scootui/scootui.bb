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

PV = "0.4.12+git"
# PR = "r0"

inherit flutter-app systemd

S = "${WORKDIR}/git"

PUBSPEC_APPNAME = "scooter_cluster"
FLUTTER_APPLICATION_INSTALL_SUFFIX = "scootui"
PUBSPEC_ENFORCE_LOCKFILE = "0"
PUBSPEC_IGNORE_LOCKFILE = "1"
FLUTTER_APPLICATION_PATH = ""

SYSTEMD_SERVICE:${PN} = "scootui.service"

do_install:append() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/scootui.service ${D}${systemd_system_unitdir}/
}
