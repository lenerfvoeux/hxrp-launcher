"""
D'où vient chaque ingrédient dans le monde : cultures, arbres fruitiers, animaux, gisements, artisanat.
Source unique : gen_monde.py en tire les textures, modèles, états de blocs, noms, recettes
et le fichier data/recolte.json lu par le mod.

Climats (pour la génération du monde et les graines trouvées dans les hautes herbes) :
  tempere  plaines, forêts          chaud    savanes, terres sèches
  tropical jungles                  humide   marais, rivières           froid  taïgas
"""

# ============================================================================ cultures
# id produit : (forme, climat, nom de la semence, rendement min, max, couleur du feuillage)
# formes : cereale, mais, racine, tubercule, bulbe, tuteur, courge, gros_fruit, pomme, tige, chardon,
#          champignon, feuilles, herbe, ananas, fraisier, baies, vigne, gousse, rame, canne, fleur, arbuste
CULTURES = {
    'ble':            ('cereale',   'tempere',  'Graines de blé', 1, 2, '#6aa83a'),
    'riz':            ('riz',       'humide',   'Grains de riz à semer', 1, 2, '#7ab84a'),
    'mais':           ('mais',      'tempere',  'Grains de maïs à semer', 1, 2, '#5aa03a'),
    'avoine':         ('cereale',   'froid',    "Graines d'avoine", 1, 2, '#7aa84a'),
    'sarrasin':       ('sarrasin',  'froid',    'Graines de sarrasin', 1, 2, '#6a9a3a'),
    'quinoa':         ('quinoa',    'chaud',    'Graines de quinoa', 1, 2, '#6a9a44'),
    'carotte':        ('racine',    'tempere',  'Graines de carotte', 1, 3, '#48a82a'),
    'pomme_de_terre': ('tubercule', 'tempere',  'Plant de pomme de terre', 2, 4, '#4a8a2e'),
    'oignon':         ('bulbe',     'tempere',  "Bulbille d'oignon", 1, 3, '#5a9a4a'),
    'echalote':       ('bulbe',     'tempere',  "Bulbille d'échalote", 2, 3, '#5a9a4a'),
    'ail':            ('bulbe',     'chaud',    "Caïeu d'ail", 1, 3, '#6aa05a'),
    'tomate':         ('tuteur',    'chaud',    'Graines de tomate', 2, 4, '#3f8a2a'),
    'poivron':        ('tuteur',    'chaud',    'Graines de poivron', 1, 3, '#3f8a2a'),
    'courgette':      ('courge',    'tempere',  'Graines de courgette', 1, 3, '#4a8a2e'),
    'aubergine':      ('tuteur',    'chaud',    "Graines d'aubergine", 1, 3, '#4a7a3a'),
    'chou':           ('pomme',     'froid',    'Graines de chou', 1, 1, '#5aa83a'),
    'chou_fleur':     ('pomme',     'froid',    'Graines de chou-fleur', 1, 1, '#4a9a3a'),
    'brocoli':        ('pomme',     'tempere',  'Graines de brocoli', 1, 1, '#3a8a3a'),
    'laitue':         ('pomme',     'tempere',  'Graines de laitue', 1, 1, '#7ac84a'),
    'poireau':        ('tige',      'froid',    'Graines de poireau', 1, 2, '#3a8a3a'),
    'concombre':      ('courge',    'tempere',  'Graines de concombre', 1, 3, '#3a7a2a'),
    'champignon':     ('champignon', 'humide',  'Spores de champignon', 2, 4, '#c89a6a'),
    'epinards':       ('feuilles',  'froid',    "Graines d'épinards", 2, 3, '#2e7426'),
    'haricot_vert':   ('rame',      'tempere',  'Graines de haricot vert', 2, 4, '#4aa032'),
    'petit_pois':     ('rame',      'tempere',  'Graines de petit pois', 2, 4, '#5ab03a'),
    'potiron':        ('gros_fruit', 'tempere', 'Graines de potiron', 1, 1, '#4a8a2e'),
    'radis':          ('racine',    'tempere',  'Graines de radis', 2, 4, '#4a9a3a'),
    'betterave':      ('racine',    'froid',    'Graines de betterave', 1, 2, '#3a7a2a'),
    'patate_douce':   ('tubercule', 'tropical', 'Plant de patate douce', 1, 3, '#4a8a3a'),
    'navet':          ('racine',    'froid',    'Graines de navet', 1, 2, '#4a9a3a'),
    'celeri':         ('tige',      'humide',   'Graines de céleri', 1, 2, '#6ab04a'),
    'asperge':        ('asperge',   'tempere',  "Griffe d'asperge", 2, 4, '#6aa03a'),
    'artichaut':      ('chardon',   'chaud',    "Graines d'artichaut", 1, 2, '#6a8a6a'),
    'ananas':         ('ananas',    'tropical', "Couronne d'ananas", 1, 1, '#3a8a3a'),
    'fraise':         ('fraisier',  'tempere',  'Plant de fraisier', 2, 4, '#3f9a2e'),
    'framboise':      ('baies',     'tempere',  'Plant de framboisier', 2, 4, '#3f8a2e'),
    'myrtille':       ('baies',     'froid',    'Plant de myrtillier', 2, 5, '#3a7a3a'),
    'raisin':         ('vigne',     'chaud',    'Plant de vigne', 1, 2, '#5a8a2a'),
    'kiwi':           ('vigne',     'tempere',  "Plant d'actinidia", 2, 3, '#4a7a2a'),
    'melon':          ('gros_fruit', 'chaud',   'Graines de melon', 1, 1, '#5a8a2e'),
    'pasteque':       ('gros_fruit', 'chaud',   'Graines de pastèque', 1, 1, '#4a8a2e'),
    'cacahuete':      ('tubercule', 'chaud',    "Graines d'arachide", 2, 4, '#5a9a3a'),
    'lentilles':      ('gousse',    'chaud',    'Graines de lentille', 2, 3, '#7a9a3a'),
    'pois_chiche':    ('gousse',    'chaud',    'Graines de pois chiche', 2, 3, '#6a9a4a'),
    'haricot_rouge':  ('rame',      'tempere',  'Graines de haricot rouge', 2, 3, '#4a9a32'),
    'haricot_blanc':  ('rame',      'tempere',  'Graines de haricot blanc', 2, 3, '#4a9a32'),
    'soja':           ('gousse',    'humide',   'Graines de soja', 2, 3, '#6aa83a'),
    'basilic':        ('herbe',     'chaud',    'Graines de basilic', 2, 3, '#3aa02a'),
    'persil':         ('herbe',     'tempere',  'Graines de persil', 2, 3, '#2e8a2a'),
    'menthe':         ('herbe',     'humide',   'Plant de menthe', 2, 3, '#4ab04a'),
    'coriandre':      ('herbe',     'chaud',    'Graines de coriandre', 2, 3, '#5ab83a'),
    'thym':           ('herbe',     'chaud',    'Graines de thym', 2, 3, '#6a8a5a'),
    'romarin':        ('herbe',     'chaud',    'Bouture de romarin', 2, 3, '#3a6a5a'),
    'ciboulette':     ('herbe',     'tempere',  'Graines de ciboulette', 2, 3, '#3a9a2a'),
    'aneth':          ('herbe',     'tempere',  "Graines d'aneth", 2, 3, '#6ab04a'),
    'estragon':       ('herbe',     'tempere',  "Bouture d'estragon", 2, 3, '#3a8a3a'),
    'grains_de_cafe': ('baies',     'tropical', 'Plant de caféier', 2, 4, '#2e6a2a'),
    'feuilles_de_the': ('arbuste',  'tropical', 'Bouture de théier', 2, 4, '#2a6a2a'),
    'canne_a_sucre':  ('canne',     'tropical', 'Bouture de canne à sucre', 1, 3, '#7ab04a'),
    'poivre_noir':    ('vigne',     'tropical', 'Bouture de poivrier', 2, 3, '#3a7a2a'),
    'piment':         ('tuteur',    'chaud',    'Graines de piment', 2, 4, '#3f8a2a'),
    'gingembre':      ('racine',    'tropical', 'Rhizome de gingembre', 1, 2, '#5a9a3a'),
    'curcuma':        ('racine',    'tropical', 'Rhizome de curcuma', 1, 2, '#5aa03a'),
    'vanille':        ('vigne',     'tropical', 'Bouture de vanillier', 1, 2, '#3a8a3a'),
    'safran':         ('fleur',     'chaud',    'Bulbe de crocus', 1, 2, '#5a8a3a'),
    'cumin':          ('fleur',     'chaud',    'Graines de cumin', 2, 3, '#6a9a4a'),
    'graines_de_moutarde': ('fleur', 'tempere', 'Graines de moutarde à semer', 2, 3, '#5a9a3a'),
    'sesame':         ('fleur',     'chaud',    'Graines de sésame à semer', 2, 3, '#5a8a3a'),
    'laurier':        ('arbuste',   'chaud',    'Bouture de laurier', 2, 3, '#3a6a2a'),
}

