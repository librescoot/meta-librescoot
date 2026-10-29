"""Keep the MDB package budget within its existing Mender rootfs partition."""
from decimal import Decimal, ROUND_CEILING
from pathlib import Path
import os
import re
import shutil
import subprocess
import tempfile
import textwrap
import unittest
from unittest.mock import Mock, patch


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

    def headroom_check(self, free_blocks):
        recipe = (ROOT / 'recipes-fsl/images/librescoot-mdb-image.bb').read_text()
        self.assertIn('do_image_ext4[postfuncs] += "check_mdb_rootfs_headroom"', recipe)
        body = re.search(r'python check_mdb_rootfs_headroom\(\) \{\n(.*?)\n\}', recipe, re.S)[1]
        bb = Mock()
        bb.fatal.side_effect = RuntimeError
        data = Mock()
        data.getVar.side_effect = {'IMGDEPLOYDIR': '/unused', 'IMAGE_NAME': 'test'}.get
        fields = f'Free blocks: {free_blocks}\nReserved block count: 6553\nBlock size: 4096\n'
        with patch('subprocess.check_output', return_value=fields) as command:
            exec(textwrap.dedent(body), {'d': data, 'bb': bb})
        self.assertEqual(command.call_args.args[0], ['dumpe2fs', '-h', '/unused/test.ext4'])
        self.assertEqual(command.call_args.kwargs['env']['LC_ALL'], 'C')
        bb.note.assert_called_once()

    def test_space_gate_rejects_reserved_only_free_space(self):
        with self.assertRaises(RuntimeError):
            self.headroom_check(6507)

    def test_space_gate_rejects_less_than_minimum(self):
        with self.assertRaises(RuntimeError):
            self.headroom_check(6553 + 4096 - 1)

    def test_space_gate_accepts_minimum(self):
        self.headroom_check(6553 + 4096)

    @unittest.skipUnless(shutil.which('mkfs.ext4') and shutil.which('dumpe2fs'), 'requires e2fsprogs')
    def test_inode_geometry_preserves_reserve_and_recovers_space(self):
        recipe = (ROOT / 'recipes-fsl/images/librescoot-mdb-image.bb').read_text()
        self.assertNotIn('EXTRA_IMAGECMD:ext4:task-', recipe)
        ratio = int(re.search(r'^EXTRA_IMAGECMD:ext4 = "-i (\d+)"', recipe, re.M)[1])
        geometries = []
        with tempfile.TemporaryDirectory() as directory:
            for value in (4096, ratio):
                image = Path(directory) / f'rootfs-{value}.ext4'
                with image.open('xb') as stream:
                    stream.truncate(512 * 1024**2)
                subprocess.run(['mkfs.ext4', '-q', '-F', '-b', '4096', '-I', '256',
                                '-i', str(value), str(image)], check=True, capture_output=True)
                output = subprocess.check_output(['dumpe2fs', '-h', str(image)], text=True,
                                                 stderr=subprocess.DEVNULL, env=dict(os.environ, LC_ALL='C'))
                fields = dict(line.split(':', 1) for line in output.splitlines() if ':' in line)
                geometries.append({key: int(fields[key]) for key in
                                   ('Inode count', 'Block count', 'Reserved block count', 'Free blocks')})
        old, new = geometries
        self.assertEqual(new['Block count'], old['Block count'])
        self.assertEqual(new['Reserved block count'], old['Reserved block count'])
        self.assertEqual(new['Inode count'], 32768)
        self.assertGreater(new['Inode count'], 3 * 10000)
        self.assertGreaterEqual((new['Free blocks'] - old['Free blocks']) * 4096, 24 * 1024**2)

    def test_partition_layout_is_unchanged(self):
        machine = (ROOT / 'conf/machine/unu-mdb.conf').read_text()
        for variable, value in [('MENDER_BOOT_PART_SIZE_MB', '32'),
                                ('MENDER_DATA_PART_SIZE_MB', '256'),
                                ('MENDER_STORAGE_TOTAL_SIZE_MB', '1344')]:
            with self.subTest(variable=variable):
                self.assertRegex(machine, rf'(?m)^{variable} = "{value}"$')


if __name__ == '__main__':
    unittest.main()
