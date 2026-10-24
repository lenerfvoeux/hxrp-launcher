"""
Aperçu d'un modèle GeckoLib (.geo.json à UV par face + .animation.json + texture) calculé exactement comme
GeckoLib 3.0.31 pour 1.12.2 : coins et UV de GeoCube/GeoQuad, rotations x et y inversées, ordre Z·Y·X, pivots,
position x inversée, rotation de 180° du rendu d'entité.

    python3 rendu.py dossier_assets identifiant sortie.png [anim:t:lacet:tangage ...]

Sans pose, dessine le modèle au repos sous quatre angles.
"""
import json
import math
import os
import re
import sys

import numpy as np
from PIL import Image, ImageDraw


def charger(dossier, ident):
    geo = json.load(open(os.path.join(dossier, 'geo', ident + '.geo.json'), encoding='utf-8'))['minecraft:geometry'][0]
    an_p = os.path.join(dossier, 'animations', ident + '.animation.json')
    anims = json.load(open(an_p, encoding='utf-8'))['animations'] if os.path.exists(an_p) else {}
    tex = Image.open(os.path.join(dossier, 'textures', 'entity', ident + '.png')).convert('RGBA')
    return geo, anims, tex


# ---------------------------------------------------------------- molang réduit
def evaluer(expr, t):
    if isinstance(expr, (int, float)):
        return float(expr)
    e = expr.replace('query.anim_time', '(%r)' % t).replace('q.anim_time', '(%r)' % t)
    e = re.sub(r'math\.(sin|cos)\(', r'_\1(', e)
    e = re.sub(r'math\.(abs|min|max|clamp|sqrt|pow|floor|ceil|round)\(', r'_\1(', e)
    env = {'_sin': lambda d: math.sin(math.radians(d)), '_cos': lambda d: math.cos(math.radians(d)), '_abs': abs, '_min': min,
           '_max': max, '_clamp': lambda v, a, b: max(a, min(b, v)), '_sqrt': math.sqrt, '_pow': math.pow, '_floor': math.floor,
           '_ceil': math.ceil, '_round': round}
    try:
        return float(eval(e, {'__builtins__': {}}, env))
    except Exception:
        return 0.0


def vec(v):
    return v['vector'] if isinstance(v, dict) else v


def canal(valeur, t, longueur=None):
    if isinstance(valeur, dict):
        cles = sorted(((float(k), v) for k, v in valeur.items()), key=lambda kv: kv[0])
        if longueur and t > longueur:
            t = longueur
        if t <= cles[0][0]:
            return [evaluer(x, t) for x in vec(cles[0][1])]
        for (t0, v0), (t1, v1) in zip(cles, cles[1:]):
            if t0 <= t <= t1:
                k = (t - t0) / max(t1 - t0, 1e-9)
                a, b = [evaluer(x, t) for x in vec(v0)], [evaluer(x, t) for x in vec(v1)]
                return [a[i] + (b[i] - a[i]) * k for i in range(3)]
        return [evaluer(x, t) for x in vec(cles[-1][1])]
    if isinstance(valeur, (int, float, str)):
        v = evaluer(valeur, t)
        return [v, v, v]
    return [evaluer(x, t) for x in valeur]


# ---------------------------------------------------------------- matrices
def rx(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1]])


def ry(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1]])


def rz(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]])


def tr(x, y, z):
    m = np.eye(4)
    m[:3, 3] = (x, y, z)
    return m


def sc(x, y, z):
    return np.diag([x, y, z, 1.0])


