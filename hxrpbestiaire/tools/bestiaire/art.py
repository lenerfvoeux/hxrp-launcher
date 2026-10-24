"""
Pixel art du bestiaire : icônes 32×32 des butins et des œufs, icônes 16×16 des projectiles, et les appâts (modèles
de bloc à éléments + textures 16×16), avec états de bloc, modèles d'objet et traductions.
"""
import json
import math
import os
import random

from PIL import Image, ImageDraw

# ============================================================================ outils
def ecrire_json(chemin, d):
    os.makedirs(os.path.dirname(chemin), exist_ok=True)
    with open(chemin, 'w', encoding='utf-8') as f:
        json.dump(d, f, indent=1, ensure_ascii=False)
        f.write('\n')


def rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


def mix(a, b, k):
    return tuple(int(round(a[i] + (b[i] - a[i]) * k)) for i in range(3))


def contour(im, couleur=(18, 10, 12)):
    """Contour sombre d'un pixel autour de la silhouette (style des icônes Minecraft)."""
    px = im.load()
    w, h = im.size
    plein = [[px[x, y][3] > 0 for x in range(w)] for y in range(h)]
    for y in range(h):
        for x in range(w):
            if plein[y][x]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                xx, yy = x + dx, y + dy
                if 0 <= xx < w and 0 <= yy < h and plein[yy][xx]:
                    px[x, y] = couleur + (255,)
                    break
    return im


def disque(px, cx, cy, r, couleur_fn, w=32, h=32):
    for y in range(h):
        for x in range(w):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if d <= r:
                c = couleur_fn(x, y, d / r)
                if c is not None:
                    px[x, y] = c + (255,) if len(c) == 3 else c


# ============================================================================ butins (32×32)
def dard_de_reine():
    """Un long dard recourbé, chitine noire et rouge, pointe cramoisie, une goutte de venin vert."""
    im = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    px = im.load()
    # axe courbe du dard : du bas-gauche (base) au haut-droit (pointe)
    pts = [(5 + t * 22 + math.sin(t * math.pi) * -3, 27 - t * 22 - math.sin(t * math.pi) * 3, t) for t in [i / 60 for i in range(61)]]
    for x0, y0, t in pts:
        r = 3.6 * (1 - t) ** 0.8 + 0.6
        for y in range(32):
            for x in range(32):
                d = math.hypot(x + 0.5 - x0, y + 0.5 - y0)
                if d <= r:
                    seg = int(t * 6) % 2
                    base = (60, 18, 26) if seg else (34, 14, 20)
                    if t > 0.7:
                        base = mix((150, 20, 30), (230, 60, 50), (t - 0.7) / 0.3)
                    lum = 1.0 - 0.45 * (d / r)
                    if (x + 0.5 - x0) - (y + 0.5 - y0) < -r * 0.3:
                        lum += 0.25
                    px[x, y] = tuple(min(255, int(c * lum)) for c in base) + (255,)
    # anneaux de la base
    for x, y in ((6, 25), (7, 24), (9, 22)):
        px[x, y] = (150, 30, 40, 255)
    # goutte de venin
    for (x, y, c) in ((28, 6, (120, 220, 60)), (28, 7, (90, 190, 40)), (27, 7, (150, 240, 90)), (28, 8, (60, 150, 30)), (29, 7, (70, 160, 30))):
        px[x, y] = c + (255,)
    return contour(im)


def bocal(couleur_dedans, reflet, couvercle, etiquette=None):
    im = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    verre = (190, 210, 220, 150)
    d.rectangle((8, 9, 23, 28), fill=verre)
    d.rectangle((9, 13, 22, 27), fill=couleur_dedans + (255,))
    for y in range(13, 28):
        k = (y - 13) / 14
        d.line((9, y, 22, y), fill=mix(couleur_dedans, (0, 0, 0), k * 0.4) + (255,))
    d.line((10, 14, 10, 25), fill=reflet + (255,))
    d.line((11, 14, 11, 18), fill=reflet + (255,))
    d.rectangle((9, 6, 22, 9), fill=couvercle + (255,))
    d.line((9, 6, 22, 6), fill=mix(couvercle, (255, 255, 255), 0.35) + (255,))
    d.rectangle((7, 9, 24, 10), fill=mix(couvercle, (0, 0, 0), 0.3) + (255,))
    if etiquette:
        d.rectangle((12, 18, 19, 22), fill=etiquette + (255,))
    d.line((23, 11, 23, 27), fill=(230, 240, 245, 200))
    return contour(im)


