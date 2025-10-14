DESCRIPTION = "The crccheck.crc module implements all CRCs listed in the Catalogue of parametrised CRC algorithms"
HOMEPAGE = "https://github.com/MartinScharrer/crccheck"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE.txt;md5=e2ca8d795ae868acef6a54fb3e2e2044"

SRC_URI[sha256sum] = "1544c0110bf0a697d875d4f29dc40d7079f9d4d402a9317383f55f90ca72563a"

inherit pypi setuptools3

S = "${WORKDIR}/crccheck-${PV}"

do_configure:prepend() {
cat > ${S}/setup.py <<-EOF
from setuptools import setup

setup(
       name="${PYPI_PACKAGE}",
       version="${PV}",
       license="${LICENSE}",
)
EOF
}

BBCLASSEXTEND = "native nativesdk"
