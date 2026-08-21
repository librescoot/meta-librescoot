# modem-service reads IMEI/ICCID and controls the SIMCom SIM7100E's GPS by
# sending AT commands through ModemManager's DBus Command() API, which upstream only
# permits in debug builds unless at_command_via_dbus is enabled. Our former
# pinned 1.24.0 recipe carried "at" in the default PACKAGECONFIG; keep the
# feature enabled now that the meta-oe recipe is used.
PACKAGECONFIG:append = " at"

# Use-after-free in transaction_complete(): the transaction is removed from the
# hash table, whose GDestroyNotify frees it, and the next statement reads
# tr->completion_fn out of the freed block and calls it. Crashes ModemManager
# repeatedly during connect on the MDB, which costs cellular for the first
# ~100s of a boot. Drop this once it lands upstream.
FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"
SRC_URI += "file://0001-netlink-do-not-read-completion_fn-out-of-a-freed-tran.patch"
