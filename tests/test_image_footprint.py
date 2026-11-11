from pathlib import Path
import os
import re
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[1]


class ImageFootprintTests(unittest.TestCase):
    def test_full_images_install_core_python_not_every_module(self):
        for board in ("mdb", "dbc"):
            with self.subTest(board=board):
                recipe = (ROOT / f"recipes-fsl/images/librescoot-{board}-image.bb").read_text()
                packages = re.search(r'CORE_IMAGE_EXTRA_INSTALL \+= "(.*?)"', recipe, re.S)[1].split()
                packages = {package for package in packages if package != "\\"}
                self.assertIn("python3-core", packages)
                self.assertNotIn("python3", packages)

    def test_python_cryptography_omits_packaged_tests_and_docs(self):
        append = (ROOT / "recipes-devtools/python/python3-cryptography.bbappend").read_text()
        self.assertIn("do_install:append:class-target()", append)
        self.assertIn("${D}${PYTHON_SITEPACKAGES_DIR}/tests", append)
        self.assertIn("${D}${PYTHON_SITEPACKAGES_DIR}/docs", append)

    def test_other_python_package_tests_are_not_shipped(self):
        setuptools = (ROOT / "recipes-devtools/python/python3-setuptools_%.bbappend").read_text()
        self.assertIn("${D}${PYTHON_SITEPACKAGES_DIR}/setuptools/tests", setuptools)
        self.assertIn("${D}${PYTHON_SITEPACKAGES_DIR}/setuptools/_distutils/tests", setuptools)
        shapely = (ROOT / "recipes-devtools/python/python3-shapely_2.0.2.bb").read_text()
        self.assertIn("${D}${PYTHON_SITEPACKAGES_DIR}/shapely/tests", shapely)

    def test_geoclue_plugin_is_removed_only_from_default_dbc(self):
        append = (ROOT / "recipes-qt/qt6/qtpositioning_git.bbappend").read_text()
        body = re.search(r"librescoot_prune_geoclue_plugin\(\) \{\n(.*?)\n\}", append, re.S)[1]
        for distro, enabled, present in (("librescoot-dbc", "0", False),
                                         ("librescoot-dbc", "1", True),
                                         ("librescoot-mdb", "0", True)):
            with self.subTest(distro=distro, enabled=enabled), tempfile.TemporaryDirectory() as tmp:
                plugin = Path(tmp) / "usr/lib/plugins/position/libqtposition_geoclue2.so"
                plugin.parent.mkdir(parents=True)
                plugin.write_bytes(b"plugin")
                env = dict(os.environ, PKGD=tmp, libdir="/usr/lib", DISTRO=distro,
                           LIBRESCOOT_DBC_GEOCLUE_MODEM=enabled)
                subprocess.run(["sh", "-eu", "-c", body], env=env, check=True)
                self.assertEqual(plugin.exists(), present)

    def test_dbc_geoclue_does_not_require_a_modem(self):
        dbc = (ROOT / "conf/distro/librescoot-dbc.conf").read_text()
        self.assertIn('LIBRESCOOT_DBC_GEOCLUE_MODEM ?= "0"', dbc)
        self.assertIn("'LIBRESCOOT_DBC_GEOCLUE_MODEM', '1', '', '3g modem-gps cdma'", dbc)
        self.assertIn("'LIBRESCOOT_DBC_GEOCLUE_MODEM', '1', '', 'geoclue'", dbc)
        positioning = (ROOT / "recipes-qt/qt6/qtpositioning_git.bbappend").read_text()
        self.assertIn("PACKAGE_PREPROCESS_FUNCS:append:class-target", positioning)
        self.assertIn('"${DISTRO}" = "librescoot-dbc"', positioning)
        self.assertIn('"${LIBRESCOOT_DBC_GEOCLUE_MODEM}" != "1"', positioning)
        self.assertIn("libqtposition_geoclue2.so", positioning)
        image = (ROOT / "recipes-fsl/images/librescoot-dbc-image.bb").read_text()
        self.assertNotIn("python3-shapely", image)
        mdb = (ROOT / "conf/distro/librescoot-mdb.conf").read_text()
        self.assertNotIn("PACKAGECONFIG:remove:pn-geoclue", mdb)
        self.assertIn("modemmanager", (ROOT / "recipes-fsl/images/librescoot-mdb-image.bb").read_text())


if __name__ == "__main__":
    unittest.main()
