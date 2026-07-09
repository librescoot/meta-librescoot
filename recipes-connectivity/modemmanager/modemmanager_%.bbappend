# modem-service reads IMEI/ICCID and controls the SIMCom SIM7100E's GPS by
# sending AT commands through ModemManager's DBus Command() API, which upstream only
# permits in debug builds unless at_command_via_dbus is enabled. Our former
# pinned 1.24.0 recipe carried "at" in the default PACKAGECONFIG; keep the
# feature enabled now that the meta-oe recipe is used.
PACKAGECONFIG:append = " at"
