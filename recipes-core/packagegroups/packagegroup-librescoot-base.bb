SUMMARY = "LibreScoot base system packages"
DESCRIPTION = "Core system utilities, shells, and infrastructure shared by all LibreScoot targets"
LICENSE = "MIT"

inherit packagegroup

RDEPENDS:${PN} = " \
    packagegroup-core-base-utils \
    bash \
    dropbear \
    redis \
    chrony \
    chronyc \
    rsync \
    curl \
    iptables \
    tzdata \
    xdelta3 \
    firmwared \
    rpm \
    shell-config \
    libubootenv-bin \
    u-boot-default-env \
"
