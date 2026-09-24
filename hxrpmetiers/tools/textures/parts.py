"""
Composants réutilisables : feuilles, tiges, grains, contenants (assiette, bol, verre, bocal,
bouteille, sac, cocotte, plat à gratin…) et aliments cuisinés (steak, frites, riz, pain…).
Chaque fonction dessine sur un Canvas ; le contour final est posé par le générateur.
"""
import math
import numpy as np
from pix import *

REG = {}      # id -> fonction de dessin
PAL = {}      # id -> palette/famille pour les mini-jeux


def item(*ids):
    def deco(fn):
        for i in ids:
            REG[i] = fn
        return fn
    return deco


def pal(id, famille, peau, chair, accent, motif=0):
    """Palette exportée dans food.json pour dessiner l'aliment dans les mini-jeux.
    motif : 0 plein, 1 cœur, 2 anneaux, 3 pépins, 4 quartiers, 5 marbré, 6 noyau, 7 alvéoles, 8 feuilles."""
    PAL[id] = {"famille": famille, "pal": [tohex(col(peau)), tohex(col(chair)), tohex(col(accent))], "motif": motif}


# ============================================================================ végétal
def leaf(c, x0, y0, x1, y1, w, color, vein=True, L=None, edge=True):
    """Feuille en amande de (x0,y0) à (x1,y1), largeur max w."""
    dx, dy = x1 - x0, y1 - y0
    Lg = math.hypot(dx, dy) or 1
    ux, uy = dx / Lg, dy / Lg
    t = np.clip(((XX - x0) * ux + (YY - y0) * uy) / Lg, 0, 1)
    along = (XX - x0) * ux + (YY - y0) * uy
    across = np.abs(-(XX - x0) * uy + (YY - y0) * ux)
    half = w / 2 * np.sin(np.pi * t) ** 0.8
    m = (along >= 0) & (along <= Lg) & (across <= half + 0.15)
    rr = R(color)
    c.shape(m, rr, L if L is not None else L_cyl(x0, y0, x1, y1, w / 2 + 0.5), edge=edge)
    if vein and Lg > 5:
        c.fill(line_mask(x0 + ux, y0 + uy, x1 - ux * 2, y1 - uy * 2), rr[1], clip=m & ~edge_of(m))
    return m


def edge_of(m):
    return edge(m)


def stem(c, pts, color, width=1):
    rr = R(color)
    for i in range(len(pts) - 1):
        (a, b), (e, f) = pts[i], pts[i + 1]
        c.fill(line_mask(a, b, e, f, width), rr[2])


def sprig(c, x0, y0, x1, y1, color, n=4, lw=5, ll=5, stem_col=None, spread=50, seed=0, vein=False):
    """Brin : tige + feuilles alternées."""
    stem(c, [(x0, y0), (x1, y1)], stem_col or mix(color, '#6a5a2a', 0.35))
    dx, dy = x1 - x0, y1 - y0
    base = math.atan2(dy, dx)
    for k in range(n):
        t = 0.25 + 0.75 * k / max(1, n - 1)
        px, py = x0 + dx * t, y0 + dy * t
        side = 1 if k % 2 == 0 else -1
        a = base + side * math.radians(spread)
        lx, ly = px + math.cos(a) * ll, py + math.sin(a) * ll
        leaf(c, px, py, lx, ly, lw, color, vein=vein)
    leaf(c, x1 - dx * 0.08, y1 - dy * 0.08, x1 + math.cos(base) * ll * 0.9, y1 + math.sin(base) * ll * 0.9, lw, color, vein=vein)


def grains(c, mask, color, n, seed=1, size=(2, 1), rot=True, edge=False, rmp=None):
    """Petits grains posés dans un masque (riz, lentilles, graines…)."""
    rs = np.random.RandomState(seed)
    rr = rmp or R(color)
    pts = rng_pts(seed, mask, n)
    for (x, y) in pts:
        w, h = size
        if rot and rs.rand() < 0.5:
            w, h = h, w
        m = rect(x, y, x + w - 1, y + h - 1) & mask
        k = rs.randint(1, len(rr))
        c.fill(m, rr[k])
        if w * h > 1:
            c.fill(rect(x, y, x, y) & mask, rr[min(len(rr) - 1, k + 1)])


def dots(c, mask, color, n, seed=3):
    for (x, y) in rng_pts(seed, mask & ~edge(mask), n):
        c.fill(rect(x, y, x, y), color)


