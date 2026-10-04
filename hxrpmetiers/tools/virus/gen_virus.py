"""
Génère tout le Hunter Virus à partir des données (tools/virus/*.py) :
  - data/virus/*.json (ingrédients, préparations, maladies, blessures), vérifiés contre le cahier des charges ;
  - icônes 32x32 des objets (textures/items/virus/), modèles d'objets ;
  - blocs 3D de l'officine (textures/blocks/virus/, models/block/virus/, blockstates) ;
  - textures d'interface (textures/gui/virus/), sons (sounds/virus/, sounds.json) ;
  - noms français (lang), et des planches d'aperçu dans tools/preview/out/.

    python3 tools/virus/gen_virus.py
"""
import glob
import json
import os
import shutil
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, 'art'))
sys.path.insert(0, os.path.join(HERE, '..', 'textures'))
sys.path.insert(0, os.path.join(HERE, '..', 'models'))
import donnees  # noqa: E402
import outils  # noqa: E402
import machines  # noqa: E402
import interfaces  # noqa: E402
import ingredients_icones  # noqa: E402
import preparations_icones  # noqa: E402
import flore  # noqa: E402
import sons  # noqa: E402
import zfight  # noqa: E402
import mc_render  # noqa: E402
from pix import Canvas  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
A = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'hxrpmetiers')
PREVIEW = os.path.join(ROOT, 'tools', 'preview', 'out')
M = 'hxrpmetiers'
LANG_DEBUT, LANG_FIN = '# --- virus : généré par tools/virus/gen_virus.py ---', '# --- fin virus ---'
MARQUE = '_virus'


def ecrire(chemin, obj):
    os.makedirs(os.path.dirname(chemin), exist_ok=True)
    with open(chemin, 'w', encoding='utf-8') as f:
        json.dump(obj, f, ensure_ascii=False, indent=1)
        f.write('\n')


def nettoyer():
    for d in ('textures/items/virus', 'textures/blocks/virus', 'models/block/virus', 'textures/gui/virus'):
        shutil.rmtree(os.path.join(A, d), ignore_errors=True)
    for d in ('blockstates', 'models/item'):
        for p in glob.glob(os.path.join(A, d, '*.json')):
            try:
                if json.load(open(p, encoding='utf-8')).get(MARQUE):
                    os.remove(p)
            except ValueError:
                pass


def icone(fn):
    c = Canvas()
    fn(c)
    c.outline()
    return c.image()


def modele_objet(id, texture):
    ecrire(os.path.join(A, 'models', 'item', id + '.json'), {MARQUE: True, 'parent': 'item/generated', 'textures': {'layer0': M + ':' + texture}})


def modele_objet_bloc(id, modele, display=None):
    o = {MARQUE: True, 'parent': M + ':block/' + modele}
    if display:
        o['display'] = display
    ecrire(os.path.join(A, 'models', 'item', id + '.json'), o)


def etats(id, variantes):
    ecrire(os.path.join(A, 'blockstates', id + '.json'), {MARQUE: True, 'variants': variantes})


def orientations(modele):
    return {'facing=north': {'model': M + ':' + modele}, 'facing=east': {'model': M + ':' + modele, 'y': 90},
            'facing=south': {'model': M + ':' + modele, 'y': 180}, 'facing=west': {'model': M + ':' + modele, 'y': 270}}


