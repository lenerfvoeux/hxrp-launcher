"""
Textures d'interface du Hunter Virus : meuble à tiroirs, formulaire de l'officine, Grimoire, carnet de consultation,
ordonnance et parchemin. Même direction artistique que le Gourmet (laiton, contours sombres) avec les matières
de l'apothicaire : bois laqué brun-rouge, papier, cordelettes.
"""
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, '..', '..', 'textures'))
import gen_gui as G  # noqa: E402

Img, OL, OR, HL, SH, CONTOUR = G.Img, G.OL, G.OR, G.HL, G.SH, G.CONTOUR
hexc = G.hexc
SORTIE = None


def enregistrer(im, nom):
    os.makedirs(SORTIE, exist_ok=True)
    Image.fromarray(im.a, 'RGBA').save(os.path.join(SORTIE, nom))


def panneau_laque(im, x, y, w, h, seed=1):
    """Panneau de bois laqué brun-rouge, cerclé de laiton, avec des coins rivetés."""
    im.rect(x, y, w, h, CONTOUR)
    im.cadre_laiton(x + 1, y + 1, w - 2, h - 2)
    im.rect(x + 4, y + 4, w - 8, h - 8, '#5c1c14')
    im.noise(x + 4, y + 4, w - 8, h - 8, ['#642016', '#541812', '#6a2418'], 0.25, seed)
    for k in range(x + 10, x + w - 6, 23):
        im.rect(k, y + 4, 1, h - 8, '#4e1610')
    im.rect(x + 4, y + 4, w - 8, 1, '#8a3a28')
    im.rect(x + 4, y + 4, 1, h - 8, '#7a3020')
    im.rect(x + 4, y + h - 5, w - 8, 1, '#2a0a06')
    im.rect(x + w - 5, y + 4, 1, h - 8, '#2a0a06')


def case_laque(im, x, y):
    """Case 18x18 creusée dans le bois laqué (la case utile commence à x+1, y+1)."""
    im.slot(x, y, fond='#3a1410', clair='#8a4a34', sombre='#1a0604')


def meuble():
    """176x246 : 54 tiroirs (y 18), tiroir des seringues (y 132), inventaire (y 164) et barre (y 222)."""
    im = Img(256, 256)
    W, H = 176, 246
    panneau_laque(im, 0, 0, W, H, 21)
    for r in range(6):
        for c in range(9):
            case_laque(im, 7 + c * 18, 17 + r * 18)
    # bandeau du tiroir de seringues : papier, cordelette
    im.rect(5, 127, W - 10, 26, CONTOUR)
    im.rect(6, 128, W - 12, 24, '#3a100c')
    im.slot(7, 131, fond='#e8ecef', clair='#ffffff', sombre='#7a8a98')
    im.rect(27, 129, W - 34, 20, '#efe2c0')
    im.noise(27, 129, W - 34, 20, ['#e8d8b0', '#f6ecd4'], 0.15, 22)
    im.rect(27, 129, W - 34, 1, '#ffffff')
    im.rect(27, 148, W - 34, 1, '#c8b890')
    im.rect(W - 18, 130, 2, 18, '#b8241c')
    im.rect(W - 19, 147, 4, 3, '#8a1a12')
    for r in range(3):
        for c in range(9):
            case_laque(im, 7 + c * 18, 163 + r * 18)
    for c in range(9):
        case_laque(im, 7 + c * 18, 221)
    enregistrer(im, 'meuble.png')


# ============================================================================ livres
def livre(im, W, H, cuir, cuir_bruit, page, coins=True, signet='#c8302a', filet=None):
    """Livre ouvert W x H en (0,0) : couverture, deux pages bombées vers la reliure, coins de laiton, signet."""
    G2 = W // 2
    im.rect(0, 2, W, H - 2, CONTOUR)
    im.rect(1, 3, W - 2, H - 4, cuir)
    im.noise(1, 3, W - 2, H - 4, cuir_bruit, 0.35, 1)
    if filet:
        im.rect(4, 5, W - 8, 1, filet)
        im.rect(4, H - 4, W - 8, 1, filet)
    for (x0, x1, gauche) in ((8, G2 - 2, True), (G2 + 2, W - 8, False)):
        im.rect(x0, 6, x1 - x0, H - 14, page[0])
        for x in range(x0, x1):
            d = (x - x0) / (x1 - x0)
            k = d if gauche else 1 - d
            ombre = max(0.0, (k - 0.85) / 0.15)
            base = np.array(hexc(page[1])) * (1 - 0.18 * ombre)
            im.a[8:H - 10, x, :3] = base.astype(np.uint8)
            im.a[8:H - 10, x, 3] = 255
        im.noise(x0 + 2, 9, x1 - x0 - 4, H - 20, page[2:], 0.12, 3 if gauche else 4)
        for k in range(3):
            im.rect(x0 + (1 if gauche else 0), H - 10 + k, x1 - x0 - 1, 1, ['#d8c8a0', '#c8b890', '#b8a880'][k])
    im.rect(G2 - 3, 6, 6, H - 12, '#3a0a06')
    im.rect(G2 - 1, 6, 2, H - 12, '#6a2418')
    if coins:
        for (cx, cy, sx, sy) in ((0, 2, 1, 1), (W - 1, 2, -1, 1), (0, H - 1, 1, -1), (W - 1, H - 1, -1, -1)):
            for i in range(10):
                for j in range(10 - i):
                    im.px(cx + sx * i, cy + sy * j, OR if i + j < 8 else OL)
            im.px(cx + sx * 2, cy + sy * 2, HL)
    if signet:
        im.rect(W - 30, 0, 8, 22, signet)
        im.rect(W - 30, 0, 1, 22, '#e8504a')
        im.rect(W - 30, 22, 4, 4, signet)
        im.rect(W - 26, 22, 4, 2, signet)


