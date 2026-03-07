SUMMARY = "scootui"
DESCRIPTION = "ScootUI"
AUTHOR = "Danylo Storozhev, André Bierlein, Teal Bauer"
HOMEPAGE = "None"
BUGTRACKER = "None"
SECTION = "graphics"

LICENSE = "CC-BY-NC-SA-4.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=fb5d051e53001fdff7fec0f368f47190"

BBINCLUDELOGS = "yes"

SCOOTUI_BRANCH ??= "main"
SCOOTUI_SRCREV ??= "${AUTOREV}"
SRCREV = "${SCOOTUI_SRCREV}"
SRC_URI = "git://github.com/librescoot/scootui.git;lfs=0;branch=${SCOOTUI_BRANCH};protocol=https;destsuffix=git"
SRC_URI += "file://scootui.service"
SRC_URI += "file://scootui-rpi4.service"

PV = "0.4.15+git"
# PR = "r0"

inherit flutter-app systemd

S = "${WORKDIR}/git"

PUBSPEC_APPNAME = "scooter_cluster"
FLUTTER_APPLICATION_INSTALL_SUFFIX = "scootui"
PUBSPEC_ENFORCE_LOCKFILE = "0"
PUBSPEC_IGNORE_LOCKFILE = "1"
FLUTTER_APPLICATION_PATH = ""
FLUTTER_BUILD_ARGS = "bundle --no-pub"

SYSTEMD_SERVICE:${PN} = "scootui.service"
SYSTEMD_AUTO_ENABLE:${PN} = "disable"

do_install:append:unu-dbc() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/scootui.service ${D}${systemd_system_unitdir}/
}

do_install:append:librescoot-dbc-rpi4() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/scootui-rpi4.service ${D}${systemd_system_unitdir}/scootui.service
}
