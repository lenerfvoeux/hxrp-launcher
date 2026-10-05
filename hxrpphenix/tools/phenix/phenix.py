"""
Le Phénix de feu : squelette GeckoLib, peinture procédurale des plumes (4 images qui scintillent),
animations, et l'œuf de cendres de la renaissance.

    python3 tools/phenix/phenix.py            -> écrit les ressources du mod
    python3 tools/phenix/phenix.py apercu     -> planches d'aperçu dans tools/apercu/
"""
import math
import os
import sys
import zlib

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import geo  # noqa: E402

ICI = os.path.dirname(os.path.abspath(__file__))
RACINE = os.path.normpath(os.path.join(ICI, '..', '..'))
ASSETS = os.path.join(RACINE, 'src', 'main', 'resources', 'assets', 'hxrpphenix')
APERCU = os.path.join(RACINE, 'tools', 'apercu')
IMAGES = 4


# ============================================================================ couleurs
def hexc(s):
    s = s.lstrip('#')
    return np.array([int(s[i:i + 2], 16) for i in (0, 2, 4)], float)


BRAISE = hexc('#4a0a08')      # liserés, ombres de plumes
CRAMOISI = hexc('#8e1410')
ROUGE = hexc('#c8260f')
VERMILLON = hexc('#e6531a')
ORANGE = hexc('#f48c1e')
OR = hexc('#f9c443')
PAILLE = hexc('#ffe98c')
BLANC = hexc('#fffbe6')
CORNE = hexc('#e8ad2c')       # bec, serres
CORNE_SOMBRE = hexc('#6e3a0e')
CENDRE = hexc('#5c5652')
CENDRE_CLAIRE = hexc('#8a837c')
CENDRE_SOMBRE = hexc('#2e2a28')

FEU = [(0.0, CRAMOISI), (0.25, ROUGE), (0.5, VERMILLON), (0.7, ORANGE), (0.86, OR), (0.95, PAILLE), (1.0, BLANC)]


def rampe(t, pal=FEU):
    t = min(1.0, max(0.0, t))
    for (t0, c0), (t1, c1) in zip(pal, pal[1:]):
        if t <= t1:
            k = (t - t0) / max(t1 - t0, 1e-9)
            return c0 * (1 - k) + c1 * k
    return pal[-1][1]


def bruit(*cles):
    """Bruit déterministe dans [-1, 1] pour un texel."""
    h = zlib.crc32(repr(tuple(int(math.floor(c)) for c in cles)).encode()) & 0xffffffff
    return (h % 2001) / 1000.0 - 1.0


def coul(c, a=255, n=0.0, frame=0, p=None, k=7):
    c = np.array(c, float)
    if p is not None and k:
        c = c + bruit(p[0] * 1.0 + 0.01, p[1] * 1.0 + 0.01, p[2] * 1.0 + 0.01) * k
    return (*np.clip(c * (1 + n), 0, 255), a)


def scintille(frame, x, y, z=0.0):
    """Variation d'une flamme selon l'image (−1..1), lissée dans l'espace."""
    return math.sin(frame * math.pi / 2 + x * 0.9 + y * 0.6 + z * 0.4)


# ============================================================================ peintres
def P_corps(clair=0.0):
    """Plumage en écailles : dos cramoisi, flancs vermillon, poitrail et ventre dorés et lumineux."""
    def p(pt, cote, cube, frame):
        x, y, z = pt
        # hauteur relative dans le corps (0 = ventre, 1 = dos) ; le ventre brille
        oy, sy = cube.origine[1], max(cube.taille[1], 1)
        hv = (y - oy) / sy
        if cote == 'haut':
            base = 0.3
        elif cote == 'bas':
            base = 0.86
        else:
            base = 0.72 - 0.42 * hv
        base += clair
        # écailles : rangées décalées le long de z, arc sombre en bas de chaque écaille
        u = z
        v = y if cote in ('gauche', 'droite', 'avant', 'arriere') else x
        if cote in ('avant', 'arriere'):
            u = x
        rang = math.floor(v / 2.5)
        fu = ((u + (rang % 2) * 1.5) % 3.0) / 3.0
        fv = (v / 2.5) - rang
        arc = (fu - 0.5) ** 2 * 4 + (1 - fv) ** 2
        t = base + 0.08 * scintille(frame, z, y, x) * (1 if base > 0.6 else 0.3)
        c = rampe(t)
        if arc > 0.95 and fv < 0.45:
            c = c * 0.78 + BRAISE * 0.22
        elif fv > 0.75:
            c = c * 1.08
        return coul(c, p=pt, k=6)
    return p


