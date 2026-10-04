"""
Les 52 maladies (data/virus_maladies.json du cahier des charges) converties en données structurées.

Chaque stade : (message RP de l'épisode, effets, symptômes visibles).
Effets (séparés par des espaces) :
  lenteur1 lenteur2 faiblesse1 faiblesse2 nausee cecite degats
  faim+ faim++ soif+ soif++ sprint5 sprint8 nosprint saut- nosaut recup- noregen
  coeur-1 coeur-2 coeur-4 nourriture-15 nourriture-25 lait muet
  floue paupieres rouge toux eternue tremble mains vertige lumiere acouphenes hallu tunnel
  gris ambre bleu inverse desoriente immobile3 immobile5
Préfixe « ~ » : par épisodes ; « ! » : en continu (sinon : métabolisme, sprint, saut, cœurs et nourriture
en continu, le reste par épisodes ; au stade « à vie » tout est continu sauf « ~ »).
Condition « @nuit », « @eau », « @y100 », « @combat », « @jour » : l'effet ne joue que dans ce cas.
"""

SYMPTOMES = [
    ('fievre', 'Fièvre'), ('frissons', 'Frissons'), ('sueurs', 'Sueurs'), ('toux', 'Toux'),
    ('eternuements', 'Éternuements, nez qui coule'), ('gorge', 'Mal de gorge, voix'), ('nausee', 'Nausée, vomissements'),
    ('ventre', 'Maux de ventre'), ('faim', 'Faim anormale'), ('soif', 'Soif anormale'),
    ('fatigue', 'Fatigue, paupières lourdes'), ('faiblesse', 'Faiblesse'), ('lenteur', 'Lenteur, courbatures'),
    ('tete', 'Maux de tête'), ('vision', 'Vision floue, yeux'), ('lumiere', 'Sensibilité à la lumière'),
    ('vertige', 'Vertiges'), ('tremblements', 'Tremblements, mains qui tremblent'), ('crampes', 'Crampes, raideurs'),
    ('souffle', 'Souffle court'), ('demangeaisons', 'Démangeaisons'), ('eruption', 'Boutons, éruption, plaques'),
    ('saignements', 'Saignements, bleus'), ('plaie', 'Plaie, morsure, piqûre'), ('jaunisse', 'Jaunisse, teint jaune'),
    ('paleur', 'Pâleur, anémie'), ('ganglions', 'Ganglions'), ('hallucinations', 'Hallucinations, délire'),
    ('oreilles', 'Acouphènes'), ('couleurs', 'Couleurs altérées'), ('desorientation', 'Désorientation'),
    ('paralysie', 'Paralysie, immobilité'), ('insomnie', 'Insomnie'), ('bouche', 'Goût étrange, bouche'),
]

MALADIES = []


def S(message, effets='', symptomes=''):
    return {'message': message, 'effets': effets.split(), 'symptomes': symptomes.split(), 'vie': False}


def VIE(message, effets='', symptomes=''):
    return {'message': message, 'effets': effets.split(), 'symptomes': symptomes.split(), 'vie': True}


def M(numero, id, nom, rang, touche, premier, jours, sang, stades, remede, accompagnements, prises, consignes, consigne_texte,
      traitement_texte, origine=(), contexte=()):
    MALADIES.append({
        'numero': numero, 'id': id, 'nom': nom, 'rang': rang, 'touche_uniquement': touche, 'premier_message': premier,
        'jours_avant_dernier_stade': jours, 'sang': sang, 'stades': stades, 'remede': remede,
        'accompagnements': list(accompagnements), 'prises': prises, 'consignes': consignes,
        'consigne_texte': consigne_texte, 'traitement_texte': traitement_texte,
        'origine': list(origine), 'contexte': list(contexte)})


def pr(delai, *items, moment=None, dormir=False):
    d = {'delai': delai, 'items': list(items)}
    if moment:
        d['moment'] = moment
    if dormir:
        d['dormir_apres'] = True
    return d


def C(type, **o):
    d = {'type': type}
    d.update(o)
    return d


# =================================================================================================
M(1, 'fievre_des_marais', 'Fièvre des marais', 0, None, "Vous avez des frissons alors qu'il ne fait pas froid.", 10,
  'GB↑, FE↓ · MR : bâtonnets verdâtres',
  [S('Un frisson vous parcourt l\'échine, et votre gorge se dessèche.', 'tremble soif+', 'frissons soif'),
   S('La fièvre vous monte au front ; vos jambes traînent.', 'rouge lenteur1 soif+', 'fievre lenteur soif'),
   S('Une nausée vous prend sans prévenir.', 'nausee soif+', 'nausee fievre soif'),
   S('Vous vous sentez sans force, la gorge en feu de soif.', 'faiblesse1 soif++', 'faiblesse soif fievre'),
   S('Vous êtes trempé de sueur, et votre vue se brouille.', 'floue soif++', 'sueurs vision fievre'),
   VIE('Votre gorge reste sèche, quoi que vous buviez.', 'soif+', 'soif')],
  'decoction_de_saule_et_bourrache', ['sirop_de_thym'],
  [pr(0, 'decoction_de_saule_et_bourrache', 'sirop_de_thym'), pr(3, 'decoction_de_saule_et_bourrache')],
  [C('a_jeun_avant', prises=[1], heures=1)], 'rester à jeun avant la prise 1',
  'Prise 1 : Décoction + Sirop · Prise 2 (+3 h) : Décoction', contexte=['marais'])

M(2, 'angine_rouge', 'Angine rouge', 0, None, 'Votre gorge vous brûle quand vous avalez.', 8,
  'GB↑↑ · MR : chaînettes de grains ronds',
  [S('Avaler votre salive vous arrache une grimace : la gorge est en feu.', '', 'gorge'),
   S('Une quinte de toux vous plie en deux.', 'toux', 'toux gorge'),
   S('La fièvre vous alourdit les membres.', 'rouge lenteur1', 'fievre lenteur gorge'),
   S('Votre voix s\'éteint dans un souffle rauque ; vous êtes sans force.', 'faiblesse1', 'gorge faiblesse'),
   VIE('Une toux sèche vous reprend, et vous manquez vite de souffle.', '~toux sprint5', 'toux souffle')],
  'sirop_de_guimauve_a_la_propolis', ['gargarisme_de_sauge'],
  [pr(0, 'sirop_de_guimauve_a_la_propolis', 'gargarisme_de_sauge'), pr(2, 'sirop_de_guimauve_a_la_propolis', 'gargarisme_de_sauge'),
   pr(2, 'sirop_de_guimauve_a_la_propolis')],
  [C('au_chaud')], "rester au chaud (intérieur ou près d'un feu)",
  'Prise 1 : Sirop + Gargarisme · Prise 2 (+2 h) : Sirop + Gargarisme · Prise 3 (+2 h) : Sirop', contexte=['froid'])

M(3, 'septicemie', 'Septicémie', 2, None, 'Une chaleur anormale vous monte dans tout le corps.', 9,
  'GB↑↑, PL↓, TX+ · MR : nuée de grains dorés',
  [S('Une fièvre brutale vous empourpre le regard.', 'rouge', 'fievre'),
   S('Vous grelottez, les jambes lourdes.', 'tremble lenteur1', 'frissons lenteur'),
   S('Vos bras ne répondent presque plus.', 'faiblesse2', 'faiblesse fievre'),
   S('Le monde tangue autour de vous.', 'vertige', 'vertige fievre'),
   S('Vos plaies refusent de se refermer.', 'recup-', 'fievre faiblesse'),
   S('Votre champ de vision se rétrécit en un tunnel noir.', 'tunnel', 'vision fievre'),
   VIE('Votre cœur bat plus faiblement qu\'avant.', 'coeur-4', 'faiblesse')],
  'injection_de_chaga_et_d_echinacee', ['cataplasme_d_ail', 'pilules_de_reishi'],
  [pr(0, 'injection_de_chaga_et_d_echinacee', 'cataplasme_d_ail'), pr(3, 'pilules_de_reishi'), pr(3, 'injection_de_chaga_et_d_echinacee')],
  [C('repos')], 'repos complet : ni sprint ni combat pendant toute la cure',
  'Prise 1 : Injection + Cataplasme · Prise 2 (+3 h) : Pilules · Prise 3 (+3 h) : Injection', contexte=['combat'])

M(4, 'toux_des_mineurs', 'Toux des mineurs', 1, None, 'Une poussière semble coincée au fond de vos poumons.', 12,
  'GB↑, GR↓ · MR : grains de poussière enrobés',
  [S('Une toux sèche vous râpe la poitrine.', '', 'toux'),
   S('Une quinte de toux vous force à vous arrêter.', 'toux', 'toux'),
   S('Vous manquez d\'air au moindre effort.', 'sprint5', 'souffle toux'),
   S('Vos mains tremblent, vous peinez à viser.', 'mains', 'tremblements toux'),
   S('Vous crachez une glaire noire ; vos forces s\'en vont.', 'toux faiblesse1', 'toux faiblesse'),
   VIE('Vos poumons ne se sont jamais remis de la poussière.', 'sprint5', 'souffle')],
  'sirop_de_lin_et_de_mousse_des_grottes', ['eau_florale_de_lavande'],
  [pr(0, 'eau_florale_de_lavande'), pr(1, 'sirop_de_lin_et_de_mousse_des_grottes'),
   pr(4, 'eau_florale_de_lavande', 'sirop_de_lin_et_de_mousse_des_grottes')],
  [C('y_min', y=50)], 'ne pas descendre sous Y 50',
  'Prise 1 : Eau florale · Prise 2 (+1 h) : Sirop · Prise 3 (+4 h) : Eau florale + Sirop', contexte=['grotte'])

