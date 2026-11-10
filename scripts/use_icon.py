import os, sys, glob
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from PIL import Image, ImageDraw, ImageFilter

MAIN = 'dependencies/RSDKv5/android/app/src/main'
RES = MAIN + '/res'
SRC = 'assets/icon.png'
ZOOM = 0.667  # tamanho da arte dentro do icone adaptativo: 0.667 preenche a area visivel; menor mostra mais da arte com bordas desfocadas

if os.path.isfile(SRC):
    im = Image.open(SRC).convert('RGBA')
    w, h = im.size
    s = min(w, h)
    l, t = (w - s) // 2, (h - s) // 2
    im = im.crop((l, t, l + s, t + s))
else:
    import make_icon
    im = make_icon.draw_icon()

def mask_paste(img, kind):
    s = img.size[0]
    m = Image.new('L', (s * 4, s * 4), 0)
    d = ImageDraw.Draw(m)
    if kind == 'circle':
        d.ellipse((0, 0, s * 4 - 1, s * 4 - 1), fill=255)
    else:
        d.rounded_rectangle((0, 0, s * 4 - 1, s * 4 - 1), radius=int(s * 4 * 0.22), fill=255)
    m = m.resize((s, s), Image.LANCZOS)
    out = Image.new('RGBA', (s, s), (0, 0, 0, 0))
    out.paste(img, (0, 0), m)
    return out

def layers(img, px):
    mean = img.convert('RGB').resize((1, 1), Image.BILINEAR).getpixel((0, 0))
    flat = Image.alpha_composite(Image.new('RGBA', img.size, mean + (255,)), img)
    bg = flat.resize((px, px), Image.LANCZOS).filter(ImageFilter.GaussianBlur(px / 18))
    art = int(px * ZOOM)
    fg = Image.new('RGBA', (px, px), (0, 0, 0, 0))
    fg.paste(img.resize((art, art), Image.LANCZOS), ((px - art) // 2, (px - art) // 2))
    return bg, fg

for f in glob.glob(os.path.join(RES, 'mipmap*', 'ic_launcher*')):
    if os.path.isfile(f):
        os.remove(f)

for dens, px in {'mdpi': 48, 'hdpi': 72, 'xhdpi': 96, 'xxhdpi': 144, 'xxxhdpi': 192}.items():
    d = os.path.join(RES, 'mipmap-' + dens)
    os.makedirs(d, exist_ok=True)
    base = im.resize((px, px), Image.LANCZOS)
    mask_paste(base, 'rounded').save(os.path.join(d, 'ic_launcher.png'))
    mask_paste(base, 'circle').save(os.path.join(d, 'ic_launcher_round.png'))

for dens, px in {'mdpi': 108, 'hdpi': 162, 'xhdpi': 216, 'xxhdpi': 324, 'xxxhdpi': 432}.items():
    d = os.path.join(RES, 'mipmap-' + dens)
    os.makedirs(d, exist_ok=True)
    bg, fg = layers(im, px)
    bg.save(os.path.join(d, 'ic_launcher_background.png'))
    fg.save(os.path.join(d, 'ic_launcher_foreground.png'))

v26 = os.path.join(RES, 'mipmap-anydpi-v26')
os.makedirs(v26, exist_ok=True)
XML = ('<?xml version="1.0" encoding="utf-8"?>\n'
       '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
       '    <background android:drawable="@mipmap/ic_launcher_background"/>\n'
       '    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>\n'
       '</adaptive-icon>\n')
for n in ('ic_launcher', 'ic_launcher_round'):
    open(os.path.join(v26, n + '.xml'), 'w').write(XML)

mf = MAIN + '/AndroidManifest.xml'
t = open(mf, encoding='utf-8').read()
if 'roundIcon' not in t:
    t = t.replace('android:icon="@mipmap/ic_launcher"',
                  'android:icon="@mipmap/ic_launcher"\n        android:roundIcon="@mipmap/ic_launcher_round"', 1)
    open(mf, 'w', encoding='utf-8').write(t)
print('icone arredondado ok')
