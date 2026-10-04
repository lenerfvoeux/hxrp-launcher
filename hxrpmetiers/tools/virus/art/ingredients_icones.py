"""
Icônes 32x32 des 84 ingrédients du Hunter Virus, dans le style du Gourmet (tools/textures : lumière en haut
à gauche, rampes à décalage de teinte, liseré sélectif, contour posé par le générateur).
Une petite bibliothèque botanique (brins, fleurs, épis, racines, champignons, écorces…) puis un dessin par
ingrédient. Chaque fonction reçoit un Canvas.
"""
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, '..', '..', 'textures'))
from pix import *  # noqa: E402,F401,F403
from parts import leaf, stem  # noqa: E402

REG = {}


def icone(*ids):
    def deco(fn):
        for i in ids:
            REG[i] = fn
        return fn
    return deco


# ============================================================================ bibliothèque botanique
def brin(c, pts, couleur, w=1):
    stem(c, pts, couleur, width=w)


def capitule(c, x, y, r, petale, coeur, n=10, coeur_r=None):
    """Fleur en marguerite : rayons de pétales autour d'un cœur bombé."""
    for k in range(n):
        a = k * 2 * math.pi / n
        px, py = x + math.cos(a) * r * 0.62, y + math.sin(a) * r * 0.62
        c.shape(ellipse(px, py, r * 0.42, r * 0.42), R(petale, light=1.15), L_sphere(px - 0.4, py - 0.4, r * 0.42, r * 0.42), edge=False)
    cr = coeur_r or r * 0.42
    c.shape(circle(x, y, cr), R(coeur, light=1.2), L_sphere(x - 0.6, y - 0.6, cr, cr), hl=1)


def fleur5(c, x, y, r, petale, coeur='#f0c020'):
    for k in range(5):
        a = -math.pi / 2 + k * 2 * math.pi / 5
        px, py = x + math.cos(a) * r * 0.55, y + math.sin(a) * r * 0.55
        c.shape(ellipse(px, py, r * 0.5, r * 0.5), R(petale, light=1.15), L_sphere(px - 0.5, py - 0.5, r * 0.5, r * 0.5), edge=False)
    c.fill(circle(x, y, max(0.8, r * 0.25)), coeur)


def ombelle(c, x, y, r, couleur, n=14, seed=1):
    g = np.random.RandomState(seed)
    for k in range(n):
        a = g.uniform(0, 2 * math.pi)
        d = math.sqrt(g.uniform(0, 1)) * r
        px, py = x + math.cos(a) * d, y + math.sin(a) * d * 0.7
        c.shape(circle(px, py, 1.3), R(couleur, light=1.2), L_sphere(px - 0.3, py - 0.3, 1.3, 1.3), edge=False)


def epi(c, x0, y0, x1, y1, couleur, ep=1.6, pas=1.8):
    """Épi de petits boutons le long d'une ligne (lavande, plantain)."""
    L = math.hypot(x1 - x0, y1 - y0)
    n = max(2, int(L / pas))
    for k in range(n):
        t = k / (n - 1)
        x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
        r = ep * (0.75 + 0.35 * math.sin(t * math.pi))
        for s in (-1, 1):
            px = x + s * r * 0.55
            c.shape(ellipse(px, y, r * 0.7, r * 0.6), R(couleur, light=1.2), L_sphere(px - 0.3, y - 0.3, r, r), edge=False)


def baies(c, pts, r, couleur, hl=1):
    for (x, y) in pts:
        c.shape(circle(x, y, r), R(couleur, light=1.3), L_sphere(x - r * 0.4, y - r * 0.4, r, r), hl=hl)


def racine(c, pts, couleur, w0=4.5, w1=1.2, radicelles=6, seed=3):
    """Racine épaisse effilée le long d'une polyligne, avec des radicelles."""
    n = len(pts) - 1
    rr = R(couleur, light=1.15)
    for i in range(n):
        (a, b), (e, f) = pts[i], pts[i + 1]
        t0, t1 = i / n, (i + 1) / n
        m = capsule(a, b, e, f, w0 * (1 - t0) + w1 * t0, w0 * (1 - t1) + w1 * t1)
        c.shape(m, rr, L_cyl(a, b, e, f, w0), edge=True)
    g = np.random.RandomState(seed)
    for k in range(radicelles):
        i = g.randint(0, n)
        (a, b), (e, f) = pts[i], pts[i + 1]
        t = g.uniform(0.2, 0.9)
        x, y = a + (e - a) * t, b + (f - b) * t
        s = 1 if g.rand() < 0.5 else -1
        c.fill(line_mask(x, y, x + s * g.uniform(2, 4), y + g.uniform(1, 3)), rr[1])
    # anneaux de croissance
    for i in range(n):
        (a, b), (e, f) = pts[i], pts[i + 1]
        for t in (0.3, 0.7):
            x, y = a + (e - a) * t, b + (f - b) * t
            c.fill(rect(int(x), int(y), int(x), int(y)), rr[1])


def champi(c, x, base, h, rx, ry, chapeau, pied='#efe6d4', lames=None, style='bombe', taches=None, hl=2):
    """Champignon : pied puis chapeau (bombé, plat, entonnoir, conique)."""
    top = base - h
    c.shape(rrect(int(x - max(1.5, rx * 0.22)), int(top), int(x + max(1.5, rx * 0.22)), int(base), 1), R(pied), L_horiz(x - 2, x + 2))
    if style == 'entonnoir':
        m = polygon([(x - rx, top - ry), (x + rx, top - ry), (x + 1.5, top + ry * 0.6), (x - 1.5, top + ry * 0.6)])
        c.shape(m, R(chapeau, light=1.2), L_sphere(x - rx * 0.4, top - ry, rx, ry * 2, 0.2), hl=hl)
        c.shape(ellipse(x, top - ry, rx, ry * 0.45), R(mix(chapeau, '#000000', 0.25)), L_const(0.4))
    elif style == 'conique':
        m = polygon([(x, top - ry * 2.2), (x + rx, top + 0.5), (x - rx, top + 0.5)]) | ellipse(x, top, rx, ry * 0.5)
        c.shape(m, R(chapeau, light=1.2), L_sphere(x - rx * 0.4, top - ry, rx, ry * 2, 0.2), hl=hl)
    else:
        cap = ellipse(x, top, rx, ry) & (YY <= top + (ry * 0.35 if style == 'bombe' else 0.5))
        c.shape(cap, R(chapeau, light=1.2), L_sphere(x - rx * 0.35, top - ry * 0.5, rx, ry, 0.3), hl=hl)
        if lames:
            c.fill(rect(int(x - rx * 0.9), int(top + ry * 0.35), int(x + rx * 0.9), int(top + ry * 0.35)) & ellipse(x, top, rx, ry), lames)
    if taches:
        for (dx, dy, r) in taches:
            c.fill(circle(x + dx, top + dy, r) & ellipse(x, top, rx, ry), '#f4f0e6')


def tas(c, cx, cy, rx, ry, couleur, grain=None, n=0, seed=4):
    c.shape(ellipse(cx, cy, rx, ry) & (YY <= cy + ry * 0.6), R(couleur, light=1.2), L_sphere(cx - rx * 0.3, cy - ry, rx, ry * 1.4, 0.2))
    if grain:
        g = np.random.RandomState(seed)
        for _ in range(n):
            a = g.uniform(math.pi, 2 * math.pi)
            d = math.sqrt(g.uniform(0, 1))
            x, y = cx + math.cos(a) * rx * 0.85 * d, cy + math.sin(a) * ry * 0.85 * d + ry * 0.2
            c.fill(rect(int(x), int(y), int(x) + 1, int(y)), grain)


def sachet(c, couleur_papier='#e8d8b0'):
    """Petit sachet de papier ouvert (graines, poudres)."""
    c.shape(polygon([(8, 13), (24, 13), (26, 28), (6, 28)]), R(couleur_papier, light=1.1), L_horiz(6, 26, 0.85, 0.35))
    c.shape(polygon([(8, 13), (24, 13), (23, 16), (9, 16)]), R(mix(couleur_papier, '#7a5a3a', 0.25)), L_const(0.5))


