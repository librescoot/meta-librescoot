# The MDB target builds against linux-libc-headers 5.4.25 (OLDEST_KERNEL 5.4.24),
# which predates the mount FD API (fsopen/move_mount/mount_setattr, ~5.12).
# util-linux 2.41 enables libmount-mountfd-support by default and then fails
# configure ("required mount FDs based API not available"). Disable it for this
# target since the running kernel does not provide the API anyway.
PACKAGECONFIG:remove = "libmount-mountfd-support"
