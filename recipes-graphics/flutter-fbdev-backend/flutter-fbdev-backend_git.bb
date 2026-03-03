#
# Flutter Embedder with FBDEV Backend.
# GPU rendering via render node, display via /dev/fb0.
#

SUMMARY = "Embedded Linux embedding for Flutter (FBDEV backend)"
AUTHOR = "LibreScoot contributors"
HOMEPAGE = "https://github.com/sony/flutter-embedded-linux"
SECTION = "graphics"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://LICENSE;md5=d45359c88eb146940e4bede4f08c821a"

DEPENDS += "\
    flutter-engine \
    libinput \
    libxkbcommon \
    virtual/egl \
    virtual/libgbm \
    "

RDEPENDS:${PN} += " \
    xkeyboard-config \
    libxkbcommon \
    flutter-engine \
    ${@bb.utils.contains('DISTRO_FEATURES', 'systemd', '', 'libuv', d)} \
    "

REQUIRED_DISTRO_FEATURES += "opengl"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_REPO ??= "github.com/sony/flutter-embedded-linux.git"
SRC_REPO_BRANCH ??= "master"

SRC_URI = "git://${SRC_REPO};protocol=https;branch=${SRC_REPO_BRANCH}"
SRC_URI += "file://0001-add-fbdev-backend.patch"

SRCREV ??= "1653fa656bf9fe9fa5f84789a59b0c07671a8fc8"

S = "${WORKDIR}/git"

inherit pkgconfig cmake features_check

require conf/include/flutter-version.inc

FLUTTER_SDK_TAG = "3.32.5"

PACKAGECONFIG:append = " ${@bb.utils.contains('DISTRO_FEATURES', 'systemd', 'systemd', 'libuv', d)}"
PACKAGECONFIG[systemd] = ",,systemd,libuv"
PACKAGECONFIG[libuv] = ",,libuv"

EXTRA_OECMAKE += "-D USER_PROJECT_PATH=${S}/examples/${PN}"

do_configure:prepend() {
    rm -rf ${S}/build || true
    mkdir -p ${S}/build || true

    FLUTTER_RUNTIME_MODES="$(ls ${STAGING_DIR_TARGET}${datadir}/flutter/${FLUTTER_SDK_VERSION}/)"

    for FLUTTER_RUNTIME_MODE in $FLUTTER_RUNTIME_MODES; do
        if [ "${FLUTTER_RUNTIME_MODE}" = "release" ]; then
            break
        elif [ "${FLUTTER_RUNTIME_MODE}" = "jit_release" ]; then
            break
        elif [ "${FLUTTER_RUNTIME_MODE}" = "profile" ]; then
            break
        elif [ "${FLUTTER_RUNTIME_MODE}" = "debug" ]; then
            break
        fi
    done

    FLUTTER_ENGINE_PATH=${STAGING_DIR_TARGET}${datadir}/flutter/${FLUTTER_SDK_VERSION}/${FLUTTER_RUNTIME_MODE}

    ln -sf ${FLUTTER_ENGINE_PATH}/lib/libflutter_engine.so ${S}/build/libflutter_engine.so
}

do_install() {
    install -D -m0755 ${B}/flutter-fbdev-backend \
        ${D}${bindir}/flutter-fbdev-backend
}

FILES:${PN} = "${bindir}"

do_package_qa[noexec] = "1"
EXCLUDE_FROM_SHLIBS = "1"

python () {
    d.setVar('FLUTTER_SDK_VERSION', get_flutter_sdk_version(d))
}
