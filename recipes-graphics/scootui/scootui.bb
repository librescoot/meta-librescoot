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
SRC_URI += "file://scootui-debug.service"
SRC_URI += "file://scootui-launch"

PV = "0.4.13+git"
# PR = "r0"

inherit flutter-app systemd

S = "${WORKDIR}/git"

PUBSPEC_APPNAME = "scooter_cluster"
FLUTTER_APPLICATION_INSTALL_SUFFIX = "scootui"
PUBSPEC_ENFORCE_LOCKFILE = "0"
PUBSPEC_IGNORE_LOCKFILE = "1"
FLUTTER_APPLICATION_PATH = ""

# Enable both debug and release runtime modes
FLUTTER_APP_RUNTIME_MODES = "debug release"

SYSTEMD_SERVICE:${PN} = "scootui.service"

do_install:append() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/scootui.service ${D}${systemd_system_unitdir}/
    install -m 0644 ${WORKDIR}/scootui-debug.service ${D}${systemd_system_unitdir}/
    
    # Install launcher script
    install -d ${D}${bindir}
    install -m 0755 ${WORKDIR}/scootui-launch ${D}${bindir}/
}
