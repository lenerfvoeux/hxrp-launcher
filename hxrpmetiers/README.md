# HxRP Métiers — Gourmet et Hunter Virus (0.6.0)

Mod Forge 1.12.2 (`modid` `hxrpmetiers`, Java 8, jar côté client **et** serveur) : cuisine, faim/soif et le métier
de Gourmet, et depuis 0.6.0 le métier de **Hunter Virus** (apothicaire : blessures, maladies, officine, diagnostic).
Le boss Phénix de feu est un mod séparé : voir `../hxrpphenix`.

## Hunter Virus (0.6.0)

Le Hunter Virus est le seul à pouvoir soigner (lui-même compris). Rangs 0★ à 3★ donnés par commande ;
**l'XP du Gourmet est retirée** (gain par plat, commande et affichages).

- **Santé des joueurs** (données persistantes côté serveur, effacées à la mort) : groupe sanguin tiré à la première
  connexion (8 groupes, 12,5 % chacun), sexe lu dans le mod Identity (`hunterxhunter`, par réflexion ; absent = aucune
  maladie réservée à un sexe), maladie et stade en temps réel même déconnecté, effets à vie, immunité de 24 h après guérison.
- **7 blessures à 4 stades** (contusions, commotion, jambe, plaie, brûlure, morsure, gelure) déclenchées par les chutes,
  coups, combats, feu, froid… avec leurs effets (ralentissements, vision trouble, saignements…), soignées par les 7 soins.
- **52 maladies** (`config/hxrpmetiers/virus/*.json`, modifiables sans recompiler) : apparition au hasard (3 à 5 jours en
  moyenne), après un plat raté, étrange ou périmé du Gourmet, ou par commande ; 5 à 8 stades ; effets en surcouches 2D
  (teinte, vignette, flou, tremblements de caméra — aucun shader). On n'en meurt jamais : le dernier stade laisse des
  effets à vie. Mourir efface tout (blessures, maladie, effets à vie, immunité, traitement), sauf le groupe sanguin.
- **Officine** : 9 machines posables façon *Carnets de l'apothicaire* (yagen, hachoir à levier, chaudron sur brasero,
  alambic de cuivre, jarres de macération, balance d'apothicaire, mortier et pilon, pilulier, table de préparation laquée),
  microscope d'analyse, Grimoire des maladies sur son lutrin, présentoir, meuble à tiroirs. Chaque machine a son mini-jeu
  (même moteur que le Gourmet : graine tirée par le serveur, partie rejouée et notée par le serveur) ; seuil de réussite 80 %,
  qualité et étoiles selon la note et le rang. Formulaire de l'officine : recettes visibles selon le rang, ingrédients
  manquants, étapes et machines.
- **Récolte** : les **84 ingrédients** ont une source en jeu — 52 plantes et champignons au sol (trois stades, replantables),
  champignons de tronc, sols, troncs écorcés, nids, lotus, sève, butins d'animaux — et les **119 préparations** ont
  leur icône (couleur tirée des ingrédients, bouchon selon le rang).
- **Diagnostic** : seringue (prise de sang, mini-jeu), microscope (lecture du sang), écoute des symptômes, carnet de
  consultation à cocher, Grimoire (sommaire, index des symptômes et du sang).
