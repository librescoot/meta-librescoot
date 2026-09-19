#!/usr/bin/env python3
"""Generate DBC U-Boot and selectable kernel-logo assets from PNG artwork."""
from pathlib import Path
from PIL import Image

ROOT = Path('/home/teal/src/librescoot/meta-librescoot')
DOWNLOADS = Path('/mnt/c/Users/Teal/Downloads')
KERNEL_LOGOS = ROOT / 'recipes-kernel/linux/linux-fslc/logo'
UBOOT_LOGO = ROOT / 'recipes-bsp/u-boot/u-boot-imx/dbc/uboot-logo.bmp'


def p3(image: Image.Image, output: Path) -> None:
    """pnmtologo only accepts ASCII PPM and at most 224 colours."""
    image = image.convert('RGB').quantize(colors=224, method=Image.Quantize.MEDIANCUT,
                                           dither=Image.Dither.FLOYDSTEINBERG).convert('RGB')
    colors = image.getcolors(224 * 224)
    if colors is None or len(colors) > 224:
        raise ValueError('kernel logo exceeds pnmtologo\'s 224-colour limit')
    with output.open('w', encoding='ascii', newline='\n') as f:
        f.write(f'P3\n{image.width} {image.height}\n255\n')
        for y in range(image.height):
            row = []
            for r, g, b in (image.getpixel((x, y)) for x in range(image.width)):
                row.append(f'{r} {g} {b}')
            f.write(' '.join(row) + '\n')


def main() -> None:
    notice = Image.open(DOWNLOADS / 'librescoot-uboot-bottom-notice.png').convert('RGB')
    if notice.size != (340, 85):
        raise ValueError(f'notice must be 340x85, got {notice.size}')

    # U-Boot's built-in bmp_logo tool accepts an uncompressed indexed BMP with
    # no more than 240 colours. Its display has no alpha; black is intentional.
    notice.quantize(colors=224, method=Image.Quantize.MEDIANCUT).save(UBOOT_LOGO, 'BMP')

    xp = Image.open(DOWNLOADS / '13a7b5c9-d8c4-4156-8849-3a97061586d6.png').convert('RGB')
    xp = xp.resize((480, 480), Image.Resampling.LANCZOS)
    # The notice export is flattened on black. Treat only exact black as
    # transparent for the kernel overlay, retaining its rendered notice.
    mask = notice.convert('L').point(lambda value: 0 if value == 0 else 255)
    xp.paste(notice, ((480 - notice.width) // 2, 480 - notice.height), mask)
    p3(xp, KERNEL_LOGOS / 'logo_alt1_clut224.ppm')
    # librescoot-xp deliberately shares the exact Windows XP kernel splash.
    p3(xp, KERNEL_LOGOS / 'logo_alt2_clut224.ppm')

    coopertino = Image.open(DOWNLOADS / 'boot-librescoopertino.png').convert('RGBA')
    background = Image.new('RGBA', coopertino.size, 'black')
    background.alpha_composite(coopertino)
    p3(background.convert('RGB'), KERNEL_LOGOS / 'logo_alt3_clut224.ppm')


if __name__ == '__main__':
    main()
