"""
Génère tout ce qu'il faut pour récolter les ingrédients dans le monde :
textures (cultures, feuillages, pousses, sachets de graines, blocs), modèles, états de blocs,
noms (lang), recettes d'artisanat et data/recolte.json lu par le mod.

    python3 tools/monde/gen_monde.py
"""
import glob
import json
import os
import shutil
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import especes  # noqa: E402
import plantes  # noqa: E402
import arbres  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
A = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'hxrpmetiers')
M = 'hxrpmetiers'
FOOD = json.load(open(os.path.join(A, 'data', 'food.json'), encoding='utf-8'))
ENTRIES = {e['id']: e for g in ('ingredients', 'epices', 'preparations', 'plats', 'boissons') for e in FOOD[g]}
LANG_DEBUT, LANG_FIN = '# --- monde : généré par tools/monde/gen_monde.py ---', '# --- fin monde ---'


def pal(i):
    return ENTRIES[i].get('pal') or ['#c83a2a', '#f0a080', '#f4e8d8']


def ecrire(chemin, obj):
    os.makedirs(os.path.dirname(chemin), exist_ok=True)
    with open(chemin, 'w', encoding='utf-8') as f:
        json.dump(obj, f, ensure_ascii=False, indent=1)
        f.write('\n')


def png(img, *chemin):
    p = os.path.join(A, 'textures', *chemin)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)


def nettoyer():
    for d in ('textures/blocks/monde', 'textures/items/monde', 'models/block/monde', 'models/item/monde'):
        shutil.rmtree(os.path.join(A, d), ignore_errors=True)
    for p in glob.glob(os.path.join(A, 'blockstates', '*.json')):
        if json.load(open(p)).get('_monde'):
            os.remove(p)
    for p in glob.glob(os.path.join(A, 'models', 'item', '*.json')):
        if json.load(open(p)).get('_monde'):
            os.remove(p)
    for p in glob.glob(os.path.join(A, 'recipes', 'monde_*.json')):
        os.remove(p)


def modele_item(nom, texture):
    ecrire(os.path.join(A, 'models', 'item', nom + '.json'),
           {'_monde': True, 'parent': 'item/generated', 'textures': {'layer0': M + ':' + texture}})


def modele_item_bloc(nom, modele):
    ecrire(os.path.join(A, 'models', 'item', nom + '.json'), {'_monde': True, 'parent': M + ':' + modele})


def etats(nom, variantes):
    ecrire(os.path.join(A, 'blockstates', nom + '.json'), {'_monde': True, 'variants': variantes})


