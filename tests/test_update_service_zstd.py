"""Native zstd is required for artifact selection and delta payload processing."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]


class UpdateServiceZstdRecipeTest(unittest.TestCase):
    def test_zstd_is_an_unconditional_runtime_dependency(self):
        recipe = (ROOT / 'recipes-core/update-service/update-service.bb').read_text()
        self.assertRegex(recipe, r'(?m)^RDEPENDS:\$\{PN\} \+= "zstd"$')

    def test_v2_marker_is_only_copied_from_aware_updater_source(self):
        recipe = (ROOT / 'recipes-core/update-service/update-service.bb').read_text()
        source = '${B}/src/github.com/librescoot/update-service/packaging/ota-feed-v2'
        target = '${D}${datadir}/librescoot/ota-feed-v2'
        self.assertIn(f'if [ -f {source} ]; then', recipe)
        self.assertIn(f"printf 'v2\\nrelease-%s\\n' '${{LIBRESCOOT_VERSION}}' > {target}", recipe)
        self.assertIn(f'chmod 0644 {target}', recipe)


if __name__ == '__main__':
    unittest.main()
