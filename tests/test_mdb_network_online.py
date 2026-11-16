import re
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
IMAGES = ROOT / 'recipes-fsl/images'


class MdbNetworkOnlineTest(unittest.TestCase):
    def test_postprocess_removes_only_the_mdb_networkd_waiter(self):
        recipe = (IMAGES / 'librescoot-mdb-image.bb').read_text()
        match = re.search(r'^strip_mdb_networkd_wait_online_enable\(\) \{\n(.*?)^\}',
                          recipe, re.MULTILINE | re.DOTALL)
        self.assertIsNotNone(match)
        self.assertIn('do_rootfs[postfuncs] += "strip_mdb_networkd_wait_online_enable"', recipe)
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            wants = root / 'etc/systemd/system/network-online.target.wants'
            wants.mkdir(parents=True)
            units = root / 'usr/lib/systemd/system'
            units.mkdir(parents=True)
            for name in ('systemd-networkd-wait-online.service', 'NetworkManager-wait-online.service'):
                (units / name).write_text('[Service]\nExecStart=/bin/true\n[Install]\nWantedBy=network-online.target\n')
            networkd = wants / 'systemd-networkd-wait-online.service'
            nm = wants / 'NetworkManager-wait-online.service'
            networkd.symlink_to('/usr/lib/systemd/system/systemd-networkd-wait-online.service')
            nm.symlink_to('/usr/lib/systemd/system/NetworkManager-wait-online.service')
            def enabled(name):
                return subprocess.run(['systemctl', '--root', str(root), 'is-enabled', name],
                                      capture_output=True, text=True).stdout.strip()
            self.assertEqual(enabled(networkd.name), 'enabled')
            self.assertEqual(enabled(nm.name), 'enabled')
            script = match.group(1).replace('${IMAGE_ROOTFS}', str(root)).replace('${sysconfdir}', '/etc')
            subprocess.run(['bash', '-eu', '-c', script], check=True)
            self.assertFalse(networkd.is_symlink())
            self.assertTrue(nm.is_symlink())
            self.assertEqual(enabled(networkd.name), 'disabled')
            self.assertEqual(enabled(nm.name), 'enabled')
            subprocess.run(['bash', '-eu', '-c', script], check=True)
        dbc = (IMAGES / 'librescoot-dbc-image.bb').read_text()
        self.assertNotIn('strip_mdb_networkd_wait_online_enable', dbc)


if __name__ == '__main__':
    unittest.main()