def P_tete():
    def p(pt, cote, cube, frame):
        x, y, z = pt
        oy = cube.origine[1]
        hv = (y - oy) / cube.taille[1]
        c = rampe(0.7 - 0.38 * hv)
        if cote == 'haut':
            c = rampe(0.32)
        if cote == 'bas':
            c = rampe(0.84)
        if cote == 'avant' and y > 30.5:
            c = rampe(0.4)
        if cote in ('gauche', 'droite'):
            # œil incandescent
            ez, ey = -21.0, 29.8
            d = math.hypot(z - ez, (y - ey) * 1.1)
            if d < 1.05:
                return coul(BLANC, k=0)
            if d < 1.9:
                return coul(PAILLE if frame % 2 == 0 else OR * 0.35 + PAILLE * 0.65, k=0)
            if d < 2.6:
                c = c * 0.25 + ORANGE * 0.75
            # sourcil sombre au-dessus, larme dorée qui file vers l'arrière
            if 31.4 < y < 32.4 and -23.2 < z < -18.4:
                c = BRAISE * 0.75 + c * 0.25
            if 27.9 < y < 28.9 and -19.6 < z < -16:
                c = OR
        return coul(c, p=pt, k=5)
    return p


def P_bec(pointe=False):
    def p(pt, cote, cube, frame):
        x, y, z = pt
        t = (z - cube.origine[2]) / max(cube.taille[2], 1)   # 0 = bout du bec
        c = CORNE * (0.75 + 0.25 * t) + CORNE_SOMBRE * (0.25 - 0.25 * t)
        if pointe:
            c = CORNE_SOMBRE * 0.8 + CORNE * 0.2
        if not pointe and cote in ('gauche', 'droite') and abs(z + 25.5) < 0.6 and y > 29.2:
            c = CORNE_SOMBRE          # narine
        if cote == 'haut':
            c = c * 1.12
        return coul(c, p=pt, k=5)
    return p


def langues(pos, longueur, frame, periode=3.2, amp=0.35, graine=0.0):
    """Hauteur d'une crête de flammes à la position pos (0..1 le long de la base) : des langues de feu."""
    k = pos * longueur / periode + graine
    forme = 0.55 + 0.45 * abs(math.cos(math.pi * k))
    return forme * (1 - amp * 0.5 + amp * 0.5 * scintille(frame, pos * 6, graine))


def P_crete(frame_amp=1.0):
    """Plan vertical (x constant) : crête de plumes-flammes balayées vers l'arrière."""
    def p(pt, cote, cube, frame):
        x, y, z = pt
        oz, sz = cube.origine[2], max(cube.taille[2], 1)
        oy, sy = cube.origine[1], max(cube.taille[1], 1)
        u = (z - oz) / sz          # 0 = avant
        v = (y - oy) / sy          # 0 = base
        # profil : haute au milieu, basse devant, effilée derrière ; langues inclinées vers l'arrière
        prof = math.sin(math.pi * min(1, u * 1.15)) ** 0.7 * 0.9 + 0.1
        lim = prof * langues(u + v * 0.35, sz, frame, periode=3.0, graine=cube.origine[0])
        if v > lim:
            return None
        t = 0.3 + 0.7 * (v / max(lim, 1e-3))
        c = rampe(t)
        if ((u * sz + v * sy * 0.35) % 3.0) < 0.6 and t < 0.85:
            c = c * 0.7 + BRAISE * 0.3
        return coul(c, k=4, p=pt)
    return p


def P_barbe():
    def p(pt, cote, cube, frame):
        x, y, z = pt
        oz, sz = cube.origine[2], cube.taille[2]
        oy, sy = cube.origine[1], cube.taille[1]
        u = (z - oz) / sz
        v = 1 - (y - oy) / sy        # 0 = attache (en haut)
        lim = 0.35 + 0.65 * math.sin(math.pi * u) * (0.85 + 0.15 * scintille(frame, u * 4, 1))
        if v > lim:
            return None
        return coul(rampe(0.45 + 0.5 * v / lim), k=4, p=pt)
    return p


def P_os_aile():
    """Bord d'attaque de l'aile : petites plumes en écailles, cramoisi relevé d'orange."""
    corps = P_corps(-0.08)

    def p(pt, cote, cube, frame):
        x, y, z = pt
        c = corps((z, y, abs(x)), cote, cube, frame)
        return c
    return p


def plume_bande(s, n, largeur):
    """Position dans la plume n° (s en px le long de l'empilement) : (index, 0..1 dans la plume)."""
    i = math.floor(s / largeur)
    return i, (s / largeur) - i


def P_couvertures(z0, z1, periode=3.0, festons=1.6, t0=0.25, t1=0.72):
    """Plan horizontal de plumes rangées côte à côte le long de l'aile (x), pointes en festons à l'arrière."""
    def p(pt, cote, cube, frame):
        x, y, z = pt
        ax = abs(x)
        i, f = plume_bande(ax, 0, periode)
        fin = z1 - festons * (1 - math.sin(math.pi * f))
        if z > fin:
            return None
        r = (z - z0) / max(fin - z0, 1e-3)
        c = rampe(t0 + (t1 - t0) * r + 0.03 * bruit(i, 7))
        if f < 0.12 or f > 0.94:
            c = c * 0.6 + BRAISE * 0.4             # séparation des plumes
        elif abs(f - 0.5) < 0.09 and r > 0.15:
            c = c * 1.15                            # rachis clair
        if fin - z < 0.9:
            c = c * 0.75 + BRAISE * 0.25           # liseré de la pointe
        return coul(c, p=pt, k=5)
    return p