def herbs(c, x, y, color='#3f9a2a', seed=0, n=3):
    """Petite garniture de persil / herbes hachées."""
    rs = np.random.RandomState(seed)
    rr = R(color)
    for k in range(n):
        xx, yy = x + rs.randint(-2, 3), y + rs.randint(-1, 2)
        c.fill(rect(xx, yy, xx + 1, yy), rr[3])
        c.fill(rect(xx, yy + 1, xx, yy + 1), rr[1])


# ============================================================================ contenants
def plate(c, cx=16, cy=19.5, rx=14.5, ry=9, color='#f1ece2', rim=2.6):
    """Assiette en vue trois-quarts. Retourne le masque du creux."""
    rr = R(color, dark=0.7)
    c.shape(ellipse(cx, cy + 1.2, rx, ry), R(mix(color, '#8a8278', 0.45)), L_const(0.3), edge=True)
    c.shape(ellipse(cx, cy, rx, ry), rr, L_vert(cy - ry, cy + ry, 0.95, 0.5))
    well = ellipse(cx, cy + 0.4, rx - rim, ry - rim * 0.66)
    c.shape(well, [rr[2], rr[3], rr[3]], L_vert(cy - ry, cy + ry, 0.2, 0.9), edge=False, dither=False)
    c.fill(arc_band(cx, cy + 0.4, rx - rim, ry - rim * 0.66, 1, 200, 340), rr[1])
    return well


def board(c, x0=2, y0=14, x1=29, y1=27, color='#c58a4a'):
    """Planche en bois épaisse (vue trois-quarts)."""
    rr = R(color)
    c.shape(rrect(x0, y0 + 2, x1, y1, 1), R(mix(color, '#5a3418', 0.45)), L_const(0.4))
    top = rrect(x0, y0, x1, y1 - 2, 1)
    c.shape(top, rr, L_vert(y0, y1, 0.85, 0.5))
    for k in range(3):
        yy = y0 + 2 + k * 3
        c.fill(line_mask(x0 + 3 + k * 2, yy, x0 + 9 + k * 3, yy), rr[1], clip=top & ~edge(top))
    return top


def bowl(c, cx=16, cy=15, rx=13, ry=5, depth=10, color='#f2eee6', inside=None, foot=True):
    """Bol vu de trois-quarts. Retourne le masque de l'ouverture (pour poser le contenu).
    Le contenu doit être dessiné juste après, puis bowl_rim()."""
    rr = R(color, dark=0.62)
    body = ellipse(cx, cy, rx, ry) | (ellipse(cx, cy, rx, ry + depth) & (YY >= cy))
    if foot:
        body |= rect(int(cx - rx * 0.45), int(cy + ry + depth - 2), int(cx + rx * 0.45), int(cy + ry + depth - 1)) & (YY <= cy + ry + depth)
    c.shape(body, rr, L_sphere(cx - 2, cy + depth * 0.25, rx, depth + ry, 0.25))
    opening = ellipse(cx, cy, rx - 1.2, ry - 0.9)
    c.shape(opening, [rr[1], rr[2], rr[2]], L_vert(cy - ry, cy + ry, 0.1, 0.9), edge=False, dither=False)
    return opening


def bowl_rim(c, cx=16, cy=15, rx=13, ry=5, color='#f2eee6'):
    rr = R(color, dark=0.62)
    band = arc_band(cx, cy, rx, ry, 1.3)
    c.fill(band & (YY < cy), rr[4])
    c.fill(band & (YY >= cy), rr[3])


def fill_bowl(c, opening, color, level=0.6, cx=16, cy=15, rx=13, ry=5, L=None, edge=False):
    """Remplit l'ouverture d'un bol (liquide ou purée)."""
    m = opening & ellipse(cx, cy + (1 - level) * 1.2, rx - 1.6, ry - 1.2)
    c.shape(m, R(color), L if L is not None else L_vert(cy - ry, cy + ry, 0.85, 0.45), edge=edge)
    return m


