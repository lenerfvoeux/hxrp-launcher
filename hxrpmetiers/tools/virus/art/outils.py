"""
Icônes des objets de l'apothicaire (32x32, même moteur que le Gourmet : tools/textures/pix.py et parts.py) :
seringues, ordonnance, carnet de consultation, parchemin, préparation ratée, préparation en cours.
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, '..', '..', 'textures'))
from pix import *  # noqa: E402,F401,F403
from parts import leaf, stem, jar, bottle  # noqa: E402,F401

REG = {}


def icone(*ids):
    def deco(fn):
        for i in ids:
            REG[i] = fn
        return fn
    return deco


# ============================================================================ seringues
def seringue(c, liquide=None, niveau=0.65, x0=4.5, y0=27.5, x1=27, y1=5):
    """Seringue en diagonale : aiguille en bas à gauche, piston en haut à droite.
    liquide : couleur du contenu (None : vide)."""
    dx, dy = x1 - x0, y1 - y0
    L = math.hypot(dx, dy)
    ux, uy = dx / L, dy / L
    px, py = -uy, ux                       # perpendiculaire

    def pt(t):
        return x0 + ux * t, y0 + uy * t

    # aiguille
    ax, ay = pt(0)
    bx, by = pt(6.5)
    c.fill(line_mask(ax, ay, bx, by), '#c8ccd4')
    c.fill(line_mask(ax + 0.6, ay - 0.4, bx, by), '#eef2f6')
    # embout en laiton
    hx, hy = pt(6.5)
    ex, ey = pt(9)
    c.shape(capsule(hx, hy, ex, ey, 1.3, 1.9), R('#c8922a', light=1.3), L_cyl(hx, hy, ex, ey, 2), hl=1)
    # corps en verre
    gx0, gy0 = pt(9)
    gx1, gy1 = pt(22)
    corps = capsule(gx0, gy0, gx1, gy1, 3.4)
    c.shape(corps, [(150, 170, 186), (196, 216, 228), (222, 236, 244), (238, 247, 252)], L_cyl(gx0, gy0, gx1, gy1, 3.6))
    if liquide:
        lx1, ly1 = pt(9 + 13 * niveau)
        liq = capsule(gx0, gy0, lx1, ly1, 2.3) & corps & ~edge(corps)
        c.shape(liq, R(liquide, light=1.25), L_cyl(gx0, gy0, lx1, ly1, 2.4), edge=False)
    # graduations
    for k in range(3, 13, 2):
        mx, my = pt(9 + k)
        c.fill(line_mask(mx + px * 1.6, my + py * 1.6, mx + px * 3.0, my + py * 3.0), '#5a6a78')
    # reflet
    rx0, ry0 = pt(10)
    rx1, ry1 = pt(20)
    c.fill(line_mask(rx0 - px * 1.8, ry0 - py * 1.8, rx1 - px * 1.8, ry1 - py * 1.8) & corps & ~edge(corps), '#ffffff')
    # ailettes (appui des doigts)
    fx, fy = pt(22.3)
    c.shape(capsule(fx - px * 5, fy - py * 5, fx + px * 5, fy + py * 5, 1.1), R('#c8922a'), L_const(0.6), hl=1)
    # tige et poussoir du piston
    tx, ty = pt(30)
    c.fill(line_mask(fx, fy, tx, ty, 2), '#9aa2ac')
    c.fill(line_mask(fx, fy, tx, ty, 1), '#d8dde4')
    c.shape(capsule(tx - px * 3.2, ty - py * 3.2, tx + px * 3.2, ty + py * 3.2, 1.2), R('#2a2c32'), L_const(0.6))


@icone('seringue_vide')
def seringue_vide(c):
    c.shadow_under(16, 28, 9, 2.2, 0.25)
    seringue(c)


@icone('seringue_pleine')
def seringue_pleine(c):
    c.shadow_under(16, 28, 9, 2.2, 0.25)
    seringue(c, '#a8121a', 0.85)
    # étiquette nouée au nom du patient
    c.shape(rrect(17, 21, 25, 26, 1), R('#f4ecd0'), L_const(0.75))
    c.fill(line_mask(18, 23, 23, 23), '#5a4a3a')
    c.fill(line_mask(18, 24.5, 21, 24.5), '#5a4a3a')
    c.fill(line_mask(16, 20, 18, 21.5), '#b8302a')


# ============================================================================ papiers
def papier(c, pts, couleur='#f4ecd4', coin=True):
    m = polygon(pts)
    c.shape(m, R(couleur, light=1.1), L_sphere(14, 10, 22, 22, 0.8))
    if coin:
        # coin replié en bas à droite
        (ax, ay), (bx, by) = pts[2], pts[1]
        cm = polygon([(pts[2][0] - 5, pts[2][1]), (pts[2][0], pts[2][1] - 5), (pts[2][0] - 4.5, pts[2][1] - 4.5)])
        c.shape(cm, R(mix(couleur, '#a08860', 0.35)), L_const(0.45))
    return m


def ecriture(c, m, x0, x1, ys, encre='#2a3a5a', seed=1):
    rs = np.random.RandomState(seed)
    for y in ys:
        x = x0
        fin = x1 - rs.randint(0, 5)
        while x < fin:
            n = rs.randint(2, 5)
            c.fill(line_mask(x, y + rs.uniform(-0.3, 0.3), min(fin, x + n), y + rs.uniform(-0.3, 0.3)) & m & ~edge(m), encre)
            x += n + rs.randint(1, 3)


@icone('ordonnance')
def ordonnance(c):
    c.shadow_under(17, 28.5, 11, 2, 0.25)
    m = papier(c, [(6, 4), (25, 3), (27, 28), (8, 29.5)])
    # en-tête : serpent et coupe de l'apothicaire
    c.fill(line_mask(9, 7, 22, 6.5) & m, '#7a2a1a')
    c.shape(ellipse(11.5, 11, 2.6, 2.6), R('#2e6a4a'), L_const(0.6))
    c.fill(points([(11, 10), (12, 11), (11, 12)]), '#f4ecd4')
    ecriture(c, m, 15, 23, [10, 12.5], '#3a2a20', 3)
    ecriture(c, m, 9.5, 24, [16, 18.5, 21, 23.5], '#2a3a5a', 5)
    # cachet de cire
    c.shape(ellipse(21.5, 25.5, 3.4, 3.1), R('#b8241c', light=1.3), L_sphere(20.5, 24.5, 3.4, 3.1), hl=1)
    c.fill(points([(21, 25), (22, 25), (21, 26)]), '#7a1008')


@icone('carnet_de_consultation')
def carnet(c):
    c.shadow_under(16, 28.5, 12, 2.2, 0.25)
    # couverture de cuir, ruban, étiquette
    cov = polygon([(5, 6), (24, 4), (27, 26), (8, 28.5)])
    c.shape(cov, R('#5a2a1a', light=1.25), L_sphere(12, 10, 22, 22, 0.6))
    pages = polygon([(8, 27), (26.5, 24.5), (27, 26), (8, 28.5)])
    c.shape(pages, R('#efe2c0'), L_const(0.75), edge=False)
    for k in range(3):
        c.fill(line_mask(9, 27.4 + k * 0.3, 26, 25 + k * 0.4) & pages, '#c8b890')
    c.shape(polygon([(10, 9), (21, 7.5), (22, 14), (11, 15.5)]), R('#f4ecd4'), L_const(0.75))
    ecriture(c, polygon([(10, 9), (21, 7.5), (22, 14), (11, 15.5)]), 12, 20, [10.5, 12.8], '#3a2a20', 7)
    # croix de l'apothicaire
    c.shape(rect(15, 18, 17, 24) | rect(13, 20, 19, 22), R('#c8922a'), L_const(0.65), hl=1)
    # ruban marque-page
    c.shape(polygon([(20, 3), (22.5, 3), (22.5, 9), (21.2, 8), (20, 9)]), R('#b8241c'), L_const(0.6))


@icone('parchemin_du_second_souffle')
def parchemin(c):
    c.shadow_under(16, 28, 12, 2, 0.25)
    feuille = polygon([(7, 7), (25, 7), (25, 25), (7, 25)])
    c.shape(feuille, R('#ead6a0', light=1.15), L_sphere(14, 12, 18, 18, 0.7))
    for y in (6.5, 25.5):
        rl = capsule(5, y, 27, y, 2.6)
        c.shape(rl, R('#d8bc80', light=1.2), L_cyl(5, y, 27, y, 2.8), hl=1)
        c.shape(ellipse(4.5, y, 1.6, 2.6) | ellipse(27.5, y, 1.6, 2.6), R('#7a4a22'), L_const(0.6))
    ecriture(c, feuille, 9.5, 23, [11, 13.5, 16, 18.5], '#4a2a1a', 11)
    # sceau doré de la plume de phénix
    c.shape(ellipse(16, 21.5, 3.2, 2.8), R('#e8a020', light=1.35), L_sphere(15, 20.5, 3.2, 2.8), hl=2)
    c.fill(line_mask(14.5, 23, 17.5, 20), '#b84a10')


@icone('preparation_ratee')
def ratee(c):
    c.shadow_under(16, 28, 10, 2.2, 0.3)
    # pot fendu, contenu grisâtre qui déborde
    pot = rrect(8, 13, 24, 28, 3)
    c.shape(pot, R('#8a7a6a'), L_horiz(8, 24, 0.85, 0.3))
    c.shape(ellipse(16, 13.5, 8, 2.6), R('#5a5a3a'), L_const(0.5))
    c.shape(ellipse(13, 11.5, 4, 2.4) | ellipse(18.5, 11, 3.5, 2.2) | rect(20, 12, 22, 17), R('#6a6a42'), L_sphere(14, 10, 8, 4, 0.4), hl=1)
    c.fill(line_mask(15, 15, 17, 20) | line_mask(17, 20, 15, 24) | line_mask(15, 24, 16, 27), '#2a2018')
    for (x, y) in ((11, 7), (19, 6), (14, 4)):
        c.shape(circle(x, y, 1.1), R('#9a9a7a'), L_const(0.6), edge=False)


@icone('preparation_en_cours_officine')
def en_cours(c):
    c.shadow_under(16, 28.5, 11, 2.2, 0.25)
    # coupelle de céramique émaillée et spatule, mélange encore brut
    c.shape(ellipse(16, 22, 12, 6.5), R('#3a6a8a', light=1.25), L_vert(16, 28, 0.9, 0.3))
    c.shape(ellipse(16, 20, 10.5, 4.6), R('#e8eef0'), L_const(0.8))
    c.shape(ellipse(16, 20.5, 8.5, 3.4), R('#6a8a3a'), L_sphere(14, 19, 9, 4, 0.4), edge=False)
    for (x, y, col_) in ((12, 20, '#c8a040'), (17, 19.5, '#8a3a2a'), (19.5, 21, '#3a7a2a'), (14.5, 21.5, '#e8d8a0')):
        c.fill(rect(x, y, x + 1, y), col_)
    c.fill(line_mask(18, 19, 27, 6, 2), '#7a4a22')
    c.fill(line_mask(18.5, 19, 27, 6.5), '#b07a40')
