"""
Petit atelier de modèles GeckoLib (format Bedrock « geo » 1.12.0) : os, cubes en UV « boîte », rangement
de la texture, peinture procédurale en 3D, export du .geo.json et du .animation.json, et un aperçu qui
reproduit exactement les calculs de GeckoLib 3.0.31 pour 1.12.2 (axe x inversé, rotations x et y
négatives, ordre Z·Y·X, pivots absolus, coordonnées de texture de GeoQuad).

Conventions du modèle (celles de Blockbench) : unités = pixels (16 = un bloc), y vers le haut, origine
aux pieds de l'entité, l'avant regarde vers -z, +x est le flanc GAUCHE de l'animal.
"""
import json
import math
import re

import numpy as np
from PIL import Image


# ============================================================================ modèle
class Cube:
    def __init__(self, origine, taille, peintre, rot=None, pivot=None, nom=''):
        self.origine = [float(v) for v in origine]
        self.taille = [int(v) for v in taille]
        assert all(v >= 0 for v in self.taille), taille
        self.peintre = peintre
        self.rot = rot
        self.pivot = pivot
        self.nom = nom
        self.uv = None

    def region(self):
        w, h, d = self.taille
        return 2 * (d + w), d + h


class Os:
    def __init__(self, nom, pivot, rot=(0, 0, 0), parent=None):
        self.nom = nom
        self.pivot = [float(v) for v in pivot]
        self.rot = [float(v) for v in rot]
        self.parent = parent
        self.cubes = []
        self.enfants = []

    def cube(self, origine, taille, peintre, rot=None, pivot=None, nom=''):
        self.cubes.append(Cube(origine, taille, peintre, rot, pivot, nom or self.nom))
        return self

    def os(self, nom, pivot, rot=(0, 0, 0)):
        o = Os(nom, pivot, rot, self)
        self.enfants.append(o)
        return o


class Modele:
    def __init__(self, ident):
        self.ident = ident
        self.racines = []
        self.tw = self.th = 0

    def os(self, nom, pivot, rot=(0, 0, 0)):
        o = Os(nom, pivot, rot)
        self.racines.append(o)
        return o

    def tous(self):
        out = []

        def visiter(o):
            out.append(o)
            for e in o.enfants:
                visiter(e)
        for r in self.racines:
            visiter(r)
        return out

    def cubes(self):
        return [c for o in self.tous() for c in o.cubes]

    def ranger(self, largeur=256):
        """Range les régions UV en étagères ; hauteur de texture = puissance de deux suffisante."""
        cs = sorted(self.cubes(), key=lambda c: (-c.region()[1], -c.region()[0]))
        x = y = rang = 0
        for c in cs:
            rw, rh = c.region()
            assert rw <= largeur, (c.nom, rw)
            if x + rw > largeur:
                x, y, rang = 0, y + rang, 0
            c.uv = (x, y)
            x += rw
            rang = max(rang, rh)
        h = 32
        while h < y + rang:
            h *= 2
        self.tw, self.th = largeur, h

    # ------------------------------------------------------------------ export
    def geo_json(self):
        bones = []
        for o in self.tous():
            b = {'name': o.nom, 'pivot': o.pivot}
            if o.parent:
                b['parent'] = o.parent.nom
            if any(o.rot):
                b['rotation'] = o.rot
            if o.cubes:
                cs = []
                for c in o.cubes:
                    d = {'origin': c.origine, 'size': c.taille, 'uv': list(c.uv)}
                    if c.rot:
                        d['rotation'] = c.rot
                        d['pivot'] = c.pivot
                    cs.append(d)
                b['cubes'] = cs
            bones.append(b)
        return {
            'format_version': '1.12.0',
            'minecraft:geometry': [{
                'description': {
                    'identifier': 'geometry.' + self.ident,
                    'texture_width': self.tw,
                    'texture_height': self.th,
                    'visible_bounds_width': 8,
                    'visible_bounds_height': 5,
                    'visible_bounds_offset': [0, 1.5, 0],
                },
                'bones': bones,
            }],
        }


# ============================================================================ faces et UV (GeoCube / GeoQuad)
def _sommets(c):
    """Les huit coins dans l'espace de GeckoLib (blocs), comme GeoCube.createFromPojoCube."""
    ox, oy, oz = c.origine
    sx, sy, sz = [v / 16 for v in c.taille]
    x0, y0, z0 = -(ox + c.taille[0]) / 16, oy / 16, oz / 16
    P = {
        1: (x0, y0, z0), 2: (x0, y0, z0 + sz), 3: (x0, y0 + sy, z0), 4: (x0, y0 + sy, z0 + sz),
        5: (x0 + sx, y0, z0), 6: (x0 + sx, y0, z0 + sz), 7: (x0 + sx, y0 + sy, z0), 8: (x0 + sx, y0 + sy, z0 + sz),
    }
    return {k: np.array(v) for k, v in P.items()}