def glass(c, x0, x1, y0, y1, liquid, level=0.8, taper=1.5, glass_col='#dcecf2', foam=None, ice=False):
    """Verre droit légèrement évasé avec liquide. Retourne (masque du verre, niveau y)."""
    pts = [(x0 - taper, y0), (x1 + taper, y0), (x1, y1), (x0, y1)]
    body = polygon(pts)
    gr = R(glass_col)
    c.shape(body, [gr[1], gr[2], gr[3]], L_horiz(x0, x1, 0.9, 0.2), edge=True)
    ly = y1 - (y1 - y0) * level
    liq = body & (YY >= ly) & ~edge(body)
    lr = R(liquid)
    c.shape(liq, lr, L_horiz(x0, x1, 0.8, 0.3), edge=False)
    c.fill(liq & (YY < ly + 1), lr[4])
    if foam:
        fm = body & (YY >= ly - 1.5) & (YY < ly + 1) & ~edge(body)
        c.shape(fm, R(foam), L_const(0.8), edge=False)
    if ice:
        for (ix, iy) in ((x0 + 2, ly + 1), (x1 - 5, ly + 2.5)):
            c.shape(rect(int(ix), int(iy), int(ix) + 2, int(iy) + 2) & liq, R('#eaf6fb'), L_const(0.75), edge=False)
    # reflets verticaux
    c.fill(line_mask(x0 + 0.5, y0 + 2, x0 + 1.2, y1 - 2) & body & ~edge(body), '#ffffff')
    c.fill(line_mask(x1 - 1.5, y0 + 3, x1 - 1.5, y0 + 5) & body & ~edge(body), '#f4fbff')
    c.fill(line_mask(x0 - taper + 1, y0, x1 + taper - 1, y0) & body, gr[4])
    return body, ly


def straw(c, x0, y0, x1, y1, color='#e8403a', stripe='#ffffff'):
    m = line_mask(x0, y0, x1, y1, 2)
    c.shape(m, R(color), L_const(0.6), edge=True)
    pts = np.argwhere(m)
    for i, (y, x) in enumerate(pts):
        if (x + y) % 4 == 0:
            c.fill(rect(x, y, x, y), stripe)


def mug(c, x0=7, x1=21, y0=10, y1=26, color='#f0ebe2', liquid='#4a2a18', handle=True, saucer=True):
    if saucer:
        c.shape(ellipse(15, y1, 12, 3.4), R('#ece6da'), L_vert(y1 - 3, y1 + 3, 0.9, 0.5))
    if handle:
        hm = arc_band(x1 + 1, (y0 + y1) / 2, 4.5, 4.5, 2, 270, 90)
        c.shape(hm, R(color), L_const(0.6))
    body = rrect(x0, y0, x1, y1 - 1, 2) | ellipse((x0 + x1) / 2, y0, (x1 - x0) / 2 + 0.3, 2.2)
    c.shape(body, R(color), L_horiz(x0, x1, 0.95, 0.35), hl=0)
    top = ellipse((x0 + x1) / 2, y0, (x1 - x0) / 2 - 1, 1.6)
    c.shape(top, R(liquid), L_const(0.5), edge=False)
    return top


def jar(c, x0=7, x1=24, y0=9, y1=28, content='#d04030', lid='#c8a040', label=None, level=0.85, glass_col='#d8e8ee', lid_h=4):
    """Bocal en verre avec couvercle."""
    body = rrect(x0, y0 + lid_h - 1, x1, y1, 2)
    gr = R(glass_col)
    c.shape(body, [gr[1], gr[2], gr[3]], L_horiz(x0, x1, 0.8, 0.3))
    ly = y1 - (y1 - y0 - lid_h) * level
    inner = body & ~edge(body) & (YY >= ly)
    c.shape(inner, R(content), L_horiz(x0, x1, 0.85, 0.3), edge=False)
    c.fill(inner & (YY < ly + 1), R(content)[4])
    if label:
        lm = rect(x0 + 1, int((y0 + y1) / 2) + 1, x1 - 1, int((y0 + y1) / 2) + 5)
        c.shape(lm, R(label), L_const(0.7), edge=True)
    c.fill(line_mask(x0 + 1.5, y0 + lid_h + 1, x0 + 1.5, y1 - 2) & body & ~edge(body), '#ffffff')
    lm = rrect(x0 - 1, y0, x1 + 1, y0 + lid_h - 1, 1)
    c.shape(lm, R(lid), L_vert(y0, y0 + lid_h, 0.9, 0.4), hl=1)
    for k in range(x0 + 1, x1, 2):
        c.fill(rect(k, y0 + lid_h - 2, k, y0 + lid_h - 2), R(lid)[1])
    return body


