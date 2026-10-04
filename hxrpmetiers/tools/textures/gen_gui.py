"""
Textures d'interface : carnet de recettes, frigo, poubelle, jauges de faim et de soif.
Même direction artistique que les mini-jeux : laiton, bois, parchemin, contours sombres.

    python3 tools/textures/gen_gui.py
"""
import math
import os

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
OUT = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'hxrpmetiers', 'textures', 'gui')

OL = (92, 42, 8)
OR = (232, 135, 30)
HL = (255, 195, 92)
SH = (178, 87, 16)
CONTOUR = (27, 16, 12)


def hexc(s):
    s = s.lstrip('#')
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


class Img:
    def __init__(self, w, h):
        self.a = np.zeros((h, w, 4), np.uint8)
        self.w, self.h = w, h

    def rect(self, x, y, w, h, c, alpha=255):
        c = hexc(c) if isinstance(c, str) else c
        x0, y0, x1, y1 = max(0, x), max(0, y), min(self.w, x + w), min(self.h, y + h)
        if x1 <= x0 or y1 <= y0:
            return
        self.a[y0:y1, x0:x1, :3] = c
        self.a[y0:y1, x0:x1, 3] = alpha

    def px(self, x, y, c):
        self.rect(x, y, 1, 1, c)

    def noise(self, x, y, w, h, colors, density, seed):
        rs = np.random.RandomState(seed)
        for j in range(y, y + h):
            for i in range(x, x + w):
                if rs.rand() < density:
                    self.px(i, j, hexc(colors[rs.randint(len(colors))]))

    def bevel(self, x, y, w, h, light, dark, fill=None):
        if fill:
            self.rect(x, y, w, h, fill)
        self.rect(x, y, w, 1, light)
        self.rect(x, y, 1, h, light)
        self.rect(x, y + h - 1, w, 1, dark)
        self.rect(x + w - 1, y, 1, h, dark)

    def slot(self, x, y, fond='#8b8b8b', clair='#ffffff', sombre='#373737'):
        """Case d'inventaire 18x18 (la case utile commence à x+1, y+1)."""
        self.rect(x, y, 18, 18, fond)
        self.rect(x, y, 17, 1, sombre)
        self.rect(x, y, 1, 17, sombre)
        self.rect(x + 1, y + 17, 17, 1, clair)
        self.rect(x + 17, y + 1, 1, 17, clair)

    def cadre_laiton(self, x, y, w, h):
        self.rect(x, y, w, h, OL)
        self.rect(x + 1, y + 1, w - 2, h - 2, OR)
        self.rect(x + 1, y + 1, w - 2, 1, HL)
        self.rect(x + 1, y + 1, 1, h - 2, HL)
        self.rect(x + 1, y + h - 2, w - 2, 1, SH)
        self.rect(x + w - 2, y + 1, 1, h - 2, SH)
        self.rect(x + 2, y + 2, w - 4, h - 4, OL)
        for (rx, ry) in ((x + 3, y + 3), (x + w - 4, y + 3), (x + 3, y + h - 4), (x + w - 4, y + h - 4)):
            self.px(rx, ry, (255, 226, 166))

    def save(self, name):
        os.makedirs(OUT, exist_ok=True)
        Image.fromarray(self.a, 'RGBA').save(os.path.join(OUT, name))


# ============================================================================ carnet de recettes
def etoile_9(pleine):
    """Étoile 9x9 dessinée à la main : dorée et bien contrastée si pleine, pâle si vide."""
    motif = ["....o....",
             "...oYo...",
             "oooYYYooo",
             "oYYYYYYYo",
             ".oyyyyyo.",
             "..oyyyo..",
             ".oyyoyyo.",
             ".oyo.oyo.",
             ".oo...oo."]
    if pleine:
        cols = {'o': (138, 62, 6), 'Y': (255, 242, 150), 'y': (255, 190, 32)}
    else:
        cols = {'o': (172, 152, 118), 'Y': (240, 230, 206), 'y': (228, 214, 184)}
    out = np.zeros((9, 9, 4), np.uint8)
    for y, row in enumerate(motif):
        for x, ch in enumerate(row):
            if ch in cols:
                out[y, x, :3] = cols[ch]
                out[y, x, 3] = 255
    return out


