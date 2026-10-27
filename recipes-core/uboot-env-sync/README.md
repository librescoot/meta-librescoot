# Managed hostname migration

On MDB and DBC, the identity configuration initializes a missing or empty
`mender_pre_setup_commands` to the machine's CFG1 hostname command. It replaces
only the exact CFG0 command in that board's identity configuration. Other
commands, including modified versions of the old command, are preserved.
The sync runs after boot, so a changed command takes effect at the next boot.

The generated hostname retains its board prefix but its UID suffix changes.
Operators using the generated name in DNS, SSH configuration, monitoring, or
log searches should account for the new name. SSH host keys are not changed by
this migration, but a connection under a new name may need a known-hosts entry.
The `mdb` and `dbc` aliases in the shipped hosts files use fixed link addresses
and are not changed. Journal entries include the hostname, so searches grouped
by hostname can span both names after a rename.
