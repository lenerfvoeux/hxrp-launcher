"""
Le formulaire de l'officine : chaque préparation (ingrédient préparé, remède, accompagnement, soin)
avec ses ingrédients et sa chaîne de machines, converties depuis les recettes lisibles du cahier
des charges (« ingrédient + ingrédient → Machine → Machine »).

Étapes : « machine:mode » séparées par « > ».
  hachoir, yagen, pilulier          sans mode
  mortier                           (pilon) ; mode purement descriptif : gel, baume, pate
  chaudron   douce | forte | reduction | sec | fondre | ebullition    (+ « _long » : chauffe plus longue)
  alambic    eau_florale | distiller | concentrer
  jarres     nombre d'heures d'attente réelle (1 à 24), « o » = jarre ouverte (vinaigre)
  balance    normal | stricte (dose stricte : dépasser la masse gâche tout)
  table      onguent | baume | cataplasme | compresse | filtrer | refroidir | emplatre | attelle | pansement | bandage

Formes : fiole, seringue, pilules, onguent, bandage, attelle, fumigation (administrées par un Virus),
base (ingrédient préparé), ingredient (le koji, qui est aussi un ingrédient récolté).
« eau » = bouteille d'eau du Gourmet ; « seringue_vide » = seringue du meuble à tiroirs.
"""

PREPARATIONS = []


def P(id, nom, forme, ingredients, etapes, texte, **o):
    d = {'id': id, 'nom': nom, 'forme': forme, 'ingredients': ingredients.split() if isinstance(ingredients, str) else ingredients,
         'etapes': etapes, 'texte': texte}
    d.update(o)
    PREPARATIONS.append(d)
    return id


# ============================================================================ ingrédients préparés
P('koji_culture', 'Koji (culture)', 'ingredient', 'riz eau koji', 'chaudron:forte > jarres:12',
  'Riz (Gourmet) cuit au Chaudron → Jarres avec un koji (12 h)', produit='koji', quantite=2)
P('koji_sauvage', 'Koji sauvage', 'ingredient', 'riz eau', 'chaudron:forte > jarres:24',
  "Riz cuit au Chaudron laissé 24 h dans une jarre ouverte : les spores de l'air s'y installent", produit='koji', quantite=1)
P('alcool_de_riz', 'Alcool de riz', 'base', 'riz koji eau', 'mortier > jarres:6 > alambic:distiller',
  'Riz + koji + eau → Mortier → Jarres (fermentation 6 h) → Alambic')
P('alcool_fort', 'Alcool fort', 'base', 'alcool_de_riz', 'alambic:distiller', 'Alcool de riz → Alambic (2e distillation)')
P('vinaigre_de_riz', 'Vinaigre de riz', 'base', 'alcool_de_riz', 'jarres:6o', 'Alcool de riz → Jarres (6 h, ouvertes)')
P('huile_de_lin', 'Huile de lin', 'base', 'lin_graines', 'yagen > jarres:2', 'Graines de lin → Yagen → Jarres (décantation 2 h)')
P('base_d_onguent', "Base d'onguent", 'base', 'saindoux cire_d_abeille', 'chaudron:fondre > table:onguent',
  "Saindoux + cire d'abeille → Chaudron (fondre) → Table")
P('bandes_de_laine', 'Bandes de laine', 'base', 'laine', 'hachoir > chaudron:ebullition', 'Laine → Hachoir → Chaudron (ébullition)')
P('charbon_de_bambou', 'Charbon de bambou', 'base', 'bambou', 'hachoir > chaudron:sec > yagen', 'Bambou → Hachoir → Chaudron (à sec) → Yagen')
P('charbon_d_os', "Charbon d'os", 'base', 'os', 'chaudron:sec > yagen', 'Os → Chaudron (à sec, calcination) → Yagen')
# intermédiaires nommés dans plusieurs recettes
P('teinture_de_valeriane', 'Teinture de valériane', 'base', 'valeriane_racine alcool_de_riz', 'hachoir > jarres:3',
  'Racine de valériane → Hachoir + alcool de riz → Jarres (3 h)')
P('huile_essentielle_de_lavande', 'Huile essentielle de lavande', 'base', 'lavande eau', 'alambic:distiller',
  'Lavande + eau → Alambic (huile essentielle)')