M(5, 'fievre_du_lait', 'Fièvre du lait', 0, None, 'Votre ventre gargouille et se tord.', 9,
  'GB↑, SU↓',
  [S('Votre ventre est lourd comme une pierre.', '', 'ventre'),
   S('Une nausée vous soulève l\'estomac.', 'nausee', 'nausee ventre'),
   S('Vous avez faim sans arrêt, pourtant rien ne passe.', 'faim+', 'faim ventre'),
   S('Une faiblesse vous gagne les bras.', 'faiblesse1', 'faiblesse ventre'),
   VIE('Rien qu\'à l\'odeur du lait, votre estomac se retourne.', 'lait', 'nausee')],
  'pilules_de_coquille_et_de_fenouil', ['infusion_de_menthe'],
  [pr(0, 'pilules_de_coquille_et_de_fenouil'), pr(2, 'infusion_de_menthe'), pr(1, 'infusion_de_menthe')],
  [C('a_jeun_entre', de=1, a=3)], 'ne rien manger entre la prise 1 et la prise 3',
  'Prise 1 : Pilules · Prise 2 (+2 h) : Infusion · Prise 3 (+1 h) : Infusion', origine=['alimentaire', 'lait'])

M(6, 'mal_des_tanneurs', 'Mal des tanneurs', 1, None, 'Votre peau vous démange sans arrêt.', 14,
  'GB↑, PL↑ · MR : bâtonnets épais à spores',
  [S('Vous vous grattez les avant-bras jusqu\'au sang.', '', 'demangeaisons'),
   S('Des plaques rouges s\'étendent sur votre peau.', '', 'eruption demangeaisons'),
   S('La fièvre s\'installe ; vous traînez des pieds.', 'rouge lenteur1', 'fievre lenteur eruption'),
   S('Un ulcère s\'ouvre et vous lance.', 'degats', 'plaie eruption'),
   S('Vous vous sentez faible et fiévreux.', 'faiblesse1', 'faiblesse fievre'),
   VIE('Le mal vous a laissé moins robuste.', 'coeur-2', 'faiblesse')],
  'onguent_de_bardane_et_millepertuis', ['decoction_d_ortie'],
  [pr(0, 'onguent_de_bardane_et_millepertuis'), pr(3, 'decoction_d_ortie'), pr(3, 'onguent_de_bardane_et_millepertuis', 'decoction_d_ortie')],
  [C('pas_mouiller')], 'ne pas se mouiller (ni pluie ni eau)',
  'Prise 1 : Onguent · Prise 2 (+3 h) : Décoction · Prise 3 (+3 h) : Onguent + Décoction', contexte=['plaines'])

M(7, 'bouche_de_pierre', 'Bouche-de-pierre', 2, None, 'Votre mâchoire se crispe toute seule.', 10,
  'TX+ · MR : bâtonnets en baguette de tambour',
  [S('Votre mâchoire se serre malgré vous, vous parlez entre vos dents.', '', 'crampes bouche'),
   S('Une crampe vous noue les mollets.', 'lenteur1', 'crampes lenteur'),
   S('Un spasme vous cloue les jambes au sol.', '~nosaut', 'crampes paralysie'),
   S('Tout votre corps est secoué de tremblements.', 'tremble', 'tremblements crampes'),
   S('Vos muscles se contractent à vous faire mal.', 'lenteur2', 'crampes lenteur'),
   S('Votre corps est raide comme une planche, impossible de courir.', 'nosprint', 'crampes'),
   VIE('Vos muscles ne se sont jamais tout à fait détendus.', 'saut- lenteur1', 'crampes lenteur')],
  'injection_de_criniere_de_lion', ['infusion_de_melisse'],
  [pr(0, 'injection_de_criniere_de_lion', 'infusion_de_melisse', dormir=True), pr(3, 'injection_de_criniere_de_lion', dormir=True)],
  [C('dormir_apres_chaque_prise')], 'dormir dans un lit après chaque prise',
  'Prise 1 : Injection + Infusion, puis dormir dans un lit · Prise 2 (+3 h) : Injection, puis dormir', contexte=['combat'])

M(8, 'peste_grise', 'Peste grise', 3, None, 'Vous vous sentez soudain vidé de vos forces.', 9,
  'GB↑↑, PL↓, TX+ · MR : bacilles en épingle grise',
  [S('Vos paupières tombent toutes seules, une fatigue de plomb.', 'paupieres', 'fatigue'),
   S('La fièvre vous brûle.', 'rouge', 'fievre'),
   S('Des boules douloureuses gonflent à votre cou ; vous traînez.', 'lenteur1', 'ganglions lenteur'),
   S('Des taches grises marbrent votre peau.', '', 'eruption'),
   S('Vos bras pendent, sans force.', 'faiblesse2', 'faiblesse'),
   S('Les couleurs du monde se fanent en gris.', 'gris', 'couleurs'),
   S('Le monde se réduit à un tunnel sombre.', 'tunnel', 'vision'),
   VIE('Le monde est resté gris, et votre cœur affaibli.', 'gris coeur-4', 'couleurs faiblesse')],
  'elixir_du_lotus_de_l_aube', ['decoction_d_echinacee', 'pilules_de_cordyceps'],
  [pr(0, 'decoction_d_echinacee', 'pilules_de_cordyceps'), pr(2, 'elixir_du_lotus_de_l_aube'),
   pr(4, 'decoction_d_echinacee', 'pilules_de_cordyceps'), pr(2, 'elixir_du_lotus_de_l_aube')],
  [C('a_jeun_avant', prises=[1, 3], heures=1)], 'à jeun avant la prise 1 et la prise 3',
  'Prise 1 : Décoction + Pilules · Prise 2 (+2 h) : Élixir · Prise 3 (+4 h) : Décoction + Pilules · Prise 4 (+2 h) : Élixir')

M(9, 'rouille_des_plaies', 'Rouille des plaies', 0, None, 'Une vieille égratignure vous lance et rougit.', 7,
  'GB↑',
  [S('La vieille plaie est rouge et chaude sous vos doigts.', '', 'plaie'),
   S('La plaie suinte un liquide jaunâtre.', '', 'plaie'),
   S('Une petite fièvre vous gagne.', 'rouge', 'fievre plaie'),
   S('Vous vous sentez sans force.', 'faiblesse1', 'faiblesse plaie'),
   VIE('Vos blessures mettent depuis plus longtemps à guérir.', 'recup-', 'faiblesse')],
  'cataplasme_de_plantain', ['teinture_de_propolis'],
  [pr(0, 'cataplasme_de_plantain'), pr(1, 'teinture_de_propolis'), pr(2, 'cataplasme_de_plantain')],
  [], 'aucune', 'Prise 1 : Cataplasme · Prise 2 (+1 h) : Teinture · Prise 3 (+2 h) : Cataplasme', contexte=['combat'])

M(10, 'rhume_des_plaines', 'Rhume des plaines', 0, None, 'Votre nez coule et vous éternuez.', 6,
  'GB↑',
  [S('Votre nez est bouché, vous respirez par la bouche.', '', 'eternuements'),
   S('Un éternuement vous secoue tout entier.', 'eternue', 'eternuements'),
   S('Vos paupières sont lourdes, vous bâillez sans arrêt.', 'paupieres', 'fatigue eternuements'),
   S('Ce rhume vous vide de vos forces.', 'faiblesse1', 'faiblesse eternuements'),
   VIE('Vous éternuez encore, souvent et sans raison.', '~eternue', 'eternuements')],
  'sirop_de_sureau', ['infusion_de_camomille_et_thym'],
  [pr(0, 'sirop_de_sureau', 'infusion_de_camomille_et_thym'), pr(2, 'sirop_de_sureau', 'infusion_de_camomille_et_thym')],
  [C('au_chaud')], 'rester au chaud', 'Prise 1 : Sirop + Infusion · Prise 2 (+2 h) : Sirop + Infusion', contexte=['plaines', 'froid'])

M(11, 'grippe_des_hauts_plateaux', 'Grippe des hauts plateaux', 1, None, 'Vos jambes sont lourdes, comme après une longue course.', 10,
  'GB↓, PL↓ · MR : grains en couronne',
  [S('Des courbatures vous scient les jambes.', 'lenteur1', 'lenteur'),
   S('La fièvre vous rougit le regard.', 'rouge', 'fievre'),
   S('Vous claquez des dents, les bras sans force.', 'tremble faiblesse1', 'frissons faiblesse'),
   S('Une toux grasse vous secoue.', 'toux', 'toux'),
   S('Vous n\'avez qu\'une envie : vous allonger.', 'lenteur2', 'lenteur fatigue'),
   VIE('Cette grippe vous a laissé moins résistant.', 'coeur-2', 'faiblesse')],
  'decoction_de_shiitake_et_d_angelique', ['sirop_de_thym'],
  [pr(0, 'decoction_de_shiitake_et_d_angelique', 'sirop_de_thym', dormir=True), pr(3, 'decoction_de_shiitake_et_d_angelique')],
  [C('a_jeun_avant', prises=[1], heures=1), C('dormir_entre', de=1, a=2)],
  'à jeun avant la prise 1, et dormir dans un lit entre les deux prises',
  'Prise 1 : Décoction + Sirop, puis dormir dans un lit · Prise 2 (+3 h) : Décoction', contexte=['altitude'])

