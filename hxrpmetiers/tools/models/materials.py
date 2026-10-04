"""
Textures des stations (32x32, soit 2 texels par unité de modèle).
Matières communes (acier brossé, fonte, laiton, bois, céramique…) et faces peintes
(façade du four, plaque de cuisson, portes du frigo…). Tout est dessiné au pixel.
"""
import math
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, '..', 'textures'))
from pix import *  # noqa: E402,F401

S = 32
TEX = {}


def tex(name):
    def deco(fn):
        TEX[name] = fn
        return fn
    return deco


def U(v):
    """unité de modèle -> texel"""
    return int(round(v * 2))


def new(color=None):
    c = Canvas()
    if color:
        c.px[:, :, :3] = col(color)
        c.px[:, :, 3] = 255
    return c


def noise(c, colors, density, seed, mask=None, size=(1, 1)):
    rs = np.random.RandomState(seed)
    m = rs.rand(S, S) < density
    if mask is not None:
        m &= mask
    ys, xs = np.nonzero(m)
    for x, y in zip(xs, ys):
        cc = colors[rs.randint(len(colors))]
        c.px[y:y + size[1], x:x + size[0], :3] = col(cc)
        c.px[y:y + size[1], x:x + size[0], 3] = 255


def hstreaks(c, colors, n, seed, mask=None, lmin=4, lmax=14):
    rs = np.random.RandomState(seed)
    for _ in range(n):
        x, y = rs.randint(0, S), rs.randint(0, S)
        L = rs.randint(lmin, lmax)
        cc = col(colors[rs.randint(len(colors))])
        for i in range(L):
            xx = (x + i) % S
            if mask is None or mask[y, xx]:
                c.px[y, xx, :3] = cc
                c.px[y, xx, 3] = 255


def vstreaks(c, colors, n, seed, mask=None, lmin=4, lmax=14):
    rs = np.random.RandomState(seed)
    for _ in range(n):
        x, y = rs.randint(0, S), rs.randint(0, S)
        L = rs.randint(lmin, lmax)
        cc = col(colors[rs.randint(len(colors))])
        for i in range(L):
            yy = (y + i) % S
            if mask is None or mask[yy, x]:
                c.px[yy, x, :3] = cc
                c.px[yy, x, 3] = 255


def box_bevel(c, x0, y0, x1, y1, light, dark, fill=None):
    """Rectangle en relief (texels inclusifs)."""
    if fill:
        c.fill(rect(x0, y0, x1, y1), fill)
    c.fill(rect(x0, y0, x1, y0), light)
    c.fill(rect(x0, y0, x0, y1), light)
    c.fill(rect(x0, y1, x1, y1), dark)
    c.fill(rect(x1, y0, x1, y1), dark)


def disc(c, cx, cy, r, color):
    c.fill(circle(cx, cy, r), color)


def ring(c, cx, cy, r, th, color):
    c.fill(circle(cx, cy, r) & ~circle(cx, cy, r - th), color)


# ============================================================================ matières
@tex('acier')
def acier():
    c = new('#b9bec6')
    hstreaks(c, ['#c9ced6', '#aeb3bb', '#d3d7de', '#a8adb5'], 90, 1)
    return c


@tex('acier_sombre')
def acier_sombre():
    c = new('#6e737c')
    hstreaks(c, ['#787d86', '#646971', '#80858e'], 80, 2)
    return c


@tex('inox_poli')
def inox_poli():
    c = new('#c8ccd2')
    for x in range(S):
        v = 0.5 + 0.5 * math.sin(x / 11.0 * math.pi * 2 - 1.2)
        c.px[:, x, :3] = mix('#737982', '#eef2f6', v)
    vstreaks(c, ['#ffffff', '#5e646c'], 8, 3, lmin=6, lmax=20)
    return c


@tex('chrome')
def chrome():
    c = new('#dfe3e8')
    for y in range(S):
        v = 0.5 + 0.5 * math.sin(y / S * math.pi * 4)
        c.px[y, :, :3] = mix('#9aa0a8', '#ffffff', v)
    return c


@tex('noir_mat')
def noir_mat():
    c = new('#26272b')
    noise(c, ['#2e2f34', '#202125', '#34353a'], 0.35, 4)
    return c


@tex('fonte')
def fonte():
    c = new('#2a2d33')
    noise(c, ['#33373e', '#23262b', '#3c4048', '#1c1e22'], 0.45, 5)
    return c


@tex('laiton')
def laiton():
    c = new('#d4a23a')
    for y in range(S):
        v = 0.5 + 0.5 * math.sin(y / S * math.pi * 2 + 0.8)
        c.px[y, :, :3] = mix('#9a6a1a', '#f8d880', v)
    noise(c, ['#e8b84a'], 0.05, 6)
    return c


@tex('bois_clair')
def bois_clair():
    c = new('#c8955a')
    vstreaks(c, ['#b98548', '#d4a468', '#a8763c', '#dcae74'], 70, 7, lmin=8, lmax=30)
    for (x, y) in [(6, 10), (22, 24)]:
        c.fill(ellipse(x, y, 1.6, 2.4), '#8a5a2a')
        c.fill(ellipse(x, y, 0.8, 1.2), '#6a4018')
    return c


