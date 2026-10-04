"""
Assemble et vérifie les données du Hunter Virus, puis écrit les JSON lus par le mod
(assets/hxrpmetiers/data/virus/*.json, copiés à la première ouverture dans config/hxrpmetiers/virus/).

    python3 tools/virus/donnees.py

Vérifie aussi tout contre le cahier des charges (tools/virus/cahier/*.json) : nombre de stades,
noms des remèdes, ingrédients connus, prises cohérentes avec le remède et les accompagnements.
"""
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from ingredients import INGREDIENTS, REPRIS_DU_GOURMET  # noqa: E402
from preparations import PREPARATIONS  # noqa: E402
from maladies import MALADIES, SYMPTOMES  # noqa: E402
from blessures import BLESSURES  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
A = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'hxrpmetiers')
OUT = os.path.join(A, 'data', 'virus')
FOOD = json.load(open(os.path.join(A, 'data', 'food.json'), encoding='utf-8'))
GOURMET = {e['id']: e for g in FOOD for e in FOOD[g]}

ING = {i['id']: i for i in INGREDIENTS}
PREP = {p['id']: p for p in PREPARATIONS}
GROUPES = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-']
MACHINES = ['yagen', 'hachoir', 'chaudron', 'alambic', 'jarres', 'balance', 'mortier', 'pilulier', 'table']
MODES = {
    'chaudron': {'douce', 'forte', 'reduction', 'sec', 'fondre', 'ebullition', 'forte_long'},
    'alambic': {'eau_florale', 'distiller', 'concentrer'},
    'balance': {'', 'stricte'},
    'table': {'onguent', 'baume', 'cataplasme', 'compresse', 'filtrer', 'refroidir', 'emplatre', 'attelle', 'pansement', 'bandage'},
    'mortier': {'', 'gel', 'baume', 'pate'},
}
EFFETS = set('''lenteur0 lenteur1 lenteur2 faiblesse1 faiblesse2 nausee cecite degats faim+ faim++ soif+ soif++ sprint5 sprint8 nosprint
saut- nosaut recup- noregen coeur-1 coeur-2 coeur-4 nourriture-15 nourriture-25 lait muet floue paupieres rouge toux eternue tremble
mains vertige lumiere acouphenes hallu tunnel gris ambre bleu inverse desoriente immobile3 immobile5
saignement hemorragie degats_saut degats_marche poison'''.split())
CONTINUS = {'faim+', 'faim++', 'soif+', 'soif++', 'sprint5', 'sprint8', 'nosprint', 'saut-', 'nosaut', 'recup-', 'noregen',
            'coeur-1', 'coeur-2', 'coeur-4', 'nourriture-15', 'nourriture-25', 'lait', 'muet', 'lenteur0'}
CONDITIONS = {'nuit', 'jour', 'eau', 'y100', 'combat'}
CONSIGNES = {'a_jeun_avant', 'a_jeun_entre', 'sans_nourriture', 'a_jeun_apres', 'a_jeun_pendant', 'repos', 'pas_de_sprint',
             'aucun_combat', 'dormir_apres_chaque_prise', 'dormir_apres', 'dormir_entre', 'dormir_dans_l_heure', 'au_chaud', 'a_l_ombre',
             'ombre_pendant', 'pres_du_feu', 'pas_mouiller', 'pas_d_alcool', 'y_min', 'y_max', 'pas_miner_sous', 'pas_de_grotte',
             'pas_d_eau', 'distance_max', 'manger_plat', 'pas_de_viande', 'pas_de_poisson', 'soleil_apres_prise', 'pas_de_nether'}
GROUPES_SPECIAUX = {
    'TOUTES_LES_PLANTES': [i['id'] for i in INGREDIENTS if i['categorie'] == 'plante'],
    'TOUS_LES_CHAMPIGNONS': [i['id'] for i in INGREDIENTS if i['categorie'] == 'champignon'],
    'TOUTES_LES_RARETES': [i['id'] for i in INGREDIENTS if i['categorie'] == 'rare_hunter'],
    'TOUTES_LES_RESSOURCES': [i['id'] for i in INGREDIENTS if i['categorie'] == 'animal_ruche'],
}

erreurs = []


def err(m):
    erreurs.append(m)


