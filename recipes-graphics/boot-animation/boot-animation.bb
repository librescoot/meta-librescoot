SUMMARY = "Boot animation for LibreScoot DBC"
DESCRIPTION = "Renders Lottie JSON animations to /dev/fb0 using ThorVG"
HOMEPAGE = "https://github.com/librescoot/boot-animation"
LICENSE = "CC-BY-NC-4.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=3c4054a8416ddbc5debdf26a5359d962"

SRC_URI = "git://github.com/librescoot/boot-animation.git;protocol=https;branch=main"
SRC_URI += " \
    file://boot-animation.service \
    file://dbc-dispatcher-after-boot-animation.conf \
    file://00-shutdown-timeout.conf \
    file://librescoot.json \
    file://windowsxp.json \
"

SRCREV = "${AUTOREV}"
PV = "0.1.0+git"

S = "${WORKDIR}/git"

DEPENDS = "thorvg"

inherit systemd pkgconfig

SYSTEMD_SERVICE:${PN} = "boot-animation.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_compile() {
    ${CC} ${CFLAGS} ${LDFLAGS} \
        $(pkg-config --cflags thorvg-1) \
        -o ${B}/boot-animation ${S}/main.c \
        $(pkg-config --libs --static thorvg-1) \
        -lstdc++ -lm
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${B}/boot-animation ${D}${bindir}/boot-animation

    install -d ${D}${datadir}/boot-animation
    install -m 0644 ${WORKDIR}/librescoot.json ${D}${datadir}/boot-animation/
    install -m 0644 ${WORKDIR}/windowsxp.json ${D}${datadir}/boot-animation/

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/boot-animation.service ${D}${systemd_system_unitdir}/

    # dbc-dispatcher should wait for boot-animation to signal ready
    install -d ${D}${sysconfdir}/systemd/system/dbc-dispatcher.service.d
    install -m 0644 ${WORKDIR}/dbc-dispatcher-after-boot-animation.conf \
        ${D}${sysconfdir}/systemd/system/dbc-dispatcher.service.d/after-boot-animation.conf

    # Mask getty on tty1 to prevent console flash
    ln -sf /dev/null ${D}${sysconfdir}/systemd/system/getty@tty1.service

    # Reduce shutdown timeout for cleaner poweroff
    install -d ${D}${sysconfdir}/systemd/system.conf.d
    install -m 0644 ${WORKDIR}/00-shutdown-timeout.conf \
        ${D}${sysconfdir}/systemd/system.conf.d/00-shutdown-timeout.conf
}

FILES:${PN} += " \
    ${datadir}/boot-animation \
    ${sysconfdir}/systemd/system/dbc-dispatcher.service.d \
    ${sysconfdir}/systemd/system/getty@tty1.service \
    ${sysconfdir}/systemd/system.conf.d \
"