def fiole_ingredient(c, contenu, verre='#d8ecf4', bouchon='#b07a44'):
    """Petit flacon d'ingrédient récolté (sève, encre)."""
    c.shadow_under(16, 28.5, 8, 2, 0.25)
    corps = rrect(10, 12, 22, 28, 3)
    c.shape(corps, R(verre), L_horiz(10, 22, 0.9, 0.4))
    c.shape(rrect(11, 16, 21, 27, 2), R(contenu, light=1.3), L_horiz(11, 21, 0.85, 0.35), edge=False)
    c.shape(rect(13, 8, 19, 12), R(verre), L_horiz(13, 19, 0.9, 0.4))
    c.shape(rrect(12, 5, 20, 9, 1), R(bouchon), L_horiz(12, 20, 0.85, 0.35), hl=1)
    c.fill(rect(12, 14, 12, 25), '#ffffff')


# ============================================================================ 34 plantes
@icone('camomille')
def camomille(c):
    for (x, y) in ((10, 9), (21, 7), (16, 16)):
        brin(c, [(16, 31), (x, y + 3)], '#6a9a3a')
    for (x0, y0, x1, y1) in ((14, 26, 7, 22), (17, 24, 25, 20)):
        leaf(c, x0, y0, x1, y1, 2.5, '#5a9a3a', vein=False)
    for (x, y, r) in ((10, 9, 5), (21, 7, 5), (16, 16, 5.5)):
        capitule(c, x, y, r, '#f4f4ec', '#f0b820', n=11)


@icone('souci')
def souci(c):
    brin(c, [(16, 31), (15, 13)], '#5a8a2a', 2)
    for (x0, y0, x1, y1) in ((15, 27, 6, 23), (16, 24, 26, 21), (15, 19, 8, 16)):
        leaf(c, x0, y0, x1, y1, 4, '#6aa03a')
    capitule(c, 15, 10, 8.5, '#f08a14', '#c85a08', n=14, coeur_r=3)


@icone('pissenlit')
def pissenlit(c):
    for (x0, y0, x1, y1) in ((16, 30, 4, 22), (16, 30, 28, 23), (16, 30, 9, 15), (16, 30, 24, 16)):
        m = leaf(c, x0, y0, x1, y1, 4, '#4a9a2a', vein=False)
        for (x, y) in np.argwhere(edge(m))[:, ::-1][::3]:
            c.erase(rect(x, y, x, y))
    brin(c, [(16, 30), (17, 11)], '#8ab84a', 2)
    capitule(c, 17, 9, 7, '#f8d81a', '#e8a808', n=16, coeur_r=2.5)


@icone('plantain')
def plantain(c):
    for (x0, y0, x1, y1) in ((16, 30, 5, 24), (16, 30, 27, 24), (16, 30, 11, 18), (16, 30, 22, 18)):
        leaf(c, x0, y0, x1, y1, 6, '#3a8a2a')
    for (x, top) in ((13, 4), (19, 6)):
        brin(c, [(16, 28), (x, top + 8)], '#6a8a3a')
        epi(c, x, top, x, top + 8, '#7a6a3a', ep=1.4)


@icone('lavande')
def lavande(c):
    for (x0, x1, top) in ((11, 8, 3), (16, 16, 2), (21, 24, 4)):
        brin(c, [(16, 31), (x1, top + 10)], '#6a8a5a')
        epi(c, x1, top, x1 + (x1 - x0) * 0.1, top + 10, '#8a5ac8', ep=1.7)
    c.shape(rect(13, 26, 19, 28), R('#c8a040'), L_const(0.6))


@icone('coquelicot')
def coquelicot(c):
    brin(c, [(16, 31), (14, 13)], '#5a8a3a', 1)
    leaf(c, 15, 26, 7, 21, 3, '#5a8a3a', vein=False)
    for (dx, dy) in ((-5, -1), (5, -1), (0, -5), (0, 3)):
        c.shape(ellipse(14 + dx, 11 + dy, 5.5, 4.5), R('#e02a1a', light=1.2), L_sphere(13 + dx, 9 + dy, 5.5, 4.5), edge=False)
    c.fill(circle(14, 11, 2.2), '#1a1010')
    c.fill(points([(13, 10), (15, 12)]), '#5a5a3a')


@icone('bourrache')
def bourrache(c):
    brin(c, [(16, 31), (16, 14)], '#6a8a4a', 2)
    for (x0, y0, x1, y1) in ((16, 28, 5, 24), (16, 26, 27, 22)):
        m = leaf(c, x0, y0, x1, y1, 6, '#5a8a4a')
        c.speckle(m, '#9ab88a', 0.2, seed=2)
    for (x, y) in ((10, 11), (20, 9), (15, 5)):
        brin(c, [(16, 15), (x, y + 2)], '#6a8a4a')
        for k in range(5):
            a = -math.pi / 2 + k * 2 * math.pi / 5
            c.shape(polygon([(x, y), (x + math.cos(a - 0.3) * 4, y + math.sin(a - 0.3) * 4), (x + math.cos(a) * 5, y + math.sin(a) * 5),
                             (x + math.cos(a + 0.3) * 4, y + math.sin(a + 0.3) * 4)]), R('#3a6ad8', light=1.2), L_const(0.65), edge=False)
        c.fill(circle(x, y, 1), '#1a1a2a')


@icone('echinacee')
def echinacee(c):
    brin(c, [(16, 31), (16, 14)], '#5a8a2a', 2)
    leaf(c, 16, 26, 6, 20, 4, '#4a8a2a')
    leaf(c, 16, 24, 26, 19, 4, '#4a8a2a')
    for k in range(9):
        a = math.pi * (0.15 + 0.7 * k / 8)
        x0, y0 = 16 + math.cos(a) * 3, 11 + math.sin(a) * 2
        x1, y1 = 16 + math.cos(a) * 11, 13 + math.sin(a) * 7
        c.shape(capsule(x0, y0, x1, y1, 1.4, 1.1), R('#c84aa0', light=1.2), L_const(0.65))
    c.shape(ellipse(16, 9, 5, 4.5), R('#a8541a', light=1.15), L_sphere(15, 7, 5, 4.5), hl=1)
    c.speckle(ellipse(16, 9, 5, 4.5), '#e8902a', 0.3, seed=5)


@icone('achillee')
def achillee(c):
    for (x, y) in ((10, 10), (17, 7), (23, 11)):
        brin(c, [(16, 31), (x, y + 2)], '#6a8a4a')
    for (x0, y0, x1, y1) in ((16, 27, 6, 20), (16, 25, 27, 19)):
        m = leaf(c, x0, y0, x1, y1, 3, '#5a8a3a', vein=False)
        c.speckle(m, '#8ab86a', 0.4, seed=7)
    for (x, y, s) in ((10, 10, 1), (17, 7, 2), (23, 11, 3)):
        ombelle(c, x, y, 4.5, '#f2ece0', n=13, seed=s)


@icone('bourse_a_pasteur')
def bourse_a_pasteur(c):
    brin(c, [(16, 31), (16, 3)], '#7a9a4a')
    for k, y in enumerate(range(6, 26, 4)):
        s = 1 if k % 2 else -1
        x = 16 + s * 4.5
        brin(c, [(16, y + 2), (x, y)], '#7a9a4a')
        c.shape(polygon([(x, y + 1), (x - 3, y - 2), (x, y - 1), (x + 3, y - 2)]), R('#8ab84a', light=1.2), L_const(0.6))
    for (x0, y0, x1, y1) in ((16, 30, 7, 27), (16, 30, 25, 27)):
        leaf(c, x0, y0, x1, y1, 3, '#5a8a3a', vein=False)


@icone('arnica')
def arnica(c):
    brin(c, [(16, 31), (13, 12)], '#6a8a3a', 1)
    brin(c, [(16, 31), (22, 15)], '#6a8a3a', 1)
    leaf(c, 16, 29, 7, 25, 4, '#5a8a3a')
    leaf(c, 16, 29, 26, 25, 4, '#5a8a3a')
    capitule(c, 12, 10, 6.5, '#f8b81a', '#d87a08', n=12)
    capitule(c, 22, 14, 5, '#f8c82a', '#d88a10', n=10)


@icone('gentiane')
def gentiane(c):
    racine(c, [(16, 30), (15, 25), (17, 21)], '#c8a060', w0=3, w1=1.5, radicelles=4)
    for (x, y) in ((11, 8), (21, 9)):
        brin(c, [(16, 21), (x, y + 5)], '#4a7a3a')
        c.shape(polygon([(x - 4, y - 4), (x + 4, y - 4), (x + 2, y + 5), (x - 2, y + 5)]), R('#2a5ae8', light=1.2), L_horiz(x - 4, x + 4))
        for k in (-3, 0, 3):
            c.fill(points([(x + k, y - 5)]), '#5a8af8')
    leaf(c, 16, 22, 6, 19, 3, '#4a7a3a', vein=False)
    leaf(c, 16, 22, 26, 19, 3, '#4a7a3a', vein=False)