def resoudre(x):
    """Identifiant d'objet : ingrédient custom, objet du Gourmet, préparation, ou objet d'un autre mod (modid:id)."""
    if x == 'eau':
        return 'bouteille_d_eau'
    if ':' in x or x == 'seringue_vide' or x in ING or x in GOURMET or x in PREP_PRODUITS:
        return x
    err('ingrédient inconnu : ' + x)
    return x


PREP_PRODUITS = {p.get('produit', p['id']) for p in PREPARATIONS}


def etapes(s, pid):
    out = []
    for part in s.split('>'):
        part = part.strip()
        m, _, mode = part.partition(':')
        if m not in MACHINES:
            err('%s : machine inconnue %s' % (pid, m))
        e = {'machine': m}
        if m == 'jarres':
            mm = re.fullmatch(r'(\d+)(o?)', mode)
            if not mm:
                err('%s : durée de jarres invalide %s' % (pid, mode))
                continue
            e['heures'] = int(mm.group(1))
            if not 1 <= e['heures'] <= 24:
                err('%s : jarres hors 1-24 h' % pid)
            if mm.group(2):
                e['mode'] = 'ouverte'
        elif m in MODES:
            if mode not in MODES[m]:
                err('%s : mode %s inconnu pour %s' % (pid, mode, m))
            if mode:
                e['mode'] = mode
        elif mode:
            err('%s : %s ne prend pas de mode' % (pid, m))
        out.append(e)
    return out


def effets(tokens, vie, pid):
    out = []
    for t in tokens:
        mode = None
        if t[0] in '~!':
            mode = 'episode' if t[0] == '~' else 'continu'
            t = t[1:]
        code, _, cond = t.partition('@')
        if code not in EFFETS:
            err('%s : effet inconnu %s' % (pid, code))
        if cond and cond not in CONDITIONS:
            err('%s : condition inconnue %s' % (pid, cond))
        if mode is None:
            mode = 'continu' if (vie or code in CONTINUS) else 'episode'
        e = {'code': code, 'mode': mode}
        if cond:
            e['condition'] = cond
        out.append(e)
    return out


def sang(s):
    out = {}
    s = s.replace("rien d'anormal", '').strip()
    parts = [p.strip() for p in s.split('·')]
    for p in parts:
        if not p:
            continue
        if p.startswith('MR'):
            out['MR'] = p.split(':', 1)[1].strip()
            continue
        for v in p.split(','):
            v = v.strip()
            mm = re.fullmatch(r'(GB|GR|PL|SU|FE|TX|PA)(↑↑|↑|↓|\+)', v)
            if not mm:
                err('valeur sanguine illisible : ' + v)
                continue
            out[mm.group(1)] = mm.group(2)
    return out


def touche(t):
    if t is None:
        return None
    t = t.strip()
    if t == 'Hommes':
        return {'sexe': 'homme'}
    if t == 'Femmes':
        return {'sexe': 'femme'}
    if t == 'Tous sauf les groupes O':
        return {'groupes': [g for g in GROUPES if not g.startswith('O')]}
    mm = re.fullmatch(r'Groupes? (A|B|AB|O)(?: et (A|B|AB|O))?', t)
    if not mm:
        err('restriction illisible : ' + t)
        return None
    lettres = [x for x in mm.groups() if x]
    return {'groupes': [g for g in GROUPES if g.rstrip('+-') in lettres]}


def bonus(p):
    """Effet bonus propre au remède, quand il est réussi à 95 % ou plus."""
    n = p['id']
    if p.get('soin'):
        return 'soin_express'
    if p['forme'] == 'seringue':
        return 'immunite'
    if p['forme'] == 'pilules':
        return 'convalescence'
    if p['forme'] in ('bandage', 'attelle'):
        return 'resistance'
    if p['forme'] == 'fumigation':
        return 'apaisement'
    if p['forme'] == 'onguent':
        return 'regeneration'
    if n.startswith('sirop'):
        return 'apaisement'
    if n.startswith('eau_florale') or n.startswith('eau_de'):
        return 'rafraichi'
    if n.startswith('teinture') or n.startswith('vinaigre') or n.startswith('lotion'):
        return 'resistance'
    if n.startswith('elixir') or n.startswith('antidote') or n.startswith('antivenin'):
        return 'vigueur'
    if n.startswith('infusion') or n.startswith('gargarisme'):
        return 'rafraichi'
    if n.startswith('bouillon'):
        return 'rassasie'
    return 'regeneration'


