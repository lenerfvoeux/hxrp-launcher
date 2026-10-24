"""
Vérifie que les ressources du Phénix sont cohérentes entre elles et avec le code Java, avant de compiler :
modèle GeckoLib (os, parents, UV dans la texture), animations (os existants, noms appelés par le Java),
textures et modèles d'objets et de blocs, sons (Sons.java ↔ sounds.json ↔ .ogg), traductions, et le classeur des
boss rangé dans le jar (lignes « phenix », attaques lues par le Java).
"""
import glob
import json
import os
import re
import sys
import zipfile
import xml.etree.ElementTree as ET

from PIL import Image

ICI = os.path.dirname(os.path.abspath(__file__))
RACINE = os.path.normpath(os.path.join(ICI, '..', '..'))
RES = os.path.join(RACINE, 'src', 'main', 'resources', 'assets', 'hxrpphenix')
JAVA = os.path.join(RACINE, 'src', 'main', 'java')

erreurs = []


def err(msg):
    erreurs.append(msg)


def lire(chemin):
    with open(chemin, encoding='utf-8') as f:
        return f.read()


code = '\n'.join(lire(p) for p in glob.glob(os.path.join(JAVA, '**', '*.java'), recursive=True))

# ---------------------------------------------------------------- modèle
geo = json.load(open(os.path.join(RES, 'geo', 'phenix.geo.json')))
g = geo['minecraft:geometry'][0]
tw, th = g['description']['texture_width'], g['description']['texture_height']
os_noms = set()
for b in g['bones']:
    if b['name'] in os_noms:
        err('os en double : ' + b['name'])
    os_noms.add(b['name'])
for b in g['bones']:
    if 'parent' in b and b['parent'] not in os_noms:
        err('parent inconnu %s pour %s' % (b['parent'], b['name']))
    for c in b.get('cubes', []):
        w, h, d = c['size']
        if any(int(v) != v or v < 0 for v in c['size']):
            err('taille non entière dans %s : %s' % (b['name'], c['size']))
        u, v = c['uv']
        if u < 0 or v < 0 or u + 2 * (w + d) > tw or v + h + d > th:
            err('UV hors texture dans %s : %s' % (b['name'], c))
for nom in ('racine', 'assiette', 'oeuf'):
    if nom not in os_noms:
        err('os attendu par ModelePhenix absent : ' + nom)

# ---------------------------------------------------------------- textures du modèle
for i in range(4):
    p = os.path.join(RES, 'textures', 'entity', 'phenix_%d.png' % i)
    if not os.path.exists(p):
        err('texture manquante : ' + p)
    elif Image.open(p).size != (tw, th):
        err('taille de %s différente du modèle (%dx%d)' % (p, tw, th))

# ---------------------------------------------------------------- animations
anim = json.load(open(os.path.join(RES, 'animations', 'phenix.animation.json')))['animations']
for nom, a in anim.items():
    for b in a.get('bones', {}):
        if b not in os_noms:
            err('animation %s : os inconnu %s' % (nom, b))
for nom in sorted(set(re.findall(r'"(animation\.phenix\.[a-z_]+)"', code))):
    if nom not in anim:
        err('animation appelée par le Java mais absente : ' + nom)

# ---------------------------------------------------------------- ressources citées par le Java
VANILLA = {'textures/gui/bars.png'}      # barre de boss orange : texture de Minecraft
for chemin in sorted(set(re.findall(r'"((?:geo|animations|textures)/[a-z0-9_/]+\.(?:json|png))"', code))):
    if chemin in VANILLA:
        continue
    if not os.path.exists(os.path.join(RES, chemin)):
        err('ressource citée par le Java absente : ' + chemin)
for chemin in ('textures/entity/boule_de_feu.png', 'textures/entity/plume_ardente.png'):
    im = Image.open(os.path.join(RES, chemin))
    if im.size != (32, 128):
        err('%s : 4 images de 32×32 empilées attendues, trouvé %s' % (chemin, im.size))

# ---------------------------------------------------------------- objets
objets = re.findall(r'setRegistryName\(HxrpPhenix\.MODID, "([a-z_]+)"\)', code)
lang = {}
for ligne in lire(os.path.join(RES, 'lang', 'fr_fr.lang')).splitlines():
    if '=' in ligne and not ligne.startswith('#'):
        k, v = ligne.split('=', 1)
        lang[k] = v