def bottle(c, cx=16, y0=3, y1=29, w=10, neck=4, shoulder=11, color='#3a7a2a', liquid=None, cap='#c8a040', label=None,
           cap_h=3, glass=True):
    """Bouteille : corps, épaules arrondies, goulot, bouchon."""
    x0, x1 = cx - w / 2, cx + w / 2
    body = rrect(int(x0), int(y0 + shoulder), int(x1) - 1, y1, 2)
    sh = ellipse(cx, y0 + shoulder + 0.5, w / 2, 3.5) & (YY <= y0 + shoulder + 1)
    nk = rect(int(cx - neck / 2), y0 + cap_h, int(cx + neck / 2) - 1, int(y0 + shoulder))
    whole = body | sh | nk
    rr = R(color)
    c.shape(whole, rr, L_horiz(x0, x1, 0.95, 0.25), hl=0)
    if liquid:
        lq = (body & ~edge(body))
        c.shape(lq, R(liquid), L_horiz(x0, x1, 0.8, 0.25), edge=False)
    if label:
        lm = rect(int(x0) + 1, int(y0 + shoulder + 4), int(x1) - 2, int(y0 + shoulder + 10))
        c.shape(lm, R(label), L_const(0.7), edge=True)
    if glass:
        c.fill(line_mask(x0 + 1.5, y0 + shoulder + 1, x0 + 1.5, y1 - 2) & whole & ~edge(whole), mix(rr[4], '#ffffff', 0.6))
    cm = rect(int(cx - neck / 2) - 1, y0, int(cx + neck / 2), y0 + cap_h - 1)
    c.shape(cm, R(cap), L_vert(y0, y0 + cap_h, 0.9, 0.4))
    return whole


def sack(c, color='#d8c49a', content='#f4f0e6', x0=5, x1=26, y0=9, y1=29, grains_col=None, n=40, seed=5):
    """Sac en toile ouvert, contenu visible en haut."""
    rr = R(color)
    body = polygon([(x0 + 2, y0 + 3), (x1 - 2, y0 + 3), (x1 + 1, y1 - 4), (x1 - 1, y1), (x0 + 1, y1), (x0 - 1, y1 - 4)])
    c.shape(body, rr, L_sphere(15, 18, 14, 12, 0.5))
    # ourlet
    lip = ellipse((x0 + x1) / 2, y0 + 3.5, (x1 - x0) / 2 - 0.5, 3)
    c.shape(lip, rr, L_vert(y0, y0 + 6, 0.9, 0.4))
    top = ellipse((x0 + x1) / 2, y0 + 3, (x1 - x0) / 2 - 2.5, 2.2) | ellipse((x0 + x1) / 2, y0 + 2, (x1 - x0) / 2 - 5, 2.5)
    c.shape(top, R(content), L_sphere((x0 + x1) / 2 - 2, y0, 9, 4, 0.3), edge=False)
    if grains_col:
        grains(c, top, grains_col, n, seed=seed)
    # coutures
    for yy in range(y0 + 8, y1 - 1, 3):
        c.fill(rect(x0 + 1, yy, x0 + 1, yy), rr[1])
        c.fill(rect(x1 - 2, yy, x1 - 2, yy), rr[1])
    return body


def spoon(c, x0, y0, x1, y1, color='#b98a52', bowl_r=3.5, content=None, seed=1):
    """Cuillère en bois avec éventuellement un contenu (graines, poudre)."""
    stem(c, [(x0, y0), (x1, y1)], color, width=2)
    m = ellipse(x1, y1, bowl_r + 0.5, bowl_r * 0.75)
    c.shape(m, R(color), L_sphere(x1, y1, bowl_r, bowl_r, 0.3))
    if content:
        inner = ellipse(x1, y1 - 0.5, bowl_r - 0.6, bowl_r * 0.6)
        c.shape(inner, R(content), L_sphere(x1 - 1, y1 - 1, bowl_r, bowl_r * 0.7, 0.2), edge=False)
        return inner
    return m


def pile(c, cx, cy, rx, ry, color, seed=1, grain=None, n=0, gsize=(1, 1)):
    """Petit tas conique (poudre, épice, farine)."""
    m = ellipse(cx, cy, rx, ry) & (YY >= cy - ry) | (ellipse(cx, cy + ry * 0.3, rx, ry * 0.7))
    c.shape(m, R(color), L_sphere(cx - 1, cy - 1, rx, ry, 0.2))
    if grain:
        grains(c, m & ~edge(m), grain, n, seed=seed, size=gsize)
    return m


def gratin_dish(c, content, x0=3, x1=28, y0=12, y1=27, color='#e8e0d0', crust=None, seed=2):
    """Plat à gratin rectangulaire (vue trois-quarts) avec contenu gratiné."""
    rr = R(color, dark=0.62)
    outer = rrect(x0, y0, x1, y1, 3)
    c.shape(outer, rr, L_vert(y0, y1, 0.9, 0.4))
    # oreilles
    c.shape(rrect(x0 - 1, y0 + 3, x0 + 1, y0 + 6, 1) | rrect(x1 - 1, y0 + 3, x1 + 1, y0 + 6, 1), rr, L_const(0.7))
    inner = rrect(x0 + 2, y0 + 2, x1 - 2, y1 - 5, 2)
    c.shape(inner, R(content), L_sphere((x0 + x1) / 2 - 3, y0 + 3, (x1 - x0) / 2, (y1 - y0) / 1.5, 0.6), edge=False)
    if crust:
        cm = inner & ~edge(inner)
        rs = np.random.RandomState(seed)
        for (x, y) in rng_pts(seed, cm, 26):
            c.fill(rect(x, y, x + rs.randint(0, 2), y), R(crust)[rs.randint(1, 4)])
    c.fill(rect(x0 + 2, y1 - 3, x1 - 2, y1 - 3), rr[4])
    return inner