# fleurs portées au stade 2 (ou 1 pour les plantes à fruits) : couleur des pétales
FLEURS = {
    'pomme_de_terre': '#f4f0f8', 'patate_douce': '#c890d8', 'cacahuete': '#f0c820', 'tomate': '#f0d020',
    'poivron': '#f4f4ec', 'aubergine': '#a878c8', 'piment': '#f4f4ec', 'courgette': '#f0b020', 'concombre': '#f0d020',
    'potiron': '#f0a820', 'melon': '#f0d020', 'pasteque': '#f0d020', 'fraise': '#fbfbf4', 'framboise': '#f4f0f4',
    'myrtille': '#f4e0f0', 'grains_de_cafe': '#fbfbf4', 'safran': '#9a5ad0', 'cumin': '#f4f0f4',
    'graines_de_moutarde': '#f0d828', 'sesame': '#f8f0f4', 'ciboulette': '#b87ad8', 'romarin': '#8a8ad8',
    'lentilles': '#e8e0f8', 'pois_chiche': '#f4e8f4', 'soja': '#c8a0e0', 'haricot_vert': '#f4f4f0',
    'petit_pois': '#f4f4f8', 'haricot_rouge': '#e84a3a', 'haricot_blanc': '#f4f4f0', 'artichaut': '#8a4ad0',
    'kiwi': '#f4ecd0', 'poivre_noir': '#f4f4e0', 'vanille': '#f0e8a0', 'thym': '#e8c8e8', 'basilic': '#f4f4f4',
}