def main():
    os.makedirs(OUT, exist_ok=True)
    cahier = os.path.join(HERE, 'cahier')
    orig_m = {m['numero']: m for m in json.load(open(os.path.join(cahier, 'virus_maladies.json'), encoding='utf-8'))}
    orig_b = {b['id']: b for b in json.load(open(os.path.join(cahier, 'virus_blessures.json'), encoding='utf-8'))}
    orig_i = json.load(open(os.path.join(cahier, 'virus_ingredients.json'), encoding='utf-8'))
    # ---------------------------------------------------------------- ingrédients
    noms_orig = {i['nom'] for i in orig_i['custom']}
    for i in INGREDIENTS:
        if i['nom'] not in noms_orig:
            err('ingrédient absent du cahier : ' + i['nom'])
    for nom, gid in REPRIS_DU_GOURMET.items():
        if gid not in GOURMET:
            err('objet du Gourmet introuvable : ' + gid)
    collision = set(ING) & set(GOURMET)
    if collision:
        err('identifiants en double avec le Gourmet : %s' % collision)
    # ---------------------------------------------------------------- préparations
    rangs = {}
    for m in MALADIES:
        for x in [m['remede']] + m['accompagnements']:
            rangs[x] = min(rangs.get(x, 9), m['rang'])
    usages = {}

    def utiliser(pid, r):
        if pid in PREP:
            usages[pid] = min(usages.get(pid, 9), r)
            for x in PREP[pid]['ingredients']:
                if x in PREP:
                    utiliser(x, r)
    for pid, r in rangs.items():
        utiliser(pid, r)
    preps = []
    for p in PREPARATIONS:
        ings = []
        for x in p['ingredients']:
            ings.extend(GROUPES_SPECIAUX.get(x, [x]))
        q = {'id': p['id'], 'nom': p['nom'], 'forme': p['forme'], 'ingredients': [resoudre(x) for x in ings],
             'etapes': etapes(p['etapes'], p['id']), 'texte': p['texte'],
             'rang': p.get('rang', 3 if p.get('secret') else 0 if p['forme'] in ('base', 'ingredient') or p.get('soin') else usages.get(p['id'], 0)),
             'produit': p.get('produit', p['id']), 'quantite': p.get('quantite', 1), 'bonus': bonus(p)}
        if p['forme'] == 'base' and p['id'] in usages and not p.get('secret'):
            q['rang'] = usages[p['id']] if p['id'] in ('teinture_de_valeriane',) else 0
        if p.get('soin'):
            q['soin'] = p['soin']
        if p.get('secret'):
            q['secret'] = True
        if p['forme'] == 'seringue' and 'seringue_vide' not in q['ingredients']:
            err(p['id'] + ' : une préparation en seringue doit consommer une seringue vide')
        if len(q['etapes']) == 0:
            err(p['id'] + ' : aucune étape')
        if p['forme'] not in ('fiole', 'seringue', 'pilules', 'onguent', 'bandage', 'attelle', 'fumigation', 'base', 'ingredient'):
            err(p['id'] + ' : forme inconnue ' + p['forme'])
        preps.append(q)
    utilises = set(rangs) | {b['soin'] for b in BLESSURES} | {x for p in PREPARATIONS for x in p['ingredients']}
    for p in PREPARATIONS:
        if p['id'] not in utilises and p['forme'] != 'ingredient' and p['id'] != 'remede_du_second_souffle':
            err('préparation jamais utilisée : ' + p['id'])
    # ---------------------------------------------------------------- maladies
    sym_ids = {s for s, _ in SYMPTOMES}
    maladies = []
    for m in MALADIES:
        o = orig_m[m['numero']]
        if len(m['stades']) != len(o['stades']):
            err('%s : %d stades au lieu de %d' % (m['id'], len(m['stades']), len(o['stades'])))
        if not m['stades'][-1]['vie'] or any(s['vie'] for s in m['stades'][:-1]):
            err(m['id'] + ' : seul le dernier stade est « à vie »')
        if m['nom'] != o['nom'] or m['rang'] != o['rang'] or m['jours_avant_dernier_stade'] != o['jours_avant_dernier_stade']:
            err(m['id'] + ' : nom, rang ou durée différents du cahier')
        if m['premier_message'] != o['premier_message']:
            err(m['id'] + ' : premier message différent du cahier')
        if PREP[m['remede']]['nom'] != o['remede_specifique']['nom']:
            err('%s : remède %s ≠ %s' % (m['id'], PREP[m['remede']]['nom'], o['remede_specifique']['nom']))
        if [PREP[a]['nom'] for a in m['accompagnements']] != [a['nom'] for a in o.get('accompagnement', [])]:
            err('%s : accompagnements %s ≠ %s' % (m['id'], [PREP[a]['nom'] for a in m['accompagnements']], [a['nom'] for a in o['accompagnements']]))
        if m['traitement_texte'] != o['traitement'].replace('**', ''):
            err(m['id'] + ' : texte du traitement différent du cahier')
        if PREP[m['remede']]['forme'] != o['remede_specifique']['forme']:
            err('%s : forme du remède %s ≠ %s' % (m['id'], PREP[m['remede']]['forme'], o['remede_specifique']['forme']))
        for a, oa in zip(m['accompagnements'], o.get('accompagnement', [])):
            if PREP[a]['forme'] != oa['forme']:
                err('%s : forme de %s %s ≠ %s' % (m['id'], a, PREP[a]['forme'], oa['forme']))
        permis = set([m['remede']] + m['accompagnements'])
        vus = set()
        for k, p in enumerate(m['prises']):
            for x in p['items']:
                if x not in permis:
                    err('%s : prise %d utilise %s' % (m['id'], k + 1, x))
                vus.add(x)
            if k == 0 and p['delai'] != 0:
                err(m['id'] + ' : la prise 1 n\'a pas de délai')
        if vus != permis:
            err('%s : %s jamais pris' % (m['id'], permis - vus))
        for c in m['consignes']:
            if c['type'] not in CONSIGNES:
                err('%s : consigne inconnue %s' % (m['id'], c['type']))
        stades = []
        for s in m['stades']:
            for x in s['symptomes']:
                if x not in sym_ids:
                    err('%s : symptôme inconnu %s' % (m['id'], x))
            stades.append({'message': s['message'], 'effets': effets(s['effets'], s['vie'], m['id']), 'symptomes': s['symptomes'], 'vie': s['vie']})
        q = {'numero': m['numero'], 'id': m['id'], 'nom': m['nom'], 'rang': m['rang'], 'touche': touche(m['touche_uniquement']),
             'touche_texte': m['touche_uniquement'], 'premier_message': m['premier_message'], 'jours': m['jours_avant_dernier_stade'],
             'sang': sang(m['sang']), 'sang_texte': m['sang'], 'stades': stades, 'remede': m['remede'], 'accompagnements': m['accompagnements'],
             'prises': m['prises'], 'consignes': m['consignes'], 'consigne_texte': m['consigne_texte'],
             'traitement_texte': m['traitement_texte'], 'origine': m['origine'], 'contexte': m['contexte']}
        if q['touche'] is None:
            del q['touche']
        maladies.append(q)
    # ---------------------------------------------------------------- blessures
    blessures = []
    for b in BLESSURES:
        o = orig_b[b['id']]
        if b['nom'] != o['nom'] or PREP[b['soin']]['nom'] != o['soin']['nom']:
            err(b['id'] + ' : nom ou soin différent du cahier')
        if PREP[b['soin']].get('soin') != b['id']:
            err(b['id'] + ' : le soin ne désigne pas la blessure')
        blessures.append({'id': b['id'], 'nom': b['nom'], 'declencheur': b['declencheur'], 'soin': b['soin'],
                          'stades': [{'message': msg, 'effets': effets(e.split(), False, b['id'])} for msg, e in b['stades']],
                          'stades_texte': o['stades']})
    if erreurs:
        print('\n'.join('!! ' + e for e in erreurs))
        raise SystemExit('%d erreur(s)' % len(erreurs))

    def ecrire(nom, obj):
        with open(os.path.join(OUT, nom), 'w', encoding='utf-8') as f:
            json.dump(obj, f, ensure_ascii=False, indent=1)
            f.write('\n')
    ecrire('ingredients.json', {'repris_du_gourmet': REPRIS_DU_GOURMET, 'ingredients': INGREDIENTS})
    ecrire('preparations.json', {'preparations': preps})
    ecrire('maladies.json', {'symptomes': [{'id': s, 'nom': n} for s, n in SYMPTOMES], 'maladies': maladies})
    ecrire('blessures.json', {'blessures': blessures})
    print('%d ingrédients, %d préparations, %d maladies, %d blessures, %d symptômes' % (
        len(INGREDIENTS), len(preps), len(maladies), len(blessures), len(SYMPTOMES)))
    return preps, maladies, blessures


if __name__ == '__main__':
    main()
