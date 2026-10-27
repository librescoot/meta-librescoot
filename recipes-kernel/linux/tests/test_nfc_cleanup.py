import re
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path


LINUX = Path(__file__).resolve().parents[1]
SOURCE = LINUX / "linux-fslc"
MDB = SOURCE / "mdb"
CLEANUP = MDB / "0006-nfc-pn5xx-demote-optional-gpio-warnings.patch"


class NfcCleanupTest(unittest.TestCase):
    def test_patch_order_and_only_optional_log_levels_change(self):
        recipe = (LINUX / "linux-fslc_%.bbappend").read_text()
        block = re.search(r'SRC_URI:append:unu-mdb = "(.*?)"', recipe, re.S)[1]
        patches = [SOURCE / p for p in re.findall(r"file://([^\s]+\.patch)", block)]
        base = next(p for p in patches if p.name.startswith("0004-nfc"))
        balance = next(p for p in patches if p.name.startswith("0005-nfc"))
        self.assertLess(patches.index(base), patches.index(balance))
        self.assertLess(patches.index(balance), patches.index(CLEANUP))
        self.assertNotIn(CLEANUP.name, recipe.split('SRC_URI:append:unu-dbc = "')[1].split('"')[0])
        body = base.read_text().split("+++ b/drivers/nfc/pn5xx/pn5xx_i2c.c\n", 1)[1].split("diff --git", 1)[0]
        original = "".join(line[1:] for line in body.splitlines(keepends=True) if line.startswith("+"))
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "drivers/nfc/pn5xx/pn5xx_i2c.c"
            source.parent.mkdir(parents=True)
            source.write_text(original)
            subprocess.run(["git", "apply", str(balance)], cwd=directory, check=True)
            before = source.read_text()
            subprocess.run(["git", "apply", str(CLEANUP)], cwd=directory, check=True)
            expected = before
            for name in ("FIRM", "CLKREQ"):
                old = f'dev_warn(dev, "{name} GPIO <OPTIONAL>'
                self.assertEqual(expected.count(old), 1)
                expected = expected.replace(old, f'dev_dbg(dev, "{name} GPIO <OPTIONAL>')
            self.assertEqual(source.read_text(), expected)
            for name in ("VEN", "IRQ"):
                self.assertIn(f'dev_err(dev, "{name} GPIO error', expected)

    @unittest.skipUnless(shutil.which("dtc") and shutil.which("fdtget"), "device-tree tools required")
    def test_compiled_supplies_preserve_reader_gpio_and_wake_configuration(self):
        with tempfile.TemporaryDirectory() as directory:
            dtb = Path(directory) / "mdb.dtb"
            subprocess.run(["dtc", "-I", "dts", "-O", "dtb", "-o", str(dtb),
                            str(SOURCE / "librescoot-mdb.dts")], check=True, capture_output=True)

            def get(node, prop):
                return subprocess.check_output(["fdtget", "-t", "x", str(dtb), node, prop], text=True).strip()

            regulator = "/regulators/regulator@2"
            handle = get(regulator, "phandle")
            self.assertEqual(get(regulator, "regulator-always-on"), "")
            props = subprocess.check_output(["fdtget", "-p", str(dtb), regulator], text=True).splitlines()
            self.assertNotIn("gpio", props)
            self.assertNotIn("gpios", props)
            readers = [("21a0000", "2b", "2d a 1", "2e 10 0", False),
                       ("21a4000", "28", "31 0 1", "2e 11 0", False),
                       ("21a8000", "29", "31 1 1", "2d 2 0", True)]
            for bus, address, enable, irq, wake in readers:
                node = f"/soc/aips-bus@2100000/i2c@{bus}/pn547@{address}"
                self.assertEqual(get(node, "enable-gpios"), enable)
                self.assertEqual(get(node, "interrupt-gpios"), irq)
                props = subprocess.check_output(["fdtget", "-p", str(dtb), node], text=True).splitlines()
                self.assertEqual("wakeup-source" in props, wake)
                self.assertNotIn("firmware-gpios", props)
                self.assertNotIn("nxp,pn54x-clkreq", props)
                for supply in ("vbat", "sevdd", "pvdd", "pmuvcc"):
                    self.assertEqual(get(node, f"nxp,pn54x-{supply}-supply"), handle)


if __name__ == "__main__":
    unittest.main()
