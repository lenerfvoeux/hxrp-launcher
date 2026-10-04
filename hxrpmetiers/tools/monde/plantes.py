"""
Dessin des cultures (32x32, vue de côté, posées sur le modèle « crop » : quatre plans en dièse).
Trois stades : 0 vient d'être planté, 1 pousse (bientôt prêt), 2 prêt à récolter.
La ligne du bas est le niveau du sol.
"""
import math
import os
import sys
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, '..', 'textures'))
from parts import *  # noqa: E402,F401,F403
from pix import Canvas, mix  # noqa: E402

import especes  # noqa: E402

SOL = 31


def rnd(key):
    return np.random.RandomState(zlib.crc32(key.encode()) & 0x7fffffff)


def tige(c, x0, y0, x1, y1, color, w=1):
    c.fill(line_mask(x0, y0, x1, y1, w), R(color)[2])
    if w > 1:
        c.fill(line_mask(x0 + 0.5, y0, x1 + 0.5, y1, 1), R(color)[1])


def fruit(c, x, y, r, color, hl=1, ry=None):
    m = ellipse(x, y, r, ry or r)
    c.shape(m, R(color, light=1.2), L_sphere(x - r * 0.4, y - r * 0.4, r, ry or r), hl=hl)
    return m


def fleur(c, x, y, color, r=1.2, coeur='#f0c020'):
    for a in range(0, 360, 72):
        px, py = x + math.cos(math.radians(a)) * r, y + math.sin(math.radians(a)) * r
        c.fill(rect(int(round(px)), int(round(py)), int(round(px)), int(round(py))), color)
    c.fill(rect(int(x), int(y), int(x), int(y)), coeur)


def terre(c, x0=4, x1=27):
    """Petite butte de terre au pied de la plante."""
    c.shape(ellipse((x0 + x1) / 2, SOL + 1, (x1 - x0) / 2, 2.5) & (YY <= SOL), R('#6a4a2a'), L_const(0.5), edge=False)


def pousse(c, x, h, color, fine=False, seed=0):
    """Jeune pousse : tigelle et deux cotylédons (ou brin fin pour les graminées)."""
    top = SOL - h
    if fine:
        for dx, lean in ((-1, -2), (0, 0), (1, 2)):
            tige(c, x + dx, SOL, x + dx + lean, top + abs(lean), color)
        return
    tige(c, x, SOL, x, top + 1, color)
    leaf(c, x, top + 1, x - 4, top - 1, 2.6, color, vein=False)
    leaf(c, x, top + 1, x + 4, top - 1, 2.6, color, vein=False)


def graminee(c, xs, h, color, epi=None, epi_col=None, courbe=0.0, seed=0):
    """Touffe de tiges fines ; epi : longueur de l'épi en haut de chaque tige."""
    rs = rnd('g%d' % seed)
    for x in xs:
        hh = h + rs.randint(-2, 3)
        lean = rs.uniform(-2, 2)
        tx, ty = x + lean, SOL - hh
        tige(c, x, SOL, tx, ty, color)
        # feuilles le long de la tige
        for k in range(2):
            ly = SOL - hh * (0.3 + 0.25 * k)
            s = 1 if (k + int(x)) % 2 else -1
            c.fill(line_mask(x + lean * 0.3, ly, x + lean * 0.3 + s * 4, ly - 3), R(color)[3])
        if epi:
            ex, ey = tx + courbe, ty - epi * 0.3
            m = capsule(tx, ty, ex, ty - epi, 1.4, 0.9)
            c.shape(m, R(epi_col, light=1.25), L_horiz(tx - 1.5, tx + 1.5, 0.95, 0.45), edge=False)
            c.fill(edge(m) & (XX > tx), R(epi_col)[1])
            for j in range(int(epi)):
                c.fill(rect(int(tx + courbe * j / max(1, epi) + (1 if j % 2 else -1)), int(ty - j), int(tx + courbe * j / max(1, epi) + (1 if j % 2 else -1)), int(ty - j)), R(epi_col)[1])


def touffe(c, cx, h, color, n=5, w=4.5, spread=12, seed=0, veine=True):
    """Feuillage en rosette vu de côté."""
    rs = rnd('t%d' % seed)
    for k in range(n):
        t = k / max(1, n - 1) - 0.5
        x1 = cx + t * spread + rs.uniform(-1, 1)
        y1 = SOL - h + abs(t) * h * 0.5 + rs.uniform(-1, 1)
        leaf(c, cx + t * 2, SOL - 1, x1, y1, w, mix(color, '#ffffff', 0.08 * (k % 2)), vein=veine)


