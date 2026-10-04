"""
Les 7 blessures (data/virus_blessures.json), 4 stades chacune.
Les effets d'un stade s'ajoutent à ceux des stades précédents (une blessure s'aggrave, elle ne change pas).
Mêmes codes d'effets que les maladies ; « saignement » = petite perte de vie régulière,
« hemorragie » = perte de vie continue ; « degats_saut » / « degats_marche » = dégâts en sautant / en marchant ;
« lenteur0 » = lenteur légère.
"""

BLESSURES = [
    {'id': 'contusions', 'nom': 'Contusions', 'declencheur': '4 coups reçus en 30 s', 'soin': 'baume_d_arnica',
     'stades': [
         ('Des bleus violacés fleurissent sur votre corps.', ''),
         ('Chaque mouvement réveille vos douleurs.', 'lenteur0'),
         ('De gros hématomes vous coupent les forces.', 'faiblesse1'),
         ('Une côte fêlée vous coupe le souffle : courir est impossible, sauter vous déchire.', 'nosprint degats_saut faiblesse2'),
     ]},
    {'id': 'commotion', 'nom': 'Commotion', 'declencheur': 'Coup de 6 dégâts ou plus', 'soin': 'elixir_de_ginkgo',
     'stades': [
         ('Sonné, vous voyez trente-six chandelles.', '~vertige'),
         ('Un mal de tête vous brouille la vue.', '~floue'),
         ('La nausée vous prend, un sifflement vous vrille les oreilles.', '~nausee ~acouphenes'),
         ('Par moments, vous ne savez plus où sont la gauche et la droite ; le monde s\'éteint un instant.', '~desoriente ~cecite'),
     ]},
    {'id': 'jambe', 'nom': 'Jambe blessée', 'declencheur': 'Chute de 4 blocs ou plus', 'soin': 'attelle_de_consoude',
     'stades': [
         ('Une entorse légère vous fait boitiller.', 'lenteur0'),
         ('Votre cheville enflée vous empêche de bien sauter.', 'lenteur1 saut-'),
         ('Fracture ! Vous ne pouvez plus courir.', 'lenteur2 nosprint'),
         ('Fracture ouverte : impossible de sauter, chaque pas vous coûte du sang.', 'nosaut degats_marche'),
     ]},
    {'id': 'plaie', 'nom': 'Plaie', 'declencheur': 'Lame, griffes, épines, balle', 'soin': 'pansement_d_achillee',
     'stades': [
         ('La coupure saigne un peu.', 'saignement'),
         ('La plaie est profonde et saigne encore.', 'saignement'),
         ('Hémorragie : le sang ne s\'arrête plus, vos forces s\'en vont.', 'faiblesse1'),
         ('Hémorragie grave : vous perdez votre sang, le monde se resserre.', 'hemorragie tunnel'),
     ]},
    {'id': 'brulure', 'nom': 'Brûlure', 'declencheur': 'Feu, lave, attaques du Phénix', 'soin': 'onguent_de_souci_et_d_aloe',
     'stades': [
         ('Votre peau rougit et vous cuit.', ''),
         ('Des cloques se forment ; votre corps récupère mal.', 'recup-'),
         ('Brûlure profonde : votre corps ne se régénère plus.', 'noregen'),
         ('Carbonisation : plus de régénération, plus de force, plus de course.', 'faiblesse1 nosprint'),
     ]},
    {'id': 'morsure', 'nom': 'Morsure venimeuse', 'declencheur': 'Araignées et créatures venimeuses', 'soin': 'antivenin_de_bardane',
     'stades': [
         ('Une piqûre vous lance.', ''),
         ('La morsure enfle, une nausée vous prend.', '~nausee'),
         ('Le venin vous empoisonne.', 'poison'),
         ('Paralysie partielle : vos membres tremblent et vous obéissent à peine.', 'lenteur2 tremble'),
     ]},
    {'id': 'gelure', 'nom': 'Gelure', 'declencheur': 'Froid ou eau glacée prolongés', 'soin': 'bandage_de_peau_de_cerf',
     'stades': [
         ('Vos doigts sont gourds.', ''),
         ('Des engelures vous font trembler les mains.', 'mains'),
         ('La gelure vous ralentit.', 'lenteur1'),
         ('Nécrose : la chair meurt, vous ralentissez et souffrez.', 'lenteur2 poison'),
     ]},
]