M(12, 'fievre_a_pustules', 'Fièvre à pustules', 1, None, 'Des petits boutons apparaissent sur vos bras.', 12,
  'GB↓, PL↓',
  [S('De nouveaux petits boutons vous couvrent les bras.', '', 'eruption'),
   S('La fièvre vous monte au visage.', 'rouge', 'fievre eruption'),
   S('Les boutons vous démangent à vous en arracher la peau.', 'degats', 'demangeaisons eruption'),
   S('Vos égratignures ne cicatrisent plus.', 'recup-', 'eruption'),
   S('Vous vous sentez sans force.', 'faiblesse1', 'faiblesse eruption'),
   VIE('Les marques sur votre peau ne partiront plus.', 'coeur-1', 'eruption')],
  'onguent_d_aloe_et_de_prele', ['infusion_de_pissenlit'],
  [pr(0, 'onguent_d_aloe_et_de_prele'), pr(2, 'onguent_d_aloe_et_de_prele'), pr(2, 'onguent_d_aloe_et_de_prele', 'infusion_de_pissenlit')],
  [C('pas_mouiller')], 'ne pas se mouiller',
  'Prise 1 : Onguent · Prise 2 (+2 h) : Onguent · Prise 3 (+2 h) : Onguent + Infusion')

M(13, 'fievre_jaune_des_jungles', 'Fièvre jaune des jungles', 1, None, 'Vous transpirez à grosses gouttes, puis vous grelottez.', 14,
  'GB↓, PL↓, FE↑ · MR : sphères jaunâtres',
  [S('Vous suez à grosses gouttes, puis vous claquez des dents.', 'tremble', 'sueurs frissons'),
   S('Une vague de fièvre vous écrase.', 'rouge lenteur1', 'fievre lenteur'),
   S('Votre gorge réclame de l\'eau sans répit.', 'soif++', 'soif'),
   S('Le blanc de vos yeux a jauni ; vous êtes sans force.', 'faiblesse1', 'jaunisse faiblesse'),
   S('Une nausée vous retourne l\'estomac.', 'nausee', 'nausee'),
   S('Votre nez saigne sans raison.', 'degats', 'saignements'),
   VIE('Des vagues de fièvre vous reprennent de temps à autre.', '~lenteur1 ~rouge', 'fievre')],
  'pilules_de_pollen_dore', ['decoction_de_reglisse', 'teinture_de_pissenlit'],
  [pr(0, 'decoction_de_reglisse', 'teinture_de_pissenlit'), pr(2, 'pilules_de_pollen_dore'), pr(3, 'pilules_de_pollen_dore')],
  [C('a_jeun_entre', de=1, a=2)], 'à jeun entre la prise 1 et la prise 2',
  'Prise 1 : Décoction + Teinture · Prise 2 (+2 h) : Pilules · Prise 3 (+3 h) : Pilules', contexte=['jungle'])

M(14, 'oeil_rouge', 'Œil rouge', 0, None, 'Vos yeux piquent et pleurent tout seuls.', 5,
  "rien d'anormal",
  [S('Vos yeux piquent, vous les frottez sans cesse.', '', 'vision'),
   S('Votre vue se trouble de larmes.', 'floue', 'vision'),
   S('Vos yeux sont collés, vous peinez à les ouvrir.', '', 'vision'),
   S('La lumière du jour vous éblouit douloureusement.', 'lumiere@jour', 'lumiere vision'),
   VIE('Votre vue se trouble encore, de temps à autre.', '~floue', 'vision')],
  'eau_florale_de_camomille', ['compresse_de_the'],
  [pr(0, 'eau_florale_de_camomille', 'compresse_de_the'), pr(1, 'eau_florale_de_camomille', 'compresse_de_the'),
   pr(1, 'eau_florale_de_camomille', 'compresse_de_the')],
  [C('a_l_ombre')], "rester à l'ombre ou à l'intérieur",
  'Prise 1 : Eau florale + Compresse · Prise 2 (+1 h) : idem · Prise 3 (+1 h) : idem', contexte=['desert'])

M(15, 'fievre_des_ruisseaux', 'Fièvre des ruisseaux', 0, None, 'Votre ventre se noue d\'un coup.', 6,
  'SU↓',
  [S('Votre ventre se noue, vous vous pliez en deux.', '', 'ventre'),
   S('Une nausée vous prend à la gorge.', 'nausee', 'nausee ventre'),
   S('Vous mourez de soif.', 'soif++', 'soif'),
   S('Votre estomac crie famine, vos bras sont mous.', 'faim+ faiblesse1', 'faim faiblesse'),
   VIE('Votre estomac réclame plus souvent qu\'avant.', 'faim+', 'faim')],
  'pilules_de_charbon_de_bambou', ['eau_de_riz_salee'],
  [pr(0, 'pilules_de_charbon_de_bambou'), pr(2, 'eau_de_riz_salee'), pr(2, 'eau_de_riz_salee')],
  [C('sans_nourriture')], 'aucune nourriture pendant toute la cure',
  'Prise 1 : Pilules · Prise 2 (+2 h) : Eau de riz · Prise 3 (+2 h) : Eau de riz', origine=['alimentaire', 'eau'], contexte=['eau'])

M(16, 'rougeole_des_steppes', 'Rougeole des steppes', 1, 'Groupes A', 'Une fièvre vous prend et vos yeux deviennent sensibles.', 10,
  'GB↓ · MR : cellules géantes à plusieurs noyaux',
  [S('La fièvre vous brûle le front.', 'rouge', 'fievre'),
   S('Une toux vous secoue.', 'toux', 'toux'),
   S('De petits points blancs piquettent l\'intérieur de vos joues.', '', 'bouche'),
   S('Une éruption rouge vous couvre ; vous êtes sans force.', 'faiblesse1', 'eruption faiblesse'),
   S('La lumière vous fait cligner des yeux.', 'lumiere@jour', 'lumiere'),
   VIE('La rougeole vous a laissé plus fragile.', 'coeur-2', 'faiblesse')],
  'infusion_d_achillee', ['onguent_de_souci', 'sirop_de_propolis'],
  [pr(0, 'infusion_d_achillee', 'onguent_de_souci', dormir=True), pr(3, 'sirop_de_propolis')],
  [C('dormir_entre', de=1, a=2)], 'dormir dans un lit entre les deux prises',
  'Prise 1 : Infusion + Onguent · Prise 2 (+3 h) : Sirop', contexte=['plaines'])

M(17, 'langueur_grise', 'Langueur grise', 1, None, 'Vous bâillez sans cesse, épuisé sans raison.', 16,
  'GB↑ · MR : cellules en dentelle',
  [S('Une fatigue sans fond vous engourdit.', 'paupieres', 'fatigue'),
   S('Vos paupières s\'abaissent toutes seules.', 'paupieres', 'fatigue'),
   S('Chaque pas vous coûte.', 'lenteur1', 'lenteur fatigue'),
   S('Vous n\'avez plus de force dans les bras.', 'faiblesse1', 'faiblesse fatigue'),
   S('Votre cou est gonflé, vous avancez à peine.', 'lenteur2', 'ganglions lenteur'),
   VIE('Vous avez faim bien plus souvent qu\'avant, sans endurance.', 'faim++', 'faim')],
  'decoction_de_ginseng_et_maitake', ['teinture_de_melisse'],
  [pr(0, 'decoction_de_ginseng_et_maitake', 'teinture_de_melisse', dormir=True), pr(4, 'decoction_de_ginseng_et_maitake', dormir=True),
   pr(4, 'decoction_de_ginseng_et_maitake', 'teinture_de_melisse', dormir=True)],
  [C('dormir_apres_chaque_prise')], 'dormir dans un lit après chaque prise',
  'Prise 1 : Décoction + Teinture · Prise 2 (+4 h) : Décoction · Prise 3 (+4 h) : Décoction + Teinture')

