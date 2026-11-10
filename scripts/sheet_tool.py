import sys, os
from PIL import Image, ImageDraw

ROLES = ['dpad', 'dpad_up', 'dpad_down', 'dpad_left', 'dpad_right',
         'button', 'button_pressed', 'pill', 'pill_pressed']
USO = """uso:
  python3 sheet_tool.py grid FOLHA SAIDA.png
      salva a folha ampliada com uma grade e os numeros das coordenadas
  python3 sheet_tool.py make FOLHA PASTA_DO_MOD papel=x,y,largura,altura ... [--key=R,G,B]
      corta pedacos da folha e salva em PASTA_DO_MOD/TouchControls/papel.png
      papeis: """ + ', '.join(ROLES) + """
      a cor-chave (fundo transparente) e magenta 255,0,255 se voce nao informar"""

def cmd_grid(sheet, out, step=16, zoom=4):
    im = Image.open(sheet).convert('RGBA')
    big = im.resize((im.width * zoom, im.height * zoom), Image.NEAREST)
    d = ImageDraw.Draw(big)
    for x in range(0, im.width + 1, step):
        d.line((x * zoom, 0, x * zoom, big.height), fill=(0, 255, 255, 255), width=1)
        d.text((x * zoom + 2, 2), str(x), fill=(255, 255, 255, 255))
    for y in range(0, im.height + 1, step):
        d.line((0, y * zoom, big.width, y * zoom), fill=(0, 255, 255, 255), width=1)
        d.text((2, y * zoom + 2), str(y), fill=(255, 255, 255, 255))
    big.convert('RGB').save(out)
    print('grade salva em', out, '- tamanho original %dx%d' % im.size)

def cmd_make(sheet, outdir, args):
    im = Image.open(sheet).convert('RGBA')
    key = (255, 0, 255)
    for a in args:
        if a.startswith('--key='):
            key = tuple(int(v) for v in a[6:].split(','))
    dest = os.path.join(outdir, 'TouchControls')
    os.makedirs(dest, exist_ok=True)
    n = 0
    for a in args:
        if '=' not in a or a.startswith('--'):
            continue
        role, box = a.split('=', 1)
        if role not in ROLES:
            sys.exit('papel desconhecido: %s (use: %s)' % (role, ', '.join(ROLES)))
        x, y, w, h = [int(v) for v in box.split(',')]
        part = im.crop((x, y, x + w, y + h))
        px = part.load()
        for yy in range(part.height):
            for xx in range(part.width):
                if px[xx, yy][:3] == key:
                    px[xx, yy] = (0, 0, 0, 0)
        part.save(os.path.join(dest, role + '.png'))
        n += 1
    ini = os.path.join(outdir, 'mod.ini')
    if not os.path.exists(ini):
        open(ini, 'w', newline='\r\n').write('Name=%s\nAuthor=\nVersion=1.0\nTargetVersion=5\n' % os.path.basename(os.path.normpath(outdir)))
    print(n, 'imagens salvas em', dest)

if __name__ == '__main__':
    a = sys.argv[1:]
    if len(a) >= 3 and a[0] == 'grid':
        cmd_grid(a[1], a[2])
    elif len(a) >= 3 and a[0] == 'make':
        cmd_make(a[1], a[2], a[3:])
    else:
        print(USO)