# ============================================================================ soins des blessures
P('baume_d_arnica', "Baume d'arnica", 'onguent', 'arnica base_d_onguent', 'hachoir > mortier:baume > table:baume',
  "Arnica fraîche + base d'onguent → Hachoir → Mortier → Table", soin='contusions')
P('elixir_de_ginkgo', 'Élixir de ginkgo', 'fiole', 'ginkgo_feuilles bourrache eau miel', 'chaudron:douce > balance > alambic:concentrer',
  'Ginkgo + bourrache + eau + miel → Chaudron (infusion) → Balance → Alambic', soin='commotion')
P('attelle_de_consoude', 'Attelle de consoude', 'attelle', 'consoude carapace_de_crabe base_d_onguent peau_de_boeuf bambou bandes_de_laine',
  'hachoir > mortier > table:emplatre > table:attelle',
  "Consoude + poudre de carapace de crabe + base d'onguent + peau de bœuf + bambou + bandes de laine → Hachoir → Mortier → Table (emplâtre) → Table (attelle)",
  soin='jambe')
P('pansement_d_achillee', "Pansement d'achillée", 'bandage', 'achillee bourse_a_pasteur duvet bandes_de_laine', 'yagen > balance > table:pansement',
  "Poudre d'achillée + poudre de bourse-à-pasteur + duvet + bandes de laine → Yagen → Balance → Table", soin='plaie')
P('onguent_de_souci_et_d_aloe', "Onguent de souci et d'aloe", 'onguent', 'souci huile_de_lin aloe base_d_onguent',
  'jarres:3 > hachoir > mortier:gel > table:onguent',
  "Huile infusée de souci + gel d'aloe + base d'onguent → Jarres → Hachoir → Mortier → Table", soin='brulure')
P('antivenin_de_bardane', 'Antivenin de bardane', 'fiole', 'bardane_racine plantain charbon_d_os eau', 'hachoir > chaudron:forte > balance',
  "Racine de bardane + plantain + charbon d'os + eau → Hachoir → Chaudron (décoction) → Balance", soin='morsure')
P('bandage_de_peau_de_cerf', 'Bandage de peau de cerf', 'bandage', 'peau_de_cerf angelique_racine saindoux', 'hachoir > mortier:baume > table:bandage',
  "Peau de cerf + racine d'angélique + saindoux → Hachoir → Mortier (baume) → Table", soin='gelure')

# ============================================================================ remèdes et accompagnements (52 maladies)
# 1 Fièvre des marais
P('decoction_de_saule_et_bourrache', 'Décoction de saule et bourrache', 'fiole', 'ecorce_de_saule bourrache eau', 'hachoir > chaudron:forte',
  'écorce de saule + bourrache + eau → Hachoir → Chaudron (fort)')
P('sirop_de_thym', 'Sirop de thym', 'fiole', 'thym eau miel', 'chaudron:douce > chaudron:reduction',
  'infusion de thym (Gourmet) + miel → Chaudron (réduction)')
# 2 Angine rouge
P('teinture_de_propolis', 'Teinture de propolis', 'fiole', 'propolis alcool_de_riz', 'jarres:2', 'propolis + alcool de riz → Jarres (2 h)')
P('sirop_de_guimauve_a_la_propolis', 'Sirop de guimauve à la propolis', 'fiole', 'guimauve_racine eau teinture_de_propolis miel',
  'hachoir > chaudron:forte > chaudron:reduction',
  'racine de guimauve → Hachoir → Chaudron (décoction) + teinture de propolis + miel → Chaudron (réduction)')
P('gargarisme_de_sauge', 'Gargarisme de sauge', 'fiole', 'sauge eau', 'chaudron:douce', 'sauge + eau → Chaudron (infusion)')
# 3 Septicémie
P('injection_de_chaga_et_d_echinacee', "Injection de chaga et d'échinacée", 'seringue', 'chaga echinacee eau seringue_vide',
  'hachoir > chaudron:forte > balance > alambic:concentrer',
  "chaga + racine d'échinacée → Hachoir → Chaudron (décoction) → Balance → Alambic (concentrer)")
P('cataplasme_d_ail', "Cataplasme d'ail", 'onguent', 'ail miel', 'mortier > table:cataplasme', 'ail (Gourmet) + miel → Mortier → Table')
P('pilules_de_reishi', 'Pilules de reishi', 'pilules', 'reishi miel', 'hachoir > yagen > balance > mortier > pilulier',
  'poudre de reishi + miel → Balance → Mortier → Pilulier')
