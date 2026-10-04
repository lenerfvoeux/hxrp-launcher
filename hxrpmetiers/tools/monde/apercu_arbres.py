import json, os, sys
from PIL import Image, ImageDraw
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import especes, arbres
FOOD = json.load(open(os.path.join(os.path.dirname(__file__), '../../src/main/resources/assets/hxrpmetiers/data/food.json'), encoding='utf-8'))
PAL = {e['id']: e.get('pal') for g in ('ingredients', 'epices') for e in FOOD[g]}
S = 3; cell = 32 * S
ids = list(especes.ARBRES)
cols = 3
img = Image.new('RGBA', (cols * (cell * 4 + 14), ((len(ids) + cols - 1) // cols) * (cell + 14) + cell + 20), (120, 160, 210, 255))
d = ImageDraw.Draw(img)
for k, a in enumerate(ids):
    x, y = (k % cols) * (cell * 4 + 14), (k // cols) * (cell + 14)
    fr = especes.ARBRES[a][0]
    for s in range(3):
        img.alpha_composite(arbres.feuillage(a, s, PAL.get(fr)).resize((cell, cell), Image.NEAREST), (x + s * cell, y))
    img.alpha_composite(arbres.pousse_arbre(a).resize((cell, cell), Image.NEAREST), (x + 3 * cell, y))
    d.text((x + 2, y + cell), a, fill=(0, 0, 0, 255))
y = ((len(ids) + cols - 1) // cols) * (cell + 14)
blocs = [arbres.minerai_de_sel(), arbres.ruche('avant', False), arbres.ruche('avant', True), arbres.ruche('cote', True), arbres.coquillages('moule'), arbres.coquillages('huitre')]
T = os.path.join(os.path.dirname(__file__), '../../src/main/resources/assets/hxrpmetiers/textures/items/')
for j, (b) in enumerate(blocs):
    img.alpha_composite(b.resize((cell, cell), Image.NEAREST), (j * cell, y))
for j, i in enumerate(['ble', 'carotte', 'tomate', 'basilic']):
    img.alpha_composite(arbres.sachet(Image.open(T + i + '.png').convert('RGBA'), especes.CULTURES[i][5]).resize((cell, cell), Image.NEAREST), ((6 + j) * cell, y))
img.save(os.path.join(os.path.dirname(__file__), '../preview/out/arbres.png'))
