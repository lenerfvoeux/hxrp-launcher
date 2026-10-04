"""
Les 12 stations de cuisine + frigo + poubelle, en vrais modèles 3D à éléments.
La face avant de chaque modèle regarde le NORD (convention Minecraft) ; le blockstate la tourne
vers le joueur à la pose.

Faces peintes : une texture « vue de face » (nord), « vue de côté », « vue de dessus » est projetée
sur tous les éléments concernés (UV par défaut = position de l'élément), comme un dessin technique.
"""
import json
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from materials import *  # noqa: E402,F401,F403
import materials  # noqa: E402

MODID = 'hxrpmetiers'
MODELS = {}


# ============================================================================ DSL des modèles
FACE_KEYS = {'n': 'north', 's': 'south', 'w': 'west', 'e': 'east', 'u': 'up', 'd': 'down'}


def proj_uv(face, f, t):
    fx, fy, fz = f
    tx, ty, tz = t
    uv = {
        'north': [16 - tx, 16 - ty, 16 - fx, 16 - fy],
        'south': [fx, 16 - ty, tx, 16 - fy],
        'west': [fz, 16 - ty, tz, 16 - fy],
        'east': [16 - tz, 16 - ty, 16 - fz, 16 - fy],
        'up': [fx, fz, tx, tz],
        'down': [fx, 16 - tz, tx, 16 - fz],
    }[face]
    return _fit(uv)


def size_uv(face, f, t, off=(0, 0)):
    dx, dy, dz = (abs(t[i] - f[i]) for i in range(3))
    w, h = {'north': (dx, dy), 'south': (dx, dy), 'west': (dz, dy), 'east': (dz, dy), 'up': (dx, dz), 'down': (dx, dz)}[face]
    w, h = max(w, 0.0), max(h, 0.0)
    u0, v0 = off
    return _fit([u0, v0, u0 + min(16, w), v0 + min(16, h)])


def _fit(uv):
    u0, v0, u1, v1 = uv
    for (a, b) in ((0, 2), (1, 3)):
        lo, hi = min(uv[a], uv[b]), max(uv[a], uv[b])
        if lo < 0:
            uv[a] -= lo
            uv[b] -= lo
        if hi > 16:
            d = hi - 16
            uv[a] -= d
            uv[b] -= d
        uv[a] = max(0, min(16, uv[a]))
        uv[b] = max(0, min(16, uv[b]))
    return [round(v, 3) for v in uv]


class Model:
    def __init__(self, name, particle, ao=True, light=0, aabb=(0, 0, 0, 16, 16, 16), gui_scale=0.625, gui_dy=0.0):
        self.name = name
        self.textures = {'particle': particle}
        self.elements = []
        self.ao = ao
        self.light = light
        self.aabb = aabb
        self.gui_scale = gui_scale
        self.gui_dy = gui_dy
        MODELS[name] = self

    def _key(self, t):
        k = t.replace('/', '_')
        self.textures[k] = MODID + ':blocks/' + t
        return '#' + k

    def box(self, f, t, tex, faces='nsewud', uv='size', rot=None, uvs=None, shade=True, rotations=None):
        """tex : nom de texture, ou dict {lettre de face: texture}.
        uv : 'size' (matière répétée à densité constante) ou 'proj' (dessin projeté), ou dict par face."""
        f = [round(v, 4) for v in f]
        t = [round(v, 4) for v in t]
        el = {'from': f, 'to': t, 'faces': {}}
        if rot:
            axis, angle, origin = rot
            el['rotation'] = {'origin': list(origin), 'axis': axis, 'angle': angle}
        if not shade:
            el['shade'] = False
        for letter in faces:
            face = FACE_KEYS[letter]
            tname = tex.get(letter, tex.get('*')) if isinstance(tex, dict) else tex
            if tname is None:
                continue
            mode = uv.get(letter, uv.get('*', 'size')) if isinstance(uv, dict) else uv
            if uvs and letter in uvs:
                u = uvs[letter]
            elif mode == 'proj':
                u = proj_uv(face, f, t)
            elif mode == 'full':
                u = [0, 0, 16, 16]
            else:
                u = size_uv(face, f, t)
            fd = {'uv': u, 'texture': self._key(tname)}
            if rotations and letter in rotations:
                fd['rotation'] = rotations[letter]
            if not rot:
                cull = _cull(face, f, t)
                if cull:
                    fd['cullface'] = cull
            el['faces'][face] = fd
        self.elements.append(el)
        return el

    def sprite(self, cx, cz, y0, y1, w, tex):
        """Deux plans en croix (comme les plantes vanilla) : flammes, vapeur, fouet…"""
        h = w / 2
        for ang in (45, -45):
            self.box([cx - h, y0, cz], [cx + h, y1, cz], tex, faces='ns', uv='full', rot=('y', ang, (cx, y0, cz)), shade=False)

    def ring(self, cx, cz, r, y0, y1, th, tex, faces='nsewu', uv='size', a=0.62, b=0.86):
        """Paroi « ronde » à pans (bol, marmite, mortier…) : 4 murs + 4 coins en escalier."""
        A, B = r * a, r * b
        self.box([cx - A, y0, cz - r], [cx + A, y1, cz - r + th], tex, faces, uv)
        self.box([cx - A, y0, cz + r - th], [cx + A, y1, cz + r], tex, faces, uv)
        self.box([cx - r, y0, cz - A], [cx - r + th, y1, cz + A], tex, faces, uv)
        self.box([cx + r - th, y0, cz - A], [cx + r, y1, cz + A], tex, faces, uv)
        for sx in (-1, 1):
            for sz in (-1, 1):
                x0, x1 = sorted((cx + sx * A, cx + sx * B))
                z0, z1 = sorted((cz + sz * A, cz + sz * B))
                self.box([x0, y0, z0], [x1, y1, z1], tex, faces, uv)

    def disc(self, cx, cz, r, y0, y1, tex, faces='nsewud', uv='size', a=0.62, b=0.86):
        """Volume « rond » plein : une croix et quatre coins, en pavés qui ne se chevauchent pas
        (des faces superposées scintilleraient). UV en coordonnées absolues pour que la matière
        se raccorde d'un pavé à l'autre ; 'full' étale la texture sur tout le disque."""
        self.cross(cx, cz, r, r * a, r * b, y0, y1, tex, faces, uv)

    def cross(self, cx, cz, r, A, B, y0, y1, tex, faces='nsewud', uv='size', avant=None):
        """avant : (texture, uv) de la face nord du pavé avant seulement (façade dessinée)."""
        pieces = [(cx - r, cz - A, cx + r, cz + A),
                  (cx - A, cz - r, cx + A, cz - A),
                  (cx - A, cz + A, cx + A, cz + r)]
        for sx in (-1, 1):
            for sz in (-1, 1):
                x0, x1 = sorted((cx + sx * A, cx + sx * B))
                z0, z1 = sorted((cz + sz * A, cz + sz * B))
                pieces.append((x0, z0, x1, z1))
        L, T, D = cx - r, cz - r, 2 * r
        for (x0, z0, x1, z1) in pieces:
            f, t = [x0, y0, z0], [x1, y1, z1]
            uvs = {}
            for letter in faces:
                face = FACE_KEYS[letter]
                mode = uv.get(letter, uv.get('*', 'size')) if isinstance(uv, dict) else uv
                if mode == 'full':
                    X0, X1 = (x0 - L) / D * 16, (x1 - L) / D * 16
                    Z0, Z1 = (z0 - T) / D * 16, (z1 - T) / D * 16
                    uvs[letter] = [round(v, 3) for v in {
                        'north': [16 - X1, 0, 16 - X0, 16], 'south': [X0, 0, X1, 16],
                        'west': [Z0, 0, Z1, 16], 'east': [16 - Z1, 0, 16 - Z0, 16],
                        'up': [X0, Z0, X1, Z1], 'down': [X0, 16 - Z1, X1, 16 - Z0]}[face]]
                else:
                    uvs[letter] = proj_uv(face, f, t)
            tx = tex
            if avant and z0 == cz - r and 'n' in faces:
                tx = {'n': avant[0], '*': tex}
                uvs['n'] = avant[1]
            self.box(f, t, tx, faces, uvs=uvs)

    def json(self):
        d = {'parent': 'block/block', 'ambientocclusion': self.ao, 'textures': self.textures, 'elements': self.elements}
        return d


