# Class for LibreScoot Go services - versioning from git tags
#
# This class injects version information from git tags into Go binaries
# via ldflags, similar to what the Makefile does with:
#   -X main.version=$(git describe --tags --always --dirty)
#
# It also sets PV and PKGV to reflect the git-derived version.

inherit go-mod

# For PV, we use a git-based version from SRCPV
SRCPV = "${@bb.fetch2.get_srcrev(d)}"

# Use a PV format that includes git info
PV = "0.0+git${SRCPV}"

# PKGV will be set dynamically during do_package
PKGV = "${LIBRESCOOT_GO_VERSION}"
LIBRESCOOT_GO_VERSION ?= "${PV}"

# FIXME: Network access during do_compile is a Yocto anti-pattern.
# It breaks build reproducibility, offline builds, and hash equivalence.
# The correct fix is to vendor Go modules in each service repository:
#   cd <service-repo> && go mod vendor && git add vendor/
# Then go-mod.bbclass will use the vendored deps automatically and
# no network access is needed during compile.
#
# Until all service repos have vendored their modules, this line is
# required for the build to succeed. Remove it after vendoring.
do_compile[network] = "1"

# Override do_compile to inject version ldflags directly
# We can't use prepend because GOBUILDFLAGS is already expanded in go_do_compile
do_compile() {
    export TMPDIR="${GOTMPDIR}"

    # Find git directory and extract version
    GITDIR=""
    if [ -d "${S}/.git" ]; then
        GITDIR="${S}"
    elif [ -n "${GO_IMPORT}" ] && [ -d "${S}/src/${GO_IMPORT}/.git" ]; then
        GITDIR="${S}/src/${GO_IMPORT}"
    fi

    VERSION="unknown"
    REVISION="unknown"

    if [ -n "$GITDIR" ]; then
        ORIGDIR="$(pwd)"
        cd "$GITDIR"
        VERSION=$(git describe --tags --always --dirty 2>/dev/null || echo "unknown")
        REVISION=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
        cd "$ORIGDIR"
        bbnote "librescoot-go: Found git version=$VERSION revision=$REVISION in $GITDIR"
    else
        # Fallback: use SRCREV if available
        if [ -n "${SRCREV}" ] && [ "${SRCREV}" != "INVALID" ] && [ "${SRCREV}" != "AUTOINC" ]; then
            REVISION=$(echo "${SRCREV}" | cut -c1-8)
            VERSION="0.0.0-g$REVISION"
            bbnote "librescoot-go: Using SRCREV fallback version=$VERSION"
        fi
    fi

    # Save version for packaging
    echo "$VERSION" > ${WORKDIR}/librescoot-go-version

    # Build version ldflags
    VERSION_LDFLAGS="-X main.version=$VERSION -X main.gitRevision=$REVISION"

    # Build complete ldflags string matching go.bbclass format
    GO_LDFLAGS_FULL="${GO_RPATH} ${GO_LINKMODE} ${GO_LINUXLOADER} $VERSION_LDFLAGS -extldflags '${GO_EXTLDFLAGS}'"

    bbnote "librescoot-go: VERSION_LDFLAGS=$VERSION_LDFLAGS"

    if [ -n "${GO_INSTALL}" ]; then
        if [ -n "${GO_LINKSHARED}" ]; then
            ${GO} install ${GO_PARALLEL_BUILD} -v -ldflags="$GO_LDFLAGS_FULL" -trimpath -modcacherw `go_list_packages`
            rm -rf ${B}/bin
        fi
        ${GO} install ${GO_LINKSHARED} ${GO_PARALLEL_BUILD} -v -ldflags="$GO_LDFLAGS_FULL" -trimpath -modcacherw `go_list_packages`
    fi
}

# Read the computed version for packaging
python do_package:prepend() {
    import os
    version_file = os.path.join(d.getVar('WORKDIR'), 'librescoot-go-version')
    if os.path.exists(version_file):
        with open(version_file, 'r') as f:
            version = f.read().strip()
            if version and version != "unknown":
                d.setVar('PKGV', version)
                d.setVar('LIBRESCOOT_GO_VERSION', version)
                bb.note("librescoot-go: Package version set to %s" % version)
}
