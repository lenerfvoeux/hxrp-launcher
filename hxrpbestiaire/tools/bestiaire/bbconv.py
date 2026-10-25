"""
Conversion d'un modèle Blockbench (.bbmodel, format « free model ») en ressources GeckoLib 3.0.31 pour 1.12.2 :
.geo.json (format Bedrock 1.12.0, UV par face), .animation.json et une texture unique.

Ce que GeckoLib 3 pour 1.12 ne sait pas faire, et qu'on cuit ici à l'identique :
- plusieurs textures par modèle : elles sont empilées dans une seule (UV décalées) ;
- la rotation d'UV d'une face (90°, 270°) : le morceau de texture est recopié tourné dans une zone libre
  ajoutée sous la texture (180° = UV inversées, sans copie) ;
- l'interpolation « catmullrom » et « step » des images-clés : échantillonnées en images-clés linéaires
  (tous les 1/20 s), identiques à l'œil.

Conventions reprises de l'export Bedrock/GeckoLib de Blockbench : x inversé pour les pivots et origines, rotations
x et y inversées, UV des faces haut/bas inversées (uv + taille, taille négative). Les valeurs des animations sont
déjà dans la convention Bedrock et passent telles quelles.

    python3 -I bbconv.py modele.bbmodel identifiant dossier_assets [--echelle-texture N]
"""
import base64
import io
import json
import math
import os
import re
import sys

from PIL import Image

PAS = 0.05          # échantillonnage des courbes (s)


def lire(chemin):
    with open(chemin, encoding='utf-8') as f:
        return json.load(f)


# ============================================================================ textures
def images(d):
    """Les textures du modèle : (image RGBA, largeur UV, hauteur UV)."""
    res = d.get('resolution', {})
    out = []
    for t in d.get('textures', []):
        src = t.get('source', '')
        if not src.startswith('data:image/png;base64,'):
            raise SystemExit('texture %s : pas de PNG intégré au .bbmodel' % t.get('name'))
        im = Image.open(io.BytesIO(base64.b64decode(src.split(',', 1)[1]))).convert('RGBA')
        uw = t.get('uv_width') or res.get('width') or im.width
        uh = t.get('uv_height') or res.get('height') or im.height
        # texture animée (images empilées) : GeckoLib 1.12 ne les fait pas défiler, on garde la première
        image_h = im.width * uh / uw
        if im.height > image_h * 1.5 and abs(im.height / image_h - round(im.height / image_h)) < 1e-6:
            im = im.crop((0, 0, im.width, int(round(image_h))))
        out.append((im, float(uw), float(uh)))
    return out