M(18, 'fievre_de_meteor', 'Fièvre de Meteor', 3, None, 'Un goût de fer vous emplit la bouche.', 8,
  'GB↓, GR↓, PL↓ · MR : filaments en crosse',
  [S('La fièvre vous empourpre le front.', 'rouge', 'fievre bouche'),
   S('Votre tête cogne, votre vue se brouille.', 'floue', 'tete vision'),
   S('Des bleus apparaissent sur votre corps sans que vous vous soyez cogné.', '', 'saignements'),
   S('Vous saignez du nez et des gencives.', 'degats', 'saignements'),
   S('Vos forces vous abandonnent.', 'faiblesse2', 'faiblesse'),
   S('Vos blessures ne se referment presque plus.', 'recup-', 'saignements faiblesse'),
   S('Gauche et droite se confondent, vous ne savez plus où aller.', 'desoriente', 'desorientation'),
   VIE('Votre corps cicatrise mal et votre cœur s\'est affaibli.', 'recup- coeur-4', 'faiblesse')],
  'elixir_du_chardon_de_meteor', ['pilules_de_bourse_a_pasteur', 'eau_florale_de_lys_des_nuees'],
  [pr(0, 'elixir_du_chardon_de_meteor', 'pilules_de_bourse_a_pasteur'), pr(2, 'eau_florale_de_lys_des_nuees'), pr(3, 'elixir_du_chardon_de_meteor')],
  [C('pas_de_viande'), C('distance_max', blocs=20)], "pas de viande, et ne pas s'éloigner de plus de 20 blocs entre deux prises",
  'Prise 1 : Élixir + Pilules · Prise 2 (+2 h) : Eau florale · Prise 3 (+3 h) : Élixir', contexte=['aride'])

M(19, 'ver_des_rivieres', 'Ver des rivières', 0, None, 'Vous avez faim alors que vous venez de manger.', 12,
  'GR↓, FE↓, PA+',
  [S('Votre ventre est gonflé et dur.', '', 'ventre'),
   S('Vous avez faim, encore faim.', 'faim+', 'faim'),
   S('Une faim dévorante vous tenaille.', 'faim++', 'faim'),
   S('Une faiblesse vous coupe les bras.', 'faiblesse1', 'faiblesse faim'),
   S('Vous êtes pâle et vos jambes traînent.', 'lenteur1', 'paleur lenteur'),
   VIE('Le ver est parti, mais votre faim ne vous lâche plus.', 'faim+', 'faim')],
  'pilules_de_gentiane_et_d_ail', ['decoction_de_fenouil'],
  [pr(0, 'pilules_de_gentiane_et_d_ail'), pr(2, 'decoction_de_fenouil'), pr(6, 'pilules_de_gentiane_et_d_ail', 'decoction_de_fenouil')],
  [C('a_jeun_avant', prises=[1], heures=1), C('a_jeun_apres', prises=[1, 3], heures=2)],
  'à jeun avant la prise 1, et rien manger 2 h après chaque prise de pilules',
  'Prise 1 : Pilules · Prise 2 (+2 h) : Décoction · Prise 3 (+6 h) : Pilules + Décoction', origine=['alimentaire', 'poisson', 'eau'],
  contexte=['eau'])

M(20, 'gale_des_greniers', 'Gale des greniers', 0, None, 'Ça vous gratte entre les doigts, surtout la nuit.', 9,
  'GB↑ · MR : sillons d\'acariens',
  [S('Une démangeaison vous réveille, entre les doigts.', '', 'demangeaisons'),
   S('Vous ne pouvez plus vous empêcher de vous gratter.', '', 'demangeaisons'),
   S('Vous vous grattez jusqu\'au sang.', 'degats', 'demangeaisons plaie'),
   S('Vous n\'avez pas fermé l\'œil, vos paupières pèsent.', 'paupieres', 'fatigue insomnie'),
   VIE('Les nuits, les démangeaisons reviennent vous écorcher.', '~degats@nuit', 'demangeaisons')],
  'onguent_de_lavande_au_saindoux', ['lotion_de_vinaigre_de_riz'],
  [pr(0, 'lotion_de_vinaigre_de_riz'), pr(1, 'onguent_de_lavande_au_saindoux'), pr(3, 'lotion_de_vinaigre_de_riz', 'onguent_de_lavande_au_saindoux')],
  [C('pas_mouiller')], 'ne pas se mouiller',
  'Prise 1 : Lotion · Prise 2 (+1 h) : Onguent · Prise 3 (+3 h) : Lotion + Onguent', contexte=['nuit'])

M(21, 'fievre_tierce_des_marais', 'Fièvre tierce des marais', 1, 'Tous sauf les groupes O', 'Une vague de chaleur vous traverse, puis disparaît.', 14,
  'GR↓, FE↓, PA+ · MR : anneaux dans les globules',
  [S('Un frisson vous parcourt de la tête aux pieds.', 'tremble', 'frissons'),
   S('Un accès de fièvre vous prend, vos jambes ralentissent.', 'rouge lenteur1', 'fievre lenteur'),
   S('Vous suez, la gorge sèche comme du sable.', 'soif++', 'sueurs soif'),
   S('Vous êtes pâle et sans force.', 'faiblesse1', 'paleur faiblesse'),
   S('L\'accès de fièvre vous écrase les jambes.', 'rouge lenteur2', 'fievre lenteur'),
   S('Vous entendez des pas derrière vous… il n\'y a personne.', 'hallu', 'hallucinations'),
   VIE('Les accès de fièvre reviennent, réguliers comme une horloge.', '~rouge ~lenteur1', 'fievre')],
  'teinture_de_mousse_de_numere', ['decoction_de_saule', 'sirop_de_reglisse'],
  [pr(0, 'teinture_de_mousse_de_numere', 'decoction_de_saule'), pr(3, 'teinture_de_mousse_de_numere', 'decoction_de_saule'),
   pr(3, 'teinture_de_mousse_de_numere', 'sirop_de_reglisse')],
  [C('dormir_apres_chaque_prise'), C('pas_mouiller')], "dormir dans un lit, à l'abri de la pluie",
  'Prise 1 : Teinture + Décoction · Prise 2 (+3 h) : Teinture + Décoction · Prise 3 (+3 h) : Teinture + Sirop', contexte=['marais'])

M(22, 'douve_du_foie', 'Douve du foie', 1, None, 'Vous avez un goût amer dans la bouche après chaque repas.', 15,
  'GB↑, SU↓, PA+ · MR : œufs operculés',
  [S('Votre ventre est sensible au moindre geste.', '', 'ventre'),
   S('Votre repas vous remonte, la nausée vous prend.', 'nausee', 'nausee bouche'),
   S('Votre teint vire au jaune.', '', 'jaunisse'),
   S('Vos forces vous quittent.', 'faiblesse1', 'faiblesse'),
   S('Votre flanc droit est gonflé ; vous avancez péniblement.', 'lenteur1', 'ventre lenteur'),
   VIE('Votre foie abîmé ne tire plus tout ce que vous mangez.', 'nourriture-25', 'faim')],
  'teinture_d_artichaut_et_de_pissenlit', ['pilules_de_gentiane'],
  [pr(0, 'teinture_d_artichaut_et_de_pissenlit', 'pilules_de_gentiane'), pr(3, 'teinture_d_artichaut_et_de_pissenlit')],
  [C('a_jeun_avant', prises=[1], heures=1), C('pas_de_poisson')], "à jeun avant la prise 1, et pas de poisson jusqu'à la fin de la cure",
  'Prise 1 : Teinture + Pilules · Prise 2 (+3 h) : Teinture', origine=['alimentaire', 'poisson'])

M(23, 'sangsue_fantome', 'Sangsue-fantôme', 2, None, 'Vous trouvez une petite plaie que vous ne vous rappelez pas.', 10,
  'GR↓, FE↓, PA+ · MR : crochets noirs',
  [S('Une petite plaie, nette, que vous ne vous expliquez pas.', '', 'plaie'),
   S('Une fatigue vous tombe dessus.', 'paupieres', 'fatigue'),
   S('Vous êtes blême ; vos jambes ralentissent.', 'lenteur1', 'paleur lenteur'),
   S('Votre sang semble s\'être vidé, vos bras sont mous.', 'faiblesse1', 'paleur faiblesse'),
   S('La tête vous tourne.', 'vertige', 'vertige'),
   S('Vos plaies se referment à peine.', 'recup-', 'paleur'),
   VIE('Il vous manque du sang, pour toujours.', 'coeur-4', 'paleur')],
  'sirop_de_fer_sang', ['cataplasme_d_algue_noire'],
  [pr(0, 'cataplasme_d_algue_noire'), pr(1, 'sirop_de_fer_sang'), pr(3, 'sirop_de_fer_sang')],
  [C('pas_d_eau')], "ne pas entrer dans l'eau pendant la cure",
  'Prise 1 : Cataplasme · Prise 2 (+1 h) : Sirop · Prise 3 (+3 h) : Sirop', contexte=['eau', 'marais'])

M(24, 'ver_cerebral', 'Ver cérébral', 3, None, 'Une migraine vous serre le crâne comme un étau.', 18,
  'GB↑, PA+ · MR : kystes blancs',
  [S('Votre crâne est pris dans un étau.', 'floue', 'tete'),
   S('Le sol se dérobe sous vous.', 'vertige', 'vertige tete'),
   S('Une silhouette vous suit du coin de l\'œil… puis disparaît.', 'hallu', 'hallucinations'),
   S('Un sifflement aigu couvre tous les bruits.', 'acouphenes', 'oreilles'),
   S('Vous perdez l\'équilibre, vos sauts sont maladroits.', 'saut-', 'vertige'),
   S('Une crise vous fige sur place.', 'immobile3', 'paralysie'),
   S('Gauche et droite s\'inversent dans votre tête.', 'desoriente', 'desorientation'),
   VIE('Le sifflement est resté, et parfois tout s\'inverse.', 'acouphenes ~desoriente', 'oreilles desorientation')],
  'pilules_de_mousse_des_abysses', ['decoction_de_ginkgo_et_valeriane', 'eau_florale_de_lys_des_nuees'],
  [pr(0, 'pilules_de_mousse_des_abysses', 'decoction_de_ginkgo_et_valeriane'), pr(2, 'eau_florale_de_lys_des_nuees'),
   pr(4, 'pilules_de_mousse_des_abysses', 'decoction_de_ginkgo_et_valeriane'), pr(2, 'eau_florale_de_lys_des_nuees', dormir=True)],
  [C('pas_d_alcool')], "pas d'alcool pendant la cure",
  'Prise 1 : Pilules + Décoction · Prise 2 (+2 h) : Eau florale · Prise 3 (+4 h) : Pilules + Décoction · Prise 4 (+2 h) : Eau florale, puis dormir',
  origine=['alimentaire'])