# ============================================================================ arbres fruitiers
# id de l'arbre : (fruit récolté, nom de l'arbre, climat, forme, bois vanilla, feuillage, fleur, taille du fruit)
# formes : rond, ovale, large, buisson, palmier, bananier ; bois : oak, spruce, birch, jungle, acacia, dark_oak
ARBRES = {
    'pommier':    ('pomme', 'Pommier', 'tempere', 'rond', 'oak', '#4a8a2e', '#f8d8e4', 3),
    'poirier':    ('poire', 'Poirier', 'tempere', 'ovale', 'oak', '#4a8a36', '#fbf6f0', 3),
    'cerisier':   ('cerise', 'Cerisier', 'tempere', 'rond', 'birch', '#4a8a2e', '#f8c8dc', 1),
    'noyer':      ('noix', 'Noyer', 'tempere', 'large', 'dark_oak', '#4a7a2a', None, 2),
    'noisetier':  ('noisette', 'Noisetier', 'tempere', 'buisson', 'oak', '#5a8a2e', None, 1),
    'pecher':     ('peche', 'Pêcher', 'chaud', 'rond', 'oak', '#5a8a2a', '#f4a8c0', 3),
    'abricotier': ('abricot', 'Abricotier', 'chaud', 'rond', 'oak', '#4a8a2a', '#fbe8ec', 2),
    'figuier':    ('figue', 'Figuier', 'chaud', 'large', 'birch', '#3a7a2a', None, 2),
    'olivier':    ('olives', 'Olivier', 'chaud', 'large', 'dark_oak', '#6a8a6a', '#f4f0d8', 1),
    'amandier':   ('amande', 'Amandier', 'chaud', 'rond', 'oak', '#5a8a3a', '#fbf0f4', 1),
    'pistachier': ('pistache', 'Pistachier', 'chaud', 'buisson', 'acacia', '#5a7a3a', None, 1),
    'oranger':    ('orange', 'Oranger', 'chaud', 'rond', 'oak', '#2e6a2a', '#fbfbf4', 3),
    'citronnier': ('citron', 'Citronnier', 'chaud', 'ovale', 'oak', '#3a7a2a', '#fbfbf4', 2),
    'limettier':  ('citron_vert', 'Limettier', 'tropical', 'buisson', 'oak', '#2e6a2a', '#fbfbf4', 2),
    'manguier':   ('mangue', 'Manguier', 'tropical', 'large', 'jungle', '#2a5a22', '#f0e0a0', 3),
    'avocatier':  ('avocat', 'Avocatier', 'tropical', 'ovale', 'jungle', '#2a5a2a', '#e8f0b0', 3),
    'cacaoyer':   ('feves_de_cacao', 'Cacaoyer', 'tropical', 'rond', 'jungle', '#3a6a2a', '#fbf0f0', 3),
    'cannelier':  ('cannelle', 'Cannelier', 'tropical', 'ovale', 'jungle', '#4a6a2a', '#f0f0d0', 1),
    'muscadier':  ('muscade', 'Muscadier', 'tropical', 'ovale', 'jungle', '#2e5a2a', '#f4f0c8', 2),
    'giroflier':  ('clou_de_girofle', 'Giroflier', 'tropical', 'ovale', 'jungle', '#3a6a2a', '#f8d0d8', 1),
    'cocotier':   ('noix_de_coco', 'Cocotier', 'tropical', 'palmier', 'jungle', '#4a8a2a', None, 4),
    'bananier':   ('banane', 'Bananier', 'tropical', 'bananier', 'jungle', '#5aa03a', None, 4),
}