def P_secondaires(z0, z1):
    def p(pt, cote, cube, frame):
        x, y, z = pt
        ax = abs(x)
        i, f = plume_bande(ax, 0, 2.8)
        flamme = scintille(frame, i * 1.7, 2)
        pointe = (1 - abs(2 * f - 1)) ** 0.7
        fin = z1 - 4.2 * (1 - pointe) - 1.0 + flamme * 0.9
        if z > fin:
            return None
        r = (z - z0) / max(fin - z0, 1e-3)
        c = rampe(0.36 + 0.64 * r ** 1.1)
        if f < 0.1 or f > 0.95:
            c = c * 0.6 + BRAISE * 0.4
        elif abs(f - 0.5) < 0.08:
            c = c * 1.12
        return coul(c, p=pt, k=5)
    return p


def P_primaires(poignet_x, poignet_z):
    """Rémiges en éventail depuis le poignet : longues, séparées en doigts au bout de l'aile."""
    def p(pt, cote, cube, frame):
        x, y, z = pt
        dx, dz = abs(x) - poignet_x, z - poignet_z
        r = math.hypot(dx, dz)
        th = math.degrees(math.atan2(dz, dx))          # 0 = vers le bout de l'aile, 90 = vers l'arrière
        if th < -12 or th > 92:
            return None
        k = (th + 12) / 11.5
        i = math.floor(k)
        f = k - i
        L = 20.5 - 0.06 * max(0.0, th) ** 1.0 - (1.5 if i == 0 else 0)
        L += 0.9 * scintille(frame, i * 2.3, 3)
        # doigts : écart entre les plumes près du bout
        if r > L * 0.58 and (f < 0.16 or f > 0.9) and th < 40:
            return None
        bout = L - 2.4 * (1 - math.sin(math.pi * f)) ** 2
        if r > bout:
            return None
        t = r / max(bout, 1e-3)
        c = rampe(0.34 + 0.66 * t ** 1.15)
        if (f < 0.12 or f > 0.92) and r < L * 0.58:
            c = c * 0.62 + BRAISE * 0.38
        elif abs(f - 0.5) < 0.09:
            c = c * 1.12
        if r < 2.2:
            c = rampe(0.28)
        return coul(c, p=pt, k=5)
    return p


def P_queue(z0, z1, demi, flammes=False, t0=0.3, t1=0.75, separees=0.45):
    """Plumes de queue côte à côte (2,4 px), séparées vers leur pointe ; flammes au dernier segment."""
    def p(pt, cote, cube, frame):
        x, y, z = pt
        i, f = plume_bande(x + demi, 0, 2.4)
        cen = 1 - abs(x) / demi
        r0 = (z - z0) / max(z1 - z0, 1e-3)
        fin = z1 - 3.0 * (1 - cen) ** 1.2
        if flammes:
            pointe = (1 - abs(2 * f - 1)) ** 0.8
            fin = z1 - 7.0 * (1 - cen) ** 1.3 - 3.0 * (1 - pointe) + 1.3 * scintille(frame, i * 1.9, 4)
        if z > fin:
            return None
        if r0 > separees and (f < 0.13 or f > 0.92):
            return None                                   # jour entre les plumes
        r = (z - z0) / max(fin - z0, 1e-3)
        c = rampe(t0 + (t1 - t0) * r)
        if f < 0.16 or f > 0.9:
            c = c * 0.7 + BRAISE * 0.3
        elif abs(f - 0.5) < 0.1:
            c = c * 1.1
        return coul(c, p=pt, k=5)
    return p


def P_ruban():
    """Longue plume de parade : tige étroite puis ocelle de feu (goutte dorée au cœur blanc)."""
    def p(pt, cote, cube, frame):
        x, y, z = pt
        oz, sz = cube.origine[2], cube.taille[2]
        ox, sx = cube.origine[0], cube.taille[0]
        u = (z - oz) / sz                      # 0 = attache
        w = abs((x - ox) / sx - 0.5) * 2       # 0 = milieu
        if u < 0.78:
            if w > 0.35 + 0.3 * u:
                return None
            c = rampe(0.3 + 0.35 * u)
            if w < 0.2:
                c = c * 1.15
            return coul(c, k=4, p=pt)
        # ocelle
        e = (u - 0.78) / 0.22
        demi = math.sin(math.pi * min(1, e * 1.05)) ** 0.8 * (1.05 + 0.08 * scintille(frame, 3, 5))
        if w > demi:
            return None
        cc = math.hypot(w / max(demi, 1e-3), (e - 0.5) * 2)
        c = rampe(1.0 - 0.6 * cc)
        if 0.55 < cc < 0.7:
            c = CRAMOISI
        return coul(c, k=3, p=pt)
    return p


