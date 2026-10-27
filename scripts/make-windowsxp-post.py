#!/usr/bin/env python3
# SPDX-License-Identifier: CC-BY-NC-SA-4.0

"""Generate the Windows XP kernel splash and its one-shot Lottie POST intro."""

import argparse
import base64
import gzip
import json
from pathlib import Path
import struct
import zlib

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
COLUMNS, ROWS = 60, 30
GRAY, WHITE, DIM = (170, 170, 170), (255, 255, 255), (110, 110, 110)
COPYRIGHT = '© 2024-2026 librescoot project'
EARLY_LINES = 5
POST = [
    ('Processor initialization', 'OK'),
    ('CPU', 'ARM Cortex-A9'),
    ('Memory controller', 'OK'),
    ('Memory check', '1048576K OK'),
    ('Display panel', '480x480 RGB565'),
    ('Framebuffer console', '60x30 TEXT'),
    ('Boot media', 'DETECTED'),
    ('Kernel image', 'LOADED'),
    ('Root filesystem', 'MOUNTED'),
    ('USB controller', 'INITIALIZED'),
    ('Internal board link', 'STARTING'),
    ('Persistent storage', 'CHECKED'),
    ('Service manager', 'STARTING'),
    ('System datastore', 'STARTING'),
    ('Settings storage', 'READY'),
    ('Graphics subsystem', 'STARTING'),
    ('Dashboard assets', 'FOUND'),
    ('Display services', 'STARTING'),
    ('Dashboard', 'PENDING'),
]


def load_font(path):
    data = gzip.decompress(path.read_bytes())
    if data[:2] != b'\x36\x04' or data[3] != 16 or not data[2] & 2:
        raise ValueError('font must be an 8x16 PSF1 font with Unicode mappings')
    count = 512 if data[2] & 1 else 256
    bitmaps = data[4:4 + count * 16]
    mapping = {}
    index = 0
    sequence = False
    for (value,) in struct.iter_unpack('<H', data[4 + count * 16:]):
        if value == 0xffff:
            index += 1
            sequence = False
        elif value == 0xfffe:
            sequence = True
        elif not sequence:
            mapping.setdefault(chr(value), index)
    return bitmaps, mapping


def render(font, flame, visible=EARLY_LINES, complete=False):
    bitmaps, mapping = font
    image = Image.new('RGB', (COLUMNS * 8, ROWS * 16), 'black')

    def text(column, row, value, color=GRAY):
        assert 0 <= row < ROWS and 0 <= column and column + len(value) <= COLUMNS
        for offset, char in enumerate(value):
            glyph = mapping[char]
            for y in range(16):
                bits = bitmaps[glyph * 16 + y]
                for x in range(8):
                    if bits & (0x80 >> x):
                        image.putpixel(((column + offset) * 8 + x, row * 16 + y), color)

    text(13, 1, 'Librescoot System BIOS', WHITE)
    text(13, 2, 'NXP i.MX6 DualLite')
    text(13, 3, 'Boot Revision 1.0')
    text(13, 4, 'Power-On Self Test')
    text(2, 5, '-' * 56, DIM)
    for offset, (label, status) in enumerate(POST[:visible]):
        row = 6 + offset
        text(2, row, label)
        begin = 3 + len(label)
        text(begin, row, '.' * (37 - begin), DIM)
        text(39, row, status, WHITE)
    if complete:
        text(2, 26, 'Boot sequence complete. Starting Librescoot...', WHITE)
    text(2, 27, '-' * 56, DIM)
    text(2, 28, COPYRIGHT)
    text(2, 29, 'non-commercial use only')
    text(36, 29, 'librescoot.org/license')
    image.paste(flame, (12, 6))
    assert len(set(image.getdata())) <= 224
    return image


def png(image):
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data))

    width, height = image.size
    pixels = image.convert('RGB').tobytes()
    stride = width * 3
    raw = b''.join(b'\0' + pixels[y * stride:(y + 1) * stride] for y in range(height))
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 2, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b''))


def make_lottie(font, flame):
    end = 182
    assets = []
    layers = []

    def image_layer(name, image, row, start):
        assets.append({'id': name, 'w': image.width, 'h': image.height,
                       'u': '', 'p': 'data:image/png;base64,' + base64.b64encode(png(image)).decode(), 'e': 1})
        layers.insert(0, {'ty': 2, 'nm': name, 'refId': name, 'ip': start, 'op': end, 'st': 0,
                         'ks': {'o': {'a': 0, 'k': 100}, 'r': {'a': 0, 'k': 0},
                                'p': {'a': 0, 'k': [0, row * 16, 0]}, 'a': {'a': 0, 'k': [0, 0, 0]},
                                's': {'a': 0, 'k': [100, 100, 100]}}})

    image_layer('early-post', render(font, flame), 0, 0)
    for step, visible in enumerate(range(EARLY_LINES + 1, len(POST) + 1), 1):
        row = 6 + visible - 1
        image = render(font, flame, visible).crop((0, row * 16, 480, (row + 1) * 16))
        image_layer(f'post-{row}', image, row, step * 9)
    message = render(font, flame, len(POST), True).crop((0, 26 * 16, 480, 27 * 16))
    image_layer('starting-librescoot', message, 26, 135)
    layers.insert(0, {'ty': 1, 'nm': 'handoff-to-xp', 'sw': 480, 'sh': 480, 'sc': '#000000',
                      'ip': 170, 'op': end, 'st': 0,
                      'ks': {'o': {'a': 0, 'k': 100}, 'r': {'a': 0, 'k': 0},
                             'p': {'a': 0, 'k': [0, 0, 0]}, 'a': {'a': 0, 'k': [0, 0, 0]},
                             's': {'a': 0, 'k': [100, 100, 100]}}})
    for index, layer in enumerate(layers, 1):
        layer['ind'] = index
    return {'v': '5.7.5', 'fr': 100, 'ip': 0, 'op': end, 'w': 480, 'h': 480,
            'nm': 'Windows XP POST intro', 'assets': assets, 'layers': layers}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--font', type=Path, default=Path('/usr/share/consolefonts/Lat15-VGA16.psf.gz'))
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    font = load_font(args.font)
    flame = Image.open(ROOT / 'scripts/assets/windowsxp-flame.png').convert('RGB')
    flame.thumbnail((66, 78), Image.Resampling.NEAREST)
    static = render(font, flame)
    values = [' '.join(str(channel) for x in range(480) for channel in static.getpixel((x, y))) for y in range(480)]
    outputs = {
        ROOT / 'recipes-kernel/linux/linux-fslc/logo/logo_alt1_clut224.ppm':
            ('P3\n480 480\n255\n' + '\n'.join(values) + '\n').encode(),
        ROOT / 'recipes-graphics/boot-animation/files/windowsxp-post.json':
            (json.dumps(make_lottie(font, flame), separators=(',', ':')) + '\n').encode(),
    }
    for path, data in outputs.items():
        if args.check:
            if path.read_bytes() != data:
                raise SystemExit(f'generated asset differs: {path}')
        else:
            path.write_bytes(data)
        print(('verified ' if args.check else 'wrote ') + str(path.relative_to(ROOT)))


if __name__ == '__main__':
    main()