@icone('sauge')
def sauge(c):
    brin(c, [(16, 31), (16, 5)], '#7a8a6a', 2)
    for (x0, y0, x1, y1, w) in ((16, 27, 5, 24, 7), (16, 27, 27, 24, 7), (16, 19, 6, 14, 6), (16, 19, 26, 14, 6), (16, 11, 11, 4, 5), (16, 11, 21, 4, 5)):
        m = leaf(c, x0, y0, x1, y1, w, '#8aa08a')
        c.speckle(m, '#b0c4ae', 0.25, seed=x1)
    epi(c, 16, 1, 16, 6, '#8a6ac8', ep=1.2)


@icone('fenouil_graines')
def fenouil_graines(c):
    sachet(c)
    g = np.random.RandomState(9)
    for _ in range(18):
        x, y = g.uniform(9, 23), g.uniform(8, 15)
        c.shape(ellipse(x, y, 1.6, 0.9), R('#8a9a4a', light=1.2), L_const(0.6), edge=False)
        c.fill(rect(int(x), int(y), int(x), int(y)), '#5a6a2a')
    for (x, y) in ((11, 22), (20, 20)):
        c.fill(line_mask(x - 4, y, x + 4, y), '#7a6a4a')


@icone('reglisse_racine')
def reglisse_racine(c):
    c.shadow_under(16, 27, 12, 2.4, 0.25)
    for k, (x0, y0, x1, y1) in enumerate(((4, 22, 27, 13), (6, 26, 28, 19), (3, 17, 22, 8))):
        racine(c, [(x0, y0), (x1, y1)], '#8a5a2a', w0=2.2, w1=1.8, radicelles=2, seed=k)
        c.fill(circle(x1, y1, 1.4), '#e8c060')
    c.shape(rect(13, 13, 15, 23), R('#c8a040'), L_const(0.6))


@icone('lin_graines')
def lin_graines(c):
    c.shadow_under(16, 26, 12, 3, 0.25)
    tas(c, 16, 21, 12, 7, '#8a5a2a', '#c88a4a', 40, seed=11)
    for (x, y) in ((8, 8), (14, 5), (21, 9)):
        brin(c, [(16, 16), (x, y + 2)], '#8aa05a')
        fleur5(c, x, y, 3.2, '#5a8ae8', '#f8f0d0')


@icone('consoude')
def consoude(c):
    brin(c, [(16, 31), (16, 9)], '#5a8a3a', 2)
    for (x0, y0, x1, y1) in ((16, 29, 3, 20), (16, 28, 29, 21), (16, 20, 6, 12), (16, 19, 26, 13)):
        m = leaf(c, x0, y0, x1, y1, 7, '#3a7a2a')
        c.speckle(m, '#6aa04a', 0.15, seed=x1)
    for k, (x, y) in enumerate(((13, 7), (16, 5), (19, 7), (15, 9))):
        c.shape(ellipse(x, y, 1.6, 2.6), R('#9a5ab8', light=1.2), L_sphere(x - 0.5, y - 1, 1.6, 2.6), edge=False)


@icone('guimauve_racine')
def guimauve_racine(c):
    c.shadow_under(16, 28, 11, 2.4, 0.25)
    racine(c, [(9, 7), (14, 14), (17, 22), (22, 29)], '#e8d8b8', w0=4.5, w1=1.6, radicelles=8, seed=12)
    racine(c, [(14, 14), (8, 21), (5, 27)], '#e8d8b8', w0=2.2, w1=1, radicelles=3, seed=13)
    c.shape(ellipse(9, 6, 3, 2), R('#c8b890'), L_const(0.6))


@icone('prele')
def prele(c):
    for (x0, x1, top) in ((12, 9, 3), (16, 16, 1), (20, 23, 4)):
        L = 30 - top
        for k in range(6):
            y0, y1 = 30 - k * L / 6, 30 - (k + 1) * L / 6
            xa, xb = x0 + (x1 - x0) * k / 6, x0 + (x1 - x0) * (k + 1) / 6
            c.shape(capsule(xa, y0, xb, y1 + 0.5, 1.3, 1.1), R('#5a9a3a', light=1.2), L_cyl(xa, y0, xb, y1, 1.4), edge=False)
            c.fill(line_mask(xb - 1.5, y1, xb + 1.5, y1), '#2a4a1a')
            for s in (-1, 1):
                c.fill(line_mask(xb, y1, xb + s * 3, y1 + 2), '#7ab84a')


@icone('melisse')
def melisse(c):
    brin(c, [(16, 31), (16, 5)], '#5a8a3a', 2)
    for (x0, y0, x1, y1, w) in ((16, 27, 5, 25, 8), (16, 27, 27, 25, 8), (16, 20, 6, 15, 7), (16, 20, 26, 15, 7), (16, 12, 10, 6, 6), (16, 12, 22, 6, 6), (16, 7, 16, 1, 5)):
        m = leaf(c, x0, y0, x1, y1, w, '#7ac84a')
        for (x, y) in np.argwhere(edge(m))[:, ::-1]:
            if (x * 3 + y) % 4 == 0:
                c.fill(rect(x, y, x, y), '#b0e080')


@icone('millepertuis')
def millepertuis(c):
    for (x, y) in ((9, 9), (16, 5), (23, 10), (13, 15), (20, 16)):
        brin(c, [(16, 31), (x, y + 2)], '#6a8a3a')
    for (x0, y0, x1, y1) in ((16, 26, 8, 23), (16, 24, 25, 21)):
        leaf(c, x0, y0, x1, y1, 3, '#5a8a3a', vein=False)
    for (x, y) in ((9, 9), (16, 5), (23, 10), (13, 15), (20, 16)):
        fleur5(c, x, y, 4, '#f8d020', '#e88a10')
        for k in range(3):
            c.fill(points([(x - 2 + k * 2, y - 2)]), '#c85a10')


@icone('ortie')
def ortie(c):
    brin(c, [(16, 31), (16, 4)], '#4a7a2a', 2)
    for (x0, y0, x1, y1, w) in ((16, 28, 4, 24, 7), (16, 28, 28, 24, 7), (16, 20, 5, 15, 6), (16, 20, 27, 15, 6), (16, 12, 9, 6, 5), (16, 12, 23, 6, 5)):
        m = leaf(c, x0, y0, x1, y1, w, '#3a7a2a')
        for (x, y) in np.argwhere(edge(m))[:, ::-1]:
            if (x + y) % 3 == 0:
                c.erase(rect(x, y, x, y))
        c.speckle(m, '#9ac87a', 0.06, seed=x1)


@icone('valeriane_racine')
def valeriane_racine(c):
    c.shadow_under(16, 28.5, 11, 2.2, 0.25)
    c.shape(ellipse(16, 12, 6, 4), R('#7a5a3a'), L_sphere(14, 10, 6, 4))
    g = np.random.RandomState(14)
    for k in range(9):
        x0 = 11 + k * 1.3
        x1 = x0 + g.uniform(-5, 5)
        racine(c, [(x0, 14), (x1, 28)], '#c8a87a', w0=1.4, w1=0.8, radicelles=1, seed=k)
    leaf(c, 16, 9, 9, 2, 3, '#5a8a3a', vein=False)
    leaf(c, 16, 9, 23, 2, 3, '#5a8a3a', vein=False)


@icone('bardane_racine')
def bardane_racine(c):
    c.shadow_under(16, 28, 13, 2.2, 0.25)
    racine(c, [(4, 6), (11, 13), (19, 21), (28, 28)], '#6a4a2a', w0=4, w1=1.4, radicelles=7, seed=15)
    c.shape(ellipse(5, 5, 3, 3), R('#e8dcc0'), L_const(0.7))
    c.fill(circle(5, 5, 1.2), '#c8b890')


