SUMMARY = "scootui"
DESCRIPTION = "ScootUI"
AUTHOR = "Danylo Storozhev"
HOMEPAGE = "None"
BUGTRACKER = "None"
SECTION = "graphics"

LICENSE = "CLOSED"

SRCREV = "${AUTOREV}"
SRC_URI = "git://github.com/librescoot/scootui.git;lfs=0;branch=main;protocol=https;destsuffix=git"
SRC_URI += "file://scootui.service"

PV = "0.2.1+git"
## If a committed change results in changing the package output, then the value of the PR variable needs to be increased (or “bumped”) as part of that commit.
## For new recipes you should add the PR variable and set its initial value equal to “r0”, which is the default.
PR = "r1"

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
