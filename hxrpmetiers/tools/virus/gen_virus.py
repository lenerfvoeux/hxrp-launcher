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
    for d in ('textures/items/virus', 'textures/blocks/virus', 'models/block/virus', 'textures/gui/virus', 'sounds/virus'):
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

    # ------------------------------------------------------------------ blocs 3D
    tdir = os.path.join(A, 'textures', 'blocks', 'virus')
    machines.save_textures(tdir)
    mdir = os.path.join(A, 'models', 'block', 'virus')
    noms_blocs = {'meuble_a_tiroirs': 'Meuble à tiroirs'}
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
        disp = None
        if m.gui_scale != 0.625 or m.gui_dy:
            disp = {'gui': {'rotation': [30, 225, 0], 'translation': [0, m.gui_dy, 0], 'scale': [m.gui_scale] * 3}}
        modele_objet_bloc(nom, 'virus/' + nom, disp)
        etats(nom, orientations('virus/' + nom))
        lang.append('tile.%s.%s.name=%s' % (M, nom, noms_blocs[nom]))
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