# ---------------------------------------------------------------- GeoCube / GeoQuad
def quads_cube(c, tw, th):
    sx, sy, sz = [v / 16 for v in c['size']]
    ox, oy, oz = c['origin']
    x0, y0, z0 = -(ox + c['size'][0]) / 16, oy / 16, oz / 16
    i = c.get('inflate', 0) / 16
    P = {1: (x0 - i, y0 - i, z0 - i), 2: (x0 - i, y0 - i, z0 + sz + i), 3: (x0 - i, y0 + sy + i, z0 - i), 4: (x0 - i, y0 + sy + i, z0 + sz + i),
         5: (x0 + sx + i, y0 - i, z0 - i), 6: (x0 + sx + i, y0 - i, z0 + sz + i), 7: (x0 + sx + i, y0 + sy + i, z0 - i),
         8: (x0 + sx + i, y0 + sy + i, z0 + sz + i)}
    P = {k: np.array(v) for k, v in P.items()}
    out = []
    for nom, ids, n in (('west', (4, 3, 1, 2), (-1, 0, 0)), ('east', (7, 8, 6, 5), (1, 0, 0)), ('north', (3, 7, 5, 1), (0, 0, -1)),
                        ('south', (8, 4, 2, 6), (0, 0, 1)), ('up', (4, 8, 7, 3), (0, 1, 0)), ('down', (1, 5, 6, 2), (0, -1, 0))):
        f = c.get('uv', {}).get(nom)
        if not f:
            continue
        u1, v1 = f['uv']
        u2, v2 = u1 + f['uv_size'][0], v1 + f['uv_size'][1]
        uvs = [(u2, v1), (u1, v1), (u1, v2), (u2, v2)]
        # en pixels de la texture
        out.append(([P[k] for k in ids], [(u * tw, v * th) for u, v in uvs], np.array(n, float)))
    return out


def poser(geo, anim=None, t=0.0, longueur=None):
    tw0, th0 = geo['description']['texture_width'], geo['description']['texture_height']
    os_ = {b['name']: b for b in geo['bones']}
    enfants = {}
    racines = []
    for b in geo['bones']:
        if b.get('parent') in os_:
            enfants.setdefault(b['parent'], []).append(b)
        else:
            racines.append(b)
    a_os = (anim or {}).get('bones', {})
    monde = ry(math.pi)
    quads = []

    def visiter(b, M):
        a = a_os.get(b['name'], {})
        rot = canal(a['rotation'], t, longueur) if 'rotation' in a else [0, 0, 0]
        pos = canal(a['position'], t, longueur) if 'position' in a else [0, 0, 0]
        ech = canal(a['scale'], t, longueur) if 'scale' in a else [1, 1, 1]
        r0 = b.get('rotation', [0, 0, 0])
        piv = b.get('pivot', [0, 0, 0])
        px, py, pz = -piv[0] / 16, piv[1] / 16, piv[2] / 16
        M = M @ tr(-pos[0] / 16, pos[1] / 16, pos[2] / 16)
        M = M @ tr(px, py, pz) @ rz(math.radians(r0[2] + rot[2])) @ ry(math.radians(-(r0[1] + rot[1]))) \
            @ rx(math.radians(-(r0[0] + rot[0]))) @ sc(*ech) @ tr(-px, -py, -pz)
        for c in b.get('cubes', []):
            Mc = M
            if c.get('rotation'):
                cr, cp = c['rotation'], c.get('pivot', [0, 0, 0])
                p = (-cp[0] / 16, cp[1] / 16, cp[2] / 16)
                Mc = M @ tr(*p) @ rz(math.radians(cr[2])) @ ry(math.radians(-cr[1])) @ rx(math.radians(-cr[0])) @ tr(*[-v for v in p])
            for pts, uvs, n in quads_cube(c, 1 / tw0, 1 / th0):
                W = [(monde @ Mc @ np.append(p, 1))[:3] for p in pts]
                nn = (monde @ Mc @ np.append(n, 0))[:3]
                quads.append((W, uvs, nn))
        for e in enfants.get(b['name'], []):
            visiter(e, M)
    for r in racines:
        visiter(r, np.eye(4))
    return quads