def cocotte(c, color='#2b3a52', content=None, lid=False, x0=4, x1=27, top=13, bottom=28):
    """Cocotte en fonte avec anses."""
    rr = R(color, dark=0.55)
    cx = (x0 + x1) / 2
    rx = (x1 - x0) / 2
    c.shape(rrect(x0 - 3, top + 2, x0, top + 5, 1) | rrect(x1, top + 2, x1 + 3, top + 5, 1), rr, L_const(0.6))
    body = (ellipse(cx, top, rx, 4) | rrect(x0, top, x1, bottom - 1, 3) | ellipse(cx, bottom - 3, rx, 3))
    c.shape(body, rr, L_horiz(x0, x1, 0.95, 0.25), hl=2)
    if lid:
        lm = ellipse(cx, top - 1, rx + 1, 4.5)
        c.shape(lm, rr, L_sphere(cx - 3, top - 3, rx, 5, 0.3), hl=2)
        c.shape(rrect(int(cx) - 2, top - 7, int(cx) + 1, top - 4, 1), R('#b08a40'), L_const(0.7))
        return None
    opening = ellipse(cx, top, rx - 1.3, 3)
    if content:
        c.shape(opening, R(content), L_vert(top - 3, top + 3, 0.9, 0.5), edge=False)
    c.fill(arc_band(cx, top, rx, 4, 1.2) & (YY < top), rr[3])
    return opening


def ramekin(c, color='#f2eee6', content='#e8c070', x0=6, x1=25, top=14, bottom=26, crust=None):
    rr = R(color, dark=0.62)
    cx = (x0 + x1) / 2
    rx = (x1 - x0) / 2
    body = ellipse(cx, top, rx, 4) | rect(x0, top, x1, bottom - 2) | ellipse(cx, bottom - 2, rx, 2.5)
    c.shape(body, rr, L_horiz(x0, x1, 0.95, 0.35))
    for xx in range(x0 + 2, x1, 3):
        c.fill(line_mask(xx, top + 3, xx, bottom - 2) & body & ~edge(body), rr[2])
    op = ellipse(cx, top, rx - 1.2, 3)
    c.shape(op, R(content), L_sphere(cx - 2, top - 2, rx, 4, 0.6), edge=False, hl=2)
    if crust:
        c.speckle(op, crust, 0.25, seed=4)
    return op


# ============================================================================ aliments cuisinés
def steak(c, cx, cy, rx=7.5, ry=4.5, color='#7a3a1c', marks=True, seed=0):
    c.shape(ellipse(cx + 0.5, cy + 1.3, rx, ry), R(mix(color, '#2a1208', 0.55)), L_const(0.3))
    m = ellipse(cx, cy, rx, ry)
    c.shape(m, R(color), L_sphere(cx - 2, cy - 2, rx, ry, 0.8), hl=2)
    if marks:
        for k in range(-1, 2):
            x = cx + k * rx * 0.55
            c.fill(line_mask(x - 2, cy + ry * 0.5, x + 1.5, cy - ry * 0.55), mix(color, '#1a0804', 0.6), clip=m & ~edge(m))
    return m


def fry_stick(c, x0, y0, x1, y1, color='#f0c040'):
    """Une frite : bâtonnet de 2 px, clair dessus, ombré dessous, sans gros contour."""
    rr = R(color)
    m = line_mask(x0, y0, x1, y1, 2)
    c.fill(m, rr[3])
    c.fill(m & ~shift(m, 0, -1), rr[1])
    c.fill(m & ~shift(m, 0, 1) & ~(m & ~shift(m, 0, -1)), rr[4])
    c.fill(edge(m) & ~shift(m, 1, 0) & (YY > (y0 + y1) / 2), rr[2])
    return m


def fries(c, x0, y0, n=6, color='#f0c040', seed=2, length=9):
    rs = np.random.RandomState(seed)
    for k in range(n):
        x = x0 + k * 2 + rs.randint(0, 2)
        a = math.radians(rs.randint(-30, 30))
        x1, y1 = x + math.sin(a) * length, y0 - math.cos(a) * length
        fry_stick(c, x, y0, x1, y1, color)