def _cull(face, f, t):
    if face == 'north' and f[2] == 0:
        return 'north'
    if face == 'south' and t[2] == 16:
        return 'south'
    if face == 'west' and f[0] == 0:
        return 'west'
    if face == 'east' and t[0] == 16:
        return 'east'
    if face == 'down' and f[1] == 0:
        return 'down'
    if face == 'up' and t[1] == 16:
        return 'up'
    return None


# helpers de peinture (face nord : colonne texel = 32 - 2x ; ligne = 32 - 2y)
def NX(x):
    return int(round(32 - 2 * x))


def NY(y):
    return int(round(32 - 2 * y))


def TX(x):
    return int(round(2 * x))


def knob_face(c, cx, cy, r, body='#26272b', ring_col='#c0c4cc', mark='#ffffff', a=-40):
    c.fill(circle(cx, cy, r), ring_col)
    c.fill(circle(cx, cy, r - 1), body)
    c.fill(circle(cx - 0.8, cy - 0.8, r - 2.5), mix(body, '#ffffff', 0.18))
    ang = math.radians(a - 90)
    c.fill(line_mask(cx, cy, cx + math.cos(ang) * (r - 1.2), cy + math.sin(ang) * (r - 1.2)), mark)


# ============================================================================ textures peintes
@tex('bouton')
def bouton():
    c = new('#26272b')
    knob_face(c, 16, 16, 15, a=-45)
    return c


@tex('bouton_laiton')
def bouton_laiton():
    c = new('#9a6a1a')
    knob_face(c, 16, 16, 15, body='#c8922a', ring_col='#f8d880', mark='#3a2408', a=30)
    return c


# ---------------------------------------------------------------- four
@tex('four_front')
def four_front():
    c = materials.acier()
    # bandeau de commande (y 12 -> 15.5)
    box_bevel(c, 1, NY(15.5), 30, NY(12) - 1, '#3a3d44', '#0a0b0d', fill='#16181c')
    c.fill(rect(2, NY(15.5) + 1, 29, NY(15.5) + 1), '#2a2d33')
    # afficheur
    c.fill(rect(12, 3, 19, 6), '#0a0506')
    for (x, y) in [(13, 4), (13, 5), (15, 4), (16, 4), (16, 5), (15, 5), (17, 4), (18, 4), (17, 5), (18, 5)]:
        c.fill(rect(x, y, x, y), '#ff8a1a')
    c.fill(rect(19, 3, 19, 3), '#ff8a1a')
    # voyants
    c.fill(rect(10, 4, 10, 4), '#ff3a2a')
    c.fill(rect(21, 4, 21, 4), '#3aff6a')
    # porte : cadre + hublot
    x0, x1, y0, y1 = NX(15), NX(1) - 1, NY(11.5), NY(1.5) - 1
    box_bevel(c, x0, y0, x1, y1, '#dfe3e8', '#7a7f88', fill='#b9bec6')
    wx0, wx1, wy0, wy1 = x0 + 3, x1 - 3, y0 + 3, y1 - 2
    for y in range(wy0, wy1 + 1):
        t = (y - wy0) / max(1, wy1 - wy0)
        c.fill(rect(wx0, y, wx1, y), mix('#1a0e0a', '#7a2a0a', t ** 1.5))
    c.fill(rect(wx0, wy0, wx1, wy0), '#050303')
    c.fill(rect(wx0, wy0, wx0, wy1), '#050303')
    # résistance rougeoyante et grille
    c.fill(rect(wx0 + 1, wy1 - 1, wx1 - 1, wy1 - 1), '#ff6a1a')
    c.fill(rect(wx0 + 1, wy1, wx1 - 1, wy1), '#ffb040')
    gy = wy0 + 8
    c.fill(rect(wx0 + 1, gy, wx1 - 1, gy), '#8a6a5a')
    # plat doré sur la grille : poulet rôti dans un plat
    c.fill(rect(wx0 + 4, gy - 1, wx1 - 4, gy - 1), '#c0c4cc')
    c.fill(ellipse(16, gy - 3, 6, 2.6) & (YY < gy - 0.5), '#c8701e')
    c.fill(ellipse(15, gy - 4, 3.5, 1.4), '#e8a040')
    c.fill(rect(15, gy - 5, 16, gy - 5), '#f8d080')
    # reflet du verre
    c.fill(line_mask(wx0 + 2, wy1 - 2, wx0 + 8, wy0 + 1), '#3a2a2a')
    c.fill(line_mask(wx0 + 4, wy1 - 2, wx0 + 10, wy0 + 1), '#4a3430')
    # plinthe ventilée
    for x in range(4, 29, 3):
        c.fill(rect(x, NY(1.5), x + 1, NY(1.5)), '#6e737c')
    return c


@tex('four_side')
def four_side():
    c = materials.acier()
    c.fill(rect(0, 0, 31, 0), '#dfe3e8')
    for y in (4, 6, 8):
        c.fill(rect(20, y, 28, y), '#6e737c')
    c.fill(rect(0, 30, 31, 31), '#8e949c')
    return c


@tex('four_top')
def four_top():
    c = materials.acier()
    for y in range(22, 30, 2):
        c.fill(rect(6, y, 25, y), '#5a5f68')
    box_bevel(c, 0, 0, 31, 31, '#dfe3e8', '#8e949c')
    return c


# ---------------------------------------------------------------- fourneau
@tex('fourneau_front')
def fourneau_front():
    c = materials.acier()
    # bandeau des boutons (y 11.5 -> 14)
    box_bevel(c, 1, NY(14), 30, NY(11.5) - 1, '#dfe3e8', '#7a7f88', fill='#a8adb5')
    for x in (NX(13.5), NX(10.5), NX(5.5), NX(2.5)):
        c.fill(rect(x - 1, NY(13.9), x + 1, NY(13.9)), '#6e737c')
    c.fill(rect(15, 7, 16, 8), '#ff3a2a')
    c.fill(rect(15, 7, 15, 7), '#ffb0a0')
    # deux portes
    for (xa, xb) in ((1, 7.9), (8.1, 15)):
        x0, x1 = NX(xb), NX(xa) - 1
        y0, y1 = NY(11), NY(1.5) - 1
        box_bevel(c, x0, y0, x1, y1, '#dfe3e8', '#6e737c', fill='#c3c8d0')
        box_bevel(c, x0 + 2, y0 + 2, x1 - 2, y1 - 2, '#8e949c', '#e8ecf0')
    c.fill(rect(0, NY(1.5), 31, 31), '#3a3c42')
    return c


