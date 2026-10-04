"""
Le Hunter Virus dans le monde : dessins des plantes et champignons au sol (32x32, trois stades, modèle en croix
comme les fleurs vanilla), champignons accrochés aux troncs (modèles à éléments), sols à ingrédient,
troncs écorcés, nid d'aigle-araignée, lotus de l'aube et source de sève.
La ligne du bas des sprites (y = 31) est le niveau du sol.
"""
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, '..', '..', 'monde'))
sys.path.insert(0, os.path.join(HERE, '..', '..', 'textures'))
sys.path.insert(0, os.path.join(HERE, '..', '..', 'models'))
import plantes as Pl  # noqa: E402
from pix import *  # noqa: E402,F401,F403
from parts import leaf, stem  # noqa: E402
import stations  # noqa: E402
import materials as Mat  # noqa: E402

SOL = Pl.SOL
tige, pousse, touffe, dome, fleur, fruit = Pl.tige, Pl.pousse, Pl.touffe, Pl.dome, Pl.fleur, Pl.fruit


def rnd(cle):
    return Pl.rnd(cle)


# ============================================================================ archétypes de plantes
def marguerite(c, s, feu, petale, coeur, n=3, h=15):
    if s == 0:
        pousse(c, 11, 5, feu); pousse(c, 21, 5, feu)
        return
    hh = 10 if s == 1 else h
    xs = (8, 16, 24)[:n] if n == 3 else (11, 21)
    touffe(c, 16, 6, feu, n=5, w=4, spread=16, seed=3)
    for cx in xs:
        tige(c, cx, SOL, cx, SOL - hh, feu)
        if s == 2:
            for k in range(8):
                a = k * math.pi / 4
                c.fill(circle(cx + math.cos(a) * 2.2, SOL - hh + math.sin(a) * 1.6, 1.2), petale)
            c.fill(circle(cx, SOL - hh, 1.3), coeur)
        else:
            c.fill(circle(cx, SOL - hh, 1.3), mix(feu, '#ffffff', 0.3))


def fleurs5(c, s, feu, petale, coeur='#f0c020', h=13, n=4):
    if s == 0:
        pousse(c, 11, 5, feu); pousse(c, 21, 5, feu)
        return
    hh = 9 if s == 1 else h
    rs = rnd('f5' + petale)
    xs = [6 + k * (20 / max(1, n - 1)) for k in range(n)]
    for k, cx in enumerate(xs):
        top = SOL - hh - rs.randint(0, 4)
        tige(c, 16, SOL, cx, top, feu)
        leaf(c, (16 + cx) / 2, (SOL + top) / 2, (16 + cx) / 2 + (3 if k % 2 else -3), (SOL + top) / 2 - 2, 2.4, feu, vein=False)
        if s == 2:
            fleur(c, cx, top, petale, r=1.6, coeur=coeur)


def epis(c, s, feu, couleur, h=16, n=4, feuilles=None):
    if s == 0:
        pousse(c, 11, 5, feu, fine=True); pousse(c, 21, 5, feu, fine=True)
        return
    hh = 11 if s == 1 else h
    touffe(c, 16, 6, feuilles or feu, n=6, w=4, spread=18, seed=5)
    for k in range(n):
        cx = 7 + k * (18 / max(1, n - 1))
        top = SOL - hh - (k % 2) * 2
        tige(c, 16 + (cx - 16) * 0.3, SOL - 3, cx, top + 5, feu)
        if s == 2:
            for j in range(6):
                y = top + j
                c.fill(rect(int(cx) - 1, int(y), int(cx), int(y)), couleur if j % 2 == 0 else mix(couleur, '#000000', 0.2))
        else:
            c.fill(rect(int(cx), int(top + 2), int(cx), int(top + 5)), mix(feu, couleur, 0.3))


