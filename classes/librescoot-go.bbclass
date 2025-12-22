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

# Go needs network access to download modules during compile
do_compile[network] = "1"

# We prepend to do_compile to rebuild GOBUILDFLAGS with version info,
# then let the standard go_do_compile run
do_compile:prepend() {
    # Find the git directory
    GITDIR=""
    if [ -n "${GO_IMPORT}" ] && [ -d "${S}/src/${GO_IMPORT}/.git" ]; then
        GITDIR="${S}/src/${GO_IMPORT}"
    elif [ -d "${S}/.git" ]; then
        GITDIR="${S}"
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
        bbnote "librescoot-go: No .git directory found, trying B directory"
        # Check in B (build directory where go-mod puts sources)
        if [ -d "${B}/src/${GO_IMPORT}/.git" ]; then
            ORIGDIR="$(pwd)"
            cd "${B}/src/${GO_IMPORT}"
            VERSION=$(git describe --tags --always --dirty 2>/dev/null || echo "unknown")
            REVISION=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
            cd "$ORIGDIR"
            bbnote "librescoot-go: Found git version=$VERSION revision=$REVISION in ${B}/src/${GO_IMPORT}"
        else
            # Fallback: use SRCREV if available
            if [ -n "${SRCREV}" ] && [ "${SRCREV}" != "INVALID" ] && [ "${SRCREV}" != "AUTOINC" ]; then
                REVISION=$(echo "${SRCREV}" | cut -c1-8)
                VERSION="0.0.0-g$REVISION"
                bbnote "librescoot-go: Using SRCREV fallback version=$VERSION"
            fi
        fi
    fi

    # Save version for packaging
    echo "$VERSION" > ${WORKDIR}/librescoot-go-version

    # Build version ldflags - these match what the Makefile uses
    # Note: Using $VERSION not ${VERSION} to get shell variable expansion
    VERSION_LDFLAGS="-X main.version=$VERSION -X main.gitRevision=$REVISION"

    # Rebuild GOBUILDFLAGS with our version info injected
    # We need to insert VERSION_LDFLAGS into the ldflags string
    # Original GO_LDFLAGS format: -ldflags="${GO_RPATH} ${GO_LINKMODE} ${GO_LINUXLOADER} ${GO_EXTRA_LDFLAGS} -extldflags '${GO_EXTLDFLAGS}'"
    NEW_GO_LDFLAGS="-ldflags=${GO_RPATH} ${GO_LINKMODE} ${GO_LINUXLOADER} $VERSION_LDFLAGS -extldflags '${GO_EXTLDFLAGS}'"

    # Export the new GOBUILDFLAGS (this overrides the environment variable)
    export GOBUILDFLAGS="${GO_PARALLEL_BUILD} -v $NEW_GO_LDFLAGS -trimpath -modcacherw -buildmode=pie"

    bbnote "librescoot-go: VERSION_LDFLAGS=$VERSION_LDFLAGS"
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