def dome(c, cx, h, w, color, leafw=3.6, n=22, seed=0, veine=False):
    """Buisson dense : feuilles réparties dans un dôme, les plus sombres derrière, les plus claires devant."""
    rs = rnd('d%d%s' % (seed, color))
    feuilles = []
    for k in range(n):
        a = rs.uniform(0, math.pi)
        r = math.sqrt(rs.uniform(0.05, 1))
        x = cx + math.cos(a) * w * r
        y = SOL - math.sin(a) * h * r - 1
        feuilles.append((y, x, rs.uniform(-1, 1)))
    rr = R(color)
    for k, (y, x, t) in enumerate(sorted(feuilles)):
        shade = [rr[1], rr[2], rr[2], rr[3]][min(3, int((y - (SOL - h)) / max(1, h) * 4))]
        ang = math.atan2(y - SOL, x - cx) + t * 0.6
        L = leafw * 1.4
        leaf(c, x, y, x + math.cos(ang) * L, y + math.sin(ang) * L, leafw, shade, vein=veine)


def tuteur(c, x, h):
    tige(c, x, SOL, x, SOL - h, '#b08a5a', 1)
    c.fill(rect(x, SOL - h, x, SOL - h), '#8a6a3a')


def treillis(c):
    for x in (6, 25):
        tige(c, x, SOL, x, 6, '#a07a4a', 1)
    for y in (10, 18):
        c.fill(line_mask(6, y, 25, y), '#b08a5a')


# ------------------------------------------------------------------------------ formes
def f_cereale(c, s, id, pal, feu):
    if s == 0:
        pousse(c, 11, 6, feu, fine=True); pousse(c, 20, 7, feu, fine=True)
    elif s == 1:
        graminee(c, [7, 11, 15, 19, 23], 18, feu, seed=1)
    else:
        mature = mix(pal[0], '#c8a040', 0.3)
        graminee(c, [6, 10, 14, 18, 22, 26], 22, mature, epi=6, epi_col=pal[0], seed=2)


def f_riz(c, s, id, pal, feu):
    c.fill(rect(0, SOL - 1, 31, SOL), '#5a8ab0')
    c.fill(rect(0, SOL - 1, 31, SOL - 1), '#8ab8d8')
    if s == 0:
        pousse(c, 10, 7, feu, fine=True); pousse(c, 21, 6, feu, fine=True)
    elif s == 1:
        graminee(c, [7, 12, 17, 22, 26], 16, feu, seed=3)
    else:
        graminee(c, [6, 11, 16, 21, 26], 20, mix(feu, '#c8b060', 0.4), epi=7, epi_col='#e8d890', courbe=4, seed=4)