def ombelles(c, s, feu, couleur, h=17, n=3):
    if s == 0:
        pousse(c, 11, 5, feu); pousse(c, 21, 5, feu)
        return
    hh = 11 if s == 1 else h
    touffe(c, 16, 7, feu, n=5, w=4.5, spread=18, seed=6)
    for k in range(n):
        cx = 8 + k * (16 / max(1, n - 1))
        top = SOL - hh + (k % 2) * 2
        tige(c, 16, SOL - 2, cx, top, feu)
        if s == 2:
            for j in range(9):
                a = j * math.pi / 8 + math.pi
                c.fill(rect(int(cx + math.cos(a) * 3.2), int(top + math.sin(a) * 1.6), int(cx + math.cos(a) * 3.2), int(top + math.sin(a) * 1.6)), couleur)
            c.fill(rect(int(cx) - 2, int(top), int(cx) + 2, int(top) + 1), couleur)


def feuillage(c, s, feu, h=14, fleurs=None, large=4.2):
    if s == 0:
        pousse(c, 11, 5, feu); pousse(c, 21, 5, feu)
        return
    hh = 10 if s == 1 else h
    for cx in (10, 22):
        dome(c, cx, hh, 7, feu, leafw=large, n=14 if s == 1 else 20, seed=cx, veine=True)
    if s == 2 and fleurs:
        rs = rnd('fe' + fleurs)
        for _ in range(6):
            x, y = rs.uniform(6, 26), rs.uniform(SOL - hh - 1, SOL - hh + 4)
            c.fill(rect(int(x), int(y), int(x), int(y) + 1), fleurs)


def rosette_racine(c, s, feu, racine_col, fleurs=None, h=13, tete=None):
    """Plante à racine : rosette de feuilles, la racine affleure quand elle est prête, et sa floraison."""
    if s == 0:
        pousse(c, 15, 5, feu)
        return
    hh = 9 if s == 1 else h
    touffe(c, 16, hh, feu, n=7, w=5, spread=20, seed=8)
    if s == 2:
        c.shape(ellipse(16, SOL - 1, 4, 2) & (YY <= SOL), R(racine_col), L_const(0.6))
        if fleurs:
            tige(c, 16, SOL - 3, 17, SOL - hh - 5, feu)
            if tete == 'ombelle':
                for j in range(7):
                    c.fill(circle(14 + j * 1, SOL - hh - 6 + abs(j - 3) * 0.6, 1.2), fleurs)
            elif tete == 'trompette':
                for dx in (-3, 3):
                    c.shape(polygon([(17 + dx - 2, SOL - hh - 7), (17 + dx + 2, SOL - hh - 7), (17 + dx + 1, SOL - hh - 2), (17 + dx - 1, SOL - hh - 2)]),
                            R(fleurs), L_const(0.65))
            elif tete == 'baies':
                for (dx, dy) in ((0, 0), (-1.5, 1.5), (1.5, 1.5)):
                    c.fill(circle(17 + dx, SOL - hh - 6 + dy, 1.2), fleurs)
            else:
                fleur(c, 17, SOL - hh - 5, fleurs, r=1.6)


def arbuste(c, s, feu, fruits=None, fleurs=None, h=22):
    if s == 0:
        pousse(c, 15, 7, feu)
        return
    hh = 15 if s == 1 else h
    tige(c, 16, SOL, 16, SOL - 5, '#6a4a2a', 2)
    dome(c, 16, hh, 12, feu, leafw=3, n=26 if s == 1 else 40, seed=3, veine=True)
    if s == 2:
        rs = rnd('ar' + str(fruits) + str(fleurs))
        for _ in range(9):
            x, y = rs.uniform(7, 25), rs.uniform(SOL - hh + 2, SOL - 6)
            if fruits:
                fruit(c, x, y, 1.4, fruits, hl=1)
            if fleurs:
                c.fill(circle(x, y, 1.3), fleurs)


