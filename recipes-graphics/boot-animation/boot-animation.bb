SUMMARY = "Boot animation for Librescoot DBC"
DESCRIPTION = "Renders Lottie JSON animations to /dev/fb0 using ThorVG"
HOMEPAGE = "https://github.com/librescoot/boot-animation"
LICENSE = "CC-BY-NC-4.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=3c4054a8416ddbc5debdf26a5359d962"

SRC_URI = "git://github.com/librescoot/boot-animation.git;protocol=https;nobranch=1"
SRC_URI += " \
    file://boot-animation.service \
    file://boot-animation-launch.sh \
    file://dbc-dispatcher-after-boot-animation.conf \
    file://00-shutdown-timeout.conf \
    file://librescoot.json \
    file://windowsxp.json \
"

# v0.2.1: audio-independent 32-bpp stream and cadence fixes.
SRCREV = "722685a5fa94e05824de4e5596711b738e863fda"
PV = "0.2.1"

DEPENDS = "thorvg zlib thorvg-native zlib-native"

inherit systemd pkgconfig

SYSTEMD_SERVICE:${PN} = "boot-animation.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

# Panel geometry and playback rate the animations are packed for. A stream that
# does not match the framebuffer is ignored at runtime and the animation is
# rasterised live instead, so a machine with a different panel still boots with
# a splash, just the slow way.
BOOT_ANIMATION_WIDTH ?= "480"
BOOT_ANIMATION_HEIGHT ?= "480"
BOOT_ANIMATION_FPS ?= "25"

do_compile() {
    ${CC} ${CFLAGS} ${LDFLAGS} \
        $(pkg-config --cflags thorvg-1) \
        -I${S} \
        -o ${B}/boot-animation \
        ${S}/main.c ${S}/render_utils.c ${S}/signal_utils.c ${S}/stream_format.c \
        $(pkg-config --libs --static thorvg-1) \
        -lstdc++ -lm -lpthread -lz

    # Prerender the animations into frame streams. Rasterising Lottie costs
    # more per frame than the frame budget on the DBC, which stretches the
    # animation and steals CPU from the dashboard startup we are waiting on;
    # unpacking a frame is a decompress and a memcpy. The packer is a build
    # host binary, so it needs the native ThorVG and zlib.
    PKG_CONFIG_SYSROOT_DIR="" PKG_CONFIG_PATH="${STAGING_LIBDIR_NATIVE}/pkgconfig" \
    ${BUILD_CC} ${BUILD_CFLAGS} ${BUILD_LDFLAGS} \
        $(PKG_CONFIG_SYSROOT_DIR="" PKG_CONFIG_PATH="${STAGING_LIBDIR_NATIVE}/pkgconfig" pkg-config --cflags thorvg-1) \
        -I${S} \
        -o ${B}/lottie2stream ${S}/tools/lottie2stream.c ${S}/stream_format.c \
        $(PKG_CONFIG_SYSROOT_DIR="" PKG_CONFIG_PATH="${STAGING_LIBDIR_NATIVE}/pkgconfig" pkg-config --libs --static thorvg-1) \
        -lstdc++ -lm -lz

    # Exercise the dependency-free framebuffer/timing validation with the
    # native compiler during every image build.
    ${BUILD_CC} ${BUILD_CPPFLAGS} ${BUILD_CFLAGS} ${BUILD_LDFLAGS} \
        -D_POSIX_C_SOURCE=200809L -std=c11 -Wall -Wextra -Werror -I${S} \
        -o ${B}/test-render-utils \
        ${S}/tests/test_render_utils.c ${S}/render_utils.c \
        ${S}/signal_utils.c ${S}/stream_format.c -lz
    ${B}/test-render-utils

    ${BUILD_CC} ${BUILD_CPPFLAGS} ${BUILD_CFLAGS} ${BUILD_LDFLAGS} \
        -std=c11 -Wall -Wextra -Werror -I${S} \
        -o ${B}/check-stream ${S}/tests/check_stream.c \
        ${S}/stream_format.c -lz

    # librescoot plays once and holds its last frame; the others loop.
    ${B}/lottie2stream ${UNPACKDIR}/librescoot.json \
        ${BOOT_ANIMATION_WIDTH} ${BOOT_ANIMATION_HEIGHT} ${BOOT_ANIMATION_FPS} \
        ${B}/librescoot.lsba
    ${B}/lottie2stream ${UNPACKDIR}/windowsxp.json \
        ${BOOT_ANIMATION_WIDTH} ${BOOT_ANIMATION_HEIGHT} ${BOOT_ANIMATION_FPS} \
        ${B}/windowsxp.lsba --loop

    # Fail the image build if either packaged stream cannot be read back with
    # the production geometry/cadence. Runtime also validates the 32-bpp
    # framebuffer layout before selecting this RGB565 source.
    interval_ms=$(( (1000 + ${BOOT_ANIMATION_FPS} / 2) / ${BOOT_ANIMATION_FPS} ))
    ${B}/check-stream ${B}/librescoot.lsba \
        ${BOOT_ANIMATION_WIDTH} ${BOOT_ANIMATION_HEIGHT} "$interval_ms"
    ${B}/check-stream ${B}/windowsxp.lsba \
        ${BOOT_ANIMATION_WIDTH} ${BOOT_ANIMATION_HEIGHT} "$interval_ms"
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${B}/boot-animation ${D}${bindir}/boot-animation

    install -d ${D}${libexecdir}/librescoot
    install -m 0755 ${UNPACKDIR}/boot-animation-launch.sh ${D}${libexecdir}/librescoot/boot-animation-launch.sh

    # The JSON stays alongside the stream: it is what the launch script names,
    # and it is the fallback if a stream is ever unusable.
    install -d ${D}${datadir}/boot-animation
    install -m 0644 ${UNPACKDIR}/librescoot.json ${D}${datadir}/boot-animation/
    install -m 0644 ${UNPACKDIR}/windowsxp.json ${D}${datadir}/boot-animation/
    install -m 0644 ${B}/librescoot.lsba ${D}${datadir}/boot-animation/
    install -m 0644 ${B}/windowsxp.lsba ${D}${datadir}/boot-animation/

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/boot-animation.service ${D}${systemd_system_unitdir}/

    # dbc-dispatcher should wait for boot-animation to signal ready
    install -d ${D}${sysconfdir}/systemd/system/dbc-dispatcher.service.d
    install -m 0644 ${UNPACKDIR}/dbc-dispatcher-after-boot-animation.conf \
        ${D}${sysconfdir}/systemd/system/dbc-dispatcher.service.d/after-boot-animation.conf

    # Reduce shutdown timeout for cleaner poweroff
    install -d ${D}${sysconfdir}/systemd/system.conf.d
    install -m 0644 ${UNPACKDIR}/00-shutdown-timeout.conf \
        ${D}${sysconfdir}/systemd/system.conf.d/00-shutdown-timeout.conf
}

FILES:${PN} += " \
    ${datadir}/boot-animation \
    ${libexecdir}/librescoot \
    ${sysconfdir}/systemd/system/dbc-dispatcher.service.d \
    ${sysconfdir}/systemd/system.conf.d \
"