@icone('sureau_fleurs')
def sureau_fleurs(c):
    for (x, y) in ((9, 12), (17, 8), (24, 13), (14, 18), (21, 19)):
        brin(c, [(16, 31), (x, y)], '#6a8a3a')
    leaf(c, 16, 27, 6, 25, 4, '#4a7a2a')
    leaf(c, 16, 27, 27, 25, 4, '#4a7a2a')
    for k, (x, y) in enumerate(((9, 12), (17, 8), (24, 13), (14, 18), (21, 19))):
        ombelle(c, x, y, 4.5, '#f6f0d8', n=12, seed=k + 20)


@icone('aubepine_baies')
def aubepine_baies(c):
    brin(c, [(4, 28), (14, 18), (26, 8)], '#5a3a22', 1)
    for (x0, y0, x1, y1) in ((14, 18, 8, 9), (20, 13, 27, 18), (9, 23, 4, 16)):
        m = leaf(c, x0, y0, x1, y1, 5, '#3a7a2a')
        for (x, y) in np.argwhere(edge(m))[:, ::-1][::4]:
            c.erase(rect(x, y, x, y))
    baies(c, [(14, 22), (18, 24), (16, 27), (21, 21), (12, 26), (22, 26)], 2.4, '#c81e1a')


@icone('angelique_racine')
def angelique_racine(c):
    c.shadow_under(16, 28, 12, 2.2, 0.25)
    racine(c, [(16, 6), (15, 14), (17, 22)], '#a8784a', w0=4.5, w1=2.5, radicelles=3, seed=16)
    for (x0, x1) in ((15, 7), (16, 11), (17, 23), (16, 27)):
        racine(c, [(16, 20), (x1, 29)], '#a8784a', w0=1.8, w1=0.8, radicelles=1, seed=x1)
    c.shape(ellipse(16, 6, 3.4, 2), R('#e8d8b0'), L_const(0.7))


@icone('ginkgo_feuilles')
def ginkgo_feuilles(c):
    for k, (x, y, a) in enumerate(((10, 11, -0.5), (21, 9, 0.4), (15, 19, 0.0))):
        brin(c, [(16, 31), (x, y + 6)], '#6a5a2a')
        m = np.zeros((32, 32), bool)
        for t in np.linspace(-0.75, 0.75, 15):
            ang = a + t - math.pi / 2
            m |= line_mask(x, y + 6, x + math.cos(ang) * 8, y + 6 + math.sin(ang) * 8, 2)
        m &= circle(x, y + 6, 8)
        c.shape(m, R('#9ac83a' if k != 1 else '#d8c030', light=1.2), L_sphere(x - 2, y, 8, 8, 0.3))
        c.fill(line_mask(x + math.cos(a - math.pi / 2) * 2, y + 6 + math.sin(a - math.pi / 2) * 2, x + math.cos(a - math.pi / 2) * 7,
                         y + 6 + math.sin(a - math.pi / 2) * 7) & m, '#6a9a2a')


@icone('ecorce_de_bouleau')
def ecorce_de_bouleau(c):
    c.shadow_under(16, 27, 12, 2.4, 0.25)
    for k, (x0, y0, x1, y1) in enumerate(((4, 20, 26, 12), (6, 26, 28, 18))):
        m = capsule(x0, y0, x1, y1, 4)
        c.shape(m, R('#ecece4', light=1.05), L_cyl(x0, y0, x1, y1, 4))
        g = np.random.RandomState(30 + k)
        for _ in range(7):
            t = g.uniform(0.1, 0.9)
            x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
            c.fill(line_mask(x - 1.5, y - 1, x + 1.5, y + 0.2) & m, '#2a2a2a')
        c.shape(ellipse(x1, y1, 2, 4) & m, R('#c8a46a'), L_const(0.6), edge=False)


@icone('ecorce_de_saule')
def ecorce_de_saule(c):
    c.shadow_under(16, 27, 12, 2.4, 0.25)
    for k, (x0, y0, x1, y1) in enumerate(((3, 22, 27, 10), (6, 27, 29, 17))):
        m = capsule(x0, y0, x1, y1, 3.6)
        c.shape(m, R('#6a5a42', light=1.15), L_cyl(x0, y0, x1, y1, 3.6))
        for t in np.linspace(0.1, 0.9, 8):
            x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
            c.fill(line_mask(x - 1, y - 2.5, x + 0.6, y + 2.5) & m & ~edge(m), '#3a3226')
        c.shape(ellipse(x1, y1, 1.8, 3.6) & m, R('#d8b878'), L_const(0.6), edge=False)


@icone('ginseng_racine')
def ginseng_racine(c):
    c.shadow_under(16, 29, 11, 2.2, 0.25)
    racine(c, [(16, 5), (16, 11), (15, 17)], '#e8c890', w0=3, w1=4.5, radicelles=2, seed=17)
    racine(c, [(15, 17), (10, 23), (7, 29)], '#e8c890', w0=4, w1=1.2, radicelles=3, seed=18)
    racine(c, [(15, 17), (20, 23), (24, 29)], '#e8c890', w0=4, w1=1.2, radicelles=3, seed=19)
    for (x, y) in ((16, 3), (13, 4), (19, 4)):
        c.shape(circle(x, y, 1.6), R('#d8241c', light=1.3), L_sphere(x - 0.4, y - 0.4, 1.6, 1.6), edge=False)


@icone('aloe')
def aloe(c):
    c.shadow_under(16, 29, 10, 2, 0.3)
    for (x1, y1, w) in ((5, 8, 4.5), (27, 9, 4.5), (11, 3, 4), (21, 3, 4), (16, 2, 3.5)):
        m = leaf(c, 16, 29, x1, y1, w, '#5a9a6a', vein=False)
        for (x, y) in np.argwhere(edge(m))[:, ::-1][::3]:
            c.fill(rect(x, y, x, y), '#c8d8a0')
        c.speckle(m, '#9ac8a8', 0.12, seed=x1)


@icone('bambou')
def bambou(c):
    for (x, top, w) in ((11, 3, 2.6), (19, 6, 2.4)):
        for k in range(4):
            y0, y1 = 31 - k * (31 - top) / 4, 31 - (k + 1) * (31 - top) / 4
            c.shape(rect(int(x - w), int(y1) + 1, int(x + w), int(y0)), R('#7ab83a', light=1.15), L_horiz(x - w, x + w))
            c.shape(rect(int(x - w - 0.5), int(y1), int(x + w + 0.5), int(y1) + 1), R('#5a8a2a'), L_const(0.5))
    for (x0, y0, x1, y1) in ((13, 12, 25, 6), (9, 18, 2, 12), (21, 20, 29, 18)):
        leaf(c, x0, y0, x1, y1, 3.6, '#4a9a2a', vein=False)


@icone('mousse_des_grottes')
def mousse_des_grottes(c):
    c.shadow_under(16, 27, 12, 2.6, 0.3)
    c.shape(ellipse(16, 25, 12, 4) & (YY >= 22), R('#6a6e74'), L_vert(21, 29, 0.8, 0.3))
    g = np.random.RandomState(40)
    for _ in range(26):
        x, y = g.uniform(6, 26), g.uniform(13, 24)
        r = g.uniform(1.8, 3.2)
        c.shape(circle(x, y, r), R(['#3a7a3a', '#4a8a3a', '#2e6a3a'][g.randint(3)], light=1.25), L_sphere(x - 0.6, y - 0.6, r, r), edge=False)
    for _ in range(6):
        x, y = g.uniform(8, 24), g.uniform(12, 22)
        c.fill(rect(int(x), int(y), int(x), int(y)), '#9ad8f0')


# ============================================================================ 20 champignons
@icone('pleurote')
def pleurote(c):
    c.shadow_under(16, 28, 12, 2.4, 0.25)
    for (x, y, rx, ry, col_) in ((11, 20, 9, 5, '#a8a49a'), (20, 15, 9, 5, '#bab6aa'), (14, 10, 7, 4, '#c8c4b8')):
        c.shape(ellipse(x, y, rx, ry), R(col_, light=1.15), L_sphere(x - 3, y - 2, rx, ry, 0.3), hl=1)
        for k in range(5):
            c.fill(line_mask(x - rx * 0.1, y + ry * 0.2, x - rx * 0.8 + k * 3, y + ry * 0.7) & ellipse(x, y, rx, ry), mix(col_, '#5a5040', 0.3))
    c.shape(rect(22, 22, 25, 29), R('#e8e0d0'), L_horiz(22, 25))


