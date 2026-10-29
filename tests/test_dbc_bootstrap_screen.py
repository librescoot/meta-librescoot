import subprocess
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
RECIPE = ROOT / 'recipes-core/dbc-bootstrap-screen/dbc-bootstrap-screen.bb'
FILES = RECIPE.parent / 'files'
IMAGE = ROOT / 'recipes-fsl/images/librescoot-dbc-minimal-image.bb'


class DbcBootstrapScreenTest(unittest.TestCase):
    def test_screen_is_readable_and_can_be_replaced(self):
        with tempfile.TemporaryDirectory() as directory:
            console = Path(directory) / 'tty1'
            subprocess.run(
                ['sh', str(FILES / 'dbc-bootstrap-screen.sh'), str(console)],
                check=True,
            )
            output = console.read_bytes()
            self.assertTrue(output.startswith(b'\x1b%G\x1b[?25l\x1b[2J\x1b[H'))
            self.assertIn(b'Follow the installer', output)
            self.assertIn(b'Keep power and cables connected', output)
            self.assertNotIn(b'login:', output)
            console.write_text('installer progress')
            self.assertEqual(console.read_text(), 'installer progress')

    def test_only_bootstrap_image_installs_screen_and_masks_display_getty(self):
        minimal = IMAGE.read_text()
        full = (IMAGE.parent / 'librescoot-dbc-image.bb').read_text()
        service = (FILES / 'dbc-bootstrap-screen.service').read_text()
        self.assertIn('    dbc-bootstrap-screen \\\n', minimal)
        self.assertNotIn('    dbc-bootstrap-screen \\\n', full)
        self.assertIn('do_rootfs[postfuncs] += "mask_bootstrap_getty_tty1"', minimal)
        self.assertIn('/systemd/system/getty@tty1.service', minimal)
        self.assertIn('WantedBy=multi-user.target', service)
        self.assertNotIn('Restart=', service)
        self.assertIn('SYSTEMD_AUTO_ENABLE:${PN} = "enable"', RECIPE.read_text())


if __name__ == '__main__':
    unittest.main()
