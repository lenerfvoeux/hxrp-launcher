"""
Vérifie que les ressources du Phénix sont cohérentes entre elles et avec le code Java, avant de compiler :
modèle GeckoLib (os, parents, UV dans la texture), animations (os existants, noms appelés par le Java),
textures et modèles d'objets, sons (Sons.java ↔ sounds.json ↔ .ogg), traductions.
"""
import glob
import json
import os
import re
import sys

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
for chemin in sorted(set(re.findall(r'"((?:geo|animations|textures)/[a-z0-9_/]+\.(?:json|png))"', code))):
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
    if 'item.hxrpphenix.%s.name' % o not in lang:
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

if erreurs:
    print('\n'.join('!! ' + e for e in erreurs))
    sys.exit(1)
print('ressources du Phénix OK : %d os, %d animations, %d objets, %d sons' % (len(os_noms), len(anim), len(objets), len(sons_java)))