@icone('oreille_de_judas')
def oreille_de_judas(c):
    c.shadow_under(16, 28, 12, 2.4, 0.3)
    for (x, y, rx, ry) in ((11, 18, 8, 7), (21, 14, 7, 7)):
        m = ellipse(x, y, rx, ry)
        c.shape(m, R('#6a3a2a', light=1.25), L_sphere(x - 2, y - 2, rx, ry, 0.2), hl=2)
        c.fill(arc_band(x + 1, y + 1, rx * 0.6, ry * 0.55, 1, 200, 340) & m, '#3a1a12')
        c.fill(arc_band(x - 1, y - 1, rx * 0.45, ry * 0.4, 1, 30, 150) & m, '#9a6a52')


@icone('coprin')
def coprin(c):
    c.shadow_under(16, 29, 9, 2, 0.25)
    c.shape(rect(14, 18, 18, 29), R('#f4f0e6'), L_horiz(14, 18))
    cap = ellipse(16, 12, 6, 11) & (YY <= 20)
    c.shape(cap, R('#ece6da', light=1.05), L_sphere(13, 6, 6, 11, 0.3), hl=1)
    g = np.random.RandomState(41)
    for _ in range(14):
        x, y = g.uniform(11, 21), g.uniform(4, 18)
        c.fill(line_mask(x, y, x + 1, y + 1.5) & cap, '#a89880')
    c.fill(rect(10, 18, 22, 20) & cap, '#2a2a2a')


@icone('vesse_de_loup')
def vesse_de_loup(c):
    c.shadow_under(16, 28, 10, 2.4, 0.3)
    for (x, y, r) in ((13, 20, 7.5), (22, 22, 5)):
        c.shape(ellipse(x, y, r, r * 0.92), R('#f0eadc', light=1.05), L_sphere(x - r * 0.4, y - r * 0.4, r, r), hl=2)
        c.speckle(ellipse(x, y, r, r), '#d0c4a8', 0.25, seed=int(x))
        c.fill(rect(int(x) - 1, int(y - r * 0.9), int(x), int(y - r * 0.9)), '#b8a888')


@icone('girolle')
def girolle(c):
    c.shadow_under(16, 29, 11, 2.2, 0.25)
    champi(c, 11, 28, 9, 6, 2.6, '#f0a01a', pied='#f0b030', style='entonnoir')
    champi(c, 21, 29, 7, 5, 2.2, '#f8b02a', pied='#f8c040', style='entonnoir')


@icone('pied_bleu')
def pied_bleu(c):
    c.shadow_under(16, 29, 11, 2.2, 0.25)
    champi(c, 12, 28, 10, 8, 5, '#9a7ab8', pied='#a88ac8', lames='#6a4a88')
    champi(c, 23, 29, 6, 5, 3.5, '#8a6aa8', pied='#a88ac8')


@icone('koji')
def koji(c):
    c.shadow_under(16, 27, 12, 3, 0.25)
    tas(c, 16, 21, 12, 7, '#f0ece0')
    g = np.random.RandomState(42)
    for _ in range(30):
        x, y = g.uniform(6, 26), g.uniform(15, 24)
        c.fill(rect(int(x), int(y), int(x) + 1, int(y)), '#fffef8')
    for _ in range(26):
        x, y = g.uniform(6, 26), g.uniform(13, 23)
        c.fill(rect(int(x), int(y), int(x), int(y)), ['#b8d860', '#e8f0c0', '#c8e080'][g.randint(3)])


@icone('shiitake')
def shiitake(c):
    c.shadow_under(16, 29, 11, 2.2, 0.25)
    champi(c, 13, 28, 8, 10, 6, '#7a4a2a', pied='#e8dcc8', lames='#d8c8a8')
    for (dx, dy) in ((-4, -2), (2, -4), (5, -1), (-1, 0)):
        c.fill(line_mask(13 + dx, 20 + dy, 14.5 + dx, 21 + dy), '#e8d0b0')
    champi(c, 24, 29, 5, 5, 3.4, '#8a5a32', pied='#e8dcc8')


@icone('maitake')
def maitake(c):
    c.shadow_under(16, 29, 12, 2.4, 0.25)
    g = np.random.RandomState(43)
    for k in range(13):
        x, y = g.uniform(7, 25), 27 - k * 1.5
        a = g.uniform(-0.8, 0.8)
        m = ellipse(x, y, 4.2, 2.2)
        c.shape(m, R('#8a7a62', light=1.25), L_sphere(x - 1.5, y - 1, 4.2, 2.2, 0.3), hl=1)
        c.fill(arc_band(x, y + 0.5, 3.5, 1.6, 1, 200, 340) & m, '#5a4a38')


@icone('polypore')
def polypore(c):
    c.shadow_under(16, 28, 12, 2.4, 0.25)
    m = polygon([(5, 15), (27, 13), (25, 22), (8, 24)])
    c.shape(m, R('#b8a684', light=1.15), L_sphere(10, 12, 16, 10, 0.3), hl=1)
    for k in range(4):
        c.fill(line_mask(6, 16 + k * 2, 26, 14 + k * 2) & m, ['#8a7a5a', '#d8c8a0'][k % 2])
    c.shape(polygon([(8, 24), (25, 22), (24, 25), (9, 26)]), R('#efe6d0'), L_const(0.7))