def elements_carnet(im):
    """Mêmes éléments d'interface que le carnet du Gourmet (onglets, lignes, bouton, coche, étoiles, ascenseur, recherche)."""
    for (v, fill, light) in ((256, '#f4e8c8', '#ffffff'), (272, '#c8b890', '#d8c8a0')):
        im.rect(0, v, 84, 14, CONTOUR)
        im.rect(1, v + 1, 82, 13, fill)
        im.rect(1, v + 1, 82, 1, light)
    im.rect(0, 288, 176, 18, '#d8b8a0')
    im.rect(0, 288, 176, 1, '#f0d4bc')
    im.rect(0, 305, 176, 1, '#a8705a')
    im.rect(0, 308, 176, 18, '#efe0d0')
    for k, (fill, hl, sh) in enumerate(((OR, HL, SH), ('#f8a040', '#ffe0a0', '#c86a20'), ('#9a8a7a', '#b8a898', '#6a5a4a'))):
        v = 330 + k * 22
        im.rect(0, v, 72, 20, OL)
        im.rect(1, v + 1, 70, 18, fill)
        im.rect(1, v + 1, 70, 1, hl)
        im.rect(1, v + 1, 1, 18, hl)
        im.rect(1, v + 18, 70, 1, sh)
        im.rect(70, v + 1, 1, 18, sh)
    coche = ["........#", ".......##", "......##.", "#....##..", "##..##...", ".####....", "..##.....", ".........", "........."]
    croix = ["#.......#", "##.....##", ".##...##.", "..##.##..", "...###...", "..##.##..", ".##...##.", "##.....##", "#.......#"]
    for (u, motif, c) in ((100, coche, '#3a9a2a'), (112, croix, '#c8302a')):
        for j, row in enumerate(motif):
            for i, ch in enumerate(row):
                if ch == '#':
                    im.px(u + i, 256 + j, c)
    im.a[256:265, 124:133] = G.etoile_9(True)
    im.a[256:265, 136:145] = G.etoile_9(False)
    im.rect(160, 256, 6, 144, '#c8b890')
    im.rect(161, 257, 4, 142, '#b8a880')
    im.rect(170, 256, 6, 16, OL)
    im.rect(171, 257, 4, 14, OR)
    im.rect(171, 257, 4, 1, HL)
    im.rect(180, 256, 172, 14, '#9a8664')
    im.rect(181, 257, 171, 13, '#fbf4e0')
    im.rect(181, 257, 170, 1, '#d8c8a4')
    loupe = ["..###...", ".#...#..", "#.....#.", "#.....#.", "#.....#.", ".#...#..", "..####..", "......##", ".......#"]
    for j, row in enumerate(loupe):
        for i, ch in enumerate(row):
            if ch == '#':
                im.px(184 + i, 258 + j, '#7a6450')


def formulaire():
    """Formulaire de l'officine : livre relié de laque brun-rouge, filets dorés, cordelette rouge."""
    im = Img(512, 512)
    livre(im, 400, 240, '#5c1c14', ['#642016', '#541812', '#6a2418'], ['#e4d4ac', '#f4e8c8', '#ecdcb8', '#e8d8b0', '#f8f0d8'],
          signet='#2e6a4a', filet='#c8922a')
    # sceau de l'apothicaire en haut de la page de droite
    for (x, y) in ((388, 16),):
        im.rect(x - 3, y - 3, 7, 7, '#b8241c')
        im.px(x, y, '#f0c0a0')
    elements_carnet(im)
    enregistrer(im, 'formulaire.png')