M(25, 'puces_de_la_fievre', 'Puces de la fièvre', 0, None, 'De petites piqûres rouges couvrent vos chevilles.', 8,
  'GB↑ · MR : petites bactéries en virgule',
  [S('Vos chevilles piquées vous démangent.', '', 'demangeaisons plaie'),
   S('La fièvre vous empourpre.', 'rouge', 'fievre'),
   S('Votre tête cogne, vous avancez plus lentement.', 'lenteur1', 'tete lenteur'),
   S('Une éruption vous couvre ; vous êtes sans force.', 'faiblesse1', 'eruption faiblesse'),
   VIE('Des fièvres passagères vous reprennent sans prévenir.', '~lenteur1 ~rouge', 'fievre')],
  'baume_de_romarin_au_polypore', ['fumigation_de_plumes'],
  [pr(0, 'fumigation_de_plumes'), pr(1, 'baume_de_romarin_au_polypore'), pr(2, 'baume_de_romarin_au_polypore')],
  [], 'aucune', 'Prise 1 : Fumigation · Prise 2 (+1 h) : Baume · Prise 3 (+2 h) : Baume', contexte=['plaines'])

M(26, 'pied_de_marais', 'Pied de marais', 0, None, 'Vos orteils vous démangent dans vos bottes.', 10,
  "rien d'anormal · MR : filaments en réseau",
  [S('Vos orteils vous démangent atrocement.', '', 'demangeaisons'),
   S('La peau de vos pieds pèle par lambeaux.', '', 'eruption'),
   S('Des crevasses vous font boiter.', 'lenteur1', 'lenteur plaie'),
   S('Chaque pas vous fait mal, vous ne pouvez pas courir longtemps.', 'sprint5', 'lenteur'),
   VIE('Dans l\'eau, vos pieds vous font souffrir.', 'lenteur1@eau', 'lenteur')],
  'huile_d_ecorce_de_bouleau', ['poudre_de_sauge'],
  [pr(0, 'huile_d_ecorce_de_bouleau'), pr(1, 'poudre_de_sauge'), pr(2, 'huile_d_ecorce_de_bouleau', 'poudre_de_sauge')],
  [C('pas_mouiller')], 'ne pas se mouiller',
  'Prise 1 : Huile · Prise 2 (+1 h) : Poudre · Prise 3 (+2 h) : Huile + Poudre', contexte=['marais', 'eau'])

M(27, 'teigne_des_bergers', 'Teigne des bergers', 0, None, 'Une tache ronde et rouge est apparue sur votre bras.', 12,
  "rien d'anormal · MR : spores en mosaïque",
  [S('La tache ronde sur votre bras a grandi.', '', 'eruption'),
   S('Ça vous démange sans relâche.', '', 'demangeaisons'),
   S('Les plaques s\'étendent sur votre peau.', '', 'eruption'),
   S('Vos cheveux tombent par poignées.', '', 'eruption'),
   VIE('Les plaques sont restées, et vous démangent encore.', '', 'eruption demangeaisons')],
  'onguent_de_vesse_de_loup', ['vinaigre_de_sauge'],
  [pr(0, 'onguent_de_vesse_de_loup', 'vinaigre_de_sauge'), pr(2, 'onguent_de_vesse_de_loup', 'vinaigre_de_sauge'),
   pr(2, 'onguent_de_vesse_de_loup', 'vinaigre_de_sauge')],
  [], 'aucune', 'Prise 1 : Onguent + Vinaigre · Prise 2 (+2 h) : idem · Prise 3 (+2 h) : idem', contexte=['plaines'])

M(28, 'muguet_blanc', 'Muguet blanc', 0, None, 'Vous avez un drôle de goût dans la bouche.', 7,
  'SU↑ · MR : levures bourgeonnantes',
  [S('Un goût de moisi vous reste en bouche.', '', 'bouche'),
   S('Des plaques blanches tapissent votre langue.', '', 'bouche'),
   S('Manger vous fait mal ; vous avalez à peine.', 'nourriture-25', 'bouche faim'),
   S('Vous êtes sans force.', 'faiblesse1', 'faiblesse'),
   VIE('Votre bouche abîmée ne profite plus vraiment des repas.', 'nourriture-15', 'bouche')],
  'gargarisme_de_citron_et_thym', ['infusion_de_sauge_au_miel'],
  [pr(0, 'gargarisme_de_citron_et_thym'), pr(1, 'infusion_de_sauge_au_miel'), pr(1, 'gargarisme_de_citron_et_thym')],
  [], 'aucune', 'Prise 1 : Gargarisme · Prise 2 (+1 h) : Infusion · Prise 3 (+1 h) : Gargarisme', origine=['alimentaire'])

M(29, 'poumon_de_spores', 'Poumon-de-spores', 2, None, 'Vous respirez comme à travers un tissu humide.', 14,
  'GB↑ · MR : filaments ramifiés',
  [S('Une petite toux vous gratte la gorge.', '', 'toux'),
   S('Une quinte vous arrête net.', 'toux', 'toux'),
   S('Vous manquez d\'air dès que vous courez.', 'sprint5', 'souffle'),
   S('Vos mains tremblent.', 'mains', 'tremblements'),
   S('Fiévreux, vous vous traînez.', 'rouge faiblesse1', 'fievre faiblesse'),
   S('Vous crachez du sang.', 'toux degats', 'toux saignements'),
   VIE('Vos poumons ne vous laissent plus courir.', 'nosprint', 'souffle')],
  'decoction_de_reishi', ['eau_florale_de_thym', 'sirop_de_reglisse'],
  [pr(0, 'decoction_de_reishi', 'eau_florale_de_thym'), pr(3, 'sirop_de_reglisse'), pr(3, 'decoction_de_reishi', 'eau_florale_de_thym')],
  [C('pas_de_grotte')], 'ne pas entrer dans une grotte pendant la cure',
  'Prise 1 : Décoction + Eau florale · Prise 2 (+3 h) : Sirop · Prise 3 (+3 h) : Décoction + Eau florale', contexte=['grotte', 'foret'])

M(30, 'ongle_noir', 'Ongle-noir', 1, None, 'Un de vos ongles a noirci sans raison.', 15,
  'GB↑ · MR : spores noires en chapelet',
  [S('Un de vos ongles est terne et cassant.', '', 'eruption'),
   S('Vos ongles noircissent les uns après les autres.', '', 'eruption'),
   S('Tenir un outil vous fait mal.', 'faiblesse1', 'crampes'),
   S('Vos mains tremblent.', 'mains', 'tremblements'),
   S('Vos doigts gonflés vous élancent.', 'degats', 'crampes'),
   VIE('Vos mains tremblent pour toujours.', 'mains', 'tremblements')],
  'teinture_de_coprin', ['onguent_de_lavande_au_saindoux'],
  [pr(0, 'teinture_de_coprin', 'onguent_de_lavande_au_saindoux'), pr(2, 'onguent_de_lavande_au_saindoux'),
   pr(2, 'teinture_de_coprin', 'onguent_de_lavande_au_saindoux')],
  [], 'aucune', 'Prise 1 : Teinture + Onguent · Prise 2 (+2 h) : Onguent · Prise 3 (+2 h) : Teinture + Onguent', contexte=['grotte'])

M(31, 'lepre_des_racines', 'Lèpre des racines', 2, 'Groupes O', 'Votre peau durcit par endroits, comme de l\'écorce.', 20,
  'GB↑, FE↓ · MR : filaments ligneux',
  [S('Votre peau durcit par plaques rugueuses.', '', 'eruption'),
   S('Des taches brunes comme de l\'écorce marbrent vos bras.', '', 'eruption'),
   S('Vous ne sentez plus les coups.', 'muet', 'paralysie'),
   S('Votre corps raidi ralentit.', 'lenteur1', 'crampes lenteur'),
   S('Vos doigts sont raides et tremblent.', 'mains', 'tremblements crampes'),
   S('La faiblesse vous gagne.', 'faiblesse1', 'faiblesse'),
   VIE('Votre corps reste raide, et votre cœur fragile.', 'lenteur1 coeur-2', 'lenteur crampes')],
  'onguent_de_kukuroo', ['decoction_de_cordyceps'],
  [pr(0, 'onguent_de_kukuroo', 'decoction_de_cordyceps'), pr(6, 'onguent_de_kukuroo', 'decoction_de_cordyceps'),
   pr(6, 'onguent_de_kukuroo', 'decoction_de_cordyceps')],
  [C('soleil_apres_prise', minutes=10)], 'rester au soleil 10 min après chaque prise',
  'Prise 1 : Onguent + Décoction · Prise 2 (+6 h) : idem · Prise 3 (+6 h) : idem', contexte=['foret', 'marais'])

