"""
Vérifie que chaque objet et bloc enregistré par le Hunter Virus (VirusRegistre) a son modèle, son état de bloc
(avec une variante pour chaque état que le bloc peut prendre : plein, vide, épuisé, fouillé…), ses textures et son
nom français ; de même pour les blocs de récolte du Gourmet. Vérifie aussi qu'aucun identifiant retiré en v0.7 ne
traîne dans le code ou les ressources. Lancé par la CI avant la compilation.

    python3 tools/virus/verifier_ressources.py
"""
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, '..', '..', 'src', 'main')
A = os.path.join(SRC, 'resources', 'assets', 'hxrpmetiers')
# Retirés en v0.7 (seul Remappage.java peut encore les nommer, pour remapper ou ignorer les anciens objets)
RETIRES = re.compile(r"fer_sang|fer-sang|abysses|aigle_araignee|aigle-araignée|feuille_de_kukuroo|Feuille de Kukuroo|baie_de_l_ile|"
                     r"Baie de l'île|fumigation|Fumigation|quinze_raretes|quinze raretés|nid_d_aigle")
FACINGS = ['north', 'east', 'south', 'west']


def produit(*axes):
    """Toutes les clés de variante « a=x,b=y » (propriétés triées par nom, comme Minecraft)."""
    cles = ['']
    for nom, valeurs in sorted(axes):
        cles = [(c + ',' if c else '') + '%s=%s' % (nom, v) for c in cles for v in valeurs]
    return set(cles)
M = 'hxrpmetiers'
MACHINES = ['yagen', 'hachoir_a_levier', 'chaudron_sur_brasero', 'alambic_de_cuivre', 'jarres_de_maceration', 'balance_d_apothicaire',
            'mortier_d_apothicaire', 'pilulier', 'table_de_preparation']


def charger(nom):
    return json.load(open(os.path.join(A, 'data', 'virus', nom), encoding='utf-8'))


