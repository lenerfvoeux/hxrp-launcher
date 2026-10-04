"""
Les 84 ingrédients custom du Hunter Virus (data/virus_ingredients.json du cahier des charges),
avec pour chacun sa façon d'être récolté en jeu.

Récolte (champ « recolte ») :
  plante   : bloc plante posé au sol, généré par milieu ; clic droit quand il est mûr pour cueillir
             sans arracher, l'ingrédient se replante sur le bon sol.
  tronc    : champignon accroché au flanc d'un tronc (bois : chene, bouleau, chene_noir, tous).
  sol      : bloc plein du monde (terre truffière, vase, roche…), on gratte ou on creuse.
  ecorce   : clic droit sur un tronc naturel (bouleau / saule = chêne des marais et rivières).
  nid      : nid d'aigle-araignée sur les falaises.
  lotus    : fleur flottante des rivières, repousse en 3 jours réels.
  arbre    : source de sève posée par les admins.
  jarres   : cultivé à l'officine (koji).
  animal   : butin d'animaux (liste d'entités).
  ruche    : ruches sauvages (main vide).
  oeuf     : œuf du Gourmet cassé (accroupi + clic droit) ou utilisé en cuisine.

Durée de vie en heures réelles (0 = ne périme pas).
"""

# milieux (voir MilieuVirus.java) :
# plaines, prairies, collines, montagne, montagne_boisee, pics_glaces, savane, desert, aride, foret,
# foret_claire, lisiere, foret_bouleau, foret_sombre, foret_humide, taiga, taiga_enneigee, marais,
# riviere, jungle, plage, partout, grotte, grotte_profonde, abysses

def I(id, nom, cat, source, rarete, vie, recolte, **opts):
    d = {'id': id, 'nom': nom, 'categorie': cat, 'source': source, 'rarete': rarete, 'vie': vie, 'recolte': recolte}
    d.update(opts)
    return d


def plante(milieux, sol='herbe', **o):
    d = {'type': 'plante', 'milieux': milieux if isinstance(milieux, list) else [milieux], 'sol': sol}
    d.update(o)
    return d


def tronc(milieux, bois):
    return {'type': 'tronc', 'milieux': milieux if isinstance(milieux, list) else [milieux], 'bois': bois}


def sol(bloc):
    return {'type': 'sol', 'bloc': bloc}