def fries_pile(c, cx, cy, color='#f0c040', seed=3, n=9):
    rs = np.random.RandomState(seed)
    for k in range(n):
        x = cx + rs.randint(-5, 5)
        y = cy + rs.randint(-3, 3)
        a = rs.uniform(-0.6, 0.6) + (math.pi / 2 if k % 3 == 0 else 0)
        L = rs.randint(5, 8)
        x1, y1 = x + math.cos(a) * L, y + math.sin(a) * L * 0.55
        fry_stick(c, x, y, x1, y1, color)


def rice_mound(c, cx, cy, rx=6, ry=4, color='#f6f3ea', seed=4):
    m = ellipse(cx, cy, rx, ry)
    c.shape(m, R(color, dark=0.8), L_sphere(cx - 1, cy - 1, rx, ry, 0.3))
    for (x, y) in rng_pts(seed, m & ~edge(m), int(rx * ry * 0.5)):
        c.fill(rect(x, y, x + 1, y), '#ffffff')
        c.fill(rect(x + 1, y + 1, x + 1, y + 1), R(color, dark=0.8)[1])
    return m


def mash(c, cx, cy, rx=6, ry=4, color='#f0dca0', seed=5):
    m = ellipse(cx, cy, rx, ry)
    c.shape(m, R(color), L_sphere(cx - 1, cy - 1.5, rx, ry, 0.2), hl=1)
    for k in range(3):
        c.fill(arc_band(cx, cy - k, rx * (0.8 - k * 0.2), ry * (0.6 - k * 0.15), 1, 200, 320), R(color)[1], clip=m & ~edge(m))
    return m


def sauce_pool(c, cx, cy, rx, ry, color, seed=1):
    m = ellipse(cx, cy, rx, ry)
    c.shape(m, R(color), L_vert(cy - ry, cy + ry, 0.8, 0.4), edge=True, hl=2)
    return m


def blob(c, cx, cy, rx, ry, color, hl=1, edge=True, flat=0.3):
    m = ellipse(cx, cy, rx, ry)
    c.shape(m, R(color), L_sphere(cx - rx * 0.3, cy - ry * 0.3, rx, ry, flat), hl=hl, edge=edge)
    return m


def pea(c, x, y, color='#6cc03a'):
    m = circle(x, y, 1.3)
    c.fill(m, R(color)[2])
    c.fill(rect(int(x) - 1, int(y) - 1, int(x) - 1, int(y) - 1), R(color)[4])


def veg_bits(c, mask, colors, n=8, seed=7, size=2):
    rs = np.random.RandomState(seed)
    for i, (x, y) in enumerate(rng_pts(seed, mask & ~edge(mask), n)):
        cc = colors[i % len(colors)]
        rr = R(cc)
        m = rect(x, y, x + size - 1, y + size - 1) & mask
        c.fill(m, rr[3])
        c.fill(rect(x + size - 1, y + size - 1, x + size - 1, y + size - 1) & mask, rr[1])


def bun_bottom(c, cx, y, rx=11, color='#d89040'):
    m = rrect(int(cx - rx), y, int(cx + rx), y + 3, 1)
    c.shape(m, R(color), L_vert(y, y + 4, 0.8, 0.3))
    return m


def bun_top(c, cx, y, rx=11, ry=6, color='#d89040', sesame=True, seed=3):
    m = ellipse(cx, y, rx, ry) & (YY <= y + 1.5)
    c.shape(m, R(color), L_sphere(cx - 3, y - 3, rx, ry, 0.2), hl=3)
    if sesame:
        for (x, yy) in rng_pts(seed, m & ~edge(m) & (YY < y - 1), 7):
            c.fill(rect(x, yy, x + 1, yy), '#f8ecc8')
    return m


def patty(c, cx, y, rx=11, color='#5a2c16'):
    m = rrect(int(cx - rx), y, int(cx + rx), y + 3, 1)
    c.shape(m, R(color), L_vert(y, y + 3, 0.7, 0.3))
    c.speckle(m, mix(color, '#2a1008', 0.5), 0.2, seed=6)
    return m


def cheese_slice(c, cx, y, rx=11, color='#f6c83a'):
    m = rect(int(cx - rx), y, int(cx + rx), y + 1)
    m |= polygon([(cx - 4, y + 1), (cx - 1, y + 1), (cx - 2.5, y + 4)])
    m |= polygon([(cx + 4, y + 1), (cx + 7, y + 1), (cx + 5.5, y + 3.5)])
    c.shape(m, R(color), L_const(0.7))


