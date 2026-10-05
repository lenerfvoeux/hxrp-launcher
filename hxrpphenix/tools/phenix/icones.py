"""
Icônes 32×32 (plume de phénix, œuf de phénix) dans le style des icônes du Gourmet (pixels francs, contour
presque noir, lumière en haut à gauche), et images des projectiles : boule de feu et plume enflammée,
4 images chacune empilées verticalement (32×128).

Réutilise le petit moteur de pixel art du Gourmet (hxrpmetiers/tools/textures/pix.py).
"""
import math
import os
import sys

import numpy as np
from PIL import Image

ICI = os.path.dirname(os.path.abspath(__file__))
RACINE = os.path.normpath(os.path.join(ICI, '..', '..'))
sys.path.insert(0, os.path.normpath(os.path.join(RACINE, '..', 'hxrpmetiers', 'tools', 'textures')))
import pix  # noqa: E402
from pix import XX, YY, Canvas, ellipse, polygon, capsule, line_mask  # noqa: E402

ASSETS = os.path.join(RACINE, 'src', 'main', 'resources', 'assets', 'hxrpphenix')

FEU = ['#5a0c08', '#8e1410', '#c8260f', '#e6531a', '#f48c1e', '#f9c443', '#ffe98c', '#fffbe6']


def feu(t):
    """Couleur de flamme pour t dans [0, 1] (braise sombre -> blanc incandescent)."""
    t = min(1.0, max(0.0, t)) * (len(FEU) - 1)
    i = min(int(t), len(FEU) - 2)
    return pix.mix(FEU[i], FEU[i + 1], t - i)


def scintille(frame, a, b=0.0):
    return math.sin(frame * math.pi / 2 + a * 1.3 + b)


# ============================================================================ plume
def masque_plume(x0, y0, x1, y1, largeur, courbe=1.5):
    """Plume de (x0,y0) (calamus) à (x1,y1) (pointe) : vexille dissymétrique légèrement courbée."""
    dx, dy = x1 - x0, y1 - y0
    L = math.hypot(dx, dy)
    ux, uy = dx / L, dy / L
    along = (XX - x0) * ux + (YY - y0) * uy
    t = np.clip(along / L, 0, 1)
    across = -(XX - x0) * uy + (YY - y0) * ux - courbe * np.sin(np.pi * t)
    gauche = largeur * 0.55 * np.sin(np.pi * np.clip((t - 0.12) / 0.88, 0, 1)) ** 0.65
    droite = largeur * 0.45 * np.sin(np.pi * np.clip((t - 0.16) / 0.84, 0, 1)) ** 0.75
    m = (along >= 0) & (along <= L) & (((across <= 0) & (-across <= gauche)) | ((across > 0) & (across <= droite)))
    return m, t, across, along


def peindre_plume(c, x0, y0, x1, y1, largeur, frame=0, flammes=True, brillant=True):
    m, t, across, along = masque_plume(x0, y0, x1, y1, largeur)
    L = math.hypot(x1 - x0, y1 - y0)
    # langues de feu au bout et sur les bords externes
    if flammes:
        fl = np.zeros_like(m)
        for k in range(5):
            a = 0.72 + k * 0.06
            ex = x0 + (x1 - x0) * a
            ey = y0 + (y1 - y0) * a
            side = -1 if k % 2 == 0 else 1
            nx, ny = (y1 - y0) / L * side, -(x1 - x0) / L * side
            long_ = 2.0 + 1.2 * scintille(frame, k)
            ox = ex + nx * (largeur * 0.4 + long_) + (x1 - x0) / L * 2.5
            oy = ey + ny * (largeur * 0.4 + long_) + (y1 - y0) / L * 2.5
            fl |= capsule(ex, ey, ox, oy, 1.6, 0.25)
        fl |= capsule(x1, y1, x1 + (x1 - x0) / L * (3 + scintille(frame, 9)), y1 + (y1 - y0) / L * (3 + scintille(frame, 9)), 1.3, 0.2)
        fl &= ~m
        c.shape(fl, [pix.col(feu(0.62)), pix.col(feu(0.75)), pix.col(feu(0.88))], pix.L_const(0.6), edge=False)
    # vexille : dégradé du calamus (cramoisi) à la pointe (or pâle), ombre du côté droit
    img = c.px
    for j in range(32):
        for i in range(32):
            if not m[j, i]:
                continue
            tt = t[j, i]
            v = 0.2 + 0.72 * tt ** 0.9
            if across[j, i] > 0:
                v -= 0.1
            # barbes : stries obliques
            if int((along[j, i] + abs(across[j, i]) * 0.9) * 0.55) % 2 == 0 and abs(across[j, i]) > 1.2:
                v -= 0.07
            col = feu(v)
            img[j, i, :3] = col
            img[j, i, 3] = 255
    # rachis clair
    c.fill(line_mask(x0, y0, x0 + (x1 - x0) * 0.92, y0 + (y1 - y0) * 0.92, 1) & m, pix.mix('#fff4c8', feu(0.85), 0.3))
    # calamus nu
    cal = capsule(x0 - (x1 - x0) / L * 3, y0 - (y1 - y0) / L * 3, x0 + (x1 - x0) / L * 2, y0 + (y1 - y0) / L * 2, 0.7)
    c.shape(cal & ~m, ['#8a5a1e', '#c89a4a', '#f0d088'], pix.L_const(0.6), edge=False)
    if brillant:
        # reflets incandescents
        for k in (0.55, 0.7):
            px_, py_ = x0 + (x1 - x0) * k - (y1 - y0) / L * 1.5, y0 + (y1 - y0) * k + (x1 - x0) / L * 1.5
            c.fill(ellipse(px_, py_, 0.6, 0.6) & m, '#fffbe6')
    return m