def planche(images, chemin, echelle=3, cols=10):
    cell = 32 * echelle + 8
    rows = (len(images) + cols - 1) // cols
    im = Image.new('RGBA', (cols * cell, rows * (cell + 12)), (58, 62, 70, 255))
    d = ImageDraw.Draw(im)
    for k, (nom, ic) in enumerate(images):
        x, y = (k % cols) * cell, (k // cols) * (cell + 12)
        im.alpha_composite(ic.resize((32 * echelle, 32 * echelle), Image.NEAREST), (x + 4, y + 4))
        d.text((x + 3, y + cell - 2), nom[:16], fill=(255, 255, 255, 255))
    os.makedirs(os.path.dirname(chemin), exist_ok=True)
    im.save(chemin)


def monde(ingredients, lang):
    """Plantes, champignons de tronc, sols, troncs écorcés, nid, lotus, source de sève : textures, modèles, états, noms."""
    tdir = os.path.join(A, 'textures', 'blocks', 'virus', 'monde')
    mdir = os.path.join(A, 'models', 'block', 'virus', 'monde')
    os.makedirs(tdir, exist_ok=True)
    T = M + ':blocks/virus/monde/'
    noms = {i['id']: i['nom'] for i in ingredients}
    apercu = []
    for ing in ingredients:
        id, r = ing['id'], ing['recolte']
        if r['type'] == 'plante':
            variantes = {}
            for s in range(3):
                im = flore.plante(id, s)
                im.save(os.path.join(tdir, 'plante_%s_%d.png' % (id, s)))
                if id in flore.TAPIS:
                    modele = {MARQUE: True, 'parent': 'block/thin_block', 'ambientocclusion': False,
                              'textures': {'particle': T + 'plante_%s_%d' % (id, s), 'tapis': T + 'plante_%s_%d' % (id, s)},
                              'elements': [{'from': [0, 0, 0], 'to': [16, 0.5, 16],
                                            'faces': {'up': {'uv': [0, 0, 16, 16], 'texture': '#tapis'},
                                                      'down': {'uv': [0, 0, 16, 16], 'texture': '#tapis', 'cullface': 'down'}}}]}
                else:
                    modele = {MARQUE: True, 'parent': 'block/cross', 'textures': {'cross': T + 'plante_%s_%d' % (id, s)}}
                ecrire(os.path.join(mdir, 'plante_%s_%d.json' % (id, s)), modele)
                variantes['age=%d' % s] = {'model': M + ':virus/monde/plante_%s_%d' % (id, s)}
                if s == 2:
                    apercu.append(('plante_' + id, im))
            etats('plante_' + id, variantes)
            lang.append('tile.%s.plante_%s.name=%s (plante)' % (M, id, noms[id]))
        elif r['type'] == 'tronc':
            flore.texture_tronc(id).image().save(os.path.join(tdir, 'tronc_%s.png' % id))
            variantes = {}
            for age in range(3):
                mo = flore.modele_tronc(id, age)
                ecrire(os.path.join(mdir, 'tronc_%s_%d.json' % (id, age)), mo.json())
                for f, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
                    v = {'model': M + ':virus/monde/tronc_%s_%d' % (id, age)}
                    if y:
                        v['y'] = y
                    variantes['age=%d,facing=%s' % (age, f)] = v
            etats('tronc_' + id, variantes)
            lang.append('tile.%s.tronc_%s.name=%s (sur le tronc)' % (M, id, noms[id]))
        elif r['type'] == 'sol':
            bloc = r['bloc']
            flore.sol(bloc).image().save(os.path.join(tdir, bloc + '.png'))
            ecrire(os.path.join(mdir, bloc + '.json'), {MARQUE: True, 'parent': 'block/cube_all', 'textures': {'all': T + bloc}})
            etats(bloc, {'normal': {'model': M + ':virus/monde/' + bloc}})
            modele_objet_bloc(bloc, 'virus/monde/' + bloc)
            lang.append('tile.%s.%s.name=%s' % (M, bloc, NOMS_SOLS[bloc]))
            apercu.append((bloc, flore.sol(bloc).image()))
    # troncs écorcés
    for bouleau, nom in ((True, 'bouleau'), (False, 'saule')):
        id = 'tronc_ecorce_' + nom
        flore.tronc_ecorce(bouleau).image().save(os.path.join(tdir, id + '.png'))
        flore.tronc_ecorce(bouleau, bout=True).image().save(os.path.join(tdir, id + '_bout.png'))
        ecrire(os.path.join(mdir, id + '.json'), {MARQUE: True, 'parent': 'block/cube_column', 'textures': {'side': T + id, 'end': T + id + '_bout'}})
        mm = M + ':virus/monde/' + id
        etats(id, {'axis=y': {'model': mm}, 'axis=z': {'model': mm, 'x': 90}, 'axis=x': {'model': mm, 'x': 90, 'y': 90}, 'axis=none': {'model': mm}})
        lang.append('tile.%s.%s.name=Tronc de %s écorcé' % (M, id, nom))
    # nid d'aigle-araignée
    flore.texture_brindilles().image().save(os.path.join(tdir, 'brindilles.png'))
    flore.texture_oeuf().image().save(os.path.join(tdir, 'oeuf_aigle.png'))
    variantes = {}
    for n in range(3):
        ecrire(os.path.join(mdir, 'nid_%d.json' % n), flore.modele_nid(n).json())
        variantes['oeufs=%d' % n] = {'model': M + ':virus/monde/nid_%d' % n}
    etats('nid_d_aigle_araignee', variantes)
    modele_objet_bloc('nid_d_aigle_araignee', 'virus/monde/nid_2')
    lang.append("tile.%s.nid_d_aigle_araignee.name=Nid d'aigle-araignée" % M)
    # lotus de l'aube
    flore.texture_lotus_feuille().image().save(os.path.join(tdir, 'lotus_feuille.png'))
    flore.texture_lotus_fleur(True).image().save(os.path.join(tdir, 'lotus_fleur.png'))
    flore.texture_lotus_fleur(False).image().save(os.path.join(tdir, 'lotus_bouton.png'))
    for fleuri in (True, False):
        mo = flore.modele_lotus(fleuri)
        ecrire(os.path.join(mdir, mo.name + '.json'), mo.json())
    etats('lotus_de_l_aube_plante', {'fleuri=true': {'model': M + ':virus/monde/lotus_fleuri'},
                                     'fleuri=false': {'model': M + ':virus/monde/lotus_bouton'}})
    ic = icone(flore.icone_lotus_plante)
    ic.save(os.path.join(A, 'textures', 'items', 'virus', 'lotus_de_l_aube_plante.png'))
    modele_objet('lotus_de_l_aube_plante', 'items/virus/lotus_de_l_aube_plante')
    lang.append("tile.%s.lotus_de_l_aube_plante.name=Lotus de l'aube (plante)" % M)
    # source de sève de l'Arbre-Monde
    flore.seve('cote').image().save(os.path.join(tdir, 'seve_cote.png'))
    flore.seve('dessus').image().save(os.path.join(tdir, 'seve_dessus.png'))
    ecrire(os.path.join(mdir, 'source_de_seve.json'), {MARQUE: True, 'parent': 'block/cube_column', 'textures': {'side': T + 'seve_cote', 'end': T + 'seve_dessus'}})
    etats('source_de_seve', {'normal': {'model': M + ':virus/monde/source_de_seve'}})
    modele_objet_bloc('source_de_seve', 'virus/monde/source_de_seve')
    lang.append("tile.%s.source_de_seve.name=Source de sève de l'Arbre-Monde" % M)
    # planche d'aperçu des plantes mûres et des sols
    planche(apercu, os.path.join(PREVIEW, 'virus_monde.png'), echelle=2, cols=12)


NOMS_SOLS = {'pierre_moussue_humide': 'Pierre moussue humide', 'terre_truffiere': 'Terre truffière',
             'vase_a_algue_noire': 'Vase à algue noire', 'roche_a_fer_sang': 'Roche à fer-sang', 'roche_des_abysses': 'Roche des abysses'}


def main():
    preps, maladies, blessures = donnees.main()
    nettoyer()
    lang = ['itemGroup.%s.virus=Hunter Virus · Officine' % M]

    # ------------------------------------------------------------------ objets de l'apothicaire
    noms_outils = {'seringue_vide': 'Seringue stérile', 'seringue_pleine': 'Prise de sang', 'ordonnance': 'Ordonnance',
                   'carnet_de_consultation': 'Carnet de consultation', 'parchemin_du_second_souffle': 'Parchemin du Second Souffle',
                   'preparation_ratee': 'Préparation ratée', 'preparation_en_cours_officine': 'Préparation en cours'}
    apercus = []
    for id, fn in outils.REG.items():
        ic = icone(fn)
        p = os.path.join(A, 'textures', 'items', 'virus', id + '.png')
        os.makedirs(os.path.dirname(p), exist_ok=True)
        ic.save(p)
        modele_objet(id, 'items/virus/' + id)
        lang.append('item.%s.%s.name=%s' % (M, id, noms_outils[id]))
        apercus.append((id, ic))
    planche(apercus, os.path.join(PREVIEW, 'virus_outils.png'))

    # ------------------------------------------------------------------ ingrédients et préparations
    lang.append('itemGroup.%s.virus_ingredients=Hunter Virus · Ingrédients' % M)
    lang.append('itemGroup.%s.virus_remedes=Hunter Virus · Remèdes' % M)
    ingredients = json.load(open(os.path.join(A, 'data', 'virus', 'ingredients.json'), encoding='utf-8'))['ingredients']
    toutes_preps = json.load(open(os.path.join(A, 'data', 'virus', 'preparations.json'), encoding='utf-8'))['preparations']
    icones_ing = {}
    apercus_ing = []
    for ing in ingredients:
        ic = icone(ingredients_icones.REG[ing['id']])
        icones_ing[ing['id']] = ic
        ic.save(os.path.join(A, 'textures', 'items', 'virus', ing['id'] + '.png'))
        modele_objet(ing['id'], 'items/virus/' + ing['id'])
        lang.append('item.%s.%s.name=%s' % (M, ing['id'], ing['nom']))
        apercus_ing.append((ing['id'], ic))
    planche(apercus_ing, os.path.join(PREVIEW, 'virus_ingredients.png'), echelle=2, cols=12)
    couleurs = preparations_icones.Couleurs(toutes_preps, icones_ing, os.path.join(A, 'textures', 'items'))
    vus = set()
    apercus_preps = []
    for pr in toutes_preps:
        pid = pr.get('produit') or pr['id']
        if pid in vus or pid in icones_ing or ':' in pid:
            continue
        vus.add(pid)
        ic = icone(lambda c, pr=pr: preparations_icones.dessiner(c, pr, couleurs))
        ic.save(os.path.join(A, 'textures', 'items', 'virus', pid + '.png'))
        modele_objet(pid, 'items/virus/' + pid)
        lang.append('item.%s.%s.name=%s' % (M, pid, pr['nom']))
        apercus_preps.append((pid, ic))
    planche(apercus_preps, os.path.join(PREVIEW, 'virus_preparations.png'), echelle=2, cols=12)

    # ------------------------------------------------------------------ le Virus dans le monde
    monde(ingredients, lang)

    # ------------------------------------------------------------------ blocs 3D
    tdir = os.path.join(A, 'textures', 'blocks', 'virus')
    machines.save_textures(tdir)
    mdir = os.path.join(A, 'models', 'block', 'virus')
    noms_blocs = {'meuble_a_tiroirs': 'Meuble à tiroirs', 'yagen': 'Yagen', 'hachoir_a_levier': 'Hachoir à levier',
                  'chaudron_sur_brasero': 'Chaudron sur brasero', 'alambic_de_cuivre': 'Alambic de cuivre',
                  'jarres_de_maceration': 'Jarres de macération', 'balance_d_apothicaire': "Balance d'apothicaire",
                  'mortier_d_apothicaire': 'Mortier et pilon', 'pilulier': 'Pilulier en bois',
                  'table_de_preparation': 'Table de préparation laquée', 'microscope_d_analyse': "Microscope d'analyse",
                  'grimoire_des_maladies': 'Grimoire des maladies', 'presentoir_de_l_apothicaire': "Présentoir de l'apothicaire"}
    for b in machines.BUILDERS:
        b()
    utilisees = set()
    for nom, m in machines.MODELS.items():
        j = m.json()
        c = zfight.conflits(j)
        if c:
            raise SystemExit('Faces superposées (scintillement) dans %s : %s' % (nom, c[:5]))
        ecrire(os.path.join(mdir, nom + '.json'), j)
        for v in m.textures.values():
            utilisees.add(v.split('/')[-1])
        if nom.startswith('jarres_de_maceration_'):
            continue
        disp = None
        if m.gui_scale != 0.625 or m.gui_dy:
            disp = {'gui': {'rotation': [30, 225, 0], 'translation': [0, m.gui_dy, 0], 'scale': [m.gui_scale] * 3}}
        modele_objet_bloc(nom, 'virus/' + nom, disp)
        etats(nom, orientations('virus/' + nom))
        lang.append('tile.%s.%s.name=%s' % (M, nom, noms_blocs[nom]))
    # jarres : le nombre de jarres scellées se voit (0 à 3)
    variantes = {}
    for n in range(4):
        for f, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
            v = {'model': M + ':virus/jarres_de_maceration_%d' % n}
            if y:
                v['y'] = y
            variantes['facing=%s,scellees=%d' % (f, n)] = v
    etats('jarres_de_maceration', variantes)
    modele_objet_bloc('jarres_de_maceration', 'virus/jarres_de_maceration_1')
    lang.append('tile.%s.jarres_de_maceration.name=%s' % (M, noms_blocs['jarres_de_maceration']))
    manquantes = [t for t in utilisees if not os.path.exists(os.path.join(tdir, t + '.png'))]
    if manquantes:
        raise SystemExit('Textures manquantes : ' + ', '.join(manquantes))
    for p in glob.glob(os.path.join(tdir, '*.png')):
        if os.path.basename(p)[:-4] not in utilisees:
            os.remove(p)
    rendus = []
    for nom in machines.MODELS:
        els, imgs = mc_render.load_model(mdir, nom, os.path.join(A, 'textures'))
        rendus.append((nom, mc_render.render(els, imgs, size=256, scale=256 / 30), mc_render.render(els, imgs, size=256, scale=256 / 30, yaw=225)))
    if rendus:
        im = Image.new('RGBA', (512, 268 * len(rendus)), (74, 84, 96, 255))
        d = ImageDraw.Draw(im)
        for k, (nom, a, b) in enumerate(rendus):
            im.alpha_composite(a, (0, k * 268))
            im.alpha_composite(b, (256, k * 268))
            d.text((6, k * 268 + 254), nom, fill=(255, 255, 255, 255))
        os.makedirs(PREVIEW, exist_ok=True)
        im.save(os.path.join(PREVIEW, 'virus_blocs.png'))

    # ------------------------------------------------------------------ interfaces et sons
    interfaces.generer(os.path.join(A, 'textures', 'gui', 'virus'))
    sons.generer(os.path.join(A, 'sounds', 'virus'), os.path.join(A, 'sounds.json'))
    lang.append('container.%s.meuble=Meuble à tiroirs' % M)
    for evt, nom in (('toux', 'Quelqu\'un tousse'), ('eternuement', 'Quelqu\'un éternue'), ('sifflement', 'Sifflement dans les oreilles'),
                     ('murmure', 'Murmures'), ('battement', 'Battements de cœur'), ('pilon', 'Pilon'), ('bouillon', 'Bouillonnement'),
                     ('verre', 'Tintement de verre'), ('seringue', 'Seringue'), ('lame', 'Lame du hachoir'), ('page', 'Page tournée'),
                     ('meule', 'Meule du yagen')):
        lang.append('subtitles.%s.virus.%s=%s' % (M, evt, nom))

    # ------------------------------------------------------------------ noms
    for fichier in ('fr_fr.lang', 'en_us.lang'):
        p = os.path.join(A, 'lang', fichier)
        txt = open(p, encoding='utf-8').read()
        bloc = LANG_DEBUT + '\n' + '\n'.join(lang) + '\n' + LANG_FIN + '\n'
        if LANG_DEBUT in txt and LANG_FIN in txt[txt.index(LANG_DEBUT):]:
            i = txt.index(LANG_DEBUT)
            j = txt.index(LANG_FIN, i) + len(LANG_FIN)
            txt = txt[:i] + bloc.rstrip('\n') + txt[j:]
        else:
            txt = txt.rstrip('\n') + '\n' + bloc
        open(p, 'w', encoding='utf-8').write(txt)
    print('%d objets, %d blocs 3D, %d noms' % (len(outils.REG), len(machines.MODELS), len(lang)))


if __name__ == '__main__':
    main()
