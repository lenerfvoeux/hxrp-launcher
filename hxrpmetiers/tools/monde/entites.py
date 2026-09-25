"""
Les animaux du Gourmet : modèles (boîtes Minecraft, animations par rôle) et textures peintes.

Chaque modèle est décrit en espace « modèle » de Minecraft (unités = pixels, y vers le bas,
y = 24 au niveau du sol, l'avant de l'animal vers -z). Les boîtes sont rangées automatiquement dans
la texture ; chaque face est peinte selon la disposition exacte de ModelBox (1.12).

Rôles animés côté Java (ModeleJson) : tete, patte_ag, patte_ad, patte_pg, patte_pd, aile_g, aile_d,
queue (poissons : ondulation), nageoire, pince_g, pince_d, patte_crabe, antenne.
"""
import json
import math
import os
import zlib

import numpy as np
from PIL import Image


def hexc(s):
    s = s.lstrip('#')
    return np.array([int(s[i:i + 2], 16) for i in (0, 2, 4)], float)


def rnd(key):
    return np.random.RandomState(zlib.crc32(key.encode()) & 0x7fffffff)


# ============================================================================ modèle
class Boite:
    def __init__(self, pos, taille, peintre, gonfle=0.0, miroir=False):
        self.pos = [float(v) for v in pos]
        self.taille = [int(v) for v in taille]
        self.peintre = peintre
        self.gonfle = gonfle
        self.miroir = miroir
        self.uv = None

    def region(self):
        w, h, d = self.taille
        return 2 * (d + w), d + h


class Partie:
    def __init__(self, nom, pivot, rot=(0, 0, 0), role=None):
        self.nom, self.pivot, self.rot, self.role = nom, [float(v) for v in pivot], [float(v) for v in rot], role
        self.boites, self.enfants = [], []

    def boite(self, pos, taille, peintre, gonfle=0.0):
        self.boites.append(Boite(pos, taille, peintre, gonfle))
        return self

    def enfant(self, p):
        self.enfants.append(p)
        return p


class Modele:
    def __init__(self, id, echelle=1.0, ombre=0.4):
        self.id, self.echelle, self.ombre = id, echelle, ombre
        self.parties = []
        self.tw = self.th = 0

    def partie(self, nom, pivot, rot=(0, 0, 0), role=None):
        p = Partie(nom, pivot, rot, role)
        self.parties.append(p)
        return p

    def toutes_boites(self):
        out = []

        def visit(p):
            out.extend(p.boites)
            for e in p.enfants:
                visit(e)
        for p in self.parties:
            visit(p)
        return out

    def ranger(self):
        """Range les boîtes dans une texture (étagères), largeur 64 ou 128."""
        bs = sorted(self.toutes_boites(), key=lambda b: -b.region()[1])
        for tw in (64, 128):
            x = y = rowh = 0
            ok = True
            for b in bs:
                rw, rh = b.region()
                if rw > tw:
                    ok = False
                    break
                if x + rw > tw:
                    x, y, rowh = 0, y + rowh, 0
                b.uv = (x, y)
                x += rw
                rowh = max(rowh, rh)
            if ok:
                th = 32
                while th < y + rowh:
                    th *= 2
                if th <= tw:
                    self.tw, self.th = tw, th
                    return
        raise SystemExit('Texture trop petite pour ' + self.id)

    def texture(self):
        self.ranger()
        img = np.zeros((self.th, self.tw, 4), float)
        for b in self.toutes_boites():
            u, v = b.uv
            w, h, d = b.taille
            b.peintre(img, faces(u, v, w, h, d), b)
        return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), 'RGBA')

    def json(self):
        def enc(p):
            d = {'nom': p.nom, 'pivot': p.pivot, 'rot': p.rot,
                 'boites': [{'uv': list(b.uv), 'pos': b.pos, 'taille': b.taille, 'gonfle': b.gonfle} for b in p.boites]}
            if p.role:
                d['role'] = p.role
            if p.enfants:
                d['enfants'] = [enc(e) for e in p.enfants]
            return d
        return {'texture': [self.tw, self.th], 'echelle': self.echelle, 'ombre': self.ombre, 'parties': [enc(p) for p in self.parties]}