def lettuce(c, cx, y, rx=12, color='#6cc03a', seed=4):
    rs = np.random.RandomState(seed)
    m = np.zeros((H, W), bool)
    for x in range(int(cx - rx), int(cx + rx) + 1):
        h = 1 + (1 if (x + seed) % 3 == 0 else 0) + (1 if rs.rand() < 0.3 else 0)
        m |= rect(x, y, x, y + h)
    c.shape(m, R(color), L_vert(y, y + 3, 0.9, 0.4))


def tomato_slice_side(c, cx, y, rx=10, color='#e0321f'):
    m = rrect(int(cx - rx), y, int(cx + rx), y + 1, 0)
    c.shape(m, R(color), L_const(0.6))
    return m


def slice_round(c, cx, cy, r, skin, flesh, motif='seeds', ry=None):
    """Rondelle vue de face (tomate, citron, concombre…)."""
    ry = ry or r
    m = ellipse(cx, cy, r, ry)
    c.shape(m, R(skin), L_const(0.55))
    inner = ellipse(cx, cy, r - 1, ry - 1)
    c.shape(inner, R(flesh), L_sphere(cx - 1, cy - 1, r, ry, 0.8), edge=False)
    if motif == 'citrus':
        for a in range(0, 360, 60):
            x2 = cx + math.cos(math.radians(a)) * (r - 1.2)
            y2 = cy + math.sin(math.radians(a)) * (ry - 1.2)
            c.fill(line_mask(cx, cy, x2, y2), mix(flesh, '#ffffff', 0.5), clip=inner)
    elif motif == 'seeds':
        for a in range(0, 360, 90):
            x2 = cx + math.cos(math.radians(a + 45)) * (r * 0.45)
            y2 = cy + math.sin(math.radians(a + 45)) * (ry * 0.45)
            c.fill(rect(int(x2), int(y2), int(x2), int(y2)), '#f6e8a0')
    elif motif == 'ring':
        c.fill(arc_band(cx, cy, r * 0.55, ry * 0.55, 1), mix(flesh, '#ffffff', 0.4), clip=inner)
    return m


def fish_body(c, x0, y0, x1, y1, h, back, belly, fin=None, eye=True, spots=None, stripe=None, seed=1, tail=True):
    """Poisson couché de (x0,y0) tête à (x1,y1) queue, hauteur h."""
    dx, dy = x1 - x0, y1 - y0
    Lg = math.hypot(dx, dy)
    ux, uy = dx / Lg, dy / Lg
    along = (XX - x0) * ux + (YY - y0) * uy
    across = -(XX - x0) * uy + (YY - y0) * ux
    t = np.clip(along / Lg, 0, 1)
    prof = h / 2 * np.where(t < 0.3, np.sqrt(np.clip(t / 0.3, 0, 1)) * 0.95 + 0.05, 1 - (t - 0.3) / 0.7 * 0.78)
    body = (along >= 0) & (along <= Lg) & (np.abs(across) <= prof)
    if tail:
        tx, ty = x1 + ux * 0.5, y1 + uy * 0.5
        tl = polygon([(tx - ux, ty - uy), (tx + ux * 4 - uy * 4, ty + uy * 4 + ux * 4), (tx + ux * 2.5, ty + uy * 2.5),
                      (tx + ux * 4 + uy * 4, ty + uy * 4 - ux * 4)])
        c.shape(tl, R(fin or back), L_const(0.55))
    top = across < 0
    L = L_cyl(x0, y0, x1, y1, h / 2)
    c.shape(body, R(belly), L, hl=2)
    c.shape(body & (across < -prof * 0.15), R(back), L, edge=False)
    c.fill(edge(body), R(back)[0])
    if stripe:
        c.fill(body & (np.abs(across + prof * 0.05) < 0.8) & ~edge(body) & (t > 0.2) & (t < 0.95), stripe)
    if spots:
        dots(c, body & (across < 0) & (t > 0.2), spots, 8, seed=seed)
    if eye:
        ex, ey = x0 + ux * 3.2 - uy * (-h * 0.12), y0 + uy * 3.2 + ux * (-h * 0.12)
        c.fill(rect(int(ex), int(ey), int(ex), int(ey)), '#101010')
        c.fill(rect(int(ex) - 1, int(ey) - 1, int(ex) - 1, int(ey) - 1), '#ffffff')
        gx, gy = x0 + ux * 5.5, y0 + uy * 5.5
        c.fill(line_mask(gx - uy * h * 0.3, gy + ux * h * 0.3, gx + uy * h * 0.3 * 0.2, gy - ux * h * 0.3 * 0.2) & body & ~edge(body), R(belly)[1])
    return body


