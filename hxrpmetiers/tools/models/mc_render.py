"""
Rendu logiciel des modèles de blocs Minecraft 1.12 (format JSON à éléments), pour vérifier
les stations sans lancer le jeu. Reproduit les règles de Minecraft :
UV par défaut projetées depuis la position de l'élément, rotation d'élément (règle de la main droite,
origine, rescale), rotation de face par pas de 90°, ombrage fixe par orientation, découpe alpha.
"""
import json
import math
import os

import numpy as np
from PIL import Image

FACES = ('north', 'south', 'west', 'east', 'up', 'down')


def load_model(models_dir, name, textures_dir):
    """Charge un modèle et résout les parents (dans models_dir) et les variables de textures."""
    chain = []
    n = name
    while n:
        path = os.path.join(models_dir, n.split(':')[-1].replace('block/', '') + '.json')
        if not os.path.exists(path):
            break
        m = json.load(open(path))
        chain.append(m)
        n = m.get('parent')
        if n and not n.startswith('hxrpmetiers:'):
            break
    tex = {}
    elements = None
    for m in reversed(chain):
        tex.update(m.get('textures', {}))
        if 'elements' in m:
            elements = m['elements']
    def resolve(v, depth=0):
        while v.startswith('#') and depth < 10:
            v = tex.get(v[1:], v)
            depth += 1
        return v
    images = {}
    for k, v in tex.items():
        rv = resolve(v)
        p = os.path.join(textures_dir, rv.split(':')[-1] + '.png')
        if os.path.exists(p):
            images['#' + k] = np.asarray(Image.open(p).convert('RGBA')).astype(float) / 255.0
    return elements or [], images


def default_uv(face, f, t):
    fx, fy, fz = f
    tx, ty, tz = t
    return {
        'north': [16 - tx, 16 - ty, 16 - fx, 16 - fy],
        'south': [fx, 16 - ty, tx, 16 - fy],
        'west': [fz, 16 - ty, tz, 16 - fy],
        'east': [16 - tz, 16 - ty, 16 - fz, 16 - fy],
        'up': [fx, fz, tx, tz],
        'down': [fx, 16 - tz, tx, 16 - fz],
    }[face]


def face_corners(face, f, t):
    """Coins de la face dans l'ordre : haut-gauche, haut-droite, bas-droite, bas-gauche (vus de l'extérieur,
    orientés comme la texture)."""
    x0, y0, z0 = f
    x1, y1, z1 = t
    if face == 'north':
        return [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)]
    if face == 'south':
        return [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)]
    if face == 'west':
        return [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)]
    if face == 'east':
        return [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)]
    if face == 'up':
        return [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)]
    return [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)]


NORMALS = {'north': (0, 0, -1), 'south': (0, 0, 1), 'west': (-1, 0, 0), 'east': (1, 0, 0), 'up': (0, 1, 0), 'down': (0, -1, 0)}


def rot_matrix(axis, deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    if axis == 'x':
        return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])
    if axis == 'y':
        return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def shade_for(n):
    nx, ny, nz = n
    up = 1.0 if ny > 0 else 0.5
    return nx * nx * 0.6 + ny * ny * up + nz * nz * 0.8


def render(elements, images, size=256, yaw=45, pitch=30, scale=None, bg=(0, 0, 0, 0), model_rot_y=0):
    """Rendu orthographique. yaw 45 / pitch 30 : caméra au nord-ouest, en hauteur, comme la vue
    d'inventaire (face nord à gauche, face ouest à droite). yaw 225 : vue de dos (sud-est)."""
    img = np.zeros((size, size, 4))
    img[:, :] = np.array(bg) / 255.0
    zbuf = np.full((size, size), np.inf)
    # caméra : on fait tourner le modèle autour du centre puis on projette
    Ry = rot_matrix('y', -yaw)
    Rx = rot_matrix('x', -pitch)
    Rm = rot_matrix('y', -model_rot_y)
    view = Rx @ Ry
    sc = scale or size / 26.0
    quads = []
    for el in elements:
        f, t = el['from'], el['to']
        R = None
        if 'rotation' in el:
            r = el['rotation']
            R = rot_matrix(r['axis'], r['angle'])
            origin = np.array(r['origin'], float)
            resc = np.ones(3)
            if r.get('rescale'):
                k = 1 / math.cos(math.radians(abs(r['angle'])))
                resc = np.array([k if a != r['axis'] else 1 for a in 'xyz'])
        for face, fd in el.get('faces', {}).items():
            texkey = fd.get('texture')
            if texkey not in images:
                continue
            corners = np.array(face_corners(face, f, t), float)
            n = np.array(NORMALS[face], float)
            if R is not None:
                corners = (corners - origin) * resc
                corners = corners @ R.T + origin
                n = R @ n
            corners = (corners - 8) @ Rm.T
            n = Rm @ n
            uv = fd.get('uv') or default_uv(face, f, t)
            u0, v0, u1, v1 = uv
            uvc = [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]
            rot = int(fd.get('rotation', 0)) // 90
            uvc = uvc[-rot:] + uvc[:-rot] if rot else uvc
            p = corners @ view.T
            quads.append((p, uvc, images[texkey], shade_for(n) if el.get('shade', True) else 1.0))
    for (p, uvc, tex, shd) in quads:
        th, tw = tex.shape[:2]
        pts2 = np.stack([-p[:, 0] * sc + size / 2, -p[:, 1] * sc + size / 2], -1)
        depth = p[:, 2]
        for tri in ((0, 1, 2), (0, 2, 3)):
            a, b, c = [pts2[i] for i in tri]
            da, db, dc = [depth[i] for i in tri]
            ua, ub, uc = [np.array(uvc[i]) for i in tri]
            minx, maxx = int(max(0, math.floor(min(a[0], b[0], c[0])))), int(min(size - 1, math.ceil(max(a[0], b[0], c[0]))))
            miny, maxy = int(max(0, math.floor(min(a[1], b[1], c[1])))), int(min(size - 1, math.ceil(max(a[1], b[1], c[1]))))
            if maxx < minx or maxy < miny:
                continue
            det = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1])
            if abs(det) < 1e-9:
                continue
            ys, xs = np.mgrid[miny:maxy + 1, minx:maxx + 1]
            px, py = xs + 0.5, ys + 0.5
            w0 = ((b[1] - c[1]) * (px - c[0]) + (c[0] - b[0]) * (py - c[1])) / det
            w1 = ((c[1] - a[1]) * (px - c[0]) + (a[0] - c[0]) * (py - c[1])) / det
            w2 = 1 - w0 - w1
            inside = (w0 >= -1e-6) & (w1 >= -1e-6) & (w2 >= -1e-6)
            if not inside.any():
                continue
            d = w0 * da + w1 * db + w2 * dc
            u = w0 * ua[0] + w1 * ub[0] + w2 * uc[0]
            v = w0 * ua[1] + w1 * ub[1] + w2 * uc[1]
            tx = np.clip((u / 16.0 * tw).astype(int), 0, tw - 1)
            ty = np.clip((v / 16.0 * th).astype(int), 0, th - 1)
            col = tex[ty, tx]
            ok = inside & (col[..., 3] > 0.1) & (d < zbuf[ys, xs])
            zbuf[ys[ok], xs[ok]] = d[ok]
            cc = col[ok].copy()
            cc[:, :3] *= shd
            cc[:, 3] = 1
            img[ys[ok], xs[ok]] = cc
    return Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8), 'RGBA')