M(32, 'empoisonnement_aux_champignons', 'Empoisonnement aux champignons', 0, None, 'Votre estomac se retourne violemment.', 7,
  'TX+',
  [S('Votre estomac se retourne.', 'nausee', 'nausee'),
   S('Vous vomissez ; votre ventre crie aussitôt famine.', 'nausee faim+', 'nausee faim'),
   S('La tête vous tourne.', 'vertige', 'vertige'),
   S('Vos forces vous abandonnent.', 'faiblesse2', 'faiblesse'),
   VIE('Le poison a laissé votre cœur affaibli.', 'coeur-2', 'faiblesse')],
  'pilules_de_charbon_d_os', ['infusion_de_menthe_et_reglisse'],
  [pr(0, 'pilules_de_charbon_d_os'), pr(1, 'infusion_de_menthe_et_reglisse')],
  [C('a_jeun_pendant', heures=3)], 'à jeun pendant 3 h',
  'Prise 1 : Pilules · Prise 2 (+1 h) : Infusion', origine=['alimentaire', 'champignon'], contexte=['foret'])

M(33, 'mal_du_plomb', 'Mal du plomb', 1, None, 'Vous avez un goût de métal sur la langue.', 20,
  'GR↓, FE↓, TX+ · MR : grains bleutés dans les globules',
  [S('Un goût de métal vous colle à la langue.', '', 'bouche'),
   S('Votre ventre vous fait souffrir.', '', 'ventre'),
   S('Vos paupières sont lourdes de fatigue.', 'paupieres', 'fatigue'),
   S('Vous êtes pâle et sans force.', 'faiblesse1', 'paleur faiblesse'),
   S('Des tremblements vous secouent.', 'tremble', 'tremblements'),
   VIE('Vos mains tremblent et vos forces ne sont pas revenues.', 'mains faiblesse1', 'tremblements faiblesse')],
  'teinture_de_coriandre', ['decoction_d_ortie', 'pilules_de_charbon_de_bambou'],
  [pr(0, 'teinture_de_coriandre', 'decoction_d_ortie', 'pilules_de_charbon_de_bambou'),
   pr(4, 'teinture_de_coriandre', 'decoction_d_ortie', 'pilules_de_charbon_de_bambou'),
   pr(4, 'teinture_de_coriandre', 'decoction_d_ortie', 'pilules_de_charbon_de_bambou')],
  [C('pas_miner_sous', y=30)], 'ne pas miner sous Y 30 pendant la cure',
  'Prise 1 : les trois ensemble · Prise 2 (+4 h) : les trois · Prise 3 (+4 h) : les trois', contexte=['grotte'])

M(34, 'venin_de_scorpion_des_sables', 'Venin de scorpion des sables', 1, None, 'Une piqûre brûlante vous lance la cheville.', 7,
  'TX+ · MR : cristaux en étoile',
  [S('La piqûre à votre cheville vous brûle.', '', 'plaie'),
   S('Votre jambe s\'engourdit.', 'lenteur1', 'paralysie lenteur'),
   S('Vous suez, assoiffé.', 'soif++', 'sueurs soif'),
   S('Des crampes vous nouent les muscles.', 'faiblesse1', 'crampes faiblesse'),
   S('Votre cœur s\'emballe douloureusement.', 'degats', 'faiblesse'),
   VIE('Le venin a laissé votre cœur affaibli.', 'coeur-2', 'faiblesse')],
  'antidote_de_defense_de_sanglier', ['cataplasme_d_aloe'],
  [pr(0, 'antidote_de_defense_de_sanglier', 'cataplasme_d_aloe'), pr(2, 'antidote_de_defense_de_sanglier')],
  [C('repos')], 'repos : ni sprint ni combat', 'Prise 1 : Antidote + Cataplasme · Prise 2 (+2 h) : Antidote', contexte=['desert'])

M(35, 'alcool_frelate', 'Alcool frelaté', 0, None, 'Votre tête tourne et votre vue se trouble.', 7,
  'TX+, SU↓',
  [S('Une ivresse lourde vous retourne l\'estomac.', 'nausee', 'nausee vertige'),
   S('Votre tête cogne.', '', 'tete'),
   S('Votre vue se trouble.', 'floue', 'vision'),
   S('Vos bras sont mous.', 'faiblesse1', 'faiblesse'),
   VIE('Votre vue se trouble encore, de temps en temps.', '~floue', 'vision')],
  'decoction_d_oreille_de_judas', ['eau_de_riz_salee'],
  [pr(0, 'eau_de_riz_salee'), pr(1, 'decoction_d_oreille_de_judas'), pr(2, 'decoction_d_oreille_de_judas')],
  [C('pas_d_alcool', heures=24)], "pas d'alcool pendant 24 h",
  'Prise 1 : Eau de riz · Prise 2 (+1 h) : Décoction · Prise 3 (+2 h) : Décoction', origine=['alcool'])

M(36, 'poumon_de_soufre', 'Poumon de soufre', 1, None, 'Votre gorge vous pique comme après une fumée âcre.', 9,
  'TX+, GB↑',
  [S('Votre gorge irritée vous brûle.', '', 'gorge'),
   S('Vous toussez à vous en arracher les poumons.', 'toux', 'toux'),
   S('Vous manquez d\'air en courant.', 'sprint5', 'souffle'),
   S('Vos yeux brûlent et pleurent.', 'floue', 'vision'),
   VIE('Votre souffle est resté court.', 'sprint5', 'souffle')],
  'sirop_de_pleurote', ['eau_florale_de_menthe'],
  [pr(0, 'eau_florale_de_menthe'), pr(1, 'sirop_de_pleurote'), pr(3, 'eau_florale_de_menthe', 'sirop_de_pleurote')],
  [], 'aucune', 'Prise 1 : Eau florale · Prise 2 (+1 h) : Sirop · Prise 3 (+3 h) : Eau florale + Sirop', contexte=['grotte'])

M(37, 'venin_de_la_vipere_de_kakin', 'Venin de la vipère de Kakin', 2, None, 'Une morsure noirâtre vous élance le bras.', 8,
  'PL↓, TX+ · MR : cristaux en aiguilles',
  [S('La morsure noire vous élance.', '', 'plaie'),
   S('Votre bras enfle ; vous ralentissez.', 'lenteur1', 'plaie lenteur'),
   S('Des hématomes fleurissent sur votre peau.', '', 'saignements'),
   S('Vous saignez sans raison.', 'degats', 'saignements'),
   S('Le venin vous vide de vos forces.', 'faiblesse2', 'faiblesse'),
   S('La paralysie vous fige sur place.', 'immobile3', 'paralysie'),
   VIE('Le venin a laissé ses traces : vous cicatrisez mal et sautez à peine.', 'recup- saut-', 'faiblesse')],
  'antivenin_de_kakin', ['pilules_de_bourse_a_pasteur', 'breuvage_de_valeriane'],
  [pr(0, 'antivenin_de_kakin', 'pilules_de_bourse_a_pasteur'), pr(2, 'breuvage_de_valeriane', dormir=True)],
  [C('dormir_apres', prises=[2])], 'dormir dans un lit après la prise 2',
  'Prise 1 : Antivenin + Pilules · Prise 2 (+2 h) : Breuvage, puis dormir dans un lit', contexte=['jungle'])

M(38, 'mal_de_l_ambre', "Mal de l'ambre", 3, None, 'Une fatigue dorée et sucrée vous engourdit.', 12,
  'SU↑, FE↑, TX+ · MR : paillettes d\'ambre',
  [S('Une torpeur dorée vous engourdit.', 'paupieres', 'fatigue'),
   S('Une sueur sucrée vous colle à la peau ; vous avez soif.', 'soif+', 'sueurs soif'),
   S('Votre peau prend une teinte de miel.', '', 'jaunisse'),
   S('Vos pas ralentissent.', 'lenteur1', 'lenteur'),
   S('Vos bras perdent leur force.', 'faiblesse1', 'faiblesse'),
   S('Vos membres pèsent comme de l\'ambre figé.', 'lenteur2', 'lenteur'),
   S('Tout ce que vous voyez vire à l\'ambre.', 'ambre', 'couleurs'),
   VIE('Le monde est resté ambré, et vos jambes lentes.', 'ambre lenteur1', 'couleurs lenteur')],
  'elixir_de_l_aigle_araignee', ['pilules_de_charbon_d_os'],
  [pr(0, 'pilules_de_charbon_d_os'), pr(2, 'elixir_de_l_aigle_araignee'), pr(4, 'pilules_de_charbon_d_os'), pr(2, 'elixir_de_l_aigle_araignee')],
  [C('sans_nourriture')], 'à jeun pendant toute la cure',
  'Prise 1 : Pilules · Prise 2 (+2 h) : Élixir · Prise 3 (+4 h) : Pilules · Prise 4 (+2 h) : Élixir', origine=['alimentaire'])