def pizza_top(c, cx=16, cy=17, rx=13.5, ry=10, sauce='#c83a22', cheese='#f4d070', crust='#d8983e', cut=True):
    """Pizza vue de trois-quarts, une part décalée."""
    c.shape(ellipse(cx, cy + 1, rx, ry), R(mix(crust, '#6a3a14', 0.35)), L_const(0.4))
    base = ellipse(cx, cy, rx, ry)
    c.shape(base, R(crust), L_sphere(cx - 3, cy - 3, rx, ry, 0.4), hl=2)
    inner = ellipse(cx, cy, rx - 2, ry - 1.6)
    c.shape(inner, R(sauce), L_const(0.55), edge=True)
    ch = inner & ~edge(inner)
    rs = np.random.RandomState(7)
    cm = np.zeros((H, W), bool)
    for (x, y) in rng_pts(9, ch, 9):
        cm |= ellipse(x, y, rs.uniform(1.5, 3), rs.uniform(1.2, 2)) & ch
    c.shape(cm, R(cheese), L_const(0.7), edge=False)
    if cut:
        c.fill(line_mask(cx, cy, cx + rx, cy - 1) & base, R(crust)[0])
        c.fill(line_mask(cx, cy, cx + rx * 0.3, cy + ry) & base, R(crust)[0])
    return inner


def cake_slice(c, layers, top=None, x0=4, x1=27, y0=10, y1=26, frosting=None, deco=None):
    """Part de gâteau triangulaire vue de trois-quarts : face avant en couches, dessus clair."""
    # dessus (triangle) puis face avant (rectangle incliné)
    tip = (x1, y0 + 2)
    topm = polygon([(x0, y0 + 6), (x0 + 8, y0 + 1), tip, (x1 - 1, y0 + 4)])
    front_pts = [(x0, y0 + 6), (x1 - 1, y0 + 4), (x1 - 1, y1 - 4), (x0, y1)]
    front = polygon(front_pts)
    h = (y1 - (y0 + 6))
    acc = 0
    tot = sum(t for _, t in layers)
    for colr, th in layers:
        ya = y0 + 6 + h * acc / tot
        yb = y0 + 6 + h * (acc + th) / tot
        band = front & (YY >= ya - (XX - x0) * 2 / (x1 - x0) * 1) & (YY < yb - (XX - x0) * 2 / (x1 - x0) * 1 + 0.01)
        c.shape(band, R(colr), L_horiz(x0, x1, 0.8, 0.4), edge=False)
        acc += th
    c.fill(edge(front) & (XX > x0 + 0.9), R(layers[-1][0])[0])
    c.shape(topm, R(top or layers[0][0]), L_vert(y0, y0 + 6, 0.95, 0.6), hl=1)
    if deco:
        deco(c)
    return topm, front


def tart(c, filling, crust='#d8a050', cx=16, cy=17, rx=13.5, ry=9.5, deco=None):
    c.shape(ellipse(cx, cy + 2, rx, ry), R(mix(crust, '#6a3a14', 0.4)), L_const(0.3))
    base = ellipse(cx, cy, rx, ry)
    c.shape(base, R(crust), L_sphere(cx - 3, cy - 3, rx, ry, 0.4))
    for a in range(0, 360, 20):
        x = cx + math.cos(math.radians(a)) * (rx - 0.8)
        y = cy + math.sin(math.radians(a)) * (ry - 0.6)
        c.fill(rect(int(x), int(y), int(x), int(y)), R(crust)[1])
    inner = ellipse(cx, cy - 0.3, rx - 2.2, ry - 1.8)
    c.shape(inner, R(filling), L_sphere(cx - 3, cy - 3, rx, ry, 0.7), edge=True, hl=2)
    if deco:
        deco(c, inner)
    return inner


def cup_dessert(c, content, x0=8, x1=23, y0=9, y1=26, glass_col='#dcecf2', top=None):
    """Verrine / coupe en verre."""
    body, ly = glass(c, x0, x1, y0, y1, content, level=0.85, taper=1.2, glass_col=glass_col)
    return body, ly


def skewer(c, x0, y0, x1, y1, pieces, wood='#c8a070'):
    stem(c, [(x0, y0), (x1, y1)], wood, width=1)
    n = len(pieces)
    for k, pc in enumerate(pieces):
        t = 0.18 + 0.64 * k / max(1, n - 1)
        x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
        blob(c, x, y, 2.8, 2.6, pc, hl=1)