def champignons(c, s, chapeau, pied='#efe6d4', forme='bombe', taches=False, lueur=None):
    if lueur and s == 2:
        c.fill(circle(16, SOL - 6, 12) & (np.random.RandomState(5).rand(32, 32) < 0.06), lueur)
    tailles = [(12, 1.6), (20, 1.3)] if s == 0 else [(9, 2.6), (17, 3.4), (24, 2.4)] if s == 1 else [(8, 3.8), (17, 5), (25, 3.6), (13, 2.4)]
    for (x, r) in tailles:
        hgt = r * (2.4 if forme == 'haut' else 1.6) + 1
        c.shape(rect(int(x - r * 0.3), int(SOL - hgt), int(x + r * 0.3), SOL), R(pied), L_horiz(x - 1, x + 1, 0.9, 0.4))
        if forme == 'entonnoir':
            m = polygon([(x - r, SOL - hgt - 1), (x + r, SOL - hgt - 1), (x + 1, SOL - hgt + r * 0.6), (x - 1, SOL - hgt + r * 0.6)])
        elif forme == 'conique':
            m = polygon([(x, SOL - hgt - r * 1.6), (x + r, SOL - hgt + 0.5), (x - r, SOL - hgt + 0.5)])
        elif forme == 'boule':
            m = ellipse(x, SOL - r, r, r * 0.9)
        else:
            m = ellipse(x, SOL - hgt, r + 0.5, r * 0.7) & (YY <= SOL - hgt + 0.5)
        c.shape(m, R(chapeau, light=1.2), L_sphere(x - 1, SOL - hgt - 1, r, r * 0.7), hl=1)
        if taches and r > 2:
            c.fill(m & ~edge(m) & (np.random.RandomState(int(x)).rand(32, 32) < 0.15), '#f4f0e6')


def tapis(c, s, couleur, fleurs=None):
    """Mousse en tapis, vue de dessus (texture du dessus d'un modèle plat)."""
    rs = rnd('tapis' + couleur)
    dens = (0.35, 0.65, 0.95)[s]
    for _ in range(int(140 * dens)):
        x, y = rs.uniform(0, 32), rs.uniform(0, 32)
        r = rs.uniform(1.2, 2.6)
        c.fill(circle(x, y, r), mix(couleur, '#000000', rs.uniform(0, 0.3)))
    if s == 2 and fleurs:
        for _ in range(10):
            x, y = rs.randint(2, 30), rs.randint(2, 30)
            c.fill(rect(x, y, x, y), fleurs)


def prele(c, s, feu):
    n = (3, 5, 7)[s]
    hh = (6, 12, 20)[s]
    for k in range(n):
        x = 5 + k * (22 / max(1, n - 1))
        top = SOL - hh + (k % 3) * 2
        for y in range(int(top), SOL + 1):
            c.fill(rect(int(x), y, int(x), y), feu if (y - int(top)) % 4 else '#2a4a1a')
            if (y - int(top)) % 4 == 0 and s > 0:
                c.fill(rect(int(x) - 2, y, int(x) + 2, y), mix(feu, '#ffffff', 0.15))


def aloe(c, s, feu):
    hh = (6, 11, 15)[s]
    for (x1, dy, w) in ((4, 0.5, 3.4), (28, 0.5, 3.4), (10, 1, 3), (22, 1, 3), (16, 1, 2.6)):
        leaf(c, 16, SOL, x1, SOL - hh * dy, w, feu, vein=False)
    if s == 2:
        tige(c, 16, SOL - 4, 18, SOL - hh - 8, '#6a8a4a')
        for j in range(4):
            c.fill(rect(17, SOL - hh - 8 + j * 2, 19, SOL - hh - 8 + j * 2), '#e8781a')


def bambou(c, s, feu):
    hh = (8, 18, 30)[s]
    for (x, w) in ((10, 2.2), (20, 2)):
        for y in range(SOL - hh + 2, SOL + 1):
            c.fill(rect(int(x - w), y, int(x + w), y), feu if (y % 7) else '#5a8a2a')
        leaf(c, x, SOL - hh + 4, x + 7, SOL - hh, 2.6, '#4a9a2a', vein=False)
        leaf(c, x, SOL - hh + 8, x - 7, SOL - hh + 5, 2.6, '#4a9a2a', vein=False)