class Atlas:
    """Textures empilées verticalement à une même densité (pixels par unité d'UV), plus une zone de morceaux tournés."""

    def __init__(self, texs):
        self.dens = max(im.width / uw for im, uw, uh in texs)
        self.largeur = max(uw for _, uw, _ in texs)
        self.decalage = []
        y = 0.0
        self.bandes = []
        for im, uw, uh in texs:
            self.decalage.append(y)
            w, h = int(round(uw * self.dens)), int(round(uh * self.dens))
            self.bandes.append(im.resize((w, h), Image.NEAREST) if im.size != (w, h) else im)
            y += uh
        self.hauteur_textures = y
        self.morceaux = []          # (image, x, y) en pixels, dans la zone sous les textures
        self.demandes = {}          # clé -> image tournée, en attendant le rangement
        self.places = {}            # clé -> UV dans l'atlas
        self.h_morceaux = 0

    def image_de(self, ti):
        return self.bandes[ti]

    def tourner(self, ti, uv, rot):
        """Le rectangle d'UV (dans la texture ti, orienté u1→u2, v1→v2) tourné de rot° (sens horaire) : au premier
        passage on le découpe, au second (après ranger()) on rend ses UV dans l'atlas."""
        cle = (ti, tuple(uv), rot)
        if cle in self.places:
            return self.places[cle]
        if cle not in self.demandes:
            u1, v1, u2, v2 = uv
            d = self.dens
            x0, x1 = sorted((u1 * d, u2 * d))
            y0, y1 = sorted((v1 * d, v2 * d))
            bx0, by0 = int(math.floor(x0 + 1e-6)), int(math.floor(y0 + 1e-6))
            bx1, by1 = max(bx0 + 1, int(math.ceil(x1 - 1e-6))), max(by0 + 1, int(math.ceil(y1 - 1e-6)))
            p = self.bandes[ti].crop((bx0, by0, bx1, by1))
            if u1 > u2:
                p = p.transpose(Image.FLIP_LEFT_RIGHT)
            if v1 > v2:
                p = p.transpose(Image.FLIP_TOP_BOTTOM)
            self.demandes[cle] = p.rotate(-rot, expand=True)       # PIL tourne dans le sens antihoraire
        return [0, 0, 1, 1]

    def ranger(self):
        """Rangement « ligne d'horizon » des morceaux, des plus hauts aux plus bas, 1 pixel de marge."""
        # un morceau plus large que les textures élargit l'atlas (les UV des textures ne bougent pas)
        plus_large = max([p.width for p in self.demandes.values()] or [0])
        self.largeur = max(self.largeur, math.ceil(plus_large / self.dens / 16.0) * 16)
        L = int(round(self.largeur * self.dens))
        horizon = [0] * L
        oy = self.hauteur_textures
        d = self.dens
        for cle, p in sorted(self.demandes.items(), key=lambda kv: (-kv[1].height, -kv[1].width)):
            w, h = p.width, p.height
            if w > L:
                raise SystemExit('morceau tourné plus large que la texture (%d > %d px)' % (w, L))
            meilleur = None
            for x in range(0, L - w + 1):
                y = max(horizon[x:x + w])
                if meilleur is None or y < meilleur[1]:
                    meilleur = (x, y)
            x, y = meilleur
            for i in range(x, min(L, x + w + 1)):
                horizon[i] = y + h + 1
            self.morceaux.append((p, x, y))
            self.places[cle] = [x / d, oy + y / d, (x + w) / d, oy + (y + h) / d]
        self.h_morceaux = max(horizon) if self.morceaux else 0

    def image(self):
        d = self.dens
        h_uv = self.hauteur_textures + self.h_morceaux / d
        # hauteur en UV entière et multiple de 16 (GeckoLib divise par la taille déclarée)
        h_uv = int(math.ceil(h_uv / 16.0) * 16)
        W, H = int(round(self.largeur * d)), int(round(h_uv * d))
        im = Image.new('RGBA', (W, H), (0, 0, 0, 0))
        for b, y in zip(self.bandes, self.decalage):
            im.paste(b, (0, int(round(y * d))))
        oy = int(round(self.hauteur_textures * d))
        for p, x, y in self.morceaux:
            im.paste(p, (x, oy + y))
        return im, self.largeur, h_uv


# ============================================================================ géométrie
FACES = ('north', 'east', 'south', 'west', 'up', 'down')


def cube_geo(e, atlas, stats):
    f, t = e['from'], e['to']
    taille = [t[i] - f[i] for i in range(3)]
    c = {'origin': [-t[0], f[1], f[2]], 'size': taille}
    if e.get('inflate'):
        c['inflate'] = e['inflate']
    rot = e.get('rotation', [0, 0, 0])
    if any(rot):
        c['pivot'] = [-e['origin'][0], e['origin'][1], e['origin'][2]]
        c['rotation'] = [-rot[0], -rot[1], rot[2]]
    uv = {}
    for nom in FACES:
        fa = e.get('faces', {}).get(nom)
        if not fa or fa.get('texture') is None or 'uv' not in fa:
            continue
        ti = int(fa['texture'])
        u1, v1, u2, v2 = [float(x) for x in fa['uv']]
        if u1 == u2 and v1 == v2:
            continue    # face sans surface de texture (tranche d'un plan) : Blockbench la laisse vide, on ne la dessine pas
        r = int(fa.get('rotation', 0) or 0) % 360
        if r in (90, 270):
            # copie tournée dans la zone des morceaux : UV déjà en coordonnées de l'atlas
            u1, v1, u2, v2 = atlas.tourner(ti, [u1, v1, u2, v2], r)
            stats['cuites'] += 1
        else:
            if r == 180:
                u1, v1, u2, v2 = u2, v2, u1, v1
                stats['rot180'] += 1
            oy = atlas.decalage[ti]
            v1, v2 = v1 + oy, v2 + oy
        if nom in ('up', 'down'):
            uv[nom] = {'uv': [u2, v2], 'uv_size': [u1 - u2, v1 - v2]}
        else:
            uv[nom] = {'uv': [u1, v1], 'uv_size': [u2 - u1, v2 - v1]}
    c['uv'] = uv
    return c