# 4 Toux des mineurs
P('sirop_de_lin_et_de_mousse_des_grottes', 'Sirop de lin et de mousse des grottes', 'fiole', 'lin_graines mousse_des_grottes eau miel',
  'chaudron:forte > chaudron:reduction', 'graines de lin + mousse des grottes + eau → Chaudron (décoction) + miel → Chaudron (réduction)')
P('eau_florale_de_lavande', 'Eau florale de lavande', 'fiole', 'lavande eau', 'alambic:eau_florale', 'lavande + eau → Alambic')
# 5 Fièvre du lait
P('pilules_de_coquille_et_de_fenouil', 'Pilules de coquille et de fenouil', 'pilules', 'coquille_d_oeuf fenouil_graines miel',
  'yagen > balance > mortier > pilulier', "coquille d'œuf + graines de fenouil → Yagen → Balance + miel → Mortier → Pilulier")
P('infusion_de_menthe', 'Infusion de menthe', 'fiole', 'menthe eau', 'chaudron:douce', 'menthe (Gourmet) + eau → Chaudron (infusion)')
# 6 Mal des tanneurs
P('onguent_de_bardane_et_millepertuis', 'Onguent de bardane et millepertuis', 'onguent',
  'bardane_racine millepertuis huile_de_lin base_d_onguent', 'hachoir > yagen > jarres:3 > table:onguent',
  "racine de bardane → Hachoir → Yagen + huile infusée de millepertuis + base d'onguent → Table")
P('decoction_d_ortie', "Décoction d'ortie", 'fiole', 'ortie eau', 'hachoir > chaudron:forte', 'ortie + eau → Hachoir → Chaudron (fort)')
# 7 Bouche-de-pierre
P('injection_de_criniere_de_lion', 'Injection de crinière de lion', 'seringue', 'criniere_de_lion eau teinture_de_valeriane seringue_vide',
  'hachoir > chaudron:forte > balance > alambic:concentrer',
  'crinière de lion → Hachoir → Chaudron (décoction) + teinture de valériane → Balance → Alambic')
P('infusion_de_melisse', 'Infusion de mélisse', 'fiole', 'melisse eau', 'chaudron:douce', 'mélisse + eau → Chaudron (infusion)')
# 8 Peste grise
P('elixir_du_lotus_de_l_aube', "Élixir du lotus de l'aube", 'fiole', 'lotus_de_l_aube eau miel alcool_fort',
  'alambic:eau_florale > jarres:3 > alambic:concentrer',
  "lotus de l'aube + eau → Alambic (eau florale) + miel + alcool fort → Jarres (3 h) → Alambic")
P('decoction_d_echinacee', "Décoction d'échinacée", 'fiole', 'echinacee eau', 'hachoir > chaudron:forte',
  "racine d'échinacée + eau → Hachoir → Chaudron (fort)")
P('pilules_de_cordyceps', 'Pilules de cordyceps', 'pilules', 'cordyceps miel', 'hachoir > yagen > balance > mortier > pilulier',
  'poudre de cordyceps + miel → Balance → Mortier → Pilulier')
# 9 Rouille des plaies
P('cataplasme_de_plantain', 'Cataplasme de plantain', 'onguent', 'plantain miel', 'mortier > table:cataplasme',
  'plantain frais + miel → Mortier → Table')
# 10 Rhume des plaines
P('sirop_de_sureau', 'Sirop de sureau', 'fiole', 'sureau_fleurs eau miel', 'chaudron:douce > chaudron:reduction',
  'fleurs de sureau + eau → Chaudron (infusion) + miel → Chaudron (réduction)')
P('infusion_de_camomille_et_thym', 'Infusion de camomille et thym', 'fiole', 'camomille thym eau', 'chaudron:douce',
  'camomille + thym (Gourmet) + eau → Chaudron (infusion)')
# 11 Grippe des hauts plateaux
P('decoction_de_shiitake_et_d_angelique', "Décoction de shiitake et d'angélique", 'fiole', 'shiitake angelique_racine eau',
  'hachoir > chaudron:forte', "shiitake + racine d'angélique + eau → Hachoir → Chaudron (fort)")