def carnet():
    """Carnet ouvert 400x240 en (0,0) ; éléments d'interface sous le livre (v >= 256). Texture 512x512."""
    im = Img(512, 512)
    W, H = 400, 240
    G = W // 2          # reliure
    # couverture en cuir
    im.rect(0, 2, W, H - 2, CONTOUR)
    im.rect(1, 3, W - 2, H - 4, '#6a2a1a')
    im.noise(1, 3, W - 2, H - 4, ['#74301e', '#5e2416', '#7a3622'], 0.35, 1)
    im.rect(2, 4, W - 4, 1, '#8a4a30')
    # pages (légèrement bombées vers la reliure)
    for (x0, x1, gauche) in ((8, G - 2, True), (G + 2, W - 8, False)):
        im.rect(x0, 6, x1 - x0, H - 14, '#e4d4ac')
        for x in range(x0, x1):
            d = (x - x0) / (x1 - x0)
            k = d if gauche else 1 - d
            ombre = max(0.0, (k - 0.85) / 0.15)
            base = np.array(hexc('#f4e8c8')) * (1 - 0.18 * ombre)
            im.a[8:H - 10, x, :3] = base.astype(np.uint8)
            im.a[8:H - 10, x, 3] = 255
        im.noise(x0 + 2, 9, x1 - x0 - 4, H - 20, ['#ecdcb8', '#e8d8b0', '#f8f0d8'], 0.12, 3 if gauche else 4)
        for k in range(3):
            im.rect(x0 + (1 if gauche else 0), H - 10 + k, x1 - x0 - 1, 1, ['#d8c8a0', '#c8b890', '#b8a880'][k])
        for y in range(76, H - 26, 18):
            im.rect(x0 + 8, y, x1 - x0 - 16, 1, '#e8d8b4')
    # reliure centrale
    im.rect(G - 3, 6, 6, H - 12, '#4a1a10')
    im.rect(G - 1, 6, 2, H - 12, '#7a3a24')
    # coins en laiton
    for (cx, cy, sx, sy) in ((0, 2, 1, 1), (W - 1, 2, -1, 1), (0, H - 1, 1, -1), (W - 1, H - 1, -1, -1)):
        for i in range(10):
            for j in range(10 - i):
                im.px(cx + sx * i, cy + sy * j, OR if i + j < 8 else OL)
        im.px(cx + sx * 2, cy + sy * 2, HL)
    # marque-page
    im.rect(W - 30, 0, 8, 22, '#c8302a')
    im.rect(W - 30, 0, 1, 22, '#e8504a')
    im.rect(W - 30, 22, 4, 4, '#c8302a')
    im.rect(W - 26, 22, 4, 2, '#c8302a')
    # --- éléments d'interface (v >= 256)
    # onglets 84x14 : actif (0,256), inactif (0,272)
    for (v, fill, light) in ((256, '#f4e8c8', '#ffffff'), (272, '#c8b890', '#d8c8a0')):
        im.rect(0, v, 84, 14, CONTOUR)
        im.rect(1, v + 1, 82, 13, fill)
        im.rect(1, v + 1, 82, 1, light)
    # ligne sélectionnée (0,288) et survolée (0,308), 176x18
    im.rect(0, 288, 176, 18, '#e8c878')
    im.rect(0, 288, 176, 1, '#f8e0a0')
    im.rect(0, 305, 176, 1, '#b8903a')
    im.rect(0, 308, 176, 18, '#efe0bc')
    # bouton « cuisiner » 72x20 : normal (0,330), survol (0,352), désactivé (0,374)
    for k, (fill, hl, sh) in enumerate(((OR, HL, SH), ('#f8a040', '#ffe0a0', '#c86a20'), ('#9a8a7a', '#b8a898', '#6a5a4a'))):
        v = 330 + k * 22
        im.rect(0, v, 72, 20, OL)
        im.rect(1, v + 1, 70, 18, fill)
        im.rect(1, v + 1, 70, 1, hl)
        im.rect(1, v + 1, 1, 18, hl)
        im.rect(1, v + 18, 70, 1, sh)
        im.rect(70, v + 1, 1, 18, sh)
    # coche (100,256) et croix (112,256) 9x9
    coche = ["........#", ".......##", "......##.", "#....##..", "##..##...", ".####....", "..##.....", ".........", "........."]
    croix = ["#.......#", "##.....##", ".##...##.", "..##.##..", "...###...", "..##.##..", ".##...##.", "##.....##", "#.......#"]
    for (u, motif, c) in ((100, coche, '#3a9a2a'), (112, croix, '#c8302a')):
        for j, row in enumerate(motif):
            for i, ch in enumerate(row):
                if ch == '#':
                    im.px(u + i, 256 + j, c)
    # étoiles 9x9 : pleine (124,256), vide (136,256)
    im.a[256:265, 124:133] = etoile_9(True)
    im.a[256:265, 136:145] = etoile_9(False)
    # ascenseur : rail 6x144 (160,256), curseur 6x16 (170,256)
    im.rect(160, 256, 6, 144, '#c8b890')
    im.rect(161, 257, 4, 142, '#b8a880')
    im.rect(170, 256, 6, 16, OL)
    im.rect(171, 257, 4, 14, OR)
    im.rect(171, 257, 4, 1, HL)
    # champ de recherche 172x14 (180,256) : creux dans le papier + loupe
    im.rect(180, 256, 172, 14, '#9a8664')
    im.rect(181, 257, 171, 13, '#fbf4e0')
    im.rect(181, 257, 170, 1, '#d8c8a4')
    loupe = ["..###...", ".#...#..", "#.....#.", "#.....#.", "#.....#.", ".#...#..", "..####..", "......##", ".......#"]
    for j, row in enumerate(loupe):
        for i, ch in enumerate(row):
            if ch == '#':
                im.px(184 + i, 258 + j, '#7a6450')
    im.save('carnet.png')


