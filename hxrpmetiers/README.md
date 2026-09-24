# HxRP Métiers — Gourmet (0.5.0)

Mod Forge 1.12.2 (`modid` `hxrpmetiers`, Java 8) : cuisine, faim/soif et le métier de Gourmet du serveur HxRP.

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

## Compiler

```sh
./gradlew build          # produit build/libs/hxrpmetiers-0.5.0.jar
./gradlew runClient      # client de développement
```

À chaque push, GitHub Actions (`.github/workflows/hxrpmetiers.yml`) lance le banc d'essai, compile le mod,
vérifie le jar, puis démarre un vrai serveur Forge et un vrai client avec : le jar est joint à l'exécution
(artefact `hxrpmetiers-jar`).

Le nom du jar a changé (0.4.0 → 0.5.0) : si le launcher HxRP télécharge ce mod, mettre à jour son manifeste
(nom, taille, empreinte) en même temps que le jar publié.

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
