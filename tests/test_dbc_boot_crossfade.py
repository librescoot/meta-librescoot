import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
GRAPHICS = ROOT / "recipes-graphics"


class DbcBootCrossfadeTest(unittest.TestCase):
    def test_overlay_patch_is_dbc_only(self):
        recipe = (ROOT / "recipes-kernel/linux/linux-fslc_%.bbappend").read_text()
        patch = "0004-drm-imx-add-live-boot-overlay-framebuffer.patch"
        self.assertEqual(recipe.count(patch), 1)
        dbc = recipe.split('SRC_URI:append:unu-dbc = "', 1)[1].split('"', 1)[0]
        self.assertIn(patch, dbc)
        text = (ROOT / "recipes-kernel/linux/linux-fslc" / patch).read_text()
        self.assertIn("+static int legacyfb_depth = 32;", text)
        self.assertIn("+\t.fb_destroy = imx_overlay_fb_destroy,", text)
        self.assertIn("+\tcancel_delayed_work_sync(&ofb->setup_work);", text)
        self.assertIn("+\tint ret = 0;", text)
        self.assertEqual(text.count("+backoff:\n+\tdrm_atomic_state_clear(state);\n+\tdrm_atomic_state_put(state);"), 2)

    def test_recipe_installs_complete_display_path(self):
        recipe = (GRAPHICS / "scootui-qt/scootui-qt.bb").read_text()
        self.assertIn('RDEPENDS:${PN}:append:unu-dbc = " qt-eglfs-fbdev imx-overlay-alpha"', recipe)
        self.assertIn("install -m 0755 ${UNPACKDIR}/scootui-qt-launch.sh", recipe)
        unit = (GRAPHICS / "scootui-qt/files/scootui-qt.service").read_text()
        self.assertIn("ExecStart=/usr/libexec/librescoot/scootui-qt-launch.sh", unit)
        plugin = (ROOT / "recipes-qt/qt6/qt-eglfs-fbdev.bb").read_text()
        self.assertIn('SRCREV = "8553f62498c5ead9330ac1e41bb5f6fd3b283010"', plugin)
        self.assertIn('PV = "0.1.1+git${SRCPV}"', plugin)
        self.assertIn("qt6-cmake", plugin)
        self.assertIn("LICENSE.CC-BY-NC-SA-4.0", plugin)

    def test_launcher_selects_overlay_or_kms(self):
        source = (GRAPHICS / "scootui-qt/files/scootui-qt-launch.sh").read_text()
        for device, alpha, helper_ok, expected in (
            (True, True, True, "eglfs_fbdev"),
            (False, True, True, "eglfs_kms"),
            (True, False, True, "eglfs_kms"),
            (True, True, False, None),
            (False, True, False, None),
        ):
            with self.subTest(device=device, alpha=alpha, helper_ok=helper_ok), tempfile.TemporaryDirectory() as tmp:
                root = Path(tmp)
                alpha_path = root / "alpha"
                if alpha:
                    alpha_path.write_text("255\n")
                helper = root / "helper"
                helper.write_text("#!/bin/sh\ntest \"$*\" = 0 || exit 2\nexit " + ("0" if helper_ok else "1") + "\n")
                helper.chmod(0o755)
                app = root / "app"
                app.write_text("#!/bin/sh\nprintf '%s\\n' \"$QT_QPA_EGLFS_INTEGRATION\" \"$*\"\n")
                app.chmod(0o755)
                launch = root / "launch"
                launch.write_text(source.replace("/dev/fb1", "/dev/null" if device else str(root / "absent"))
                                  .replace("/sys/class/graphics/fb1/overlay_alpha", str(alpha_path))
                                  .replace("/usr/bin/imx-overlay-alpha", str(helper))
                                  .replace("/usr/bin/scootui-qt", str(app)))
                result = subprocess.run(["sh", str(launch), "argument with spaces"], text=True, capture_output=True)
                if expected is None:
                    self.assertNotEqual(result.returncode, 0)
                    self.assertEqual(result.stdout, "")
                else:
                    self.assertEqual(result.returncode, 0, result.stderr)
                    self.assertEqual(result.stdout.splitlines(), [expected, "argument with spaces"])

    def test_alpha_helper_propagates_write_failures(self):
        source = GRAPHICS / "imx-overlay-alpha/files/imx-overlay-alpha.c"
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            test = root / "test.c"
            test.write_text(f'''#define main overlay_command_main
#include "{source}"
#undef main
#include <assert.h>
int main(void) {{
    FILE *file = tmpfile();
    assert(file);
    int fd = fileno(file);
    assert(do_fade(fd, 0, 255, 0) == 0);
    char buf[5] = {{0}};
    assert(pread(fd, buf, 4, 0) == 4);
    assert(strcmp(buf, "255\\n") == 0);
    assert(do_fade(fd, -1, 255, 0) < 0);
    assert(do_fade(fd, 0, 256, 0) < 0);
    assert(do_fade(fd, 0, 255, -1) < 0);
    fclose(file);
    assert(do_fade(-1, 0, 255, 0) < 0);
    return 0;
}}
''')
            binary = root / "test"
            subprocess.run([os.environ.get("CC", "cc"), "-Wall", "-Wextra", "-Werror", str(test), "-o", str(binary)], check=True)
            subprocess.run([str(binary)], check=True, capture_output=True)


if __name__ == "__main__":
    unittest.main()