@tex('fourneau_side')
def fourneau_side():
    c = materials.acier()
    c.fill(rect(0, NY(14), 31, NY(14)), '#dfe3e8')
    c.fill(rect(0, NY(1.5), 31, 31), '#3a3c42')
    return c


@tex('fourneau_top')
def fourneau_top():
    c = materials.acier_sombre()
    for (bx, bz) in ((4.5, 4.5), (11.5, 4.5), (4.5, 11.5), (11.5, 11.5)):
        x, y = TX(bx), TX(bz)
        c.fill(circle(x, y, 6.5), '#1a1b1e')
        c.fill(circle(x, y, 5.2), '#2e3036')
        ring(c, x, y, 4.2, 1, '#4a8aff')
        c.fill(circle(x, y, 3), '#b8902a')
        c.fill(circle(x - 0.7, y - 0.7, 1.6), '#f0c860')
        c.fill(rect(x + 4, y - 4, x + 4, y - 4), '#c0c4cc')
    box_bevel(c, 0, 0, 31, 31, '#9aa0a8', '#3a3c42')
    return c


@tex('poele')
def poele():
    """Dessus de la poêle (projeté) : steak qui grésille dans le beurre."""
    c = materials.fonte()
    x0, x1, z0, z1 = TX(8), TX(15), TX(1.5), TX(8.5)
    cx, cz = (x0 + x1) / 2, (z0 + z1) / 2
    c.fill(circle(cx, cz, 6.6), '#15161a')
    c.fill(circle(cx, cz, 5.8), '#2a2c32')
    c.fill(ellipse(cx - 1, cz + 1, 4.8, 3.6), '#e8c040')
    st = ellipse(cx, cz - 0.3, 4.2, 3)
    c.fill(st, '#7a3a1c')
    c.fill(ellipse(cx - 1, cz - 1.2, 2.4, 1.2), '#a8582a')
    for k in (-2, 0, 2):
        c.fill(line_mask(cx + k - 1, cz + 2, cx + k + 1, cz - 2) & st, '#3a1808')
    c.fill(rect(int(cx) + 3, int(cz) + 2, int(cx) + 3, int(cz) + 2), '#fff4c0')
    return c


@tex('casserole')
def casserole():
    c = materials.inox_poli()
    x0, x1, z0, z1 = TX(1.5), TX(7.5), TX(8.5), TX(14.5)
    cx, cz = (x0 + x1) / 2, (z0 + z1) / 2
    c.fill(rect(x0 + 1, z0 + 1, x1 - 2, z1 - 2), '#8ab8d8')
    c.fill(circle(cx, cz, 4.5), '#a8d0e8')
    for (dx, dz, r) in [(-2, -1, 1.2), (1.5, 1, 1.5), (0, 2.5, 0.9), (2, -2, 0.8)]:
        ring(c, cx + dx, cz + dz, r + 0.6, 1, '#f4fbff')
    for (dx, dz) in [(-2.5, 1.5), (1, -1.5)]:
        c.fill(rect(int(cx + dx), int(cz + dz), int(cx + dx) + 2, int(cz + dz)), '#f0d070')
    return c


# ---------------------------------------------------------------- plan de travail
@tex('plan_front')
def plan_front():
    c = materials.bois_clair()
    # tiroir
    box_bevel(c, 1, NY(13.6), 30, NY(11) - 1, '#e4b87a', '#7a4a22')
    # portes
    for (xa, xb) in ((0.5, 7.9), (8.1, 15.5)):
        x0, x1 = NX(xb), NX(xa) - 1
        y0, y1 = NY(10.5), NY(1.4) - 1
        box_bevel(c, x0, y0, x1, y1, '#e4b87a', '#7a4a22')
        box_bevel(c, x0 + 2, y0 + 2, x1 - 2, y1 - 2, '#8a5a2a', '#e8c088')
    c.fill(rect(0, NY(1.2), 31, 31), '#5a3a1e')
    return c


@tex('plan_side')
def plan_side():
    c = materials.bois_clair()
    box_bevel(c, 0, 0, 31, 31, '#e4b87a', '#7a4a22')
    box_bevel(c, 3, 4, 28, 27, '#8a5a2a', '#e8c088')
    return c


@tex('plan_top')
def plan_top():
    c = materials.billot()
    # farine sur le plan, près du rouleau
    rs = np.random.RandomState(31)
    for _ in range(90):
        a, r = rs.rand() * 6.28, math.sqrt(rs.rand()) * 6
        x, y = int(26 + math.cos(a) * r), int(22 + math.sin(a) * r * 0.8)
        if 0 <= x < 32 and 0 <= y < 32:
            c.fill(rect(x, y, x, y), '#f8f4ea' if rs.rand() < 0.6 else '#e8dcc4')
    return c


@tex('planche')
def planche():
    """Planche à découper (projetée sur son dessus)."""
    c = new('#e0b070')
    vstreaks(c, ['#d4a060', '#ecc088'], 40, 32, lmin=6, lmax=20)
    x0, x1, z0, z1 = TX(2.5), TX(10.5), TX(2.5), TX(10)
    c.fill(rect(x0 + 1, z0 + 1, x1 - 2, z0 + 1) | rect(x0 + 1, z1 - 2, x1 - 2, z1 - 2) | rect(x0 + 1, z0 + 1, x0 + 1, z1 - 2) |
           rect(x1 - 2, z0 + 1, x1 - 2, z1 - 2), '#c08a4a')
    for (x, y) in [(9, 16), (10, 17), (12, 15), (8, 18)]:
        c.fill(rect(x, y, x, y), '#e83a2a')
    return c


@tex('lame')
def lame():
    c = new('#d8dde4')
    for y in range(32):
        c.px[y, :, :3] = mix('#f8fbff', '#8e949c', y / 31)
    c.fill(rect(0, 0, 31, 1), '#ffffff')
    return c


@tex('manche')
def manche():
    c = new('#2a1c14')
    noise(c, ['#3a281c', '#20140e'], 0.3, 33)
    for x in (6, 16, 26):
        c.fill(circle(x, 16, 2.2), '#c8ccd2')
    return c


@tex('tomate_peau')
def tomate_peau():
    c = new('#d8281c')
    for y in range(32):
        c.px[y, :, :3] = mix('#f05040', '#a81810', y / 31)
    c.fill(rect(6, 4, 9, 8), '#f88878')
    return c


@tex('tomate_dessus')
def tomate_dessus():
    c = new('#d8281c')
    star = polygon([(16, 4), (19, 12), (28, 11), (21, 17), (24, 26), (16, 20), (8, 26), (11, 17), (4, 11), (13, 12)])
    c.fill(star, '#3a8a2a')
    c.fill(circle(16, 16, 2.5), '#2a6a1a')
    return c


@tex('rouleau')
def rouleau():
    c = new('#d8a868')
    for y in range(32):
        v = 0.5 + 0.5 * math.sin(y / 31 * math.pi)
        c.px[y, :, :3] = mix('#9a6a3a', '#f0c890', v)
    vstreaks(c, ['#c89858'], 10, 34)
    return c


# ---------------------------------------------------------------- bol / tamis / mortier
@tex('bol_bord')
def bol_bord():
    c = materials.ceramique()
    c.fill(rect(0, 4, 31, 5), '#3a6ab0')
    c.fill(rect(0, 8, 31, 8), '#3a6ab0')
    return c


@tex('bol_pate')
def bol_pate():
    c = new('#f0dca8')
    cx = cy = 16
    for k in range(6):
        c.fill(arc_band(cx, cy, 2 + k * 2.2, 2 + k * 2.2, 1, k * 55, k * 55 + 220), '#fbeec8')
    c.fill(arc_band(cx, cy, 9, 9, 1, 300, 60), '#d8c088')
    return c