@tex('bois_fonce')
def bois_fonce():
    c = new('#6a4226')
    vstreaks(c, ['#5a361e', '#7a4e2e', '#4e2e18', '#825636'], 70, 8, lmin=8, lmax=30)
    return c


@tex('billot')
def billot():
    """Plan de travail en bois de bout (lamellé-collé)."""
    c = new('#d8a868')
    rs = np.random.RandomState(9)
    for k in range(0, S, 4):
        base = mix('#c8945a', '#e4b87a', rs.rand())
        c.fill(rect(k, 0, k + 3, S - 1), base)
        c.fill(rect(k + 3, 0, k + 3, S - 1), mix(base, '#6a4020', 0.35))
    vstreaks(c, ['#c08850', '#e8c088'], 40, 10, lmin=4, lmax=12)
    return c


@tex('ceramique')
def ceramique():
    c = new('#f2eee6')
    noise(c, ['#faf8f2', '#e8e3d8'], 0.15, 11)
    return c


@tex('ceramique_bleue')
def ceramique_bleue():
    c = new('#3a6ab0')
    noise(c, ['#4a7ac0', '#2e5a9e'], 0.2, 12)
    for y in (6, 7):
        c.fill(rect(0, y, S - 1, y), '#f2eee6')
    return c


@tex('terre_cuite')
def terre_cuite():
    c = new('#b8583a')
    noise(c, ['#c8684a', '#a84a2e', '#d0785a'], 0.35, 13)
    return c


@tex('pierre')
def pierre():
    c = new('#8a8c90')
    noise(c, ['#9a9ca0', '#76787c', '#aeb0b4', '#5e6064', '#c0c2c6'], 0.55, 14)
    return c


@tex('pierre_sombre')
def pierre_sombre():
    c = new('#4e5054')
    noise(c, ['#5a5c60', '#42444a', '#66686c', '#36383c'], 0.55, 15)
    return c


@tex('caoutchouc')
def caoutchouc():
    c = new('#1e1f22')
    for y in range(0, S, 4):
        c.fill(rect(0, y, S - 1, y), '#2a2b2f')
    return c


@tex('verre')
def verre():
    c = new('#cfe6ee')
    c.fill(rect(3, 0, 4, S - 1), '#f4fbfd')
    c.fill(rect(7, 0, 7, S - 1), '#e8f4f8')
    c.fill(rect(26, 0, 27, S - 1), '#b4d0da')
    return c


@tex('menthe')
def menthe():
    c = new('#8fd3c4')
    noise(c, ['#98dccd', '#86c9ba'], 0.12, 16)
    return c


@tex('creme')
def creme_mat():
    c = new('#f4ecd8')
    noise(c, ['#f8f2e2', '#ece2cc'], 0.12, 17)
    return c


@tex('rouge_email')
def rouge_email():
    c = new('#c8342a')
    noise(c, ['#d4443a', '#b82a22'], 0.15, 18)
    return c


@tex('orange_email')
def orange_email():
    c = new('#e87a22')
    noise(c, ['#f08a32', '#d86a18'], 0.15, 19)
    return c


@tex('braise')
def braise():
    c = new('#2a1410')
    rs = np.random.RandomState(20)
    for _ in range(38):
        x, y = rs.randint(0, S), rs.randint(0, S)
        r = rs.uniform(1.5, 3.2)
        c.fill(ellipse(x, y, r, r * 0.8), mix('#3a1a12', '#1a0c08', rs.rand()))
        c.fill(ellipse(x - 0.5, y - 0.5, r * 0.55, r * 0.4), ['#e8501a', '#f8a030', '#c83014'][rs.randint(3)])
    noise(c, ['#ffd060'], 0.03, 21)
    return c


@tex('grille')
def grille():
    """Grille de barbecue (découpe alpha)."""
    c = Canvas()
    for x in range(1, S, 4):
        c.fill(rect(x, 0, x + 1, S - 1), '#3a3c42')
        c.fill(rect(x, 0, x, S - 1), '#6e727a')
    c.fill(rect(0, 0, S - 1, 1) | rect(0, S - 2, S - 1, S - 1), '#2a2c30')
    return c


@tex('panier')
def panier():
    """Maille du panier de friteuse (découpe alpha)."""
    c = Canvas()
    for k in range(0, S, 3):
        c.fill(rect(k, 0, k, S - 1), '#9aa0a8')
        c.fill(rect(0, k, S - 1, k), '#c0c6ce')
    c.fill(rect(0, 0, S - 1, 1), '#d8dde4')
    return c


@tex('tamis')
def tamis_maille():
    c = Canvas()
    for k in range(0, S, 2):
        c.fill(rect(k, 0, k, S - 1), '#b8bcc4')
        c.fill(rect(0, k, S - 1, k), '#a8acb4')
    return c