# 12 Fièvre à pustules
P('onguent_d_aloe_et_de_prele', "Onguent d'aloe et de prêle", 'onguent', 'aloe prele base_d_onguent',
  'hachoir > mortier:gel > yagen > table:onguent', "aloe → Hachoir → Mortier (gel) + prêle → Yagen + base d'onguent → Table")
P('infusion_de_pissenlit', 'Infusion de pissenlit', 'fiole', 'pissenlit eau', 'chaudron:douce', 'pissenlit + eau → Chaudron (infusion)')
# 13 Fièvre jaune des jungles
P('pilules_de_pollen_dore', 'Pilules de pollen doré', 'pilules', 'pollen_dore miel', 'balance > mortier > pilulier',
  'pollen doré + miel → Balance → Mortier → Pilulier')
P('decoction_de_reglisse', 'Décoction de réglisse', 'fiole', 'reglisse_racine eau', 'hachoir > chaudron:forte',
  'racine de réglisse + eau → Hachoir → Chaudron (fort)')
P('teinture_de_pissenlit', 'Teinture de pissenlit', 'fiole', 'pissenlit alcool_de_riz', 'jarres:2', 'pissenlit + alcool de riz → Jarres (2 h)')
# 14 Œil rouge
P('eau_florale_de_camomille', 'Eau florale de camomille', 'fiole', 'camomille eau', 'alambic:eau_florale', 'camomille + eau → Alambic')
P('compresse_de_the', 'Compresse de thé', 'onguent', 'feuilles_de_the eau bandes_de_laine', 'chaudron:douce > table:compresse',
  'feuilles de thé (Gourmet) + eau → Chaudron (infusion) + bandes de laine → Table')
# 15 Fièvre des ruisseaux
P('pilules_de_charbon_de_bambou', 'Pilules de charbon de bambou', 'pilules', 'charbon_de_bambou miel', 'balance > mortier > pilulier',
  'charbon de bambou + miel → Balance → Mortier → Pilulier')
P('eau_de_riz_salee', 'Eau de riz salée', 'fiole', 'riz sel eau', 'chaudron:forte > table:filtrer',
  'riz (Gourmet) + sel (Gourmet) + eau → Chaudron (fort) → filtrer à la Table')
# 16 Rougeole des steppes
P('infusion_d_achillee', "Infusion d'achillée", 'fiole', 'achillee eau', 'chaudron:douce', 'achillée + eau → Chaudron (infusion)')
P('onguent_de_souci', 'Onguent de souci', 'onguent', 'souci huile_de_lin base_d_onguent', 'jarres:3 > table:onguent',
  "huile infusée de souci + base d'onguent → Table")
P('sirop_de_propolis', 'Sirop de propolis', 'fiole', 'teinture_de_propolis miel', 'chaudron:reduction',
  'teinture de propolis + miel → Chaudron (réduction)')
# 17 Langueur grise
P('decoction_de_ginseng_et_maitake', 'Décoction de ginseng et maitake', 'fiole', 'ginseng_racine maitake eau', 'hachoir > chaudron:forte',
  'racine de ginseng + maitake + eau → Hachoir → Chaudron (fort)')
P('teinture_de_melisse', 'Teinture de mélisse', 'fiole', 'melisse alcool_de_riz', 'jarres:2', 'mélisse + alcool de riz → Jarres (2 h)')
# 18 Fièvre de Meteor
P('elixir_du_chardon_de_meteor', 'Élixir du chardon de Meteor', 'seringue',
  'chardon_de_meteor eau seve_de_l_arbre_monde alcool_fort seringue_vide', 'hachoir > chaudron:forte > jarres:6 > alambic:concentrer',
  "chardon de Meteor → Hachoir → Chaudron (décoction) + sève de l'Arbre-Monde + alcool fort → Jarres (6 h) → Alambic")
P('pilules_de_bourse_a_pasteur', 'Pilules de bourse-à-pasteur', 'pilules', 'bourse_a_pasteur miel', 'yagen > balance > mortier > pilulier',
  'bourse-à-pasteur → Yagen → Balance + miel → Mortier → Pilulier')
P('eau_florale_de_lys_des_nuees', 'Eau florale de lys des nuées', 'fiole', 'lys_des_nuees eau', 'alambic:eau_florale',
  'lys des nuées + eau → Alambic')