@tex('ardoise')
def ardoise():
    c = materials.pierre_sombre()
    for (x, y) in [(4, 26), (6, 27), (5, 29), (27, 5), (28, 7), (26, 27)]:
        c.fill(circle(x, y, 1.1), '#1a1614')
        c.fill(rect(x, y, x, y), '#4a3a30')
    for (x, y) in [(25, 25), (27, 28)]:
        c.fill(ellipse(x, y, 2.2, 1.2), '#f4ecdc')
    c.fill(line_mask(3, 3, 8, 6), '#4a8a2a')
    c.fill(ellipse(6, 5, 2, 1.2), '#3a9a2a')
    return c


@tex('epices_mortier')
def epices_mortier():
    c = materials.epices()
    c.fill(circle(16, 16, 3), '#e8a040')
    return c


@tex('poudre')
def poudre():
    c = Canvas()
    rs = np.random.RandomState(35)
    for _ in range(70):
        x, y = rs.randint(4, 28), rs.randint(0, 32)
        c.px[y, x, :3] = (250, 248, 240)
        c.px[y, x, 3] = 255
    return c


# ---------------------------------------------------------------- grill
@tex('grill_front')
def grill_front():
    c = materials.fonte()
    # thermomètre
    c.fill(circle(16, 10, 4.5), '#c0c4cc')
    c.fill(circle(16, 10, 3.5), '#f4f0e4')
    c.fill(line_mask(16, 10, 18.5, 8), '#c83a2a')
    c.fill(arc_band(16, 10, 3.2, 3.2, 1, 150, 210), '#e8501a')
    # aérations
    for x in range(5, 12, 2):
        c.fill(rect(x, 20, x, 26), '#101114')
    for x in range(21, 28, 2):
        c.fill(rect(x, 20, x, 26), '#101114')
    c.fill(rect(0, 0, 31, 0), '#4a4e56')
    return c


@tex('saucisse')
def saucisse():
    c = new('#a8482a')
    for y in range(32):
        v = 0.5 + 0.5 * math.sin(y / 31 * math.pi)
        c.px[y, :, :3] = mix('#6a2a14', '#d8784a', v)
    for x in range(3, 32, 7):
        c.fill(line_mask(x, 4, x + 3, 28), '#3a140a')
    return c


@tex('steak_grill')
def steak_grill():
    c = new('#7a3a1c')
    noise(c, ['#8a4a24', '#6a2e14'], 0.4, 36)
    for k in range(-2, 6):
        c.fill(line_mask(k * 6, 31, k * 6 + 12, 0, 2), '#2a1006')
    return c


@tex('brochette')
def brochette():
    c = Canvas()
    cols = ['#e8b060', '#d8281c', '#e8b060', '#3a9a2a', '#e8b060']
    for k, cc in enumerate(cols):
        c.fill(rect(k * 6 + 1, 0, k * 6 + 5, 31), cc)
        c.fill(rect(k * 6 + 1, 0, k * 6 + 5, 3), mix(cc, '#ffffff', 0.3))
        c.fill(rect(k * 6 + 1, 26, k * 6 + 5, 31), mix(cc, '#000000', 0.35))
    return c


@tex('fumee')
def fumee():
    c = Canvas()
    for (x, ph) in [(12, 0), (20, 1.5)]:
        for y in range(2, 30):
            xx = x + math.sin(y * 0.35 + ph) * 3
            a = y / 30
            for dx in (0, 1):
                c.px[y, int(xx) + dx, :3] = (170, 170, 176)
                c.px[y, int(xx) + dx, 3] = int(170 * a)
    return c


# ---------------------------------------------------------------- friteuse
@tex('friteuse_front')
def friteuse_front():
    c = materials.acier()
    box_bevel(c, 2, 4, 29, 13, '#dfe3e8', '#6e737c', fill='#2a2c32')
    c.fill(rect(4, 6, 5, 7), '#ff3a2a')
    c.fill(rect(4, 10, 5, 11), '#3aff6a')
    for x in range(8, 24, 3):
        c.fill(rect(x, 11, x, 12), '#c0c4cc')
    c.fill(rect(8, 6, 23, 6), '#f0a020')
    c.fill(rect(0, 28, 31, 31), '#3a3c42')
    return c


@tex('frites_dessus')
def frites_dessus():
    c = new('#d8a030')
    rs = np.random.RandomState(37)
    for _ in range(40):
        x, y = rs.randint(0, 30), rs.randint(0, 30)
        if rs.rand() < 0.5:
            c.fill(rect(x, y, x + rs.randint(3, 7), y + 1), '#f4c848')
            c.fill(rect(x, y + 1, x + 3, y + 1), '#c88a20')
        else:
            c.fill(rect(x, y, x + 1, y + rs.randint(3, 7)), '#f8d060')
    return c


# ---------------------------------------------------------------- presse-agrumes
@tex('pichet')
def pichet():
    c = materials.verre()
    for y in range(9, 32):
        c.fill(rect(1, y, 30, y), mix('#f8b030', '#e88818', (y - 9) / 23))
    c.fill(rect(1, 9, 30, 9), '#fbd070')
    c.fill(rect(3, 0, 4, 31), '#fff4dc')
    c.fill(rect(0, 0, 31, 1), '#eef8fb')
    return c


@tex('orange_peau')
def orange_peau():
    c = new('#f08a14')
    noise(c, ['#e07a0c', '#f89a28'], 0.3, 38)
    return c


@tex('orange_tranche')
def orange_tranche():
    c = new('#f08a14')
    c.fill(circle(16, 16, 14), '#fbf0c8')
    c.fill(circle(16, 16, 12), '#f8a830')
    for a in range(0, 360, 45):
        c.fill(line_mask(16, 16, 16 + math.cos(math.radians(a)) * 12, 16 + math.sin(math.radians(a)) * 12, 2), '#fbd890')
    c.fill(circle(16, 16, 2), '#fbf0c8')
    return c


@tex('herbes_feuille')
def herbes_feuille():
    return new('#3a8a2a')


@tex('shaker_tapis')
def shaker_tapis():
    c = materials.caoutchouc()
    for k in range(2, 32, 4):
        c.fill(rect(k, 0, k, 31), '#2a2b2f')
    for (x, z) in ((3.2, 3.2), (5.4, 4.6)):
        cx, cy = TX(x), TX(z)
        c.fill(circle(cx, cy, 2.6), '#e8c820')
        c.fill(circle(cx, cy, 1.9), '#fbf0a0')
        c.fill(rect(cx, cy - 1, cx, cy + 1) | rect(cx - 1, cy, cx + 1, cy), '#f0dc60')
    return c


@tex('cone_strie')
def cone_strie():
    c = materials.orange_email()
    for x in range(0, 32, 4):
        c.fill(rect(x, 0, x, 31), '#b85a12')
    return c


# ---------------------------------------------------------------- shaker
@tex('glacon')
def glacon():
    c = new('#dff4fb')
    c.fill(rect(0, 0, 31, 3), '#ffffff')
    c.fill(rect(0, 0, 3, 31), '#ffffff')
    c.fill(rect(28, 0, 31, 31), '#b8dcea')
    return c