@tex('huile')
def huile():
    c = new('#e8b02a')
    for y in range(S):
        c.px[y, :, :3] = mix('#f0c040', '#d89a1a', y / S)
    rs = np.random.RandomState(22)
    for _ in range(14):
        x, y = rs.randint(2, S - 2), rs.randint(2, S - 2)
        r = rs.uniform(0.8, 2)
        ring(c, x, y, r + 0.5, 1, '#fbe080')
        c.fill(rect(x - 1, y - 1, x - 1, y - 1), '#ffffff')
    return c


@tex('farine')
def farine():
    c = new('#f6f2e8')
    noise(c, ['#ffffff', '#ebe4d4', '#e0d8c4'], 0.3, 23)
    return c


@tex('pate')
def pate():
    c = new('#f0dca8')
    for k in range(4):
        c.fill(arc_band(16, 16, 4 + k * 4, 4 + k * 4, 1, k * 70, k * 70 + 200), '#fbeec8')
    noise(c, ['#e4cc94'], 0.08, 24)
    return c


@tex('epices')
def epices():
    c = new('#c86a2a')
    noise(c, ['#d8802e', '#a8501a', '#e8a040', '#6a3a1a', '#f0c060'], 0.6, 25)
    return c


@tex('ragout')
def ragout():
    c = new('#7a3a1a')
    noise(c, ['#8a4a24', '#6a2e14'], 0.4, 26)
    rs = np.random.RandomState(27)
    for _ in range(12):
        x, y = rs.randint(2, S - 3), rs.randint(2, S - 3)
        cc = ['#f07a1a', '#6a2e18', '#f4e4a8', '#3a8a2a', '#c8a070'][rs.randint(5)]
        c.fill(rect(x, y, x + 2, y + 1), cc)
        c.fill(rect(x, y + 1, x + 2, y + 1), mix(cc, '#000000', 0.3))
    for _ in range(4):
        x, y = rs.randint(3, S - 3), rs.randint(3, S - 3)
        ring(c, x, y, 1.6, 1, '#a85a2a')
    return c


@tex('jus_orange')
def jus_orange():
    c = new('#f8a020')
    noise(c, ['#f8b040', '#e89018'], 0.2, 28)
    c.fill(rect(4, 3, 9, 3), '#fbd070')
    return c


@tex('cocktail')
def cocktail():
    c = new('#f06a3a')
    for y in range(S):
        c.px[y, :, :3] = mix('#f8c020', '#e0303a', y / S)
    return c


@tex('lait')
def lait():
    c = new('#fbfaf4')
    noise(c, ['#f0ece2'], 0.1, 29)
    return c


@tex('sucre_glace')
def sucre_glace():
    c = new('#ffffff')
    noise(c, ['#f0f0f0', '#e4e4e4'], 0.2, 30)
    return c


# ============================================================================ sprites (plans en croix)
@tex('flamme')
def flamme():
    c = Canvas()
    for (cx, h, w) in [(8, 20, 6), (16, 28, 8), (24, 18, 6)]:
        m = polygon([(cx - w / 2, 32), (cx - w / 2 + 1, 32 - h * 0.6), (cx, 32 - h), (cx + w / 2 - 1, 32 - h * 0.6), (cx + w / 2, 32)])
        c.fill(m, '#e8501a')
        c.fill(shift(m, 0, 3) & m | polygon([(cx - w / 3, 32), (cx, 32 - h * 0.7), (cx + w / 3, 32)]), '#f8a030')
        c.fill(polygon([(cx - w / 5, 32), (cx, 32 - h * 0.4), (cx + w / 5, 32)]), '#fff0a0')
    return c


@tex('fouet')
def fouet():
    c = Canvas()
    c.fill(rect(15, 0, 16, 11), '#3a3c42')
    c.fill(rect(15, 0, 15, 11), '#6e727a')
    for w in (4, 7, 10):
        c.fill(ellipse(16, 22, w, 10) & ~ellipse(16, 22, w - 1, 9) & (YY > 11), '#c8ccd2')
    return c


@tex('vapeur')
def vapeur():
    c = Canvas()
    for (x, ph) in [(10, 0), (20, 2)]:
        for y in range(4, 30):
            xx = x + math.sin(y * 0.45 + ph) * 2.5
            a = y / 30
            c.px[y, int(xx), :3] = (240, 244, 248)
            c.px[y, int(xx), 3] = int(200 * a)
    return c


@tex('herbes')
def herbes_sprite():
    c = Canvas()
    for (x0, x1, y1) in [(16, 8, 6), (16, 24, 8), (16, 15, 2)]:
        c.fill(line_mask(16, 31, x1, y1 + 6), '#4a7a2a')
        c.fill(ellipse(x1, y1 + 3, 3, 4), '#3a9a2a')
        c.fill(ellipse(x1 - 1, y1 + 2, 1.5, 2), '#6ac04a')
    return c


def save_all(out_dir):
    os.makedirs(out_dir, exist_ok=True)
    for name, fn in TEX.items():
        c = fn()
        c.image().save(os.path.join(out_dir, name + '.png'))
