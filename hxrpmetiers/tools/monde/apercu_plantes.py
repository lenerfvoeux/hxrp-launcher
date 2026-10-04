import json, os, sys
from PIL import Image, ImageDraw
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import especes, plantes
FOOD = json.load(open(os.path.join(os.path.dirname(__file__), '../../src/main/resources/assets/hxrpmetiers/data/food.json'), encoding='utf-8'))
PAL = {e['id']: e.get('pal') for g in ('ingredients', 'epices') for e in FOOD[g]}
ids = sys.argv[1:] or list(especes.CULTURES)
S = 3; cell = 32 * S
cols = 4
img = Image.new('RGBA', (cols * (cell * 3 + 12), ((len(ids) + cols - 1) // cols) * (cell + 14)), (120, 160, 210, 255))
d = ImageDraw.Draw(img)
for k, i in enumerate(ids):
    x, y = (k % cols) * (cell * 3 + 12), (k // cols) * (cell + 14)
    for s in range(3):
        bg = Image.new('RGBA', (cell, cell), (120, 160, 210, 255))
        bg.paste((110, 78, 48, 255), (0, cell - 4, cell, cell))
        im = plantes.culture(i, s, PAL[i]).resize((cell, cell), Image.NEAREST)
        bg.alpha_composite(im)
        img.alpha_composite(bg, (x + s * cell, y))
    d.text((x + 2, y + cell), i, fill=(0, 0, 0, 255))
out = os.path.join(os.path.dirname(__file__), '../preview/out/cultures.png')
img.save(out); print(out, img.size)