# ---------------------------------------------------------------- doseur
def bocal(color, label='#f4ecd0'):
    c = materials.verre()
    for y in range(10, 32):
        c.fill(rect(1, y, 30, y), mix(color, '#000000', 0.15 * (y - 10) / 22))
    noise(c, [mix(color, '#ffffff', 0.25), mix(color, '#000000', 0.25)], 0.25, 39, mask=rect(1, 10, 30, 31))
    c.fill(rect(0, 14, 31, 21), label)
    c.fill(rect(0, 14, 31, 14), mix(label, '#000000', 0.2))
    c.fill(rect(0, 21, 31, 21), mix(label, '#000000', 0.2))
    c.fill(rect(4, 0, 5, 31), '#fbfdff')
    return c


for _n, _c in [('paprika', '#c8321a'), ('curcuma', '#f0a818'), ('herbes', '#4a8a2a'), ('poivre', '#2a2420'), ('sel', '#f8f8f4'),
               ('safran', '#e8401a')]:
    TEX['bocal_' + _n] = (lambda cc: (lambda: bocal(cc)))(_c)


@tex('saliere')
def saliere():
    c = materials.verre()
    for y in range(6, 32):
        c.fill(rect(1, y, 30, y), '#fbfbf8')
    c.fill(rect(4, 0, 5, 31), '#ffffff')
    return c


@tex('moulin')
def moulin():
    c = materials.bois_fonce()
    c.fill(rect(0, 10, 31, 11), '#d4a23a')
    return c


# ---------------------------------------------------------------- frigo
@tex('frigo_front')
def frigo_front():
    c = materials.menthe()
    for x in range(32):
        v = 0.5 + 0.5 * math.cos((x - 8) / 32 * math.pi)
        c.px[:, x, :3] = [int(a * (0.9 + 0.12 * v)) if a * (0.9 + 0.12 * v) < 255 else 255 for a in col('#8fd3c4')]
    # joint entre les portes (y 10.8 -> 11)
    c.fill(rect(0, NY(11), 31, NY(10.8)), '#3a5a54')
    # bords arrondis des portes
    for (ya, yb) in ((11, 15.3), (1.3, 10.8)):
        y0, y1 = NY(yb), NY(ya) - 1
        c.fill(rect(1, y0, 30, y0), '#c8f0e6')
        c.fill(rect(1, y1, 30, y1), '#5a9a8e')
        c.fill(rect(1, y0, 1, y1), '#b0e4d8')
        c.fill(rect(30, y0, 30, y1), '#6aa89c')
    # écusson chromé
    box_bevel(c, 8, NY(9.3), 19, NY(8.1), '#ffffff', '#8e949c', fill='#dfe3e8')
    for x in (10, 12, 13, 15, 17):
        c.fill(rect(x, NY(8.9), x, NY(8.5)), '#c83a2a')
    # thermomètre du freezer
    c.fill(circle(12, NY(13.1), 2.5), '#dfe3e8')
    c.fill(circle(12, NY(13.1), 1.7), '#4a8ad0')
    c.fill(rect(12, NY(13.1) - 1, 12, NY(13.1) + 1) | rect(11, NY(13.1), 13, NY(13.1)), '#ffffff')
    return c


@tex('frigo_side')
def frigo_side():
    c = materials.menthe()
    for x in range(32):
        v = x / 31
        c.px[:, x, :3] = mix('#9adccd', '#7cc0b2', v)
    c.fill(rect(0, 0, 31, 0), '#b8ecde')
    return c


@tex('frigo_top')
def frigo_top():
    c = materials.menthe()
    box_bevel(c, 0, 0, 31, 31, '#c8f0e6', '#6aa89c')
    return c


# ---------------------------------------------------------------- poubelle
@tex('poubelle_cote')
def poubelle_cote():
    c = materials.acier()
    for x in range(0, 32, 4):
        c.fill(rect(x, 0, x, 31), '#8e949c')
        c.fill(rect(x + 1, 0, x + 1, 31), '#dfe3e8')
    return c


@tex('poubelle_face')
def poubelle_face():
    c = poubelle_cote()
    c.fill(circle(16, 14, 6), '#3a8a3a')
    c.fill(circle(16, 14, 5), '#4aa84a')
    for a in (90, 210, 330):
        ang = math.radians(a)
        x, y = 16 + math.cos(ang) * 2.5, 14 - math.sin(ang) * 2.5
        c.fill(polygon([(x, y - 1.5), (x + 1.5, y + 1), (x - 1.5, y + 1)]), '#ffffff')
    return c


# ============================================================================ modèles
def four():
    m = Model('four', 'hxrpmetiers:blocks/acier')
    for x in (1, 13.5):
        for z in (1.5, 13.5):
            m.box([x, 0, z], [x + 1.5, 1, z + 1.5], 'noir_mat')
    m.box([0.5, 1, 1], [15.5, 15.5, 15.5], {'n': 'four_front', 's': 'acier', 'w': 'four_side', 'e': 'four_side', 'd': 'noir_mat', 'u': 'four_top'},
          uv={'n': 'proj', 'w': 'proj', 'e': 'proj', 'u': 'proj', '*': 'size'})
    m.box([0.3, 15.5, 0.8], [15.7, 16, 15.7], {'*': 'acier', 'u': 'four_top'}, uv={'u': 'proj', '*': 'size'})
    m.box([1, 1.5, 0.3], [15, 11.5, 1], {'n': 'four_front', '*': 'acier_sombre'}, faces='nwedu', uv={'n': 'proj', '*': 'size'})
    m.box([2, 9.8, -1.1], [14, 10.6, -0.3], 'chrome')
    for x in (2.6, 12.6):
        m.box([x, 9.9, -0.4], [x + 0.8, 10.5, 0.3], 'chrome', faces='wedu')
    for x in (2.2, 12.3):
        m.box([x, 12.6, 0.2], [x + 1.5, 14.1, 1], {'n': 'bouton', '*': 'noir_mat'}, uv={'n': 'full', '*': 'size'}, faces='nweud')
    return m


def fourneau():
    m = Model('fourneau', 'hxrpmetiers:blocks/acier')
    m.box([1, 0, 1.5], [15, 1, 15], 'noir_mat')
    m.box([0.5, 1, 1], [15.5, 14, 15.5], {'n': 'fourneau_front', 'w': 'fourneau_side', 'e': 'fourneau_side', 's': 'acier', 'd': 'noir_mat'},
          faces='nswed', uv={'n': 'proj', 'w': 'proj', 'e': 'proj', '*': 'size'})
    m.box([0, 14, 0.5], [16, 15, 16], {'u': 'fourneau_top', '*': 'acier'}, uv={'u': 'proj', '*': 'size'})
    m.box([0, 15, 15], [16, 17.5, 16], 'acier')
    m.box([0.5, 16.2, 14.6], [15.5, 17.2, 15], 'acier_sombre', faces='nwe')
    for (cx, cz) in ((4.5, 4.5), (11.5, 4.5), (4.5, 11.5), (11.5, 11.5)):
        m.box([cx - 3, 15, cz - 0.35], [cx + 3, 15.6, cz + 0.35], 'fonte')
        m.box([cx - 0.35, 15, cz - 3], [cx + 0.35, 15.6, cz - 0.35], 'fonte')
        m.box([cx - 0.35, 15, cz + 0.35], [cx + 0.35, 15.6, cz + 3], 'fonte')
    # poêle (avant gauche vu de face = est) avec son steak
    m.box([8.5, 15.6, 1.5], [14.5, 16.9, 7.5], {'u': 'poele', '*': 'fonte'}, uv={'u': 'proj', '*': 'size'})
    m.box([11, 16.2, -2.6], [12, 16.8, 1.5], 'noir_mat')
    # casserole (arrière droite = ouest) qui bout
    m.box([1.5, 15.6, 8.5], [7.5, 20, 14.5], {'u': 'casserole', '*': 'inox_poli'}, uv={'u': 'proj', '*': 'size'})
    m.box([1, 19, 11], [1.5, 19.6, 12], 'noir_mat', faces='nsweu')
    m.box([7.5, 19, 11], [8, 19.6, 12], 'noir_mat', faces='nseu')
    m.sprite(4.5, 11.5, 20, 26, 5, 'vapeur')
    for x in (2.5, 5.5, 10.5, 13.5):
        m.box([x - 0.75, 12, 0.3], [x + 0.75, 13.5, 1], {'n': 'bouton', '*': 'noir_mat'}, uv={'n': 'full', '*': 'size'}, faces='nweud')
    for x in (7.1, 8.3):
        m.box([x, 5.5, 0.3], [x + 0.6, 9, 1], 'chrome', faces='nweud')
    return m