def main():
    nettoyer()
    lang = ['itemGroup.%s.recolte=Gourmet · Récolte' % M]
    recolte = {'cultures': [], 'arbres': [], 'animaux': [], 'vanilla': especes.VANILLA, 'blocs': [], 'objets': list(especes.OBJETS)}
    items_dir = os.path.join(A, 'textures', 'items')

    # ------------------------------------------------------------------ cultures
    for id, (forme, climat, semence, mn, mx, feu) in especes.CULTURES.items():
        bloc, graine = 'culture_' + id, 'graines_' + id
        for s in range(3):
            png(plantes.culture(id, s, pal(id)), 'blocks', 'monde', 'cultures', '%s_%d.png' % (id, s))
            ecrire(os.path.join(A, 'models', 'block', 'monde', 'cultures', '%s_%d.json' % (id, s)),
                   {'parent': 'block/crop', 'textures': {'crop': '%s:blocks/monde/cultures/%s_%d' % (M, id, s)}})
        etats(bloc, {'age=%d' % s: {'model': '%s:monde/cultures/%s_%d' % (M, id, s)} for s in range(3)})
        icone = Image.open(os.path.join(items_dir, id + '.png')).convert('RGBA')
        png(arbres.sachet(icone, feu), 'items', 'monde', 'graines', id + '.png')
        modele_item(graine, 'items/monde/graines/' + id)
        lang.append('tile.%s.%s.name=%s' % (M, bloc, 'Plant de ' + ENTRIES[id]['name'].lower()))
        lang.append('item.%s.%s.name=%s' % (M, graine, semence))
        recolte['cultures'].append({'id': id, 'forme': forme, 'climat': climat, 'min': mn, 'max': mx})

    # ------------------------------------------------------------------ arbres fruitiers
    for a, (fruit, nom, climat, forme, bois, feu, fl, taille) in especes.ARBRES.items():
        f_bloc, p_bloc = 'feuilles_' + a, 'pousse_' + a
        fpal = pal(fruit) if fruit in ENTRIES else None
        for e in range(3):
            png(arbres.feuillage(a, e, fpal), 'blocks', 'monde', 'arbres', '%s_%d.png' % (a, e))
            ecrire(os.path.join(A, 'models', 'block', 'monde', 'arbres', '%s_%d.json' % (a, e)),
                   {'parent': 'block/leaves', 'textures': {'all': '%s:blocks/monde/arbres/%s_%d' % (M, a, e)}})
        etats(f_bloc, {'fruit=%d' % e: {'model': '%s:monde/arbres/%s_%d' % (M, a, e)} for e in range(3)})
        modele_item_bloc(f_bloc, 'monde/arbres/%s_0' % a)
        png(arbres.pousse_arbre(a), 'blocks', 'monde', 'arbres', 'pousse_' + a + '.png')
        ecrire(os.path.join(A, 'models', 'block', 'monde', 'arbres', 'pousse_' + a + '.json'),
               {'parent': 'block/cross', 'textures': {'cross': '%s:blocks/monde/arbres/pousse_%s' % (M, a)}})
        etats(p_bloc, {'stage=%d' % s: {'model': '%s:monde/arbres/pousse_%s' % (M, a)} for s in range(2)})
        modele_item(p_bloc, 'blocks/monde/arbres/pousse_' + a)
        lang.append('tile.%s.%s.name=Feuilles de %s' % (M, f_bloc, nom.lower()))
        lang.append('tile.%s.%s.name=Pousse de %s' % (M, p_bloc, nom.lower()))
        recolte['arbres'].append({'id': a, 'fruit': fruit, 'climat': climat, 'forme': forme, 'bois': bois})

    # ------------------------------------------------------------------ blocs du monde
    png(arbres.minerai_de_sel(), 'blocks', 'monde', 'minerai_de_sel.png')
    ecrire(os.path.join(A, 'models', 'block', 'monde', 'minerai_de_sel.json'),
           {'parent': 'block/cube_all', 'textures': {'all': M + ':blocks/monde/minerai_de_sel'}})
    etats('minerai_de_sel', {'normal': {'model': M + ':monde/minerai_de_sel'}})
    modele_item_bloc('minerai_de_sel', 'monde/minerai_de_sel')
    for pleine in (0, 1):
        for face in ('avant', 'cote', 'dessus'):
            png(arbres.ruche(face, bool(pleine)), 'blocks', 'monde', 'ruche_%s_%d.png' % (face, pleine))
        ecrire(os.path.join(A, 'models', 'block', 'monde', 'ruche_%d.json' % pleine),
               {'parent': 'block/orientable', 'textures': {
                   'front': '%s:blocks/monde/ruche_avant_%d' % (M, pleine), 'side': '%s:blocks/monde/ruche_cote_%d' % (M, pleine),
                   'top': '%s:blocks/monde/ruche_dessus_%d' % (M, pleine)}})
    rot = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
    etats('ruche_sauvage', {'facing=%s,pleine=%s' % (f, str(bool(p)).lower()): dict({'model': '%s:monde/ruche_%d' % (M, p)}, **({'y': r} if r else {}))
                            for f, r in rot.items() for p in (0, 1)})
    modele_item_bloc('ruche_sauvage', 'monde/ruche_1')
    for bloc, kind in (('banc_de_moules', 'moule'), ('banc_d_huitres', 'huitre')):
        png(arbres.coquillages(kind), 'blocks', 'monde', bloc + '.png')
        t = '%s:blocks/monde/%s' % (M, bloc)
        ecrire(os.path.join(A, 'models', 'block', 'monde', bloc + '.json'), {
            'parent': 'block/thin_block', 'textures': {'particle': t, 'texture': t},
            'elements': [{'from': [0, 0, 0], 'to': [16, 2, 16], 'faces': {
                'down': {'uv': [0, 0, 16, 16], 'texture': '#texture', 'cullface': 'down'},
                'up': {'uv': [0, 0, 16, 16], 'texture': '#texture'},
                'north': {'uv': [0, 14, 16, 16], 'texture': '#texture'}, 'south': {'uv': [0, 14, 16, 16], 'texture': '#texture'},
                'west': {'uv': [0, 14, 16, 16], 'texture': '#texture'}, 'east': {'uv': [0, 14, 16, 16], 'texture': '#texture'}}}]})
        etats(bloc, {'normal': {'model': '%s:monde/%s' % (M, bloc)}})
        modele_item_bloc(bloc, 'monde/' + bloc)
    for bloc, (nom, produit) in especes.BLOCS.items():
        lang.append('tile.%s.%s.name=%s' % (M, bloc, nom))
        recolte['blocs'].append({'id': bloc, 'produit': produit})

    # ------------------------------------------------------------------ objets
    olives = arbres.Canvas()
    for (x, y) in [(10, 12), (18, 10), (14, 19), (22, 18), (9, 23), (19, 25)]:
        m = arbres.ellipse(x, y, 3.2, 4)
        olives.shape(m, arbres.R('#4a5a2a', light=1.3), arbres.L_sphere(x - 1, y - 1, 3, 4), hl=1)
    arbres.leaf(olives, 14, 8, 22, 2, 3.4, '#6a8a6a')
    olives.outline()
    png(olives.image(), 'items', 'monde', 'olives.png')
    modele_item('olives', 'items/monde/olives')
    for o, nom in especes.OBJETS.items():
        lang.append('item.%s.%s.name=%s' % (M, o, nom))

    # ------------------------------------------------------------------ animaux (noms, œufs d'apparition)
    for a, (nom, comp, milieux, donne) in especes.ANIMAUX.items():
        lang.append('entity.%s.%s.name=%s' % (M, a, nom))
        recolte['animaux'].append({'id': a, 'comportement': comp, 'milieux': milieux, 'donne': donne})

    # ------------------------------------------------------------------ recettes
    def ing(x):
        if x == 'minecraft:water_bottle':
            return {'type': 'minecraft:item_nbt', 'item': 'minecraft:potion', 'nbt': {'Potion': 'minecraft:water'}}
        return {'item': x if ':' in x else M + ':' + x}
    for k, (produit, n, ings) in enumerate(especes.RECETTES):
        ecrire(os.path.join(A, 'recipes', 'monde_%s.json' % produit),
               {'type': 'minecraft:crafting_shapeless', 'ingredients': [ing(x) for x in ings],
                'result': {'item': M + ':' + produit, 'count': n}})

    # ------------------------------------------------------------------ lang
    for fichier in ('fr_fr.lang', 'en_us.lang'):
        p = os.path.join(A, 'lang', fichier)
        txt = open(p, encoding='utf-8').read()
        if LANG_DEBUT in txt:
            txt = txt[:txt.index(LANG_DEBUT)].rstrip('\n') + '\n'
        txt += LANG_DEBUT + '\n' + '\n'.join(lang) + '\n' + LANG_FIN + '\n'
        open(p, 'w', encoding='utf-8').write(txt)

    ecrire(os.path.join(A, 'data', 'recolte.json'), recolte)
    print('%d cultures, %d arbres, %d animaux, %d blocs, %d recettes, %d noms' % (
        len(recolte['cultures']), len(recolte['arbres']), len(recolte['animaux']), len(recolte['blocs']), len(especes.RECETTES), len(lang)))


if __name__ == '__main__':
    main()