def P_patte(cuisse=False):
    def p(pt, cote, cube, frame):
        x, y, z = pt
        if cuisse:
            return coul(rampe(0.38 + 0.1 * ((y * 2) % 2)), p=pt, k=6)
        c = CORNE * 0.85 + ORANGE * 0.15
        if (y * 2) % 2 < 0.6:
            c = c * 0.8
        return coul(c, p=pt, k=5)
    return p


def P_doigt():
    def p(pt, cote, cube, frame):
        x, y, z = pt
        # la serre (bout du doigt) est sombre
        ext = cube.origine, cube.taille
        bout = False
        if cube.taille[2] >= cube.taille[0]:
            bout = z < cube.origine[2] + 0.9 if cube.origine[2] < 0 else z > cube.origine[2] + cube.taille[2] - 0.9
        else:
            bout = abs(x) > abs(cube.origine[0]) + cube.taille[0] - 0.9 if cube.origine[0] >= 0 else x < cube.origine[0] + 0.9
        return coul(CORNE_SOMBRE if bout else CORNE * 0.9, p=pt, k=4)
    return p


def P_oeuf():
    """Œuf de cendres : pierre grise mouchetée, fêlures où rougeoie la braise (pulsent d'une image à l'autre)."""
    def p(pt, cote, cube, frame):
        x, y, z = pt
        th = math.atan2(z, x)
        n = bruit(x * 1.0, y * 1.0, z * 1.0)
        c = CENDRE * (1 + 0.12 * n)
        if n > 0.75:
            c = CENDRE_CLAIRE
        elif n < -0.8:
            c = CENDRE_SOMBRE
        if cote == 'haut' or cote == 'bas':
            th = math.atan2(z, x)
        # quatre fêlures en zigzag qui montent
        braise = 0.0
        for k in range(4):
            a = k * math.pi / 2 + 0.6 * math.sin(y * 0.55 + k * 1.7) + 0.25 * math.sin(y * 1.9 + k)
            d = abs(math.atan2(math.sin(th - a), math.cos(th - a)))
            rr = math.hypot(x, z)
            if d * max(rr, 3) < 0.75 and 1.5 < y < 19:
                braise = max(braise, 1 - d * max(rr, 3) / 0.75)
        if cote in ('haut',) and math.hypot(x, z) < 2.2:
            braise = max(braise, 0.6)
        if braise > 0:
            puls = 0.75 + 0.25 * math.sin(frame * math.pi / 2)
            c = rampe(0.55 + 0.45 * braise * puls)
        return coul(c, k=0)
    return p


