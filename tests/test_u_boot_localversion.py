"""Keep Nightly release labels out of the flashed U-Boot image."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]


class UBootLocalVersionTests(unittest.TestCase):
    def test_local_version_is_stable_across_image_releases(self):
        recipe = (ROOT / 'recipes-bsp/u-boot/u-boot-imx_2017.03.bb').read_text()
        localversion = re.search(r'^LOCALVERSION:append = "([^"]+)"$', recipe, re.M)[1]
        self.assertEqual(localversion, '-librescoot-${MACHINE}')
        self.assertNotIn('DISTRO_VERSION', localversion)


if __name__ == '__main__':
    unittest.main()
