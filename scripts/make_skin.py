import os, sys
from PIL import Image, ImageDraw

ROOT = sys.argv[1] if len(sys.argv) > 1 else 'dependencies/RSDKv5/android/app/src/main/assets/mods'
MOD = os.path.join(ROOT, 'ExampleSkin')
OUT = os.path.join(MOD, 'TouchControls')
os.makedirs(OUT, exist_ok=True)
SS = 4
BG, BG_ON = (8, 22, 24, 235), (16, 92, 58, 245)
RING, RING_ON = (43, 255, 136, 255), (190, 255, 220, 255)

def canvas(w, h):
    return Image.new('RGBA', (w * SS, h * SS), (0, 0, 0, 0))

def done(img, w, h, name):
    img.resize((w, h), Image.LANCZOS).save(os.path.join(OUT, name))

def button(on):
    w = h = 160
    im = canvas(w, h); d = ImageDraw.Draw(im); m = 6 * SS
    d.ellipse((m, m, w * SS - m, h * SS - m), fill=BG_ON if on else BG, outline=RING_ON if on else RING, width=10 * SS)
    d.ellipse((m + 22 * SS, m + 22 * SS, w * SS - m - 22 * SS, h * SS - m - 22 * SS), outline=(43, 255, 136, 70), width=3 * SS)
    done(im, w, h, 'button_pressed.png' if on else 'button.png')

def pill(on):
    w, h = 320, 112
    im = canvas(w, h); d = ImageDraw.Draw(im); m = 5 * SS
    d.rounded_rectangle((m, m, w * SS - m, h * SS - m), radius=(h * SS) // 2 - m, fill=BG_ON if on else BG, outline=RING_ON if on else RING, width=8 * SS)
    done(im, w, h, 'pill_pressed.png' if on else 'pill.png')

def tri(d, cx, cy, direction, s, **kw):
    pts = {'up': [(cx, cy - s), (cx - s * 0.9, cy + s * 0.7), (cx + s * 0.9, cy + s * 0.7)],
           'down': [(cx, cy + s), (cx - s * 0.9, cy - s * 0.7), (cx + s * 0.9, cy - s * 0.7)],
           'left': [(cx - s, cy), (cx + s * 0.7, cy - s * 0.9), (cx + s * 0.7, cy + s * 0.9)],
           'right': [(cx + s, cy), (cx - s * 0.7, cy - s * 0.9), (cx - s * 0.7, cy + s * 0.9)]}[direction]
    d.polygon(pts, **kw)

def dpad():
    w = h = 320; c = w // 2
    dirs = {'up': (0, -1), 'down': (0, 1), 'left': (-1, 0), 'right': (1, 0)}
    im = canvas(w, h); d = ImageDraw.Draw(im); m = 6 * SS
    d.ellipse((m, m, w * SS - m, h * SS - m), fill=BG, outline=RING, width=10 * SS)
    for dr, (dx, dy) in dirs.items():
        tri(d, (c + dx * 100) * SS, (c + dy * 100) * SS, dr, 34 * SS, fill=(20, 60, 50, 255), outline=(43, 255, 136, 160))
    done(im, w, h, 'dpad.png')
    for dr, (dx, dy) in dirs.items():
        im = canvas(w, h); d = ImageDraw.Draw(im)
        tri(d, (c + dx * 100) * SS, (c + dy * 100) * SS, dr, 34 * SS, fill=RING, outline=RING_ON)
        done(im, w, h, 'dpad_%s.png' % dr)

button(False); button(True); pill(False); pill(True); dpad()
open(os.path.join(OUT, 'skin.ini'), 'w').write('; showLabels=n hides the text over the buttons / esconde o texto dos botoes\nshowLabels=y\n')
open(os.path.join(MOD, 'mod.ini'), 'w', newline='\r\n').write('Name=Example Skin\nDescription=Example of Touch Controls skin (neon green). Replace the PNGs with your own.\nAuthor=Playerzin13MV\nVersion=1.0\nTargetVersion=5\n')
open(os.path.join(MOD, 'README.txt'), 'w', encoding='utf-8').write(
"""Example Skin for the Touch Controls of Sonic Mania: D.T / Skin de exemplo dos Touch Controls

EN: Copy this folder, rename it and replace the PNGs inside TouchControls/. Transparent PNGs.
Missing files fall back to the default look. Enable it in MENU > Mods (the game restarts).
PT: Copie esta pasta, troque o nome e troque os PNGs dentro de TouchControls/. PNG com fundo transparente.
Arquivo que faltar usa o visual padrao. Ative em MENU > Mods (o jogo reinicia).

Files / Arquivos:
  dpad.png, dpad_up/down/left/right.png   d-pad base + overlays drawn when pressed (same size)
  button.png, button_pressed.png          round buttons. Keep button.png BLANK (no letter): the letters
                                          are drawn by the game, so new buttons can use it.
  pill.png, pill_pressed.png              pill buttons (START)
  skin.ini                                showLabels=n hides the text drawn over the images
  font.ttf / font.otf (optional)          custom font for the labels: MENU > Button font > Mod font
""")
print('skin ok', MOD)
