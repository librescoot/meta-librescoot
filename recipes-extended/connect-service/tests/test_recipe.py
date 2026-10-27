import os
import re
import stat
import subprocess
import tempfile
import unittest
from pathlib import Path


RECIPE = Path(__file__).resolve().parents[1] / "connect-service.bb"
LAYER = RECIPE.parents[2]


class ConnectPackagingTest(unittest.TestCase):
    def test_only_cli_is_built_without_service_startup(self):
        recipe = RECIPE.read_text()
        self.assertIn('GO_INSTALL = "${GO_IMPORT}/cmd/connect"', recipe)
        self.assertIn('export CGO_ENABLED = "0"', recipe)
        self.assertIn('RDEPENDS:${PN} += "radio-gaga ca-certificates"', recipe)
        self.assertEqual(re.findall(r"^inherit (.+)$", recipe, re.M), ["librescoot-go"])
        self.assertNotIn("SYSTEMD", recipe)
        self.assertNotIn("pkg_postinst", recipe)
        images = LAYER / "recipes-fsl/images"
        included = [p.name for p in images.glob("*.bb") if "    connect-service \\\n" in p.read_text()]
        self.assertEqual(included, ["librescoot-mdb-image.bb"])

    def test_install_contains_only_connect(self):
        body = re.search(r"do_install\(\) \{(.*?)\n\}", RECIPE.read_text(), re.S)[1]
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            binaries = root / "build/bin/linux_arm"
            binaries.mkdir(parents=True)
            for name in ("connect", "connect-server", "connect-bouncer", "connect-poc-device"):
                (binaries / name).write_bytes(b"binary fixture")
            destination = root / "image"
            environment = dict(os.environ, B=str(root / "build"), D=str(destination), bindir="/usr/bin")
            subprocess.run(["sh", "-eu", "-c", body], env=environment, check=True)
            files = [str(p.relative_to(destination)) for p in destination.rglob("*") if p.is_file()]
            self.assertEqual(files, ["usr/bin/connect"])
            self.assertEqual(stat.S_IMODE((destination / "usr/bin/connect").stat().st_mode), 0o755)


if __name__ == "__main__":
    unittest.main()
