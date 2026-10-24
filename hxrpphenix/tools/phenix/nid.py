"""
Le nid de braises, appât du Phénix : textures (branches calcinées veinées de braises, lit de braises animé),
modèle de bloc à éléments (fond, lit de braises, quatre murets de branches, brindilles croisées et dressées),
modèle d'objet et état de bloc.

    python3 tools/phenix/nid.py [apercu.png]

Avec un chemin, dessine aussi un aperçu du modèle (rendu logiciel de hxrpmetiers/tools/models/mc_render.py).
"""
import json
import os
import random
import sys

from PIL import Image

ICI = os.path.dirname(os.path.abspath(__file__))
RACINE = os.path.normpath(os.path.join(ICI, '..', '..'))
ASSETS = os.path.join(RACINE, 'src', 'main', 'resources', 'assets', 'hxrpphenix')
NOM = 'nid_de_braises'

# palette
CHARBON = (24, 15, 11)
ECORCE = [(43, 26, 17), (61, 37, 22), (84, 52, 30), (110, 70, 40)]
BRAISE = [(150, 36, 10), (214, 74, 18), (255, 128, 30), (255, 186, 70), (255, 230, 150)]


def branches():
    """16×16 : quatre branches horizontales, écorce sombre, bout charbonneux et veines de braises ; jours transparents."""
    rnd = random.Random(7)
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = im.load()
    for rang, y0 in enumerate((0, 4, 8, 12)):
        decal = rnd.randint(-1, 1)
        for x in range(16):
            ondule = 1 if (x + rang * 3) % 9 in (4, 5) else 0
            for k in range(3):
                y = y0 + k + (ondule if k == 2 else 0)
                if y > 15:
                    continue
                c = ECORCE[2 - k] if k < 2 else ECORCE[0]
                if k == 0 and (x * 7 + rang) % 5 == 0:
                    c = ECORCE[3]
                px[x, y] = c + (255,)
            # nœuds charbonneux
            if (x + decal * 5 + rang * 4) % 11 == 0:
                px[x, y0 + 1] = CHARBON + (255,)
        # veines de braises le long de la branche
        x = rnd.randint(1, 4)
        while x < 15:
            long = rnd.randint(2, 4)
            for i in range(long):
                if x + i < 16:
                    px[x + i, y0 + 1] = BRAISE[2 if i in (0, long - 1) else 3] + (255,)
            if x + long // 2 < 16:
                px[x + long // 2, y0 + 2] = BRAISE[1] + (255,)
            x += long + rnd.randint(3, 6)
    # jours entre les branches : brindilles qui les relient par endroits
    for y in (3, 7, 11, 15):
        for x in range(16):
            if px[x, y][3] == 0 and rnd.random() < 0.3:
                px[x, y] = ECORCE[0] + (255,)
    return im


def braises(frames=4):
    """16×64 : lit de charbons rougeoyants, quatre images qui palpitent (texture animée)."""
    rnd = random.Random(11)
    gros = [[rnd.random() for _ in range(8)] for _ in range(8)]       # charbons de 2×2
    base = [[gros[y // 2][x // 2] * 0.8 + rnd.random() * 0.2 for x in range(16)] for y in range(16)]
    chauds = [(rnd.randint(0, 15), rnd.randint(0, 15), rnd.random()) for _ in range(14)]
    im = Image.new('RGBA', (16, 16 * frames), (0, 0, 0, 255))
    px = im.load()
    for f in range(frames):
        for y in range(16):
            for x in range(16):
                v = base[y][x]
                # charbons (sombres) séparés par des fentes rougeoyantes
                fente = (x % 2 == 0 and gros[y // 2][(x - 1) // 2 % 8] < 0.35) or (y % 2 == 0 and (x * 5 + y) % 11 == 0)
                if fente:
                    c = BRAISE[1] if v > 0.4 else BRAISE[0]
                else:
                    c = CHARBON if v < 0.55 else (52, 22, 14) if v < 0.85 else (90, 30, 14)
                px[x, f * 16 + y] = c + (255,)
        for (x, y, ph) in chauds:
            lum = (ph + f / frames) % 1.0
            niveau = 2 + int(lum * 2.99)          # 2 à 4
            px[x, f * 16 + y] = BRAISE[niveau] + (255,)
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                xx, yy = x + dx, y + dy
                if 0 <= xx < 16 and 0 <= yy < 16 and niveau >= 3:
                    px[xx, f * 16 + yy] = BRAISE[niveau - 2] + (255,)
    return im


def boite(f, t, tex_cotes='#branches', tex_dessus='#branches', rot=None, faces=('north', 'south', 'west', 'east', 'up', 'down')):
    """Un élément de modèle ; les UV des côtés suivent les branches (horizontales), le dessus reprend la longueur."""
    e = {'from': list(f), 'to': list(t), 'faces': {}}
    for fa in faces:
        tex = tex_dessus if fa in ('up', 'down') else tex_cotes
        e['faces'][fa] = {'texture': tex}
    if rot:
        e['rotation'] = rot
    return e


def modele():
    el = [
        # fond de branches et lit de braises
        boite((2, 0, 2), (14, 1.5, 14), '#branches', '#braises', faces=('north', 'south', 'west', 'east', 'up', 'down')),
        boite((3, 1.5, 3), (13, 2.5, 13), '#braises', '#braises', faces=('up', 'north', 'south', 'west', 'east')),
        # murets
        boite((2, 1, 1), (14, 5, 3)),
        boite((2, 1, 13), (14, 5, 15)),
        boite((1, 1, 2), (3, 5, 14)),
        boite((13, 1, 2), (15, 5, 14)),
        # brindilles croisées aux quatre coins, sur le bord (dans 0-16 : les UV par défaut restent dans la texture)
        boite((0, 4, 2.5), (6, 5.5, 4), rot={'origin': [3, 4.5, 3], 'axis': 'y', 'angle': 45}),
        boite((10, 4, 12), (16, 5.5, 13.5), rot={'origin': [13, 4.5, 13], 'axis': 'y', 'angle': 45}),
        boite((10, 4, 2.5), (16, 5.5, 4), rot={'origin': [13, 4.5, 3], 'axis': 'y', 'angle': -45}),
        boite((0, 4, 12), (6, 5.5, 13.5), rot={'origin': [3, 4.5, 13], 'axis': 'y', 'angle': -45}),
        # brindilles dressées
        boite((4, 4, 1.5), (5, 8, 2.5), rot={'origin': [4.5, 4, 2], 'axis': 'x', 'angle': -22.5}),
        boite((11, 4, 13.5), (12, 8.5, 14.5), rot={'origin': [11.5, 4, 14], 'axis': 'x', 'angle': 22.5}),
        boite((1.5, 4, 10), (2.5, 7.5, 11), rot={'origin': [2, 4, 10.5], 'axis': 'z', 'angle': 22.5}),
        boite((13.5, 4, 5), (14.5, 7, 6), rot={'origin': [14, 4, 5.5], 'axis': 'z', 'angle': -22.5}),
    ]
    return {
        'ambientocclusion': False,
        'textures': {
            'particle': 'hxrpphenix:blocks/%s_branches' % NOM,
            'branches': 'hxrpphenix:blocks/%s_branches' % NOM,
            'braises': 'hxrpphenix:blocks/%s_braises' % NOM,
        },
        'elements': el,
        'display': {
            'gui': {'rotation': [30, 225, 0], 'translation': [0, 2, 0], 'scale': [0.85, 0.85, 0.85]},
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.4, 0.4, 0.4]},
            'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.6, 0.6, 0.6]},
            'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
            'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 2, 0], 'scale': [0.4, 0.4, 0.4]},
            'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 2, 0], 'scale': [0.4, 0.4, 0.4]},
        },
    }


def ecrire_json(chemin, d):
    os.makedirs(os.path.dirname(chemin), exist_ok=True)
    with open(chemin, 'w', encoding='utf-8') as f:
        json.dump(d, f, indent=1, ensure_ascii=False)
        f.write('\n')


def ecrire():
    tex = os.path.join(ASSETS, 'textures', 'blocks')
    os.makedirs(tex, exist_ok=True)
    branches().save(os.path.join(tex, NOM + '_branches.png'))
    braises().save(os.path.join(tex, NOM + '_braises.png'))
    ecrire_json(os.path.join(tex, NOM + '_braises.png.mcmeta'), {'animation': {'frametime': 5, 'interpolate': True}})
    m = modele()
    ecrire_json(os.path.join(ASSETS, 'models', 'block', NOM + '.json'), m)
    ecrire_json(os.path.join(ASSETS, 'models', 'item', NOM + '.json'), {'parent': 'hxrpphenix:block/' + NOM})
    ecrire_json(os.path.join(ASSETS, 'blockstates', NOM + '.json'), {'variants': {'normal': {'model': 'hxrpphenix:' + NOM}}})
    return m


def apercu(chemin, m):
    sys.path.insert(0, os.path.join(RACINE, '..', 'hxrpmetiers', 'tools', 'models'))
    import numpy as np
    import mc_render
    images = {}
    for k in ('branches', 'braises'):
        im = Image.open(os.path.join(ASSETS, 'textures', 'blocks', '%s_%s.png' % (NOM, k))).convert('RGBA').crop((0, 0, 16, 16))
        images['#' + k] = np.asarray(im).astype(float) / 255.0
    vues = [(45, 30), (225, 30), (45, 60), (135, 15)]
    planche = Image.new('RGBA', (300 * len(vues) + 200, 300), (40, 46, 60, 255))
    for i, (lacet, tangage) in enumerate(vues):
        r = mc_render.render(m['elements'], images, size=300, yaw=lacet, pitch=tangage, scale=300 / 20.0)
        planche.alpha_composite(r, (i * 300, 0))
    for i, k in enumerate(('branches', 'braises')):
        im = Image.open(os.path.join(ASSETS, 'textures', 'blocks', '%s_%s.png' % (NOM, k))).convert('RGBA')
        im = im.resize((im.width * 6, im.height * 6), Image.NEAREST)
        if im.height > 300:
            im = im.crop((0, 0, im.width, 300))
        planche.alpha_composite(im, (300 * len(vues) + 4 + i * 98, 4))
    planche.save(chemin)


if __name__ == '__main__':
    m = ecrire()
    print('nid de braises : textures, modèles et état de bloc écrits')
    if len(sys.argv) > 1:
        apercu(sys.argv[1], m)
