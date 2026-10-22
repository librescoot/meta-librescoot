"""Execute data-server's install task against a temporary package root."""
import os
from pathlib import Path
import re
import shlex
import subprocess
import tempfile
import unittest

RECIPE_DIR = Path(__file__).resolve().parents[1]
RECIPE = RECIPE_DIR / "data-server.bb"
ADDRESSES = {"unu-mdb": "192.168.7.1:8080", "unu-dbc": "192.168.7.2:8080",
             "librescoot-dbc-rpi4": "192.168.7.2:8080"}


def install_unit(root, machine):
    text = RECIPE.read_text()
    assignments = dict(re.findall(r'^DATA_SERVER_ADDR(?::([\w-]+))? = "([^"]+)"$', text, re.M))
    address = assignments.get(machine, assignments[""])
    build = root / "build"
    binary = build / "bin/linux_arm/data-server"
    binary.parent.mkdir(parents=True)
    binary.write_text("#!/bin/sh\nexit 0\n")
    binary.chmod(0o755)
    dest = root / "image"
    task = re.search(r'^do_install\(\) \{\n(.*?)^\}', text, re.M | re.S)
    if task is None:
        raise AssertionError("missing do_install")
    # BitBake expands this value even inside the sed expression's quotes.
    script = task[1].replace("${DATA_SERVER_ADDR}", address)
    subprocess.run(["sh", "-eu", "-c", script], check=True, env={
        **os.environ, "D": str(dest), "B": str(build),
        "UNPACKDIR": str(RECIPE_DIR / "files"), "bindir": "/usr/bin",
        "systemd_system_unitdir": "/usr/lib/systemd/system", "DATA_SERVER_ADDR": address,
    })
    return dest / "usr/lib/systemd/system/librescoot-data-server.service"


def listen_args(unit):
    command = re.search(r'^ExecStart=(.*)$', unit.read_text(), re.M)
    if command is None:
        raise AssertionError("missing ExecStart")
    return shlex.split(command[1])


class PackagingTests(unittest.TestCase):
    def test_board_units(self):
        recipe = RECIPE.read_text()
        self.assertIn('PACKAGE_ARCH = "${MACHINE_ARCH}"', recipe)
        for machine, address in ADDRESSES.items():
            with self.subTest(machine=machine), tempfile.TemporaryDirectory() as tmp:
                unit = install_unit(Path(tmp), machine)
                self.assertEqual(listen_args(unit), ["/usr/bin/data-server", "-addr", address])
                self.assertEqual(unit.stat().st_mode & 0o777, 0o644)
                text = unit.read_text()
                self.assertNotIn("@DATA_SERVER_ADDR@", text)
                self.assertRegex(text, r'(?m)^Requires=.*\blibrescoot-netconfig\.service\b')
                self.assertRegex(text, r'(?m)^After=.*\blibrescoot-netconfig\.service\b')
                self.assertIn("Restart=on-failure", text)
                expected = "mdb-netconfig" if machine == "unu-mdb" else "dbc-netconfig"
                self.assertIn(f'RDEPENDS:${{PN}}:append:{machine} = " {expected}"', recipe)

    def test_unknown_machine_defaults_to_loopback(self):
        with tempfile.TemporaryDirectory() as tmp:
            unit = install_unit(Path(tmp), "test-unknown")
            self.assertEqual(listen_args(unit), ["/usr/bin/data-server", "-addr", "127.0.0.1:8080"])


if __name__ == "__main__":
    unittest.main()
