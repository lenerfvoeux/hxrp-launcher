# HxRP Phénix (1.0.0)

Mod Forge 1.12.2 séparé (`modid` `hxrpphenix`, Java 8) : **le Phénix de feu**, un boss volant animé avec
**GeckoLib 3** (`geckolib-forge-1.12.2-3.0.31`, déjà présent sur le serveur — il doit aussi l'être chez les joueurs).
Le jar va côté **client et serveur**. Il ne dépend pas de `hxrpmetiers`, mais s'en sert s'il est là.

## Le Phénix

- **Apparition** : uniquement avec un **œuf de phénix** posé au sol (objet admin / récompense d'événement), ou
  `/phenix invoquer`. Aucune apparition naturelle. Il éclot dans une gerbe de flammes ; le lieu d'éclosion
  devient **son arène**.
- **Modèle GeckoLib** : grand oiseau de feu (≈ 7 blocs d'envergure), ailes articulées en trois segments,
  rémiges en langues de feu, crête et aigrettes de flammes, longue queue avec quatre plumes de parade à ocelle,
  yeux incandescents. Quatre textures se relaient pour faire danser les flammes ; rendu **à pleine lumière**
  de jour comme de nuit ; le corps s'incline dans les piqués et prend du roulis dans les virages.
  Avec OptiFine, l'éclairage dynamique le fait **éclairer les alentours**. Traînée de braises en vol.
- **600 PV** (réglable), **barre de boss** (rouge, puis jaune quand il s'embrase, blanche pour l'œuf),
  visible à moins de 64 blocs.
- **Comportement** : tourne au-dessus de l'arène, prend pour cible tout joueur à moins de **24 blocs**
  (et celui qui le frappe), poursuit jusqu'à 48 blocs de l'arène puis revient. Après quelques attaques il
  **se pose sur un perchoir** (cime d'arbre, faîte de toit, rocher…) pendant quelques secondes — c'est le
  moment de le frapper à l'épée — puis repart d'un coup d'ailes qui repousse les joueurs collés à lui.
  Laissé seul, il se refait une santé.
- **Ne détruit aucun bloc et n'allume aucun feu** : ses attaques ne touchent que les joueurs (ni bêtes, ni PNJ,
  ni constructions). Les joueurs touchés brûlent deux secondes.

### Attaques (beaucoup de particules : flammes, lave, braises, cendres)

| Attaque | Ce qui se passe | Comment s'en sortir |
|---|---|---|
| **Boule de feu** | une boule (trois quand il est embrasé) qui explose en flammes à l'impact | l'esquiver, ou **la renvoyer d'un coup d'épée** : revenue sur lui, elle lui fait 20 dégâts |
| **Souffle ardent** | il prend son souffle (les flammes convergent vers le bec), puis crache un cône de feu pendant 3 s | il tourne lentement pendant le souffle : courir de côté, ou s'abriter derrière un mur |
| **Pluie de plumes enflammées** | un cercle de feu marque le sol autour d'un joueur, puis des plumes en flammes y tombent | sortir du cercle, ou se mettre sous un toit |
| **Plongée de braise** | il monte, crie, puis pique sur un joueur : onde de choc à l'impact, puis un **anneau de feu** qui s'élargit | s'éloigner du point d'impact, sauter par-dessus l'anneau ; il reste au sol 2 s (vulnérable) |
| **Tempête de feu** (sous 50 % de vie) | une tornade de flammes tourne autour de l'arène pendant 10 s et **aspire les joueurs vers le centre**, où il plane | courir contre l'aspiration en évitant la tornade |
| **Renaissance** (une fois, à 0 PV) | il s'effondre en **œuf de cendres** : 10 s pour le briser (60 PV) | frapper l'œuf à plusieurs ; sinon il renaît avec 30 % de vie, encore embrasé |

- **Hunter Virus** : chaque coup de feu fait monter la blessure **Brûlure** d'un stade (appel par réflexion
  à `VirusAPI.brulure` ; si `hxrpmetiers` est absent, rien ne se passe).
- **Butin** : 1 à 2 **plumes de phénix**, rien d'autre (ni expérience). La plume ne brûle pas (feu, lave,
  explosions), luit dans l'inventaire, rougeoie au sol et y reste une heure. C'est l'ingrédient
  `hxrpphenix:plume_de_phenix` du **Remède du Second Souffle** du Hunter Virus.

## Commandes (permission 2)

| Commande | Effet |
|---|---|
| `/phenix invoquer [x y z]` | fait éclore un Phénix (devant soi, aux coordonnées, ou — depuis la console — au point d'apparition du monde) |
| `/phenix attaque <boule\|souffle\|pluie\|plongee\|tempete\|perche>` | le Phénix le plus proche lance cette attaque tout de suite (mise en scène d'événement) |
| `/phenix info` | liste les Phénix chargés : PV, état, position |
| `/phenix retirer [rayon]` | retire les Phénix (et leurs projectiles), partout ou dans le rayon |
| `/phenix oeuf [joueur]` | donne un œuf de phénix |
| `/phenix plume [joueur] [nombre]` | donne des plumes de phénix |

Réglages dans `config/hxrpphenix.cfg` : points de vie, multiplicateur de dégâts, rayon de ciblage, taille de
l'arène, renaissance (oui/non, durée et PV de l'œuf, part de vie au retour), Brûlure du Virus, nombre de plumes.

## Tester en jeu

1. Serveur avec `geckolib-forge-1.12.2-3.0.31.jar`, `hxrpphenix-1.0.0.jar` (et `hxrpmetiers` pour la Brûlure) ;
   les mêmes jars côté client.
2. `/phenix oeuf` puis poser l'œuf dans un grand espace dégagé (ou `/phenix invoquer`), passer en survie.
3. Observer : ronde, attaques, perchoir. `/phenix attaque tempete` pour voir la tempête tout de suite,
   `/kill @e[type=hxrpphenix:phenix]` pour déclencher l'œuf de cendres (un second `/kill` le brise).
4. Récupérer les plumes, les jeter dans la lave : elles restent.

## Fabrication

- `python3 tools/phenix/phenix.py` : squelette, peinture procédurale des plumes (4 images), animations
  (`assets/hxrpphenix/geo`, `animations`, `textures/entity`). `python3 tools/phenix/phenix.py apercu` : planches
  d'aperçu dans `tools/apercu/`, calculées exactement comme GeckoLib (axes, pivots, ordre des rotations, UV).
- `python3 tools/phenix/icones.py` : icônes 32×32 (moteur de pixel art du Gourmet), boule de feu et plume enflammée.
- `python3 tools/phenix/sons.py` : 13 sons synthétisés (Ogg Vorbis via ffmpeg) et `sounds.json`.
- `python3 tools/phenix/verifier.py` : cohérence modèle / animations / Java / sons / traductions (lancé par la CI).

## Compiler

```sh
./gradlew build      # build/libs/hxrpphenix-1.0.0.jar (réobfusqué, noms SRG)
```

À chaque push, `.github/workflows/hxrpphenix.yml` vérifie les ressources, récupère GeckoLib 3.0.31, compile,
contrôle le jar (noms SRG, Java 8, GeckoLib non embarqué), puis démarre un vrai serveur Forge avec un scénario
(éclosion, tempête de feu, perchoir, œuf de cendres, renaissance, attaques sans cible, retrait) et un vrai client.
Le jar est joint à l'exécution (artefact `hxrpphenix-jar`, avec le jar GeckoLib utilisé pour le test).