# 19 Ver des rivières
P('pilules_de_gentiane_et_d_ail', "Pilules de gentiane et d'ail", 'pilules', 'gentiane ail miel', 'hachoir > yagen > mortier > balance > pilulier',
  'racine de gentiane → Hachoir → Yagen + ail (Gourmet) → Mortier + miel → Balance → Pilulier')
P('decoction_de_fenouil', 'Décoction de fenouil', 'fiole', 'fenouil_graines eau', 'chaudron:forte', 'graines de fenouil + eau → Chaudron (fort)')
# 20 Gale des greniers
P('onguent_de_lavande_au_saindoux', 'Onguent de lavande au saindoux', 'onguent', 'huile_essentielle_de_lavande base_d_onguent', 'table:onguent',
  "lavande + eau → Alambic (huile essentielle) + base d'onguent → Table")
P('lotion_de_vinaigre_de_riz', 'Lotion de vinaigre de riz', 'fiole', 'vinaigre_de_riz eau', 'mortier', 'vinaigre de riz + eau → Mortier')
# 21 Fièvre tierce des marais
P('teinture_de_mousse_de_numere', 'Teinture de mousse de Numere', 'fiole', 'mousse_de_numere alcool_fort', 'jarres:4',
  'mousse de Numere + alcool fort → Jarres (4 h)')
P('decoction_de_saule', 'Décoction de saule', 'fiole', 'ecorce_de_saule eau', 'hachoir > chaudron:forte', 'écorce de saule + eau → Hachoir → Chaudron (fort)')
P('sirop_de_reglisse', 'Sirop de réglisse', 'fiole', 'decoction_de_reglisse miel', 'chaudron:reduction',
  'décoction de réglisse + miel → Chaudron (réduction)')
# 22 Douve du foie
P('teinture_d_artichaut_et_de_pissenlit', "Teinture d'artichaut et de pissenlit", 'fiole', 'artichaut pissenlit alcool_de_riz', 'hachoir > jarres:3',
  'artichaut (Gourmet) + pissenlit → Hachoir + alcool de riz → Jarres (3 h)')
P('pilules_de_gentiane', 'Pilules de gentiane', 'pilules', 'gentiane miel', 'hachoir > yagen > balance > mortier > pilulier',
  'poudre de gentiane + miel → Balance → Mortier → Pilulier')
# 23 Sangsue-fantôme
P('sirop_de_fer_sang', 'Sirop de fer-sang', 'fiole', 'racine_de_fer_sang eau miel', 'hachoir > chaudron:forte > chaudron:reduction',
  'racine de fer-sang → Hachoir → Chaudron (décoction) + miel → Chaudron (réduction)')
P('cataplasme_d_algue_noire', "Cataplasme d'algue noire", 'onguent', 'algue_noire miel', 'mortier > table:cataplasme',
  'algue noire fraîche + miel → Mortier → Table')
# 24 Ver cérébral
P('pilules_de_mousse_des_abysses', 'Pilules de mousse des abysses', 'pilules', 'mousse_des_abysses champignon_luminescent miel',
  'yagen > balance > mortier > pilulier', 'mousse des abysses + champignon luminescent → Yagen → Balance + miel → Mortier → Pilulier')
P('decoction_de_ginkgo_et_valeriane', 'Décoction de ginkgo et valériane', 'fiole', 'ginkgo_feuilles valeriane_racine eau', 'hachoir > chaudron:forte',
  'ginkgo + racine de valériane + eau → Hachoir → Chaudron (fort)')
# 25 Puces de la fièvre
P('baume_de_romarin_au_polypore', 'Baume de romarin au polypore', 'onguent', 'romarin polypore base_d_onguent', 'yagen > table:baume',
  "romarin (Gourmet) + polypore → Yagen + base d'onguent → Table")
P('fumigation_de_plumes', 'Fumigation de plumes', 'fumigation', 'plume polypore', 'chaudron:sec',
  'plumes + polypore → Chaudron (à sec), le patient respire la fumée')
# 26 Pied de marais
P('huile_d_ecorce_de_bouleau', "Huile d'écorce de bouleau", 'onguent', 'ecorce_de_bouleau huile_de_lin', 'hachoir > jarres:3',
  'écorce de bouleau → Hachoir + huile de lin → Jarres (3 h)')
