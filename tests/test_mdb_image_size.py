"""Keep the MDB package budget within its existing Mender rootfs partition."""
from decimal import Decimal, ROUND_CEILING
from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[1]


class MdbImageSizeTests(unittest.TestCase):
    def test_package_budget_does_not_expand_fixed_rootfs(self):
        recipe = (ROOT / 'recipes-fsl/images/librescoot-mdb-image.bb').read_text()
        factor = Decimal(re.search(r'^IMAGE_OVERHEAD_FACTOR = "([0-9.]+)"', recipe, re.M)[1])
        # Budget includes enrollment and the persistent SMS stack; units are KiB.
        package_budget = 440 * 1024
        partition_size = 512 * 1024
        padded_size = int((package_budget * factor).to_integral_value(rounding=ROUND_CEILING))
        self.assertGreaterEqual(factor, 1)
        self.assertEqual(max(partition_size, padded_size), partition_size)

    def test_partition_layout_is_unchanged(self):
        machine = (ROOT / 'conf/machine/unu-mdb.conf').read_text()
        for variable, value in [('MENDER_BOOT_PART_SIZE_MB', '32'),
                                ('MENDER_DATA_PART_SIZE_MB', '256'),
                                ('MENDER_STORAGE_TOTAL_SIZE_MB', '1344')]:
            with self.subTest(variable=variable):
                self.assertRegex(machine, rf'(?m)^{variable} = "{value}"$')


if __name__ == '__main__':
    unittest.main()
