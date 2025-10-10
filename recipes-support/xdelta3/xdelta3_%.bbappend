DEPENDS += "xz"

EXTRA_OECONF += "--with-liblzma"

RDEPENDS:${PN} += "xz"