P('poudre_de_sauge', 'Poudre de sauge', 'onguent', 'sauge', 'yagen > balance', 'sauge → Yagen → Balance')
# 27 Teigne des bergers
P('onguent_de_vesse_de_loup', 'Onguent de vesse-de-loup', 'onguent', 'vesse_de_loup base_d_onguent', 'yagen > balance > table:onguent',
  "vesse-de-loup → Yagen → Balance + base d'onguent → Table")
P('vinaigre_de_sauge', 'Vinaigre de sauge', 'fiole', 'sauge vinaigre_de_riz', 'jarres:2', 'sauge + vinaigre de riz → Jarres (2 h)')
# 28 Muguet blanc
P('gargarisme_de_citron_et_thym', 'Gargarisme de citron et thym', 'fiole', 'citron thym eau', 'chaudron:douce',
  'citron (Gourmet) + thym (Gourmet) + eau → Chaudron (infusion)')
P('infusion_de_sauge_au_miel', 'Infusion de sauge au miel', 'fiole', 'sauge eau miel', 'chaudron:douce', 'sauge + eau + miel → Chaudron (infusion)')
# 29 Poumon-de-spores
P('decoction_de_reishi', 'Décoction de reishi', 'fiole', 'reishi eau', 'hachoir > chaudron:forte_long', 'reishi + eau → Hachoir → Chaudron (fort, long)')
P('eau_florale_de_thym', 'Eau florale de thym', 'fiole', 'thym eau', 'alambic:eau_florale', 'thym (Gourmet) + eau → Alambic')
# 30 Ongle-noir
P('teinture_de_coprin', 'Teinture de coprin', 'fiole', 'coprin alcool_fort', 'jarres:3', 'coprin + alcool fort → Jarres (3 h)')
# 31 Lèpre des racines
P('onguent_de_kukuroo', 'Onguent de Kukuroo', 'onguent', 'feuille_de_kukuroo bolet_cendre huile_de_lin base_d_onguent', 'yagen > table:onguent',
  "feuille de Kukuroo + bolet cendré → Yagen + huile de lin + base d'onguent → Table")
P('decoction_de_cordyceps', 'Décoction de cordyceps', 'fiole', 'cordyceps eau', 'hachoir > chaudron:forte', 'cordyceps + eau → Hachoir → Chaudron (fort)')
# 32 Empoisonnement aux champignons
P('pilules_de_charbon_d_os', "Pilules de charbon d'os", 'pilules', 'charbon_d_os miel', 'balance > mortier > pilulier',
  "charbon d'os + miel → Balance → Mortier → Pilulier")
P('infusion_de_menthe_et_reglisse', 'Infusion de menthe et réglisse', 'fiole', 'menthe reglisse_racine eau', 'hachoir > chaudron:douce',
  'menthe (Gourmet) + réglisse + eau → Hachoir → Chaudron (infusion)')
# 33 Mal du plomb
P('teinture_de_coriandre', 'Teinture de coriandre', 'fiole', 'coriandre alcool_de_riz', 'jarres:3', 'coriandre (Gourmet) + alcool de riz → Jarres (3 h)')
# 34 Venin de scorpion des sables
P('antidote_de_defense_de_sanglier', 'Antidote de défense de sanglier', 'fiole', 'defense_de_sanglier infusion_de_melisse miel',
  'yagen > balance > chaudron:douce', 'défense de sanglier → Yagen → Balance + infusion de mélisse + miel → Chaudron')
P('cataplasme_d_aloe', "Cataplasme d'aloe", 'onguent', 'aloe', 'hachoir > mortier > table:cataplasme', 'aloe → Hachoir → Mortier → Table')
# 35 Alcool frelaté
P('decoction_d_oreille_de_judas', "Décoction d'oreille de Judas", 'fiole', 'oreille_de_judas eau', 'hachoir > chaudron:forte',
  'oreille de Judas + eau → Hachoir → Chaudron (fort)')
# 36 Poumon de soufre
P('sirop_de_pleurote', 'Sirop de pleurote', 'fiole', 'pleurote eau miel', 'hachoir > chaudron:forte > chaudron:reduction',
  'pleurote + eau → Hachoir → Chaudron (décoction) + miel → Chaudron (réduction)')