def main():
    ing = charger('ingredients.json')['ingredients']
    preps = charger('preparations.json')['preparations']
    ids_ing = [i['id'] for i in ing]
    objets = ['seringue_vide', 'seringue_pleine', 'ordonnance', 'carnet_de_consultation', 'parchemin_du_second_souffle',
              'preparation_en_cours_officine', 'preparation_ratee', 'lotus_de_l_aube_plante'] + ids_ing
    for p in preps:
        pid = p.get('produit') or p['id']
        if pid not in objets and ':' not in pid:
            objets.append(pid)
    blocs_objet = ['meuble_a_tiroirs'] + MACHINES + ['microscope_d_analyse', 'grimoire_des_maladies', 'presentoir_de_l_apothicaire',
                                                       'source_de_seve']
    blocs_objet += sorted({i['recolte']['bloc'] for i in ing if i['recolte']['type'] == 'sol'})
    # plantes et champignons de tronc : un objet pour que les admins les posent (onglet « Blocs du monde »)
    blocs_objet += ['plante_' + i['id'] for i in ing if i['recolte']['type'] == 'plante']
    blocs_objet += ['tronc_' + i['id'] for i in ing if i['recolte']['type'] == 'tronc']
    blocs_monde = ['tronc_ecorce_bouleau', 'tronc_ecorce_saule', 'lotus_de_l_aube_plante']
    # variantes attendues de chaque bloc de récolte (Virus et Gourmet)
    variantes = {'source_de_seve': produit(('tarie', ['false', 'true'])),
                 'lotus_de_l_aube_plante': produit(('fleuri', ['false', 'true'])),
                 'minerai_de_sel': produit(('epuise', ['false', 'true'])),
                 'banc_de_moules': produit(('vide', ['false', 'true'])),
                 'banc_d_huitres': produit(('vide', ['false', 'true'])),
                 'ruche_sauvage': produit(('facing', FACINGS), ('pleine', ['false', 'true']))}
    for i in ing:
        r = i['recolte']
        if r['type'] == 'plante':
            variantes['plante_' + i['id']] = produit(('age', ['0', '1', '2']))
        elif r['type'] == 'tronc':
            variantes['tronc_' + i['id']] = produit(('age', ['0', '1', '2']), ('facing', FACINGS))
        elif r['type'] == 'sol':
            variantes[r['bloc']] = produit(('vide', ['false', 'true']))
    recolte = json.load(open(os.path.join(A, 'data', 'recolte.json'), encoding='utf-8'))
    for c in recolte['cultures']:
        variantes['culture_' + c['id']] = produit(('age', ['0', '1', '2']))
    for a in recolte['arbres']:
        variantes['feuilles_' + a['id']] = produit(('fruit', ['0', '1', '2']))
    gourmet = ['minerai_de_sel', 'banc_de_moules', 'banc_d_huitres', 'ruche_sauvage'] + ['culture_' + c['id'] for c in recolte['cultures']] \
        + ['feuilles_' + a['id'] for a in recolte['arbres']]
    lang = open(os.path.join(A, 'lang', 'fr_fr.lang'), encoding='utf-8').read()
    erreurs = []

    def texture_ok(ref):
        if ref.startswith('#') or not ref.startswith(M + ':'):
            return True
        return os.path.exists(os.path.join(A, 'textures', ref.split(':', 1)[1] + '.png'))

    def modele_bloc_ok(ref, chemin):
        if ':' not in ref:
            return True
        p = os.path.join(A, 'models', 'block', ref.split(':', 1)[1] + '.json')
        if not os.path.exists(p):
            erreurs.append('%s : modèle de bloc absent %s' % (chemin, ref))
            return False
        m = json.load(open(p, encoding='utf-8'))
        for t in m.get('textures', {}).values():
            if not texture_ok(t):
                erreurs.append('%s : texture absente %s' % (ref, t))
        return True

    for o in objets + blocs_objet:
        p = os.path.join(A, 'models', 'item', o + '.json')
        if not os.path.exists(p):
            erreurs.append('modèle d\'objet absent : ' + o)
            continue
        m = json.load(open(p, encoding='utf-8'))
        par = m.get('parent', '')
        if par.startswith(M + ':block/'):
            modele_bloc_ok(M + ':' + par.split(':block/', 1)[1], o)
        for t in m.get('textures', {}).values():
            if not texture_ok(t):
                erreurs.append('%s : texture absente %s' % (o, t))
    for o in objets:
        if o == 'lotus_de_l_aube_plante':
            continue
        if not re.search(r'^item\.%s\.%s\.name=.+$' % (M, re.escape(o)), lang, re.M):
            erreurs.append('nom absent : item.%s.%s.name' % (M, o))
    for b in blocs_objet + blocs_monde + gourmet:
        p = os.path.join(A, 'blockstates', b + '.json')
        if not os.path.exists(p):
            erreurs.append('état de bloc absent : ' + b)
            continue
        vs = json.load(open(p, encoding='utf-8')).get('variants', {})
        for k, v in vs.items():
            for var in (v if isinstance(v, list) else [v]):
                modele_bloc_ok(M + ':' + var['model'].split(':', 1)[1], b + '#' + k)
        if b in variantes and set(vs) != variantes[b]:
            erreurs.append('%s : variantes %s au lieu de %s' % (b, sorted(set(vs) ^ variantes[b])[:6], 'celles attendues'))
        if not re.search(r'^tile\.%s\.%s\.name=.+$' % (M, re.escape(b)), lang, re.M):
            erreurs.append('nom absent : tile.%s.%s.name' % (M, b))
    # identifiants retirés en v0.7
    for racine, _, fichiers in os.walk(SRC):
        for f in fichiers:
            if f == 'Remappage.java' or not f.endswith(('.java', '.json', '.lang')):
                continue
            chemin = os.path.join(racine, f)
            for n, ligne in enumerate(open(chemin, encoding='utf-8'), 1):
                m = RETIRES.search(ligne)
                if m:
                    erreurs.append('%s:%d : « %s » a été retiré en v0.7' % (os.path.relpath(chemin, SRC), n, m.group(0)))
    # chaque maladie : un remède et des prises qui existent
    prep_ids = {p['id'] for p in preps}
    for m in charger('maladies.json')['maladies']:
        for x in [m['remede']] + m['accompagnements'] + [i for pr in m['prises'] for i in pr['items']]:
            if x not in prep_ids:
                erreurs.append('maladie %d (%s) : %s n\'existe pas' % (m['numero'], m['id'], x))
    for e in erreurs:
        print('  !! ' + e)
    print('%d objets, %d blocs vérifiés : %s' % (len(objets) + len(blocs_objet), len(blocs_objet) + len(blocs_monde), 'OK' if not erreurs else '%d erreur(s)' % len(erreurs)))
    sys.exit(1 if erreurs else 0)


if __name__ == '__main__':
    main()
