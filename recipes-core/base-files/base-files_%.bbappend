FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append = " file://librescoot-motd"

do_install:append() {
    # Write the greeting ourselves instead of leaving it to base-files'
    # do_install_basefilesissue, which strips $DATE out of DISTRO_VERSION. A
    # nightly version is "nightly-<date>T<time>", so losing the date makes two
    # nightlies built at the same time on different days indistinguishable in the
    # banner - the one thing the banner exists for. Releases are unaffected.
    #
    # /etc/issue.net is the pre-auth SSH banner (dropbear -b in the dropbear
    # bbappend). One plain line: dropbear does not expand escapes, and scripts
    # read it as well as people.
    printf '%s %s\n' "${DISTRO_NAME}" "${DISTRO_VERSION}" > ${D}${sysconfdir}/issue.net

    # /etc/issue is the same line on the serial console, where agetty does expand
    # escapes: keep \n (nodename) and \l (line) literal for it.
    printf '%s %s \\n \\l\n' "${DISTRO_NAME}" "${DISTRO_VERSION}" > ${D}${sysconfdir}/issue
    printf '\n' >> ${D}${sysconfdir}/issue

    # /etc/motd is printed by pam_motd on the console and by dropbear itself on
    # interactive SSH logins (dropbear's -m turns that off, ~/.hushlogin skips it).
    # Base-files ships it empty, which makes a board on a bench say nothing about
    # itself.
    {
        printf '%s %s - https://librescoot.org/docs\n' "${DISTRO_NAME}" "${DISTRO_VERSION}"
        cat ${UNPACKDIR}/librescoot-motd
    } > ${D}${sysconfdir}/motd
    chmod 0644 ${D}${sysconfdir}/motd
}