P('eau_florale_de_menthe', 'Eau florale de menthe', 'fiole', 'menthe eau', 'alambic:eau_florale', 'menthe (Gourmet) + eau → Alambic')
# 37 Venin de la vipère de Kakin
P('antivenin_de_kakin', 'Antivenin de Kakin', 'seringue', 'orchidee_de_kakin alcool_fort charbon_d_os seringue_vide',
  'jarres:2 > balance > chaudron:douce > alambic:concentrer',
  "orchidée de Kakin + alcool fort → Jarres (2 h) + poudre de charbon d'os → Balance → Chaudron → Alambic")
P('breuvage_de_valeriane', 'Breuvage de valériane', 'fiole', 'valeriane_racine eau miel', 'hachoir > chaudron:forte',
  'racine de valériane + eau → Hachoir → Chaudron (fort) + miel')
# 38 Mal de l'ambre
P('elixir_de_l_aigle_araignee', "Élixir de l'aigle-araignée", 'fiole', 'oeuf_d_aigle_araignee truffe_noire alcool_fort',
  'mortier > jarres:6 > alambic:concentrer', "œuf d'aigle-araignée + truffe noire → Mortier + alcool fort → Jarres (6 h) → Alambic")
# 39 Scorbut
P('sirop_d_aubepine', "Sirop d'aubépine", 'fiole', 'aubepine_baies eau miel', 'mortier > chaudron:douce > chaudron:reduction',
  "baies d'aubépine + eau → Mortier → Chaudron (infusion) + miel → Chaudron (réduction)")
# 40 Anémie de fer
P('sirop_d_ortie', "Sirop d'ortie", 'fiole', 'ortie eau miel', 'hachoir > chaudron:forte > chaudron:reduction',
  'ortie + eau → Hachoir → Chaudron (fort) + miel → Chaudron (réduction)')
# 41 Coup de chaleur
P('eau_florale_de_menthe_glacee', 'Eau florale de menthe glacée', 'fiole', 'menthe eau', 'alambic:eau_florale > table:refroidir',
  'menthe (Gourmet) + eau → Alambic, puis refroidir à la Table')
# 42 Mal des neiges
P('decoction_de_bois_de_cerf', 'Décoction de bois de cerf', 'fiole', 'bois_de_cerf eau miel', 'yagen > chaudron:forte_long',
  'bois de cerf → Yagen + eau → Chaudron (fort, long) + miel')
# 43 Mal des montagnes
P('pilules_des_cimes', 'Pilules des cimes', 'pilules', 'cordyceps ginkgo_feuilles miel', 'yagen > balance > mortier > pilulier',
  'cordyceps + ginkgo → Yagen → Balance + miel → Mortier → Pilulier')
P('infusion_de_ginkgo', 'Infusion de ginkgo', 'fiole', 'ginkgo_feuilles eau', 'chaudron:douce', 'ginkgo + eau → Chaudron (infusion)')
# 44 Insomnie noire
P('pilules_de_coquelicot', 'Pilules de coquelicot', 'pilules', 'coquelicot miel', 'yagen > balance > mortier > pilulier',
  'coquelicot → Yagen → Balance + miel → Mortier → Pilulier')
P('decoction_de_valeriane', 'Décoction de valériane', 'fiole', 'valeriane_racine eau', 'hachoir > chaudron:forte',
  'racine de valériane + eau → Hachoir → Chaudron (fort)')
P('infusion_de_camomille_et_lavande', 'Infusion de camomille et lavande', 'fiole', 'camomille lavande eau', 'chaudron:douce',
  'camomille + lavande + eau → Chaudron (infusion)')
# 45 Sang épais
P('teinture_de_lys_des_nuees', 'Teinture de lys des nuées', 'fiole', 'lys_des_nuees ginkgo_feuilles alcool_fort', 'jarres:4',
  'lys des nuées + ginkgo + alcool fort → Jarres (4 h)')
# 46 Épuisement d'aura
P('bouillon_de_girolle_et_de_ginseng', 'Bouillon de girolle et de ginseng', 'fiole', 'girolle ginseng_racine eau miel', 'hachoir > chaudron:forte',
  'girolle + racine de ginseng + eau → Hachoir → Chaudron (fort) + miel')