def grimoire():
    """Grimoire des maladies : cuir sombre, coins et filets d'or, ruban rouge ; flèches et onglets sous le livre."""
    im = Img(512, 512)
    livre(im, 400, 240, '#2a1a12', ['#33211a', '#24160e', '#3a2618'], ['#ddcc9e', '#efe0b8', '#e4d4a8', '#e0cfa2', '#f4e8c4'],
          signet='#8a1e14', filet='#c8922a')
    # flèches 18x12 : gauche (0,256) normal / (20,256) survol ; droite (40,256) / (60,256)
    for (u, droite, hl) in ((0, True, False), (20, True, True), (40, False, False), (60, False, True)):
        fond = '#f8d890' if hl else '#c8922a'
        for j in range(12):
            w = 6 - abs(j - 5.5)
            for i in range(18):
                x = i if droite else 17 - i
                if x < 10 and abs(j - 5.5) <= x * 0.6 + 0.5:
                    im.px(u + i, 256 + j, fond)
                elif 10 <= x and 3 <= j <= 8:
                    im.px(u + i, 256 + j, fond)
        im.rect(u, 256, 18, 1, OL)
    # onglets de ruban 26x26 : actif (80,256), inactif (80,284)
    for (v, c, cl) in ((256, '#a8281c', '#d8483a'), (284, '#6a1810', '#8a2a1e')):
        im.rect(80, v, 26, 26, CONTOUR)
        im.rect(81, v + 1, 24, 24, c)
        im.rect(81, v + 1, 24, 2, cl)
        im.rect(81, v + 23, 24, 1, '#3a0a06')
    enregistrer(im, 'grimoire.png')


def carnet_consultation():
    """Carnet de consultation : couverture de cuir brun, papier quadrillé, marge rouge ; cases à cocher sous le livre."""
    im = Img(512, 512)
    livre(im, 400, 240, '#5a3a22', ['#644228', '#4e321c', '#6a4a2e'], ['#e8e4d4', '#f6f4ea', '#eeeadc', '#ecE8d8', '#fafaf2'],
          coins=False, signet='#2a5aa8')
    for x0, x1 in ((8, 198), (202, 392)):
        for y in range(14, 228, 8):
            im.rect(x0 + 2, y, x1 - x0 - 4, 1, '#d4dce8')
        im.rect(x0 + 12, 8, 1, 222, '#e8a0a0')
    # case vide (0,256) et cochée (10,256), 9x9
    for (u, cochee) in ((0, False), (10, True)):
        im.rect(u, 256, 9, 9, '#3a4a6a')
        im.rect(u + 1, 257, 7, 7, '#fafaf2')
        if cochee:
            for (i, j) in ((1, 4), (2, 5), (3, 6), (4, 5), (5, 4), (6, 3), (7, 2)):
                im.px(u + i, 256 + j, '#2e7a1e')
                im.px(u + i, 256 + j - 1, '#2e7a1e')
    enregistrer(im, 'carnet_consultation.png')


def ordonnance():
    """Feuille d'ordonnance 200x240 : papier, en-tête de l'officine, filets, cachet de cire."""
    im = Img(256, 256)
    W, H = 200, 240
    im.rect(2, 3, W - 2, H - 3, '#5a4a3a')
    im.rect(0, 0, W - 2, H - 3, CONTOUR)
    im.rect(1, 1, W - 4, H - 5, '#f4ecd4')
    im.noise(1, 1, W - 4, H - 5, ['#ece2c4', '#f8f2e0'], 0.12, 31)
    im.rect(12, 30, W - 26, 1, '#9a2a1e')
    im.rect(12, 32, W - 26, 1, '#9a2a1e')
    for y in range(44, H - 40, 11):
        im.rect(12, y + 9, W - 26, 1, '#e4d8bc')
    # coupe et serpent de l'apothicaire
    for (x, y) in ((22, 12), (23, 13), (21, 14), (22, 15), (23, 16), (22, 17), (21, 18)):
        im.px(x, y, '#2e6a4a')
    im.rect(19, 20, 7, 2, '#c8922a')
    im.rect(21, 22, 3, 4, '#c8922a')
    # cachet de cire en bas à gauche
    for j in range(-5, 6):
        for i in range(-5, 6):
            if i * i + j * j <= 25:
                im.px(26 + i, H - 26 + j, '#b8241c' if i * i + j * j <= 16 else '#8a1810')
    enregistrer(im, 'ordonnance.png')


def generer(sortie):
    global SORTIE
    SORTIE = sortie
    meuble()
    formulaire()
    grimoire()
    carnet_consultation()
    ordonnance()