# ============================================================================ frigo
def frigo():
    im = Img(256, 256)
    W, H = 176, 168
    im.rect(0, 0, W, H, CONTOUR)
    im.rect(1, 1, W - 2, H - 2, '#8fd3c4')
    im.rect(1, 1, W - 2, 1, '#c8f0e6')
    im.rect(1, 1, 1, H - 2, '#b0e4d8')
    im.rect(1, H - 2, W - 2, 1, '#5a9a8e')
    im.rect(W - 2, 1, 1, H - 2, '#6aa89c')
    im.noise(2, 2, W - 4, H - 4, ['#98dccd', '#86c9ba'], 0.08, 5)
    # intérieur du frigo (blanc bleuté, clayettes)
    im.rect(5, 14, W - 10, 60, '#5a9a8e')
    im.rect(6, 15, W - 12, 58, '#eef8fb')
    for y in (15 + 18, 15 + 36):
        im.rect(6, y + 1, W - 12, 1, '#b8dcea')
    for (x, y) in ((9, 70), (160, 20), (150, 66), (20, 22)):
        for (dx, dy) in ((0, 0), (1, 1), (-1, 1), (0, 2), (1, -1)):
            im.px(x + dx, y + dy, '#ffffff')
    im.rect(6, 15, 3, 58, '#dff0f6')
    for r in range(3):
        for c in range(9):
            im.slot(7 + c * 18, 17 + r * 18, fond='#c8e4ee', clair='#ffffff', sombre='#7aa8b8')
    # inventaire du joueur (cases du conteneur en y = 85 et 143)
    for r in range(3):
        for c in range(9):
            im.slot(7 + c * 18, 84 + r * 18)
    for c in range(9):
        im.slot(7 + c * 18, 142)
    # petit thermomètre
    im.rect(160, 3, 5, 9, '#dfe3e8')
    im.rect(161, 4, 3, 7, '#4a8ad0')
    im.rect(162, 5, 1, 5, '#ffffff')
    im.save('frigo.png')


