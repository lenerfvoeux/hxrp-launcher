"""
Les machines de l'officine du Hunter Virus en modèles 3D à éléments (même DSL que les stations du Gourmet),
face avant au NORD : yagen, hachoir à levier, chaudron sur brasero, alambic de cuivre, jarres de macération
(quatre variantes : 0 à 3 jarres scellées), balance d'apothicaire, mortier, pilulier, table de préparation laquée,
microscope d'analyse, Grimoire des maladies sur son lutrin, présentoir de l'apothicaire.
"""
import math

import numpy as np

from machines import modele, builder, V
from matieres import tex, new, noise, hstreaks, vstreaks, box_bevel, anneau
from pix import rect, ellipse, circle, line_mask, col, mix


# ============================================================================ matières propres aux machines
@tex('brique')
def brique():
    c = new('#8a3a24')
    for y in range(0, 32, 6):
        c.fill(rect(0, y, 31, y), '#5a2014')
        off = 0 if (y // 6) % 2 == 0 else 6
        for x in range(off, 32, 12):
            c.fill(rect(x, y, x, y + 5), '#5a2014')
    noise(c, ['#9a4a2e', '#7a3020', '#a85a38'], 0.25, 301)
    return c


@tex('foyer_face')
def foyer_face():
    """Façade du foyer de l'alambic : briques, gueule du feu avec braises."""
    c = brique()
    c.fill(rect(9, 14, 22, 31), '#1a0a06')
    c.fill(ellipse(15.5, 14, 6.5, 4) & rect(9, 10, 22, 14), '#1a0a06')
    for k in range(10):
        x = 10 + (k * 7) % 12
        c.fill(ellipse(x, 28 - (k % 3), 1.6, 1.2), ['#f8a030', '#e8501a', '#ffd060'][k % 3])
    c.fill(rect(8, 13, 23, 13), '#c8922a')
    return c


@tex('poudre_verte')
def poudre_verte():
    c = new('#7a9a4a')
    noise(c, ['#8aaa5a', '#6a8a3a', '#9aba6a', '#5a7a2a'], 0.6, 302)
    return c


@tex('herbes_v')
def herbes_v():
    c = new('#4a7a2a')
    for x in range(0, 32, 2):
        c.fill(rect(x, 0, x, 31), '#3a6a1e' if x % 4 else '#5a8a34')
    noise(c, ['#7aa848', '#2e5a18'], 0.15, 303)
    return c


@tex('decoction')
def decoction():
    c = new('#7a4a18')
    for y in range(32):
        for x in range(32):
            if (x * 3 + y * 5) % 23 < 2:
                c.px[y, x, :3] = col('#a8702a')
    noise(c, ['#8a5a22', '#6a3a10', '#c88a3a'], 0.2, 304)
    return c


@tex('eau_v')
def eau_v():
    c = new('#2a6a9a')
    for y in range(0, 32, 5):
        c.fill(rect((y * 3) % 20, y, (y * 3) % 20 + 8, y), '#5a9aca')
    noise(c, ['#3a7aaa', '#24608e'], 0.2, 305)
    return c


@tex('acier_v')
def acier_v():
    c = new('#aab4c0')
    hstreaks(c, ['#c8d0dc', '#8a96a4', '#e0e6ee'], 60, 306)
    return c


@tex('page')
def page():
    """Pages couvertes d'écriture à l'encre, deux colonnes."""
    c = new('#efe2c0')
    noise(c, ['#e8d8b0', '#f6ecd4'], 0.15, 307)
    rs = np.random.RandomState(308)
    for y in range(3, 30, 3):
        for (x0, x1) in ((2, 14), (18, 30)):
            x = x0
            while x < x1:
                L = rs.randint(2, 5)
                c.fill(rect(x, y, min(x1, x + L - 1), y), '#5a3a28')
                x += L + 1
    c.fill(rect(15, 0, 16, 31), '#c8b890')
    c.fill(ellipse(8, 8, 2.5, 2.5), '#8a1e14')
    return c


@tex('cuir_rouge')
def cuir_rouge():
    c = new('#7a1e16')
    noise(c, ['#8a2a1e', '#6a1610', '#9a3424'], 0.35, 309)
    box_bevel(c, 0, 0, 31, 31, '#c8922a', '#8a5a10')
    return c


@tex('bougie')
def bougie():
    c = new('#f2ead8')
    for y in range(32):
        c.fill(rect(0, y, 31, y), mix('#fff8e8', '#d8ccb0', y / 31))
    c.fill(rect(10, 0, 13, 9), '#fffaf0')
    return c


@tex('flamme_bougie')
def flamme_bougie():
    from pix import Canvas
    c = Canvas()
    c.fill(ellipse(16, 20, 5, 10), '#f8a030')
    c.fill(ellipse(16, 22, 3, 7), '#ffe070')
    c.fill(ellipse(16, 25, 1.5, 3), '#ffffff')
    return c


@tex('tissu')
def tissu():
    c = new('#e8e0cc')
    for y in range(32):
        for x in range(32):
            if (x + y) % 3 == 0:
                c.px[y, x, :3] = col('#d8d0b8')
    c.fill(rect(0, 3, 31, 4), '#b8241c')
    return c


@tex('pilulier_dessus')
def pilulier_dessus():
    """Dessus du pilulier : planche claire, six rainures, boudin de pâte et pilules coupées."""
    c = new('#d4a86a')
    vstreaks(c, ['#e0b87a', '#c8985a'], 40, 310, lmin=6, lmax=20)
    for x in (5, 9, 12, 17, 21, 26):
        c.fill(rect(x, 4, x, 27), '#7a5028')
        c.fill(rect(x + 1, 4, x + 1, 27), '#e8c890')
    c.fill(rect(2, 14, 29, 17), '#6a8a4a')
    c.fill(rect(2, 14, 29, 14), '#8aaa6a')
    box_bevel(c, 0, 0, 31, 31, '#e8c890', '#8a6030')
    return c


@tex('table_dessus')
def table_dessus():
    """Plateau de la table laquée : laque profonde, filet doré, linge et godets peints dessus."""
    c = new('#5c1c14')
    vstreaks(c, ['#6a2418', '#4e1610'], 50, 311, lmin=8, lmax=24)
    box_bevel(c, 0, 0, 31, 31, '#8a3a28', '#2a0a06')
    c.fill(rect(2, 2, 29, 2) | rect(2, 29, 29, 29) | rect(2, 2, 2, 29) | rect(29, 2, 29, 29), '#c8922a')
    # reflet de vernis en biais
    for k in range(10):
        c.fill(rect(6 + k, 22 - k, 7 + k, 22 - k), '#8a3a28')
    return c


@tex('presentoir_fond')
def presentoir_fond():
    c = new('#4e1610')
    vstreaks(c, ['#5c1c14', '#44140e'], 40, 312, lmin=8, lmax=24)
    # panneaux à motif de nuages dorés
    for (cx, cy) in ((8, 8), (24, 8), (8, 24), (24, 24)):
        c.fill(circle(cx, cy, 3) & ~circle(cx, cy, 2), '#a8781e')
        c.fill(circle(cx + 2, cy - 1, 1.5) & ~circle(cx + 2, cy - 1, 0.6), '#a8781e')
    return c


@tex('roue_yagen')
def roue_yagen():
    """Face de la roue du yagen : disque de fonte, six rayons, moyeu de laiton (découpe ronde)."""
    from pix import Canvas
    c = Canvas()
    c.fill(circle(16, 16, 15.5), '#3c3e46')
    c.fill(circle(16, 16, 15.5) & ~circle(16, 16, 13.5), '#5a5e68')
    c.fill(circle(16, 16, 13.5) & ~circle(16, 16, 12.5), '#24262a')
    for k in range(6):
        a = k * math.pi / 3
        c.fill(line_mask(16, 16, 16 + math.cos(a) * 13, 16 + math.sin(a) * 13, 2), '#6a6e78')
    c.fill(circle(16, 16, 4), '#c8922a')
    c.fill(circle(15, 15, 1.6), '#f0d080')
    c.fill(circle(16, 16, 1.2), '#5a3a10')
    return c


@tex('lame_microscope')
def lame_microscope():
    c = new('#dcecf2')
    c.fill(ellipse(16, 16, 6, 4), '#c84a4a')
    c.fill(ellipse(15, 15, 3, 2), '#e87a7a')
    return c


# ============================================================================ outils de construction
def roue_verticale(m, cx, cy, z0, z1, r, t, a=0.62, b=0.86):
    """Disque vertical (axe nord-sud) en pavés qui ne se chevauchent pas : une croix et quatre coins."""
    A, B = r * a, r * b
    m.box([cx - r, cy - A, z0], [cx + r, cy + A, z1], t)
    m.box([cx - A, cy + A, z0], [cx + A, cy + r, z1], t)
    m.box([cx - A, cy - r, z0], [cx + A, cy - A, z1], t)
    for sx in (-1, 1):
        for sy in (-1, 1):
            x0, x1 = sorted((cx + sx * A, cx + sx * B))
            y0, y1 = sorted((cy + sy * A, cy + sy * B))
            m.box([x0, y0, z0], [x1, y1, z1], t)


# ============================================================================ YAGEN
@builder
def yagen():
    m = modele('yagen', 'fonte_v')
    m.box([1, 0, 5], [15, 1.5, 11], V('bois_brut'))
    # auge en barque : flancs, fond de gorge, extrémités relevées
    m.box([3, 1.5, 5.8], [13, 5, 7], V('fonte_v'))
    m.box([3, 1.5, 9], [13, 5, 10.2], V('fonte_v'))
    m.box([3, 1.5, 7], [13, 3.4, 9], V('fonte_v'), faces='ud')
    m.box([3.5, 3.4, 7], [12.5, 3.8, 9], V('poudre_verte'), faces='u')
    m.box([1.6, 2.5, 6.4], [3, 5.6, 9.6], V('fonte_v'))
    m.box([13, 2.5, 6.4], [14.4, 5.6, 9.6], V('fonte_v'))
    # la roue de fonte, debout dans la gorge (jante en pavés, faces peintes), et son axe de bois tenu à deux mains
    cx, cy, r = 6.5, 8.4, 4.6
    roue_verticale(m, cx, cy, 7.3, 8.7, r, V('fonte_rouille'))
    for z, f in ((7.29, 'n'), (8.71, 's')):
        m.box([cx - r, cy - r, z], [cx + r, cy + r, z], V('roue_yagen'), faces=f, uv='full')
    m.box([6, 7.9, 8.71], [7, 8.9, 14.5], V('bois_clair_v'))
    m.box([6, 7.9, 1.5], [7, 8.9, 7.29], V('bois_clair_v'))
    m.box([5.7, 7.6, 14.5], [7.3, 9.2, 15.5], V('bois_brut'))
    m.box([5.7, 7.6, 0.5], [7.3, 9.2, 1.5], V('bois_brut'))
    return m


# ============================================================================ HACHOIR À LEVIER
@builder
def hachoir():
    m = modele('hachoir_a_levier', 'bois_clair_v')
    m.box([2, 0, 4], [14, 4, 12], {'u': V('bois_clair_v'), '*': V('bois_brut')})
    m.box([4.5, 4, 6.5], [10, 5.2, 9.5], V('herbes_v'))
    # montant et pivot en laiton
    m.box([12.6, 4, 7.2], [14, 7.5, 8.8], V('laiton_v'))
    # lame d'acier levée (22,5°) et sa poignée de bois
    m.box([2.5, 6.2, 7.7], [13.4, 8.4, 8.3], V('acier_v'), rot=('z', -22.5, (13.3, 6.8, 8)))
    m.box([0.6, 7.6, 7.4], [3.6, 8.8, 8.6], V('bois_brut'), rot=('z', -22.5, (13.3, 6.8, 8)))
    return m


# ============================================================================ CHAUDRON SUR BRASERO
@builder
def chaudron():
    m = modele('chaudron_sur_brasero', 'fonte_v', light=9)
    for (x, z) in ((3, 4), (13, 4), (8, 13.5)):
        m.box([x - 0.6, 0, z - 0.6], [x + 0.6, 3.5, z + 0.6], V('fonte_v'))
    m.ring(8, 8.5, 6.5, 3, 4.6, 1, V('fonte_rouille'))
    m.disc(8, 8.5, 5.6, 3, 4, V('braises_v'), faces='u', uv='full')
    m.sprite(8, 8.5, 4, 6.5, 6, V('flamme_v'))
    # chaudron de fonte, rebord, décoction qui bout, anses
    m.disc(8, 8.5, 5, 5, 11, V('fonte_v'))
    m.ring(8, 8.5, 5.6, 11, 12.6, 0.8, V('fonte_v'))
    m.disc(8, 8.5, 4.6, 11, 12, V('decoction'), faces='u', uv='full')
    m.box([1.6, 10.5, 8], [2.4, 12, 9], V('fonte_v'))
    m.box([13.6, 10.5, 8], [14.4, 12, 9], V('fonte_v'))
    m.sprite(8, 8.5, 12.6, 16, 6, V('vapeur_v'))
    return m


# ============================================================================ ALAMBIC DE CUIVRE
@builder
def alambic():
    m = modele('alambic_de_cuivre', 'cuivre', light=6)
    m.box([1, 0, 4], [8, 5, 12], {'n': V('foyer_face'), '*': V('brique')}, uv={'n': 'full', '*': 'size'})
    m.disc(4.5, 8, 3.4, 5, 9.5, V('cuivre'))
    m.disc(4.5, 8, 2.4, 9.5, 11.5, V('cuivre'))
    m.box([4, 11.5, 7.5], [5, 13, 8.5], V('cuivre'))
    # col de cygne vers le serpentin
    m.box([5, 12.2, 7.6], [8, 13, 8.4], V('cuivre'))
    m.box([8, 11, 7.6], [10, 12.6, 8.4], V('cuivre'), rot=('z', -22.5, (8, 12.6, 8)))
    m.box([10.4, 9.4, 7.6], [11.2, 11, 8.4], V('cuivre'))
    # tonneau d'eau froide cerclé de fer
    m.disc(12, 8, 3.2, 0, 9.2, V('bois_brut'))
    m.ring(12, 8, 3.4, 2, 2.8, 0.4, V('fonte_v'), faces='nsewud')
    m.ring(12, 8, 3.4, 7, 7.8, 0.4, V('fonte_v'), faces='nsewud')
    m.disc(12, 8, 2.6, 9.2, 9.4, V('eau_v'), faces='u', uv='full')
    # bec et fiole de verre qui recueille le distillat
    m.box([11.6, 2.5, 3.4], [12.4, 3.1, 4.8], V('cuivre'))
    m.box([11, 0, 1.2], [13, 2.2, 3.2], V('verre_v'), faces='nsewu')
    m.box([11.6, 2.2, 1.8], [12.4, 3, 2.6], V('verre_v'), faces='nsewu')
    m.box([11.2, 0.1, 1.4], [12.8, 1.2, 3], V('poudre_verte'), faces='nsewu')
    return m


# ============================================================================ JARRES DE MACÉRATION (0 à 3 scellées)
JARRES = [  # x, z, rayon, hauteur, matière
    (5, 10.5, 3.4, 9, 'ceramique_celadon'),
    (11.7, 10.8, 2.9, 7.5, 'terre_emaillee'),
    (8.2, 4.6, 2.4, 5.6, 'porcelaine_bleue'),
]


def jarres_n(n):
    m = modele('jarres_de_maceration_%d' % n, 'ceramique_celadon')
    m.box([1, 0, 1.5], [15, 0.8, 14.5], V('bois_brut'))
    for k, (x, z, r, h, mat) in enumerate(JARRES):
        y0 = 0.8
        m.disc(x, z, r * 0.75, y0, y0 + 1, V(mat))
        m.disc(x, z, r, y0 + 1, y0 + h * 0.75, V(mat))
        m.disc(x, z, r * 0.6, y0 + h * 0.75, y0 + h, V(mat), faces='nsewd')
        haut = y0 + h
        if k < n:
            # couvercle de papier ficelé d'une cordelette rouge
            m.disc(x, z, r * 0.72, haut, haut + 0.5, V('papier'), uv='full')
            m.ring(x, z, r * 0.74, haut - 0.6, haut, 0.25, V('corde_rouge'), faces='nsewud')
            m.box([x - 0.3, haut - 1.8, z - r * 0.6 - 0.15], [x + 0.3, haut - 0.6, z - r * 0.6], V('corde_rouge'), faces='nsew')
        else:
            m.disc(x, z, r * 0.6, haut, haut + 0.01, V('laque_noire'), faces='u', uv='full')
    return m


for _n in range(4):
    builder(lambda n=_n: jarres_n(n))


# ============================================================================ BALANCE D'APOTHICAIRE
@builder
def balance():
    m = modele('balance_d_apothicaire', 'laiton_v')
    m.box([3, 0, 5.5], [13, 1.6, 10.5], {'u': V('laque'), '*': V('laque_sombre')})
    m.box([6.5, 0.5, 5.3], [9.5, 1.2, 5.5], V('laiton_v'), faces='nsewu')
    m.box([7.4, 1.6, 7.4], [8.6, 10.6, 8.6], V('laiton_v'))
    m.box([7, 10.6, 7], [9, 11.4, 9], V('laiton_v'))
    m.box([2.4, 11.4, 7.7], [13.6, 12, 8.3], V('laiton_v'))
    m.box([7.8, 12, 7.8], [8.2, 14.5, 8.2], V('fonte_v'))
    for x in (3.4, 12.6):
        for dz in (-1.2, 1.2):
            m.box([x - 0.1, 6.4, 8 + dz - 0.1], [x + 0.1, 11.4, 8 + dz + 0.1], V('fonte_v'), faces='nsew')
        m.disc(x, 8, 2.2, 5.8, 6.4, V('laiton_v'))
    # un petit tas de poudre à gauche, des poids à droite
    m.disc(3.4, 8, 1.2, 6.4, 7.2, V('poudre_verte'))
    m.disc(12.6, 8, 0.9, 6.4, 7.8, V('laiton_v'))
    m.disc(12.6, 8, 0.6, 7.8, 8.6, V('laiton_v'))
    for k, h in enumerate((1.2, 0.9, 0.7)):
        m.disc(9.8 + k * 1.2, 9.6, 0.5, 1.6, 1.6 + h, V('laiton_v'))
    return m


# ============================================================================ MORTIER ET PILON
@builder
def mortier():
    m = modele('mortier_d_apothicaire', 'porcelaine_bleue')
    m.disc(8, 8, 3.2, 0, 1, V('porcelaine_bleue'))
    m.ring(8, 8, 4.6, 1, 6, 1, V('porcelaine_bleue'), uv='size')
    m.disc(8, 8, 3.6, 1, 2.6, V('porcelaine_bleue'))
    m.disc(8, 8, 3.4, 2.6, 3.6, V('poudre_verte'), faces='u', uv='full')
    # pilon de porcelaine à manche de bois, appuyé dans le bol
    m.box([7.2, 3.6, 7.2], [8.8, 5, 8.8], V('porcelaine_bleue'), rot=('x', 22.5, (8, 3.6, 8)))
    m.box([7.5, 5, 7.5], [8.5, 11.5, 8.5], V('bois_clair_v'), rot=('x', 22.5, (8, 3.6, 8)))
    return m


# ============================================================================ PILULIER
@builder
def pilulier():
    m = modele('pilulier', 'bois_clair_v')
    m.box([1, 0, 3], [15, 1.6, 13], {'u': V('pilulier_dessus'), '*': V('bois_brut')}, uv={'u': 'proj', '*': 'size'})
    m.box([7.4, 1.6, 2.6], [8.6, 2.4, 13.4], V('acier_v'))
    for z in (1.2, 13.4):
        m.box([7.1, 1.4, z], [8.9, 3, z + 1.4], V('bois_brut'))
    # coupelle de pilules prêtes
    m.disc(12.8, 5, 1.8, 1.6, 2.2, V('porcelaine_bleue'))
    return m


# ============================================================================ TABLE DE PRÉPARATION LAQUÉE
@builder
def table():
    m = modele('table_de_preparation', 'laque')
    for (x, z) in ((1, 1), (13.5, 1), (1, 13.5), (13.5, 13.5)):
        m.box([x, 0, z], [x + 1.5, 13, z + 1.5], V('laque_noire'))
    m.box([2.5, 3, 1.4], [13.5, 4, 2.4], V('laque_sombre'))
    m.box([2.5, 3, 13.6], [13.5, 4, 14.6], V('laque_sombre'))
    m.box([0, 13, 0], [16, 15, 16], {'u': V('table_dessus'), '*': V('laque_sombre')}, uv={'u': 'proj', '*': 'size'})
    # formulaire ouvert, linge, godets de porcelaine, spatule
    m.box([2, 15, 2], [8.5, 15.4, 7], V('cuir_rouge'))
    m.box([2.3, 15.4, 2.3], [8.2, 15.8, 6.7], V('page'), uv='full')
    m.box([9, 15, 2.5], [14.5, 15.2, 7.5], V('tissu'), uv='full')
    for (x, z) in ((4, 11), (7, 12), (10, 11.5), (13, 12.2)):
        m.disc(x, z, 1.3, 15, 16, V('porcelaine_bleue'))
    m.box([9.5, 15.2, 4.5], [14, 15.5, 5.1], V('bois_clair_v'))
    return m


# ============================================================================ MICROSCOPE D'ANALYSE
@builder
def microscope():
    m = modele('microscope_d_analyse', 'laiton_v')
    m.box([4, 0, 4], [12, 1, 12], V('fonte_v'))
    # potence en laiton (colonne arrière et bras), platine, miroir
    m.box([7, 1, 10], [9, 11, 11.5], V('laiton_v'))
    m.box([7.2, 9.5, 7.6], [8.8, 11, 10], V('laiton_v'))
    m.box([4.5, 4.5, 4.5], [11.5, 5.2, 10], V('fonte_v'))
    m.box([6, 5.2, 5.6], [10, 5.4, 7.6], V('lame_microscope'), faces='u', uv='full')
    m.box([7, 2, 6.2], [9, 3.4, 6.6], V('verre_v'), faces='nsewu')
    # tube droit : tourelle d'objectifs, corps, oculaire
    m.box([7.4, 5.8, 6.2], [8.6, 7, 7.4], V('fonte_v'))
    m.box([7.1, 7, 5.9], [8.9, 13, 7.7], V('laiton_v'))
    m.box([7.3, 13, 6.1], [8.7, 15, 7.5], V('fonte_v'))
    # molettes de mise au point de part et d'autre de la colonne
    m.box([5.6, 6.5, 10.1], [7, 8.1, 11.4], V('fonte_v'))
    m.box([9, 6.5, 10.1], [10.4, 8.1, 11.4], V('fonte_v'))
    return m


# ============================================================================ GRIMOIRE SUR SON LUTRIN
@builder
def grimoire():
    m = modele('grimoire_des_maladies', 'cuir_rouge')
    m.box([3, 0, 4], [13, 1, 12], V('laque_sombre'))
    m.box([7, 1, 7], [9, 9.5, 9], V('laque'))
    m.box([2, 9.5, 5], [14, 10.5, 12], V('laque'), rot=('x', 22.5, (8, 10, 8.5)))
    m.box([2.5, 10.5, 5.3], [13.5, 11, 11.7], V('cuir_rouge'), rot=('x', 22.5, (8, 10, 8.5)))
    m.box([3, 11, 5.6], [13, 11.6, 11.4], V('page'), uv='full', rot=('x', 22.5, (8, 10, 8.5)))
    m.box([7.8, 7.5, 4.2], [8.3, 10.5, 4.5], V('corde_rouge'), faces='nsew')
    # bougeoir de laiton et sa bougie
    m.disc(11.5, 9.5, 1.2, 1, 1.4, V('laiton_v'))
    m.box([11.1, 1.4, 9.1], [11.9, 4, 9.9], V('bougie'))
    m.sprite(11.5, 9.5, 4, 5.6, 1.6, V('flamme_bougie'))
    return m


# ============================================================================ PRÉSENTOIR DE L'APOTHICAIRE
@builder
def presentoir():
    m = modele('presentoir_de_l_apothicaire', 'laque')
    m.box([0, 0, 14.5], [16, 16, 16], {'n': V('presentoir_fond'), '*': V('laque')}, uv={'n': 'proj', '*': 'size'})
    m.box([0, 0, 4], [1, 15, 14.5], V('laque'))
    m.box([15, 0, 4], [16, 15, 14.5], V('laque'))
    for y in (0, 7.5):
        m.box([1, y, 4], [15, y + 1, 14.5], V('laque_sombre'))
        m.box([1, y + 1, 4], [15, y + 1.6, 4.5], V('laiton_v'))
    m.box([0, 15, 4], [16, 16, 14.5], V('laque_sombre'))
    for x in (0, 15):
        m.box([x, 15.8, 3.6], [x + 1, 16, 4], V('laiton_v'), faces='nsewu')
    return m
