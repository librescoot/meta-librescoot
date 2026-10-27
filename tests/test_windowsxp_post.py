# SPDX-License-Identifier: CC-BY-NC-SA-4.0

import base64
import json
import os
from pathlib import Path
import struct
import subprocess
import tempfile
import unittest
import zlib

ROOT = Path(__file__).resolve().parents[1]
FILES = ROOT / 'recipes-graphics/boot-animation/files'


def png_rgb(encoded):
    data = base64.b64decode(encoded.split(',', 1)[1])
    assert data[:8] == b'\x89PNG\r\n\x1a\n'
    position = 8
    compressed = bytearray()
    while position < len(data):
        size, = struct.unpack_from('>I', data, position)
        kind = data[position + 4:position + 8]
        payload = data[position + 8:position + 8 + size]
        if kind == b'IHDR':
            width, height, depth, color, compression, filtering, interlace = struct.unpack('>IIBBBBB', payload)
            assert (depth, color, compression, filtering, interlace) == (8, 2, 0, 0, 0)
        elif kind == b'IDAT':
            compressed.extend(payload)
        position += size + 12
    raw = zlib.decompress(compressed)
    stride = width * 3 + 1
    assert len(raw) == height * stride
    assert all(raw[y * stride] == 0 for y in range(height))
    return width, height, b''.join(raw[y * stride + 1:(y + 1) * stride] for y in range(height))


class WindowsXPPostTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.intro = json.loads((FILES / 'windowsxp-post.json').read_text())
        tokens = (ROOT / 'recipes-kernel/linux/linux-fslc/logo/logo_alt1_clut224.ppm').read_text().split()
        assert tokens[:4] == ['P3', '480', '480', '255']
        cls.pixels = bytes(int(value) for value in tokens[4:])
        cls.assets = {asset['id']: asset for asset in cls.intro['assets']}

    def test_native_geometry_and_palette(self):
        self.assertEqual(len(self.pixels), 480 * 480 * 3)
        self.assertLessEqual(len(set(zip(self.pixels[0::3], self.pixels[1::3], self.pixels[2::3]))), 224)
        self.assertEqual((self.intro['w'], self.intro['h'], self.intro['fr']), (480, 480, 100))
        self.assertEqual(set(self.pixels[176 * 480 * 3:432 * 480 * 3]), {0})

    def test_first_animated_frame_matches_kernel(self):
        width, height, pixels = png_rgb(self.assets['early-post']['p'])
        self.assertEqual((width, height), (480, 480))
        self.assertEqual(pixels, self.pixels)
        first = [layer for layer in self.intro['layers'] if layer['ip'] == 0]
        self.assertEqual([layer['refId'] for layer in first], ['early-post'])

    def test_post_rows_are_native_and_leave_footer_alone(self):
        rows = [layer for layer in self.intro['layers'] if layer['nm'].startswith('post-')]
        self.assertEqual(len(rows), 14)
        for layer in rows:
            row = int(layer['nm'].split('-')[1])
            self.assertEqual(layer['ks']['p']['k'], [0, row * 16, 0])
            self.assertEqual(layer['ks']['s']['k'], [100, 100, 100])
            width, height, _ = png_rgb(self.assets[layer['refId']]['p'])
            self.assertEqual((width, height), (480, 16))
            self.assertGreaterEqual(row, 11)
            self.assertLessEqual(row, 24)
        self.assertEqual(sorted(layer['ip'] for layer in rows), list(range(9, 127, 9)))
        black = self.intro['layers'][0]
        self.assertEqual((black['nm'], black['ip'], black['op']), ('handoff-to-xp', 170, 182))

    def test_recipe_installs_nonlooping_intro_and_fallback(self):
        recipe = (FILES.parent / 'boot-animation.bb').read_text()
        self.assertIn('file://windowsxp-post.json', recipe)
        self.assertIn('${B}/windowsxp-post.lsba\n', recipe)
        self.assertNotIn('${B}/windowsxp-post.lsba --loop', recipe)
        self.assertIn('install -m 0644 ${UNPACKDIR}/windowsxp-post.json', recipe)
        self.assertIn('install -m 0644 ${B}/windowsxp-post.lsba', recipe)
        self.assertIn('${B}/check-stream ${B}/windowsxp-post.lsba', recipe)

    def test_launcher_adds_intro_only_to_windowsxp(self):
        source = (FILES / 'boot-animation-launch.sh').read_text()
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            commandline = root / 'cmdline'
            binary = root / 'player'
            binary.write_text('#!/bin/sh\nprintf "%s\\n" "$@"\n')
            binary.chmod(0o755)
            firmware = root / 'fw_printenv'
            firmware.write_text('#!/bin/sh\nprintf "1\\n"\n')
            firmware.chmod(0o755)
            launch = root / 'launch'
            launch.write_text(source.replace('/proc/cmdline', str(commandline))
                              .replace('/usr/bin/boot-animation', str(binary))
                              .replace('/usr/share/boot-animation', str(root)))
            environment = dict(os.environ, PATH=str(root) + ':/usr/bin:/bin')
            for name in ('windowsxp', 'librescoot', 'librescoot-xp', 'coopertino'):
                commandline.write_text('quiet boot.animation=' + name)
                args = subprocess.check_output(['/bin/sh', str(launch)], env=environment, text=True).splitlines()
                if name == 'windowsxp':
                    self.assertEqual(args[:3], ['--intro', str(root / 'windowsxp-post.json'), str(root / 'windowsxp.json')])
                else:
                    self.assertNotIn('--intro', args)
                    self.assertEqual(args[0], str(root / (name + '.json')))
                self.assertEqual('--once' in args, name == 'librescoot')


if __name__ == '__main__':
    unittest.main()
