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


def generer(sortie):
    global SORTIE
    SORTIE = sortie
    meuble()