blocs = [o for o in objets if os.path.exists(os.path.join(RES, 'blockstates', o + '.json'))]


def elements_dans_la_texture(nom, modele):
    """UV explicites ou par défaut (déduites de la position) dans 0-16 : sinon Minecraft lit le voisin dans l'atlas."""
    for i, e in enumerate(modele.get('elements', [])):
        for a in ('from', 'to'):
            if any(v < -16 or v > 32 for v in e[a]):
                err('%s, élément %d : %s hors limites' % (nom, i, a))
        r = e.get('rotation')
        if r and (r['angle'] not in (-45, -22.5, 0, 22.5, 45) or r['axis'] not in 'xyz'):
            err('%s, élément %d : rotation interdite %s' % (nom, i, r))
        for fa, fd in e.get('faces', {}).items():
            uv = fd.get('uv')
            if uv is None:
                f, t = e['from'], e['to']
                if fa in ('up', 'down'):
                    uv = [f[0], f[2], t[0], t[2]]
                elif fa in ('north', 'south'):
                    uv = [f[0], 16 - t[1], t[0], 16 - f[1]]
                else:
                    uv = [f[2], 16 - t[1], t[2], 16 - f[1]]
            if any(v < 0 or v > 16 for v in uv):
                err('%s, élément %d, face %s : UV %s hors de la texture' % (nom, i, fa, uv))
            if not fd.get('texture', '').startswith('#') or fd['texture'][1:] not in modele.get('textures', {}):
                err('%s, élément %d, face %s : texture %s non déclarée' % (nom, i, fa, fd.get('texture')))


for b in blocs:
    etat = json.load(open(os.path.join(RES, 'blockstates', b + '.json')))
    for v in etat.get('variants', {}).values():
        nom = v['model'].split(':', 1)[1]
        mb = os.path.join(RES, 'models', 'block', nom + '.json')
        if not os.path.exists(mb):
            err('modèle de bloc manquant : ' + nom)
            continue
        m = json.load(open(mb))
        elements_dans_la_texture(nom, m)
        for t in m.get('textures', {}).values():
            chem = os.path.join(RES, 'textures', t.split(':', 1)[1] + '.png')
            if not os.path.exists(chem):
                err('texture du bloc %s manquante : %s' % (b, t))
                continue
            w, h = Image.open(chem).size
            anime = os.path.exists(chem + '.mcmeta')
            if w != 16 or (h != 16 and not (anime and h % 16 == 0)):
                err('texture %s : 16×16 attendu (ou 16×16n animée), trouvé %d×%d' % (t, w, h))
    mi = os.path.join(RES, 'models', 'item', b + '.json')
    if os.path.exists(mi) and json.load(open(mi)).get('parent') != 'hxrpphenix:block/' + b:
        err('le modèle d\'objet %s doit reprendre le modèle du bloc' % b)
    if 'tile.hxrpphenix.%s.name' % b not in lang:
        err('nom manquant pour le bloc ' + b)
if 'GameRegistry.registerTileEntity' in code and 'TileNidDeBraises' not in code:
    err('entité de bloc du nid non enregistrée')

for o in objets:
    mj = os.path.join(RES, 'models', 'item', o + '.json')
    if not os.path.exists(mj):
        err('modèle d\'objet manquant : ' + o)
    else:
        for t in json.load(open(mj)).get('textures', {}).values():
            dom, chem = t.split(':', 1)
            if not os.path.exists(os.path.join(RES, 'textures', chem + '.png')):
                err('texture de l\'objet %s manquante : %s' % (o, t))
            elif Image.open(os.path.join(RES, 'textures', chem + '.png')).size != (32, 32):
                err('icône %s : 32×32 attendu' % o)
    if o not in blocs and 'item.hxrpphenix.%s.name' % o not in lang:
        err('nom manquant pour l\'objet ' + o)
for e in re.findall(r'new ResourceLocation\(HxrpPhenix\.MODID, "([a-z_]+)"\), \d', code):
    if 'entity.hxrpphenix.%s.name' % e not in lang:
        err('nom manquant pour l\'entité ' + e)
for cle in ('itemGroup.hxrpphenix', 'death.attack.phenix', 'death.attack.phenix_seul', 'death.attack.phenix_renvoi'):
    if cle not in lang:
        err('traduction manquante : ' + cle)
