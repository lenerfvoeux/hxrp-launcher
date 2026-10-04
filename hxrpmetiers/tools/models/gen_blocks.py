"""
Génère les stations 3D : textures (textures/blocks), modèles (models/block, models/item),
blockstates (orientées vers le joueur), et des aperçus rendus dans tools/preview/out/.

    python3 tools/models/gen_blocks.py
"""
import glob
import json
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import stations  # noqa: E402
import materials  # noqa: E402
import mc_render  # noqa: E402
import zfight  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'hxrpmetiers')
PREVIEW = os.path.join(ROOT, 'tools', 'preview', 'out')


def main():
    tdir = os.path.join(ASSETS, 'textures', 'blocks')
    for old in glob.glob(os.path.join(tdir, '*.png')):
        os.remove(old)
    materials.save_all(tdir)
    for b in stations.BUILDERS:
        b()
    mdir = os.path.join(ASSETS, 'models', 'block')
    idir = os.path.join(ASSETS, 'models', 'item')
    bdir = os.path.join(ASSETS, 'blockstates')
    used = set()
    for name, m in stations.MODELS.items():
        with open(os.path.join(mdir, name + '.json'), 'w') as f:
            json.dump(m.json(), f, indent=1)
        for v in m.textures.values():
            used.add(v.split('/')[-1])
        item = {'parent': 'hxrpmetiers:block/' + name}
        if m.gui_scale != 0.625 or m.gui_dy:
            item['display'] = {'gui': {'rotation': [30, 225, 0], 'translation': [0, m.gui_dy, 0], 'scale': [m.gui_scale] * 3}}
        with open(os.path.join(idir, name + '.json'), 'w') as f:
            json.dump(item, f, indent=1)
        variants = {'facing=north': {'model': 'hxrpmetiers:' + name},
                    'facing=east': {'model': 'hxrpmetiers:' + name, 'y': 90},
                    'facing=south': {'model': 'hxrpmetiers:' + name, 'y': 180},
                    'facing=west': {'model': 'hxrpmetiers:' + name, 'y': 270}}
        with open(os.path.join(bdir, name + '.json'), 'w') as f:
            json.dump({'variants': variants}, f, indent=1)
    for name, m in stations.MODELS.items():
        c = zfight.conflits(m.json())
        if c:
            raise SystemExit('Faces superposées (scintillement) dans %s : %s' % (name, c[:5]))
    missing = [t for t in used if not os.path.exists(os.path.join(tdir, t + '.png'))]
    if missing:
        raise SystemExit('Textures manquantes : ' + ', '.join(missing))
    unused = [os.path.basename(p)[:-4] for p in glob.glob(os.path.join(tdir, '*.png')) if os.path.basename(p)[:-4] not in used]
    for u in unused:
        os.remove(os.path.join(tdir, u + '.png'))
    # aperçus
    os.makedirs(PREVIEW, exist_ok=True)
    names = list(stations.MODELS)
    cell = 240
    sheet = Image.new('RGBA', (cell * 4, (cell + 16) * ((len(names) + 1) // 2)), (74, 84, 96, 255))
    d = ImageDraw.Draw(sheet)
    for k, n in enumerate(names):
        els, imgs = mc_render.load_model(mdir, n, os.path.join(ASSETS, 'textures'))
        a = mc_render.render(els, imgs, size=cell, scale=cell / 30)
        b = mc_render.render(els, imgs, size=cell, scale=cell / 30, yaw=225)
        x, y = (k % 2) * cell * 2, (k // 2) * (cell + 16)
        sheet.alpha_composite(a, (x, y))
        sheet.alpha_composite(b, (x + cell, y))
        d.text((x + 6, y + cell), n, fill=(255, 255, 255, 255))
        big = mc_render.render(els, imgs, size=512, scale=512 / 28)
        big.save(os.path.join(PREVIEW, 'bloc_' + n + '.png'))
    sheet.save(os.path.join(PREVIEW, 'blocs.png'))
    for n, m in stations.MODELS.items():
        print('%-16s elements=%3d light=%d aabb=%s' % (n, len(m.elements), m.light, m.aabb))


if __name__ == '__main__':
    main()
