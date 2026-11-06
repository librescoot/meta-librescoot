"""Experimental decoder packaging contract."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]


class HDiffPatchRecipeTest(unittest.TestCase):
    def test_decoder_is_pinned_and_installed(self):
        recipe = (ROOT / 'recipes-core/hdiffpatch/hdiffpatch_5.1.3.bb').read_text()
        self.assertIn('3b9dca715ca492873bf2c49e22e5d5b7d2a78620', recipe)
        self.assertIn('DEPENDS = "zstd"', recipe)
        self.assertIn('oe_runmake hpatchz CC="${CC} ${LDFLAGS}"', recipe)
        self.assertIn('${D}${bindir}/hpatchz', recipe)

    def test_updater_uses_feature_branch_and_decoder(self):
        recipe = (ROOT / 'recipes-core/update-service/update-service.bb').read_text()
        self.assertIn('branch=feat/hdiffpatch', recipe)
        self.assertIn('RDEPENDS:${PN} += "hdiffpatch"', recipe)


if __name__ == '__main__':
    unittest.main()
