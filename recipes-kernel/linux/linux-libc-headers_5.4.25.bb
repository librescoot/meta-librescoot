require recipes-kernel/linux-libc-headers/linux-libc-headers.inc

SRC_URI:append_libc-musl = "\
    file://0001-libc-compat.h-fix-some-issues-arising-from-in6.h.patch \
    file://0003-remove-inclusion-of-sysinfo.h-in-kernel.h.patch \
    file://0001-libc-compat.h-musl-_does_-define-IFF_LOWER_UP-DORMAN.patch \
    file://0001-include-linux-stddef.h-in-swab.h-uapi-header.patch \
   "

SRC_URI:append = "\
    file://0001-scripts-Use-fixed-input-and-output-files-instead-of-.patch \
    file://0001-kbuild-install_headers.sh-Strip-_UAPI-from-if-define.patch \
"

SRC_URI[md5sum] = "db3ed10e2dbf158409ee7a6c52f8c4dc"
SRC_URI[sha256sum] = "c0ed974b088d84847aeca0ab99943918c472739db4480b263e75e8c19a025e25"