# ============================================================================ animaux
# nouveaux animaux : id : (nom, comportement, climat/biomes, ce qu'ils donnent)
ANIMAUX = {
    'saumon':    ('Saumon', 'poisson', ['riviere', 'ocean_froid'], ['saumon']),
    'truite':    ('Truite', 'poisson', ['riviere'], ['truite']),
    'thon':      ('Thon', 'poisson', ['ocean'], ['thon']),
    'cabillaud': ('Cabillaud', 'poisson', ['ocean', 'ocean_froid'], ['cabillaud']),
    'sardine':   ('Sardine', 'banc', ['ocean'], ['sardine']),
    'maquereau': ('Maquereau', 'banc', ['ocean'], ['maquereau']),
    'anchois':   ('Anchois', 'banc', ['ocean'], ['anchois']),
    'crevette':  ('Crevette', 'fond', ['ocean', 'plage'], ['crevette']),
    'crabe':     ('Crabe', 'crabe', ['plage'], ['crabe']),
    'dinde':     ('Dinde', 'volaille', ['tempere'], ['dinde']),
    'canard':    ('Canard', 'volaille', ['humide', 'riviere'], ['canard']),
    'cerf':      ('Cerf', 'gibier', ['tempere', 'froid'], ['cerf']),
    'sanglier':  ('Sanglier', 'sanglier', ['tempere'], ['sanglier']),
    'chevre':    ('Chèvre', 'chevre', ['montagne', 'chaud'], ['lait_de_chevre']),
}

# animaux vanilla : ce qu'ils donnent en plus
VANILLA = {
    'cow': ['boeuf'], 'cow_bebe': ['veau'], 'pig': ['porc'], 'chicken': ['poulet'], 'sheep': ['mouton'],
    'rabbit': ['lapin'], 'squid': ['calamar'],
}

# ============================================================================ blocs du monde et artisanat
BLOCS = {
    'minerai_de_sel': ('Minerai de sel', 'sel'),
    'ruche_sauvage':  ('Ruche sauvage', 'miel'),
    'banc_de_moules': ('Banc de moules', 'moule'),
    'banc_d_huitres': ("Banc d'huîtres", 'huitre'),
}

# autres objets créés pour la récolte
OBJETS = {
    'olives': 'Olives',
}

# recettes d'artisanat (sans table) : produit, quantité, ingrédients (ids du mod ou minecraft:…)
RECETTES = [
    ('sucre', 2, ['canne_a_sucre']),
    ('huile_d_olive', 1, ['olives', 'olives', 'olives', 'olives', 'minecraft:glass_bottle']),
    ('vinaigre', 1, ['raisin', 'raisin', 'minecraft:glass_bottle']),
    ('sauce_soja', 1, ['soja', 'soja', 'sel', 'minecraft:glass_bottle']),
    ('levure', 2, ['ble', 'sucre', 'minecraft:water_bottle']),
    ('paprika', 2, ['poivron', 'piment']),
    ('bouteille_d_eau', 1, ['minecraft:water_bottle']),
    ('oeuf', 1, ['minecraft:egg']),
    ('lait', 3, ['minecraft:milk_bucket', 'minecraft:glass_bottle', 'minecraft:glass_bottle', 'minecraft:glass_bottle']),
]

# comment se procurer les autres (mécaniques propres)
MECANIQUES = {
    'oeuf': 'Les poules pondent des œufs (toutes les 5 à 10 minutes).',
    'lait': 'Clic droit sur une vache avec une fiole vide.',
    'lait_de_chevre': 'Clic droit sur une chèvre avec une fiole vide.',
    'miel': 'Clic droit sur une ruche sauvage pleine avec une fiole vide.',
    'bouteille_d_eau': "Une fiole d'eau se transforme en bouteille d'eau.",
}
