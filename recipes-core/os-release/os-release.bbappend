# Use full MACHINE name as VARIANT_ID (e.g., unu-mdb, unu-dbc, librescoot-dbc-rpi5)
python() {
    machine = d.getVar('MACHINE')
    if machine:
        d.setVar('VARIANT_ID', machine)
        d.setVar('VARIANT', machine.upper())
}

# Add VARIANT and VARIANT_ID to os-release output
OS_RELEASE_FIELDS:append = " VARIANT VARIANT_ID"