# ============================================================================ poubelle
def poubelle():
    im = Img(256, 256)
    W, H = 176, 132
    im.rect(0, 0, W, H, CONTOUR)
    im.rect(1, 1, W - 2, H - 2, '#5a5e66')
    im.noise(1, 1, W - 2, H - 2, ['#646870', '#50545c', '#6e727a'], 0.3, 6)
    im.rect(1, 1, W - 2, 1, '#9aa0a8')
    im.rect(1, 1, 1, H - 2, '#80868e')
    im.rect(1, H - 2, W - 2, 1, '#34373c')
    im.rect(W - 2, 1, 1, H - 2, '#34373c')
    # bandes de danger autour de la zone de dépôt
    im.rect(5, 14, W - 10, 24, CONTOUR)
    for x in range(6, W - 5):
        for y in range(15, 37):
            if y in (15, 16, 35, 36) or x in (6, 7, W - 7, W - 6):
                im.px(x, y, '#f0c020' if ((x + y) // 4) % 2 == 0 else '#1a1b1e')
    im.rect(8, 17, W - 16, 18, '#2a2c32')
    for c in range(9):
        im.slot(7 + c * 18, 17, fond='#3a3c42', clair='#6e727a', sombre='#1a1b1e')
    for r in range(3):
        for c in range(9):
            im.slot(7 + c * 18, 48 + r * 18)
    for c in range(9):
        im.slot(7 + c * 18, 106)
    im.save('poubelle.png')


# ============================================================================ jauges du HUD
def hud():
    """128x32 : cadre 72x9 en (0,0), remplissage faim 70x7 en (0,9), soif en (0,16),
    icônes 9x9 : faim (80,0), faim clignotante (89,0), soif (80,9), soif clignotante (89,9)."""
    im = Img(128, 32)
    # cadre en laiton
    im.rect(0, 0, 72, 9, OL)
    im.rect(1, 1, 70, 7, '#2a1508')
    im.rect(0, 0, 72, 1, OL)
    im.px(0, 0, (0, 0, 0))
    for x in range(8, 72, 12):
        im.rect(x, 8, 1, 1, OR)
    # remplissages
    for (v, a, b, hl) in ((9, '#f0a040', '#b85a1a', '#ffd08a'), (16, '#5ab0f0', '#2a6ac8', '#b8e4ff')):
        for y in range(7):
            t = y / 6
            c = tuple(int(hexc(a)[i] * (1 - t) + hexc(b)[i] * t) for i in range(3))
            im.rect(0, v + y, 70, 1, c)
        im.rect(0, v, 70, 1, hl)
        for x in range(9, 70, 12):
            im.rect(x, v + 1, 1, 6, tuple(max(0, c - 40) for c in hexc(b)))
    # icône faim : pilon
    def pilon(u, v, chair, os_):
        pts = {(1, 1): chair, (2, 1): chair, (3, 1): chair, (0, 2): chair, (1, 2): chair, (2, 2): chair, (3, 2): chair, (4, 2): chair,
               (0, 3): chair, (1, 3): chair, (2, 3): chair, (3, 3): chair, (4, 3): chair, (1, 4): chair, (2, 4): chair, (3, 4): chair,
               (4, 4): chair, (3, 5): os_, (4, 5): os_, (5, 6): os_, (6, 6): os_, (6, 7): os_, (7, 6): os_}
        for (x, y), c in pts.items():
            im.px(u + x, v + y, c)
        im.px(u + 1, v + 2, (255, 214, 150))
        for (x, y) in ((0, 1), (1, 0), (2, 0), (3, 0), (4, 1), (5, 2), (5, 3), (5, 4), (4, 5), (0, 4), (1, 5), (2, 5), (7, 5), (8, 6), (7, 7), (6, 8), (5, 7)):
            im.px(u + x, v + y, CONTOUR)
    pilon(80, 0, (200, 112, 40), (240, 232, 216))
    pilon(89, 0, (230, 64, 42), (255, 220, 200))
    # icône soif : goutte
    def goutte(u, v, c, hl):
        rows = ["....#....", "...###...", "...###...", "..#####..", ".#######.", ".#######.", ".#######.", "..#####..", "...###..."]
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                if ch == '#':
                    im.px(u + i, v + j, c)
        im.px(u + 3, v + 4, hl)
        im.px(u + 3, v + 5, hl)
    goutte(80, 9, (58, 140, 230), (220, 245, 255))
    goutte(89, 9, (230, 64, 42), (255, 220, 200))
    im.save('hud.png')


if __name__ == '__main__':
    for old in ('bol_swirl.png', 'couper.png', 'etaler.png', 'props.png', 'scene_acier.png', 'scene_bois.png', 'scene_bois_farine.png',
                'scene_bol.png', 'scene_couper.png', 'scene_etaler.png', 'ui.png'):
        p = os.path.join(OUT, old)
        if os.path.exists(p):
            os.remove(p)
    carnet()
    frigo()
    poubelle()
    hud()
    print('textures d\'interface générées dans', OUT)