def plan_de_travail():
    m = Model('plan_de_travail', 'hxrpmetiers:blocks/billot')
    m.box([1, 0, 2], [15, 1, 15], 'noir_mat')
    m.box([0, 1, 1], [16, 14, 16], {'n': 'plan_front', 'w': 'plan_side', 'e': 'plan_side', 's': 'bois_clair', 'd': 'bois_fonce'},
          faces='nswed', uv={'n': 'proj', '*': 'size'})
    m.box([0, 14, 0], [16, 16, 16], {'u': 'plan_top', '*': 'billot'}, uv={'u': 'proj', '*': 'size'})
    m.box([5, 11.7, 0.3], [11, 12.3, 1], 'laiton', faces='nweud')
    for x in (6.5, 8.7):
        m.box([x, 5.6, 0.3], [x + 0.8, 6.6, 1], 'laiton', faces='nweud')
    # planche à découper, couteau, tomate
    m.box([2.5, 16, 2.5], [10.5, 16.8, 10], {'u': 'planche', '*': 'bois_clair'}, uv={'u': 'proj', '*': 'size'})
    m.box([3.2, 16.8, 3.8], [8.6, 17.05, 5.1], 'lame', faces='nsweu')
    m.box([8.6, 16.8, 3.9], [11.6, 17.5, 5], 'manche')
    m.box([4, 16.8, 6.5], [6.4, 18.9, 8.9], {'u': 'tomate_dessus', 'd': 'tomate_peau', '*': 'tomate_peau'}, uv={'u': 'full', '*': 'full'})
    m.box([6.9, 16.8, 6.8], [8.9, 17.5, 8.8], {'u': 'tomate_coupe', '*': 'tomate_peau'}, uv={'u': 'full', '*': 'size'})
    # rouleau à pâtisserie posé en biais
    o = (13, 16.5, 12)
    m.box([10, 16, 11.5], [16, 17, 12.5], 'rouleau', rot=('y', 22.5, o))
    m.box([9, 16.25, 11.75], [10, 16.75, 12.25], 'bois_fonce', rot=('y', 22.5, o))
    m.box([16, 16.25, 11.75], [17, 16.75, 12.25], 'bois_fonce', rot=('y', 22.5, o))
    return m


@tex('tomate_coupe')
def tomate_coupe():
    c = new('#d8281c')
    c.fill(circle(16, 16, 14), '#e84a3a')
    c.fill(circle(16, 16, 6), '#f06a50')
    for a in range(0, 360, 60):
        x, y = 16 + math.cos(math.radians(a)) * 9, 16 + math.sin(math.radians(a)) * 9
        c.fill(ellipse(x, y, 2, 1.4), '#f8e8a0')
    return c


def bol():
    m = Model('bol', 'hxrpmetiers:blocks/ceramique', aabb=(1, 0, 1, 15, 10, 15), gui_scale=0.8, gui_dy=1.5)
    m.box([1, 0, 1], [15, 0.8, 15], 'bois_fonce')
    m.disc(8, 8, 3, 0.8, 1.8, 'ceramique')
    m.disc(8, 8, 5, 1.8, 3.2, 'ceramique')
    m.disc(8, 8, 6.2, 3.2, 5.2, 'ceramique')
    m.box([2.6, 5, 2.6], [13.4, 7.2, 13.4], {'u': 'bol_pate'}, faces='u', uv='full')
    m.ring(8, 8, 6.5, 5.2, 8.2, 1, 'bol_bord')
    o = (10.5, 6.5, 8.5)
    rot = ('z', -22.5, o)
    m.box([10.5, 6.5, 6.5], [10.5, 12.5, 10.5], 'fouet', faces='we', uv='full', rot=rot, shade=False)
    m.box([8.5, 6.5, 8.5], [12.5, 12.5, 8.5], 'fouet', faces='ns', uv='full', rot=rot, shade=False)
    m.box([10.1, 12.5, 8.1], [10.9, 17, 8.9], 'noir_mat', rot=rot)
    return m


def mortier():
    m = Model('mortier', 'hxrpmetiers:blocks/pierre', aabb=(2, 0, 2, 14, 11, 14), gui_scale=0.8, gui_dy=1.5)
    m.box([2, 0, 2], [14, 0.6, 14], {'u': 'ardoise', '*': 'pierre_sombre'}, uv={'u': 'proj', '*': 'size'})
    m.disc(8, 8, 3, 0.6, 1.6, 'pierre')
    m.disc(8, 8, 4.6, 1.6, 3, 'pierre')
    m.disc(8, 8, 5.4, 3, 5.8, 'pierre')
    m.box([3.3, 5.8, 3.3], [12.7, 6.8, 12.7], {'u': 'epices_mortier'}, faces='u', uv='full')
    m.ring(8, 8, 5.6, 5.8, 8, 1, 'pierre')
    o = (8, 6.5, 8)
    rot = ('z', 22.5, o)
    m.box([6.9, 6.2, 6.9], [9.1, 8.2, 9.1], 'pierre', rot=rot)
    m.box([7.3, 8.2, 7.3], [8.7, 15.5, 8.7], 'pierre', rot=rot)
    m.box([7.1, 15.5, 7.1], [8.9, 16.3, 8.9], 'pierre', rot=rot)
    return m


def tamis():
    m = Model('tamis', 'hxrpmetiers:blocks/bois_clair', aabb=(1, 0, 1, 15, 9, 15), gui_scale=0.8, gui_dy=1.5)
    m.disc(8, 8, 3, 0, 1, 'ceramique_bleue')
    m.disc(8, 8, 5, 1, 2.5, 'ceramique_bleue')
    m.box([2.6, 2.5, 2.6], [13.4, 4, 13.4], {'u': 'farine'}, faces='u', uv='full')
    m.ring(8, 8, 6.2, 2.5, 4.6, 1, 'ceramique_bleue')
    m.sprite(8, 8, 4, 6.3, 7, 'poudre')
    m.ring(8, 8, 7, 5.5, 8.5, 0.8, 'bois_clair')
    m.cross(8, 8, 6.4, 7 * 0.62, 7 * 0.86, 6.3, 6.3, 'tamis', faces='ud', uv='full')   # toile tendue dans le cercle
    m.disc(8, 8, 3.2, 6.3, 7.3, 'farine', faces='nsewu')
    m.disc(8, 8, 1.6, 7.3, 7.9, 'farine', faces='nsewu')
    return m