if lire(os.path.join(RES, 'lang', 'fr_fr.lang')) != lire(os.path.join(RES, 'lang', 'en_us.lang')):
    err('en_us.lang doit reprendre fr_fr.lang (le serveur traduit la barre de boss avec en_us)')

# ---------------------------------------------------------------- sons
sons_java = re.findall(r'son\(r, "([a-z_]+)"\)', code)
table = json.load(open(os.path.join(RES, 'sounds.json')))
for s in sons_java:
    if s not in table:
        err('son absent de sounds.json : ' + s)
    elif not os.path.exists(os.path.join(RES, 'sounds', s + '.ogg')):
        err('fichier son manquant : ' + s)
    if 'subtitles.hxrpphenix.' + s not in lang:
        err('sous-titre manquant : ' + s)
for s in table:
    if s not in sons_java:
        err('son de sounds.json non enregistré par Sons.java : ' + s)

# ---------------------------------------------------------------- classeur des boss (copie par défaut du jar)
NS = '{http://schemas.openxmlformats.org/spreadsheetml/2006/main}'
RNS = '{http://schemas.openxmlformats.org/officeDocument/2006/relationships}'


def colonne(ref):
    n = 0
    for ch in ref:
        if not ch.isalpha():
            break
        n = n * 26 + ord(ch) - 64
    return n - 1


def feuilles(chemin):
    z = zipfile.ZipFile(chemin)
    partages = []
    if 'xl/sharedStrings.xml' in z.namelist():
        for si in ET.fromstring(z.read('xl/sharedStrings.xml')).iter(NS + 'si'):
            partages.append(''.join(t.text or '' for t in si.iter(NS + 't')))
    rels = {r.get('Id'): r.get('Target') for r in ET.fromstring(z.read('xl/_rels/workbook.xml.rels'))}
    out = {}
    for sh in ET.fromstring(z.read('xl/workbook.xml')).iter(NS + 'sheet'):
        cible = rels[sh.get(RNS + 'id')]
        doc = ET.fromstring(z.read(cible.lstrip('/') if cible.startswith('/') else 'xl/' + cible))
        lignes, entetes = [], None
        for row in doc.iter(NS + 'row'):
            val = {}
            for c in row.iter(NS + 'c'):
                v = c.find(NS + 'v')
                if c.get('t') == 'inlineStr':
                    x = ''.join(t.text or '' for t in c.iter(NS + 't'))
                elif v is None:
                    continue
                elif c.get('t') == 's':
                    x = partages[int(v.text)]
                else:
                    x = v.text
                val[colonne(c.get('r'))] = x
            if not val:
                continue
            if entetes is None:
                entetes = val
            else:
                lignes.append({entetes.get(k, '').strip().lower(): x for k, x in val.items()})
        out[sh.get('name')] = lignes
    return out


classeur = os.path.join(RES, 'bestiaire_stats.xlsx')
if not os.path.exists(classeur):
    err('classeur des boss absent du jar : ' + classeur)
else:
    f = feuilles(classeur)
    for nom in ('Boss', 'Attaques', 'Appâts', 'Butins'):
        if nom not in f:
            err('classeur : onglet « %s » absent' % nom)
    phases = {int(float(l['phase'])) for l in f.get('Boss', []) if l.get('id') == 'phenix'}
    if not {1, 2, 3} <= phases:
        err('classeur : il faut les phases 1, 2 et 3 du phénix, trouvé %s' % sorted(phases))
    attaques = {l.get('id attaque') for l in f.get('Attaques', []) if l.get('id boss') == 'phenix'}
    for a in sorted(set(re.findall(r'(?:attaque|degats|feu|portee|porteeBase|pret)\("([a-z]+)"', code))):
        if a not in attaques:
            err('attaque « %s » lue par le Java mais absente du classeur (phenix)' % a)
    if not any(l.get('id boss') == 'phenix' for l in f.get('Appâts', [])):
        err('classeur : pas d\'appât pour le phénix')
    if not any(l.get('id boss') == 'phenix' and l.get('id objet') == 'plume_de_phenix' for l in f.get('Butins', [])):
        err('classeur : pas de plume de phénix dans les butins')

if erreurs:
    print('\n'.join('!! ' + e for e in erreurs))
    sys.exit(1)
print('ressources du Phénix OK : %d os, %d animations, %d objets dont %d bloc(s), %d sons, classeur des boss lu'
      % (len(os_noms), len(anim), len(objets), len(blocs), len(sons_java)))