# ============================================================================ squelette
def modele():
    m = geo.Modele('phenix')
    racine = m.os('racine', (0, 0, 0))
    assiette = racine.os('assiette', (0, 24, 0))
    corps = assiette.os('corps', (0, 24, 0))
    corps.cube((-6, 18, -10), (12, 12, 10), P_corps(0.0), nom='poitrail')
    corps.cube((-5, 18.5, 0), (10, 10, 9), P_corps(0.02), nom='ventre')
    corps.cube((-4, 20, 9), (8, 7, 5), P_corps(-0.05), nom='croupion')
    corps.cube((-4.5, 25.5, -13), (9, 6, 4), P_corps(-0.02), nom='collerette')

    # cou dressé, tête à l'horizontale (rotations de repos ; les animations s'y ajoutent)
    cou = corps.os('cou', (0, 28, -11), (-20, 0, 0))
    cou.cube((-3, 25, -18.5), (6, 6, 9), P_corps(-0.04), nom='cou')
    tete = cou.os('tete', (0, 28.5, -18), (16, 0, 0))
    tete.cube((-4, 25.5, -24.5), (8, 7, 8), P_tete(), nom='crane')
    tete.cube((-1.5, 27.5, -29.5), (3, 3, 5), P_bec(), nom='bec')
    tete.cube((-1, 26.5, -30.5), (2, 2, 2), P_bec(True), nom='bec_pointe')
    tete.cube((0, 32, -25), (0, 12, 18), P_crete(), nom='crete')
    tete.cube((2.2, 31.5, -23), (0, 8, 13), P_crete(), rot=(0, 0, -20), pivot=(2.2, 31.5, -23), nom='aigrette_g')
    tete.cube((-2.2, 31.5, -23), (0, 8, 13), P_crete(), rot=(0, 0, 20), pivot=(-2.2, 31.5, -23), nom='aigrette_d')
    tete.cube((0, 21.5, -26.5), (0, 4, 6), P_barbe(), nom='barbe')
    mach = tete.os('bec_inf', (0, 27.4, -24.5))
    mach.cube((-1, 26.4, -28.2), (2, 1, 4), P_bec(), nom='bec_inf')

    for s, cote in ((1, 'g'), (-1, 'd')):
        def X(x0, w):          # boîte de x0 à x0 + w côté gauche, en miroir côté droit
            return x0 if s > 0 else -(x0 + w)
        aile = corps.os('aile_' + cote, (s * 5, 28, -6))
        aile.cube((X(5, 16), 26.5, -8), (16, 3, 5), P_os_aile(), nom='humerus_' + cote)
        aile.cube((X(5, 17), 28.2, -3), (17, 0, 12), P_couvertures(-3, 9), nom='couvertures_' + cote)
        bras = aile.os('avant_bras_' + cote, (s * 21, 28, -6))
        bras.cube((X(21, 14), 27, -7.5), (14, 2, 4), P_os_aile(), nom='radius_' + cote)
        bras.cube((X(21, 15), 28.0, -3.5), (15, 0, 18), P_secondaires(-3.5, 14.5), nom='secondaires_' + cote)
        main = bras.os('main_' + cote, (s * 35, 28, -6))
        main.cube((X(35, 10), 27.5, -6.5), (10, 1, 2), P_os_aile(), nom='main_' + cote)
        main.cube((X(34, 21), 27.8, -5), (21, 0, 19), P_primaires(34.5, -5.5), nom='primaires_' + cote)

    q1 = corps.os('queue1', (0, 25, 12))
    q1.cube((-5, 25, 12), (10, 0, 15), P_queue(12, 27, 5, t0=0.3, t1=0.52, separees=0.7), nom='queue1')
    q2 = q1.os('queue2', (0, 25, 26))
    q2.cube((-6, 24.8, 26), (12, 0, 15), P_queue(26, 41, 6, t0=0.48, t1=0.7, separees=0.35), nom='queue2')
    q3 = q2.os('queue3', (0, 25, 40))
    q3.cube((-7, 24.6, 40), (14, 0, 19), P_queue(40, 59, 7, flammes=True, t0=0.66, t1=1.0, separees=0.0), nom='queue3')
    for s, cote in ((1, 'g'), (-1, 'd')):
        r = q1.os('ruban_' + cote, (s * 2, 25.4, 13), (0, s * 7, 0))
        r.cube((s * 2 - 1.5, 25.4, 13), (3, 0, 50), P_ruban(), nom='ruban_' + cote)
        r2 = q1.os('ruban2_' + cote, (s * 4, 25.2, 13), (0, s * 17, 0))
        r2.cube((s * 4 - 1.5, 25.2, 13), (3, 0, 36), P_ruban(), nom='ruban2_' + cote)

    for s, cote in ((1, 'g'), (-1, 'd')):
        pt = corps.os('patte_' + cote, (s * 3, 20, 2))
        pt.cube((s * 3 - 1.5, 15, 0.5), (3, 6, 3), P_patte(True), nom='cuisse_' + cote)
        bas = pt.os('tarse_' + cote, (s * 3, 15.5, 2))
        bas.cube((s * 3 - 0.5, 9, 1.5), (1, 7, 1), P_patte(), nom='tarse_' + cote)
        bas.cube((s * 3 - 0.5, 8, -1.5), (1, 1, 3), P_doigt(), nom='doigt_av_' + cote)
        bas.cube((s * 3 + (0.5 if s > 0 else -2.5), 8, 1.5), (2, 1, 1), P_doigt(), nom='doigt_ext_' + cote)
        bas.cube((s * 3 - 0.5, 8, 2.5), (1, 1, 2), P_doigt(), nom='doigt_ar_' + cote)

    oeuf = racine.os('oeuf', (0, 0, 0))
    for (o, t) in (((-4, 0, -4), (8, 1, 8)), ((-6, 1, -6), (12, 3, 12)), ((-7, 4, -7), (14, 8, 14)),
                   ((-6, 12, -6), (12, 4, 12)), ((-5, 16, -5), (10, 3, 10)), ((-3, 19, -3), (6, 2, 6))):
        oeuf.cube(o, t, P_oeuf(), nom='oeuf')
    return m


# ============================================================================ animations
# Sens vérifiés dans l'aperçu (calcul identique à GeckoLib) :
#   rotation x positive : la tête (ou la pointe d'un os tourné vers l'avant) descend ; une patte part vers l'arrière ;
#   aile gauche (+x) : rotation z positive = l'aile s'abaisse, y négative = l'aile se rabat vers l'arrière.
#   Côté droit : y et z en miroir.
def S(f, phase=0.0, amp=1.0, base=0.0):
    """Expression Molang : base + amp·sin(t·f·360 + phase)."""
    return '%g+%g*math.sin(query.anim_time*%g+%g)' % (base, amp, f * 360, phase)


