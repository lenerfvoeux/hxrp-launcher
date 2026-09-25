"""
Feuillages d'arbres fruitiers (3 états : sans fruit, en fleur ou fruits verts, fruits mûrs), pousses,
sachets de graines, et blocs du monde (minerai de sel, ruche sauvage, bancs de moules et d'huîtres).
"""
import math
import os
import sys
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, '..', 'textures'))
from parts import *  # noqa: E402,F401,F403
from pix import Canvas, mix  # noqa: E402
from PIL import Image  # noqa: E402

import especes  # noqa: E402


def rnd(key):
    return np.random.RandomState(zlib.crc32(key.encode()) & 0x7fffffff)


# ------------------------------------------------------------------------------ feuillages
def _feuilles_base(c, feu, key, palmier=False):
    """Masse de feuilles qui se raccorde en mosaïque, avec quelques trous (graphismes détaillés)."""
    rs = rnd(key)
    rr = R(feu)
    # fond sombre partout (le mode « rapide » affiche la texture sans transparence)
    c.fill(rect(0, 0, 31, 31), rr[0])
    if palmier:
        for k in range(7):
            y0 = k * 5 - 2
            c.fill(line_mask(-2, y0 + 6, 34, y0, 1), rr[1])
            for x in range(0, 32, 2):
                yy = y0 + 6 - (x + 2) * 6 / 36
                c.fill(line_mask(x, yy, x + 2, yy + 4, 1), rr[2 + (x // 2 + k) % 2])
        return
    for k in range(95):
        x, y = rs.randint(-2, 34), rs.randint(-2, 34)
        a = rs.uniform(0, 2 * math.pi)
        L = rs.uniform(3, 5)
        tone = rr[1 + rs.randint(0, 4)] if rs.rand() < 0.85 else rr[4]
        for dx in (-32, 0, 32):
            for dy in (-32, 0, 32):
                m = capsule(x + dx, y + dy, x + dx + math.cos(a) * L, y + dy + math.sin(a) * L, 1.5, 0.7)
                c.fill(m, tone)
    # quelques jours entre les feuilles
    for k in range(10):
        x, y = rs.randint(0, 31), rs.randint(0, 31)
        c.px[y, x, 3] = 0


def _fruit_arbre(c, x, y, taille, col, id, rs):
    if id == 'cerise':
        c.fill(line_mask(x, y - 3, x + 1, y - 5), '#5a7a2a')
        c.fill(line_mask(x + 3, y - 3, x + 1, y - 5), '#5a7a2a')
        fruit_(c, x, y, 1.4, col)
        fruit_(c, x + 3, y, 1.4, col)
        return
    if id == 'banane':
        for row in range(3):
            for j in range(3):
                bx, by = x - 3 + j * 2.6, y - 3 + row * 3
                m = capsule(bx, by, bx + 1.6, by + 3.2, 1.1, 0.9)
                c.shape(m, R(col, light=1.2), L_sphere(bx - 0.5, by, 1.5, 2.5), hl=0)
        c.shape(capsule(x, y - 6, x, y - 3, 0.8), R('#6a7a3a'), L_const(0.5))
        return
    if id == 'noix_de_coco':
        for (dx, dy) in [(0, 0), (4, 1), (2, 3)]:
            fruit_(c, x + dx, y + dy, 2.6, col)
        return
    if id in ('feves_de_cacao',):
        m = ellipse(x, y, 2, 3.4)
        c.shape(m, R(col), L_sphere(x - 1, y - 1, 2, 3.4), hl=1)
        for j in range(-2, 3, 2):
            c.fill(rect(x, y + j, x, y + j) & m, R(col)[1])
        return
    if id in ('cannelle',):
        c.shape(capsule(x, y - 2, x + 1, y + 3, 0.9), R('#8a5028'), L_const(0.6))
        return
    if id == 'clou_de_girofle':
        for dx in (-2, 0, 2):
            c.fill(line_mask(x + dx, y - 2, x + dx, y + 1), '#8a3a2a')
            c.fill(rect(x + dx, y - 3, x + dx, y - 3), '#d86a4a')
        return
    r = {1: 1.6, 2: 2.2, 3: 2.8, 4: 3.2}[taille]
    ry = r * (1.3 if id in ('poire', 'mangue', 'avocat', 'citron') else 1)
    fruit_(c, x, y, r, col, ry)
    if id in ('pomme', 'poire', 'peche', 'abricot', 'orange', 'mangue'):
        c.fill(rect(x, int(y - ry) - 1, x, int(y - ry) - 1), '#5a3a1a')


def fruit_(c, x, y, r, col, ry=None):
    m = ellipse(x, y, r, ry or r)
    c.shape(m, R(col, light=1.25), L_sphere(x - r * 0.4, y - r * 0.4, r, ry or r), hl=1)


def feuillage(arbre, etat, pal):
    fruit_id, nom, climat, forme, bois, feu, fl, taille = especes.ARBRES[arbre]
    c = Canvas()
    _feuilles_base(c, feu, arbre, palmier=forme in ('palmier', 'bananier'))
    rs = rnd(arbre + 'f')
    spots = [(6, 7), (18, 5), (27, 11), (12, 16), (22, 19), (4, 24), (15, 27), (27, 28)]
    if forme in ('palmier', 'bananier'):
        spots = [(10, 14), (22, 20)] if etat else []
    if etat == 1:
        for (x, y) in spots:
            if fl:
                for (dx, dy) in [(0, 0), (-2, 1), (2, 1), (-1, -2), (1, -2)]:
                    c.fill(rect(x + dx, y + dy, x + dx, y + dy), fl)
                c.fill(rect(x, y, x, y), '#f0c830')
            else:
                _fruit_arbre(c, x, y, max(1, taille - 1), mix(feu, '#a8d070', 0.5), fruit_id, rs)
    elif etat == 2:
        col = pal[0] if pal else '#c83a2a'
        if fruit_id == 'olives':
            col = '#4a5a2a'
        if fruit_id == 'amande':
            col = '#c8d890'
        if fruit_id == 'noix':
            col = '#a8c860'
        if fruit_id == 'noisette':
            col = '#b8783a'
        if fruit_id == 'pistache':
            col = '#e8b0a8'
        if fruit_id == 'avocat':
            col = '#5a7a2a'
        if fruit_id == 'muscade':
            col = '#e8c870'
        if fruit_id == 'feves_de_cacao':
            col = '#d8781a'
        for (x, y) in spots:
            _fruit_arbre(c, x, y, taille, col, fruit_id, rs)
    return c.image()


def pousse_arbre(arbre):
    fruit_id, nom, climat, forme, bois, feu, fl, taille = especes.ARBRES[arbre]
    c = Canvas()
    tronc = {'oak': '#6a4a2a', 'birch': '#d8d0c0', 'dark_oak': '#4a3420', 'jungle': '#7a5a2a', 'acacia': '#8a6a4a', 'spruce': '#4a3420'}[bois]
    if forme in ('palmier', 'bananier'):
        c.shape(rect(15, 16, 16, 31), R(tronc), L_horiz(15, 17, 0.9, 0.3))
        for (x1, y1) in [(4, 12), (27, 12), (8, 5), (23, 5), (16, 2)]:
            leaf(c, 16, 16, x1, y1, 5 if forme == 'bananier' else 3.6, feu, vein=True)
        return c.image()
    c.shape(rect(15, 18, 16, 31), R(tronc), L_horiz(15, 17, 0.9, 0.3))
    c.fill(line_mask(16, 22, 21, 18), R(tronc)[2])
    rr = R(feu)
    for (x, y, r) in [(12, 14, 6), (20, 13, 6), (16, 8, 6.5), (16, 16, 5)]:
        m = circle(x, y, r)
        c.shape(m, rr, L_sphere(x - 2, y - 2, r, r), hl=0)
    rs = rnd(arbre + 'p')
    for k in range(26):
        x, y = rs.randint(7, 25), rs.randint(3, 20)
        if c.opaque()[y, x]:
            c.fill(rect(x, y, x, y), rr[1] if k % 2 else rr[4])
    if fl:
        for (x, y) in [(11, 10), (20, 9), (16, 15)]:
            c.fill(rect(x, y, x, y), fl)
    c.outline()
    return c.image()


# ------------------------------------------------------------------------------ sachets de graines
def sachet(produit_img, feu):
    """Sachet de graines en papier avec l'aliment dessiné dessus."""
    c = Canvas()
    body = polygon([(6, 5), (26, 5), (27, 30), (5, 30)])
    c.shape(body, R('#e8d8b0'), L_horiz(5, 27, 0.9, 0.35))
    c.shape(rect(6, 3, 26, 6), R('#d8c898'), L_const(0.6))
    for x in range(7, 26, 2):
        c.fill(rect(x, 3, x, 3), '#c8b888')
    c.shape(rect(7, 25, 25, 28), R(feu), L_const(0.6), edge=False)
    im = c.image()
    icone = produit_img.resize((18, 18), Image.BOX)
    a = np.array(icone)
    a[..., 3] = np.where(a[..., 3] > 110, 255, 0)
    im.alpha_composite(Image.fromarray(a), (7, 7))
    c2 = Canvas()
    c2.px[:] = np.array(im)
    c2.outline()
    return c2.image()


# ------------------------------------------------------------------------------ blocs du monde
def pierre(seed=1):
    c = Canvas()
    rs = rnd('pierre%d' % seed)
    base = np.array([125, 125, 125])
    for y in range(32):
        for x in range(32):
            v = rs.randint(-10, 11)
            c.px[y, x, :3] = np.clip(base + v, 0, 255)
            c.px[y, x, 3] = 255
    for k in range(18):
        x, y = rs.randint(0, 31), rs.randint(0, 31)
        c.fill(rect(x, y, min(31, x + rs.randint(0, 3)), y), '#6e6e6e' if k % 2 else '#9a9a9a')
    return c


def minerai_de_sel():
    c = pierre(3)
    for (x, y, r) in [(7, 8, 3), (22, 6, 2.5), (15, 17, 3.5), (6, 24, 2.5), (25, 23, 3)]:
        m = polygon([(x - r, y), (x, y - r * 1.2), (x + r, y), (x, y + r * 1.1)])
        c.shape(m, ['#b8bcc4', '#d8dce2', '#eef0f4', '#ffffff'], L_sphere(x - 1, y - 1, r, r), edge=True, hl=1)
        c.fill(rect(int(x) - 1, int(y) - 1, int(x) - 1, int(y) - 1), '#ffffff')
    return c.image()


def ruche(face, pleine):
    """Ruche sauvage : écorce de tronc creux, rayons de cire, trou d'entrée ; coulures de miel si pleine."""
    c = Canvas()
    rs = rnd('ruche' + face)
    for y in range(32):
        for x in range(32):
            v = rs.randint(-12, 13)
            base = (150, 104, 52) if face != 'dessus' else (190, 150, 80)
            c.px[y, x, :3] = np.clip(np.array(base) + v, 0, 255)
            c.px[y, x, 3] = 255
    for x in range(0, 32, 5):
        c.fill(line_mask(x, 0, x + 1, 31), '#6a4420')
    # alvéoles de cire
    for (cx, cy) in [(6, 6), (12, 6), (9, 11), (15, 11), (21, 8), (18, 15), (24, 14), (27, 20), (6, 20), (12, 24)]:
        m = polygon([(cx - 2, cy), (cx - 1, cy - 2), (cx + 1, cy - 2), (cx + 2, cy), (cx + 1, cy + 2), (cx - 1, cy + 2)])
        c.shape(m, R('#e8b840'), L_const(0.6), edge=True)
        c.fill(rect(cx, cy, cx, cy), '#c88a18' if not pleine else '#f8c848')
    if face == 'avant':
        m = ellipse(16, 19, 4, 3)
        c.fill(m, '#1a1008')
        c.fill(arc_band(16, 19, 4, 3, 1, 180, 360), '#6a4420')
    if pleine:
        for (x, y0, L) in [(8, 0, 9), (19, 0, 13), (26, 0, 7), (13, 22, 6)]:
            c.shape(capsule(x, y0, x, y0 + L, 1.2, 1.5), R('#f0a818', light=1.25), L_horiz(x - 1, x + 1, 0.95, 0.4), hl=1)
    return c.image()


def coquillages(kind):
    """Dessus d'un banc de moules ou d'huîtres posé sur le sable/fond (texture vue de dessus)."""
    c = Canvas()
    rs = rnd(kind)
    for y in range(32):
        for x in range(32):
            v = rs.randint(-8, 9)
            c.px[y, x, :3] = np.clip(np.array((206, 194, 150)) + v, 0, 255)
            c.px[y, x, 3] = 255
    pts = [(6, 6), (17, 5), (26, 8), (10, 15), (22, 16), (5, 24), (15, 25), (26, 26)]
    for (x, y) in pts:
        a = rs.uniform(0, math.pi)
        if kind == 'moule':
            m = capsule(x - math.cos(a) * 3, y - math.sin(a) * 3, x + math.cos(a) * 3, y + math.sin(a) * 3, 2.4, 1.4)
            c.shape(m, R('#2a2a4a', light=1.4), L_sphere(x - 1, y - 1, 3, 3), hl=1)
            c.fill(line_mask(x - math.cos(a) * 2, y - math.sin(a) * 2, x + math.cos(a) * 2, y + math.sin(a) * 2) & m & ~edge(m), '#4a4a7a')
        else:
            m = ellipse(x, y, 3.6, 3)
            c.shape(m, R('#8c8472'), L_sphere(x - 1, y - 1, 3.6, 3), hl=0)
            c.fill(arc_band(x, y, 2.6, 2, 1) & m, '#a89e88')
    return c.image()
