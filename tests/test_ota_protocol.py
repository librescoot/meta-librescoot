"""Offline tests for the Mender updater capability advertisement."""
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


class ProtocolTests(unittest.TestCase):
    def test_only_capable_image_advertises_protocol(self):
        text = (ROOT / 'recipes-fsl/images/librescoot-ota-seed.inc').read_text()
        line = next(line for line in text.splitlines() if line.startswith('MENDER_ARTIFACT_EXTRA_ARGS:append'))
        expression = line.split(' = "', 1)[1][:-1]
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            marker = root / 'usr/share/librescoot-update/OTA_PROTOCOL'
            marker.parent.mkdir(parents=True)
            expression = expression.replace('${IMAGE_ROOTFS}', tmp).replace('${datadir}', '/usr/share')
            for version, expected in [(None, ''), ('1\n', ''), ('2\n', '--provides rootfs-image.librescoot-updater-protocol:2')]:
                if version is not None:
                    marker.write_text(version)
                result = subprocess.run(['bash', '-eu', '-c', 'printf "%s" "' + expression + '"'], text=True, capture_output=True, check=True)
                self.assertEqual(result.stdout.strip(), expected)

    def test_marker_comes_from_the_built_service(self):
        recipe = (ROOT / 'recipes-core/update-service/update-service.bb').read_text()
        self.assertIn('if [ -f ${B}/src/github.com/librescoot/update-service/OTA_PROTOCOL ]; then', recipe)
        self.assertIn('install -m 0644 ${B}/src/github.com/librescoot/update-service/OTA_PROTOCOL', recipe)


if __name__ == '__main__':
    unittest.main()