def f_sarrasin(c, s, id, pal, feu):
    if s == 0:
        pousse(c, 10, 6, feu); pousse(c, 21, 6, feu)
        return
    tiges = [8, 15, 22]
    for x in tiges:
        tige(c, x, SOL, x + 1, SOL - (16 if s == 1 else 22), '#b84a4a')
        leaf(c, x, SOL - 8, x - 5, SOL - 12, 4, feu)
        leaf(c, x + 1, SOL - 12, x + 6, SOL - 16, 3.6, feu)
    if s == 2:
        for x in tiges:
            for k in range(6):
                fleur(c, x + 1 + (k % 3) - 1, SOL - 22 + k // 3 * 2 - 1, '#f4e8ec', r=1)
            c.fill(rect(x, SOL - 19, x + 1, SOL - 19), pal[0])


def f_quinoa(c, s, id, pal, feu):
    if s == 0:
        pousse(c, 11, 6, feu); pousse(c, 20, 6, feu)
        return
    for x in (9, 16, 23):
        h = 17 if s == 1 else 24
        tige(c, x, SOL, x, SOL - h, feu)
        leaf(c, x, SOL - 7, x - 5, SOL - 10, 4, feu)
        leaf(c, x, SOL - 11, x + 5, SOL - 14, 3.6, feu)
        if s == 2:
            m = capsule(x, SOL - h, x, SOL - h + 8, 2.6, 2.2)
            c.shape(m, R(pal[2]), L_const(0.6))
            c.speckle(m, pal[0], 0.35, seed=x)


def f_mais(c, s, id, pal, feu):
    if s == 0:
        pousse(c, 15, 8, feu, fine=True)
        return
    h = 20 if s == 1 else 28
    for x in (10, 21):
        tige(c, x, SOL, x, SOL - h, mix(feu, '#a8c870', 0.2), 2)
        for k in range(4):
            y = SOL - 5 - k * (h / 5)
            sgn = 1 if (k + x) % 2 else -1
            leaf(c, x, y, x + sgn * 8, y - 2 + k, 3.2, feu, vein=True)
        if s == 2:
            ep = capsule(x + 2, SOL - h * 0.55, x + 3, SOL - h * 0.55 - 7, 1.9, 1.5)
            c.shape(ep, R('#9ac070'), L_const(0.6))
            c.shape(capsule(x + 2.5, SOL - h * 0.55 - 2, x + 3, SOL - h * 0.55 - 6, 1.2, 1), R(pal[0]), L_const(0.7), edge=False)
            for k in range(4):
                c.fill(line_mask(x, SOL - h, x - 2 + k * 1.3, SOL - h - 4), '#d8b870')


def f_racine(c, s, id, pal, feu):
    terre(c)
    if s == 0:
        pousse(c, 11, 5, feu); pousse(c, 20, 6, feu)
        return
    fin = id in ('carotte', 'aneth')
    for cx in (10, 21):
        if fin:
            for k in range(6):
                t = k / 5 - 0.5
                x1, y1 = cx + t * 10, SOL - (10 if s == 1 else 15) + abs(t) * 6
                tige(c, cx, SOL - 1, x1, y1, feu)
                for j in range(3):
                    px, py = cx + (x1 - cx) * (0.5 + j * 0.2), SOL - 1 + (y1 - SOL + 1) * (0.5 + j * 0.2)
                    c.fill(rect(int(px) - 1, int(py), int(px) + 1, int(py)), R(feu)[3])
        else:
            touffe(c, cx, 10 if s == 1 else 14, feu, n=4, w=4, spread=8, seed=cx)
        if s == 2:
            col = pal[0]
            m = ellipse(cx, SOL - 1, 3.2, 2.8) & (YY <= SOL)
            c.shape(m, R(col, light=1.2), L_sphere(cx - 1, SOL - 2, 3, 3), hl=1)


def f_tubercule(c, s, id, pal, feu):
    terre(c, 2, 29)
    if s == 0:
        pousse(c, 15, 6, feu)
        return
    touffe(c, 11, 12 if s == 1 else 15, feu, n=6, w=5, spread=11, seed=1)
    touffe(c, 21, 11 if s == 1 else 14, feu, n=5, w=5, spread=10, seed=2)
    if s == 2:
        fc = especes.FLEURS.get(id, '#f4f0f4')
        for (x, y) in [(8, SOL - 15), (14, SOL - 17), (23, SOL - 15)]:
            fleur(c, x, y, fc)
        for (x, y) in [(8, SOL), (16, SOL), (24, SOL)]:
            fruit(c, x, y - 0.5, 2.4, pal[0], hl=0, ry=1.8)


def f_bulbe(c, s, id, pal, feu):
    terre(c)
    n = 2 if s == 0 else 3
    for k, cx in enumerate((9, 16, 23)[:n] if s else (11, 20)):
        h = 6 if s == 0 else 14 if s == 1 else 17
        for dx in (-1, 0, 1):
            tige(c, cx + dx, SOL - 2, cx + dx * 3, SOL - h + abs(dx) * 3, feu)
        if s == 2:
            fruit(c, cx, SOL - 2, 3, pal[0], hl=1)
            c.fill(line_mask(cx, SOL - 5, cx, SOL - 7), pal[0])


def f_tuteur(c, s, id, pal, feu):
    if s == 0:
        pousse(c, 15, 7, feu)
        return
    h = 18 if s == 1 else 25
    for x in (9, 22):
        tuteur(c, x + 2, h + 2)
        tige(c, x, SOL, x + 1, SOL - h, feu)
        for k in range(4):
            y = SOL - 5 - k * h / 5
            sgn = 1 if k % 2 else -1
            leaf(c, x, y, x + sgn * 6, y - 3, 4.2, feu)
    fc = especes.FLEURS.get(id, '#f0d020')
    if s == 1:
        for (x, y) in [(6, SOL - 15), (12, SOL - 19), (19, SOL - 14), (25, SOL - 19)]:
            fleur(c, x, y, fc)
        return
    long_ = id in ('aubergine', 'piment')
    for (x, y) in [(7, SOL - 11), (12, SOL - 17), (20, SOL - 10), (25, SOL - 18), (10, SOL - 22)]:
        if long_:
            m = capsule(x, y, x + 0.5, y + (5 if id == 'aubergine' else 4), 1.9 if id == 'aubergine' else 1.2, 1.4 if id == 'aubergine' else 0.7)
            c.shape(m, R(pal[0], light=1.2), L_sphere(x - 1, y, 2, 4), hl=1)
        else:
            fruit(c, x, y, 2.4 if id == 'tomate' else 2.6, pal[0], ry=2.4 if id == 'tomate' else 3)
        c.fill(rect(x, int(y) - 3, x, int(y) - 2), R(feu)[1])


def f_courge(c, s, id, pal, feu):
    if s == 0:
        pousse(c, 15, 6, feu)
        return
    for (x, y, w) in [(6, SOL - 10, 7), (14, SOL - 13, 8), (22, SOL - 11, 7), (26, SOL - 6, 5), (3, SOL - 5, 5)][: 3 if s == 1 else 5]:
        tige(c, x, SOL, x, y + 3, feu)
        m = ellipse(x, y, w / 2 + 0.5, w / 2 - 0.5)
        c.shape(m, R(feu), L_sphere(x - 1, y - 1, w / 2, w / 2), hl=0)
        c.fill(line_mask(x, y + 2, x, y - 2) & m, R(feu)[3])
    if s == 2:
        fc = especes.FLEURS.get(id, '#f0c020')
        fleur(c, 18, SOL - 17, fc, r=1.5)
        m = capsule(8, SOL - 1.5, 21, SOL - 2.5, 2.4, 2.2)
        c.shape(m, R(pal[0], light=1.15), L_cyl(8, SOL - 1.5, 21, SOL - 2.5, 2.4), hl=2)
        c.speckle(m, pal[2], 0.12, seed=5)


def f_gros_fruit(c, s, id, pal, feu):
    if s == 0:
        pousse(c, 15, 6, feu)
        return
    tige(c, 2, SOL - 1, 29, SOL - 2, feu)
    for (x, y) in [(5, SOL - 6), (12, SOL - 8), (20, SOL - 7), (27, SOL - 5)][: 3 if s == 1 else 4]:
        m = ellipse(x, y, 3.8, 3)
        c.shape(m, R(feu), L_sphere(x - 1, y - 1, 4, 3))
        c.fill(line_mask(x, y + 2, x, y - 2) & m, R(feu)[3])
    if s == 1:
        fleur(c, 16, SOL - 10, especes.FLEURS.get(id, '#f0c020'), r=1.5)
        return
    if id == 'potiron':
        for (dx, rx) in [(-4, 4.5), (4, 4.5), (-1.5, 4.5), (1.5, 4.5), (0, 4.5)]:
            fruit(c, 16 + dx, SOL - 6, rx, pal[0], hl=1, ry=6)
        c.shape(capsule(16, SOL - 11, 17, SOL - 14, 1.2, 1), R('#6a7a3a'), L_const(0.5))
    elif id == 'pasteque':
        m = ellipse(16, SOL - 5.5, 9.5, 6)
        c.shape(m, R(pal[0], light=1.2), L_sphere(13, SOL - 9, 10, 6), hl=2)
        for k in range(-3, 4):
            c.fill(line_mask(16 + k * 2.6, SOL - 11, 16 + k * 3.4, SOL) & m & ~edge(m), '#1c5a1c')
    else:
        m = ellipse(16, SOL - 5.5, 7, 6)
        c.shape(m, R(pal[0], light=1.2), L_sphere(13, SOL - 9, 7, 6), hl=2)
        for (x, y) in rng_pts(8, m & ~edge(m), 26):
            c.fill(rect(x, y, x, y), '#e8e0b0')


def f_pomme(c, s, id, pal, feu):
    if s == 0:
        pousse(c, 11, 5, feu); pousse(c, 21, 5, feu)
        return
    if s == 1:
        touffe(c, 16, 11, feu, n=6, w=6, spread=16, seed=3)
        return
    touffe(c, 16, 9, R(feu)[1], n=6, w=6, spread=24, seed=4)
    if id == 'chou':
        fruit(c, 16, SOL - 7, 8, pal[0], hl=2, ry=7)
        for k in range(3):
            c.fill(arc_band(16, SOL - 3, 6 - k * 2, 7 - k * 2, 1, 200, 340), pal[2])
    elif id == 'laitue':
        for (x, y, r) in [(11, SOL - 6, 5), (21, SOL - 6, 5), (16, SOL - 9, 6)]:
            m = ellipse(x, y, r, r * 0.8)
            for a in range(0, 360, 45):
                m |= circle(x + r * 0.85 * math.cos(math.radians(a)), y + r * 0.7 * math.sin(math.radians(a)), 1.4)
            c.shape(m, R(pal[0], light=1.25), L_sphere(x - 1, y - 2, r + 1, r), hl=1)
    else:
        leaf(c, 8, SOL - 1, 5, SOL - 12, 6, feu)
        leaf(c, 24, SOL - 1, 27, SOL - 12, 6, feu)
        col = pal[0]
        for (x, y, r) in [(12, SOL - 8, 3.4), (20, SOL - 8, 3.4), (16, SOL - 10, 3.8), (14, SOL - 6, 3), (18, SOL - 6, 3)]:
            fruit(c, x, y, r, col, hl=1)
        if id == 'chou_fleur':
            c.speckle(ellipse(16, SOL - 8, 7, 4), '#e8dcc0', 0.2, seed=3)


def f_tige(c, s, id, pal, feu):
    terre(c)
    if s == 0:
        pousse(c, 11, 6, feu, fine=True); pousse(c, 20, 6, feu, fine=True)
        return
    h = 14 if s == 1 else 22
    for cx in (10, 21):
        base = pal[0] if id == 'poireau' else pal[1]
        if id == 'poireau':
            c.shape(rect(cx - 1, SOL - h * 0.45, cx + 1, SOL), R(base), L_horiz(cx - 1, cx + 1, 0.9, 0.3))
            for dx in (-4, -1, 2, 5):
                leaf(c, cx, SOL - h * 0.45, cx + dx, SOL - h, 3, feu, vein=True)
        else:
            # céleri : pied de côtes charnues vert pâle, feuillage découpé au sommet
            for dx in (-3, -1, 1, 3):
                m = capsule(cx + dx * 0.4, SOL, cx + dx, SOL - h * 0.7, 1.3, 1)
                c.shape(m, R(pal[0], light=1.15), L_horiz(cx + dx - 1.5, cx + dx + 1.5, 0.9, 0.35))
            for dx in (-5, -2, 1, 4):
                leaf(c, cx + dx * 0.5, SOL - h * 0.7, cx + dx, SOL - h, 3.8, feu, vein=True)


def f_asperge(c, s, id, pal, feu):
    terre(c)
    xs = [8, 13, 18, 23]
    for k, x in enumerate(xs[: 2 if s == 0 else 4]):
        h = 4 + k % 2 if s == 0 else 10 + (k % 2) * 3 if s == 1 else 15 + (k % 2) * 4
        m = capsule(x, SOL, x, SOL - h, 1.2, 1.1)
        c.shape(m, R(pal[0]), L_horiz(x - 1, x + 1, 0.9, 0.3))
        c.shape(ellipse(x, SOL - h, 1.4, 2), R(pal[2]), L_const(0.5))
        for j in range(2, h - 2, 3):
            c.fill(rect(x - 1, SOL - j, x - 1, SOL - j), R(pal[0])[0])


def f_chardon(c, s, id, pal, feu):
    if s == 0:
        pousse(c, 15, 6, feu)
        return
    touffe(c, 16, 12 if s == 1 else 16, feu, n=7, w=5, spread=22, seed=6)
    if s == 2:
        tige(c, 16, SOL - 6, 16, SOL - 21, feu, 2)
        m = ellipse(16, SOL - 23, 4.2, 4)
        c.shape(m, R(pal[0]), L_sphere(15, SOL - 25, 4, 4), hl=1)
        for (x, y) in [(14, SOL - 24), (17, SOL - 22), (15, SOL - 21), (18, SOL - 25)]:
            c.fill(rect(x, y, x + 1, y), R(pal[0])[1])
        c.fill(rect(15, SOL - 27, 17, SOL - 27), pal[2])


def f_champignon(c, s, id, pal, feu):
    c.fill(rect(0, SOL - 1, 31, SOL), '#4a3a28')
    c.speckle(rect(0, SOL - 1, 31, SOL), '#6a5a3a', 0.4, seed=1)
    sizes = [(10, 1.5), (20, 1.2)] if s == 0 else [(8, 2.6), (16, 3.4), (24, 2.4)] if s == 1 else [(7, 3.8), (16, 5), (25, 3.6), (12, 2.4)]
    for (x, r) in sizes:
        h = r * 1.6 + 1
        c.shape(rect(int(x - r * 0.3), int(SOL - h), int(x + r * 0.3), SOL - 1), R(pal[1]), L_horiz(x - 1, x + 1, 0.9, 0.4))
        cap = ellipse(x, SOL - h, r + 0.5, r * 0.7) & (YY <= SOL - h + 0.5)
        c.shape(cap, R(pal[0], light=1.15), L_sphere(x - 1, SOL - h - 1, r, r * 0.7), hl=1)


def f_feuilles(c, s, id, pal, feu):
    if s == 0:
        pousse(c, 11, 5, feu); pousse(c, 21, 5, feu)
        return
    touffe(c, 11, 10 if s == 1 else 14, feu, n=5, w=5.5, spread=10, seed=7)
    touffe(c, 22, 9 if s == 1 else 13, R(feu)[3], n=5, w=5.5, spread=10, seed=8)


def f_herbe(c, s, id, pal, feu):
    if s == 0:
        pousse(c, 11, 5, feu, fine=id in ('ciboulette',)); pousse(c, 21, 5, feu, fine=id in ('ciboulette',))
        return
    h = 11 if s == 1 else 16
    if id == 'ciboulette':
        for x in range(7, 26, 3):
            tige(c, x, SOL, x + (x % 5 - 2), SOL - h - (x % 3), feu)
        if s == 2:
            for x in (9, 16, 23):
                fruit(c, x + (x % 5 - 2), SOL - h - 2, 1.6, especes.FLEURS['ciboulette'], hl=0)
        return
    if id in ('thym', 'romarin', 'estragon', 'aneth'):
        for x in (8, 13, 18, 23):
            top = SOL - h - (x % 3)
            tige(c, x, SOL, x + (x % 4 - 1.5), top, R(feu)[0] if id != 'aneth' else feu)
            for j in range(3, h, 2):
                px = x + (x % 4 - 1.5) * j / h
                c.fill(rect(int(px) - 1, SOL - j, int(px) + 1, SOL - j), feu if id != 'aneth' else R(feu)[3])
        if s == 2 and id in especes.FLEURS:
            for x in (9, 17, 24):
                fleur(c, x, SOL - h - 1, especes.FLEURS[id], r=1)
        return
    # herbes à feuilles larges : basilic, persil, menthe, coriandre
    w = {'basilic': 4.2, 'menthe': 3.8, 'persil': 3, 'coriandre': 3.2}.get(id, 3.4)
    for cx in (10, 22):
        dome(c, cx, h, 7, feu, leafw=w, n=14 if s == 1 else 20, seed=cx, veine=id in ('basilic', 'menthe'))
    if s == 2 and id == 'basilic':
        for cx in (10, 22):
            for j in range(3):
                c.fill(rect(cx, SOL - h - 2 - j * 2, cx, SOL - h - 2 - j * 2), '#f4f4f4')


def f_ananas(c, s, id, pal, feu):
    rs = rnd('ananas')
    n = 5 if s == 0 else 9
    h = 7 if s == 0 else 13 if s == 1 else 12
    for k in range(n):
        t = k / (n - 1) - 0.5
        leaf(c, 16, SOL - 1, 16 + t * 26, SOL - h + abs(t) * 8, 2.6, feu, vein=False)
    if s == 2:
        m = ellipse(16, SOL - 16, 4.5, 6)
        c.shape(m, R(pal[0], light=1.15), L_sphere(15, SOL - 18, 4.5, 6), hl=1)
        for y in range(SOL - 20, SOL - 11, 2):
            for x in range(12, 21, 2):
                if m[y, x]:
                    c.fill(rect(x + (y % 4 == 0), y, x + (y % 4 == 0), y), R(pal[0])[0])
        for k in range(5):
            leaf(c, 16, SOL - 21, 16 + (k - 2) * 2.5, SOL - 27, 1.8, pal[2], vein=False)


def f_fraisier(c, s, id, pal, feu):
    if s == 0:
        pousse(c, 11, 5, feu); pousse(c, 21, 5, feu)
        return
    for cx in (9, 22):
        for k in range(3):
            t = k - 1
            x1, y1 = cx + t * 5, SOL - 9 - (1 - abs(t)) * 2
            tige(c, cx, SOL, x1, y1 + 2, feu)
            m = ellipse(x1, y1, 2.6, 2.2)
            c.shape(m, R(feu), L_sphere(x1 - 1, y1 - 1, 2.6, 2.2))
            c.fill(line_mask(x1, y1 - 1, x1, y1 + 1) & m, R(feu)[0])
    if s == 1:
        for (x, y) in [(6, SOL - 5), (16, SOL - 6), (25, SOL - 5)]:
            fleur(c, x, y, '#fbfbf4', r=1.3)
    else:
        for (x, y) in [(5, SOL - 4), (13, SOL - 5), (18, SOL - 4), (26, SOL - 5)]:
            m = polygon([(x - 2, y - 2), (x + 2, y - 2), (x, y + 2.5)]) | ellipse(x, y - 1.5, 2, 1.2)
            c.shape(m, R(pal[0], light=1.25), L_sphere(x - 1, y - 2, 2.5, 2.5), hl=1)
            c.fill(rect(x - 1, y - 3, x + 1, y - 3), '#3a8a2a')


def f_baies(c, s, id, pal, feu):
    if s == 0:
        pousse(c, 15, 7, feu)
        return
    h = 15 if s == 1 else 21
    for x in (11, 16, 21):
        tige(c, 16, SOL, x, SOL - 6, '#6a4a2a')
    dome(c, 16, h, 12, feu, leafw=3, n=28 if s == 1 else 36, seed=5)
    rs = rnd('b' + id)
    fc = especes.FLEURS.get(id, '#f4f0f4')
    for k in range(9):
        a = rs.uniform(0.2, math.pi - 0.2)
        r = rs.uniform(0.3, 0.95)
        x, y = int(16 + math.cos(a) * 12 * r), int(SOL - 1 - math.sin(a) * h * r)
        if s == 1:
            fleur(c, x, y, fc, r=1)
        else:
            col = pal[0] if id != 'grains_de_cafe' else '#c82a1a'
            fruit(c, x, y, 1.5, col, hl=1)


def f_vigne(c, s, id, pal, feu):
    treillis(c)
    if s == 0:
        pousse(c, 15, 7, feu)
        return
    rs = rnd('v' + id)
    tige(c, 15, SOL, 13, 8, '#6a4a2a', 1)
    tige(c, 15, SOL - 10, 25, SOL - 14, '#6a4a2a', 1)
    tige(c, 14, SOL - 18, 6, SOL - 21, '#6a4a2a', 1)
    for k in range(10 if s == 1 else 14):
        x, y = rs.uniform(5, 26), rs.uniform(7, SOL - 6)
        m = ellipse(x, y, 2.8, 2.4)
        c.shape(m, R(feu), L_sphere(x - 1, y - 1, 3, 2.4))
        c.fill(line_mask(x, y + 1, x, y - 1) & m, R(feu)[0])
    if s == 2:
        for (x, y) in [(9, SOL - 11), (20, SOL - 18), (22, SOL - 9), (11, SOL - 22)]:
            if id == 'raisin':
                for (dx, dy) in [(0, 0), (-1.5, -1.5), (1.5, -1.5), (0, -3), (-1.5, 1.5), (1.5, 1.5), (0, 3)]:
                    fruit(c, x + dx, y + dy, 1.3, pal[0], hl=0)
            elif id == 'kiwi':
                fruit(c, x, y, 2, pal[0], hl=0, ry=2.4)
            elif id == 'vanille':
                c.shape(capsule(x, y - 3, x + 1, y + 4, 0.8), R('#6a8a2a'), L_const(0.6))
            else:  # poivre
                for j in range(5):
                    fruit(c, x, y - 3 + j * 1.6, 0.9, '#c8321a' if j % 2 else '#3a6a2a', hl=0)


def f_gousse(c, s, id, pal, feu):
    if s == 0:
        pousse(c, 11, 5, feu); pousse(c, 21, 5, feu)
        return
    h = 12 if s == 1 else 16
    for cx in (9, 16, 23):
        tige(c, cx, SOL, cx, SOL - h, feu)
        for k in range(3):
            y = SOL - 4 - k * 4
            sgn = 1 if (k + cx) % 2 else -1
            leaf(c, cx, y, cx + sgn * 4, y - 2, 2.8, feu, vein=False)
        if s == 2:
            fc = mix(pal[0], '#c8b070', 0.4) if id != 'soja' else '#9ab85a'
            c.shape(capsule(cx + 2, SOL - h + 3, cx + 3, SOL - h + 7, 1.2, 1), R(fc), L_const(0.6))
        elif id in especes.FLEURS:
            fleur(c, cx, SOL - h - 1, especes.FLEURS[id], r=1)


def f_rame(c, s, id, pal, feu):
    for x in (8, 23):
        tige(c, x, SOL, x + (4 if x < 16 else -4), 5, '#a07a4a', 1)
    if s == 0:
        pousse(c, 15, 7, feu)
        return
    h = 18 if s == 1 else 25
    for x0 in (9, 22):
        pts = [(x0, SOL)] + [(x0 + math.sin(k) * 2.5 + (2 if x0 < 16 else -2) * k / 4, SOL - k * h / 6) for k in range(1, 7)]
        for a, b in zip(pts, pts[1:]):
            tige(c, a[0], a[1], b[0], b[1], feu)
        for (x, y) in pts[1::2]:
            leaf(c, x, y, x + 4, y - 3, 3.4, feu, vein=False)
            leaf(c, x, y, x - 4, y - 2, 3.4, feu, vein=False)
    fc = especes.FLEURS.get(id, '#f4f4f0')
    if s == 1:
        for (x, y) in [(11, SOL - 12), (20, SOL - 16), (13, SOL - 20)]:
            fleur(c, x, y, fc, r=1)
        return
    pod = pal[0] if id in ('haricot_vert', 'petit_pois') else mix(pal[0], '#d8c890', 0.5)
    for (x, y) in [(11, SOL - 9), (20, SOL - 12), (13, SOL - 18), (19, SOL - 21), (12, SOL - 13)]:
        m = capsule(x, y, x + 1, y + 6, 1.1, 0.9)
        c.shape(m, R(pod, light=1.2), L_horiz(x - 1, x + 1, 0.9, 0.3), hl=1)


def f_canne(c, s, id, pal, feu):
    xs = [9, 15, 21] if s else [12, 19]
    h = 6 if s == 0 else 20 if s == 1 else 29
    for x in xs:
        m = rect(x - 1, SOL - h, x + 1, SOL)
        c.shape(m, R(pal[0]), L_horiz(x - 1, x + 1, 0.9, 0.3))
        for y in range(SOL - 3, SOL - h, -5):
            c.fill(rect(x - 1, y, x + 1, y), R(pal[0])[0])
        leaf(c, x, SOL - h + 2, x + 6, SOL - h - 2 + (x % 3), 2.4, feu, vein=False)
        leaf(c, x, SOL - h + 5, x - 6, SOL - h + 1, 2.4, feu, vein=False)


def f_fleur(c, s, id, pal, feu):
    if s == 0:
        pousse(c, 11, 5, feu, fine=id == 'safran'); pousse(c, 21, 5, feu, fine=id == 'safran')
        return
    h = 10 if s == 1 else 15
    fc = especes.FLEURS.get(id, '#f4f4f0')
    for cx in (8, 16, 24):
        if id == 'safran':
            for dx in (-1, 1):
                tige(c, cx + dx, SOL, cx + dx * 2, SOL - h + 3, feu)
            if s == 2:
                m = polygon([(cx - 3, SOL - h - 3), (cx + 3, SOL - h - 3), (cx + 2, SOL - h + 2), (cx - 2, SOL - h + 2)])
                c.shape(m, R(fc, light=1.2), L_horiz(cx - 3, cx + 3, 0.9, 0.3))
                c.fill(line_mask(cx, SOL - h - 1, cx - 1, SOL - h - 5) | line_mask(cx, SOL - h - 1, cx + 1, SOL - h - 5), '#e0301a')
            continue
        tige(c, cx, SOL, cx, SOL - h, feu)
        leaf(c, cx, SOL - 5, cx - 4, SOL - 8, 3, feu, vein=False)
        if s == 2:
            if id == 'sesame':
                for j in range(3):
                    c.shape(capsule(cx + 1, SOL - h + 3 + j * 3, cx + 2, SOL - h + 5 + j * 3, 0.9), R('#7a9a3a'), L_const(0.6))
                fleur(c, cx, SOL - h, fc, r=1.3)
            elif id == 'cumin':
                for dx in (-2, 0, 2):
                    tige(c, cx, SOL - h + 2, cx + dx, SOL - h - 1, feu)
                    c.fill(rect(cx + dx - 1, SOL - h - 2, cx + dx + 1, SOL - h - 2), fc)
            else:
                for (dx, dy) in [(0, 0), (-2, 1), (2, 1), (-1, -2), (1, -2)]:
                    fleur(c, cx + dx, SOL - h + dy, fc, r=0.9)


def f_arbuste(c, s, id, pal, feu):
    if s == 0:
        pousse(c, 15, 7, feu)
        return
    h = 15 if s == 1 else 23
    tige(c, 16, SOL, 16, SOL - 5, '#6a4a2a', 2)
    dome(c, 16, h, 12, feu, leafw=3.4 if id == 'laurier' else 2.8, n=26 if s == 1 else 40, seed=3, veine=True)
    if s == 2 and id == 'feuilles_de_the':
        rs = rnd('the')
        for k in range(7):
            x, y = rs.uniform(7, 25), rs.uniform(SOL - h, SOL - h + 5)
            leaf(c, x, y + 2, x + 1, y - 2, 2, mix(feu, '#b8f080', 0.55), vein=False)


FORMES = {
    'cereale': f_cereale, 'riz': f_riz, 'sarrasin': f_sarrasin, 'quinoa': f_quinoa, 'mais': f_mais,
    'racine': f_racine, 'tubercule': f_tubercule, 'bulbe': f_bulbe, 'tuteur': f_tuteur, 'courge': f_courge,
    'gros_fruit': f_gros_fruit, 'pomme': f_pomme, 'tige': f_tige, 'asperge': f_asperge, 'chardon': f_chardon,
    'champignon': f_champignon, 'feuilles': f_feuilles, 'herbe': f_herbe, 'ananas': f_ananas,
    'fraisier': f_fraisier, 'baies': f_baies, 'vigne': f_vigne, 'gousse': f_gousse, 'rame': f_rame,
    'canne': f_canne, 'fleur': f_fleur, 'arbuste': f_arbuste,
}


def culture(id, stade, pal):
    forme, _, _, _, _, feu = especes.CULTURES[id]
    c = Canvas()
    FORMES[forme](c, stade, id, pal, feu)
    return c.image()