def plume_icone():
    c = Canvas()
    peindre_plume(c, 6.5, 27, 25, 6, 9.5, frame=0, flammes=True)
    c.outline()
    return c.image()


# ============================================================================ œuf
def oeuf_icone():
    c = Canvas()
    c.shadow_under(16, 28.5, 9, 2, 0.4)
    # nid de braises
    nid = ellipse(16, 27, 11, 3.2)
    c.shape(nid, ['#3a1410', '#6a2414', '#a8381a'], pix.L_vert(24, 30), edge=True)
    c.speckle(nid, '#ff9a2a', 0.22, seed=3)
    m = ellipse(16, 16.5, 8.6, 11.2)
    rr = [pix.col(feu(x)) for x in (0.12, 0.24, 0.38, 0.52, 0.66)]
    c.shape(m, rr, pix.L_sphere(13.5, 11, 8.6, 11.2), edge=True, hl=2)
    # flammes dorées qui montent depuis la base de la coquille
    fl = np.zeros_like(m)
    for k, (x, h) in enumerate(((9.5, 8), (13, 13), (16.5, 10), (20, 14), (23.5, 8.5))):
        fl |= capsule(x, 27.5, x + 1.6 * (1 if k % 2 else -1), 27.5 - h, 2.3, 0.2)
    c.shape(fl & m, [pix.col(feu(0.62)), pix.col(feu(0.78)), pix.col(feu(0.9)), pix.col(feu(0.97))], pix.L_vert(13, 27, 0.95, 0.35), edge=True, edge_col=feu(0.3))
    # fêlure lumineuse
    fel = line_mask(18, 7, 16.5, 10, 1) | line_mask(16.5, 10, 18.5, 12.5, 1)
    c.fill(fel & m, '#fff4c0')
    c.outline()
    return c.image()


# ============================================================================ projectiles
def boule_images():
    out = Image.new('RGBA', (32, 128))
    for f in range(4):
        img = np.zeros((32, 32, 4), np.uint8)
        for j in range(32):
            for i in range(32):
                dx, dy = i + 0.5 - 16, j + 0.5 - 16
                r = math.hypot(dx, dy)
                a = math.atan2(dy, dx)
                # bord en langues de flamme qui tournent
                bord = 11.0 + 1.6 * math.sin(a * 7 + f * math.pi / 2) + 1.0 * math.sin(a * 11 - f * 1.3) + 0.8 * math.sin(a * 3 + f)
                if r > bord:
                    continue
                t = 1 - r / bord
                spir = 0.08 * math.sin(a * 3 + r * 0.6 - f * math.pi / 2)
                col = feu(min(1, 0.3 + t * 0.9 + spir))
                if r > bord - 1.0:
                    col = feu(0.12)
                img[j, i, :3] = col
                img[j, i, 3] = 255
        out.paste(Image.fromarray(img, 'RGBA'), (0, f * 32))
    return out


def plume_ardente_images():
    out = Image.new('RGBA', (32, 128))
    for f in range(4):
        c = Canvas()
        peindre_plume(c, 10, 27, 22, 5, 8.5, frame=f, flammes=True, brillant=False)
        c.outline(pix.col(feu(0.05)))
        out.paste(c.image(), (0, f * 32))
    return out


def ecrire():
    os.makedirs(os.path.join(ASSETS, 'textures', 'items'), exist_ok=True)
    os.makedirs(os.path.join(ASSETS, 'textures', 'entity'), exist_ok=True)
    plume_icone().save(os.path.join(ASSETS, 'textures', 'items', 'plume_de_phenix.png'))
    oeuf_icone().save(os.path.join(ASSETS, 'textures', 'items', 'oeuf_de_phenix.png'))
    boule_images().save(os.path.join(ASSETS, 'textures', 'entity', 'boule_de_feu.png'))
    plume_ardente_images().save(os.path.join(ASSETS, 'textures', 'entity', 'plume_ardente.png'))
    print('icônes et projectiles écrits')


if __name__ == '__main__':
    ecrire()
