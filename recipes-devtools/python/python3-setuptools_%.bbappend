do_install:append:class-target() {
    rm -rf ${D}${PYTHON_SITEPACKAGES_DIR}/setuptools/tests \
           ${D}${PYTHON_SITEPACKAGES_DIR}/setuptools/_distutils/tests \
           ${D}${PYTHON_SITEPACKAGES_DIR}/setuptools/_distutils/compilers/C/tests
}