def chardon(c, s, feu, couleur):
    if s == 0:
        pousse(c, 15, 5, feu)
        return
    hh = 10 if s == 1 else 16
    for (x0, x1) in ((16, 6), (16, 26)):
        m = leaf(c, x0, SOL - 2, x1, SOL - 8, 4, feu, vein=False)
        for (x, y) in np.argwhere(edge(m))[:, ::-1][::2]:
            c.fill(rect(x, y, x, y), '#d8e8f0')
    tige(c, 16, SOL, 16, SOL - hh, feu, 2)
    if s == 2:
        c.shape(ellipse(16, SOL - hh, 3.2, 2.6), R('#5a7a6a'), L_const(0.6))
        for k in range(9):
            a = math.pi * (1.1 + 0.8 * k / 8)
            c.fill(line_mask(16, SOL - hh - 2, 16 + math.cos(a) * 5, SOL - hh - 2 + math.sin(a) * 5), couleur)


def lys(c, s, feu, couleur, coeur):
    if s == 0:
        pousse(c, 15, 5, feu)
        return
    hh = 11 if s == 1 else 17
    tige(c, 16, SOL, 16, SOL - hh, feu)
    leaf(c, 16, SOL - 3, 8, SOL - 9, 2.6, feu, vein=False)
    leaf(c, 16, SOL - 5, 24, SOL - 11, 2.6, feu, vein=False)
    if s == 2:
        for k in range(5):
            a = -math.pi / 2 + (k - 2) * 0.55
            c.shape(capsule(16, SOL - hh, 16 + math.cos(a) * 6, SOL - hh + math.sin(a) * 6, 0.8, 1.6), R(couleur, light=1.1), L_const(0.75))
        c.fill(rect(16, SOL - hh, 16, SOL - hh), coeur)
    else:
        c.shape(ellipse(16, SOL - hh - 1, 1.6, 3), R(mix(couleur, feu, 0.5)), L_const(0.6))


def orchidee(c, s, feu, couleur):
    for (x1, y1) in ((4, SOL - 3), (28, SOL - 2)):
        leaf(c, 16, SOL, x1, y1, 4.5, feu, vein=False)
    if s == 0:
        return
    hh = 10 if s == 1 else 16
    tige(c, 15, SOL - 1, 19, SOL - hh, '#3a6a2a')
    if s == 2:
        for (x, y) in ((17, SOL - hh + 5), (20, SOL - hh)):
            for (dx, dy) in ((-2, -1), (2, -1), (0, -3)):
                c.fill(ellipse(x + dx, y + dy, 1.8, 1.4), couleur)
            c.fill(circle(x, y + 1, 1.2), mix(couleur, '#000000', 0.3))


