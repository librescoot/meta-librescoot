"""License declarations for original layer content and upstream exceptions."""
from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[1]
CC_LICENSE = 'CC-BY-NC-SA-4.0'
CC_CHECKSUM = 'file://${COMMON_LICENSE_DIR}/CC-BY-NC-SA-4.0;md5=277261f4a7c6c099822f8f5cb0180da0'
HELPERS = [
    'recipes-connectivity/dbc-netconfig/dbc-netconfig.bb',
    'recipes-connectivity/mdb-firewall/mdb-firewall.bb',
    'recipes-connectivity/mdb-netconfig/mdb-netconfig.bb',
    'recipes-connectivity/ppp-link/ppp-link.bb',
    'recipes-core/boot-led-service/boot-led-service.bb',
    'recipes-core/dbc-recovery/dbc-recovery.bb',
    'recipes-core/led-curve-loader/led-curve-loader.bb',
    'recipes-core/led-curves/led-curves.bb',
    'recipes-core/machine-id-init/machine-id-init.bb',
    'recipes-core/onboot-service/onboot-service.bb',
    'recipes-core/shell-config/shell-config.bb',
    'recipes-core/uboot-env-sync/uboot-env-sync.bb',
    'recipes-core/udev-rules-mdb/udev-rules-mdb.bb',
    'recipes-graphics/drm-holder/drm-holder.bb',
    'recipes-graphics/imx-overlay-alpha/imx-overlay-alpha.bb',
    'recipes-kernel/g-ether-conf/g-ether-conf.bb',
]


def license_of(path):
    return re.search(r'^LICENSE = "([^"]+)"', path.read_text(), re.M)[1]


class LicensePolicyTests(unittest.TestCase):
    def test_original_helpers_use_noncommercial_sharealike(self):
        for name in HELPERS:
            with self.subTest(recipe=name):
                path = ROOT / name
                self.assertEqual(license_of(path), CC_LICENSE)
                self.assertIn(CC_CHECKSUM, path.read_text())

    def test_image_definitions_use_layer_license(self):
        images = list((ROOT / 'recipes-fsl/images').glob('librescoot-*-image.bb'))
        self.assertEqual(len(images), 4)
        for image in images:
            with self.subTest(recipe=image.name):
                self.assertEqual(license_of(image), CC_LICENSE)

    def test_agpl_is_an_alternative_not_an_additional_restriction(self):
        for name, variant in [('data-server', 'only'), ('version-service', 'only'),
                              ('event-service', 'only'), ('modem-service', 'only'),
                              ('pm-service', 'only'), ('update-service', 'only'),
                              ('uplink-service', 'only')]:
            with self.subTest(recipe=name):
                path = ROOT / f'recipes-core/{name}/{name}.bb'
                self.assertEqual(license_of(path), f'(AGPL-3.0-{variant} | {CC_LICENSE})')
                self.assertIn('md5=eb1e647870add0502f8f010b19de32af', path.read_text())
                self.assertIn(CC_CHECKSUM, path.read_text())

    def test_cc_by_nc_oversights_are_corrected(self):
        for name in ('alarm-service', 'keycard-service', 'lsc'):
            self.assertEqual(license_of(ROOT / f'recipes-core/{name}/{name}.bb'), CC_LICENSE)
        animation = ROOT / 'recipes-graphics/boot-animation/boot-animation.bb'
        self.assertEqual(license_of(animation), CC_LICENSE)
        self.assertIn(CC_CHECKSUM, animation.read_text())

    def test_upstream_licenses_are_preserved(self):
        self.assertEqual(license_of(ROOT / 'recipes-graphics/carplay-service/carplay-service.bb'), 'MIT')
        self.assertEqual(license_of(ROOT / 'recipes-extended/fake-hwclock/fake-hwclock.bb'), 'GPL-2.0-only')
        boot = ROOT / 'recipes-bsp/boot-assets/boot-assets.bb'
        upstream = ROOT / 'recipes-bsp/u-boot/u-boot-imx_2017.03.bb'
        self.assertEqual(license_of(boot), license_of(upstream))
        self.assertIn('# SPDX-License-Identifier: ' + CC_LICENSE, boot.read_text())


if __name__ == '__main__':
    unittest.main()