def faces(c):
    """[(nom, [4 sommets], [4 uv en pixels], normale GeckoLib)] en UV boîte, sans miroir."""
    u, v = c.uv
    w, h, d = c.taille
    P = _sommets(c)

    def quad(ids, u1, v1, us, vs):
        u2, v2 = u1 + us, v1 + vs
        return [P[i] for i in ids], [(u2, v1), (u1, v1), (u1, v2), (u2, v2)]
    out = []
    for nom, ids, rect, n in (
            ('ouest', (4, 3, 1, 2), (u + d + w, v + d, d, h), (-1, 0, 0)),
            ('est', (7, 8, 6, 5), (u, v + d, d, h), (1, 0, 0)),
            ('nord', (3, 7, 5, 1), (u + d, v + d, w, h), (0, 0, -1)),
            ('sud', (8, 4, 2, 6), (u + d + w + d, v + d, w, h), (0, 0, 1)),
            ('haut', (4, 8, 7, 3), (u + d, v, w, d), (0, 1, 0)),
            ('bas', (2, 6, 5, 1), (u + d + w, v, w, d), (0, -1, 0))):
        pts, uvs = quad(ids, *rect)
        out.append((nom, pts, uvs, np.array(n, float), rect))
    return out


# Sens « Blockbench » des faces GeckoLib : est = flanc droit (-x), ouest = flanc gauche (+x), nord = avant.
COTE = {'ouest': 'gauche', 'est': 'droite', 'nord': 'avant', 'sud': 'arriere', 'haut': 'haut', 'bas': 'bas'}


def peindre(m, frame=0):
    """Texture : chaque texel reçoit la couleur que le peintre du cube donne au point 3D (coordonnées
    Blockbench) qu'il recouvre. peintre(p, cote, cube, frame) -> (r, g, b, a) ou None (transparent)."""
    img = np.zeros((m.th, m.tw, 4), float)
    for c in m.cubes():
        for nom, pts, uvs, n, (ru, rv, rw, rh) in faces(c):
            if rw <= 0 or rh <= 0:
                continue
            # interpolation bilinéaire : coin (u2,v1)=pts[0], (u1,v1)=pts[1], (u1,v2)=pts[2], (u2,v2)=pts[3]
            for j in range(int(rh)):
                for i in range(int(rw)):
                    a = (i + 0.5) / rw   # 0 au bord u1
                    b = (j + 0.5) / rh   # 0 au bord v1
                    haut = pts[1] * (1 - a) + pts[0] * a
                    bas = pts[2] * (1 - a) + pts[3] * a
                    g = haut * (1 - b) + bas * b
                    p = np.array([-g[0] * 16, g[1] * 16, g[2] * 16])   # retour en Blockbench
                    col = c.peintre(p, COTE[nom], c, frame)
                    if col is None:
                        continue
                    img[int(rv) + j, int(ru) + i] = col
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), 'RGBA')


# ============================================================================ molang (ce dont on se sert)
def evaluer(expr, t):
    if isinstance(expr, (int, float)):
        return float(expr)
    e = expr.replace('query.anim_time', '(%r)' % t)
    e = re.sub(r'math\.sin\(', '_sin(', e)
    e = re.sub(r'math\.cos\(', '_cos(', e)
    e = re.sub(r'math\.abs\(', 'abs(', e)
    e = re.sub(r'math\.clamp\(', '_clamp(', e)
    e = re.sub(r'math\.min\(', 'min(', e)
    e = re.sub(r'math\.max\(', 'max(', e)
    return float(eval(e, {'_sin': lambda d: math.sin(math.radians(d)), '_cos': lambda d: math.cos(math.radians(d)),
                          '_clamp': lambda v, a, b: max(a, min(b, v)), 'abs': abs, 'min': min, 'max': max}))


def canal(valeur, t, longueur=None):
    """Valeur d'un canal (rotation, position, échelle) au temps t : vecteur constant ou images-clés."""
    if isinstance(valeur, dict):
        cles = sorted(((float(k), v) for k, v in valeur.items()), key=lambda kv: kv[0])
        if longueur and t > longueur:
            t = longueur
        if t <= cles[0][0]:
            return [evaluer(x, t) for x in _vec(cles[0][1])]
        for (t0, v0), (t1, v1) in zip(cles, cles[1:]):
            if t0 <= t <= t1:
                k = (t - t0) / max(t1 - t0, 1e-9)
                a, b = [evaluer(x, t) for x in _vec(v0)], [evaluer(x, t) for x in _vec(v1)]
                return [a[i] + (b[i] - a[i]) * k for i in range(3)]
        return [evaluer(x, t) for x in _vec(cles[-1][1])]
    return [evaluer(x, t) for x in valeur]


def _vec(v):
    return v['vector'] if isinstance(v, dict) else v


# ============================================================================ pose et rendu (IGeoRenderer / MatrixStack)
def _rx(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1]])


def _ry(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1]])


def _rz(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]])


def _tr(x, y, z):
    m = np.eye(4)
    m[:3, 3] = (x, y, z)
    return m


def _sc(x, y, z):
    return np.diag([x, y, z, 1.0])