def faces(u, v, w, h, d):
    """Rectangles (x0, y0, x1, y1) des six faces dans la texture, disposition de ModelBox.
    gauche (-x) : l'avant de la boîte est au bord droit ; droite (+x) : l'avant est au bord gauche.
    haut / bas : la rangée du bas est l'avant."""
    return {
        'gauche': (u, v + d, u + d, v + d + h),
        'avant': (u + d, v + d, u + d + w, v + d + h),
        'droite': (u + d + w, v + d, u + 2 * d + w, v + d + h),
        'arriere': (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
        'haut': (u + d, v, u + d + w, v + d),
        'bas': (u + d + w, v, u + d + 2 * w, v + d),
    }


# ============================================================================ peinture
def remplir(img, r, col, bruit=10, seed='x', clair=1.0):
    x0, y0, x1, y1 = r
    if x1 <= x0 or y1 <= y0:
        return
    rs = rnd(seed + str(r))
    c = hexc(col) * clair
    n = rs.randint(-bruit, bruit + 1, size=(y1 - y0, x1 - x0, 1)) if bruit else 0
    img[y0:y1, x0:x1, :3] = np.clip(c + n, 0, 255)
    img[y0:y1, x0:x1, 3] = 255


def degrade(img, r, haut, bas, bruit=8, seed='d'):
    x0, y0, x1, y1 = r
    if x1 <= x0 or y1 <= y0:
        return
    rs = rnd(seed + str(r))
    a, b = hexc(haut), hexc(bas)
    for j in range(y1 - y0):
        t = j / max(1, y1 - y0 - 1)
        c = a * (1 - t) + b * t
        for i in range(x1 - x0):
            img[y0 + j, x0 + i, :3] = np.clip(c + rs.randint(-bruit, bruit + 1), 0, 255)
            img[y0 + j, x0 + i, 3] = 255


def px(img, x, y, col):
    if 0 <= y < img.shape[0] and 0 <= x < img.shape[1]:
        img[y, x, :3] = hexc(col)
        img[y, x, 3] = 255


def P_uni(col, bruit=10, haut=None, bas=None, avant=None, arriere=None, taches=None, seed=''):
    def p(img, F, b):
        for k, r in F.items():
            cc = {'haut': haut, 'bas': bas, 'avant': avant, 'arriere': arriere}.get(k) or col
            clair = 1.08 if k == 'haut' else 0.85 if k == 'bas' else 1.0
            remplir(img, r, cc, bruit, seed + k, clair)
        if taches:
            tc, dens = taches
            rs = rnd('t' + seed)
            for k in ('gauche', 'droite', 'haut'):
                x0, y0, x1, y1 = F[k]
                for y in range(y0, y1):
                    for x in range(x0, x1):
                        if rs.rand() < dens:
                            px(img, x, y, tc)
    return p


def P_patte(col, sabot, bruit=8, seed=''):
    def p(img, F, b):
        for k, r in F.items():
            remplir(img, r, col, bruit, seed + k)
        for k in ('gauche', 'avant', 'droite', 'arriere'):
            x0, y0, x1, y1 = F[k]
            remplir(img, (x0, max(y0, y1 - 2), x1, y1), sabot, 4, seed + 's' + k)
        remplir(img, F['bas'], sabot, 4, seed + 'sb')
    return p


def oeil(img, F, cote, y, depuis_avant, col='#101010', reflet=True):
    """Œil peint sur les côtés d'une tête, à « depuis_avant » pixels de l'avant."""
    for k in ('gauche', 'droite'):
        x0, y0, x1, y1 = F[k]
        x = x1 - 1 - depuis_avant if k == 'gauche' else x0 + depuis_avant
        px(img, x, y0 + y, col)
        if reflet:
            px(img, x + (1 if k == 'droite' else -1) * 0, y0 + y - 1, '#ffffff') if False else None


def P_tete(col, oeil_y, oeil_av, nez=None, nez_h=None, bruit=8, bas=None, seed='', joue=None):
    def p(img, F, b):
        for k, r in F.items():
            remplir(img, r, (bas if k == 'bas' and bas else col), bruit, seed + k, 1.06 if k == 'haut' else 1.0)
        oeil(img, F, None, oeil_y, oeil_av)
        if nez:
            x0, y0, x1, y1 = F['avant']
            hh = nez_h or 2
            remplir(img, (x0, y1 - hh, x1, y1), nez, 4, seed + 'nez')
            if x1 - x0 >= 3:
                px(img, x0 + 1, y1 - hh, '#1a1010')
                px(img, x1 - 2, y1 - hh, '#1a1010')
        if joue:
            for k in ('gauche', 'droite'):
                x0, y0, x1, y1 = F[k]
                remplir(img, (x0, y1 - 2, x1, y1), joue, 4, seed + 'j' + k)
    return p


def P_forme(col, forme, bruit=8, bord=None, raies=None, seed=''):
    """Plan découpé (nageoire, éventail de plumes…) : forme(nx, ny) -> bool, nx, ny dans [0, 1]."""
    def p(img, F, b):
        for k, (x0, y0, x1, y1) in F.items():
            if x1 <= x0 or y1 <= y0:
                continue
            rs = rnd(seed + k)
            W, H = x1 - x0, y1 - y0
            for j in range(H):
                for i in range(W):
                    nx = (i + 0.5) / W
                    if k == 'gauche':
                        nx = 1 - nx          # même orientation des deux côtés : 0 = avant
                    ny = (j + 0.5) / H
                    if not forme(nx, ny):
                        img[y0 + j, x0 + i, 3] = 0
                        continue
                    c = hexc(col) + rs.randint(-bruit, bruit + 1)
                    if raies and raies(nx, ny):
                        c = c * 0.7
                    img[y0 + j, x0 + i, :3] = np.clip(c, 0, 255)
                    img[y0 + j, x0 + i, 3] = 255
    return p


def P_poisson(dos, ventre, oeil_av=2, taches=None, rayures=None, ligne=None, bande=None, seed=''):
    """Corps de poisson : dégradé dos → ventre sur les flancs, œil et ouïe vers l'avant."""
    def p(img, F, b):
        w, h, d = b.taille
        degrade(img, F['gauche'], dos, ventre, 6, seed + 'g')
        degrade(img, F['droite'], dos, ventre, 6, seed + 'd')
        remplir(img, F['haut'], dos, 6, seed + 'h', 0.9)
        remplir(img, F['bas'], ventre, 6, seed + 'b', 1.02)
        degrade(img, F['avant'], dos, ventre, 4, seed + 'a')
        degrade(img, F['arriere'], dos, ventre, 4, seed + 'r')
        for k in ('gauche', 'droite'):
            x0, y0, x1, y1 = F[k]
            L = x1 - x0

            def X(i):  # i = distance depuis l'avant
                return x1 - 1 - i if k == 'gauche' else x0 + i
            if bande:
                for i in range(3, L):
                    px(img, X(i), y0 + h // 2, bande)
            if ligne:
                for i in range(3, L):
                    px(img, X(i), y0 + max(1, h // 2 - 1), ligne)
            if rayures:
                for i in range(4, L - 1, 2):
                    for j in range(0, max(1, h // 2)):
                        if (i // 2 + j) % 3 == 0:
                            px(img, X(i), y0 + j, rayures)
            if taches:
                rs = rnd(seed + 't' + k)
                for i in range(3, L):
                    for j in range(0, max(1, h // 2 + 1)):
                        if rs.rand() < 0.22:
                            px(img, X(i), y0 + j, taches)
            # tête : œil, ouïe, bouche
            ey = y0 + max(0, h // 2 - 1)
            px(img, X(oeil_av), ey, '#101014')
            px(img, X(oeil_av), ey - 1 if h > 2 else ey, '#e8eef4') if h > 3 else None
            for j in range(1, h - 1):
                px(img, X(oeil_av + 2), y0 + j, '#5a4a4a' if h > 3 else dos)
        x0, y0, x1, y1 = F['avant']
        if y1 - y0 >= 3:
            for i in range(x0, x1):
                if x0 < i < x1 - 1:
                    px(img, i, y0 + (y1 - y0) // 2 + 1, '#7a6060')
    return p


# ============================================================================ les espèces
def poisson(id, L, H, W, dos, ventre, nageoire, echelle=1.0, **kw):
    m = Modele(id, echelle=echelle, ombre=0.25)
    yc = 24 - H / 2 - 1
    corps = m.partie('corps', (0, yc, 0))
    corps.boite((-W / 2, -H / 2, -L / 2), (W, H, L), P_poisson(dos, ventre, seed=id, **kw))
    # dorsale
    dh = max(2, H // 2 + 1)
    corps.boite((0, -H / 2 - dh, -L * 0.15), (0, dh, max(3, L // 3)), P_forme(nageoire, lambda x, y: y > 0.9 - x * 0.9, seed=id + 'dor'))
    # pectorales et ventrales
    for s in (-1, 1):
        corps.boite((s * W / 2, H / 2 - 1, -L * 0.25), (0, 2, 3), P_forme(nageoire, lambda x, y: y < 0.3 + x * 0.7, seed=id + 'pec'))
    queue = corps.enfant(Partie('queue', (0, 0, L / 2), role='queue'))
    queue.boite((-max(1, W - 1) / 2, -max(1, H - 2) / 2, 0), (max(1, W - 1), max(1, H - 2), 2), P_uni(dos, 6, bas=ventre, seed=id + 'q'))
    th = H + 3
    queue.boite((0, -th / 2, 1.5), (0, th, max(3, L // 4)), P_forme(nageoire, lambda x, y: abs(y - 0.5) < 0.14 + x * 0.4 and not (x > 0.55 and abs(y - 0.5) < (x - 0.55) * 0.5), seed=id + 'cau'))
    return m


def crevette():
    m = Modele('crevette', echelle=0.8, ombre=0.2)
    rose, clair = '#e89a86', '#f8c8b8'
    tete = m.partie('tete', (0, 21.5, -2), role='tete')
    tete.boite((-1.5, -1.5, -3), (3, 3, 4), P_uni(rose, 8, bas=clair, seed='cr'))
    tete.boite((-0.5, -1.2, -6), (1, 1, 3), P_uni('#d87a66', 6, seed='ro'))
    for s in (-1, 1):
        tete.boite((s * 0.8, -1.5, -11), (0, 1, 8), P_forme('#c86a56', lambda x, y: True, seed='an'))
    for s in (-1, 1):
        tete.boite((s * 1.6, -2, -2.5), (0, 1, 1), P_uni('#101010', 0))
    s1 = tete.enfant(Partie('seg1', (0, 0, 1), rot=(-0.3, 0, 0), role='antenne'))
    s1.boite((-1.5, -1.3, 0), (3, 3, 3), P_uni(rose, 8, bas=clair, seed='s1'))
    s2 = s1.enfant(Partie('seg2', (0, 0, 3), rot=(-0.45, 0, 0)))
    s2.boite((-1, -1.1, 0), (2, 2, 3), P_uni(rose, 8, bas=clair, seed='s2'))
    s3 = s2.enfant(Partie('queue', (0, 0, 3), rot=(-0.55, 0, 0), role='queue'))
    s3.boite((-1, -0.8, 0), (2, 2, 2), P_uni(rose, 8, seed='s3'))
    s3.boite((-2, 0, 2), (4, 0, 3), P_forme('#e87a66', lambda x, y: abs(x - 0.5) < 0.2 + y * 0.35, seed='ev'))
    for k in range(4):
        tete.boite((-1.5, 1.5, -2 + k), (3, 2, 0), P_forme('#e8a896', lambda x, y: x < 0.2 or x > 0.8, seed='p%d' % k))
    return m


def crabe():
    m = Modele('crabe', echelle=1.0, ombre=0.4)
    rouge, clair = '#d8502a', '#f0a080'
    c = m.partie('carapace', (0, 21, 0), rot=(0, math.pi / 2, 0))
    c.boite((-4, -2, -3), (8, 3, 6), P_uni(rouge, 10, bas=clair, taches=('#b8401e', 0.15), seed='cb'))
    for s in (-1, 1):
        c.boite((s * 2 - 0.5, -4, -3), (1, 2, 1), P_uni('#e8e0d0', 4, haut='#101010', seed='oe'))
    for s, role in ((-1, 'pince_g'), (1, 'pince_d')):
        bras = c.enfant(Partie(role, (s * 3.5, 0, -3), rot=(0, -s * 0.5, 0), role=role))
        bras.boite((-1, -1, -3), (2, 2, 3), P_uni(rouge, 8, seed='br'))
        bras.boite((-1.5, -1.5, -6), (3, 3, 3), P_uni('#c8401e', 8, avant='#401810', seed='pi'))
    for s in (-1, 1):
        for k in range(3):
            pt = c.enfant(Partie('patte_%d_%d' % (s, k), (s * 4, 0.5, -1.5 + k * 1.6), rot=(0, 0, s * 0.45), role='patte_crabe'))
            pt.boite((0 if s > 0 else -5, -0.5, -0.5), (5, 1, 1), P_uni(rouge, 6, seed='pa'))
    return m


def quadrupede(id, dims, couleurs, extras=None, echelle=1.0, ombre=0.6):
    """Corps, cou/tête, quatre pattes. dims : (lc, hc, pc, h_pattes, l_tete, h_tete, p_tete)."""
    lc, hc, pc, hp, lt, ht, pt = dims
    pelage, ventre, sabot, museau = couleurs
    m = Modele(id, echelle=echelle, ombre=ombre)
    ybas = 24 - hp
    corps = m.partie('corps', (0, ybas - hc / 2, 0))
    corps.boite((-lc / 2, -hc / 2, -pc / 2), (lc, hc, pc), P_uni(pelage, 10, bas=ventre, seed=id + 'c'))
    tete = m.partie('tete', (0, ybas - hc + 1, -pc / 2), role='tete')
    tete.boite((-lt / 2, -ht, -pt), (lt, ht, pt), P_tete(pelage, 1, 1, nez=museau, nez_h=2, seed=id + 't'))
    for nom, sx, sz in (('patte_ag', -1, -1), ('patte_ad', 1, -1), ('patte_pg', -1, 1), ('patte_pd', 1, 1)):
        p = m.partie(nom, (sx * (lc / 2 - 1.5), ybas, sz * (pc / 2 - 2)), role=nom)
        p.boite((-1, 0, -1), (2 if lc < 10 else 3, hp, 2 if lc < 10 else 3), P_patte(pelage, sabot, seed=id + nom))
    if extras:
        extras(m, corps, tete, dims)
    return m


def cerf():
    """Cerf : long cou relevé portant la tête, bois ramifiés, queue blanche."""
    pelage, ventre = '#9a6a3a', '#e8d8b8'
    m = Modele('cerf', echelle=1.0, ombre=0.6)
    lc, hc, pc, hp = 6, 7, 14, 11
    ybas = 24 - hp
    corps = m.partie('corps', (0, ybas - hc / 2, 0))
    corps.boite((-lc / 2, -hc / 2, -pc / 2), (lc, hc, pc), P_uni(pelage, 10, bas=ventre, seed='cerfc'))
    corps.boite((-1.5, -hc / 2 - 6, -pc / 2 - 1), (3, 8, 4), P_uni(pelage, 8, avant=ventre, seed='cou'))
    corps.boite((-1, -hc / 2, pc / 2), (2, 2, 1), P_uni('#f4ece0', 4, seed='qu'))
    tete = m.partie('tete', (0, ybas - hc - 5, -pc / 2), role='tete')
    tete.boite((-2, -4, -6), (4, 4, 6), P_tete(pelage, 1, 1, nez='#2a1a14', nez_h=1, seed='cerft'))
    for s in (-1, 1):
        tete.boite((s * 2.5 - 0.5, -5, -1), (1, 2, 1), P_uni('#7a4a28', 6, seed='or'))
        tete.boite((s * 1.2 - 0.5, -9, -2), (1, 5, 1), P_uni('#d8c8a0', 6, seed='b1'))
        tete.boite((s * 2.2 - 0.5, -13, -2), (1, 4, 1), P_uni('#e0d4b0', 6, seed='b2'))
        tete.boite((s * 1.2 - 0.5, -11, -5), (1, 1, 3), P_uni('#d8c8a0', 6, seed='b3'))
        tete.boite((s * 3.2 - 0.5, -11, -2), (1, 1, 1), P_uni('#d8c8a0', 6, seed='b4'))
    for nom, sx, sz in (('patte_ag', -1, -1), ('patte_ad', 1, -1), ('patte_pg', -1, 1), ('patte_pd', 1, 1)):
        p = m.partie(nom, (sx * (lc / 2 - 1.5), ybas, sz * (pc / 2 - 2)), role=nom)
        p.boite((-1, 0, -1), (2, hp, 2), P_patte(pelage, '#3a2a1a', seed='cerf' + nom))
    return m


def sanglier():
    def ext(m, corps, tete, d):
        lc, hc, pc, hp, lt, ht, pt = d
        corps.boite((-1, -hc / 2 - 2, -pc / 2 + 1), (2, 2, pc - 4), P_uni('#2a1a10', 14, seed='crin'))
        tete.boite((-2, -3, -pt - 2), (4, 3, 2), P_uni('#5a3a28', 8, avant='#d8a098', seed='gr'))
        for s in (-1, 1):
            tete.boite((s * 2 - 0.5, -3, -pt - 1.5), (1, 2, 1), P_uni('#f4ecd8', 3, seed='def'))
            tete.boite((s * 2.5 - 0.5, -ht - 1, -2), (1, 2, 1), P_uni('#3a2418', 6, seed='or'))
        corps.boite((-0.5, -hc / 2 + 1, pc / 2), (1, 4, 1), P_uni('#3a2418', 6, seed='qu'))
    return quadrupede('sanglier', (9, 8, 15, 6, 6, 6, 6), ('#5a3a26', '#6a4a32', '#1a1008', '#3a2418'), ext, ombre=0.7)


def chevre():
    def ext(m, corps, tete, d):
        lc, hc, pc, hp, lt, ht, pt = d
        for s in (-1, 1):
            tete.boite((s * 1.5 - 0.5, -ht - 3, -2), (1, 3, 1), P_uni('#8a7a6a', 6, seed='co1'))
            tete.boite((s * 1.5 - 0.5, -ht - 3, 0), (1, 1, 3), P_uni('#7a6a5a', 6, seed='co2'))
            tete.boite((s * 2.5 - 0.5, -ht + 1, -1), (1, 1, 2), P_uni('#e8e0d8', 6, seed='or'))
        tete.boite((-0.5, 0, -pt + 0.5), (1, 3, 1), P_uni('#d8d0c4', 6, seed='bar'))
        corps.boite((-1, -hc / 2 - 2, pc / 2 - 1), (2, 3, 1), P_uni('#f0ece4', 6, seed='qu'))
    return quadrupede('chevre', (7, 7, 12, 9, 4, 5, 6), ('#f0ece4', '#e0dad0', '#4a3a2a', '#c8b8a8'), ext)


def volaille(id, corps_d, plumage, ventre, tete_col, bec, pattes, queue=None, cou=None):
    lc, hc, pc = corps_d
    m = Modele(id, echelle=1.0, ombre=0.4)
    yb = 24 - 5
    corps = m.partie('corps', (0, yb - hc / 2, 0))
    corps.boite((-lc / 2, -hc / 2, -pc / 2), (lc, hc, pc), P_uni(plumage, 12, bas=ventre, seed=id + 'c'))
    tete = m.partie('tete', (0, yb - hc + 1, -pc / 2 + 1), role='tete')
    hauteur_cou = 5 if id == 'dinde' else 4
    tete.boite((-1.5, -hauteur_cou - 3, -2), (3, hauteur_cou + 3, 3), P_tete(tete_col, 1, 1, seed=id + 't', joue=cou))
    if id == 'canard':
        tete.boite((-1.5, -hauteur_cou - 1, -5), (3, 1, 3), P_uni(bec, 6, seed='bec'))
        tete.boite((-1.6, -hauteur_cou + 2.5, -2.1), (3, 1, 3), P_uni('#f4f4f0', 4, seed='col'), gonfle=0.05)
    else:
        tete.boite((-0.5, -hauteur_cou - 1, -3.5), (1, 1, 2), P_uni(bec, 6, seed='bec'))
        tete.boite((-0.5, -hauteur_cou, -3), (1, 3, 1), P_uni('#c8201a', 6, seed='caroncule'))
    for s, role in ((-1, 'aile_g'), (1, 'aile_d')):
        a = m.partie(role, (s * lc / 2, yb - hc + 1, 0), role=role)
        a.boite((0 if s > 0 else -1, 0, -pc / 2 + 1), (1, hc - 2, pc - 2), P_uni(plumage, 10, seed=id + role, taches=('#2a5ab0', 0.08) if id == 'canard' else None))
    for nom, s in (('patte_ag', -1), ('patte_ad', 1)):
        p = m.partie(nom, (s * 1.5, yb, 0), role=nom)
        p.boite((-0.5, 0, -0.5), (1, 5, 1), P_uni(pattes, 4, seed=id + nom))
        p.boite((-1, 4.5, -2.5), (2, 0, 3), P_forme(pattes, lambda x, y: True, seed=id + 'pied'))
    if queue:
        queue(m, corps, (lc, hc, pc), yb)
    return m


def dinde():
    def eventail(m, corps, d, yb):
        lc, hc, pc = d
        q = m.partie('eventail', (0, yb - hc / 2, pc / 2 - 1), rot=(-0.25, 0, 0), role='queue_oiseau')
        q.boite((-8, -12, 0), (16, 12, 0), P_forme('#7a4a28', lambda x, y: (x - 0.5) ** 2 * 1.2 + (y - 1.0) ** 2 < 0.95 ** 2 * 0.72,
                                                  raies=lambda x, y: int(((x - 0.5) ** 2 + (y - 1) ** 2) ** 0.5 * 9) % 3 == 0, seed='ev'))
    return volaille('dinde', (7, 7, 9), '#6a4a30', '#3a2a1c', '#b8c0d8', '#d8c8a0', '#c89878', eventail, cou='#c8281e')


def canard():
    def queue_(m, corps, d, yb):
        lc, hc, pc = d
        corps.boite((-1.5, -hc / 2 - 1, pc / 2 - 1), (3, 2, 2), P_uni('#2a2a2a', 6, seed='qc'))
    return volaille('canard', (6, 5, 9), '#8a8478', '#c8c0b0', '#2a7a3a', '#e8b820', '#f08a20', queue_)


def especes():
    return [
        poisson('saumon', 14, 5, 3, '#4a5a70', '#e8ecf0', '#6a7488', taches='#1a2028'),
        poisson('truite', 11, 4, 3, '#5a6a3a', '#f0e8d0', '#7a8a4a', taches='#2a2a1a', bande='#e88a8a'),
        poisson('thon', 18, 7, 5, '#1a2a5a', '#d8dce4', '#e8c030', echelle=1.2, ligne='#6a8ab0'),
        poisson('cabillaud', 13, 5, 4, '#7a7a52', '#f0ece0', '#8a8a60', taches='#5a5a38', ligne='#f4f0e0'),
        poisson('sardine', 7, 3, 2, '#2a5a7a', '#eef2f6', '#4a7a98', taches='#1a2a3a'),
        poisson('maquereau', 9, 3, 2, '#2a6a6a', '#eef2f2', '#3a7a7a', rayures='#0a2a2a'),
        poisson('anchois', 6, 2, 2, '#4a6e8e', '#f0f4f8', '#6a8aa8', bande='#dfe8f2'),
        crevette(),
        crabe(),
        dinde(),
        canard(),
        cerf(),
        sanglier(),
        chevre(),
    ]


# ============================================================================ aperçu (rendu fidèle aux ModelBox)
def _rot(ax, a):
    c, s = math.cos(a), math.sin(a)
    if ax == 'x':
        return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])
    if ax == 'y':
        return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def quads_boite(b):
    x, y, z = b.pos
    w, h, d = b.taille
    g = b.gonfle
    x0, y0, z0, x1, y1, z1 = x - g, y - g, z - g, x + w + g, y + h + g, z + d + g
    P = {7: (x0, y0, z0), 0: (x1, y0, z0), 1: (x1, y1, z0), 2: (x0, y1, z0), 3: (x0, y0, z1), 4: (x1, y0, z1), 5: (x1, y1, z1), 6: (x0, y1, z1)}
    u, v = b.uv

    def q(ids, u1, v1, u2, v2):
        # TexturedQuad : [0]->(u2,v1) [1]->(u1,v1) [2]->(u1,v2) [3]->(u2,v2)
        return [P[i] for i in ids], [(u2, v1), (u1, v1), (u1, v2), (u2, v2)]
    return [
        q((4, 0, 1, 5), u + d + w, v + d, u + d + w + d, v + d + h),
        q((7, 3, 6, 2), u, v + d, u + d, v + d + h),
        q((4, 3, 7, 0), u + d, v, u + d + w, v + d),
        q((1, 2, 6, 5), u + d + w, v + d, u + d + w + w, v),
        q((0, 7, 2, 1), u + d, v + d, u + d + w, v + d + h),
        q((3, 4, 5, 6), u + d + w + d, v + d, u + d + w + d + w, v + d + h),
    ]


def rendu(m, tex, taille=256, yaw=35, pitch=20, anim=None):
    T = np.asarray(tex).astype(float) / 255.0
    th, tw = T.shape[:2]
    tris = []

    def visit(p, M, O):
        rx, ry, rz = p.rot
        if anim and p.role in anim:
            dx, dy, dz = anim[p.role]
            rx, ry, rz = rx + dx, ry + dy, rz + dz
        R = _rot('z', rz) @ _rot('y', ry) @ _rot('x', rx)
        M2 = M @ R
        O2 = O + M @ np.array(p.pivot)
        for b in p.boites:
            for pts, uvs in quads_boite(b):
                wp = [O2 + M2 @ np.array(pt) for pt in pts]
                tris.append((wp, uvs))
        for e in p.enfants:
            visit(e, M2, O2)
    for p in m.parties:
        visit(p, np.eye(3), np.zeros(3))
    img = np.zeros((taille, taille, 4))
    zb = np.full((taille, taille), np.inf)
    V = _rot('x', math.radians(pitch)) @ _rot('y', math.radians(yaw))
    allp = np.array([pt for wp, _ in tris for pt in wp])
    ctr = (allp.max(0) + allp.min(0)) / 2
    ext = (allp.max(0) - allp.min(0)).max()
    sc = taille * 0.8 / max(ext, 1)
    for wp, uvs in tris:
        # espace modèle : y vers le bas, avant vers -z ; on regarde depuis l'avant-gauche
        pts = [V @ ((pt - ctr) * np.array([1, 1, 1])) for pt in wp]
        s2 = [(p_[0] * sc + taille / 2, p_[1] * sc + taille / 2, p_[2]) for p_ in pts]
        for tri in ((0, 1, 2), (0, 2, 3)):
            a, b, c = [s2[i] for i in tri]
            ua, ub, uc = [np.array(uvs[i], float) for i in tri]
            minx, maxx = int(max(0, math.floor(min(a[0], b[0], c[0])))), int(min(taille - 1, math.ceil(max(a[0], b[0], c[0]))))
            miny, maxy = int(max(0, math.floor(min(a[1], b[1], c[1])))), int(min(taille - 1, math.ceil(max(a[1], b[1], c[1]))))
            if maxx < minx or maxy < miny:
                continue
            det = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1])
            if abs(det) < 1e-9:
                continue
            ys, xs = np.mgrid[miny:maxy + 1, minx:maxx + 1]
            pxx, pyy = xs + 0.5, ys + 0.5
            w0 = ((b[1] - c[1]) * (pxx - c[0]) + (c[0] - b[0]) * (pyy - c[1])) / det
            w1 = ((c[1] - a[1]) * (pxx - c[0]) + (a[0] - c[0]) * (pyy - c[1])) / det
            w2 = 1 - w0 - w1
            ins = (w0 >= -1e-6) & (w1 >= -1e-6) & (w2 >= -1e-6)
            if not ins.any():
                continue
            dd = w0 * a[2] + w1 * b[2] + w2 * c[2]
            uu = w0 * ua[0] + w1 * ub[0] + w2 * uc[0]
            vv = w0 * ua[1] + w1 * ub[1] + w2 * uc[1]
            tx = np.clip(np.floor(uu).astype(int), 0, tw - 1)
            ty = np.clip(np.floor(vv).astype(int), 0, th - 1)
            col = T[ty, tx]
            ok = ins & (col[..., 3] > 0.1) & (dd < zb[ys, xs])
            zb[ys[ok], xs[ok]] = dd[ok]
            cc = col[ok].copy()
            cc[:, 3] = 1
            img[ys[ok], xs[ok]] = cc
    return Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8), 'RGBA')
