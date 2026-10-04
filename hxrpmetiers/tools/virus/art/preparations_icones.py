"""
Icônes 32x32 des préparations de l'officine : un contenant par sorte de préparation (fiole ronde, flacon d'eau
florale, sirop, teinture à pipette, élixir taillé, onguent en pot, baume en boîte, cataplasme, pilulier, seringue,
bandage, attelle, fumigation, poudre, alcools…), coloré d'après ses ingrédients (couleur vive de leurs icônes).
Le bouchon dit le rang : bois (0★), cire rouge (1★), cire bleue (2★), or (3★).
"""
import math
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, '..', '..', 'textures'))
from pix import *  # noqa: E402,F401,F403
import outils  # noqa: E402

BOUCHONS = ['#b07a44', '#b8241c', '#2a5aa8', '#e8b830']
NEUTRES = {'bouteille_d_eau', 'eau', 'miel', 'alcool_de_riz', 'alcool_fort', 'seringue_vide', 'base_d_onguent', 'huile_de_lin',
           'bandes_de_laine', 'sel', 'vinaigre_de_riz'}


# ============================================================================ couleurs
def couleur_vive(img):
    """Couleur moyenne des pixels bien colorés d'une icône (comme ClientJeux.couleurVive côté jeu)."""
    a = np.asarray(img.convert('RGBA')).reshape(-1, 4).astype(int)
    a = a[a[:, 3] == 255]
    mx, mn = a[:, :3].max(1), a[:, :3].min(1)
    ok = (mx >= 60) & ((mx - mn) >= mx * 0.3)
    if ok.sum() < 6:
        ok = mx >= 60
    if ok.sum() == 0:
        return '#8fb860'
    r, g, b = a[ok, :3].mean(0)
    return '#%02x%02x%02x' % (int(r), int(g), int(b))


class Couleurs:
    """Couleur dominante et d'accent de chaque préparation, déduites de ses ingrédients."""

    def __init__(self, preps, icones_ingredients, dossier_items_gourmet):
        self.preps = {p.get('produit') or p['id']: p for p in preps}
        self.par_id = {p['id']: p for p in preps}
        self.ic = dict(icones_ingredients)
        self.gourmet = dossier_items_gourmet
        self.cache = {}

    def ingredient(self, id):
        if id in self.cache:
            return self.cache[id]
        c = None
        if id in self.ic:
            c = couleur_vive(self.ic[id])
        elif id in self.par_id or id in self.preps:
            c = self.prep(self.par_id.get(id) or self.preps[id])[0]
        else:
            p = os.path.join(self.gourmet, id + '.png')
            if os.path.exists(p):
                c = couleur_vive(Image.open(p))
        self.cache[id] = c or '#8fb860'
        return self.cache[id]

    def prep(self, p):
        ids = [i for i in p['ingredients'] if ':' not in i]
        utiles = [i for i in ids if i not in NEUTRES] or ids
        if not utiles:
            return '#e8a020', '#f8e070'
        c1 = self.ingredient(utiles[0])
        c2 = self.ingredient(utiles[1]) if len(utiles) > 1 else mix(c1, '#ffffff', 0.4)
        return c1 if isinstance(c1, str) else tohex(c1), c2 if isinstance(c2, str) else tohex(c2)


def h(c):
    return c if isinstance(c, str) else tohex(c)


