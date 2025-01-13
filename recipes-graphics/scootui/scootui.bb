SUMMARY = "scootui"
DESCRIPTION = "ScootUI"
AUTHOR = "Danylo Storozhev"
HOMEPAGE = "None"
BUGTRACKER = "None"
SECTION = "graphics"

LICENSE = "CLOSED"

SRCREV = "d541794e1881774ecdd2cb9c0887e83616994e27"
SRC_URI = "git://github.com/Zanooda/scootui-test;lfs=0;branch=main;protocol=https;destsuffix=git"
SRC_URI += "file://scootui.service"

S = "${WORKDIR}/git"

PUBSPEC_APPNAME = "scooter_cluster"
FLUTTER_APPLICATION_INSTALL_SUFFIX = "scootui"
PUBSPEC_IGNORE_LOCKFILE = "1"
FLUTTER_APPLICATION_PATH = ""

SYSTEMD_SERVICE:${PN} = "scootui.service"

do_install:append() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/scootui.service ${D}${systemd_system_unitdir}/
}

inherit flutter-app systemd