M(39, 'scorbut', 'Scorbut', 0, None, 'Vos gencives saignent quand vous mangez.', 15,
  'GR↓, PL↓',
  [S('Une fatigue tenace vous accompagne.', 'paupieres', 'fatigue'),
   S('Vos gencives saignent dans votre bouche.', '', 'saignements bouche'),
   S('Des bleus apparaissent et vous font mal.', 'degats', 'saignements'),
   S('Vos plaies ne guérissent plus.', 'recup-', 'saignements'),
   S('Vous êtes sans force.', 'faiblesse1', 'faiblesse'),
   VIE('Vos blessures guérissent plus lentement qu\'avant.', 'recup-', 'faiblesse')],
  'sirop_d_aubepine', [],
  [pr(0, 'sirop_d_aubepine'), pr(2, 'sirop_d_aubepine')],
  [C('manger_plat', de=1, a=2, plat='fruits_legumes')], 'manger un plat de fruits ou de légumes du Gourmet entre les deux prises',
  'Prise 1 : Sirop · Entre les deux : manger un plat de fruits ou de légumes du Gourmet · Prise 2 (+2 h) : Sirop', origine=['carence'])

M(40, 'anemie_de_fer', 'Anémie de fer', 0, None, 'Vous êtes pâle et vite essoufflé.', 14,
  'GR↓, FE↓',
  [S('Une fatigue vous tombe dessus.', 'paupieres', 'fatigue'),
   S('Vous êtes d\'une pâleur de cire.', '', 'paleur'),
   S('Vous êtes vite à bout de souffle.', 'sprint8', 'souffle paleur'),
   S('Vos jambes ralentissent.', 'lenteur1', 'lenteur paleur'),
   VIE('Votre souffle est resté court.', 'sprint8', 'souffle')],
  'sirop_d_ortie', [],
  [pr(0, 'sirop_d_ortie'), pr(3, 'sirop_d_ortie')],
  [C('manger_plat', de=1, a=2, plat='viande_rouge')], 'manger un plat de viande rouge du Gourmet entre les deux prises',
  'Prise 1 : Sirop · Entre les deux : manger un plat de viande rouge du Gourmet · Prise 2 (+3 h) : Sirop', origine=['carence'])

M(41, 'coup_de_chaleur', 'Coup de chaleur', 0, None, 'La tête vous tourne sous ce soleil écrasant.', 7,
  'SU↓',
  [S('La tête vous tourne sous la chaleur.', 'vertige', 'vertige'),
   S('Une soif terrible vous dessèche.', 'soif++', 'soif'),
   S('Une nausée vous prend.', 'nausee', 'nausee'),
   S('Vous êtes sans force et vous traînez.', 'faiblesse1 lenteur1', 'faiblesse lenteur'),
   VIE('Vous avez soif bien plus vite qu\'avant.', 'soif+', 'soif')],
  'eau_florale_de_menthe_glacee', ['cataplasme_d_aloe'],
  [pr(0, 'eau_florale_de_menthe_glacee', 'cataplasme_d_aloe'), pr(1, 'eau_florale_de_menthe_glacee')],
  [C('ombre_pendant', heures=1)], "rester à l'ombre pendant 1 h",
  'Prise 1 : Eau florale + Cataplasme · Prise 2 (+1 h) : Eau florale', contexte=['desert', 'savane'])

M(42, 'mal_des_neiges', 'Mal des neiges', 0, None, 'Vous ne sentez plus vos doigts tant il fait froid.', 7,
  "rien d'anormal",
  [S('Un frisson glacé vous traverse.', 'tremble', 'frissons'),
   S('Vos doigts gourds tremblent.', 'mains', 'tremblements'),
   S('Le froid vous ralentit.', 'lenteur1', 'lenteur'),
   S('Vos paupières se ferment, vos forces s\'en vont.', 'paupieres faiblesse1', 'fatigue faiblesse'),
   VIE('Le froid vous a laissé un peu moins vigoureux.', 'coeur-1', 'faiblesse')],
  'decoction_de_bois_de_cerf', [],
  [pr(0, 'decoction_de_bois_de_cerf'), pr(1, 'decoction_de_bois_de_cerf')],
  [C('pres_du_feu', heures=1)], "rester à côté d'un feu pendant 1 h",
  'Prise 1 : Décoction · Prise 2 (+1 h) : Décoction', contexte=['froid'])

M(43, 'mal_des_montagnes', 'Mal des montagnes', 1, None, "L'air vous semble trop mince pour respirer.", 7,
  'GR↑',
  [S('Votre tête bat la mesure.', '', 'tete'),
   S('Vous manquez d\'air au moindre effort.', 'sprint5', 'souffle'),
   S('Une nausée vous prend.', 'nausee', 'nausee'),
   S('Le vertige vous ralentit.', 'vertige lenteur1', 'vertige lenteur'),
   VIE('En altitude, vous manquez toujours d\'air.', 'sprint5@y100', 'souffle')],
  'pilules_des_cimes', ['infusion_de_ginkgo'],
  [pr(0, 'pilules_des_cimes', 'infusion_de_ginkgo'), pr(2, 'pilules_des_cimes')],
  [C('y_max', y=80)], 'descendre sous Y 80 pendant la cure', 'Prise 1 : Pilules + Infusion · Prise 2 (+2 h) : Pilules', contexte=['altitude'])

M(44, 'insomnie_noire', 'Insomnie noire', 1, None, "Vous n'arrivez plus à fermer l'œil.", 10,
  "rien d'anormal",
  [S('Vos yeux sont lourds de sommeil perdu.', '', 'insomnie fatigue'),
   S('Vos paupières tombent toutes seules.', 'paupieres', 'fatigue'),
   S('Tout vous irrite, votre vue se brouille.', 'floue', 'insomnie vision'),
   S('Vous avancez au ralenti.', 'lenteur1', 'lenteur'),
   S('Des ombres bougent dans le coin de votre œil.', 'hallu', 'hallucinations'),
   VIE('Le sommeil vous fuit encore, par moments.', '~paupieres', 'fatigue')],
  'pilules_de_coquelicot', ['decoction_de_valeriane', 'infusion_de_camomille_et_lavande'],
  [pr(0, 'decoction_de_valeriane', 'infusion_de_camomille_et_lavande', moment='soir', dormir=True),
   pr(0, 'pilules_de_coquelicot', moment='reveil')],
  [C('dormir_dans_l_heure', prises=[1])], 'dormir dans un lit dans l\'heure qui suit la prise 1',
  "Prise 1, le soir : Décoction + Infusion, puis dormir dans un lit dans l'heure · Prise 2, au réveil : Pilules", contexte=['nuit'])

M(45, 'sang_epais', 'Sang épais', 2, 'Groupes AB', 'Vos jambes sont lourdes, comme pleines de plomb.', 18,
  'GR↑, PL↑ · MR : globules en grappes',
  [S('Vos jambes sont lourdes comme du plomb.', '', 'lenteur'),
   S('Vous traînez des pieds.', 'lenteur1', 'lenteur'),
   S('Vos doigts froids tremblent.', 'mains', 'tremblements'),
   S('Votre tête bat lourdement.', 'floue', 'tete'),
   S('Le monde tangue, une nausée vous prend.', 'vertige nausee', 'vertige nausee'),
   S('Vos forces vous abandonnent.', 'faiblesse2', 'faiblesse'),
   VIE('Votre sang reste épais : vous êtes plus lent et plus fragile.', 'lenteur1 coeur-2', 'lenteur')],
  'teinture_de_lys_des_nuees', ['decoction_de_saule'],
  [pr(0, 'teinture_de_lys_des_nuees', 'decoction_de_saule'), pr(3, 'teinture_de_lys_des_nuees', 'decoction_de_saule'),
   pr(3, 'teinture_de_lys_des_nuees')],
  [C('pas_de_sprint')], 'pas de sprint pendant la cure',
  'Prise 1 : Teinture + Décoction · Prise 2 (+3 h) : idem · Prise 3 (+3 h) : Teinture')

M(46, 'epuisement_d_aura', "Épuisement d'aura", 1, None, 'Vous vous sentez creux, comme si votre énergie s\'était vidée.', 8,
  'SU↓, FE↓',
  [S('Une lassitude profonde vous habite.', '', 'fatigue'),
   S('Vos coups ont perdu leur force.', 'faiblesse1', 'faiblesse'),
   S('Vos paupières s\'alourdissent.', 'paupieres', 'fatigue'),
   S('Votre corps refuse l\'effort.', 'faiblesse1', 'faiblesse'),
   S('Après le combat, le monde se met à tourner.', 'vertige@combat', 'vertige'),
   VIE('Après chaque combat, vos forces s\'effondrent.', 'faiblesse1@combat', 'faiblesse')],
  'bouillon_de_girolle_et_de_ginseng', ['pilules_de_reishi'],
  [pr(0, 'bouillon_de_girolle_et_de_ginseng', dormir=True), pr(3, 'pilules_de_reishi')],
  [C('aucun_combat')], 'aucun combat pendant la cure',
  'Prise 1 : Bouillon, puis dormir dans un lit · Prise 2 (+3 h) : Pilules', contexte=['combat'])

