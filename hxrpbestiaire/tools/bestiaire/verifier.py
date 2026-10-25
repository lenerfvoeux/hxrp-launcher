"""
Vérifie, avant de compiler, que les ressources du bestiaire sont cohérentes entre elles et avec le code Java :
- chaque espèce (Especes.java) a son modèle, sa texture et ses animations de base ; chaque attaque des boss
  (new Coup("id", "animation")) existe dans le classeur (onglet Attaques) et son animation dans le modèle ; les
  attaques des sbires aussi ; durees.properties couvre toutes les animations ;
- modèles GeckoLib : os uniques, parents connus, UV dans la texture ; animations : os existants ;
- objets (butins, œufs, projectiles) et appâts : modèles, textures, états de bloc, traductions ;
- sons : Sons.java ↔ sounds.json ↔ .ogg ↔ sous-titres ; classeur : phases et butins des boss du mod.
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
RES = os.path.join(RACINE, 'src', 'main', 'resources', 'assets', 'hxrpbestiaire')
JAVA = os.path.join(RACINE, 'src', 'main', 'java', 'fr', 'lenerfvoeux', 'hxrp', 'bestiaire')
erreurs = []


def err(m):
    erreurs.append(m)


def lire(p):
    with open(p, encoding='utf-8') as f:
        return f.read()


# ---------------------------------------------------------------- espèces
especes = {}
src = lire(os.path.join(JAVA, 'Especes.java'))
for bloc in re.split(r'public static final Espece ', src)[1:]:
    m = re.match(r'(\w+) = (boss|sbire)\("(\w+)", "([^"]+)", "(\w+)", (\w+)\.class, (\d+)\)', bloc)
    if not m:
        continue
    const, genre, id_, nom, modele, classe, num = m.groups()
    e = {'const': const, 'boss': genre == 'boss', 'id': id_, 'nom': nom, 'modele': modele, 'classe': classe, 'num': int(num),
         'repos': 'idle', 'marche': 'walk', 'mort': 'death', 'apparition': '', 'appat': ''}
    a = re.search(r'\.anims\("(\w+)", "(\w+)", "(\w+)"\)', bloc)
    if a:
        e['repos'], e['marche'], e['mort'] = a.groups()
    a = re.search(r'\.apparition\("(\w+)"\)', bloc)
    if a:
        e['apparition'] = a.group(1)
    a = re.search(r'\.appat\("(\w+)", "([^"]+)"', bloc)
    if a:
        e['appat'] = a.group(1)
    e['cachees'] = re.findall(r'\.cacher\("(\w+)"', bloc)
    especes[const] = e
if not especes:
    err('aucune espèce lue dans Especes.java')
nums = [e['num'] for e in especes.values()]
if len(set(nums)) != len(nums):
    err('numéros d\'entité en double : %s' % nums)

# ---------------------------------------------------------------- modèles et animations
animations = {}
os_modele = {}
for e in especes.values():
    md = e['modele']
    if md in animations:
        continue
    pg = os.path.join(RES, 'geo', md + '.geo.json')
    pa = os.path.join(RES, 'animations', md + '.animation.json')
    pt = os.path.join(RES, 'textures', 'entity', md + '.png')
    if not (os.path.exists(pg) and os.path.exists(pa) and os.path.exists(pt)):
        err('modèle %s incomplet (geo, animations, texture)' % md)
        animations[md] = {}
        continue
    g = json.load(open(pg))['minecraft:geometry'][0]
    tw, th = g['description']['texture_width'], g['description']['texture_height']
    im = Image.open(pt)
    if abs(im.width / tw - im.height / th) > 1e-6:
        err('%s : texture %dx%d pas à l\'échelle de %sx%s' % (md, im.width, im.height, tw, th))
    noms = set()
    for b in g['bones']:
        if b['name'] in noms:
            err('%s : os en double %s' % (md, b['name']))
        noms.add(b['name'])
    os_modele[md] = noms
    for b in g['bones']:
        if 'parent' in b and b['parent'] not in noms:
            err('%s : parent inconnu %s' % (md, b['parent']))
        for c in b.get('cubes', []):
            for f, uv in c.get('uv', {}).items():
                u0, v0 = uv['uv']
                u1, v1 = u0 + uv['uv_size'][0], v0 + uv['uv_size'][1]
                if min(u0, u1) < -1e-6 or max(u0, u1) > tw + 1e-6 or min(v0, v1) < -1e-6 or max(v0, v1) > th + 1e-6:
                    err('%s, os %s : UV hors texture %s' % (md, b['name'], uv))
    an = json.load(open(pa))['animations']
    courtes = {}
    for k, a in an.items():
        if not k.startswith('animation.%s.' % md):
            err('%s : animation mal nommée %s' % (md, k))
        for b in a.get('bones', {}):
            if b not in noms:
                err('%s, %s : os inconnu %s' % (md, k, b))
        if not isinstance(a.get('loop'), bool):
            err('%s, %s : « loop » doit être un booléen (GeckoLib 1.12)' % (md, k))
        courtes[k.split('.', 2)[2]] = a
    animations[md] = courtes
durees = {}
for l in lire(os.path.join(RES, 'durees.properties')).splitlines():
    if '=' in l and not l.startswith('#'):
        k, v = l.split('=', 1)
        durees[k] = int(v)
for md, courtes in animations.items():
    for n, a in courtes.items():
        k = '%s.%s' % (md, n)
        if durees.get(k) != max(1, int(round(float(a['animation_length']) * 20))):
            err('durees.properties : %s absent ou faux' % k)


def exige(md, anim, ou):
    if anim and anim not in animations.get(md, {}):
        err('%s : animation « %s » absente du modèle %s' % (ou, anim, md))


for e in especes.values():
    for k in ('repos', 'marche', 'mort', 'apparition'):
        exige(e['modele'], e[k], e['id'])
    for o in e['cachees']:
        if o not in os_modele.get(e['modele'], set()):
            err('%s : os caché « %s » absent du modèle %s' % (e['id'], o, e['modele']))

# ---------------------------------------------------------------- classeur
NS = '{http://schemas.openxmlformats.org/spreadsheetml/2006/main}'
RNS = '{http://schemas.openxmlformats.org/officeDocument/2006/relationships}'


def feuilles(chemin):
    z = zipfile.ZipFile(chemin)
    partages = [''.join(t.text or '' for t in si.iter(NS + 't')) for si in ET.fromstring(z.read('xl/sharedStrings.xml')).iter(NS + 'si')]
    rels = {r.get('Id'): r.get('Target') for r in ET.fromstring(z.read('xl/_rels/workbook.xml.rels'))}
    out = {}
    for sh in ET.fromstring(z.read('xl/workbook.xml')).iter(NS + 'sheet'):
        doc = ET.fromstring(z.read('xl/' + rels[sh.get(RNS + 'id')].lstrip('/')))
        lignes, entetes = [], None
        for row in doc.iter(NS + 'row'):
            val = {}
            for c in row.iter(NS + 'c'):
                v = c.find(NS + 'v')
                if v is None:
                    continue
                col = re.match(r'[A-Z]+', c.get('r')).group(0)
                val[col] = partages[int(v.text)] if c.get('t') == 's' else v.text
            if not val:
                continue
            if entetes is None:
                entetes = val
            else:
                lignes.append({entetes.get(k, '').strip().lower(): x for k, x in val.items()})
        out[sh.get('name')] = lignes
    return out


xl = feuilles(os.path.join(RES, 'bestiaire_stats.xlsx'))
attaques = {}
for l in xl['Attaques']:
    attaques.setdefault(l.get('id boss'), set()).add(l.get('id attaque'))
butins_xl = {}
for l in xl['Butins']:
    butins_xl.setdefault(l.get('id boss'), set()).add(l.get('id objet'))
sbires_xl = {}
for l in xl['Sbires']:
    sbires_xl.setdefault(l.get('id boss'), set()).add(l.get('id sbire'))
for e in especes.values():
    if e['boss'] and not any(l.get('id') == e['id'] for l in xl['Boss']):
        err('classeur : pas de ligne Boss pour %s' % e['id'])

# ---------------------------------------------------------------- attaques des boss et des sbires
par_classe = {e['classe']: e for e in especes.values()}
for p in glob.glob(os.path.join(JAVA, 'entite', '**', '*.java'), recursive=True):
    s = lire(p)
    m = re.search(r'super\(w, Especes\.(\w+)', s)
    if not m or m.group(1) not in especes:
        continue
    e = especes[m.group(1)]
    for id_, an in re.findall(r'new Coup\("(\w+)", "(\w+)"', s):
        if id_ not in attaques.get(e['id'], set()):
            err('%s : attaque « %s » absente du classeur' % (e['id'], id_))
        exige(e['modele'], an, '%s/%s' % (e['id'], id_))
    for liste in re.findall(r'ATTAQUES = \{([^}]*)\}', s):
        for an in re.findall(r'"(\w+)"', liste):
            exige(e['modele'], an, e['id'])
    for id_s in re.findall(r'invoquer\("(\w+)"', s):
        if id_s not in sbires_xl.get(e['id'], set()):
            err('%s : sbire « %s » absent du classeur' % (e['id'], id_s))
    for an in re.findall(r'anim\("(\w+)"\)', s):
        exige(e['modele'], an, e['id'])
    # animations de renaissance et durées lues par le serveur
    for an in re.findall(r'animRenaissance\(\) \{\s*return "(\w+)"', s) + re.findall(r'duree\("(\w+)"', s):
        exige(e['modele'], an, e['id'])

# ---------------------------------------------------------------- objets, appâts, traductions
lang = {}
for l in lire(os.path.join(RES, 'lang', 'fr_fr.lang')).splitlines():
    if '=' in l and not l.startswith('#'):
        k, v = l.split('=', 1)
        lang[k] = v
if lire(os.path.join(RES, 'lang', 'fr_fr.lang')) != lire(os.path.join(RES, 'lang', 'en_us.lang')):
    err('en_us.lang doit reprendre fr_fr.lang (le serveur traduit avec en_us)')
reg = lire(os.path.join(JAVA, 'Registre.java'))
butins = re.findall(r'\{"(\w+)", "[^"]+", "(\w+)", "[^"]+"\}', reg)
objets = [b for b, _ in butins] + ['oeuf_' + e['id'] for e in especes.values() if e['boss']]
proj = re.search(r'enum Type \{(.*?)\}', lire(os.path.join(JAVA, 'entite', 'EntityProjectileBoss.java')), re.S).group(1)
objets += ['projectile_' + t.lower() for t in re.findall(r'^\s*([A-Z_]+)\(', proj, re.M)]
for o in objets:
    mj = os.path.join(RES, 'models', 'item', o + '.json')
    if not os.path.exists(mj):
        err('modèle d\'objet manquant : ' + o)
        continue
    for t in json.load(open(mj)).get('textures', {}).values():
        dom, chem = t.split(':', 1)
        if dom == 'hxrpbestiaire' and not os.path.exists(os.path.join(RES, 'textures', chem + '.png')):
            err('texture manquante pour %s : %s' % (o, t))
    if 'item.hxrpbestiaire.%s.name' % o not in lang:
        err('nom manquant : item.hxrpbestiaire.%s.name' % o)
for b, boss in butins:
    if b not in butins_xl.get(boss, set()):
        err('butin %s absent du classeur pour %s' % (b, boss))
for e in especes.values():
    if e['boss']:
        for b in butins_xl.get(e['id'], set()):
            if b not in [x for x, _ in butins]:
                err('le classeur donne le butin %s à %s, mais il n\'est pas enregistré' % (b, e['id']))
    if 'entity.hxrpbestiaire.%s.name' % e['id'] not in lang:
        err('nom manquant pour l\'entité ' + e['id'])
    if not e['boss']:
        continue
    a = e['appat']
    if not a:
        err('%s : pas d\'appât' % e['id'])
        continue
    if 'tile.hxrpbestiaire.%s.name' % a not in lang:
        err('nom manquant pour l\'appât ' + a)
    pe = os.path.join(RES, 'blockstates', a + '.json')
    if not os.path.exists(pe):
        err('état de bloc manquant : ' + a)
        continue
    for v in json.load(open(pe))['variants'].values():
        mb = os.path.join(RES, 'models', 'block', v['model'].split(':')[1] + '.json')
        if not os.path.exists(mb):
            err('modèle de bloc manquant : ' + mb)
            continue
        m = json.load(open(mb))
        for t in m['textures'].values():
            if not os.path.exists(os.path.join(RES, 'textures', t.split(':')[1] + '.png')):
                err('texture de bloc manquante : ' + t)
        for i, el in enumerate(m['elements']):
            for fa, fd in el['faces'].items():
                if fd['texture'][1:] not in m['textures']:
                    err('%s, élément %d : texture %s non déclarée' % (a, i, fd['texture']))
            for k in ('from', 'to'):
                if any(x < 0 or x > 16 for x in el[k]):
                    err('%s, élément %d : %s hors du bloc (UV par défaut hors texture)' % (a, i, k))
if 'itemGroup.hxrpbestiaire' not in lang:
    err('nom de l\'onglet manquant')

# ---------------------------------------------------------------- sons
sons_java = re.findall(r'son\(r, "(\w+)"\)', lire(os.path.join(JAVA, 'Sons.java')))
table = json.load(open(os.path.join(RES, 'sounds.json')))
for s in sons_java:
    if s not in table:
        err('son absent de sounds.json : ' + s)
    if not os.path.exists(os.path.join(RES, 'sounds', s + '.ogg')):
        err('fichier son manquant : ' + s)
    if 'subtitles.hxrpbestiaire.' + s not in lang:
        err('sous-titre manquant : ' + s)
for s in table:
    if s not in sons_java:
        err('son de sounds.json non enregistré : ' + s)

if erreurs:
    print('\n'.join('!! ' + e for e in erreurs))
    sys.exit(1)
print('ressources du bestiaire OK : %d espèces, %d modèles, %d animations, %d objets, %d sons, classeur lu'
      % (len(especes), len(animations), sum(len(v) for v in animations.values()), len(objets), len(sons_java)))