PLANTES = {
    'camomille': lambda c, s: marguerite(c, s, '#6a9a3a', '#f4f4ec', '#f0b820'),
    'souci': lambda c, s: marguerite(c, s, '#5a8a2a', '#f08a14', '#c85a08'),
    'pissenlit': lambda c, s: marguerite(c, s, '#4a9a2a', '#f8d81a', '#e8a808', n=2, h=11),
    'plantain': lambda c, s: epis(c, s, '#3a8a2a', '#7a6a3a', h=15, n=3),
    'lavande': lambda c, s: epis(c, s, '#6a8a5a', '#8a5ac8', h=17, n=5),
    'coquelicot': lambda c, s: fleurs5(c, s, '#5a8a3a', '#e02a1a', '#1a1010', h=15, n=3),
    'bourrache': lambda c, s: fleurs5(c, s, '#5a8a4a', '#3a6ad8', '#1a1a2a', h=12, n=4),
    'echinacee': lambda c, s: marguerite(c, s, '#4a8a2a', '#c84aa0', '#a8541a'),
    'achillee': lambda c, s: ombelles(c, s, '#5a8a3a', '#f2ece0', h=16),
    'bourse_a_pasteur': lambda c, s: epis(c, s, '#7a9a4a', '#8ab84a', h=16, n=3),
    'arnica': lambda c, s: marguerite(c, s, '#5a8a3a', '#f8b81a', '#d87a08', n=2, h=13),
    'gentiane': lambda c, s: rosette_racine(c, s, '#4a7a3a', '#c8a060', '#2a5ae8', tete='trompette'),
    'sauge': lambda c, s: epis(c, s, '#7a8a6a', '#8a6ac8', h=14, n=3, feuilles='#8aa08a'),
    'fenouil_graines': lambda c, s: ombelles(c, s, '#7aa84a', '#e8d040', h=18),
    'reglisse_racine': lambda c, s: rosette_racine(c, s, '#4a8a3a', '#8a5a2a', '#9a8ae8'),
    'lin_graines': lambda c, s: fleurs5(c, s, '#7aa04a', '#5a8ae8', '#f8f0d0', h=14, n=5),
    'consoude': lambda c, s: feuillage(c, s, '#3a7a2a', h=13, fleurs='#9a5ab8', large=5),
    'guimauve_racine': lambda c, s: rosette_racine(c, s, '#6a9a5a', '#e8d8b8', '#f0b8d0'),
    'prele': lambda c, s: prele(c, s, '#5a9a3a'),
    'melisse': lambda c, s: feuillage(c, s, '#7ac84a', h=12),
    'millepertuis': lambda c, s: fleurs5(c, s, '#6a8a3a', '#f8d020', '#e88a10', h=14, n=5),
    'ortie': lambda c, s: feuillage(c, s, '#3a7a2a', h=15, large=3.8),
    'valeriane_racine': lambda c, s: rosette_racine(c, s, '#5a8a3a', '#c8a87a', '#f0c8d8', tete='ombelle'),
    'bardane_racine': lambda c, s: rosette_racine(c, s, '#4a7a2a', '#6a4a2a', '#9a3a8a', tete='baies'),
    'sureau_fleurs': lambda c, s: arbuste(c, s, '#4a7a2a', fleurs='#f6f0d8'),
    'aubepine_baies': lambda c, s: arbuste(c, s, '#3a7a2a', fruits='#c81e1a'),
    'angelique_racine': lambda c, s: rosette_racine(c, s, '#6a9a3a', '#a8784a', '#d8e8a0', tete='ombelle', h=15),
    'ginkgo_feuilles': lambda c, s: arbuste(c, s, '#9ac83a'),
    'ginseng_racine': lambda c, s: rosette_racine(c, s, '#4a8a3a', '#e8c890', '#d8241c', tete='baies'),
    'aloe': lambda c, s: aloe(c, s, '#5a9a6a'),
    'bambou': lambda c, s: bambou(c, s, '#7ab83a'),
    # champignons au sol
    'coprin': lambda c, s: champignons(c, s, '#ece6da', forme='haut'),
    'vesse_de_loup': lambda c, s: champignons(c, s, '#f0eadc', forme='boule'),
    'girolle': lambda c, s: champignons(c, s, '#f0a01a', '#f0b030', forme='entonnoir'),
    'pied_bleu': lambda c, s: champignons(c, s, '#9a7ab8', '#a88ac8'),
    'maitake': lambda c, s: champignons(c, s, '#8a7a62', '#c8b89a', forme='entonnoir'),
    'morille': lambda c, s: champignons(c, s, '#8a6a4a', '#f0e6cc', forme='conique'),
    'trompette_des_morts': lambda c, s: champignons(c, s, '#3a2e2a', '#3a2e2a', forme='entonnoir'),
    'amanite_rouge': lambda c, s: champignons(c, s, '#d8201a', taches=True),
    'reishi': lambda c, s: champignons(c, s, '#8a1e12', '#5a1a10'),
    'cordyceps': lambda c, s: champignons(c, s, '#e8781a', '#e8781a', forme='haut'),
    'champignon_luminescent': lambda c, s: champignons(c, s, '#3ad8e8', '#b8f0f0', lueur='#8af0ff'),
    'bolet_cendre': lambda c, s: champignons(c, s, '#7a7a7a', '#d8d0c4'),
    # raretés
    'fleur_de_lune_pale': lambda c, s: fleurs5(c, s, '#4a7a8a', '#d8e8f8', '#a8c8f0', h=13, n=3),
    'baie_de_givre': lambda c, s: arbuste(c, s, '#5a8aa0', fruits='#4aa8e8', h=18),
    'pollen_dore': lambda c, s: marguerite(c, s, '#6a8a3a', '#f8d040', '#f0a010', n=2, h=12),
    'mousse_de_numere': None,
    'baie_de_l_ile_de_la_baleine': lambda c, s: arbuste(c, s, '#3a8a5a', fruits='#4a3ab8'),
    'feuille_de_kukuroo': lambda c, s: arbuste(c, s, '#1aa89a', h=24),
    'chardon_de_meteor': lambda c, s: chardon(c, s, '#6a8a8a', '#b84ad8'),
    'lys_des_nuees': lambda c, s: lys(c, s, '#4a8a5a', '#f4f6fa', '#f0c840'),
    'orchidee_de_kakin': lambda c, s: orchidee(c, s, '#2e7a3a', '#e83aa8'),
}
TAPIS = {'mousse_de_numere': ('#5a6a5a', '#c84a8a')}


