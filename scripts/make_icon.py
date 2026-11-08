import os, glob, sys
from PIL import Image, ImageDraw, ImageFont

S = 1024
FONTS = ['/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf',
         '/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf']

def font(sz):
    for f in FONTS:
        if os.path.exists(f):
            return ImageFont.truetype(f, sz)
    try:
        return ImageFont.load_default(sz)
    except TypeError:
        return ImageFont.load_default()

def draw_icon():
    img = Image.new('RGB', (S, S))
    px = img.load()
    for y in range(S):
        t = y / (S - 1)
        r = int(10 + (24 - 10) * t); g = int(24 + (84 - 24) * t); b = int(78 + (210 - 78) * t)
        for x in range(S):
            px[x, y] = (r, g, b)
    d = ImageDraw.Draw(img, 'RGBA')
    for i, (x0, y0, x1, y1) in enumerate([(0, 640, 1024, 470), (0, 760, 1024, 600), (0, 540, 1024, 380)]):
        d.line((x0, y0, x1, y1), fill=(255, 255, 255, 28), width=90 - i * 18)
    cx, cy, R, T = 512, 470, 330, 78
    d.ellipse((cx - R, cy - R, cx + R, cy + R), fill=(255, 194, 26, 255))
    d.ellipse((cx - R + T, cy - R + T, cx + R - T, cy + R - T), fill=(12, 34, 96, 255))
    d.arc((cx - R + 14, cy - R + 14, cx + R - 14, cy + R - 14), 200, 290, fill=(255, 238, 160, 255), width=22)
    d.arc((cx - R + T - 6, cy - R + T - 6, cx + R - T + 6, cy + R - T + 6), 20, 110, fill=(10, 24, 70, 255), width=14)
    f = font(330)
    txt = 'D.T'
    bb = d.textbbox((0, 0), txt, font=f)
    tw, th = bb[2] - bb[0], bb[3] - bb[1]
    tx, ty = cx - tw // 2 - bb[0], cy - th // 2 - bb[1] - 6
    for dx, dy in [(-10, -10), (10, -10), (-10, 10), (10, 10), (0, 14), (0, -14), (14, 0), (-14, 0)]:
        d.text((tx + dx, ty + dy), txt, font=f, fill=(8, 14, 40, 255))
    d.text((tx, ty), txt, font=f, fill=(255, 255, 255, 255))
    d.rounded_rectangle((200, 790, 824, 940), radius=40, fill=(200, 24, 40, 255), outline=(255, 255, 255, 255), width=10)
    f2 = font(112)
    bb2 = d.textbbox((0, 0), 'BETA', font=f2)
    d.text((512 - (bb2[2] - bb2[0]) // 2 - bb2[0], 865 - (bb2[3] - bb2[1]) // 2 - bb2[1]), 'BETA', font=f2, fill=(255, 255, 255, 255))
    mask = Image.new('L', (S, S), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, S - 1, S - 1), radius=190, fill=255)
    out = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out

def main(res):
    icon = draw_icon()
    sizes = {'mdpi': 48, 'hdpi': 72, 'xhdpi': 96, 'xxhdpi': 144, 'xxxhdpi': 192}
    for f in glob.glob(os.path.join(res, 'mipmap*', 'ic_launcher*')):
        if os.path.isfile(f):
            os.remove(f)
    for dens, px in sizes.items():
        dd = os.path.join(res, 'mipmap-' + dens)
        os.makedirs(dd, exist_ok=True)
        icon.resize((px, px), Image.LANCZOS).save(os.path.join(dd, 'ic_launcher.png'))
    print('icone ok')

if __name__ == '__main__':
    main(sys.argv[1] if len(sys.argv) > 1 else 'dependencies/RSDKv5/android/app/src/main/res')
