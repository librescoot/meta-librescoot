# Extract machine variant from MACHINE variable (e.g., librescoot-mdb -> mdb)
python() {
    machine = d.getVar('MACHINE')
    if machine and machine.startswith('librescoot-'):
        variant = machine.replace('librescoot-', '')
        d.setVar('VARIANT_ID', variant)
        d.setVar('VARIANT', variant.upper())
}

# Add VARIANT and VARIANT_ID to os-release output
OS_RELEASE_FIELDS:append = " VARIANT VARIANT_ID"