def plante(id, stade):
    c = Canvas()
    if id in TAPIS:
        tapis(c, stade, *TAPIS[id])
    else:
        PLANTES[id](c, stade)
    return c.image()


# ============================================================================ champignons de tronc
TRONC = {  # id : (chapeau, dessous, forme)
    'pleurote': ('#b8b4a8', '#8a8478', 'etages'),
    'oreille_de_judas': ('#6a3a2a', '#3a1a12', 'oreilles'),
    'shiitake': ('#7a4a2a', '#e8dcc8', 'etages'),
    'polypore': ('#b8a684', '#efe6d0', 'console'),
    'criniere_de_lion': ('#f0ead8', '#e0d6c0', 'crins'),
    'chaga': ('#2a2018', '#c8742a', 'masse'),
}


def texture_tronc(id):
    chap, dessous, forme = TRONC[id]
    c = Mat.new(chap)
    Mat.noise(c, [mix(chap, '#ffffff', 0.2), mix(chap, '#000000', 0.25)], 0.3, abs(sum(map(ord, id))))
    if forme == 'crins':
        for x in range(0, 32, 2):
            c.fill(rect(x, 0, x, 31), mix(chap, '#000000', 0.12))
    if forme == 'masse':
        for y in range(0, 32, 5):
            c.fill(rect(0, y, 31, y), '#120c08')
    c.fill(rect(0, 26, 31, 31), dessous)
    return c


def modele_tronc(id, age):
    """Face nord collée au tronc (FACING pointe vers le tronc) : le champignon sort vers le sud."""
    nom = 'tronc_%s_%d' % (id, age)
    m = stations.Model(nom, 'hxrpmetiers:blocks/virus/monde/tronc_' + id)
    stations.MODELS.pop(nom, None)
    t = 'virus/monde/tronc_' + id
    forme = TRONC[id][2]
    k = (0.5, 0.8, 1.0)[age]
    if forme in ('etages', 'console', 'oreilles'):
        couches = [(8, 9, 6), (5.5, 7, 8.5)] if age > 0 else [(7.5, 8, 4)]
        if age == 2:
            couches.append((10.5, 6, 5.5))
        for (y, larg, sa) in couches:
            m.box([8 - larg * k / 2, y, 0], [8 + larg * k / 2, y + 1.2 * (1 + (forme == 'console')), sa * k], t)
    elif forme == 'crins':
        m.box([8 - 4 * k, 5, 0], [8 + 4 * k, 5 + 6 * k, 6 * k], t)
        for x in (-2.5, 0, 2.5):
            m.box([8 + x * k - 0.5, 5 - 2 * k, 1], [8 + x * k + 0.5, 5, 4 * k], t)
    else:  # masse noire du chaga
        m.box([8 - 3.5 * k, 8 - 3 * k, 0], [8 + 3.5 * k, 8 + 3 * k, 4 * k], t)
    return m


