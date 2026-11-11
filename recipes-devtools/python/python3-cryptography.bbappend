# The upstream wheel installs its source tree, including tests and docs.
do_install:append:class-target() {
    rm -rf ${D}${PYTHON_SITEPACKAGES_DIR}/tests ${D}${PYTHON_SITEPACKAGES_DIR}/docs
}