def convertir_geo(d, ident, atlas):
    elements = {e['uuid']: e for e in d.get('elements', [])}
    os_list = []
    noms = set()
    stats = {'cubes': 0, 'rot180': 0, 'cuites': 0, 'caches': 0}
    uuid_nom = {}

    def nom_unique(n):
        n = re.sub(r'[^A-Za-z0-9_]', '_', n or 'os')
        base, k = n, 2
        while n in noms:
            n = '%s_%d' % (base, k)
            k += 1
        noms.add(n)
        return n

    def visiter(g, parent):
        if g.get('visibility') is False or g.get('export') is False:
            stats['caches'] += 1
            return
        nom = nom_unique(g.get('name'))
        uuid_nom[g['uuid']] = nom
        o = g.get('origin', [0, 0, 0])
        b = {'name': nom, 'pivot': [-o[0], o[1], o[2]]}
        if parent:
            b['parent'] = parent
        r = g.get('rotation', [0, 0, 0])
        if any(r):
            b['rotation'] = [-r[0], -r[1], r[2]]
        cubes = []
        enfants = []
        for ch in g.get('children', []):
            if isinstance(ch, dict):
                enfants.append(ch)
            elif ch in elements:
                e = elements[ch]
                if e.get('type', 'cube') != 'cube':
                    continue
                if e.get('visibility') is False or e.get('export') is False:
                    stats['caches'] += 1
                    continue
                cubes.append(cube_geo(e, atlas, stats))
                stats['cubes'] += 1
        if cubes:
            b['cubes'] = cubes
        os_list.append(b)
        for ch in enfants:
            visiter(ch, nom)

    racine_cubes = []
    for ch in d.get('outliner', []):
        if isinstance(ch, dict):
            visiter(ch, None)
        elif ch in elements and elements[ch].get('type', 'cube') == 'cube':
            racine_cubes.append(cube_geo(elements[ch], atlas, stats))
            stats['cubes'] += 1
    if racine_cubes:
        os_list.insert(0, {'name': nom_unique('racine_cubes'), 'pivot': [0, 0, 0], 'cubes': racine_cubes})
    return os_list, uuid_nom, stats


# ============================================================================ animations
def valeur(x):
    if isinstance(x, (int, float)):
        return float(x)
    s = str(x).strip()
    if s == '':
        return 0.0
    try:
        return float(s)
    except ValueError:
        return s                          # molang : GeckoLib l'évalue


def vecteur(kf):
    p = kf['data_points'][0]
    return [valeur(p.get(a, 0)) for a in 'xyz']


def constant(v):
    return all(isinstance(x, float) for x in v)


def catmull(p0, p1, p2, p3, s):
    s2, s3 = s * s, s * s * s
    return [0.5 * ((2 * p1[i]) + (-p0[i] + p2[i]) * s + (2 * p0[i] - 5 * p1[i] + 4 * p2[i] - p3[i]) * s2
                   + (-p0[i] + 3 * p1[i] - 3 * p2[i] + p3[i]) * s3) for i in range(3)]


def canal(kfs):
    """Images-clés Blockbench d'un canal → dictionnaire GeckoLib {temps: [x, y, z]} (linéaire)."""
    kfs = sorted(kfs, key=lambda k: k['time'])
    pts = [(float(k['time']), vecteur(k), k.get('interpolation', 'linear')) for k in kfs]
    out = {}

    def mettre(t, v):
        out['%.4f' % t] = [round(x, 4) if isinstance(x, float) else x for x in v]

    for i, (t, v, interp) in enumerate(pts):
        mettre(t, v)
        if i + 1 >= len(pts):
            break
        t1, v1, interp1 = pts[i + 1]
        if t1 - t < 1e-6:
            continue
        if interp == 'step' and constant(v):
            # la valeur tient jusqu'à l'image suivante
            mettre(t1 - 0.0005, v)
        elif (interp == 'catmullrom' or interp1 == 'catmullrom') and constant(v) and constant(v1):
            v0 = pts[i - 1][1] if i > 0 and constant(pts[i - 1][1]) else v
            v2 = pts[i + 2][1] if i + 2 < len(pts) and constant(pts[i + 2][1]) else v1
            n = max(1, int(round((t1 - t) / PAS)))
            for k in range(1, n):
                s = k / n
                mettre(t + (t1 - t) * s, catmull(v0, v, v1, v2, s))
    return out


