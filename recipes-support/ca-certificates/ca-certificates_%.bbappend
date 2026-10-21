# Remove both trust paths for the compromised eMudhra intermediates before
# installation, including native/SDK bundle generation and target postinst.
# https://bugzilla.mozilla.org/show_bug.cgi?id=2083449
do_compile:append() {
    rm -f \
        "${B}/mozilla/emSign_ECC_Root_CA_-_G3.crt" \
        "${B}/mozilla/Comodo_AAA_Services_root.crt"
}