# ============================================================================ éléments communs
def etiquette(c, x0, y0, x1, y1, accent, ficelle=True):
    m = rrect(x0, y0, x1, y1, 1)
    c.shape(m, R('#f4ecd0'), L_const(0.75))
    c.fill(rect(x0 + 1, y0 + (y1 - y0) // 2, x1 - 1, y0 + (y1 - y0) // 2), accent)
    if ficelle:
        c.fill(line_mask(x0, y0, x0 - 2, y0 - 3), '#b8241c')


def bouchon(c, x0, y0, x1, y1, rang):
    col_ = BOUCHONS[max(0, min(3, rang))]
    c.shape(rrect(x0, y0, x1, y1, 1), R(col_, light=1.3), L_horiz(x0, x1, 0.9, 0.35), hl=1)
    if rang >= 1:
        c.shape(ellipse((x0 + x1) / 2, y1 + 0.5, (x1 - x0) / 2 + 1, 1.4), R(col_), L_const(0.55))


def verre(c, m, liquide, niveau_y, couleur_verre='#d8ecf4'):
    """Remplit un contenant de verre : liquide sous niveau_y, reflet clair à gauche."""
    c.shape(m, R(couleur_verre), L_horiz(0, 31, 0.9, 0.45))
    liq = m & (YY >= niveau_y) & ~edge(m)
    if liq.any():
        c.shape(liq, R(liquide, light=1.3), L_horiz(4, 28, 0.9, 0.3), edge=False)
        c.fill(liq & (YY < niveau_y + 1), mix(liquide, '#ffffff', 0.45))
    ys, xs = np.nonzero(m & ~edge(m))
    if len(xs):
        x = xs.min() + 1
        c.fill(rect(x, ys.min() + 2, x, ys.max() - 2) & m & ~edge(m), '#ffffff')


# ============================================================================ contenants
def fiole_ronde(c, liq, acc, rang):
    c.shadow_under(16, 29, 9, 2, 0.25)
    m = circle(16, 21, 8.5) | rect(13, 7, 19, 14)
    verre(c, m, liq, 16)
    c.fill(rect(12, 7, 20, 8), '#c8dce4')
    bouchon(c, 12, 2, 20, 7, rang)
    etiquette(c, 17, 18, 24, 23, acc)


def eau_florale(c, liq, acc, rang):
    c.shadow_under(16, 29.5, 7, 1.8, 0.25)
    m = rrect(11, 13, 21, 29, 3) | rect(14, 5, 18, 13)
    verre(c, m, mix(liq, '#ffffff', 0.45), 16)
    c.shape(ellipse(16, 3.5, 2.6, 2.4), R('#e8f4f8'), L_sphere(15, 2.5, 2.6, 2.4), hl=1)
    bouchon(c, 14, 5, 18, 7, rang)
    for (x, y) in ((13, 22), (18, 25)):
        c.fill(circle(x, y, 1.1), mix(acc, '#ffffff', 0.3))


def sirop(c, liq, acc, rang):
    c.shadow_under(16, 29, 11, 2.2, 0.3)
    m = rrect(6, 13, 26, 29, 5) | rect(12, 7, 20, 13)
    verre(c, m, mix(liq, '#3a1a08', 0.35), 15, '#d8e8e0')
    bouchon(c, 11, 3, 21, 8, rang)
    c.shape(ellipse(10, 15, 1.6, 3), R(mix(liq, '#3a1a08', 0.3), light=1.3), L_const(0.6), edge=False)
    etiquette(c, 11, 19, 21, 25, acc, ficelle=False)


def teinture(c, liq, acc, rang):
    c.shadow_under(16, 29, 8, 2, 0.3)
    m = rrect(9, 13, 23, 29, 2) | rect(13, 9, 19, 13)
    verre(c, m, mix(liq, '#2a1206', 0.45), 15, '#8a5a2a')
    # pipette : bague, poire de caoutchouc
    c.shape(rect(12, 8, 20, 10), R(BOUCHONS[max(0, min(3, rang))], light=1.3), L_horiz(12, 20), hl=1)
    c.shape(ellipse(16, 4.5, 3.4, 4), R('#2a2a2e', light=1.6), L_sphere(15, 3, 3.4, 4), hl=1)
    etiquette(c, 11, 18, 21, 25, acc, ficelle=False)


def elixir(c, liq, acc, rang, eclat='#ffffff'):
    c.shadow_under(16, 29.5, 8, 1.8, 0.3)
    m = polygon([(16, 9), (25, 18), (16, 29), (7, 18)])
    verre(c, m, liq, 14)
    for (x0, y0, x1, y1) in ((16, 9, 16, 29), (7, 18, 25, 18)):
        c.fill(line_mask(x0, y0, x1, y1) & m & ~edge(m), mix(liq, '#ffffff', 0.35))
    c.shape(rect(14, 5, 18, 9), R('#d8ecf4'), L_horiz(14, 18))
    c.shape(polygon([(13, 5), (19, 5), (16, 1)]), R('#e8b830', light=1.3), L_const(0.7), hl=1)
    for (x, y) in ((11, 16), (19, 21), (15, 24)):
        c.fill(rect(x, y, x, y), eclat)


def huile(c, liq, acc, rang):
    c.shadow_under(16, 29, 9, 2, 0.25)
    m = ellipse(16, 22, 8, 7) | polygon([(13, 16), (19, 16), (17, 6), (15, 6)])
    verre(c, m, mix(liq, '#f0c030', 0.5), 18)
    bouchon(c, 14, 2, 18, 6, rang)
    c.fill(line_mask(18, 7, 23, 4), '#c8dce4')


def vinaigre(c, liq, acc, rang):
    c.shadow_under(16, 29.5, 7, 1.8, 0.25)
    m = rrect(10, 12, 22, 29, 2) | polygon([(13, 12), (19, 12), (18, 6), (14, 6)])
    verre(c, m, mix(liq, '#c8802a', 0.45), 14)
    bouchon(c, 13, 2, 19, 6, rang)
    etiquette(c, 12, 18, 20, 24, acc, ficelle=False)


def pot_onguent(c, liq, acc, rang, matiere='celadon'):
    c.shadow_under(16, 29, 11, 2.2, 0.3)
    base = '#8fbfa0' if matiere == 'celadon' else '#eef2f4'
    c.shape(rrect(6, 16, 26, 29, 4), R(base, light=1.1), L_horiz(6, 26, 0.9, 0.35))
    if matiere != 'celadon':
        c.fill(rect(7, 20, 25, 21), '#2a4a9a')
    c.shape(ellipse(16, 16, 10, 3.6), R(mix(base, '#ffffff', 0.2)), L_const(0.7))
    c.shape(ellipse(16, 16, 8.4, 2.6), R(liq, light=1.2), L_sphere(14, 15, 8.4, 2.6), edge=False, hl=1)
    c.fill(line_mask(13, 15, 19, 16) & ellipse(16, 16, 8.4, 2.6), mix(liq, '#ffffff', 0.4))
    # couvercle posé de travers
    c.shape(ellipse(20, 9, 8, 3.2), R(base, light=1.1), L_sphere(18, 8, 8, 3.2), hl=1)
    c.shape(ellipse(20, 7.5, 2.4, 1.6), R(BOUCHONS[max(0, min(3, rang))]), L_const(0.6))


def boite_baume(c, liq, acc, rang, grande=False):
    c.shadow_under(16, 28, 12, 2.4, 0.3)
    rx = 12 if grande else 10
    c.shape(rect(16 - rx, 18, 16 + rx, 26) & ellipse(16, 22, rx, 8), R('#c8922a', light=1.25), L_horiz(16 - rx, 16 + rx, 0.9, 0.3))
    c.shape(ellipse(16, 18, rx, 4), R('#e8be58'), L_const(0.7))
    c.shape(ellipse(16, 18, rx - 1.6, 3), R(liq, light=1.15), L_sphere(14, 17, rx - 2, 3), edge=False, hl=1)
    c.fill(line_mask(12, 18, 19, 17.5) & ellipse(16, 18, rx - 1.6, 3), mix(liq, '#000000', 0.25))
    c.shape(ellipse(19, 9, rx - 1, 3.4), R('#d8a438', light=1.2), L_sphere(17, 8, rx, 3.4), hl=2)
    c.fill(arc_band(19, 9, 4, 1.6, 1, 0, 360), '#8a5a10')


def cataplasme(c, liq, acc, rang):
    c.shadow_under(16, 28, 12, 2.4, 0.25)
    linge = polygon([(4, 14), (26, 11), (29, 25), (7, 28)])
    c.shape(linge, R('#ece4d0', light=1.05), L_sphere(10, 12, 18, 14, 0.5))
    for k in range(4):
        c.fill(line_mask(5 + k * 6, 14, 8 + k * 6, 27) & linge & ~edge(linge), '#d8ceb8')
    c.shape(ellipse(16, 19, 8, 5), R(liq, light=1.25), L_sphere(13, 17, 8, 5, 0.3), hl=1)
    c.speckle(ellipse(16, 19, 8, 5), mix(acc, '#000000', 0.2), 0.15, seed=7)
    c.fill(line_mask(4, 14, 1, 11) | line_mask(26, 11, 29, 8), '#b8241c')


def pilulier(c, liq, acc, rang):
    c.shadow_under(16, 28.5, 12, 2.2, 0.3)
    c.shape(rect(5, 19, 27, 26) & ellipse(16, 22.5, 11, 8), R('#a8743e', light=1.2), L_horiz(5, 27, 0.9, 0.35))
    c.shape(ellipse(16, 19, 11, 4), R('#c8945a'), L_const(0.65))
    c.shape(ellipse(16, 19, 9.5, 3), R('#5a3a1e'), L_const(0.5), edge=False)
    for (x, y) in ((11, 18.5), (15, 17.5), (19, 18.5), (13, 20), (18, 20), (22, 19.5), (9, 19.5)):
        c.shape(circle(x, y, 1.7), R(liq, light=1.3), L_sphere(x - 0.5, y - 0.6, 1.7, 1.7), hl=1)
    for (x, y) in ((23, 27), (26, 25)):
        c.shape(circle(x, y, 1.7), R(liq, light=1.3), L_sphere(x - 0.5, y - 0.6, 1.7, 1.7), hl=1)
    c.shape(ellipse(10, 9, 9, 3.6), R('#a8743e', light=1.2), L_sphere(8, 8, 9, 3.6), hl=1)
    c.fill(circle(10, 8.5, 1.4), BOUCHONS[max(0, min(3, rang))])


def bandage(c, liq, acc, rang):
    c.shadow_under(16, 28, 12, 2.4, 0.3)
    c.shape(rect(5, 12, 22, 26) & ellipse(13.5, 19, 9, 9), R('#efe8d8'), L_horiz(5, 22, 0.9, 0.4))
    c.shape(ellipse(22, 19, 4.5, 7), R('#f8f4ea'), L_sphere(21, 17, 4.5, 7))
    for r in (1.5, 3, 4.2):
        c.fill(arc_band(22, 19, r * 0.6, r, 1, 0, 360) & ellipse(22, 19, 4.5, 7), '#d8d0bc')
    c.shape(polygon([(22, 23), (29, 26), (28, 29), (21, 26)]), R('#efe8d8'), L_const(0.7))
    c.shape(ellipse(12, 19, 4, 3), R(liq), L_const(0.55), edge=False)


def attelle(c, liq, acc, rang):
    c.shadow_under(16, 28, 12, 2.4, 0.3)
    for (y0, y1) in ((8, 26), (11, 29)):
        c.shape(polygon([(6 + (y0 - 8) * 1.5, y0), (9 + (y0 - 8) * 1.5, y0 - 1), (26 + (y0 - 8) * 0.2, y1 - 2), (23 + (y0 - 8) * 0.2, y1)]),
                R('#c8945a', light=1.15), L_const(0.6))
    for t in (0.25, 0.65):
        x, y = 8 + 16 * t, 9 + 17 * t
        c.shape(polygon([(x - 4, y + 2), (x + 3, y - 3), (x + 5, y - 1), (x - 2, y + 4)]), R('#efe8d8'), L_const(0.75))
        c.fill(line_mask(x - 2, y + 2, x + 3, y - 1), liq)


def fumigation(c, liq, acc, rang):
    c.shadow_under(16, 29, 9, 2, 0.25)
    for k, (x0, x1) in enumerate(((11, 8), (14, 13), (17, 18), (20, 23))):
        c.shape(capsule(x0, 29, x1, 13, 1.5, 2.2), R(mix(liq, '#7a6a4a', 0.3) if k % 2 else liq, light=1.2), L_cyl(x0, 29, x1, 13, 2.2))
    c.shape(rect(11, 22, 21, 24), R('#b8241c'), L_const(0.6))
    for k in range(4):
        x, y = 12 + k * 3, 10 - k * 2
        c.shape(circle(x, y, 2.6 - k * 0.3), R('#d8d4cc'), L_const(0.75), edge=False)
    c.fill(rect(15, 12, 17, 13), '#f8a030')


def poudre(c, liq, acc, rang):
    c.shadow_under(16, 28, 12, 2.4, 0.25)
    c.shape(polygon([(3, 22), (16, 16), (29, 22), (16, 29)]), R('#f2e6c8', light=1.05), L_const(0.8))
    c.shape(ellipse(16, 21, 9, 5) & (YY <= 23), R(liq, light=1.2), L_sphere(13, 17, 9, 6, 0.2))
    c.speckle(ellipse(16, 21, 9, 5), mix(acc, '#ffffff', 0.2), 0.12, seed=3)
    c.fill(line_mask(3, 22, 1, 19), '#b8241c')


def alcool_de_riz(c, liq, acc, rang):
    c.shadow_under(16, 29.5, 9, 2, 0.3)
    m = ellipse(16, 21, 8.5, 8) | polygon([(13, 15), (19, 15), (18, 6), (14, 6)])
    c.shape(m, R('#f2f2ec', light=1.05), L_horiz(7, 25, 0.9, 0.35))
    c.fill(rect(9, 19, 23, 20) & m, '#2a4a9a')
    c.fill(arc_band(16, 24, 3, 2, 1, 0, 360) & m, '#2a4a9a')
    c.shape(ellipse(16, 5, 3, 1.6), R('#e8e8e0'), L_const(0.7))


def alcool_fort(c, liq, acc, rang):
    c.shadow_under(16, 29.5, 9, 2, 0.3)
    m = rrect(8, 13, 24, 29, 4) | rect(13, 6, 19, 13)
    c.shape(m, R('#5a3418', light=1.35), L_horiz(8, 24, 0.9, 0.3), hl=2)
    bouchon(c, 12, 2, 20, 7, 0)
    etiquette(c, 11, 18, 21, 24, '#b8241c', ficelle=False)


def base_onguent(c, liq, acc, rang):
    pot_onguent(c, '#f6f0e2', '#e8d8b0', rang, matiere='porcelaine')


def bandes_laine(c, liq, acc, rang):
    c.shadow_under(16, 28, 12, 2.4, 0.25)
    for (x, y) in ((11, 20), (21, 21), (16, 13)):
        c.shape(ellipse(x, y, 6, 5.5), R('#ece4cc', light=1.05), L_sphere(x - 2, y - 2, 6, 5.5))
        for r in (2, 3.6):
            c.fill(arc_band(x, y, r, r * 0.9, 1, 0, 300), '#c8bc9a')


def charbon(c, liq, acc, rang):
    c.shadow_under(16, 27, 12, 2.4, 0.3)
    c.shape(ellipse(16, 24, 12, 4), R('#d8d0c4'), L_const(0.7))
    g = np.random.RandomState(8)
    for _ in range(7):
        x, y = g.uniform(9, 23), g.uniform(15, 22)
        m = polygon([(x - 3, y), (x - 1, y - 3), (x + 3, y - 2), (x + 3, y + 2), (x, y + 3)])
        c.shape(m, R('#2a2a2e', light=1.9), L_sphere(x - 1, y - 2, 4, 4, 0.2), hl=1)


def cendre_doree(c, liq, acc, rang):
    c.fill(circle(16, 18, 13) & (np.random.RandomState(9).rand(32, 32) < 0.05), '#fff0a0')
    c.shadow_under(16, 27, 12, 2.4, 0.3)
    c.shape(ellipse(16, 22, 12, 5), R('#3a2a22'), L_vert(17, 27, 0.8, 0.3))
    tas(c, 16, 20, 9, 5, '#e8a820')
    g = np.random.RandomState(10)
    for _ in range(18):
        x, y = g.uniform(8, 24), g.uniform(15, 22)
        c.fill(rect(int(x), int(y), int(x), int(y)), ['#fff4b0', '#f86020', '#ffffff'][g.randint(3)])


def tas(c, cx, cy, rx, ry, couleur):
    c.shape(ellipse(cx, cy, rx, ry) & (YY <= cy + ry * 0.6), R(couleur, light=1.2), L_sphere(cx - rx * 0.3, cy - ry, rx, ry * 1.4, 0.2))


def seringue(c, liq, acc, rang, doree=False):
    c.shadow_under(16, 28, 9, 2.2, 0.25)
    outils.seringue(c, liq, 0.8)
    if doree:
        for (x, y) in ((10, 19), (14, 15), (8, 23)):
            c.fill(rect(x, y, x, y), '#fff4b0')
    c.shape(rrect(19, 20, 26, 25, 1), R('#f4ecd0'), L_const(0.75))
    c.fill(rect(20, 22, 25, 22), acc)


# ============================================================================ choix du contenant
def dessiner(c, p, couleurs):
    id = p.get('produit') or p['id']
    forme = p['forme']
    rang = p.get('rang', 0)
    liq, acc = couleurs.prep(p)
    liq, acc = h(liq), h(acc)
    if id == 'remede_du_second_souffle':
        return seringue(c, '#f0b020', '#e8b830', 3, doree=True)
    if id == 'alcool_de_riz':
        return alcool_de_riz(c, liq, acc, rang)
    if id == 'alcool_fort':
        return alcool_fort(c, liq, acc, rang)
    if id == 'base_d_onguent':
        return base_onguent(c, liq, acc, rang)
    if id == 'bandes_de_laine':
        return bandes_laine(c, liq, acc, rang)
    if id.startswith('charbon'):
        return charbon(c, liq, acc, rang)
    if id == 'cendre_doree':
        return cendre_doree(c, liq, acc, rang)
    if id == 'essence_des_quinze_raretes':
        return elixir(c, '#8a6ae8', '#e8b830', rang, eclat='#fff4b0')
    if id == 'baume_des_quinze_ressources':
        return boite_baume(c, '#c8904a', acc, rang, grande=True)
    if id.startswith('vinaigre') or id.startswith('lotion'):
        return vinaigre(c, liq, acc, rang)
    if id.startswith('huile'):
        return huile(c, liq, acc, rang)
    if id.startswith('poudre'):
        return poudre(c, liq, acc, rang)
    if id.startswith('teinture'):
        return teinture(c, liq, acc, rang)
    if forme == 'seringue':
        return seringue(c, liq, acc, rang)
    if forme == 'pilules':
        return pilulier(c, liq, acc, rang)
    if forme == 'bandage':
        return bandage(c, liq, acc, rang)
    if forme == 'attelle':
        return attelle(c, liq, acc, rang)
    if forme == 'fumigation':
        return fumigation(c, liq, acc, rang)
    if forme == 'onguent':
        if id.startswith('baume'):
            return boite_baume(c, liq, acc, rang)
        if id.startswith('cataplasme') or id.startswith('compresse'):
            return cataplasme(c, liq, acc, rang)
        return pot_onguent(c, liq, acc, rang, 'celadon' if rang % 2 == 0 else 'porcelaine')
    if id.startswith('eau_florale'):
        return eau_florale(c, liq, acc, rang)
    if id.startswith('sirop'):
        return sirop(c, liq, acc, rang)
    if id.startswith('elixir'):
        return elixir(c, liq, acc, rang)
    return fiole_ronde(c, liq, acc, rang)
