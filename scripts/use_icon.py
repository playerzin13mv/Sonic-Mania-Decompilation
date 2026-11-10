import os, sys, glob
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from PIL import Image

RES = 'dependencies/RSDKv5/android/app/src/main/res'
SRC = 'assets/icon.png'

if not os.path.isfile(SRC):
    import make_icon
    make_icon.main(RES)
    sys.exit(0)

im = Image.open(SRC).convert('RGBA')
w, h = im.size
s = min(w, h)
left, top = (w - s) // 2, (h - s) // 2
im = im.crop((left, top, left + s, top + s))
for f in glob.glob(os.path.join(RES, 'mipmap*', 'ic_launcher*')):
    if os.path.isfile(f):
        os.remove(f)
for dens, px in {'mdpi': 48, 'hdpi': 72, 'xhdpi': 96, 'xxhdpi': 144, 'xxxhdpi': 192}.items():
    d = os.path.join(RES, 'mipmap-' + dens)
    os.makedirs(d, exist_ok=True)
    im.resize((px, px), Image.LANCZOS).save(os.path.join(d, 'ic_launcher.png'))
print('icone do repositorio ok (assets/icon.png)')