def grill():
    m = Model('grill', 'hxrpmetiers:blocks/fonte', light=5, aabb=(1, 0, 2, 15, 13, 14))
    for x in (1.5, 13.5):
        for z in (3, 12):
            m.box([x, 0, z], [x + 1, 7, z + 1], 'noir_mat')
    m.box([2, 2, 3.5], [14, 2, 12.5], 'grille', faces='ud', uv='full')
    m.box([1, 7, 2.5], [15, 9, 13.5], {'n': 'grill_front', '*': 'fonte'}, uv={'n': 'size', '*': 'size'})
    m.box([1, 9, 2.5], [15, 12, 3.5], {'n': 'grill_front', '*': 'fonte'}, uvs={'n': [1, 0, 15, 3]})
    m.box([1, 9, 12.5], [15, 12, 13.5], 'fonte')
    m.box([1, 9, 3.5], [2, 12, 12.5], 'fonte')
    m.box([14, 9, 3.5], [15, 12, 12.5], 'fonte')
    m.box([2, 9, 3.5], [14, 10, 12.5], {'u': 'braise'}, faces='u', uv='full', shade=False)
    m.box([1.5, 11.5, 3], [14.5, 11.5, 13], 'grille', faces='ud', uv='full')
    for x in (3, 5.2):
        m.box([x, 11.5, 4.5], [x + 1.4, 12.8, 11.5], 'saucisse', faces='nsewu')
    m.box([8, 11.5, 4.5], [12.8, 12.3, 8.2], {'u': 'steak_grill', '*': 'saucisse'}, faces='nsewu')
    o = (8, 12, 13.6)
    m.box([1, 12, 13], [15, 19, 14], 'fonte', rot=('x', 22.5, o))
    m.box([5, 17.5, 14], [11, 18.2, 15], 'chrome', rot=('x', 22.5, o))
    m.sprite(8, 8, 12.5, 19, 7, 'fumee')
    return m


def friteuse():
    m = Model('friteuse', 'hxrpmetiers:blocks/acier', aabb=(1, 0, 2, 15, 12, 14))
    for x in (1.5, 13.5):
        for z in (2.5, 12.5):
            m.box([x, 0, z], [x + 1, 0.8, z + 1], 'noir_mat')
    m.box([1, 0.8, 2], [15, 9, 14], {'n': 'friteuse_front', '*': 'acier'}, uv={'n': 'size', '*': 'size'})
    m.box([1, 9, 2], [15, 11, 3], 'inox_poli')
    m.box([1, 9, 13], [15, 11, 14], 'inox_poli')
    m.box([1, 9, 3], [2, 11, 13], 'inox_poli')
    m.box([14, 9, 3], [15, 11, 13], 'inox_poli')
    m.box([2, 8.5, 3], [14, 9.6, 13], {'u': 'huile'}, faces='u', uv='full')
    m.box([3.5, 8.8, 4], [12.5, 12, 11.5], 'panier', faces='nsewd', uv='size')
    m.box([4, 9.6, 4.5], [12, 11.4, 11], {'u': 'frites_dessus', '*': 'frites_dessus'})
    m.box([7.5, 11.2, -2.4], [8.5, 12.1, 4], 'noir_mat')
    m.box([7.7, 11.4, 1], [8.3, 11.9, 3.9], 'chrome')
    m.box([11, 4.4, 1.4], [13, 6.4, 2], {'n': 'bouton', '*': 'noir_mat'}, uv={'n': 'full', '*': 'size'}, faces='nweud')
    m.box([2, 11, 13.4], [14, 13, 14], 'acier')
    return m


def marmite():
    m = Model('marmite', 'hxrpmetiers:blocks/fonte', light=4, aabb=(1, 0, 1, 15, 13, 15))
    for (x, z) in ((3, 3), (12, 3), (3, 12), (12, 12)):
        m.box([x, 0, z], [x + 1, 2.2, z + 1], 'fonte')
    m.ring(8, 8, 5.8, 2, 2.6, 0.8, 'fonte', faces='nsewud')
    m.box([4, 0, 7.2], [12, 1, 8.4], {'u': 'braise', '*': 'bois_fonce'}, rot=('y', 22.5, (8, 0.5, 7.8)))
    m.box([4, 0, 7.2], [12, 1, 8.4], {'u': 'braise', '*': 'bois_fonce'}, rot=('y', -22.5, (8, 0.5, 7.8)))
    m.sprite(8, 8, 0.5, 3.5, 6, 'flamme')
    m.disc(8, 8, 3.8, 2.6, 3.6, 'fonte')
    m.disc(8, 8, 5.4, 3.6, 5, 'fonte')
    m.disc(8, 8, 6.3, 5, 10, 'fonte')
    m.box([2.2, 9.5, 2.2], [13.8, 10.6, 13.8], {'u': 'ragout'}, faces='u', uv='full')
    m.ring(8, 8, 6.6, 10, 12, 1, 'fonte')
    m.box([0.6, 9.4, 6.6], [1.4, 10.2, 9.4], 'fonte')
    m.box([14.6, 9.4, 6.6], [15.4, 10.2, 9.4], 'fonte')
    m.box([0.6, 10.2, 6.6], [1.8, 10.8, 7.2], 'fonte')
    m.box([0.6, 10.2, 8.8], [1.8, 10.8, 9.4], 'fonte')
    m.box([14.2, 10.2, 6.6], [15.4, 10.8, 7.2], 'fonte')
    m.box([14.2, 10.2, 8.8], [15.4, 10.8, 9.4], 'fonte')
    rot = ('z', -22.5, (9.4, 10, 7.9))
    m.box([9, 10, 7.5], [9.8, 18, 8.3], 'bois_fonce', rot=rot)
    m.sprite(6.5, 9, 11.5, 18, 6, 'vapeur')
    return m


def presse():
    m = Model('presse', 'hxrpmetiers:blocks/orange_email', aabb=(1, 0, 1, 15, 8, 15), gui_scale=0.8, gui_dy=1.5)
    m.box([1, 0, 1], [15, 0.6, 15], 'bois_clair')
    # presse-agrumes émaillé, une demi-orange en train d'être pressée
    cx, cz = 9.5, 9.5
    m.disc(cx, cz, 4.6, 0.6, 1.4, 'orange_email')
    m.box([cx - 3.8, 1.4, cz - 3.8], [cx + 3.8, 2.2, cz + 3.8], {'u': 'jus_orange'}, faces='u', uv='full')
    m.ring(cx, cz, 4.8, 1.4, 3.4, 0.8, 'orange_email')
    m.box([cx - 5.6, 2.6, cz - 0.6], [cx - 4.6, 3.2, cz + 0.6], 'orange_email')
    m.disc(cx, cz, 1.5, 1.4, 3.4, 'cone_strie', faces='nsew')
    m.disc(cx, cz, 2.6, 3.4, 5.2, 'orange_peau', faces='nsewu')
    m.disc(cx, cz, 1.6, 5.2, 5.8, 'orange_peau', faces='nsewu')
    m.box([cx - 0.3, 5.8, cz - 0.3], [cx + 0.3, 6.2, cz + 0.3], 'herbes_feuille')
    # pichet de jus
    m.box([2, 0.6, 1.5], [7, 7.2, 6], {'u': 'jus_orange', '*': 'pichet'}, faces='nsewu', uv={'u': 'full', '*': 'full'})
    m.box([7, 2, 3.3], [8, 2.6, 4.2], 'verre')
    m.box([7, 5.6, 3.3], [8, 6.2, 4.2], 'verre')
    m.box([8, 2, 3.3], [8.6, 6.2, 4.2], 'verre')
    m.box([1.4, 6.4, 3.3], [2, 7.2, 4.2], 'verre')
    # demi-orange coupée, face vers le haut
    m.disc(12, 3.5, 1.9, 0.6, 2.2, {'u': 'orange_tranche', '*': 'orange_peau'}, faces='nsewu', uv={'u': 'full', '*': 'size'})
    return m


