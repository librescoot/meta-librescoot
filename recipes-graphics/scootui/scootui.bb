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

PV = "0.4.15+git"
# PR = "r0"

inherit flutter-app

S = "${WORKDIR}/git"

PUBSPEC_APPNAME = "scooter_cluster"
FLUTTER_APPLICATION_INSTALL_SUFFIX = "scootui"
PUBSPEC_ENFORCE_LOCKFILE = "0"
PUBSPEC_IGNORE_LOCKFILE = "1"
FLUTTER_APPLICATION_PATH = ""