def convertir_animations(d, ident, uuid_nom):
    anims = {}
    for a in d.get('animations', []):
        nom = re.sub(r'[^a-z0-9_]', '_', a['name'].lower())
        # GeckoLib 3 pour 1.12 ne connaît que « boucle ou pas » (« hold » finit sur sa dernière image de toute façon)
        boucle = a.get('loop', 'once') == 'loop'
        os_a = {}
        for uuid, an in (a.get('animators') or {}).items():
            os_nom = uuid_nom.get(uuid)
            if not os_nom:
                continue
            canaux = {}
            for ch in ('rotation', 'position', 'scale'):
                kfs = [k for k in an.get('keyframes', []) if k.get('channel') == ch]
                if kfs:
                    canaux[ch] = canal(kfs)
            if canaux:
                os_a[os_nom] = canaux
        anims['animation.%s.%s' % (ident, nom)] = {'loop': boucle, 'animation_length': round(float(a.get('length', 0)), 4), 'bones': os_a}
    return {'format_version': '1.8.0', 'animations': anims}


# ============================================================================ tout
def entier(v):
    return int(v) if float(v).is_integer() else v


def convertir(chemin, ident, dossier):
    d = lire(chemin)
    if d.get('meta', {}).get('box_uv'):
        raise SystemExit('UV « boîte » non prévues par ce convertisseur')
    atlas = Atlas(images(d))
    convertir_geo(d, ident, atlas)                 # 1er passage : découpe des faces tournées
    atlas.ranger()
    os_list, uuid_nom, stats = convertir_geo(d, ident, atlas)
    im, tw, th = atlas.image()
    # bornes de rendu : largeur et hauteur de l'ensemble des cubes, en blocs, avec de la marge
    xs, ys = [], []
    for e in d.get('elements', []):
        xs += [abs(e['from'][0]), abs(e['to'][0]), abs(e['from'][2]), abs(e['to'][2])]
        ys += [e['from'][1], e['to'][1]]
    larg = max(xs or [16]) * 2 / 16 + 2
    haut = (max(ys or [16]) - min(ys or [0])) / 16 + 2
    geo = {'format_version': '1.12.0', 'minecraft:geometry': [{
        'description': {'identifier': 'geometry.%s' % ident, 'texture_width': entier(tw), 'texture_height': entier(th),
                        'visible_bounds_width': round(larg, 1), 'visible_bounds_height': round(haut, 1),
                        'visible_bounds_offset': [0, round(haut / 2, 1), 0]},
        'bones': os_list}]}
    anims = convertir_animations(d, ident, uuid_nom)
    for sous in ('geo', 'animations', 'textures/entity'):
        os.makedirs(os.path.join(dossier, sous), exist_ok=True)
    with open(os.path.join(dossier, 'geo', ident + '.geo.json'), 'w', encoding='utf-8') as f:
        json.dump(geo, f, separators=(',', ':'))
    with open(os.path.join(dossier, 'animations', ident + '.animation.json'), 'w', encoding='utf-8') as f:
        json.dump(anims, f, separators=(',', ':'))
    im.save(os.path.join(dossier, 'textures', 'entity', ident + '.png'))
    print('%s : %d os, %d cubes, %d faces tournées cuites, %d à 180°, %d cachés ; texture %dx%d px (%sx%s UV) ; %d animations'
          % (ident, len(os_list), stats['cubes'], stats['cuites'], stats['rot180'], stats['caches'], im.width, im.height, tw, th,
             len(anims['animations'])))
    return geo, anims, im


if __name__ == '__main__':
    convertir(sys.argv[1], sys.argv[2], sys.argv[3])
