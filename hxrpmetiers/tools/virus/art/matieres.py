"""
Matières de l'officine du Hunter Virus (textures 32x32, 2 texels par unité de modèle), dans l'esprit des
« Carnets de l'apothicaire » : bois laqué brun-rouge, céramique émaillée (céladon, bleu et blanc), fonte,
cuivre et laiton, papier, cordelettes. Toutes enregistrées dans TEX et écrites dans textures/blocks/virus/.
"""
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, '..', '..', 'models'))
sys.path.insert(0, os.path.join(HERE, '..', '..', 'textures'))
import materials as M  # noqa: E402
from pix import *  # noqa: E402,F401,F403

S = 32
TEX = {}


def tex(name):
    def deco(fn):
        TEX[name] = fn
        return fn
    return deco


new, noise, hstreaks, vstreaks, box_bevel = M.new, M.noise, M.hstreaks, M.vstreaks, M.box_bevel


def anneau(c, cx, cy, r, th, color):
    c.fill(circle(cx, cy, r) & ~circle(cx, cy, r - th), color)


# ============================================================================ matières de base
@tex('laque')
def laque():
    """Bois laqué brun-rouge, veinage discret et reflets de vernis."""
    c = new('#5c1c14')
    vstreaks(c, ['#6a2418', '#4e1610', '#642016'], 60, 101, lmin=8, lmax=24)
    for x in (5, 17, 26):
        for y in range(S):
            if (y + x) % 7 < 4:
                c.px[y, x, :3] = col('#7a3020')
    return c


@tex('laque_sombre')
def laque_sombre():
    c = new('#3a100c')
    vstreaks(c, ['#44140e', '#300c08'], 40, 102, lmin=6, lmax=18)
    return c


@tex('laque_noire')
def laque_noire():
    c = new('#1e1414')
    hstreaks(c, ['#2a1c1a', '#181010', '#342420'], 40, 103)
    return c


@tex('bois_brut')
def bois_brut():
    c = new('#a8743e')
    vstreaks(c, ['#b88450', '#98683a', '#c09060'], 70, 104, lmin=6, lmax=20)
    return c


@tex('bois_clair_v')
def bois_clair_v():
    c = new('#d4a86a')
    vstreaks(c, ['#e0b87a', '#c8985a', '#e8c088'], 60, 105, lmin=6, lmax=20)
    return c


@tex('fonte_v')
def fonte_v():
    c = new('#2c2e32')
    noise(c, ['#363840', '#24262a', '#3c3e46', '#1e2024'], 0.45, 106)
    return c


@tex('fonte_rouille')
def fonte_rouille():
    c = new('#2e2a2a')
    noise(c, ['#3a3434', '#262222', '#4a3020', '#5a3a22'], 0.45, 107)
    return c


@tex('laiton_v')
def laiton_v():
    c = new('#c8922a')
    hstreaks(c, ['#d8a438', '#b88020', '#e8be58'], 50, 108)
    return c


@tex('cuivre')
def cuivre():
    c = new('#b86a3a')
    for y in range(S):
        v = 0.5 + 0.5 * math.sin(y / 31 * math.pi * 1.4 + 0.4)
        c.px[y, :, :3] = mix('#7a3a1a', '#f0a070', v)
    vstreaks(c, ['#e8986a', '#a85a2a', '#c87a4a'], 30, 109, lmin=4, lmax=16)
    noise(c, ['#5a9a7a'], 0.02, 110)     # vert-de-gris
    return c


@tex('ceramique_celadon')
def ceramique_celadon():
    c = new('#9ac8b0')
    for y in range(S):
        v = 0.5 + 0.5 * math.sin(y / 31 * math.pi)
        c.px[y, :, :3] = mix('#7aa890', '#c8e8d4', v)
    noise(c, ['#b0d8c4', '#8ab8a0'], 0.12, 111)
    return c


@tex('porcelaine_bleue')
def porcelaine_bleue():
    """Porcelaine blanche à motifs bleus (rinceaux)."""
    c = new('#eef2f2')
    for y in range(S):
        v = 0.5 + 0.5 * math.sin(y / 31 * math.pi)
        c.px[y, :, :3] = mix('#c8d4d8', '#ffffff', v)
    c.fill(rect(0, 4, 31, 5), '#2a4a9a')
    c.fill(rect(0, 26, 31, 27), '#2a4a9a')
    for k in range(4):
        cx = 4 + k * 8
        c.fill(arc_band(cx, 16, 3.5, 3.5, 1, 200, 340), '#3a5aaa')
        c.fill(arc_band(cx + 3, 14, 2, 2, 1, 20, 160), '#3a5aaa')
        c.fill(rect(cx, 19, cx + 1, 19), '#3a5aaa')
    return c