@icone('morille')
def morille(c):
    c.shadow_under(16, 29, 9, 2, 0.25)
    c.shape(rrect(13, 19, 19, 29, 2), R('#f0e6cc'), L_horiz(13, 19))
    cap = ellipse(16, 12, 7, 9.5)
    c.shape(cap, R('#8a6a4a', light=1.2), L_sphere(13, 7, 7, 9.5, 0.3))
    for y in range(4, 22, 3):
        for x in range(9 + (y // 3) % 2 * 2, 24, 4):
            c.fill(ellipse(x, y, 1.3, 1) & cap & ~edge(cap), '#3a2618')


@icone('trompette_des_morts')
def trompette_des_morts(c):
    c.shadow_under(16, 29, 11, 2.2, 0.3)
    for (x, base, h, rx) in ((12, 28, 13, 6), (21, 29, 10, 5)):
        m = polygon([(x - rx, base - h), (x + rx, base - h), (x + 1.5, base), (x - 1.5, base)])
        c.shape(m, R('#3a2e2a', light=1.35), L_horiz(x - rx, x + rx, 0.8, 0.25))
        c.shape(ellipse(x, base - h, rx, 2), R('#141010'), L_const(0.4))
        c.fill(line_mask(x - rx + 1, base - h + 1, x - rx + 3, base - h + 3), '#6a5a52')


@icone('amanite_rouge')
def amanite_rouge(c):
    c.shadow_under(16, 29, 11, 2.2, 0.25)
    champi(c, 13, 29, 10, 9, 6, '#d8201a', pied='#f4f0e6', lames='#efe8d8',
           taches=[(-4, -3, 1.2), (1, -5, 1.3), (5, -2, 1.0), (-1, -1, 0.9), (3, -6, 0.8)])
    c.fill(rect(10, 22, 16, 23), '#efe8d8')
    champi(c, 24, 29, 5, 4.5, 3, '#e83a1a', pied='#f4f0e6', taches=[(-1, -2, 0.8), (1.5, -1, 0.7)])


@icone('criniere_de_lion')
def criniere_de_lion(c):
    c.shadow_under(16, 28, 11, 2.4, 0.25)
    body = ellipse(16, 13, 10, 8)
    c.shape(body, R('#f0ead8'), L_sphere(13, 9, 10, 8, 0.3))
    g = np.random.RandomState(44)
    for k in range(26):
        x = g.uniform(7, 25)
        y0 = g.uniform(10, 18)
        L = g.uniform(5, 10)
        c.fill(line_mask(x, y0, x + g.uniform(-0.5, 0.5), y0 + L), ['#f8f4ea', '#e0d6c0', '#fffef8'][k % 3])


@icone('reishi')
def reishi(c):
    c.shadow_under(16, 28, 12, 2.4, 0.25)
    m = ellipse(16, 15, 12, 8) & ~ellipse(16, 23, 4, 4)
    c.shape(m, R('#8a1e12', light=1.3), L_sphere(11, 10, 12, 8, 0.2), hl=3)
    for k in range(3):
        c.fill(arc_band(16, 18, 4 + k * 3, 3 + k * 2, 1, 190, 350) & m, ['#c86a1a', '#6a120a', '#e8a040'][k])
    c.shape(rect(15, 21, 17, 28), R('#5a1a10'), L_horiz(15, 17))


@icone('chaga')
def chaga(c):
    c.shadow_under(16, 28, 12, 2.6, 0.3)
    m = polygon([(5, 20), (8, 10), (15, 6), (24, 8), (28, 17), (25, 26), (12, 27)])
    c.shape(m, R('#2a2018', light=1.6), L_sphere(10, 8, 14, 12, 0.2))
    g = np.random.RandomState(45)
    for _ in range(18):
        x, y = g.uniform(7, 26), g.uniform(8, 25)
        c.fill(line_mask(x, y, x + g.uniform(-2, 2), y + g.uniform(1, 3)) & m & ~edge(m), '#120c08')
    c.shape(polygon([(18, 18), (25, 20), (24, 25), (17, 23)]), R('#c8742a', light=1.2), L_const(0.6))


@icone('cordyceps')
def cordyceps(c):
    c.shadow_under(16, 29, 11, 2.2, 0.25)
    # chenille desséchée
    c.shape(capsule(5, 26, 25, 27, 3), R('#a8885a'), L_cyl(5, 26, 25, 27, 3))
    for x in range(7, 25, 3):
        c.fill(line_mask(x, 24, x, 29), '#7a5a3a')
    for (x0, x1, top) in ((12, 10, 5), (18, 20, 8)):
        c.shape(capsule(x0, 25, x1, top + 5, 1.2, 1.6), R('#e8781a', light=1.2), L_cyl(x0, 25, x1, top, 1.6))
        c.shape(ellipse(x1, top + 3, 2.4, 4), R('#f8902a', light=1.25), L_sphere(x1 - 1, top + 1, 2.4, 4), hl=1)


@icone('truffe_noire')
def truffe_noire(c):
    c.shadow_under(16, 27, 11, 2.6, 0.35)
    m = ellipse(14, 18, 9.5, 8.5) | ellipse(22, 21, 5, 4.5)
    c.shape(m, R('#2a2220', light=1.6), L_sphere(10, 13, 11, 10, 0.2), hl=1)
    for (x, y) in rng_pts(46, m & ~edge(m), 24):
        c.fill(rect(x, y, x, y), '#4a3c36')
    c.shape(ellipse(22, 21, 2.6, 2.2), R('#e8dcc8'), L_const(0.7))
    for k in range(3):
        c.fill(line_mask(20.5 + k, 20, 22 + k, 22.5), '#5a4030')


@icone('champignon_luminescent')
def champignon_luminescent(c):
    c.fill(circle(16, 15, 13) & (np.random.RandomState(47).rand(32, 32) < 0.04), '#8af0ff')
    champi(c, 12, 28, 10, 8, 5, '#3ad8e8', pied='#b8f0f0', lames='#1a8aa0', hl=3)
    champi(c, 23, 29, 6, 5, 3.4, '#6ae8f0', pied='#c8f8f8', hl=2)
    for (x, y) in ((9, 16), (14, 15), (22, 21)):
        c.fill(rect(x, y, x, y), '#ffffff')


@icone('bolet_cendre')
def bolet_cendre(c):
    c.shadow_under(16, 29, 11, 2.2, 0.25)
    c.shape(rrect(10, 17, 18, 29, 3), R('#d8d0c4'), L_horiz(10, 18))
    for y in range(19, 28, 2):
        c.fill(line_mask(11, y, 17, y - 1), '#a8a094')
    cap = ellipse(14, 15, 10, 7) & (YY <= 17.5)
    c.shape(cap, R('#7a7a7a', light=1.35), L_sphere(10, 10, 10, 7, 0.3), hl=2)
    c.speckle(cap, '#a8a8a8', 0.12, seed=48)
    champi(c, 25, 29, 5, 4, 3, '#6a6a6a', pied='#d8d0c4')


# ============================================================================ 15 ressources rares
@icone('fleur_de_lune_pale')
def fleur_de_lune_pale(c):
    c.fill(circle(16, 12, 11) & (np.random.RandomState(50).rand(32, 32) < 0.05), '#e8f4ff')
    brin(c, [(16, 31), (16, 15)], '#4a6a7a', 1)
    leaf(c, 16, 27, 7, 23, 3.5, '#4a7a8a', vein=False)
    leaf(c, 16, 25, 25, 21, 3.5, '#4a7a8a', vein=False)
    for k in range(6):
        a = -math.pi / 2 + k * math.pi / 3
        x, y = 16 + math.cos(a) * 5, 11 + math.sin(a) * 5
        c.shape(ellipse(x, y, 3.4, 3.4), R('#d8e8f8', light=1.1), L_sphere(x - 1, y - 1, 3.4, 3.4), edge=False)
    c.shape(circle(16, 11, 2.4), R('#a8c8f0', light=1.3), L_sphere(15, 10, 2.4, 2.4), hl=1)


@icone('algue_noire')
def algue_noire(c):
    c.shadow_under(16, 29, 10, 2, 0.25)
    for k, (x0, x1) in enumerate(((10, 6), (14, 15), (18, 23), (22, 26))):
        pts = [(x0 + math.sin(t * 3 + k) * 2, 29 - t * 24) for t in np.linspace(0, 1, 7)]
        for i in range(len(pts) - 1):
            (a, b), (e, f) = pts[i], pts[i + 1]
            c.shape(capsule(a, b, e, f, 1.8, 1.4), R('#1e2a26', light=1.8), L_cyl(a, b, e, f, 1.8), edge=False)
    c.speckle(rect(0, 0, 31, 31), '#3a5a4a', 0.04, seed=51)


@icone('baie_de_givre')
def baie_de_givre(c):
    brin(c, [(16, 31), (16, 16), (10, 9)], '#5a6a7a')
    brin(c, [(16, 18), (22, 10)], '#5a6a7a')
    leaf(c, 16, 22, 26, 19, 3.6, '#5a8aa0', vein=False)
    baies(c, [(9, 9), (12, 6), (8, 13), (21, 9), (24, 12), (22, 6)], 2.6, '#4aa8e8', hl=2)
    for (x, y) in ((8, 7), (11, 4), (20, 7), (23, 4), (7, 11)):
        c.fill(rect(x, y, x, y), '#ffffff')


@icone('pollen_dore')
def pollen_dore(c):
    c.shadow_under(16, 27, 11, 2.4, 0.25)
    # feuille repliée en coupelle, poudre dorée qui scintille
    c.shape(polygon([(4, 18), (28, 16), (24, 26), (8, 27)]), R('#5a8a3a'), L_horiz(4, 28, 0.85, 0.35))
    tas(c, 16, 20, 9, 5, '#f0c020')
    g = np.random.RandomState(52)
    for _ in range(16):
        x, y = g.uniform(5, 27), g.uniform(4, 18)
        c.fill(rect(int(x), int(y), int(x), int(y)), ['#fff4a0', '#f8d040', '#ffffff'][g.randint(3)])


@icone('mousse_de_numere')
def mousse_de_numere(c):
    c.shadow_under(16, 27, 12, 2.6, 0.3)
    g = np.random.RandomState(53)
    for _ in range(30):
        x, y = g.uniform(6, 26), g.uniform(12, 25)
        r = g.uniform(1.8, 3)
        c.shape(circle(x, y, r), R(['#5a7a4a', '#6a5a8a', '#4a6a5a'][g.randint(3)], light=1.3), L_sphere(x - 0.6, y - 0.6, r, r), edge=False)
    for _ in range(9):
        x, y = g.uniform(8, 24), g.uniform(8, 18)
        c.fill(line_mask(x, y + 3, x, y), '#8a7a5a')
        c.fill(rect(int(x), int(y), int(x), int(y)), '#c84a8a')


@icone('baie_de_l_ile_de_la_baleine')
def baie_baleine(c):
    brin(c, [(16, 5), (16, 12)], '#4a6a3a')
    leaf(c, 16, 6, 7, 3, 3.4, '#3a8a5a', vein=False)
    leaf(c, 16, 6, 25, 3, 3.4, '#3a8a5a', vein=False)
    baies(c, [(12, 15), (20, 15), (16, 20), (10, 22), (22, 22), (16, 26)], 3.6, '#4a3ab8', hl=2)
    for (x, y) in ((11, 14), (19, 14), (15, 19)):
        c.fill(rect(x, y, x, y), '#c8c0ff')


@icone('feuille_de_kukuroo')
def feuille_de_kukuroo(c):
    brin(c, [(6, 30), (26, 4)], '#2a5a4a', 1)
    m = leaf(c, 6, 30, 27, 3, 14, '#1aa89a')
    for t in np.linspace(0.15, 0.85, 6):
        x, y = 6 + 21 * t, 30 - 27 * t
        for s in (-1, 1):
            c.fill(line_mask(x, y, x + s * 5, y - 2 + s * 3) & m & ~edge(m), '#0e7a6e')
    c.speckle(m, '#7af0d8', 0.04, seed=54)


@icone('oeuf_d_aigle_araignee')
def oeuf_aigle(c):
    c.shadow_under(16, 28.5, 9, 2.4, 0.35)
    m = ellipse(16, 17, 9, 11.5)
    c.shape(m, R('#c8c0b0', light=1.15), L_sphere(13, 11, 9, 11.5), hl=2)
    # motif en toile d'araignée, sombre
    for a in range(0, 360, 45):
        c.fill(line_mask(16, 16, 16 + math.cos(math.radians(a)) * 9, 16 + math.sin(math.radians(a)) * 11) & m & ~edge(m), '#5a4a5a')
    for r in (3.5, 6.5):
        c.fill(arc_band(16, 16, r, r * 1.25, 1, 0, 360) & m & ~edge(m), '#5a4a5a')
    for (x, y) in rng_pts(55, m & ~edge(m), 8):
        c.fill(rect(x, y, x, y), '#3a2a3a')


@icone('chardon_de_meteor')
def chardon_de_meteor(c):
    brin(c, [(16, 31), (16, 14)], '#5a6a6a', 2)
    for (x0, y0, x1, y1) in ((16, 27, 5, 22), (16, 25, 27, 21)):
        m = leaf(c, x0, y0, x1, y1, 4, '#6a8a8a', vein=False)
        for (x, y) in np.argwhere(edge(m))[:, ::-1][::2]:
            c.fill(rect(x, y, x, y), '#d8e8f0')
    c.shape(ellipse(16, 14, 5, 4), R('#5a7a6a'), L_sphere(15, 12, 5, 4))
    for k in range(13):
        a = math.pi * (1.1 + 0.8 * k / 12)
        c.fill(line_mask(16, 10, 16 + math.cos(a) * 8, 10 + math.sin(a) * 8, 1), ['#b84ad8', '#e8a0ff', '#8a5ab8'][k % 3])
    c.fill(rect(15, 4, 17, 4) | rect(9, 7, 9, 7) | rect(23, 6, 23, 6), '#ffffff')


@icone('lys_des_nuees')
def lys_des_nuees(c):
    brin(c, [(16, 31), (15, 16)], '#4a7a5a', 1)
    leaf(c, 16, 28, 6, 20, 3, '#4a8a5a', vein=False)
    leaf(c, 16, 27, 26, 21, 3, '#4a8a5a', vein=False)
    for k in range(6):
        a = -math.pi / 2 + (k - 2.5) * 0.45
        x1, y1 = 15 + math.cos(a) * 11, 14 + math.sin(a) * 11
        c.shape(capsule(15, 15, x1, y1, 1.2, 2.6), R('#f4f6fa' if k % 2 else '#dce8f8', light=1.05), L_const(0.75))
    for k in range(4):
        c.fill(line_mask(15, 14, 13 + k * 1.5, 6), '#6a8ad8')
    c.fill(circle(15, 14, 1.2), '#f0c840')


@icone('orchidee_de_kakin')
def orchidee_de_kakin(c):
    brin(c, [(14, 31), (18, 6)], '#3a6a2a', 1)
    leaf(c, 14, 30, 4, 25, 5, '#2e7a3a')
    leaf(c, 14, 30, 26, 27, 5, '#2e7a3a')
    for (x, y) in ((12, 11), (21, 16)):
        for (dx, dy, rx, ry) in ((-4, -2, 3.4, 2.4), (4, -2, 3.4, 2.4), (0, -5, 2.4, 3.2)):
            c.shape(ellipse(x + dx, y + dy, rx, ry), R('#e83aa8', light=1.2), L_sphere(x + dx - 1, y + dy - 1, rx, ry), edge=False)
        c.shape(ellipse(x, y + 2, 3, 2.6), R('#a8126a', light=1.2), L_const(0.6))
        c.fill(rect(x - 1, y, x + 1, y), '#f8e040')


@icone('racine_de_fer_sang')
def racine_fer_sang(c):
    c.shadow_under(16, 28, 12, 2.4, 0.3)
    racine(c, [(5, 6), (11, 13), (16, 21), (25, 28)], '#6a1e1a', w0=4.2, w1=1.4, radicelles=7, seed=56)
    racine(c, [(11, 13), (20, 9), (27, 10)], '#6a1e1a', w0=2.2, w1=1, radicelles=3, seed=57)
    for (x, y) in ((9, 11), (14, 17), (19, 23)):
        c.fill(rect(x, y, x + 1, y), '#c83a2a')
        c.fill(rect(x, y + 1, x, y + 1), '#8a9098')


@icone('mousse_des_abysses')
def mousse_des_abysses(c):
    c.fill(circle(16, 18, 13) & (np.random.RandomState(58).rand(32, 32) < 0.05), '#6af0ff')
    c.shadow_under(16, 27, 12, 2.6, 0.3)
    c.shape(ellipse(16, 25, 12, 4) & (YY >= 22), R('#2a2e3a'), L_vert(21, 29, 0.8, 0.3))
    g = np.random.RandomState(59)
    for _ in range(26):
        x, y = g.uniform(6, 26), g.uniform(13, 24)
        r = g.uniform(1.8, 3.2)
        c.shape(circle(x, y, r), R(['#1aa8b8', '#2ac8c8', '#0e7a9a'][g.randint(3)], light=1.35), L_sphere(x - 0.6, y - 0.6, r, r), edge=False)
    for _ in range(8):
        x, y = g.uniform(8, 24), g.uniform(12, 22)
        c.fill(rect(int(x), int(y), int(x), int(y)), '#e8ffff')


@icone('lotus_de_l_aube')
def lotus_de_l_aube(c):
    c.shadow_under(16, 27, 12, 2.4, 0.25)
    c.shape(ellipse(16, 24, 13, 4.5), R('#3a8a4a'), L_vert(20, 28, 0.85, 0.35))
    for k in range(7):
        a = math.pi * (1.05 + 0.9 * k / 6)
        x1, y1 = 16 + math.cos(a) * 11, 19 + math.sin(a) * 11
        c.shape(capsule(16, 20, x1, y1, 1.8, 3), R('#f8a8c8' if k % 2 else '#fcc8d8', light=1.1), L_const(0.75))
    for k in range(4):
        a = math.pi * (1.2 + 0.6 * k / 3)
        c.shape(capsule(16, 19, 16 + math.cos(a) * 7, 19 + math.sin(a) * 7, 1.5, 2.4), R('#ffe0a0', light=1.1), L_const(0.8))
    c.fill(circle(16, 16, 1.5), '#f8c030')


@icone('seve_de_l_arbre_monde')
def seve_arbre_monde(c):
    fiole_ingredient(c, '#e89a1a', verre='#e8f0e8', bouchon='#6a4a2a')
    for (x, y) in ((14, 20), (18, 23), (15, 25)):
        c.fill(rect(x, y, x, y), '#fff4b0')


# ============================================================================ 15 ressources animales et de ruche
@icone('plume')
def plume(c):
    c.fill(line_mask(6, 28, 26, 4, 1), '#c8b8a0')
    for t in np.linspace(0.12, 0.95, 18):
        x, y = 6 + 20 * t, 28 - 24 * t
        L = 6 * math.sin(t * math.pi) + 1.5
        c.fill(line_mask(x, y, x - L * 0.8, y - L * 0.3), '#f4f2ec')
        c.fill(line_mask(x, y, x + L * 0.3, y + L * 0.8), '#e4e0d6')
    c.fill(line_mask(6, 28, 26, 4), '#a89880')


@icone('duvet')
def duvet(c):
    c.shadow_under(16, 27, 10, 2.4, 0.2)
    g = np.random.RandomState(60)
    for _ in range(40):
        a = g.uniform(0, 2 * math.pi)
        r = g.uniform(3, 10)
        x0, y0 = 16, 17
        c.fill(line_mask(x0 + math.cos(a) * r * 0.3, y0 + math.sin(a) * r * 0.3, x0 + math.cos(a) * r, y0 + math.sin(a) * r * 0.8),
               ['#fbf8f0', '#ece6d8', '#ffffff'][g.randint(3)])
    c.shape(circle(16, 17, 3), R('#f0ece0'), L_sphere(15, 16, 3, 3), edge=False)


@icone('laine')
def laine(c):
    c.shadow_under(16, 27, 12, 2.6, 0.3)
    g = np.random.RandomState(61)
    for _ in range(20):
        x, y = g.uniform(6, 26), g.uniform(10, 24)
        r = g.uniform(3, 4.6)
        c.shape(circle(x, y, r), R('#f0e8d4', light=1.05), L_sphere(x - 1, y - 1, r, r), edge=False)
    for _ in range(14):
        x, y = g.uniform(7, 25), g.uniform(11, 23)
        c.fill(arc_band(x, y, 1.8, 1.4, 1, 180, 360), '#d8ccb0')


@icone('saindoux')
def saindoux(c):
    c.shadow_under(16, 27, 12, 2.4, 0.3)
    c.shape(polygon([(4, 18), (26, 15), (29, 26), (6, 28)]), R('#e8d8b0', light=1.1), L_horiz(4, 29, 0.85, 0.35))
    c.shape(rrect(8, 11, 24, 23, 2), R('#f8f4ea'), L_sphere(12, 12, 14, 10, 0.4), hl=2)
    c.fill(line_mask(10, 13, 20, 12), '#ffffff')


@icone('os')
def os_(c):
    c.shadow_under(16, 26, 12, 2.4, 0.25)
    c.shape(capsule(8, 22, 24, 10, 2.4), R('#f0e8d4'), L_cyl(8, 22, 24, 10, 2.6))
    for (x, y) in ((6, 21), (9, 24.5), (22, 7.5), (25.5, 10.5)):
        c.shape(circle(x, y, 3), R('#f0e8d4'), L_sphere(x - 1, y - 1, 3, 3), hl=1)


@icone('coquille_d_oeuf')
def coquille_d_oeuf(c):
    c.shadow_under(16, 27, 12, 2.4, 0.25)
    for (x, y, flip) in ((11, 19, False), (22, 20, True)):
        m = ellipse(x, y, 7, 8) & ((YY >= y - 1) if not flip else (YY >= y - 2))
        zig = np.zeros((32, 32), bool)
        for k in range(-7, 8, 2):
            zig |= rect(int(x + k), int(y - 2 + (k % 4 == 1)), int(x + k + 1), int(y - 1))
        m &= ~zig
        c.shape(m, R('#e8c49a', light=1.15), L_sphere(x - 2, y - 2, 7, 8))
        c.shape(ellipse(x, y + 0.5, 5.2, 3) & m, R('#f8f0e2'), L_const(0.8), edge=False)


@icone('ecailles')
def ecailles(c):
    c.shadow_under(16, 27, 12, 2.4, 0.25)
    g = np.random.RandomState(62)
    for _ in range(14):
        x, y = g.uniform(7, 25), g.uniform(10, 24)
        m = ellipse(x, y, 3.4, 2.8)
        c.shape(m, R(['#c8d8e0', '#a8c0d0', '#e0ecf0'][g.randint(3)], light=1.2), L_sphere(x - 1, y - 1, 3.4, 2.8), hl=1)
        c.fill(arc_band(x, y + 0.5, 2.4, 1.8, 1, 200, 340) & m, '#7a90a0')


@icone('peau_de_boeuf')
def peau_de_boeuf(c):
    c.shadow_under(16, 27, 13, 2.6, 0.3)
    m = polygon([(4, 8), (11, 5), (16, 8), (22, 5), (28, 9), (26, 17), (29, 24), (21, 27), (16, 24), (10, 28), (3, 23), (6, 16)])
    c.shape(m, R('#7a4a2a', light=1.2), L_sphere(10, 8, 18, 16, 0.4))
    c.speckle(m, '#5a3420', 0.15, seed=63)
    c.shape(ellipse(13, 15, 4, 3) | ellipse(20, 19, 3, 2.4), R('#f0e6d4'), L_const(0.75), edge=False)


@icone('peau_de_cerf')
def peau_de_cerf(c):
    c.shadow_under(16, 27, 13, 2.6, 0.3)
    m = polygon([(5, 9), (12, 5), (16, 8), (21, 5), (27, 9), (25, 17), (28, 24), (20, 27), (16, 24), (11, 27), (4, 23), (7, 16)])
    c.shape(m, R('#b8783a', light=1.2), L_sphere(10, 8, 18, 16, 0.4))
    for (x, y) in rng_pts(64, m & ~edge(m), 12):
        c.fill(rect(x, y, x, y), '#f4e8c8')
    c.fill(line_mask(16, 9, 16, 24) & m, '#8a5420')


@icone('bois_de_cerf')
def bois_de_cerf(c):
    pts = [(8, 29), (10, 22), (13, 15), (17, 9), (22, 4)]
    for i in range(len(pts) - 1):
        (a, b), (e, f) = pts[i], pts[i + 1]
        c.shape(capsule(a, b, e, f, 2.2 - i * 0.3, 1.9 - i * 0.3), R('#d8c8a0', light=1.1), L_cyl(a, b, e, f, 2.2))
    for (x0, y0, x1, y1) in ((10, 22, 4, 15), (13, 15, 21, 15), (17, 9, 26, 10)):
        c.shape(capsule(x0, y0, x1, y1, 1.6, 0.9), R('#e0d4b0', light=1.1), L_cyl(x0, y0, x1, y1, 1.6))
    c.shape(ellipse(8, 29, 3, 1.6), R('#a8946a'), L_const(0.6))


@icone('defense_de_sanglier')
def defense_de_sanglier(c):
    c.shadow_under(16, 27, 11, 2.2, 0.25)
    pts = [(7, 27), (9, 19), (13, 12), (19, 8), (25, 8)]
    for i in range(len(pts) - 1):
        (a, b), (e, f) = pts[i], pts[i + 1]
        w0, w1 = 3.2 - i * 0.7, 3.2 - (i + 1) * 0.7
        c.shape(capsule(a, b, e, f, w0, max(0.6, w1)), R('#f4ecd8', light=1.05), L_cyl(a, b, e, f, 3))
    c.shape(ellipse(7, 27, 3, 1.8), R('#c8a878'), L_const(0.6))
    c.fill(line_mask(9, 22, 16, 11), '#d8ccb0')


@icone('carapace_de_crabe')
def carapace_de_crabe(c):
    c.shadow_under(16, 26, 12, 2.4, 0.3)
    m = ellipse(16, 17, 12, 8.5) & ~(YY > 23)
    c.shape(m, R('#d84a1a', light=1.2), L_sphere(12, 12, 12, 8.5, 0.3), hl=2)
    for x in (9, 23):
        c.fill(circle(x, 14, 1.4) & m, '#8a1e0a')
    c.fill(arc_band(16, 21, 7, 4, 1, 200, 340) & m, '#a8300e')
    c.speckle(m, '#f08a4a', 0.08, seed=65)


@icone('encre_de_calamar')
def encre_de_calamar(c):
    fiole_ingredient(c, '#141420', verre='#c8d4e0', bouchon='#2a2a3a')
    c.fill(rect(17, 19, 18, 19), '#3a3a5a')


@icone('cire_d_abeille')
def cire_d_abeille(c):
    c.shadow_under(16, 27, 12, 2.4, 0.3)
    m = polygon([(5, 12), (26, 10), (28, 24), (7, 27)])
    c.shape(m, R('#e8b830', light=1.15), L_sphere(10, 10, 18, 14, 0.4), hl=2)
    for y in range(13, 26, 4):
        for x in range(7 + (y // 4) % 2 * 2, 27, 4):
            c.fill(arc_band(x, y, 1.6, 1.6, 1, 0, 360) & m & ~edge(m), '#c8901a')


@icone('propolis')
def propolis(c):
    c.shadow_under(16, 27, 11, 2.4, 0.3)
    for (x, y, rx, ry) in ((11, 19, 6, 5), (20, 21, 5, 4), (17, 14, 4, 3.4)):
        c.shape(ellipse(x, y, rx, ry), R('#8a4a12', light=1.45), L_sphere(x - 2, y - 2, rx, ry, 0.2), hl=2)
    c.fill(rect(10, 17, 10, 17) | rect(19, 20, 19, 20), '#f8c060')
