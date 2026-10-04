"""
Petit moteur de pixel art pour les icônes du Gourmet.

Principe : chaque objet est une suite de « pièces » (masque + rampe de couleurs + éclairage).
Les pièces sont posées l'une sur l'autre ; chaque pièce reçoit un liseré sombre sur ses bords
(sélectif : plus foncé en bas à droite), et la silhouette finale un contour presque noir.
La lumière vient d'en haut à gauche, légèrement de face (vue trois-quarts).
"""
import colorsys
import math
import numpy as np
from PIL import Image

W = H = 32
OUTLINE = (27, 16, 12)
LIGHT = np.array([-0.55, -0.68, 0.49])
LIGHT = LIGHT / np.linalg.norm(LIGHT)

BAYER = np.array([[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]) / 16.0


# ----------------------------------------------------------------------------- couleurs
def hexc(s):
    s = s.lstrip('#')
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def tohex(c):
    return '#%02x%02x%02x' % tuple(int(v) for v in c[:3])


def mix(a, b, t):
    a, b = col(a), col(b)
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def col(c):
    if isinstance(c, str):
        return hexc(c)
    return tuple(int(v) for v in c[:3])


def _shift_hue(h, target, amount):
    d = ((target - h + 0.5) % 1.0) - 0.5
    return (h + d * amount) % 1.0


def ramp(base, n=5, dark=0.58, light=1.2, hue=True, sat=1.0):
    """Rampe de n couleurs, de la plus sombre à la plus claire, avec décalage de teinte
    (ombres vers le violet, lumières vers le jaune) comme en pixel art soigné."""
    r, g, b = [v / 255.0 for v in col(base)]
    h, s, v = colorsys.rgb_to_hsv(r, g, b)
    s = min(1.0, s * sat)
    out = []
    mid = (n - 1) / 2.0
    for i in range(n):
        k = (i - mid) / mid  # -1 .. 1
        if k < 0:
            vv = v * (1 + k * (1 - dark))
            ss = min(1.0, s * (1 - k * 0.25)) if s > 0.05 else s
            hh = _shift_hue(h, 0.72, -k * 0.10) if hue and s > 0.08 else h
        else:
            vv = min(1.0, v * (1 + k * (light - 1)) + k * 0.06)
            ss = s * (1 - k * 0.32)
            hh = _shift_hue(h, 0.15, k * 0.07) if hue and s > 0.08 else h
        rr, gg, bb = colorsys.hsv_to_rgb(hh, max(0, ss), max(0, min(1, vv)))
        out.append((int(rr * 255), int(gg * 255), int(bb * 255)))
    return out


def R(base, **kw):
    return ramp(base, **kw)


# ----------------------------------------------------------------------------- géométrie
_yy, _xx = np.mgrid[0:H, 0:W]
XX = _xx + 0.5
YY = _yy + 0.5


def ellipse(cx, cy, rx, ry):
    return ((XX - cx) / rx) ** 2 + ((YY - cy) / ry) ** 2 <= 1.0


def circle(cx, cy, r):
    return ellipse(cx, cy, r, r)


def rect(x0, y0, x1, y1):
    """Rectangle inclusif en coordonnées de pixels."""
    x0, y0, x1, y1 = int(round(x0)), int(round(y0)), int(round(x1)), int(round(y1))
    return (_xx >= x0) & (_xx <= x1) & (_yy >= y0) & (_yy <= y1)


def rrect(x0, y0, x1, y1, r=1):
    x0, y0, x1, y1 = int(round(x0)), int(round(y0)), int(round(x1)), int(round(y1))
    m = rect(x0, y0, x1, y1)
    if r <= 0:
        return m
    for (cx, cy, sx, sy) in ((x0, y0, 1, 1), (x1, y0, -1, 1), (x0, y1, 1, -1), (x1, y1, -1, -1)):
        for dy in range(r):
            for dx in range(r - dy):
                x, y = cx + sx * dx, cy + sy * dy
                if 0 <= x < W and 0 <= y < H and dx + dy < r:
                    m[y, x] = False
    return m


def polygon(pts):
    """Remplissage pair-impair d'un polygone (coordonnées continues)."""
    m = np.zeros((H, W), bool)
    n = len(pts)
    for j in range(H):
        y = j + 0.5
        xs = []
        for i in range(n):
            x0, y0 = pts[i]
            x1, y1 = pts[(i + 1) % n]
            if (y0 <= y < y1) or (y1 <= y < y0):
                xs.append(x0 + (y - y0) * (x1 - x0) / (y1 - y0))
        xs.sort()
        for k in range(0, len(xs) - 1, 2):
            a, b = xs[k], xs[k + 1]
            for i in range(W):
                if a <= i + 0.5 < b:
                    m[j, i] = True
    return m


def capsule(x0, y0, x1, y1, r0, r1=None):
    """Segment épais (rayon variable) : carotte, poireau, banane, os…"""
    if r1 is None:
        r1 = r0
    dx, dy = x1 - x0, y1 - y0
    L2 = dx * dx + dy * dy or 1e-6
    t = np.clip(((XX - x0) * dx + (YY - y0) * dy) / L2, 0, 1)
    px, py = x0 + t * dx, y0 + t * dy
    d = np.hypot(XX - px, YY - py)
    return d <= r0 + (r1 - r0) * t


def line_mask(x0, y0, x1, y1, width=1):
    m = np.zeros((H, W), bool)
    n = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
    for k in range(n + 1):
        t = k / n
        x = x0 + (x1 - x0) * t
        y = y0 + (y1 - y0) * t
        for ox in range(width):
            for oy in range(width):
                xi, yi = int(math.floor(x)) + ox, int(math.floor(y)) + oy
                if 0 <= xi < W and 0 <= yi < H:
                    m[yi, xi] = True
    return m


def points(pts):
    m = np.zeros((H, W), bool)
    for (x, y) in pts:
        if 0 <= x < W and 0 <= y < H:
            m[int(y), int(x)] = True
    return m


def arc_band(cx, cy, rx, ry, thick, a0=0, a1=360):
    outer = ellipse(cx, cy, rx, ry)
    inner = ellipse(cx, cy, max(0.1, rx - thick), max(0.1, ry - thick))
    ang = (np.degrees(np.arctan2(YY - cy, XX - cx)) + 360) % 360
    if a0 <= a1:
        sel = (ang >= a0) & (ang <= a1)
    else:
        sel = (ang >= a0) | (ang <= a1)
    return outer & ~inner & sel


# ----------------------------------------------------------------------------- éclairages
def L_sphere(cx, cy, rx, ry, flat=0.0):
    nx = np.clip((XX - cx) / rx, -1, 1)
    ny = np.clip((YY - cy) / ry, -1, 1)
    nz = np.sqrt(np.clip(1 - nx * nx - ny * ny, 0, 1)) + flat
    n = np.stack([nx, ny, nz], -1)
    n = n / np.linalg.norm(n, axis=-1, keepdims=True)
    return np.clip(n @ LIGHT * 0.5 + 0.5, 0, 1) ** 1.15


def L_cyl(x0, y0, x1, y1, r):
    """Cylindre couché le long d'un axe (éclairé en haut à gauche)."""
    dx, dy = x1 - x0, y1 - y0
    L = math.hypot(dx, dy) or 1e-6
    ux, uy = dx / L, dy / L
    nxv, nyv = -uy, ux  # normale dans le plan
    if nyv > 0 or (nyv == 0 and nxv > 0):
        nxv, nyv = -nxv, -nyv
    s = ((XX - x0) * nxv + (YY - y0) * nyv) / max(r, 0.5)  # -1..1 à travers
    s = np.clip(s, -1, 1)
    nz = np.sqrt(np.clip(1 - s * s, 0, 1))
    n = np.stack([s * nxv, s * nyv, nz], -1)
    return np.clip(n @ LIGHT * 0.5 + 0.5, 0, 1) ** 1.1


def L_vert(y0, y1, top=0.85, bottom=0.25):
    t = np.clip((YY - y0) / max(1e-6, (y1 - y0)), 0, 1)
    return top + (bottom - top) * t


def L_horiz(x0, x1, left=0.85, right=0.3):
    t = np.clip((XX - x0) / max(1e-6, (x1 - x0)), 0, 1)
    return left + (right - left) * t


def L_const(v=0.6):
    return np.full((H, W), v)


# ----------------------------------------------------------------------------- toile
def _neighbors(m):
    up = np.zeros_like(m); up[1:, :] = m[:-1, :]
    dn = np.zeros_like(m); dn[:-1, :] = m[1:, :]
    lf = np.zeros_like(m); lf[:, 1:] = m[:, :-1]
    rt = np.zeros_like(m); rt[:, :-1] = m[:, 1:]
    return up, dn, lf, rt


class Canvas:
    def __init__(self, w=W, h=H):
        self.px = np.zeros((h, w, 4), np.uint8)
        self.part = np.full((h, w), -1, int)
        self.n = 0

    # -------------------------------------------------------------- poser une pièce
    def shape(self, mask, rmp, L=None, edge=True, hl=0, dither=True, bands=(0.2, 0.42, 0.66, 0.86), clip=None,
              edge_top=True, edge_col=None):
        """Pose une pièce. rmp : rampe (liste sombre->clair) ou couleur unique.
        edge : liseré sélectif sur les bords de la pièce. hl : nb de pixels de reflet spéculaire."""
        mask = mask.copy()
        if clip is not None:
            mask &= clip
        if not mask.any():
            return mask
        if isinstance(rmp, (str, tuple)) and (isinstance(rmp, str) or len(rmp) == 3 and isinstance(rmp[0], int)):
            rmp = [col(rmp)]
        rmp = [col(c) for c in rmp]
        if L is None:
            L = L_const(0.6)
        n = len(rmp)
        if n == 1:
            idx = np.zeros((H, W), int)
        else:
            th = list(bands) if n == 5 else [i / n for i in range(1, n)]
            if dither:
                Ld = L + (BAYER[_yy % 4, _xx % 4] - 0.5) * 0.07
            else:
                Ld = L
            idx = np.digitize(Ld, th)
            idx = np.clip(idx, 0, n - 1)
        colors = np.array(rmp, np.uint8)[idx]
        self.px[mask, :3] = colors[mask]
        self.px[mask, 3] = 255
        pid = self.n
        self.n += 1
        self.part[mask] = pid
        if edge:
            up, dn, lf, rt = _neighbors(mask)
            dark = col(edge_col) if edge_col else rmp[0]
            mid = rmp[min(1, n - 1)] if n > 1 else rmp[0]
            bottom_right = mask & (~dn | ~rt)
            top_left = mask & (~up | ~lf) & ~bottom_right
            self.px[bottom_right, :3] = dark
            if edge_top:
                self.px[top_left, :3] = mid if n > 2 else dark
        if hl:
            vals = np.where(mask & ~(_edge(mask)), L, -1)
            flat = vals.flatten()
            order = np.argsort(-flat)[:hl]
            hc = mix(rmp[-1], (255, 255, 255), 0.55)
            for o in order:
                if flat[o] > 0:
                    y, x = divmod(o, W)
                    self.px[y, x, :3] = hc
        return mask

    def fill(self, mask, c, clip=None):
        """Aplat sans liseré (détails : pépins, grains, reflets)."""
        m = mask if clip is None else mask & clip
        self.px[m, :3] = col(c)
        self.px[m, 3] = 255
        return m

    def blend(self, mask, c, a):
        m = mask & (self.px[:, :, 3] == 255)
        cc = np.array(col(c), float)
        self.px[m, :3] = (self.px[m, :3] * (1 - a) + cc * a).astype(np.uint8)

    def shade(self, mask, f):
        """Assombrit (f<1) ou éclaircit (f>1) ce qui est déjà posé."""
        m = mask & (self.px[:, :, 3] == 255)
        v = self.px[m, :3].astype(float) * f
        self.px[m, :3] = np.clip(v, 0, 255).astype(np.uint8)

    def erase(self, mask):
        self.px[mask] = 0
        self.part[mask] = -1

    def opaque(self):
        return self.px[:, :, 3] == 255

    def speckle(self, mask, c, density, seed=1, avoid_edge=True):
        rng = np.random.RandomState(seed)
        m = mask & self.opaque()
        if avoid_edge:
            m &= ~_edge(m)
        r = rng.rand(H, W) < density
        self.fill(m & r, c)

    # -------------------------------------------------------------- finitions
    def outline(self, c=OUTLINE, diag=False):
        o = self.opaque()
        up, dn, lf, rt = _neighbors(o)
        ring = (~o) & (up | dn | lf | rt)
        if diag:
            d = np.zeros_like(o)
            d[1:, 1:] |= o[:-1, :-1]; d[1:, :-1] |= o[:-1, 1:]; d[:-1, 1:] |= o[1:, :-1]; d[:-1, :-1] |= o[1:, 1:]
            ring |= (~o) & d
        self.px[ring, :3] = col(c)
        self.px[ring, 3] = 255

    def shadow_under(self, cx, cy, rx, ry, a=0.35):
        """Ombre portée douce sous l'objet (avant de le dessiner)."""
        m = ellipse(cx, cy, rx, ry) & ~self.opaque()
        self.px[m, :3] = (20, 12, 10)
        self.px[m, 3] = int(255 * a)

    def image(self):
        return Image.fromarray(self.px, 'RGBA')

    def save(self, path):
        self.image().save(path)


def _edge(mask):
    up, dn, lf, rt = _neighbors(mask)
    return mask & (~up | ~dn | ~lf | ~rt)


def edge(mask):
    return _edge(mask)


def shift(mask, dx, dy):
    out = np.zeros_like(mask)
    ys, xs = np.nonzero(mask)
    ys2, xs2 = ys + dy, xs + dx
    ok = (ys2 >= 0) & (ys2 < H) & (xs2 >= 0) & (xs2 < W)
    out[ys2[ok], xs2[ok]] = True
    return out


def rng_pts(seed, mask, n):
    """n points pris au hasard (reproductible) dans un masque."""
    rs = np.random.RandomState(seed)
    ys, xs = np.nonzero(mask)
    if len(xs) == 0:
        return []
    idx = rs.choice(len(xs), size=min(n, len(xs)), replace=False)
    return [(int(xs[i]), int(ys[i])) for i in idx]