INGREDIENTS = [
    # ------------------------------------------------------------------ 34 plantes
    I('camomille', 'Camomille', 'plante', 'Plaines', 'commune', 72, plante('plaines')),
    I('souci', 'Souci', 'plante', 'Plaines', 'commune', 72, plante('plaines')),
    I('pissenlit', 'Pissenlit', 'plante', 'Plaines, prairies', 'commune', 72, plante(['plaines', 'prairies'])),
    I('plantain', 'Plantain', 'plante', "Partout sur l'herbe", 'commune', 72, plante('partout')),
    I('lavande', 'Lavande', 'plante', 'Plaines', 'commune', 120, plante('plaines')),
    I('coquelicot', 'Coquelicot', 'plante', 'Plaines', 'commune', 48, plante('plaines')),
    I('bourrache', 'Bourrache', 'plante', 'Plaines fleuries', 'commune', 72, plante(['plaines', 'prairies'])),
    I('echinacee', 'Échinacée', 'plante', 'Plaines fleuries', 'peu commune', 96, plante(['plaines', 'prairies'])),
    I('achillee', 'Achillée', 'plante', 'Prairies, collines', 'commune', 96, plante(['prairies', 'collines'])),
    I('bourse_a_pasteur', 'Bourse-à-pasteur', 'plante', 'Prairies, bords de chemin', 'commune', 72, plante(['prairies', 'plaines'])),
    I('arnica', 'Arnica', 'plante', "Prairies d'altitude", 'peu commune', 96, plante(['montagne', 'collines'])),
    I('gentiane', 'Gentiane', 'plante', "Prairies d'altitude", 'peu commune', 240, plante(['montagne', 'collines'])),
    I('sauge', 'Sauge', 'plante', 'Savanes, collines sèches', 'commune', 120, plante(['savane', 'collines'])),
    I('fenouil_graines', 'Fenouil (graines)', 'plante', 'Plaines', 'commune', 480, plante('plaines')),
    I('reglisse_racine', 'Réglisse (racine)', 'plante', 'Savanes', 'peu commune', 480, plante('savane')),
    I('lin_graines', 'Lin (graines)', 'plante', 'Plaines', 'commune', 480, plante('plaines')),
    I('consoude', 'Consoude', 'plante', 'Bords de rivière, marais', 'peu commune', 96, plante(['riviere', 'marais'])),
    I('guimauve_racine', 'Guimauve (racine)', 'plante', 'Marais', 'peu commune', 240, plante('marais')),
    I('prele', 'Prêle', 'plante', 'Marais, bords de rivière', 'commune', 120, plante(['marais', 'riviere'])),
    I('melisse', 'Mélisse', 'plante', 'Forêts', 'commune', 72, plante('foret')),
    I('millepertuis', 'Millepertuis', 'plante', 'Forêts claires', 'peu commune', 96, plante(['foret_claire', 'foret'])),
    I('ortie', 'Ortie', 'plante', 'Forêts', 'commune', 72, plante('foret')),
    I('valeriane_racine', 'Valériane (racine)', 'plante', 'Forêts', 'peu commune', 240, plante('foret')),
    I('bardane_racine', 'Bardane (racine)', 'plante', 'Lisières de forêt', 'commune', 240, plante(['lisiere', 'foret'])),
    I('sureau_fleurs', 'Sureau (fleurs)', 'plante', 'Lisières de forêt', 'commune', 48, plante(['lisiere', 'foret'], arbuste=True)),
    I('aubepine_baies', 'Aubépine (baies)', 'plante', 'Haies, lisières', 'commune', 120, plante(['lisiere', 'plaines'], arbuste=True)),
    I('angelique_racine', 'Angélique (racine)', 'plante', 'Forêts humides', 'peu commune', 240, plante(['foret_humide', 'marais'])),
    I('ginkgo_feuilles', 'Ginkgo (feuilles)', 'plante', 'Forêts de bouleaux', 'peu commune', 120, plante('foret_bouleau', arbuste=True)),
    I('ecorce_de_bouleau', 'Écorce de bouleau', 'plante', 'Forêts de bouleaux', 'commune', 720, {'type': 'ecorce', 'bois': 'bouleau'}),
    I('ecorce_de_saule', 'Écorce de saule', 'plante', 'Marais, rivières', 'commune', 720, {'type': 'ecorce', 'bois': 'saule'}),
    I('ginseng_racine', 'Ginseng (racine)', 'plante', 'Taïgas', 'rare', 480, plante('taiga', sol='foret')),
    I('aloe', 'Aloe', 'plante', 'Déserts', 'commune', 168, plante('desert', sol='sable')),
    I('bambou', 'Bambou', 'plante', 'Jungles', 'commune', 720, plante('jungle', haut=True)),
    I('mousse_des_grottes', 'Mousse des grottes', 'plante', 'Murs humides des grottes', 'commune', 120, sol('pierre_moussue_humide')),
    # ------------------------------------------------------------------ 20 champignons
    I('pleurote', 'Pleurote', 'champignon', 'Troncs morts', 'commun', 48, tronc(['foret', 'foret_claire', 'lisiere'], 'chene')),
    I('oreille_de_judas', 'Oreille de Judas', 'champignon', 'Troncs de sureau', 'commun', 72, tronc(['lisiere', 'foret', 'marais'], 'tous')),
    I('coprin', 'Coprin', 'champignon', 'Prairies après la pluie', 'commun', 24, plante(['prairies', 'plaines'], champignon=True, pluie=True)),
    I('vesse_de_loup', 'Vesse-de-loup', 'champignon', 'Prairies', 'commun', 72, plante(['prairies', 'plaines'], champignon=True)),
    I('girolle', 'Girolle', 'champignon', 'Forêts', 'commun', 72, plante('foret', sol='foret', champignon=True)),
    I('pied_bleu', 'Pied-bleu', 'champignon', 'Forêts', 'commun', 72, plante(['foret', 'foret_sombre'], sol='foret', champignon=True)),
    I('koji', 'Koji', 'champignon', 'Cultivé sur riz cuit dans une jarre', 'commun', 96, {'type': 'jarres'}),
    I('shiitake', 'Shiitake', 'champignon', 'Troncs morts des forêts sombres', 'peu commun', 72, tronc('foret_sombre', 'chene_noir')),
    I('maitake', 'Maitake', 'champignon', 'Pied des chênes', 'peu commun', 72, plante(['foret', 'foret_sombre'], sol='foret', champignon=True, pres_chene=True)),
    I('polypore', 'Polypore', 'champignon', 'Troncs de bouleau', 'peu commun', 240, tronc('foret_bouleau', 'bouleau')),
    I('morille', 'Morille', 'champignon', 'Forêts brûlées, sols cendreux', 'peu commun', 48, plante(['foret', 'savane', 'taiga'], sol='foret', champignon=True)),
    I('trompette_des_morts', 'Trompette des morts', 'champignon', 'Forêts sombres', 'peu commun', 72, plante('foret_sombre', sol='foret', champignon=True)),
    I('amanite_rouge', 'Amanite rouge', 'champignon', 'Forêts de conifères (toxique, à doser)', 'peu commun', 96, plante(['taiga', 'taiga_enneigee'], sol='foret', champignon=True)),
    I('criniere_de_lion', 'Crinière de lion', 'champignon', 'Troncs de vieux arbres', 'rare', 72, tronc(['foret_sombre', 'foret'], 'chene_noir')),
    I('reishi', 'Reishi', 'champignon', 'Forêts sombres, pied des chênes noirs', 'rare', 240, plante('foret_sombre', sol='foret', champignon=True, pres_chene=True)),
    I('chaga', 'Chaga', 'champignon', 'Troncs de bouleau, biomes froids', 'rare', 480, tronc(['taiga', 'taiga_enneigee', 'foret_bouleau'], 'bouleau')),
    I('cordyceps', 'Cordyceps', 'champignon', 'Taïgas enneigées, sur insectes morts', 'rare', 240, plante('taiga_enneigee', sol='neige', champignon=True)),
    I('truffe_noire', 'Truffe noire', 'champignon', 'Sous terre près des chênes (creuser)', 'rare', 96, sol('terre_truffiere')),
    I('champignon_luminescent', 'Champignon luminescent', 'champignon', 'Grottes profondes', 'rare', 120, plante('grotte_profonde', sol='pierre', champignon=True, lumiere=7)),
    I('bolet_cendre', 'Bolet cendré', 'champignon', 'Montagnes de Kukuroo', 'rare', 96, plante(['montagne_boisee', 'montagne'], sol='foret', champignon=True)),
    # ------------------------------------------------------------------ 15 ressources rares du monde Hunter
    I('fleur_de_lune_pale', 'Fleur de lune pâle', 'rare_hunter', "Plaines, n'éclot que la nuit", 'rare', 48, plante('plaines', nuit=True, lumiere=5)),
    I('algue_noire', 'Algue noire', 'rare_hunter', 'Fond des marais', 'rare', 72, sol('vase_a_algue_noire')),
    I('baie_de_givre', 'Baie de givre', 'rare_hunter', 'Pics glacés', 'rare', 168, plante('pics_glaces', sol='neige', arbuste=True)),
    I('pollen_dore', 'Pollen doré', 'rare_hunter', "Fleur solitaire des déserts, à l'aube", 'rare', 72, plante('desert', sol='sable', aube=True)),
    I('mousse_de_numere', 'Mousse de Numere', 'rare_hunter', 'Marais de Numere', 'rare', 96, plante('marais', tapis=True)),
    I('baie_de_l_ile_de_la_baleine', "Baie de l'île de la Baleine", 'rare_hunter', 'Rivages des îles', 'rare', 120, plante('plage', sol='sable', arbuste=True)),
    I('feuille_de_kukuroo', 'Feuille de Kukuroo', 'rare_hunter', 'Montagnes boisées de Kukuroo', 'très rare', 168, plante('montagne_boisee', sol='foret', arbuste=True)),
    I('oeuf_d_aigle_araignee', "Œuf d'aigle-araignée", 'rare_hunter', 'Nids sur falaises et ravins', 'très rare', 96, {'type': 'nid'}),
    I('chardon_de_meteor', 'Chardon de Meteor', 'rare_hunter', 'Terres arides', 'très rare', 240, plante(['aride', 'savane', 'desert'], sol='sable')),
    I('lys_des_nuees', 'Lys des nuées', 'rare_hunter', 'Sommets au-dessus de Y 110', 'très rare', 72, plante(['montagne', 'pics_glaces', 'montagne_boisee'], sol='montagne', ymin=110)),
    I('orchidee_de_kakin', 'Orchidée de Kakin', 'rare_hunter', "Cœur des jungles, à l'ombre", 'très rare', 72, plante('jungle', sol='foret', ombre=True)),
    I('racine_de_fer_sang', 'Racine de fer-sang', 'rare_hunter', 'Grottes sous Y 20', 'très rare', 480, sol('roche_a_fer_sang')),
    I('mousse_des_abysses', 'Mousse des abysses', 'rare_hunter', 'Grottes sous Y 12, lumineuse', 'très rare', 168, sol('roche_des_abysses')),
    I('lotus_de_l_aube', "Lotus de l'aube", 'rare_hunter', 'Rivières, une fleur par zone, repousse en 3 jours', 'légendaire', 48, {'type': 'lotus'}),
    I('seve_de_l_arbre_monde', "Sève de l'Arbre-Monde", 'rare_hunter', 'Événement ou lieu placé par les admins', 'légendaire', 168, {'type': 'arbre'}),
    # ------------------------------------------------------------------ 15 ressources animales et de ruche
    I('plume', 'Plume', 'animal_ruche', 'Poulets, canards, dindes du Gourmet', 'commune', 0, {'type': 'animal', 'de': ['chicken', 'canard', 'dinde']}),
    I('duvet', 'Duvet', 'animal_ruche', 'Canards du Gourmet', 'commune', 0, {'type': 'animal', 'de': ['canard']}),
    I('laine', 'Laine', 'animal_ruche', 'Moutons du Gourmet', 'commune', 0, {'type': 'animal', 'de': ['sheep']}),
    I('saindoux', 'Saindoux', 'animal_ruche', 'Porcs du Gourmet', 'commun', 0, {'type': 'animal', 'de': ['pig']}),
    I('os', 'Os', 'animal_ruche', 'Tous les animaux du Gourmet', 'commun', 0, {'type': 'animal', 'de': ['tous']}),
    I('coquille_d_oeuf', "Coquille d'œuf", 'animal_ruche', 'Œufs du Gourmet', 'commune', 0, {'type': 'oeuf'}),
    I('ecailles', 'Écailles', 'animal_ruche', 'Poissons du Gourmet', 'commune', 0, {'type': 'animal', 'de': ['poissons']}),
    I('peau_de_boeuf', 'Peau de bœuf', 'animal_ruche', 'Bœufs du Gourmet', 'commune', 0, {'type': 'animal', 'de': ['cow']}),
    I('peau_de_cerf', 'Peau de cerf', 'animal_ruche', 'Cerfs du Gourmet', 'peu commune', 0, {'type': 'animal', 'de': ['cerf']}),
    I('bois_de_cerf', 'Bois de cerf', 'animal_ruche', 'Cerfs du Gourmet', 'peu commun', 0, {'type': 'animal', 'de': ['cerf']}),
    I('defense_de_sanglier', 'Défense de sanglier', 'animal_ruche', 'Sangliers du Gourmet', 'peu commune', 0, {'type': 'animal', 'de': ['sanglier']}),
    I('carapace_de_crabe', 'Carapace de crabe', 'animal_ruche', 'Crabes du Gourmet', 'peu commune', 0, {'type': 'animal', 'de': ['crabe']}),
    I('encre_de_calamar', 'Encre de calamar', 'animal_ruche', 'Calamars du Gourmet', 'peu commune', 0, {'type': 'animal', 'de': ['squid']}),
    I('cire_d_abeille', "Cire d'abeille", 'animal_ruche', 'Ruches sauvages (forêts)', 'commune', 0, {'type': 'ruche'}),
    I('propolis', 'Propolis', 'animal_ruche', 'Ruches sauvages, en petite quantité', 'peu commune', 0, {'type': 'ruche'}),
]

# Produits du Gourmet réutilisés tels quels (ids réels du Gourmet)
REPRIS_DU_GOURMET = {
    'riz': 'riz', 'miel': 'miel', 'ail': 'ail', 'menthe': 'menthe', 'thym': 'thym', 'romarin': 'romarin',
    'coriandre': 'coriandre', 'citron': 'citron', 'feuilles de thé': 'feuilles_de_the', 'artichaut': 'artichaut',
    'lait': 'lait', 'œuf': 'oeuf', 'sel': 'sel', "bouteille d'eau": 'bouteille_d_eau',
}

assert len(INGREDIENTS) == 84, len(INGREDIENTS)
assert len({i['id'] for i in INGREDIENTS}) == 84
for _c, _n in (('plante', 34), ('champignon', 20), ('rare_hunter', 15), ('animal_ruche', 15)):
    assert sum(1 for i in INGREDIENTS if i['categorie'] == _c) == _n, _c