def animations():
    F = 1.15        # battements par seconde en vol
    A = {}
    pattes_rentrees = {
        'patte_g': {'rotation': [72, 0, 0]}, 'patte_d': {'rotation': [72, 0, 0]},
        'tarse_g': {'rotation': [25, 0, 0]}, 'tarse_d': {'rotation': [25, 0, 0]},
    }

    def rubans(f, amp):
        return {
            'ruban_g': {'rotation': [S(f, -80, amp), S(f * 0.5, 0, 3), 0]},
            'ruban_d': {'rotation': [S(f, -95, amp), S(f * 0.5, 180, 3), 0]},
            'ruban2_g': {'rotation': [S(f, -60, amp * 0.8), S(f * 0.5, 40, 4), 0]},
            'ruban2_d': {'rotation': [S(f, -70, amp * 0.8), S(f * 0.5, 220, 4), 0]},
        }

    def queue(f, amp):
        return {
            'queue1': {'rotation': [S(f, -40, amp * 0.6), 0, 0]},
            'queue2': {'rotation': [S(f, -80, amp * 0.8), 0, 0]},
            'queue3': {'rotation': [S(f, -120, amp), 0, 0]},
        }

    def battre(f, amp, leve=0.0, rabat=0.0):
        """Battement : amp en degrés, leve > 0 = ailes tenues hautes, rabat = aile ramenée en arrière à la remontée."""
        return {
            'aile_g': {'rotation': [0, S(f, 90, -rabat), S(f, 0, amp, -leve)]},
            'aile_d': {'rotation': [0, S(f, 90, rabat), S(f, 0, -amp, leve)]},
            'avant_bras_g': {'rotation': [0, S(f, 60, -rabat * 0.6), S(f, -35, amp * 0.45)]},
            'avant_bras_d': {'rotation': [0, S(f, 60, rabat * 0.6), S(f, -35, -amp * 0.45)]},
            'main_g': {'rotation': [0, 0, S(f, -70, amp * 0.35)]},
            'main_d': {'rotation': [0, 0, S(f, -70, -amp * 0.35)]},
        }

    vol = {}
    vol.update(battre(F, 38, 6, 6))
    vol.update(queue(F, 5))
    vol.update(rubans(F, 7))
    vol.update(pattes_rentrees)
    vol['corps'] = {'position': [0, S(F, 180, 1.2), 0]}
    vol['cou'] = {'rotation': [S(F, 200, 3, 2), 0, 0]}
    vol['tete'] = {'rotation': [S(F, 20, 3, 0), 0, 0]}
    A['vol'] = {'loop': True, 'bones': vol}

    plane = {}
    plane.update(battre(0.35, 4, 3, 0))
    plane.update(queue(0.4, 3))
    plane.update(rubans(0.5, 5))
    plane.update(pattes_rentrees)
    plane['corps'] = {'position': [0, S(0.35, 0, 0.6), 0]}
    A['plane'] = {'loop': True, 'bones': plane}

    plongee = {
        'aile_g': {'rotation': [0, -58, -14]}, 'aile_d': {'rotation': [0, 58, 14]},
        'avant_bras_g': {'rotation': [0, 30, 0]}, 'avant_bras_d': {'rotation': [0, -30, 0]},
        'main_g': {'rotation': [0, -24, S(4, 0, 4)]}, 'main_d': {'rotation': [0, 24, S(4, 0, -4)]},
        'cou': {'rotation': [8, 0, 0]}, 'tete': {'rotation': [6, 0, 0]},
        'patte_g': {'rotation': [-55, 0, 0]}, 'patte_d': {'rotation': [-55, 0, 0]},
        'tarse_g': {'rotation': [-20, 0, 0]}, 'tarse_d': {'rotation': [-20, 0, 0]},
    }
    plongee.update(queue(3.0, 2))
    plongee.update(rubans(3.5, 3))
    A['plongee'] = {'loop': True, 'bones': plongee}

    souffle = {}
    souffle.update(battre(1.6, 30, 10, 8))
    souffle.update(queue(1.6, 6))
    souffle.update(rubans(1.6, 8))
    souffle.update(pattes_rentrees)
    souffle['cou'] = {'rotation': [S(9, 0, 1.5, 30), 0, 0]}
    souffle['tete'] = {'rotation': [S(9, 90, 1.5, -4), 0, 0]}
    souffle['bec_inf'] = {'rotation': [S(7, 0, 4, 34), 0, 0]}
    souffle['corps'] = {'position': [0, S(1.6, 180, 1.5), 0]}
    A['souffle'] = {'loop': True, 'bones': souffle}

    boule = {}
    boule.update(battre(1.6, 30, 10, 8))
    boule.update(queue(1.6, 6))
    boule.update(rubans(1.6, 8))
    boule.update(pattes_rentrees)
    boule['cou'] = {'rotation': {'0.0': [0, 0, 0], '0.35': [-24, 0, 0], '0.5': [30, 0, 0], '0.75': [12, 0, 0], '1.0': [0, 0, 0]}}
    boule['tete'] = {'rotation': {'0.0': [0, 0, 0], '0.35': [-14, 0, 0], '0.5': [8, 0, 0], '1.0': [0, 0, 0]}}
    boule['bec_inf'] = {'rotation': {'0.0': [0, 0, 0], '0.3': [20, 0, 0], '0.5': [42, 0, 0], '0.8': [10, 0, 0], '1.0': [0, 0, 0]}}
    A['boule'] = {'loop': False, 'animation_length': 1.0, 'bones': boule}

    pluie = {}
    pluie.update(battre(2.2, 16, 46, 4))
    pluie.update(queue(2.2, 8))
    pluie.update(rubans(2.2, 10))
    pluie.update(pattes_rentrees)
    pluie['main_g'] = {'rotation': [0, 0, S(6, 0, 8, -10)]}
    pluie['main_d'] = {'rotation': [0, 0, S(6, 0, -8, 10)]}
    pluie['cou'] = {'rotation': [-14, 0, 0]}
    pluie['tete'] = {'rotation': [-10, 0, 0]}
    pluie['bec_inf'] = {'rotation': [S(5, 0, 10, 22), 0, 0]}
    A['pluie'] = {'loop': True, 'bones': pluie}

    tempete = {}
    tempete.update(battre(2.6, 12, 58, 0))
    tempete.update(queue(2.6, 10))
    tempete.update(rubans(2.6, 14))
    tempete.update(pattes_rentrees)
    tempete['corps'] = {'rotation': [0, 'query.anim_time*220', 0]}
    tempete['cou'] = {'rotation': [-30, 0, 0]}
    tempete['tete'] = {'rotation': [-22, 0, 0]}
    tempete['bec_inf'] = {'rotation': [S(4, 0, 6, 30), 0, 0]}
    A['tempete'] = {'loop': True, 'bones': tempete}

    # posé : ailes à la verticale rabattues le long des flancs (corde raccourcie), pattes au sol, regard qui balaie
    perche = {
        'racine': {'position': [0, -8, 0]},
        'aile_g': {'rotation': [88, -80, 0], 'scale': [1, 1, 0.62]},
        'aile_d': {'rotation': [88, 80, 0], 'scale': [1, 1, 0.62]},
        'avant_bras_g': {'rotation': [0, -6, 0]}, 'avant_bras_d': {'rotation': [0, 6, 0]},
        'main_g': {'rotation': [0, -6, 0]}, 'main_d': {'rotation': [0, 6, 0]},
        'corps': {'rotation': [S(0.4, 0, 1.2, -12), 0, 0]},
        'cou': {'rotation': [S(0.25, 0, 4, -6), 0, 0]},
        'tete': {'rotation': [S(0.2, 90, 6, 8), S(0.13, 0, 32), 0]},
        'queue1': {'rotation': [S(0.3, 0, 2, 22), 0, 0]},
        'queue2': {'rotation': [S(0.3, -60, 3, 14), 0, 0]},
        'queue3': {'rotation': [S(0.3, -120, 4, 8), 0, 0]},
        'patte_g': {'rotation': [12, 0, 0]}, 'patte_d': {'rotation': [12, 0, 0]},
        'tarse_g': {'rotation': [0, 0, 0]}, 'tarse_d': {'rotation': [0, 0, 0]},
    }
    perche.update(rubans(0.3, 4))
    A['perche'] = {'loop': True, 'bones': perche}

    A['oeuf'] = {'loop': True, 'bones': {
        'oeuf': {'scale': [S(1.4, 0, 0.035, 1), S(1.4, 0, 0.05, 1), S(1.4, 0, 0.035, 1)],
                 'rotation': [0, 0, S(5, 0, 1.5)]},
    }}

    ren = {}
    ren.update(battre(2.0, 20, 40, 0))
    ren.update(queue(2.0, 8))
    ren.update(rubans(2.0, 10))
    ren.update(pattes_rentrees)
    ren['assiette'] = {'scale': {'0.0': [0.15, 0.15, 0.15], '0.6': [1.25, 1.25, 1.25], '1.0': [0.95, 0.95, 0.95], '1.5': [1, 1, 1]},
                       'position': {'0.0': [0, -14, 0], '0.6': [0, 0, 0]}}
    ren['cou'] = {'rotation': [-30, 0, 0]}
    ren['tete'] = {'rotation': [-20, 0, 0]}
    ren['bec_inf'] = {'rotation': [30, 0, 0]}
    A['renaissance'] = {'loop': False, 'animation_length': 1.5, 'bones': ren}

    mort = {
        'aile_g': {'rotation': {'0.0': [0, 0, 0], '1.0': [0, -20, 55]}},
        'aile_d': {'rotation': {'0.0': [0, 0, 0], '1.0': [0, 20, -55]}},
        'avant_bras_g': {'rotation': {'0.0': [0, 0, 0], '1.0': [0, 0, 30]}},
        'avant_bras_d': {'rotation': {'0.0': [0, 0, 0], '1.0': [0, 0, -30]}},
        'cou': {'rotation': {'0.0': [0, 0, 0], '1.0': [40, 0, 0]}},
        'tete': {'rotation': {'0.0': [0, 0, 0], '1.0': [25, 0, 0]}},
        'queue1': {'rotation': {'0.0': [0, 0, 0], '1.0': [25, 0, 0]}},
        'assiette': {'scale': {'0.0': [1, 1, 1], '1.0': [1, 1, 1], '2.0': [0.55, 0.55, 0.55]},
                     'position': {'0.0': [0, 0, 0], '2.0': [0, -10, 0]}},
    }
    A['mort'] = {'loop': False, 'animation_length': 2.0, 'bones': mort}
    return A


