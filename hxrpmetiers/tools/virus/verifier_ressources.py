"""
Vérifie que chaque objet et bloc enregistré par le Hunter Virus (VirusRegistre) a son modèle, son état de bloc,
ses textures et son nom français. Lancé par la CI avant la compilation.

    python3 tools/virus/verifier_ressources.py
"""
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(HERE, '..', '..', 'src', 'main', 'resources', 'assets', 'hxrpmetiers')
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
                                                       'nid_d_aigle_araignee', 'source_de_seve']
    blocs_objet += sorted({i['recolte']['bloc'] for i in ing if i['recolte']['type'] == 'sol'})
    blocs_monde = ['plante_' + i['id'] for i in ing if i['recolte']['type'] == 'plante']
    blocs_monde += ['tronc_' + i['id'] for i in ing if i['recolte']['type'] == 'tronc']
    blocs_monde += ['tronc_ecorce_bouleau', 'tronc_ecorce_saule', 'lotus_de_l_aube_plante']
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
    for b in blocs_objet + blocs_monde:
        p = os.path.join(A, 'blockstates', b + '.json')
        if not os.path.exists(p):
            erreurs.append('état de bloc absent : ' + b)
            continue
        for k, v in json.load(open(p, encoding='utf-8')).get('variants', {}).items():
            for var in (v if isinstance(v, list) else [v]):
                modele_bloc_ok(M + ':' + var['model'].split(':', 1)[1], b + '#' + k)
        if not re.search(r'^tile\.%s\.%s\.name=.+$' % (M, re.escape(b)), lang, re.M):
            erreurs.append('nom absent : tile.%s.%s.name' % (M, b))
    for e in erreurs:
        print('  !! ' + e)
    print('%d objets, %d blocs vérifiés : %s' % (len(objets) + len(blocs_objet), len(blocs_objet) + len(blocs_monde), 'OK' if not erreurs else '%d erreur(s)' % len(erreurs)))
    sys.exit(1 if erreurs else 0)


if __name__ == '__main__':
    main()