- **Traitement** : remède spécifique en plusieurs prises (fenêtre de ± 30 min), **ordonnance** remise au patient avec un HUD
  qui rappelle la prochaine prise ; **consignes vérifiées par le serveur** (à jeun, repos, dormir, au chaud, à l'ombre,
  pas d'alcool, altitude, grotte, eau, Nether, distance, plat précis du Gourmet, prise de nuit…) : une prise oubliée ou une
  consigne non tenue fait échouer le traitement ; puis convalescence selon le rang du Virus, guérison et immunité.
- **Remède du Second Souffle** (secret, Virus 3★) : recette sur un **parchemin** qui s'ouvre comme une page du Grimoire ;
  un exemplaire de chacun des 84 ingrédients et une **plume de phénix** (`hxrpphenix:plume_de_phenix`) ; efface tous les
  effets à vie. Le Phénix de feu fait aussi monter la Brûlure à chaque coup de feu (`VirusAPI.brulure`).

### Commandes du Virus (permission 2)

| Commande | Effet |
| --- | --- |
| `/virus rang <joueur> <0-3\|aucun>` | fait du joueur un Hunter Virus de ce rang (ou le retire) |
| `/virus maladie donner <joueur> <maladie> [stade]` · `soigner <joueur>` · `info <joueur>` | maladies |
| `/virus blessure <joueur> <type> <0-4\|aucune>` | pose ou retire une blessure |
| `/virus sang <joueur>` | groupe sanguin |
| `/virus immunite <joueur> <on\|off>` | immunité |
| `/virus traitement <joueur> [annuler\|avancer]` | voir, annuler ou avancer à la prise suivante |
| `/virus avie <joueur> [retirer]` | effets à vie |
| `/virus parchemin <joueur>` | parchemin du Second Souffle |
| `/virus donner <joueur> <objet> [quantité] [qualité]` | n'importe quel objet du Virus (ingrédient, préparation…) |
| `/virus jarres` | termine les macérations des jarres à moins de 8 blocs |
| `/virus prise <joueur>` | autorise une nouvelle prise de sang tout de suite |
| `/virus recharger` | relit les JSON de `config/hxrpmetiers/virus/` |

### Tester en jeu

1. `/virus rang <soi> 3`, `/virus donner <soi> …` pour les ingrédients (ou les récolter), poser les machines et la table.
2. Ouvrir le formulaire à la table de préparation, lancer une préparation, jouer les mini-jeux machine par machine
   (`/virus jarres` pour ne pas attendre les macérations).
3. Sur un autre joueur : `/virus blessure <joueur> plaie 2`, le soigner avec le bon soin (mini-jeu d'administration).
4. `/virus maladie donner <joueur> <maladie>`, prise de sang à la seringue, analyse au microscope, cocher le carnet,
   chercher dans le Grimoire, préparer le remède, l'administrer et suivre l'ordonnance (`/virus traitement … avancer`).
5. `/virus parchemin <soi>` pour lire le Second Souffle (3★ seulement).

## Ce qui a changé depuis 0.4.0

- **Icônes** : les 361 textures d'aliments sont redessinées en pixel art 32×32. Chaque ingrédient,
  épice, préparation, plat et boisson a sa propre forme et sa propre palette (pas de recoloriage d'un modèle commun).
- **Stations en 3D** : les 12 stations et le frigo/la poubelle sont de vrais modèles à éléments
  (four à porte vitrée et thermostat, fourneau à brûleurs, marmite fumante, friteuse avec panier, grill à braises, etc.).
  Ils s'orientent vers le joueur à la pose, et leur boîte de sélection suit la forme et l'orientation du modèle.
  Le grill et la marmite émettent un peu de lumière.
- **Mini-jeux** : 17 gestes réécrits (Couper, Étaler, Pétrir, Façonner, Fouetter, Mélanger, Piler, Tamiser, Saisir,
  Bouillir, Four, Griller, Frire, Mijoter, Presser, Secouer, Assaisonner). Chaque scène est dessinée en vue 3/4 sur fond
  transparent, avec la jauge verticale en laiton commune, et montre l'aliment réel de la recette.
  Seules des entrées fiables sont utilisées (clics au bon moment, cibles, maintenir/relâcher, ZQSD/flèches, espace, molette).
  Le rang de Gourmet ne change que la vitesse, la largeur des zones, le nombre de répétitions et le délai de réaction.
- **Anti-triche** : le serveur tire la graine du mini-jeu, le client joue une simulation déterministe à pas fixe
  (10 ms) et envoie le journal horodaté de ses entrées ; le serveur rejoue la partie et calcule la note lui-même.
  Une partie simulée plus longue que le temps réel (client accéléré) est plafonnée à 50 %, une partie jouée
  au ralenti à 75 %, et un journal incohérent vaut 0.
- **Note pondérée** : les étapes décisives (saisir, four, griller) pèsent plus que les gestes d'appoint (tamiser, assaisonner).
- **Interfaces** : carnet de recettes en livre (onglets Réalisables/Toutes, ingrédients manquants, étapes et stations,
  bouton Cuisiner), frigo, poubelle et barres de faim/soif redessinés.

## Récolter les ingrédients dans le monde (0.5.0)

Chaque ingrédient et chaque épice a une source en jeu (décrite dans `tools/monde/especes.py`) :

- **69 cultures** : graines en sachet (trouvées en cassant les hautes herbes, selon le climat du biome),
  à semer sur de la terre labourée ; trois stades visibles (vient d'être planté, pousse, prêt).
  Clic droit sur une plante mûre pour récolter sans l'arracher.
- **22 arbres fruitiers** (pommier, oranger, cocotier, cacaoyer…) générés selon le climat : leurs feuilles
  fleurissent puis portent des fruits ; **taper** sur un bloc de feuilles mûr fait tomber les fruits
  sans casser l'arbre. Les feuilles donnent des pousses à replanter.
- **14 animaux** : saumon, truite, thon, cabillaud, sardine, maquereau, anchois (en bancs), crevette,
  crabe (plages), dinde, canard, cerf (farouche), sanglier (charge si on l'attaque), chèvre (se trait à la fiole).
  Les animaux vanilla donnent aussi nos viandes (bœuf, veau, porc, poulet, mouton, lapin, calamar) ;
  les vaches se traient à la fiole, les poules pondent des œufs.
- **Blocs** : minerai de sel (couches 20 à 70), ruches sauvages sur les troncs (miel à la fiole),
  bancs de moules et d'huîtres au bord de la mer.
- **Artisanat** : sucre, huile d'olive, vinaigre, sauce soja, levure, paprika, bouteille d'eau.

`python3 tools/monde/gen_monde.py` régénère textures, modèles, états de blocs, noms, recettes,
modèles d'animaux (`data/modeles/*.json`) et `data/recolte.json`.

## Compiler

```sh
./gradlew build          # produit build/libs/hxrpmetiers-0.6.0.jar
./gradlew runClient      # client de développement
```

À chaque push, GitHub Actions (`.github/workflows/hxrpmetiers.yml`) lance le banc d'essai, compile le mod,
vérifie le jar, puis démarre un vrai serveur Forge et un vrai client avec : le jar est joint à l'exécution
(artefact `hxrpmetiers-jar`).

Le nom du jar change à chaque version (0.5.0 → 0.6.0) : le launcher HxRP télécharge ce mod depuis la release
`hxrp-extras-v…` décrite dans `manifest.json` ; publier le nouveau jar dans une release puis mettre à jour le manifeste
(nom, taille, ancien nom dans `oldNames`) en même temps.

## Outils (Python 3 + Pillow + NumPy)

Tous les visuels sont générés par des scripts, pour pouvoir les retoucher et les régénérer à l'identique.

| Commande | Produit |
| --- | --- |
| `python3 tools/textures/gen_items.py` | `textures/items/*.png` + champs `famille`, `pal`, `motif` de `data/food.json` (utilisés par les mini-jeux) |
| `python3 tools/textures/gen_gui.py` | `textures/gui/carnet.png`, `frigo.png`, `poubelle.png`, `hud.png` |
| `python3 tools/models/gen_blocks.py` | textures, modèles et états de blocs des stations, du frigo et de la poubelle |
| `sh tools/test/banc.sh` | banc d'essai des mini-jeux hors Minecraft + images des scènes |
| `bash tools/test/demarrage.sh serveur <jar>` | installe un vrai serveur Forge 1.12.2, y démarre le mod et vérifie le journal |
| `xvfb-run bash tools/test/demarrage.sh client` | démarre le client obfusqué avec le mod et vérifie modèles et textures |

Les aperçus (planches d'icônes, rendus des blocs, images des mini-jeux) sont écrits dans `tools/preview/out/`, ignoré par git.

`tools/test/banc.sh` vérifie pour chaque geste et chaque rang : qu'un joueur parfait obtient ~100, qu'un joueur
qui joue au hasard reste sous 60, qu'un joueur inactif obtient 0, que le rejeu serveur redonne exactement la même
note, et que les journaux trafiqués (temps décroissants, hors de la grille de 10 ms, entrées après la fin,
partie plus longue que sa durée maximale) sont rejetés.

## Organisation du code

- `minijeu/` — logique commune client/serveur des mini-jeux (`MiniJeu`, `Jeux`, `Journal`), sans dépendance à Minecraft.
- `client/minijeu/` — dessin des scènes sur une toile de pixels (`Toile`, `Police`, `Dessin`, `Scenes`, …) ; seule `ToileGL` touche à OpenGL.
- `cuisine/Seances` — séances ouvertes côté serveur (graine, horloge, vérification du rejeu).
- `block/BlockOriente` — base des blocs à modèle 3D orienté.