def rendu(tex, quads, taille=360, lacet=35, tangage=20, centre=None, etendue=None, fond=(40, 46, 60, 255)):
    T = np.asarray(tex).astype(float) / 255.0
    th, tw = T.shape[:2]
    img = np.tile(np.array(fond, float) / 255.0, (taille, taille, 1))
    zb = np.full((taille, taille), np.inf)
    V = (rx(math.radians(tangage)) @ ry(math.radians(lacet)))[:3, :3]
    allp = np.array([p for W, _, _ in quads for p in W])
    if centre is None:
        centre = (allp.max(0) + allp.min(0)) / 2
    if etendue is None:
        etendue = (allp.max(0) - allp.min(0)).max()
    s = taille * 0.86 / max(etendue, 1e-6)
    lum = np.array([0.35, 0.8, -0.45])
    lum /= np.linalg.norm(lum)
    for W, uvs, n in quads:
        P = [V @ (p - centre) for p in W]
        S = [(q[0] * s + taille / 2, -q[1] * s + taille / 2, -q[2]) for q in P]
        nl = np.linalg.norm(n)
        ombre = 0.62 + 0.38 * abs(float(np.dot(n / max(nl, 1e-9), lum)))
        UV = [(u * tw, v * th) for u, v in uvs]
        for tri in ((0, 1, 2), (0, 2, 3)):
            a, b, c = [S[i] for i in tri]
            ua, ub, uc = [np.array(UV[i], float) for i in tri]
            minx, maxx = int(max(0, math.floor(min(a[0], b[0], c[0])))), int(min(taille - 1, math.ceil(max(a[0], b[0], c[0]))))
            miny, maxy = int(max(0, math.floor(min(a[1], b[1], c[1])))), int(min(taille - 1, math.ceil(max(a[1], b[1], c[1]))))
            if maxx < minx or maxy < miny:
                continue
            det = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1])
            if abs(det) < 1e-12:
                continue
            ys, xs = np.mgrid[miny:maxy + 1, minx:maxx + 1]
            px_, py_ = xs + 0.5, ys + 0.5
            w0 = ((b[1] - c[1]) * (px_ - c[0]) + (c[0] - b[0]) * (py_ - c[1])) / det
            w1 = ((c[1] - a[1]) * (px_ - c[0]) + (a[0] - c[0]) * (py_ - c[1])) / det
            w2 = 1 - w0 - w1
            dedans = (w0 >= -1e-6) & (w1 >= -1e-6) & (w2 >= -1e-6)
            if not dedans.any():
                continue
            prof = w0 * a[2] + w1 * b[2] + w2 * c[2]
            uu = w0 * ua[0] + w1 * ub[0] + w2 * uc[0]
            vv = w0 * ua[1] + w1 * ub[1] + w2 * uc[1]
            tx = np.clip(np.floor(uu).astype(int), 0, tw - 1)
            ty = np.clip(np.floor(vv).astype(int), 0, th - 1)
            col = T[ty, tx]
            ok = dedans & (col[..., 3] > 0.1) & (prof < zb[ys, xs])
            zb[ys[ok], xs[ok]] = prof[ok]
            cc = col[ok].copy()
            cc[:, :3] *= ombre
            cc[:, 3] = 1
            img[ys[ok], xs[ok]] = cc
    return Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8), 'RGBA')


def planche(dossier, ident, sortie, poses=None, cell=360):
    geo, anims, tex = charger(dossier, ident)
    if not poses:
        poses = ['repos:0:30:15', 'repos:0:150:15', 'repos:0:270:30', 'repos:0:0:80']
    # cadrage sur la première pose : certaines pièces (rayon, œufs…) sont rangées loin et cachées par l'animation
    an0, t0 = poses[0].split(':')[:2]
    a0 = anims.get('animation.%s.%s' % (ident, an0)) if an0 != 'repos' else None
    repos = [q for q in poser(geo, a0, float(t0), a0.get('animation_length') if a0 else None)
             if (np.ptp(np.array(q[0]), axis=0) > 1e-6).sum() >= 2]
    allp = np.array([p for W, _, _ in repos for p in W])
    centre = (allp.max(0) + allp.min(0)) / 2
    etendue = (allp.max(0) - allp.min(0)).max() * 1.15
    img = Image.new('RGBA', (cell * len(poses), cell), (40, 46, 60, 255))
    d = ImageDraw.Draw(img)
    for k, p in enumerate(poses):
        an, t, la, ta = p.split(':')
        a = anims.get('animation.%s.%s' % (ident, an)) if an != 'repos' else None
        q = poser(geo, a, float(t), a.get('animation_length') if a else None)
        img.alpha_composite(rendu(tex, q, cell, float(la), float(ta), centre, etendue), (k * cell, 0))
        d.text((6, 6), '', fill=(255, 255, 255))
        d.text((k * cell + 6, 6), p, fill=(255, 255, 255, 255))
    img.save(sortie)


if __name__ == '__main__':
    planche(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4:])
