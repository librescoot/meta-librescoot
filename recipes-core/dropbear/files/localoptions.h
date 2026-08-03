/* Pre-2020.79 Dropbear (stock unu, older librescoot images) can only do
   SHA-1 ssh-rsa signatures with the shipped RSA host key. Keep SHA-1
   enabled so upgraded and non-upgraded boards can still ssh to each
   other (the installer trampoline flashes the DBC over this link). */
#define DROPBEAR_RSA_SHA1 1

/* Legacy ssh-dss host/user keys, disabled by default upstream. */
#define DROPBEAR_DSS 1
