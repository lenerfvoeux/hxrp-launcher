# Prompt Claude Code — Métier Hunter Virus + mod Phénix (HxRP, Minecraft 1.12.2)

Tu vas développer le métier **Hunter Virus** pour le serveur roleplay **Hunter x Roleplay (HxRP)**, ainsi qu'un petit mod séparé, **le Phénix de feu**. Tout le game design est décidé : ce prompt et les fichiers du dossier `data/` sont le cahier des charges. Ne réinvente pas les règles ; si quelque chose manque ou se contredit, demande avant de trancher.

Langue du projet : **français** (textes en jeu, messages, noms d'objets, commentaires de haut niveau).

---

## 0. Contexte technique (à respecter strictement)

- **Minecraft 1.12.2**, **Forge 14.23.5.2847** (le serveur tourne en 14.23.5.2860), serveur **Mohist** (hybride Forge + Bukkit), **Java 8** (Zulu 8 côté client, OpenJ9 8 côté serveur).
- Le client utilise un **OptiFine patché** obligatoire et HBM. Les shaders posent déjà des conflits sur ce modpack : **n'utilise pas de post-process shader** pour les effets d'écran, fais des surcouches 2D (overlay HUD, teinte, vignette, mouvement de caméra).
- **GeckoLib 3 (geckolib-forge-1.12.2-3.0.31)** est déjà sur le serveur : utilise-le pour le modèle animé du Phénix.
- Mod d'identité existant : modid **`hunterxhunter`** (jar `hunterxrp-identity-server-2.5.1.jar` / client correspondant). Le **sexe du personnage** doit être lu depuis ce mod. Ouvre le jar pour trouver comment il stocke l'info (capability, NBT persistant ou API) et lis-la sans dépendance dure si possible (réflexion ou soft-dependency). Si l'info est absente : le joueur n'est éligible à aucune maladie réservée à un sexe.
- **Le Virus s'intègre au mod existant `hxrpmetiers`** (modid `hxrpmetiers`, package racine `fr.lenerfvoeux.hxrp.metiers`, un seul jar), dans le sous-package `fr.lenerfvoeux.hxrp.metiers.virus`, à côté du Hunter Gourmet. Réutilise ce qui existe déjà côté Gourmet : moteur de mini-jeux, péremption/fraîcheur, barres faim et soif (60 points chacune), ingrédients partagés.
- **Le Phénix est un mod séparé** : modid `hxrpphenix`, package `fr.lenerfvoeux.hxrp.phenix`.
- **Leçon apprise (bloquante)** : une version précédente a planté le serveur avec `NoSuchFieldError: BlockHorizontal.FACING` parce que le jar livré **n'était pas réobfusqué**. Avant toute livraison : `gradle clean build`, vérifier que la tâche `reobfJar` a tourné et que les classes référencent bien les noms SRG (`field_…` / `func_…`), puis tester le chargement sur un serveur **Mohist** si possible. Le jar doit être présent **côté client ET côté serveur**.

## 1. À retirer aussi du Gourmet

**Aucune XP** dans les métiers : retire le système d'XP du Hunter Gourmet (gain d'XP par plat, commande d'XP, affichages). Les rangs (0★ à 3★) sont donnés par commande admin en attendant la future licence Hunter.

---

## 2. Vue d'ensemble du métier

Le Hunter Virus est l'**apothicaire** du serveur. Il est le **seul** à pouvoir soigner (il peut se soigner lui-même).

Boucle : récolter → préparer à l'officine (plusieurs machines, chacune avec un mini-jeu) → soigner les blessures → diagnostiquer les maladies (prise de sang + microscope + écoute des symptômes + Grimoire) → traiter avec un remède spécifique en plusieurs prises.

**DA** : même DA que le Gourmet (pixel art détaillé, icônes 32×32, vue trois-quarts, gros pixels, contours noirs marqués, fond transparent, HUD minimal avec jauge laiton, temps restant, rang). Les machines ont le style des *Carnets de l'apothicaire* : fonte, céramique émaillée, bois laqué sombre, papier, cordelettes. Scènes de mini-jeu sur un plateau de bois laqué brun-rouge. **Aucun objet vanilla** dans le métier : tout est custom. Rien venant de l'End.

---

## 3. Règles du joueur

### 3.1 Groupe sanguin et sexe
- Groupe sanguin tiré à la **première connexion**, définitif, stocké côté serveur, parmi A+, A−, B+, B−, AB+, AB−, O+, O− avec **la même chance (12,5 %)**.
- Sexe lu depuis le mod Identity (voir §0).

### 3.2 Apparition des maladies
- **Aléatoire** : jauge cachée réglée pour qu'un joueur tombe malade **en moyenne tous les 3 à 5 jours réels** (configurable).
- **Plat périmé, étrange ou raté du Gourmet** : chance de tomber malade (configurable).
- **Commande admin** : `/virus maladie donner|soigner|info <joueur> [maladie] [stade]`.
- **Aucune contagion**.
- **Une seule maladie à la fois**. Après une guérison : **24 h réelles d'immunité**.
- Les restrictions « Touche uniquement » (groupes sanguins ou sexe) sont dans `virus_maladies.json`.

### 3.3 Stades
- 5 à 8 stades par maladie (dans le JSON). Progression **en temps réel côté serveur, même déconnecté** (timestamps absolus, comme la péremption du Gourmet). Le dernier stade arrive après `jours_avant_dernier_stade` ; répartis les stades intermédiaires régulièrement (configurable).
- À l'apparition : le **premier message** propre à la maladie (champ `premier_message`).
- Ensuite des **symptômes ponctuels** : message RP + effet. Chaque stade du JSON décrit les effets à appliquer.
- **On ne meurt jamais d'une maladie.** Le dernier stade (marqué « à vie ») applique des effets **permanents**, conservés même après guérison, effaçables seulement par le **Remède du Second Souffle** (§9).

### 3.4 Mort
**Mourir efface tout** : blessures, maladie en cours, effets à vie, immunité, traitement en cours. En RP, un joueur mort passe à un autre personnage. (Le groupe sanguin peut être retiré à la prochaine connexion du nouveau personnage : à confirmer avec moi, par défaut on le garde.)

### 3.5 Effets
Effets de base : lenteur I/II, faiblesse I/II, nausée, cécité courte, faim accélérée et soif accélérée (barres du Gourmet), sprint limité (5 ou 8 s) ou impossible, saut réduit ou impossible, dégâts légers ponctuels, récupération lente (régénération ÷2), cœurs max réduits.

Effets custom (rendus client, déclenchés par le serveur via paquet) :

| Effet | Rendu |
|---|---|
| Vision floue | brouillage intermittent de l'écran |
| Paupières lourdes | assombrissement par vagues, comme des clignements |
| Bords rouges | pulsation rouge en bordure (fièvre) |
| Toux / éternuement | le joueur est bloqué 1 s + son ; les joueurs proches voient le message RP |
| Tremblements | légère secousse de caméra |
| Mains tremblantes | le viseur dérive doucement |
| Vertige | la caméra tangue |
| Sensibilité à la lumière | écran surexposé en plein jour |
| Acouphènes | sons étouffés + sifflement |
| Hallucinations | faux sons de pas, silhouettes et particules locales au joueur |
| Vision tunnel | bords noircis |
| Daltonisme | teinte altérée (grise, ambrée, bleutée ou inversée selon la maladie) |
| Désorientation | gauche et droite inversées quelques secondes — **rare, derniers stades seulement** |
| Immobilité | impossible de bouger 3 à 5 s — rare, derniers stades |

---

## 4. Étoiles et qualité

| Rang | Bonus ajouté au total des notes | Valeurs lues à la prise de sang | Convalescence après la dernière prise | Maladies guérissables |
|---|---|---|---|---|
| 0★ | +0 | 2 | ~3 h | rang 0 |
| 1★ | +7 | 4 | ~1 h | ≤ 1 |
| 2★ | +14 | 6 | ~20 min | ≤ 2 |
| 3★ | +20 | 8 + marqueur rare | ~3 min | toutes |

- Mini-jeux **plus durs** à chaque étoile (flèches plus rapides, zones plus petites, moins de temps), comme le Gourmet.
- % affiché à la fin de chaque étape et à la fin de la préparation.
- **Note finale** = (somme des notes des étapes + bonus d'étoile) ÷ nombre d'étapes, plafonnée à 100. Exemple 3★ : (90 + 85 + 90 + 20) ÷ 3 = 95 %. Le bonus s'ajoute une seule fois, quel que soit le nombre d'étapes.
- **≥ 95 %** : fonctionne + un effet bonus propre au remède. **80–94 %** : fonctionne. **< 80 %** : objet « Préparation ratée », **inutilisable**. Même règle pour les soins de blessure.
- Fraîcheur : comme le Gourmet, les ingrédients frais périssent en temps réel et pénalisent la note de l'étape (75 % restant = 0, 50 % = −5, 25 % = −10, 1 % = −20). Poudres, teintures, sirops, huiles ne périssent plus.
- Un Virus en dessous du rang d'une maladie **ne peut pas la guérir** (le remède administré n'a pas d'effet) ; il transmet son carnet de consultation à un collègue.

---

## 5. Blessures (`data/virus_blessures.json`)

7 blessures, chacune à **4 stades**.
- Le premier dégât du bon type donne le stade 1 ; chaque nouveau dégât du même type **dans les 10 minutes** fait monter d'un stade (très grosse chute ou très gros coup : +2).
- Une blessure ne guérit jamais seule. **Au stade 4** l'effet est permanent tant qu'elle n'est pas soignée et s'alourdit toutes les 30 minutes.
- **Un seul soin par blessure**, valable à tous les stades ; aux stades 3–4 il faut en plus 15 min de repos (ni sprint ni combat) pour qu'il agisse.
- Soins posés **uniquement par un Virus**. La mort retire tout.
- Les attaques de feu du Phénix font monter la Brûlure d'un stade.

## 6. Ingrédients (`data/virus_ingredients.json`)

- 84 ingrédients custom : 34 plantes, 20 champignons, 15 ressources rares du monde Hunter, 15 ressources animales et de ruche. Chacun a sa source (biome, profondeur, moment) et sa rareté : implémente la génération / récolte en conséquence (plantes et champignons en blocs récoltables générés par biome, ruches sauvages, nids d'aigle-araignée sur les falaises, truffe à creuser, etc.). La Sève de l'Arbre-Monde n'est jamais générée : placée par les admins.
- **Pas de doublon avec le Gourmet** : les produits listés dans `repris_du_gourmet` sont les objets du Gourmet, réutilisés tels quels (vérifie les ids réels dans le code du Gourmet).
- Ressources animales = **drops ajoutés aux animaux du Gourmet** (plumes, duvet, laine, saindoux, os, coquille d'œuf, écailles, peaux, bois de cerf, défense de sanglier, carapace de crabe, encre de calamar).
- **Aucun minéral** sauf le sel du Gourmet. **Aucun objet vanilla.** Les fioles, seringues, pots, etc. sont des objets custom.
- `ingredients_prepares` : ingrédients qui se fabriquent à partir d'autres (koji, alcool de riz, alcool fort, vinaigre de riz, huile de lin, base d'onguent, bandes de laine, charbons).
- `formes_generiques` : les 9 formes de préparation (poudre, infusion, décoction, teinture, huile infusée, eau florale, sirop, pilules, élixir) et leur chaîne de machines.

## 7. Officine : machines, mini-jeux, administration

Tous les mini-jeux sont **au clic** (les gestes souris continus ont été abandonnés, trop bancals dans Minecraft), sur la base validée du Gourmet : flèche, zone verte, jauge laiton verticale à gauche, temps restant en haut à droite.

| Machine | Rôle | Mini-jeu |
|---|---|---|
| Yagen (meule en fonte dans son auge) | Poudres | La roue roule d'un bout à l'autre ; cliquer quand elle touche chaque extrémité, 6 allers-retours |
| Hachoir à levier | Trancher racines, écorces, champignons | Flèche dans la jauge ; clic dans la zone = coupe, 5 coupes |
| Chaudron sur brasero | Infusion, décoction, sirop, réduction, torréfaction à sec | La chaleur baisse seule ; cliquer pour attiser et rester dans la zone (douce / forte / à sec) |
| Alambic de cuivre | Eaux florales, alcools, concentrer un élixir | Deux jauges feu / refroidissement ; clic gauche ou droit pour les garder dans leur zone |
| Jarres de macération | Teintures, huiles, fermentations, vinaigre | Verser : maintenir le clic, relâcher sur le trait ; puis attente en temps réel (1 à 24 h) |
| Balance d'apothicaire | Doser (indispensable pour l'amanite) | Atteindre la masse cible avec des poids 1/2/5/10 g en un nombre limité de coups |
| Mortier et pilon | Pâtes, cataplasmes, gels | Cliquer quand le cercle se referme |
| Pilulier en bois | Pilules | La lame passe sur 6 rainures ; cliquer sur chacune |
| Table de préparation laquée | Onguents, baumes, bandages, filtrer | Mémoriser l'ordre des ingrédients affiché un instant, les replacer, puis étaler (flèche + zone) |

Éléments de diagnostic et de soin :
- **Meuble à tiroirs** : rangement (54 cases) ; fournit des **seringues vides à volonté** (pas besoin de les farmer).
- **Seringue** : prise de sang (trouver la veine — zone qui bouge le long du bras —, puis maintenir l'aspiration dans la zone 3 s) et injection des remèdes en seringue.
- **Microscope d'analyse** : molette = zoom / dézoom, clic gauche/droit = mise au point ; rendre l'image nette à ×10, ×40 et ×100 avant la fin du temps.

**Administration** (seul un Virus peut administrer) : fiole = 3 gorgées (clic dans la zone), seringue = veine puis injection, pilules = pas de mini-jeu, onguent/baume/cataplasme/bandage/attelle = étaler ou serrer en 3 passages. Une administration sous 50 % **gâche la dose**.

## 8. Diagnostic

1. Seringue vide prise dans le meuble.
2. Prise de sang sur le patient → seringue pleine au nom du patient. **Une prise toutes les 2 h par patient**, réussie ou non.
   - 80–100 % : toutes les valeurs du rang ; 50–79 % : la moitié, au hasard ; < 50 % : rien, seringue perdue.
3. Analyse au microscope : la seringue est consommée, résultat affiché **dans le chat** et inscrit dans le carnet. Sous 50 % : image floue, seringue perdue ; 50–79 % : une valeur illisible.
4. Le patient raconte ses symptômes en RP ; le Virus les coche dans le carnet.
5. Recherche dans le Grimoire, puis traitement.

Valeurs sanguines par rang : 0★ groupe sanguin + globules blancs ; 1★ + globules rouges + plaquettes ; 2★ + sucre + fer ; 3★ + toxines + parasites + marqueur rare. Notation dans le JSON : ↑ élevé, ↑↑ très élevé, ↓ bas, + présent, valeur non citée = normale.

- **Carnet de consultation** : un objet par patient (nom, sexe, groupe, valeurs lues, symptômes cochés, date de la dernière prise). Il se donne à un autre joueur, qui voit l'analyse.
- **Grimoire des maladies** : GUI livre pixel art, pages qui se tournent, toutes les maladies dès le départ, **une page par maladie, deux si besoin**. Deux index (par symptôme, par valeur sanguine), **sans correspondance automatique**. Pour une maladie au-dessus du rang du lecteur, le traitement est affiché « à l'encre délavée », illisible. Le Second Souffle n'y figure pas.

## 9. Maladies (`data/virus_maladies.json`) et traitements

52 maladies. Chaque entrée : `rang`, `touche_uniquement`, `premier_message`, `jours_avant_dernier_stade`, `stades`, `sang`, `remede_specifique` (nom, forme, recette), `accompagnement`, `traitement`, `consigne`.

- Les **recettes** sont écrites sous forme lisible (« ingrédient + ingrédient → Machine → Machine »). Convertis-les en données structurées (étapes ordonnées : machine, entrées, sortie, mode) dans des fichiers JSON chargés au démarrage, sous `config/hxrpmetiers/virus/` (modifiables par les admins sans recompiler). « voir maladie N » renvoie à la même préparation.
- **Traitement** : « Prise 2 (+3 h) » = 3 h après la prise précédente. Prévois une **tolérance** (par ex. ±30 min, configurable). Prise oubliée, hors fenêtre ou consigne non respectée ⇒ **le traitement échoue** et doit être recommencé.
- **Ordonnance** : objet papier remis au patient ; un petit HUD rappelle la prochaine prise et le temps restant.
- **Consignes** à vérifier côté serveur : à jeun (rien mangé), repos (ni sprint ni combat), dormir dans un lit, rester au chaud / près d'un feu, à l'ombre, ne pas se mouiller, pas d'alcool, ne pas aller sous/au-dessus d'une altitude, ne pas entrer dans une grotte / dans l'eau / dans le Nether, rester à moins de N blocs, manger un plat précis du Gourmet, prise de nuit, etc.
- Après la dernière prise réussie : **convalescence** selon le rang du Virus (§4), puis guérison et immunité 24 h.

### Remède du Second Souffle
- **Secret** : absent du Grimoire. Recette sur un **parchemin** (objet donné par un admin ou gagné en événement). Le parchemin **s'ouvre en GUI, présenté comme une page du Grimoire** : nom, effet, comment le fabriquer. Seul un Virus 3★ peut le lire et le fabriquer.
- Ingrédients : **un exemplaire de chacun des 84 ingrédients custom** + **une plume de phénix**.
- Étapes : 1) poudre des 34 plantes (Hachoir → Yagen → Balance) · 2) teinture des 20 champignons (+ alcool fort, Hachoir → Jarres 12 h) · 3) essence des 15 raretés (+ eau, Alambic) · 4) baume des 15 ressources animales et de ruche (+ base d'onguent, Yagen → Mortier → Table) · 5) plume de phénix au Chaudron à sec (cendre dorée) · 6) tout réunir : Balance → Jarres (24 h) → Alambic → seringue.
- Effet : retire **tous les effets à vie** du patient. Seuil 80 % ; raté = tout est perdu, plume comprise.

## 10. Mod Phénix de feu (`hxrpphenix`)

- **Apparition** uniquement via un **œuf de phénix** (item admin / récompense d'événement). Pas de spawn naturel.
- **Modèle GeckoLib** : grand oiseau de feu, ailes en flammes, longue queue de plumes, yeux incandescents, textures pixel art. Émet de la lumière, traînée de braises.
- **Vie** ~600 PV, barre de boss. Vole en cercle, cible tout joueur à moins de 24 blocs, plonge, se pose sur un perchoir, repart.
- **Ne détruit aucun bloc et n'enflamme rien** (constructions RP protégées).
- **Attaques** (beaucoup de particules : flammes, lave, braises, cendres) :
  - Boule de feu (explosion de flammes à l'impact, sans casser de blocs)
  - Souffle ardent (cône de flammes 3 s)
  - Pluie de plumes enflammées sur une zone marquée au sol
  - Plongée de braise (onde de choc + anneau de feu)
  - **Tempête de feu** (phase 2, sous 50 %) : tornade de flammes qui tourne autour de l'arène 10 s et aspire les joueurs vers le centre, particules en spirale
  - **Renaissance** (une fois, à 0 PV) : œuf de cendres ; 10 s pour le briser, sinon il renaît avec 30 % de vie
- Chaque coup de feu fait monter la blessure Brûlure du Virus d'un stade (dépendance souple : si `hxrpmetiers` est absent, ignorer).
- **Butin** : 1 à 2 **plumes de phénix** (ne brûlent pas, luisent dans l'inventaire). Rien d'autre.
- Commande admin : `/phenix invoquer`.

## 11. Commandes admin (permission niveau 2)

`/virus rang <joueur> <0-3>` · `/virus maladie donner|soigner|info <joueur> [maladie] [stade]` · `/virus blessure <joueur> <type> <stade>|aucune` · `/virus sang <joueur>` (voir le groupe) · `/virus immunite <joueur> <on|off>` · `/virus parchemin <joueur>` (donne le parchemin du Second Souffle) · `/phenix invoquer`.

## 12. Ordre de travail (une phase = une livraison testée en jeu)

1. **Socle** : données joueur persistantes (groupe sanguin, sexe via Identity, maladie, stades, effets à vie, immunité, blessures), effacement à la mort, retrait de l'XP du Gourmet, les 7 blessures à 4 stades, meuble à tiroirs, seringue, commandes.
2. **Officine** : les 9 machines (blocs + GUI + mini-jeux), administration, calcul de la note, seuil 80 %, ingrédients, génération dans le monde, drops animaux, ingrédients préparés, formes génériques, les 7 soins.
3. **Diagnostic** : prise de sang, microscope d'analyse, message chat, carnet de consultation, Grimoire (GUI) + 10 premières maladies (une à deux par famille).
4. **Catalogue complet** : les 52 maladies, effets custom côté client, ordonnance + HUD, consignes, convalescence, immunité.
5. **Phénix + Second Souffle** (parchemin + GUI).
6. **Visuels** : icônes 32×32 de tous les objets, modèles des machines façon *Carnets de l'apothicaire*, GUI du Grimoire, des mini-jeux, du parchemin, modèle du Phénix.

À la fin de chaque phase : build réobfusqué vérifié, liste de ce qui est testable en jeu, liste des commandes pour tester.

## 13. Points à me demander si besoin

- Ids exacts des objets du Gourmet à réutiliser (riz, miel, ail, menthe, thym, romarin, coriandre, citron, feuilles de thé, artichaut, lait, œuf, sel, bouteille d'eau) et liste réelle de ses animaux.
- Réglages fins (fréquence des maladies, chance d'être malade après un plat raté, tolérance des prises, vitesse des stades).
- Comportement du groupe sanguin après une mort (garder ou retirer).