def gelee_royale_noire():
    """Bocal de gelée noire aux reflets violets, couvercle d'alvéoles dorées."""
    im = bocal((28, 16, 34), (150, 90, 200), (196, 150, 40), etiquette=(220, 200, 150))
    px = im.load()
    for (x, y) in ((13, 19), (15, 20), (17, 19), (14, 21), (16, 21)):
        px[x, y] = (120, 80, 30, 255)
    return im


def miel_noir():
    """Un morceau de rayon d'alvéoles gorgées de miel noir, qui coule."""
    im = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    px = im.load()
    cire = (120, 84, 28)
    for y in range(6, 24):
        for x in range(4, 28):
            # alvéoles hexagonales approchées (quinconce)
            col = (x - 4) // 6
            dy = 3 if col % 2 else 0
            cx, cy = 4 + col * 6 + 3, 6 + ((y - 6 - dy) // 6) * 6 + 3 + dy
            d = max(abs(x + 0.5 - cx) * 1.15, abs(y + 0.5 - cy))
            if d > 3.2:
                continue
            if d > 2.4:
                px[x, y] = cire + (255,)
            else:
                k = (y - cy + 3) / 6
                px[x, y] = mix((60, 30, 10), (14, 8, 6), k) + (255,)
                if x == cx - 1 and y == cy - 1:
                    px[x, y] = (200, 140, 60, 255)
    # bord irrégulier en haut
    for x in range(4, 28):
        for y in range(6, 6 + (x * 7) % 3):
            px[x, y] = (0, 0, 0, 0)
    # coulures
    for x, l in ((8, 5), (15, 7), (22, 4)):
        for y in range(24, 24 + l):
            px[x, y] = (24, 12, 6, 255)
        px[x, 24 + l] = (40, 22, 10, 255)
    return contour(im)


def chair_d_abomination():
    """Un lambeau de chair sombre, veines noires, pustules verdâtres."""
    im = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    px = im.load()
    rnd = random.Random(5)
    for y in range(32):
        for x in range(32):
            dx, dy = (x + 0.5 - 16) / 12, (y + 0.5 - 17) / 9
            r = math.hypot(dx, dy) + 0.15 * math.sin(x * 0.9) * math.cos(y * 0.7)
            if r > 1:
                continue
            base = mix((120, 26, 30), (70, 14, 20), r)
            if (x * 3 + y * 5) % 11 == 0 or abs(math.sin(x * 0.5 + y * 0.3)) < 0.06:
                base = (40, 8, 14)
            if y < 13 and r < 0.6:
                base = mix(base, (170, 60, 60), 0.4)
            px[x, y] = base + (255,)
    for (x, y) in ((10, 15), (19, 12), (22, 19), (13, 20)):
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            px[x + dx, y + dy] = (140, 170, 60, 255) if (dx, dy) != (0, 0) else (200, 230, 120, 255)
    # os qui dépasse
    for i in range(7):
        px[24 + i // 2, 9 - i // 3] = (226, 216, 190, 255)
        px[25 + i // 2, 9 - i // 3] = (190, 178, 150, 255)
    return contour(im)


def poussiere_d_os_maudite():
    """Un petit tas de poussière d'os, pâle, piqueté d'éclats violets maudits."""
    im = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    px = im.load()
    rnd = random.Random(9)
    for y in range(32):
        for x in range(32):
            h = 26 - 14 * math.exp(-((x + 0.5 - 16) / 9) ** 2)
            if y < h or y > 27 or x < 3 or x > 28:
                continue
            k = (y - h) / max(1, 27 - h)
            c = mix((232, 226, 210), (150, 140, 128), k)
            n = rnd.random()
            if n < 0.18:
                c = mix(c, (110, 100, 96), 0.5)
            px[x, y] = c + (255,)
    for (x, y) in ((12, 18), (19, 16), (16, 21), (22, 23), (9, 24), (16, 14)):
        px[x, y] = (170, 60, 220, 255)
        if y > 13:
            px[x, y - 1] = (220, 150, 255, 255)
    # éclats qui flottent au-dessus
    for (x, y) in ((11, 8), (20, 6), (15, 4)):
        px[x, y] = (190, 110, 240, 255)
    return contour(im)


BUTINS = {
    'dard_de_reine': dard_de_reine,
    'gelee_royale_noire': gelee_royale_noire,
    'miel_noir': miel_noir,
    'chair_d_abomination': chair_d_abomination,
    'poussiere_d_os_maudite': poussiere_d_os_maudite,
}


# ============================================================================ œufs (32×32)
OEUFS = {
    # boss : (coque, taches, motif)
    'reine_guepe': (0x26141A, 0xC4182C, 'rayures'),
    'abomination': (0x2A1C1C, 0x8E1B1B, 'pustules'),
    'necromancien': (0x1B1F2E, 0xC81E2A, 'runes'),
}


def oeuf(coque, taches, motif):
    im = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    px = im.load()
    c1, c2 = rgb(coque), rgb(taches)
    rnd = random.Random(coque)
    for y in range(32):
        for x in range(32):
            # ovale plus étroit en haut
            ny = (y + 0.5 - 17.5) / 12.5
            rx = 9.5 * (1 - 0.18 * max(0, -ny))
            nx = (x + 0.5 - 16) / rx
            if nx * nx + ny * ny > 1:
                continue
            lum = 1.15 - 0.55 * ((nx + 0.35) ** 2 + (ny + 0.4) ** 2) ** 0.5
            c = tuple(min(255, int(v * lum)) for v in c1)
            if motif == 'rayures' and int((y + 0.5 + x * 0.25) / 4) % 2 == 0 and abs(ny) < 0.85:
                c = tuple(min(255, int(v * lum)) for v in c2)
            elif motif == 'pustules' and rnd.random() < 0.09:
                c = mix(c2, (150, 180, 70), rnd.random() * 0.5)
            elif motif == 'runes' and ((x + y) % 7 == 0 and abs(nx) < 0.6 and abs(ny) < 0.6):
                c = c2
            px[x, y] = c + (255,)
    # reflet
    for (x, y) in ((12, 9), (12, 10), (13, 8), (11, 11)):
        px[x, y] = mix(c1, (255, 255, 255), 0.7) + (255,)
    return contour(im)


# ============================================================================ projectiles (16×16)
def projectile_dard():
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = im.load()
    for i in range(12):
        x, y = 2 + i, 13 - i
        c = (40, 14, 20) if i < 8 else mix((160, 20, 30), (240, 80, 60), (i - 8) / 4)
        px[x, y] = c + (255,)
        if i < 9:
            px[x, y - 1] = (70, 24, 30, 255)
    for (x, y) in ((1, 14), (2, 14), (1, 13)):
        px[x, y] = (110, 200, 50, 255)
    return im


def projectile_ame():
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = im.load()
    disque(px, 8, 8, 5.5, lambda x, y, k: mix((240, 200, 255), (120, 30, 170), k) + (int(255 * (1 - k * 0.3)),), 16, 16)
    for (x, y) in ((6, 6), (7, 6), (6, 7)):
        px[x, y] = (255, 245, 255, 255)
    for (x, y) in ((8, 1), (9, 2), (14, 8), (13, 9), (2, 10), (3, 9)):
        px[x, y] = (170, 90, 230, 200)
    return im


# ============================================================================ appâts (modèles + textures 16×16)
def hexagone(x, y, taille):
    """Centre de l'alvéole (hexagones « pointe en haut ») qui contient (x, y), et distance normalisée au bord."""
    q = (math.sqrt(3) / 3 * x - y / 3) / taille
    r = (2 / 3 * y) / taille
    # arrondi en coordonnées cubiques
    cx, cz = q, r
    cy = -cx - cz
    rx, ry, rz = round(cx), round(cy), round(cz)
    dx, dy, dz = abs(rx - cx), abs(ry - cy), abs(rz - cz)
    if dx > dy and dx > dz:
        rx = -ry - rz
    elif dy > dz:
        ry = -rx - rz
    else:
        rz = -rx - ry
    hx = taille * math.sqrt(3) * (rx + rz / 2)
    hy = taille * 1.5 * rz
    d = max(abs(cx - rx), abs(cy - ry), abs(cz - rz)) * 2
    return hx, hy, d


def tex_rayon():
    """Alvéoles hexagonales de cire dorée, pleines de miel royal luisant (motif qui se raccorde sur 16 px)."""
    im = Image.new('RGBA', (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            hx, hy, d = hexagone(x + 0.5, y + 0.5 + 0.7, 2.31)
            if d > 0.72:
                px[x, y] = (140, 92, 24, 255) if d > 0.86 else (178, 122, 34, 255)
            else:
                k = min(1, max(0, (y + 0.5 - hy + 2) / 4))
                px[x, y] = mix((255, 214, 80), (214, 128, 16), k) + (255,)
                if x + 0.5 < hx and y + 0.5 < hy - 0.3:
                    px[x, y] = (255, 236, 160, 255)
    return im


def tex_miel():
    """Miel qui coule : ambre clair, reflet."""
    im = Image.new('RGBA', (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = mix((255, 196, 50), (200, 110, 10), (x % 4) / 4) + (255,)
    return im


def tex_cire():
    im = Image.new('RGBA', (16, 16))
    px = im.load()
    rnd = random.Random(3)
    for y in range(16):
        for x in range(16):
            px[x, y] = mix((176, 124, 40), (130, 88, 26), rnd.random() * 0.6 + (y / 32)) + (255,)
    for x in (3, 9, 13):
        for y in range(0, 4 + x % 5):
            px[x, y] = (240, 170, 40, 255)
    return im


def tex_chair():
    im = Image.new('RGBA', (16, 16))
    px = im.load()
    rnd = random.Random(7)
    for y in range(16):
        for x in range(16):
            v = rnd.random()
            c = mix((120, 30, 32), (74, 16, 20), v)
            if abs(math.sin(x * 0.7 + y * 0.45)) < 0.12:
                c = (46, 10, 16)
            if v > 0.97:
                c = (128, 150, 60)
            elif v < 0.08:
                c = (150, 40, 40)
            px[x, y] = c + (255,)
    return im


def tex_os():
    im = Image.new('RGBA', (16, 16))
    px = im.load()
    rnd = random.Random(11)
    for y in range(16):
        for x in range(16):
            px[x, y] = mix((232, 224, 200), (176, 164, 136), rnd.random() * 0.5 + (x % 4 == 0) * 0.2) + (255,)
    return im


def tex_bois():
    """Planches de bois pourri, sombre, clous rouillés."""
    im = Image.new('RGBA', (16, 16))
    px = im.load()
    rnd = random.Random(13)
    for y in range(16):
        planche = y // 4
        for x in range(16):
            c = mix((70, 46, 34), (44, 28, 22), rnd.random() * 0.5 + 0.2 * (planche % 2))
            if y % 4 == 3:
                c = (26, 16, 12)
            if (x + planche * 5) % 13 == 0 and y % 4 != 3:
                c = (34, 22, 16)
            px[x, y] = c + (255,)
    for (x, y) in ((1, 1), (14, 1), (1, 9), (14, 9)):
        px[x, y] = (130, 70, 40, 255)
    return im


def tex_interieur():
    im = Image.new('RGBA', (16, 16))
    px = im.load()
    rnd = random.Random(17)
    for y in range(16):
        for x in range(16):
            c = mix((40, 12, 18), (18, 8, 10), rnd.random())
            if rnd.random() < 0.05:
                c = (90, 20, 30)
            px[x, y] = c + (255,)
    return im


def boite(f, t, cotes='#a', dessus=None, rot=None, faces=('north', 'south', 'west', 'east', 'up', 'down')):
    e = {'from': list(f), 'to': list(t), 'faces': {}}
    for fa in faces:
        e['faces'][fa] = {'texture': (dessus or cotes) if fa in ('up', 'down') else cotes}
    if rot:
        e['rotation'] = rot
    return e


def modele_rayon():
    el = [
        boite((3, 0, 5), (13, 1.5, 11), '#cire'),                                     # socle de cire
        boite((1.5, 1.5, 6.5), (14.5, 13, 9.5), '#rayon', '#cire'),                   # le rayon, debout
        boite((1, 2.5, 7), (1.5, 12, 9), '#cire'),                                     # bords arrondis
        boite((14.5, 2.5, 7), (15, 12, 9), '#cire'),
        boite((3, 13, 7), (12, 14, 9), '#cire'),
        boite((4, 14, 7.25), (6, 16, 8.75), '#cire'),                                 # cellules royales (« cacahuètes »)
        boite((7.5, 14, 7.25), (9.5, 15.5, 8.75), '#cire'),
        boite((10.5, 14, 7.25), (12, 15, 8.75), '#cire'),
        boite((4, 1.5, 9.5), (5, 5, 10), '#miel'),                                    # coulures de miel
        boite((9, 1.5, 9.5), (10, 3.5, 10), '#miel'),
        boite((11, 1.5, 6), (12, 4, 6.5), '#miel'),
        boite((6, 1.5, 6), (7, 2.5, 6.5), '#miel'),
    ]
    tex = {'particle': 'hxrpbestiaire:blocks/appat_rayon', 'rayon': 'hxrpbestiaire:blocks/appat_rayon',
           'cire': 'hxrpbestiaire:blocks/appat_cire', 'miel': 'hxrpbestiaire:blocks/appat_miel'}
    return el, tex


def modele_charnier():
    el = [
        boite((1, 0, 1), (15, 3, 15), '#chair'),
        boite((3, 3, 2), (13, 6, 13), '#chair'),
        boite((5, 6, 5), (11, 8, 11), '#chair'),
        # côtes qui dépassent
        boite((3, 4, 7), (4, 10, 8), '#os', rot={'origin': [3.5, 4, 7.5], 'axis': 'z', 'angle': 22.5}),
        boite((12, 4, 6), (13, 10, 7), '#os', rot={'origin': [12.5, 4, 6.5], 'axis': 'z', 'angle': -22.5}),
        boite((7, 6, 3), (8, 11, 4), '#os', rot={'origin': [7.5, 6, 3.5], 'axis': 'x', 'angle': -22.5}),
        boite((9, 6, 11), (10, 10, 12), '#os', rot={'origin': [9.5, 6, 11.5], 'axis': 'x', 'angle': 22.5}),
        # un crâne
        boite((13, 3, 0.5), (16, 6, 3.5), '#os'),
    ]
    tex = {'particle': 'hxrpbestiaire:blocks/appat_chair', 'chair': 'hxrpbestiaire:blocks/appat_chair', 'os': 'hxrpbestiaire:blocks/appat_os'}
    return el, tex


def modele_cercueil():
    el = [
        boite((3, 0, 0), (13, 1, 16), '#bois'),
        boite((3, 1, 0), (4, 7, 16), '#bois'),
        boite((12, 1, 0), (13, 7, 16), '#bois'),
        boite((4, 1, 0), (12, 7, 1), '#bois'),
        boite((4, 1, 15), (12, 7, 16), '#bois'),
        boite((4, 1, 1), (12, 1.5, 15), '#interieur', faces=('up',)),
        # couvercle entrouvert, posé de travers
        boite((3, 7, 2), (13, 8, 14), '#bois', rot={'origin': [13, 7, 8], 'axis': 'z', 'angle': -22.5}),
        # os à l'intérieur
        boite((6, 1.5, 3), (9, 4.5, 6), '#os'),
        boite((5, 1.5, 8), (11, 2.5, 9), '#os'),
        boite((7, 1.5, 10), (8, 2.5, 14), '#os'),
    ]
    tex = {'particle': 'hxrpbestiaire:blocks/appat_bois', 'bois': 'hxrpbestiaire:blocks/appat_bois',
           'interieur': 'hxrpbestiaire:blocks/appat_interieur', 'os': 'hxrpbestiaire:blocks/appat_os'}
    return el, tex


APPATS = {
    'rayon_de_miel_royal': modele_rayon,
    'charnier': modele_charnier,
    'cercueil_profane': modele_cercueil,
}
TEXTURES_BLOCS = {
    'appat_rayon': tex_rayon,
    'appat_cire': tex_cire,
    'appat_miel': tex_miel,
    'appat_chair': tex_chair,
    'appat_os': tex_os,
    'appat_bois': tex_bois,
    'appat_interieur': tex_interieur,
}
AFFICHAGE = {
    'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.625, 0.625, 0.625]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
    'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
    'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
    'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
    'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
}

# ============================================================================ traductions
NOMS = {
    'itemGroup.hxrpbestiaire': 'HxRP · Bestiaire',
    'tile.hxrpbestiaire.rayon_de_miel_royal.name': 'Rayon de miel royal',
    'tile.hxrpbestiaire.charnier.name': 'Charnier',
    'tile.hxrpbestiaire.cercueil_profane.name': 'Cercueil profané',
    'item.hxrpbestiaire.oeuf_reine_guepe.name': 'Œuf de Reine-guêpe',
    'item.hxrpbestiaire.oeuf_abomination.name': "Œuf d'Abomination",
    'item.hxrpbestiaire.oeuf_necromancien.name': 'Œuf de Nécromancien',
    'item.hxrpbestiaire.dard_de_reine.name': 'Dard de reine',
    'item.hxrpbestiaire.gelee_royale_noire.name': 'Gelée royale noire',
    'item.hxrpbestiaire.miel_noir.name': 'Miel noir',
    'item.hxrpbestiaire.chair_d_abomination.name': "Chair d'abomination",
    'item.hxrpbestiaire.poussiere_d_os_maudite.name': "Poussière d'os maudite",
    'item.hxrpbestiaire.projectile_dard.name': 'Dard empoisonné',
    'item.hxrpbestiaire.projectile_ame.name': "Boule d'âme",
    'item.hxrpbestiaire.projectile_fleche.name': 'Flèche',
    'entity.hxrpbestiaire.reine_guepe.name': 'Reine-guêpe',
    'entity.hxrpbestiaire.guepe_soldat.name': 'Guêpe soldat',
    'entity.hxrpbestiaire.abomination.name': 'Abomination, porteuse de Zobae',
    'entity.hxrpbestiaire.necromancien.name': 'Nécromancien',
    'entity.hxrpbestiaire.archer_squelette.name': 'Archer squelette',
    'entity.hxrpbestiaire.projectile.name': 'Projectile du bestiaire',
}


# ============================================================================ tout
def tout(assets):
    items = os.path.join(assets, 'textures', 'items')
    blocs = os.path.join(assets, 'textures', 'blocks')
    os.makedirs(items, exist_ok=True)
    os.makedirs(blocs, exist_ok=True)
    gen = lambda nom: {'parent': 'item/generated', 'textures': {'layer0': 'hxrpbestiaire:items/' + nom}}
    for nom, f in BUTINS.items():
        f().save(os.path.join(items, nom + '.png'))
        ecrire_json(os.path.join(assets, 'models', 'item', nom + '.json'), gen(nom))
    for boss, (c, t, m) in OEUFS.items():
        oeuf(c, t, m).save(os.path.join(items, 'oeuf_' + boss + '.png'))
        ecrire_json(os.path.join(assets, 'models', 'item', 'oeuf_' + boss + '.json'), gen('oeuf_' + boss))
    projectile_dard().save(os.path.join(items, 'projectile_dard.png'))
    projectile_ame().save(os.path.join(items, 'projectile_ame.png'))
    ecrire_json(os.path.join(assets, 'models', 'item', 'projectile_dard.json'), gen('projectile_dard'))
    ecrire_json(os.path.join(assets, 'models', 'item', 'projectile_ame.json'), gen('projectile_ame'))
    ecrire_json(os.path.join(assets, 'models', 'item', 'projectile_fleche.json'),
                {'parent': 'item/generated', 'textures': {'layer0': 'minecraft:items/arrow'}})
    for nom, f in TEXTURES_BLOCS.items():
        f().save(os.path.join(blocs, nom + '.png'))
    for nom, f in APPATS.items():
        el, tex = f()
        ecrire_json(os.path.join(assets, 'models', 'block', nom + '.json'),
                    {'ambientocclusion': False, 'textures': tex, 'elements': el, 'display': AFFICHAGE})
        ecrire_json(os.path.join(assets, 'models', 'item', nom + '.json'), {'parent': 'hxrpbestiaire:block/' + nom})
        ecrire_json(os.path.join(assets, 'blockstates', nom + '.json'), {'variants': {'normal': {'model': 'hxrpbestiaire:' + nom}}})
    texte = '# HxRP Bestiaire (écrit par tools/bestiaire/gen.py)\n' + ''.join('%s=%s\n' % kv for kv in NOMS.items())
    import sons
    texte += ''.join('subtitles.hxrpbestiaire.%s=%s\n' % (k, v[0]) for k, v in sons.SONS.items())
    for langue in ('fr_fr', 'en_us'):
        p = os.path.join(assets, 'lang', langue + '.lang')
        os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(p, 'w', encoding='utf-8') as f:
            f.write(texte)
    print('art : %d butins, %d œufs, 3 projectiles, %d appâts' % (len(BUTINS), len(OEUFS), len(APPATS)))
