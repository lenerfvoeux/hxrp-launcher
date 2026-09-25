"""
Génère toutes les icônes d'items (32x32) dans src/main/resources/assets/hxrpmetiers/textures/items/,
vérifie que chaque entrée de food.json a son dessin, exporte les palettes des mini-jeux dans food.json
et produit des planches de contrôle dans tools/preview/out/.

    python3 tools/textures/gen_items.py            (tout)
    python3 tools/textures/gen_items.py carotte    (quelques ids, planche seule)
"""
import json
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from pix import Canvas  # noqa: E402
import parts  # noqa: E402
import ingredients  # noqa: E402,F401
import preparations  # noqa: E402,F401
import plats  # noqa: E402,F401
import boissons  # noqa: E402,F401
import retouches  # noqa: E402,F401  (redessins, remplacent les versions précédentes)

ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'hxrpmetiers')
FOOD = os.path.join(ASSETS, 'data', 'food.json')
OUT = os.path.join(ASSETS, 'textures', 'items')
PREVIEW = os.path.join(ROOT, 'tools', 'preview', 'out')


def render(id):
    c = Canvas()
    parts.REG[id](c)
    c.outline()
    return c.image()


def sheet(ids, path, scale=3, cols=12, names=None):
    cell = 32 * scale + 8
    rows = (len(ids) + cols - 1) // cols
    img = Image.new('RGBA', (cols * cell, rows * (cell + 12)), (58, 62, 72, 255))
    d = ImageDraw.Draw(img)
    for k, i in enumerate(ids):
        x, y = (k % cols) * cell, (k // cols) * (cell + 12)
        # damier discret pour juger la transparence
        im = render(i).resize((32 * scale, 32 * scale), Image.NEAREST)
        img.alpha_composite(im, (x + 4, y + 4))
        label = (names or {}).get(i, i)[:16]
        d.text((x + 4, y + cell - 2), label, fill=(230, 230, 230, 255))
    img.save(path)


def main(argv):
    food = json.load(open(FOOD, encoding='utf-8'))
    groups = ['ingredients', 'epices', 'preparations', 'plats', 'boissons']
    all_ids = [e['id'] for g in groups for e in food[g]] + ['preparation_en_cours']
    names = {e['id']: e['name'] for g in groups for e in food[g]}
    os.makedirs(PREVIEW, exist_ok=True)
    if argv:
        ids = [i for i in argv if i in parts.REG]
        sheet(ids, os.path.join(PREVIEW, 'items_selection.png'), scale=5, cols=min(8, len(ids)), names=names)
        return
    missing = [i for i in all_ids if i not in parts.REG]
    if missing:
        raise SystemExit('Dessins manquants : ' + ', '.join(missing))
    extra = sorted(set(parts.REG) - set(all_ids))
    if extra:
        print('Dessins sans entrée (ignorés) :', ', '.join(extra))
    os.makedirs(OUT, exist_ok=True)
    for i in all_ids:
        render(i).save(os.path.join(OUT, i + '.png'))
    for g in groups:
        sheet([e['id'] for e in food[g]], os.path.join(PREVIEW, 'items_' + g + '.png'), names=names)
    # palettes des mini-jeux (découpe, poêle, grill…)
    for g in groups:
        for e in food[g]:
            p = parts.PAL.get(e['id'])
            for k in ('famille', 'pal', 'motif'):
                e.pop(k, None)
            if p:
                e.update(p)
    with open(FOOD, 'w', encoding='utf-8') as f:
        json.dump(food, f, ensure_ascii=False, indent=1)
        f.write('\n')
    print(len(all_ids), 'icônes générées,', len(parts.PAL), 'palettes exportées')


if __name__ == '__main__':
    main(sys.argv[1:])