@tex('terre_emaillee')
def terre_emaillee():
    """Grès brun émaillé (jarres), coulures plus claires."""
    c = new('#7a4a2a')
    for y in range(S):
        v = 0.5 + 0.5 * math.sin(y / 31 * math.pi * 1.1)
        c.px[y, :, :3] = mix('#5a3018', '#a8683a', v)
    rs = np.random.RandomState(112)
    for _ in range(9):
        x = rs.randint(0, S)
        L = rs.randint(5, 14)
        for y in range(L):
            c.px[y, x, :3] = col('#b8884a' if y < L - 2 else '#94603a')
    return c


@tex('papier')
def papier():
    c = new('#efe2c0')
    noise(c, ['#e8d8b0', '#f6ecd4', '#e4d4a8'], 0.18, 113)
    return c


@tex('corde')
def corde():
    c = new('#c8a060')
    for y in range(S):
        for x in range(S):
            if (x + y) % 4 < 2:
                c.px[y, x, :3] = col('#a8803e')
    return c


@tex('corde_rouge')
def corde_rouge():
    c = new('#b8241c')
    for y in range(S):
        for x in range(S):
            if (x + y) % 4 < 2:
                c.px[y, x, :3] = col('#8a1a12')
    return c


@tex('verre_v')
def verre_v():
    c = M.Canvas()
    c.px[:, :, :3] = col('#cfe6ee')
    c.px[:, :, 3] = 120
    c.px[2:30, 3:5, :3] = col('#ffffff')
    c.px[2:30, 3:5, 3] = 200
    c.px[0, :, 3] = 200
    c.px[31, :, 3] = 200
    return c


@tex('braises_v')
def braises_v():
    return M.braise()


@tex('flamme_v')
def flamme_v():
    return M.flamme()


@tex('vapeur_v')
def vapeur_v():
    return M.vapeur()


# ============================================================================ meuble à tiroirs
COLS, ROWS = 4, 5


def tiroir(c, x0, y0, x1, y1, seed):
    """Un tiroir : cadre en relief, étiquette de papier avec deux caractères, anneau de laiton."""
    box_bevel(c, x0, y0, x1, y1, '#7a3020', '#2a0a06', fill='#5c1c14')
    lx0, lx1 = x0 + 2, x1 - 2
    ly0, ly1 = y0 + 1, y0 + 3
    c.fill(rect(lx0, ly0, lx1, ly1), '#efe2c0')
    c.fill(rect(lx0, ly1, lx1, ly1), '#c8b890')
    rs = np.random.RandomState(seed)
    for k in range(2):
        gx = lx0 + 1 + k * 3
        if gx + 1 <= lx1 - 1:
            c.fill(rect(gx, ly0 + rs.randint(0, 2), gx + rs.randint(0, 2), ly0 + 1), '#2a1a12')
    cx = (x0 + x1) / 2.0
    anneau(c, cx, y1 - 1.6, 1.6, 0.9, '#e8be58')
    c.fill(rect(int(cx), y1 - 3, int(cx), y1 - 3), '#8a5a10')


@tex('meuble_face')
def meuble_face():
    c = new('#3a100c')
    # projection : x de 0.5 à 15.5 -> texels ; y de 1 à 15 (cadre) -> rangées de tiroirs
    w = (30 - 2) / COLS
    h = (28 - 2) / ROWS
    for r in range(ROWS):
        for k in range(COLS):
            x0 = int(round(2 + k * w))
            x1 = int(round(2 + (k + 1) * w)) - 1
            y0 = int(round(3 + r * h))
            y1 = int(round(3 + (r + 1) * h)) - 1
            tiroir(c, x0, y0, x1, y1, 200 + r * 7 + k)
    box_bevel(c, 0, 0, 31, 31, '#7a3020', '#1e0604')
    c.fill(rect(1, 29, 30, 30), '#3a100c')
    return c


@tex('meuble_cote')
def meuble_cote():
    c = laque()
    box_bevel(c, 0, 0, 31, 31, '#7a3020', '#2a0a06')
    box_bevel(c, 3, 4, 28, 27, '#2a0a06', '#7a3020')
    # coins de laiton
    for (x, y) in ((1, 1), (28, 1), (1, 28), (28, 28)):
        c.fill(rect(x, y, x + 2, y + 2), '#c8922a')
        c.fill(rect(x, y, x, y), '#f0d080')
    return c


@tex('meuble_dessus')
def meuble_dessus():
    c = laque()
    for y in range(S):
        if y % 8 == 0:
            c.fill(rect(0, y, 31, y), '#4a140e')
    box_bevel(c, 0, 0, 31, 31, '#8a3a28', '#2a0a06')
    return c


def save_all(out_dir):
    os.makedirs(out_dir, exist_ok=True)
    for name, fn in TEX.items():
        fn().image().save(os.path.join(out_dir, name + '.png'))