def poser(m, anim=None, t=0.0, caches=(), longueur=None):
    """Triangles texturés (points monde en blocs, uv, normale) pour la pose donnée. Le monde est celui du
    rendu en jeu avec un lacet de 0 : GeoEntityRenderer tourne de 180° autour de y."""
    os_anim = (anim or {}).get('bones', {})
    quads = []
    monde = _ry(math.pi)

    def visiter(o, M):
        a = os_anim.get(o.nom, {})
        rot = canal(a['rotation'], t, longueur) if 'rotation' in a else [0, 0, 0]
        pos = canal(a['position'], t, longueur) if 'position' in a else [0, 0, 0]
        ech = canal(a['scale'], t, longueur) if 'scale' in a else [1, 1, 1]
        if isinstance(ech, list) and len(ech) == 1:
            ech = ech * 3
        rx = math.radians(-(o.rot[0] + rot[0]))
        ry = math.radians(-(o.rot[1] + rot[1]))
        rz = math.radians(o.rot[2] + rot[2])
        px, py, pz = -o.pivot[0] / 16, o.pivot[1] / 16, o.pivot[2] / 16
        M = M @ _tr(-pos[0] / 16, pos[1] / 16, pos[2] / 16)
        M = M @ _tr(px, py, pz) @ _rz(rz) @ _ry(ry) @ _rx(rx) @ _sc(*ech) @ _tr(-px, -py, -pz)
        if o.nom in caches:
            return
        for c in o.cubes:
            Mc = M
            if c.rot:
                cp = (-c.pivot[0] / 16, c.pivot[1] / 16, c.pivot[2] / 16)
                Mc = M @ _tr(*cp) @ _rz(math.radians(c.rot[2])) @ _ry(math.radians(-c.rot[1])) @ _rx(math.radians(-c.rot[0])) @ _tr(*[-v for v in cp])
            for nom, pts, uvs, n, rect in faces(c):
                if rect[2] <= 0 or rect[3] <= 0:
                    continue
                W = [(monde @ Mc @ np.append(p, 1))[:3] for p in pts]
                nn = (monde @ Mc @ np.append(n, 0))[:3]
                quads.append((W, uvs, nn))
        for e in o.enfants:
            visiter(e, M)
    for r in m.racines:
        visiter(r, np.eye(4))
    return quads


def rendu(m, tex, quads, taille=360, lacet=35, tangage=20, centre=None, etendue=None, eclairage=True, fond=None):
    """Projection orthographique avec z-buffer ; lacet/tangage de la caméra en degrés (lacet 0 = de face)."""
    T = np.asarray(tex).astype(float) / 255.0
    th, tw = T.shape[:2]
    img = np.zeros((taille, taille, 4)) if fond is None else np.tile(np.array(fond, float) / 255.0, (taille, taille, 1))
    zb = np.full((taille, taille), np.inf)
    # caméra au sud de l'animal (qui regarde +z), tournée vers le nord quand lacet = 0 ; x écran = x monde
    V = (_rx(math.radians(tangage)) @ _ry(math.radians(lacet)))[:3, :3]
    allp = np.array([p for W, _, _ in quads for p in W])
    if centre is None:
        centre = (allp.max(0) + allp.min(0)) / 2
    if etendue is None:
        etendue = (allp.max(0) - allp.min(0)).max()
    sc = taille * 0.86 / max(etendue, 1e-6)
    lum = np.array([0.35, 0.8, -0.45])
    lum = lum / np.linalg.norm(lum)
    for W, uvs, n in quads:
        P = [V @ (p - centre) for p in W]
        S = [(q[0] * sc + taille / 2, -q[1] * sc + taille / 2, -q[2]) for q in P]
        nv = V @ n
        ombre = 1.0
        if eclairage:
            ombre = 0.62 + 0.38 * abs(float(np.dot(n, lum)))
        for tri in ((0, 1, 2), (0, 2, 3)):
            a, b, c = [S[i] for i in tri]
            ua, ub, uc = [np.array(uvs[i], float) for i in tri]
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


# ============================================================================ contrôles
def coplanaires(m, anim=None, t=0.0, caches=()):
    """Faces de même orientation, dans le même plan et qui se recouvrent (z-fighting) dans une pose."""
    qs = poser(m, anim, t, caches)
    prob = []
    for i in range(len(qs)):
        Wi, _, ni = qs[i]
        for j in range(i + 1, len(qs)):
            Wj, _, nj = qs[j]
            if np.dot(ni, nj) < 0.999:
                continue
            if abs(np.dot(ni, Wi[0] - Wj[0])) > 1e-4:
                continue
            # recouvrement des boîtes englobantes dans le plan
            a0, a1 = np.min(Wi, 0), np.max(Wi, 0)
            b0, b1 = np.min(Wj, 0), np.max(Wj, 0)
            ov = np.minimum(a1, b1) - np.maximum(a0, b0)
            axes = [k for k in range(3) if abs(ni[k]) < 0.5]
            if all(ov[k] > 1e-4 for k in axes):
                prob.append((i, j))
    return prob


def ecrire_json(chemin, obj):
    with open(chemin, 'w') as f:
        json.dump(obj, f, indent=1, ensure_ascii=False)
        f.write('\n')