# 47 Fièvre des arènes
P('teinture_de_morille', 'Teinture de morille', 'fiole', 'morille alcool_de_riz', 'jarres:3', 'morille + alcool de riz → Jarres (3 h)')
# 48 Langueur de lune
P('eau_florale_de_lune_pale', 'Eau florale de lune pâle', 'fiole', 'fleur_de_lune_pale eau', 'alambic:eau_florale',
  'fleur de lune pâle + eau → Alambic')
# 49 Fièvre de Kakin
P('teinture_d_amanite_rouge', "Teinture d'amanite rouge", 'fiole', 'amanite_rouge alcool_fort', 'chaudron:sec > balance:stricte > jarres:4',
  'amanite rouge séchée → Balance (dose stricte) + alcool fort → Jarres (4 h)')
P('infusion_de_the_et_menthe', 'Infusion de thé et menthe', 'fiole', 'feuilles_de_the menthe eau', 'chaudron:douce',
  'feuilles de thé + menthe (Gourmet) + eau → Chaudron (infusion)')
# 50 Pétrification lente
P('onguent_de_la_baleine', 'Onguent de la Baleine', 'onguent', 'baie_de_l_ile_de_la_baleine ecailles huile_de_lin base_d_onguent',
  'yagen > table:onguent', "baie de l'île de la Baleine + écailles → Yagen + huile de lin + base d'onguent → Table")
P('decoction_de_fer_sang_et_pied_bleu', 'Décoction de fer-sang et pied-bleu', 'fiole', 'racine_de_fer_sang pied_bleu eau',
  'hachoir > chaudron:forte', 'racine de fer-sang + pied-bleu + eau → Hachoir → Chaudron (fort)')
# 51 Toxine de fourmi-chimère
P('elixir_de_l_arbre_monde', "Élixir de l'Arbre-Monde", 'seringue', 'seve_de_l_arbre_monde trompette_des_morts alcool_fort seringue_vide',
  'mortier > jarres:6 > alambic:concentrer', "sève de l'Arbre-Monde + trompette des morts → Mortier + alcool fort → Jarres (6 h) → Alambic")
# 52 Mal du Continent noir
P('elixir_noir', 'Élixir noir', 'seringue', 'encre_de_calamar baie_de_givre carapace_de_crabe alcool_fort seringue_vide',
  'yagen > balance > jarres:6 > alambic:concentrer',
  'encre de calamar + baie de givre + carapace de crabe → Yagen → Balance + alcool fort → Jarres (6 h) → Alambic')

# ============================================================================ Remède du Second Souffle (secret)
SECRET = {'secret': True}
P('poudre_des_trente_quatre_plantes', 'Poudre des trente-quatre plantes', 'base', 'TOUTES_LES_PLANTES', 'hachoir > yagen > balance',
  'Un exemplaire de chacune des 34 plantes → Hachoir → Yagen → Balance', **SECRET)
P('teinture_des_vingt_champignons', 'Teinture des vingt champignons', 'base', 'TOUS_LES_CHAMPIGNONS alcool_fort', 'hachoir > jarres:12',
  'Un exemplaire de chacun des 20 champignons + alcool fort → Hachoir → Jarres (12 h)', **SECRET)
P('essence_des_quinze_raretes', 'Essence des quinze raretés', 'base', 'TOUTES_LES_RARETES eau', 'alambic:distiller',
  'Un exemplaire de chacune des 15 raretés + eau → Alambic', **SECRET)
P('baume_des_quinze_ressources', 'Baume des quinze ressources', 'base', 'TOUTES_LES_RESSOURCES base_d_onguent', 'yagen > mortier > table:baume',
  "Un exemplaire de chacune des 15 ressources animales et de ruche + base d'onguent → Yagen → Mortier → Table", **SECRET)
P('cendre_doree', 'Cendre dorée', 'base', 'hxrpphenix:plume_de_phenix', 'chaudron:sec', 'Plume de phénix → Chaudron (à sec)', **SECRET)
P('remede_du_second_souffle', 'Remède du Second Souffle', 'seringue',
  'poudre_des_trente_quatre_plantes teinture_des_vingt_champignons essence_des_quinze_raretes baume_des_quinze_ressources cendre_doree seringue_vide',
  'balance > jarres:24 > alambic:concentrer', 'Tout réunir : Balance → Jarres (24 h) → Alambic → seringue', rang=3, **SECRET)