def shaker():
    m = Model('shaker', 'hxrpmetiers:blocks/inox_poli', aabb=(1, 0, 1, 15, 11, 15), gui_scale=0.85, gui_dy=1.0)
    m.box([1, 0, 1], [15, 0.5, 15], {'u': 'shaker_tapis', '*': 'caoutchouc'}, uv={'u': 'proj', '*': 'size'})
    m.disc(7, 6.5, 2.4, 0.5, 6.6, 'inox_poli')
    m.disc(7, 6.5, 2.1, 6.6, 7.6, 'inox_poli')
    m.box([4.5, 6.4, 4.0], [9.5, 6.8, 9.0], 'acier_sombre', faces='nswe')
    m.disc(7, 6.5, 1.5, 7.6, 9.6, 'inox_poli')
    m.box([6.4, 9.6, 5.9], [7.6, 10.6, 7.1], 'inox_poli')
    # verre à cocktail
    m.disc(11.5, 10.5, 1.6, 0.5, 0.8, 'verre')
    m.box([11.3, 0.8, 10.3], [11.7, 3.3, 10.7], 'verre')
    m.disc(11.5, 10.5, 1.4, 3.3, 4, 'verre')
    m.disc(11.5, 10.5, 2, 4, 4.8, 'verre')
    m.disc(11.5, 10.5, 2.6, 4.8, 5.5, {'u': 'cocktail', '*': 'verre'}, uv={'u': 'full', '*': 'size'})
    m.box([13.45, 4.85, 9.9], [14.15, 6.2, 11.1], 'orange_peau')
    m.box([11.2, 5.5, 10.2], [11.4, 8, 10.4], 'bois_clair')
    m.box([10.9, 7, 9.9], [11.7, 7.8, 10.7], 'rouge_email')
    # doseur double (jigger)
    m.disc(3, 11.5, 1.1, 0.5, 2, 'inox_poli')
    m.box([2.5, 2, 11], [3.5, 2.4, 12], 'inox_poli')
    m.disc(3, 11.5, 1.1, 2.4, 4, 'inox_poli')
    # glaçons
    m.box([10.5, 0.5, 2.5], [12, 2, 4], 'glacon')
    m.box([12.6, 0.5, 3.6], [13.9, 1.7, 4.9], 'glacon', rot=('y', 22.5, (13.2, 1, 4.2)))
    return m


def doseur():
    m = Model('doseur', 'hxrpmetiers:blocks/bois_fonce', aabb=(0.5, 0, 1, 15.5, 10, 15))
    m.box([1, 0, 1], [15, 1, 15], 'bois_fonce')
    m.box([1, 1, 11], [15, 5, 15], 'bois_fonce')
    m.box([1, 5, 14], [15, 11, 15], 'bois_fonce')
    for x in (0.5, 15):
        m.box([x, 0, 6], [x + 0.5, 9.5, 15], 'bois_fonce')
    m.box([1, 2.4, 6], [15, 3, 6.6], 'laiton')
    m.box([1, 7.2, 11], [15, 7.8, 11.6], 'laiton')
    lows = [(2, 'paprika'), (6.6, 'curcuma'), (11.2, 'herbes')]
    for (x, n) in lows:
        m.box([x, 1, 7.2], [x + 2.8, 5.4, 10], {'u': 'bois_fonce', '*': 'bocal_' + n}, uv={'u': 'size', '*': 'full'})
        m.box([x + 0.2, 5.4, 7.4], [x + 2.6, 6.2, 9.8], 'bois_fonce')
    highs = [(2, 'poivre'), (6.6, 'sel'), (11.2, 'safran')]
    for (x, n) in highs:
        m.box([x, 5, 11.6], [x + 2.8, 9.4, 14], {'u': 'bois_fonce', '*': 'bocal_' + n}, uv={'u': 'size', '*': 'full'})
        m.box([x + 0.2, 9.4, 11.8], [x + 2.6, 10.2, 13.8], 'laiton')
    # moulin à poivre, salière, cuillère doseuse
    m.disc(13, 3, 1.2, 1, 6.5, 'moulin')
    m.box([12.5, 6.5, 2.5], [13.5, 7.3, 3.5], 'laiton')
    m.box([9.3, 1, 2], [11.1, 4.3, 3.8], 'saliere', uv='full')
    m.box([9.3, 4.3, 2], [11.1, 5, 3.8], 'chrome')
    m.box([2, 1, 2.4], [5.2, 1.4, 3], 'laiton')
    m.disc(6.2, 2.7, 1.1, 1, 1.8, {'u': 'epices', '*': 'laiton'}, faces='nsewu', uv={'u': 'size', '*': 'size'})
    return m


def frigo():
    m = Model('frigo', 'hxrpmetiers:blocks/menthe')
    for x in (1, 13.5):
        for z in (1.5, 13.5):
            m.box([x, 0, z], [x + 1.5, 1, z + 1.5], 'chrome')
    m.box([0.5, 1, 1], [15.5, 15.5, 15.5], {'n': 'frigo_front', 'w': 'frigo_side', 'e': 'frigo_side', 's': 'menthe', 'd': 'noir_mat', 'u': 'frigo_top'},
          uv={'n': 'proj', 'u': 'proj', '*': 'size'})
    m.box([1, 15.5, 1.5], [15, 16, 15], {'u': 'frigo_top', '*': 'menthe'}, uv={'u': 'proj', '*': 'size'})
    m.box([0.7, 11, 0.3], [15.3, 15.3, 1], {'n': 'frigo_front', '*': 'menthe'}, faces='nweud', uv={'n': 'proj', '*': 'size'})
    m.box([0.7, 1.3, 0.3], [15.3, 10.8, 1], {'n': 'frigo_front', '*': 'menthe'}, faces='nweud', uv={'n': 'proj', '*': 'size'})
    for (y0, y1) in ((12, 14.6), (6.2, 10.2)):
        m.box([1.8, y0, -0.8], [2.6, y1, -0.1], 'chrome')
        m.box([1.9, y0, -0.1], [2.5, y0 + 0.5, 0.3], 'chrome', faces='nsweu')
        m.box([1.9, y1 - 0.5, -0.1], [2.5, y1, 0.3], 'chrome', faces='nsweu')
    return m


def poubelle():
    m = Model('poubelle', 'hxrpmetiers:blocks/acier', aabb=(2.5, 0, 1.5, 13.5, 15, 13.5))
    m.disc(8, 8, 5.2, 0, 0.8, 'caoutchouc')
    m.cross(8, 8, 5, 4, 4.4, 0.8, 13, 'poubelle_cote', avant=('poubelle_face', [0, 0, 8, 12.2]))
    m.disc(8, 8, 5.4, 13, 13.8, 'noir_mat')
    m.disc(8, 8, 4.6, 13.8, 14.6, 'inox_poli')
    m.disc(8, 8, 2.6, 14.6, 15, 'inox_poli')
    m.box([6, 0.2, 1.6], [10, 0.9, 3], 'noir_mat')
    m.box([6, 13, 12.8], [10, 14, 13.6], 'noir_mat', faces='nsewu')
    return m


BUILDERS = [four, fourneau, plan_de_travail, bol, mortier, tamis, grill, friteuse, marmite, presse, shaker, doseur, frigo, poubelle]