def animation_json(A):
    out = {}
    for nom, a in A.items():
        d = {'loop': a['loop']}
        if 'animation_length' in a:
            d['animation_length'] = a['animation_length']
        d['bones'] = a['bones']
        out['animation.phenix.' + nom] = d
    return {'format_version': '1.8.0', 'animations': out}


# ============================================================================ sortie
def ecrire():
    m = modele()
    m.ranger(256)
    os.makedirs(os.path.join(ASSETS, 'geo'), exist_ok=True)
    os.makedirs(os.path.join(ASSETS, 'animations'), exist_ok=True)
    os.makedirs(os.path.join(ASSETS, 'textures', 'entity'), exist_ok=True)
    geo.ecrire_json(os.path.join(ASSETS, 'geo', 'phenix.geo.json'), m.geo_json())
    geo.ecrire_json(os.path.join(ASSETS, 'animations', 'phenix.animation.json'), animation_json(animations()))
    for f in range(IMAGES):
        geo.peindre(m, f).save(os.path.join(ASSETS, 'textures', 'entity', 'phenix_%d.png' % f))
    print('phénix : %d os, %d cubes, texture %dx%d, %d animations' % (len(m.tous()), len(m.cubes()), m.tw, m.th, len(animations())))
    return m


def apercu():
    from PIL import Image, ImageDraw
    m = modele()
    m.ranger(256)
    tex = geo.peindre(m, 0)
    A = animations()
    os.makedirs(APERCU, exist_ok=True)
    poses = [('repos', None, 0.0), ('vol', 'vol', 0.0), ('vol', 'vol', 0.43), ('plane', 'plane', 0.5),
             ('plongee', 'plongee', 0.2), ('souffle', 'souffle', 0.3), ('boule', 'boule', 0.5), ('pluie', 'pluie', 0.2),
             ('tempete', 'tempete', 0.3), ('perche', 'perche', 1.0), ('renaissance', 'renaissance', 0.3), ('mort', 'mort', 1.6)]
    vues = [(35, 20), (-90, 0), (0, 89), (150, 15)]
    cell = 300
    img = Image.new('RGBA', (cell * len(vues), cell * len(poses)), (26, 22, 30, 255))
    d = ImageDraw.Draw(img)
    for k, (nom, an, t) in enumerate(poses):
        anim = A.get(an) if an else None
        caches = () if an != 'oeuf' else ('assiette',)
        q = geo.poser(m, anim, t, caches=('oeuf',) if an != 'oeuf' else ('assiette',),
                      longueur=anim.get('animation_length') if anim else None)
        for j, (la, ta) in enumerate(vues):
            r = geo.rendu(m, tex, q, cell, la, ta, centre=np.array([0, 1.6, 0.7]), etendue=7.2)
            img.alpha_composite(r, (j * cell, k * cell))
        d.text((6, k * cell + 6), '%s t=%.2f' % (nom, t), fill=(255, 255, 255, 255))
    img.save(os.path.join(APERCU, 'phenix_poses.png'))
    # l'œuf et la texture
    q = geo.poser(m, A['oeuf'], 0.2, caches=('assiette',))
    planche = Image.new('RGBA', (cell * 2 + m.tw * 2, max(cell, m.th * 2)), (26, 22, 30, 255))
    planche.alpha_composite(geo.rendu(m, tex, q, cell, 30, 20), (0, 0))
    planche.alpha_composite(geo.rendu(m, geo.peindre(m, 2), q, cell, 30, 20), (cell, 0))
    t2 = tex.resize((m.tw * 2, m.th * 2), Image.NEAREST)
    planche.alpha_composite(t2, (cell * 2, 0))
    planche.save(os.path.join(APERCU, 'phenix_oeuf_texture.png'))
    print('aperçus dans', APERCU)


if __name__ == '__main__':
    if len(sys.argv) > 1 and sys.argv[1] == 'apercu':
        apercu()
    else:
        ecrire()