# ============================================================================ sols, troncs écorcés, sève
def sol(bloc):
    if bloc == 'terre_truffiere':
        c = Mat.new('#6a4a2e')
        Mat.noise(c, ['#7a5a3a', '#5a3a22', '#8a6a46'], 0.5, 401)
        for (x, y) in ((8, 9), (22, 18), (12, 25)):
            c.fill(ellipse(x, y, 3, 2.6), '#2a2220')
            c.fill(rect(x - 1, y - 1, x - 1, y - 1), '#4a3c36')
    elif bloc == 'vase_a_algue_noire':
        c = Mat.new('#2a2a22')
        Mat.noise(c, ['#3a3a2a', '#1e1e18', '#44442e'], 0.45, 402)
        rs = np.random.RandomState(403)
        for _ in range(9):
            x = rs.randint(0, 32)
            for y in range(rs.randint(0, 10), rs.randint(18, 32)):
                c.fill(rect(x + int(math.sin(y / 3) * 1.5), y, x + int(math.sin(y / 3) * 1.5), y), '#141a16')
    elif bloc == 'pierre_moussue_humide':
        c = Mat.pierre()
        rs = np.random.RandomState(404)
        for _ in range(40):
            x, y = rs.randint(0, 32), rs.randint(0, 32)
            c.fill(circle(x, y, rs.uniform(1, 2.4)), ['#3a7a3a', '#4a8a3a', '#2e6a3a'][rs.randint(3)])
        for _ in range(5):
            x = rs.randint(0, 32)
            c.fill(rect(x, rs.randint(0, 20), x, rs.randint(20, 32)), '#7ab8d8')
    elif bloc == 'roche_a_fer_sang':
        c = Mat.pierre_sombre()
        rs = np.random.RandomState(405)
        for _ in range(7):
            x0, y0 = rs.randint(0, 32), rs.randint(0, 32)
            c.fill(line_mask(x0, y0, x0 + rs.randint(-8, 8), y0 + rs.randint(-8, 8), 1), '#8a1a14')
            c.fill(circle(x0, y0, 1.6), '#c8302a')
    else:  # roche des abysses
        c = Mat.new('#1e222e')
        Mat.noise(c, ['#262a38', '#161a24', '#2a3040'], 0.5, 406)
        rs = np.random.RandomState(407)
        for _ in range(26):
            x, y = rs.randint(0, 32), rs.randint(0, 32)
            c.fill(circle(x, y, rs.uniform(0.8, 2)), ['#1aa8b8', '#2ac8c8', '#8af0f8'][rs.randint(3)])
    return c


def tronc_ecorce(bouleau, bout=False):
    if bout:
        c = Mat.new('#e8d4a8' if bouleau else '#c8a06a')
        for r in range(2, 16, 3):
            c.fill(arc_band(16, 16, r, r, 1, 0, 360), '#b89a6a' if bouleau else '#9a7444')
        return c
    c = Mat.new('#f0e2bc' if bouleau else '#d8b47a')
    Mat.vstreaks(c, ['#e4d2a8', '#f8ecd0'] if bouleau else ['#c8a068', '#e4c088'], 60, 410 + bouleau, lmin=6, lmax=20)
    # lambeaux d'écorce restés en place
    rs = np.random.RandomState(412 + bouleau)
    for _ in range(4):
        x, y = rs.randint(0, 28), rs.randint(0, 28)
        c.fill(rect(x, y, x + 3, y + rs.randint(2, 5)), '#ecece4' if bouleau else '#6a5a42')
    return c


def seve(face):
    c = Mat.new('#5a3a22')
    Mat.vstreaks(c, ['#6a4a2a', '#4a2e18', '#7a5232'], 60, 420, lmin=8, lmax=24)
    if face == 'dessus':
        for r in range(2, 16, 3):
            c.fill(arc_band(16, 16, r, r, 1, 0, 360), '#3a2414')
        c.fill(circle(16, 16, 3), '#f0a020')
        c.fill(circle(15, 15, 1.2), '#fff0a0')
        return c
    rs = np.random.RandomState(421)
    for _ in range(5):
        x = rs.randint(2, 30)
        y0 = rs.randint(0, 14)
        L = rs.randint(8, 18)
        c.fill(rect(x, y0, x + 1, min(31, y0 + L)), '#e8901a')
        c.fill(rect(x, y0, x, min(31, y0 + L)), '#ffd060')
        c.fill(ellipse(x + 0.5, min(31, y0 + L), 1.4, 1.6), '#f0a020')
    return c


# ============================================================================ nid, lotus
def texture_brindilles():
    c = Mat.new('#7a5a32')
    rs = np.random.RandomState(430)
    for _ in range(60):
        x, y = rs.uniform(0, 32), rs.uniform(0, 32)
        a = rs.uniform(0, math.pi)
        c.fill(line_mask(x, y, x + math.cos(a) * 6, y + math.sin(a) * 6), ['#5a3e20', '#9a7a4a', '#4a3018', '#b8945a'][rs.randint(4)])
    return c


