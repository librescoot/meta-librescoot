"""Native zstd is required for artifact selection and delta payload processing."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]


class UpdateServiceZstdRecipeTest(unittest.TestCase):
    def test_zstd_is_an_unconditional_runtime_dependency(self):
        recipe = (ROOT / 'recipes-core/update-service/update-service.bb').read_text()
        self.assertRegex(recipe, r'(?m)^RDEPENDS:\$\{PN\} \+= "zstd"$')


if __name__ == '__main__':
    unittest.main()
