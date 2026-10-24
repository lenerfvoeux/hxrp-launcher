# HxRP Phénix (1.1.0)

Mod Forge 1.12.2 séparé (`modid` `hxrpphenix`, Java 8) : **le Phénix de feu**, un boss volant animé avec
**GeckoLib 3** (`geckolib-forge-1.12.2-3.0.31`, déjà présent sur le serveur — il doit aussi l'être chez les joueurs).
Le jar va côté **client et serveur**. Il ne dépend pas de `hxrpmetiers`, mais s'en sert s'il est là.

## Le Phénix

- **Apparition** (aucune apparition naturelle) :
  - **œuf de phénix** posé au sol, ou `/phenix invoquer` : il éclot sur place dans une gerbe de flammes ;
  - **nid de braises**, son **appât** (donné par un admin, `/phenix appat`) : posé au sol à l'air libre, il fume
    de plus en plus ; un cri perçant vient du ciel, puis le Phénix surgit à **40-60 blocs**, hors de vue (dans le
    dos du joueur le plus proche), **30 à 90 s** après la pose, descend **en tournoyant**, se pose sur le nid,
    le dévore et attaque. Le nid est incassable en survie ; s'il n'est pas dévoré, il s'éteint au bout de 2 min.

  Le lieu d'éclosion, ou le nid, devient **son arène**.
- **Modèle GeckoLib** : grand oiseau de feu (≈ 7 blocs d'envergure), ailes articulées en trois segments,
  rémiges en langues de feu, crête et aigrettes de flammes, longue queue avec quatre plumes de parade à ocelle,
  yeux incandescents. Quatre textures se relaient pour faire danser les flammes ; rendu **à pleine lumière**
  de jour comme de nuit ; le corps s'incline dans les piqués et prend du roulis dans les virages.
  Avec OptiFine, l'éclairage dynamique le fait **éclairer les alentours**. Traînée de braises en vol.
- **3000 PV, armure 22** (classeur des boss, voir plus bas). **Barre de boss** visible à moins de 64 blocs :
  **rouge** en phase 1, **orange** en phase 2 (le serveur envoie du jaune, que le client repeint en orange),
  **violette** en phase 3, blanche pour l'œuf.
- **Trois phases** :
  1. jusqu'à 50 % de vie ;
  2. sous 50 % : il **s'embrase** (cri, tempête de feu), plus rapide, plus fort, boules de feu en rafale ;
  3. à 0 PV : **œuf de cendres** ; s'il n'est pas brisé à temps, il **renaît avec toute sa vie**, encore plus
     enragé, et lâche une **nova solaire** toutes les 20 s.
- **Comportement** : tourne au-dessus de l'arène, prend pour cible tout joueur à moins de **24 blocs**
  (et celui qui le frappe), poursuit jusqu'à 64 blocs de l'arène puis revient. Après quelques attaques il
  **se pose sur un perchoir** (cime d'arbre, faîte de toit, rocher…) pendant quelques secondes — c'est le
  moment de le frapper à l'épée — puis repart d'un coup d'ailes qui repousse les joueurs collés à lui.
  Laissé seul, il se refait une santé.
- **Ne détruit aucun bloc et n'allume aucun feu** : ses attaques ne touchent que les joueurs (ni bêtes, ni PNJ,
  ni constructions). Les joueurs touchés brûlent quelques secondes (durée de l'effet, dans le classeur).

### Attaques (beaucoup de particules : flammes, lave, braises, cendres)

| Attaque | Ce qui se passe | Comment s'en sortir |
|---|---|---|
| **Boule de feu** | une boule (trois quand il est embrasé) qui explose en flammes à l'impact | l'esquiver, ou **la renvoyer d'un coup d'épée** : revenue sur lui, elle lui inflige ses propres dégâts |
| **Souffle ardent** | il prend son souffle (les flammes convergent vers le bec), puis crache un cône de feu pendant 3 s | il tourne lentement pendant le souffle : courir de côté, ou s'abriter derrière un mur |
| **Pluie de plumes enflammées** | un cercle de feu marque le sol autour d'un joueur, puis des plumes en flammes y tombent | sortir du cercle, ou se mettre sous un toit |
| **Plongée de braise** | il monte, crie, puis pique sur un joueur : onde de choc à l'impact, puis un **anneau de feu** qui s'élargit | s'éloigner du point d'impact, sauter par-dessus l'anneau ; il reste au sol 2 s (vulnérable) |
| **Tempête de feu** (phase 2, puis toutes les 30 s) | une tornade de flammes tourne autour de l'arène pendant 10 s et **aspire les joueurs vers le centre**, où il plane | courir contre l'aspiration en évitant la tornade |
| **Renaissance** (une fois, à 0 PV) | il s'effondre en **œuf de cendres** : 10 s pour le briser (60 PV) | frapper l'œuf à plusieurs ; sinon il renaît avec **toute sa vie** (phase 3) |
| **Nova solaire** (phase 3, toutes les 20 s) | il fond vers sa cible, se ramasse ailes levées en aspirant les flammes pendant 1 s, puis **explose** : tout joueur à moins de 15 blocs est brûlé et projeté au loin | s'éloigner pendant la charge, ou se mettre derrière un mur |

- **Hunter Virus** : chaque coup de feu fait monter la blessure **Brûlure** d'un stade (appel par réflexion
  à `VirusAPI.brulure` ; si `hxrpmetiers` est absent, rien ne se passe).
- **Butin** : 1 à 2 **plumes de phénix**, rien d'autre (ni expérience). La plume ne brûle pas (feu, lave,
  explosions), luit dans l'inventaire, rougeoie au sol et y reste une heure. C'est l'ingrédient
  `hxrpphenix:plume_de_phenix` du **Remède du Second Souffle** du Hunter Virus.

## Commandes (permission 2)

| Commande | Effet |
|---|---|
| `/phenix invoquer [x y z]` | fait éclore un Phénix (devant soi, aux coordonnées, ou — depuis la console — au point d'apparition du monde) |
| `/phenix attaque <boule\|souffle\|pluie\|plongee\|tempete\|perche\|nova>` | le Phénix le plus proche lance cette attaque tout de suite (mise en scène d'événement) |
| `/phenix info` | liste les Phénix chargés : PV, phase, état, position ; et les réglages en vigueur |
| `/phenix retirer [rayon]` | retire les Phénix (et leurs projectiles), partout ou dans le rayon |
| `/phenix oeuf [joueur]` | donne un œuf de phénix |
| `/phenix appat [joueur]` | donne un nid de braises (l'appât) |
| `/phenix plume [joueur] [nombre]` | donne des plumes de phénix |
| `/phenix recharger` | relit `config/bestiaire_stats.xlsx` et met à jour les Phénix déjà là |

## Réglages : le classeur des boss

Vie, armure, robustesse, recul, vitesse, multiplicateurs de dégâts et de portée, pause entre attaques et vie
reprise de chaque phase (onglet **Boss**), dégâts, portée, rayon, recharge, durée du feu et fréquence de chaque
attaque (onglet **Attaques**), délais et distances de l'appât (onglet **Appâts**) et plumes laissées (onglet
**Butins**) : tout est lu dans **`config/bestiaire_stats.xlsx`**, le même fichier que le futur mod
`hxrpbestiaire` (lignes `phenix`). S'il manque, il est créé avec la copie rangée dans le jar ; une case vide
garde la valeur par défaut ; un fichier illisible laisse les valeurs du jar (le journal dit pourquoi). Relu au
démarrage du serveur et par `/phenix recharger`, sans recompiler. Les valeurs sont provisoires et volontairement
hautes : l'équilibrage se fera en test.

- Dégâts infligés = dégâts de l'attaque × multiplicateur de la phase (une boule de feu : 30 × 3 = 90 en phase 1).
- La vie vanilla plafonne à 1024 : le Phénix tient la sienne en unités de 3 PV (pour 3000 PV) et divise les
  dégâts reçus d'autant **après** l'armure ; `/phenix info` affiche les vrais PV.

`config/hxrpphenix.cfg` ne garde que l'œuf de cendres (durée, PV) et la Brûlure du Virus.

## Tester en jeu

1. Serveur avec `geckolib-forge-1.12.2-3.0.31.jar`, `hxrpphenix-1.1.0.jar` (et `hxrpmetiers` pour la Brûlure) ;
   les mêmes jars côté client.
2. `/phenix appat`, poser le nid dans un grand espace dégagé, passer en survie et attendre 30 à 90 s
   (ou `/phenix oeuf`, ou `/phenix invoquer`).
3. Observer : arrivée en spirale, ronde, attaques, perchoir. `/phenix attaque tempete` pour voir la tempête tout
   de suite, `/kill @e[type=hxrpphenix:phenix]` pour déclencher l'œuf de cendres (barre blanche) : attendre 10 s
   pour la phase 3 (barre violette) et `/phenix attaque nova`. La barre orange apparaît sous 50 % de vie.
4. Récupérer les plumes, les jeter dans la lave : elles restent.

## Fabrication

- `python3 tools/phenix/phenix.py` : squelette, peinture procédurale des plumes (4 images), animations
  (`assets/hxrpphenix/geo`, `animations`, `textures/entity`). `python3 tools/phenix/phenix.py apercu` : planches
  d'aperçu dans `tools/apercu/`, calculées exactement comme GeckoLib (axes, pivots, ordre des rotations, UV).
- `python3 tools/phenix/icones.py` : icônes 32×32 (moteur de pixel art du Gourmet), boule de feu et plume enflammée.
- `python3 tools/phenix/sons.py` : 14 sons synthétisés (Ogg Vorbis via ffmpeg) et `sounds.json`.
- `python3 tools/phenix/nid.py [apercu.png]` : textures, modèle de bloc et d'objet du nid de braises.
- `python3 tools/phenix/verifier.py` : cohérence modèle / animations / Java / sons / traductions / bloc / classeur
  des boss (lancé par la CI).
- `python3 tools/test/classeur.py source.xlsx sortie.xlsx "Onglet|colonne=valeur|en-tête=nouvelle valeur"` :
  copie un classeur en changeant des cases (le test de démarrage s'en sert pour un appât pressé).

## Compiler

```sh
./gradlew build      # build/libs/hxrpphenix-1.1.0.jar (réobfusqué, noms SRG)
```

À chaque push, `.github/workflows/hxrpphenix.yml` vérifie les ressources, récupère GeckoLib 3.0.31, compile,
contrôle le jar (noms SRG, Java 8, GeckoLib non embarqué), puis démarre un vrai serveur Forge avec un scénario
(éclosion, tempête de feu, perchoir, œuf de cendres, renaissance en phase 3, nova solaire, attaques sans cible,
retrait, relecture du classeur, nid de braises : arrivée du Phénix qui le dévore) et un vrai client.
Le jar est joint à l'exécution (artefact `hxrpphenix-jar`, avec le jar GeckoLib utilisé pour le test).