def texture_oeuf():
    c = Mat.new('#c8c0b0')
    rs = np.random.RandomState(431)
    for _ in range(20):
        x, y = rs.randint(0, 32), rs.randint(0, 32)
        c.fill(rect(x, y, x + 1, y), '#5a4a5a')
    return c


def modele_nid(n):
    nom = 'nid_%d' % n
    m = stations.Model(nom, 'hxrpmetiers:blocks/virus/monde/brindilles')
    stations.MODELS.pop(nom, None)
    t = 'virus/monde/brindilles'
    m.disc(8, 8, 6, 0, 1.2, t)
    m.ring(8, 8, 7.4, 1.2, 5, 1.8, t, faces='nsewud')
    m.box([1, 3.2, 5], [3, 4.6, 6], t)
    m.box([13, 2.6, 10], [15.5, 4, 11], t)
    for k in range(n):
        x, z = (6.2, 7.5) if k == 0 else (9.8, 9)
        m.box([x - 1.3, 1.2, z - 1.6], [x + 1.3, 4, z + 1.6], 'virus/monde/oeuf_aigle')
    return m


def texture_lotus_feuille():
    c = Canvas()
    c.shape(circle(16, 16, 15) & ~polygon([(16, 16), (31, 12), (31, 20)]), R('#3a8a4a'), L_sphere(10, 10, 16, 16, 0.4))
    for a in range(0, 360, 30):
        c.fill(line_mask(16, 16, 16 + math.cos(math.radians(a)) * 14, 16 + math.sin(math.radians(a)) * 14) & circle(16, 16, 14), '#2a6a3a')
    return c


def texture_lotus_fleur(ouverte):
    c = Canvas()
    if ouverte:
        for k in range(7):
            a = math.pi * (1.05 + 0.9 * k / 6)
            x1, y1 = 16 + math.cos(a) * 12, 26 + math.sin(a) * 12
            c.shape(capsule(16, 27, x1, y1, 1.8, 3.4), R('#f8a8c8' if k % 2 else '#fcc8d8', light=1.1), L_const(0.75))
        for k in range(4):
            a = math.pi * (1.2 + 0.6 * k / 3)
            c.shape(capsule(16, 26, 16 + math.cos(a) * 7, 26 + math.sin(a) * 7, 1.5, 2.6), R('#ffe0a0'), L_const(0.8))
        c.fill(circle(16, 23, 1.6), '#f8c030')
    else:
        c.shape(ellipse(16, 22, 4, 8), R('#e888a8', light=1.15), L_sphere(15, 19, 4, 8))
        c.fill(rect(16, 29, 16, 31), '#3a8a4a')
    return c


def icone_lotus_plante(c):
    c.shadow_under(16, 27, 13, 2.4, 0.25)
    c.shape(ellipse(16, 23, 14, 5.5) & ~polygon([(16, 23), (31, 20), (31, 27)]), R('#3a8a4a'), L_vert(18, 28, 0.85, 0.35))
    for k in range(5):
        a = math.pi * (1.1 + 0.8 * k / 4)
        c.shape(capsule(15, 19, 15 + math.cos(a) * 9, 19 + math.sin(a) * 9, 1.6, 2.8), R('#fcc8d8', light=1.1), L_const(0.75))
    c.fill(circle(15, 16, 1.4), '#f8c030')


def modele_lotus(fleuri):
    nom = 'lotus_%s' % ('fleuri' if fleuri else 'bouton')
    m = stations.Model(nom, 'hxrpmetiers:blocks/virus/monde/lotus_feuille', ao=False)
    stations.MODELS.pop(nom, None)
    m.box([0, 0, 0], [16, 0.25, 16], 'virus/monde/lotus_feuille', faces='ud', uv='full')
    m.sprite(8, 8, 0.25, 9 if fleuri else 7, 9 if fleuri else 6, 'virus/monde/lotus_' + ('fleur' if fleuri else 'bouton'))
    return m
