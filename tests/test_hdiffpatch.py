"""Experimental decoder packaging contract."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]


class HDiffPatchRecipeTest(unittest.TestCase):
    def test_decoder_is_pinned_and_installed(self):
        recipe = (ROOT / 'recipes-core/hdiffpatch/hdiffpatch_5.1.3.bb').read_text()
        self.assertIn('3b9dca715ca492873bf2c49e22e5d5b7d2a78620', recipe)
        self.assertIn('DEPENDS = "zstd"', recipe)
        self.assertIn('oe_runmake -B hpatchz CC="${CC} ${LDFLAGS}"', recipe)
        self.assertIn('${D}${bindir}/hpatchz', recipe)

    def test_updater_adds_decoder_for_nightly_and_testing(self):
        recipe = (ROOT / 'recipes-core/update-service/update-service.bb').read_text()
        self.assertIn('branch=main', recipe)
        self.assertIn("(d.getVar('LIBRESCOOT_VERSION') or '').startswith(('nightly-', 'testing-'))", recipe)
        for version, expected in [('nightly-20261106T123456', True), ('testing-20261106T123456', True),
                                  ('v1.4.2', False), ('custom-nightly-20261106T123456-feature', False), ('', False)]:
            self.assertEqual(version.startswith(('nightly-', 'testing-')), expected, version)
        self.assertIn("' hdiffpatch'", recipe)


if __name__ == '__main__':
    unittest.main()