M(47, 'fievre_des_arenes', 'Fièvre des arènes', 1, 'Hommes', "Vos muscles vous brûlent après l'effort.", 10,
  'GB↑ · MR : bâtonnets rouges',
  [S('Une petite fièvre vous échauffe.', 'rouge', 'fievre'),
   S('Vos muscles douloureux vous ralentissent.', 'lenteur1', 'lenteur crampes'),
   S('Vous suez, assoiffé.', 'soif+', 'sueurs soif'),
   S('Vous êtes sans force.', 'faiblesse1', 'faiblesse'),
   S('Vos articulations raides vous empêchent de sauter haut.', 'saut-', 'crampes'),
   VIE('Vos genoux ne se sont jamais remis : vous sautez moins haut.', 'saut-', 'crampes')],
  'teinture_de_morille', ['decoction_d_echinacee'],
  [pr(0, 'teinture_de_morille', 'decoction_d_echinacee'), pr(2, 'teinture_de_morille'), pr(4, 'teinture_de_morille', 'decoction_d_echinacee')],
  [C('aucun_combat')], 'aucun combat pendant la cure',
  'Prise 1 : Teinture + Décoction · Prise 2 (+2 h) : Teinture · Prise 3 (+4 h) : Teinture + Décoction', contexte=['combat'])

M(48, 'langueur_de_lune', 'Langueur de lune', 2, 'Femmes', 'Chaque nuit, une étrange lassitude vous envahit.', 14,
  'GR↓, SU↓ · MR : cellules en croissant',
  [S('La nuit tombée, une fatigue étrange vous envahit.', 'paupieres@nuit', 'fatigue'),
   S('Le sommeil vous fuit.', '', 'insomnie'),
   S('Sous la lune, le monde tangue.', 'vertige@nuit', 'vertige'),
   S('La nuit, vos jambes ralentissent.', 'lenteur1@nuit', 'lenteur'),
   S('La nuit, vos forces vous quittent.', 'faiblesse1@nuit', 'faiblesse'),
   S('La nuit, tout prend une teinte bleutée.', 'bleu@nuit', 'couleurs'),
   VIE('Chaque nuit, vos jambes s\'alourdissent.', 'lenteur1@nuit', 'lenteur')],
  'eau_florale_de_lune_pale', ['infusion_de_melisse'],
  [pr(0, 'eau_florale_de_lune_pale', moment='nuit'), pr(2, 'infusion_de_melisse'), pr(0, 'eau_florale_de_lune_pale', moment='reveil')],
  [], 'aucune', 'Prise 1, la nuit : Eau florale · Prise 2 (+2 h) : Infusion · Prise 3, au réveil : Eau florale', contexte=['nuit'])

M(49, 'fievre_de_kakin', 'Fièvre de Kakin', 2, 'Groupes A et O', 'Une chaleur pourpre vous monte au visage.', 12,
  'GB↑↑, PL↓ · MR : grains pourpres',
  [S('La fièvre vous empourpre.', 'rouge', 'fievre'),
   S('Vous claquez des dents.', 'tremble', 'frissons'),
   S('Une toux vous secoue.', 'toux', 'toux'),
   S('Une éruption pourpre vous couvre ; vous êtes sans force.', 'faiblesse1', 'eruption faiblesse'),
   S('Des courbatures vous ralentissent.', 'lenteur1', 'lenteur'),
   S('Vous délirez : des voix, des ombres…', 'hallu', 'hallucinations fievre'),
   VIE('La fièvre de Kakin vous a laissé plus fragile.', 'coeur-2', 'faiblesse')],
  'teinture_d_amanite_rouge', ['infusion_de_the_et_menthe', 'sirop_de_reglisse'],
  [pr(0, 'teinture_d_amanite_rouge', 'infusion_de_the_et_menthe'), pr(3, 'sirop_de_reglisse'),
   pr(3, 'teinture_d_amanite_rouge', 'infusion_de_the_et_menthe')],
  [C('au_chaud')], 'rester au chaud',
  'Prise 1 : Teinture + Infusion · Prise 2 (+3 h) : Sirop · Prise 3 (+3 h) : Teinture + Infusion', contexte=['jungle'])

M(50, 'petrification_lente', 'Pétrification lente', 3, None, 'Le bout de vos doigts est gris et froid.', 21,
  'GR↓, FE↑, TX+ · MR : cristaux de pierre',
  [S('Le bout de vos doigts est gris et froid comme la pierre.', '', 'crampes'),
   S('Vos doigts raides tremblent.', 'mains', 'tremblements crampes'),
   S('Vous avancez lentement.', 'lenteur1', 'lenteur'),
   S('Vos bras se raidissent.', 'faiblesse1', 'crampes'),
   S('Vos jambes de pierre vous ralentissent.', 'lenteur2', 'lenteur crampes'),
   S('Vos jambes refusent de vous soulever.', 'nosaut', 'crampes'),
   S('Vous êtes figé sur place, comme une statue.', 'immobile5', 'paralysie'),
   VIE('Une part de vous est restée pierre.', 'lenteur2 nosaut', 'lenteur crampes')],
  'onguent_de_la_baleine', ['decoction_de_fer_sang_et_pied_bleu'],
  [pr(0, 'onguent_de_la_baleine', 'decoction_de_fer_sang_et_pied_bleu'), pr(6, 'onguent_de_la_baleine', 'decoction_de_fer_sang_et_pied_bleu'),
   pr(6, 'onguent_de_la_baleine', 'decoction_de_fer_sang_et_pied_bleu', dormir=True)],
  [C('a_jeun_avant', prises=[1, 2, 3], heures=1)], 'à jeun avant chaque prise',
  'Prise 1 : Onguent + Décoction · Prise 2 (+6 h) : idem · Prise 3 (+6 h) : idem, puis dormir', contexte=['grotte'])

M(51, 'toxine_de_fourmi_chimere', 'Toxine de fourmi-chimère', 3, None, 'Une plaie qui ne guérit pas noircit sur votre peau.', 10,
  'GB↑↑, TX+, PA+ · MR : cellules mutantes',
  [S('La plaie noircit à vue d\'œil.', '', 'plaie'),
   S('La fièvre vous brûle.', 'rouge', 'fievre'),
   S('Vos veines s\'assombrissent sous la peau.', '', 'eruption'),
   S('Vos forces vous quittent.', 'faiblesse1', 'faiblesse'),
   S('Vos jambes sont de plomb.', 'lenteur2', 'lenteur'),
   S('Des formes rampent à la lisière de votre regard.', 'hallu', 'hallucinations'),
   S('Gauche et droite s\'échangent sans prévenir.', 'desoriente', 'desorientation'),
   VIE('La toxine a laissé des traces : parfois tout s\'inverse, et votre cœur est faible.', '~desoriente coeur-4', 'desorientation faiblesse')],
  'elixir_de_l_arbre_monde', ['pilules_de_mousse_des_abysses', 'pilules_de_bourse_a_pasteur'],
  [pr(0, 'pilules_de_mousse_des_abysses', 'pilules_de_bourse_a_pasteur'), pr(2, 'elixir_de_l_arbre_monde'),
   pr(4, 'pilules_de_mousse_des_abysses'), pr(2, 'elixir_de_l_arbre_monde')],
  [C('repos')], 'repos absolu : ni sprint ni combat',
  'Prise 1 : les deux pilules · Prise 2 (+2 h) : Élixir · Prise 3 (+4 h) : Pilules de mousse des abysses · Prise 4 (+2 h) : Élixir',
  contexte=['combat'])

M(52, 'mal_du_continent_noir', 'Mal du Continent noir', 3, None, "Quelque chose d'inconnu s'est glissé en vous.", 12,
  'GB↑↑, GR↓, PL↓, TX+, PA+ · MR : spores noires inconnues',
  [S('Un malaise sans nom vous serre la poitrine.', '', 'fatigue'),
   S('La fièvre monte.', 'rouge', 'fievre'),
   S('Une toux noire vous déchire.', 'toux', 'toux'),
   S('Les bords du monde s\'obscurcissent.', 'tunnel', 'vision'),
   S('Votre corps ne vous obéit presque plus.', 'faiblesse2', 'faiblesse'),
   S('Les couleurs s\'inversent sous vos yeux.', 'inverse', 'couleurs'),
   S('Gauche et droite se mélangent.', 'desoriente', 'desorientation'),
   VIE('Les couleurs sont restées fausses, et parfois tout s\'inverse.', 'inverse ~desoriente', 'couleurs desorientation')],
  'elixir_noir', ['eau_florale_de_lys_des_nuees', 'elixir_du_lotus_de_l_aube'],
  [pr(0, 'elixir_noir', 'eau_florale_de_lys_des_nuees'), pr(2, 'elixir_du_lotus_de_l_aube'), pr(4, 'elixir_noir'),
   pr(2, 'elixir_du_lotus_de_l_aube'), pr(4, 'elixir_noir')],
  [C('a_jeun_avant', prises=[1], heures=1)], 'à jeun avant la prise 1',
  'Prise 1 : Élixir noir + Eau florale · Prise 2 (+2 h) : Élixir du lotus · Prise 3 (+4 h) : Élixir noir · Prise 4 (+2 h) : Élixir du lotus · Prise 5 (+4 h) : Élixir noir')

assert len(MALADIES) == 52
assert [m['numero'] for m in MALADIES] == list(range(1, 53))
