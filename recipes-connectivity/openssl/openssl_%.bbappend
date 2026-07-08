# pppd 2.5 loads OpenSSL's legacy provider (DES/MD4) at startup and exits
# without it. oe-core configures openssl with no-legacy by default, which
# leaves the openssl-ossl-module-legacy package empty and uninstallable —
# ppp-link RDEPENDS on it. Build the provider for target images.
PACKAGECONFIG:append:class-target = " legacy"
