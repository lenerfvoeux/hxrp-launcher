# HxRP Bestiaire (0.3.1)

Mod Forge 1.12.2 séparé (`modid` `hxrpbestiaire`, Java 8) : **les boss de HxRP**, animés avec **GeckoLib 3**
(`geckolib-forge-1.12.2-3.0.31`, déjà présent sur le serveur — il doit aussi l'être chez les joueurs).
Le jar va côté **client et serveur**. Il ne dépend ni de `hxrpmetiers` ni de `hxrpphenix`, mais s'en sert s'ils sont là.

Trois lots sont là : **la Reine-guêpe, l'Abomination et le Nécromancien**, puis **Xerra, le Roi des glaces et le
Démon noir**, puis **le Kraken, le Calamar des sables, le Démon de lave et Aï**. La Baleine est reportée.

## Ce que les boss ont en commun

- **Aucune apparition naturelle.** Un boss vient :
  - par son **appât** (bloc donné par un admin, `/<boss> appat`), posé là où il le faut : il s'anime de plus en
    plus, un **signe** prévient de l'arrivée (message et son venu de la direction d'arrivée), puis, **30 à 90 s**
    après la pose, le boss arrive de **40 à 60 blocs** (en vol ou à pied, dans le dos du joueur le plus proche) ou
    surgit sur place, **dévore l'appât** et attaque. Les appâts sont incassables en survie ; un appât dont le boss
    a été tué ou retiré en route se défait au bout de 2 min ;
  - par son **œuf** (admins, `/<boss> oeuf`) ou `/<boss> invoquer` : il apparaît sur place.

  Le lieu d'apparition (ou l'appât) devient **son arène** : il prend pour cible tout joueur à moins de 24 blocs
  (et celui qui le frappe), le poursuit jusqu'à 64 blocs de l'arène, puis revient. Laissé seul, il se refait une santé.
- **Barre de boss** à moins de 64 blocs : **rouge** en phase 1, **orange** en phase 2 (le serveur envoie du jaune,
  que le client repeint en orange), **violette** en phase 3.
- **Phase 2 sous 50 % de vie** : il entre en rage (cri, explosion), plus rapide, plus fort, des attaques en plus
  et des pauses plus courtes. Les boss à **trois phases** (Phénix, Xerra, Kraken, Roi des glaces, Démon noir)
  s'effondrent à 0 PV et **se relèvent avec toute leur vie**.
- **Sbires** : des versions réduites du boss ou de ses serviteurs. Ils **disparaissent quand leur boss meurt**
  (ou est retiré). Plus nombreux à chaque phase (multiplicateurs du classeur).
- **Ne détruisent aucun bloc** ; leurs attaques ne touchent que les joueurs. Les effets (poison, lenteur, recul…)
  et leur durée viennent du classeur.
- **Butin** : les objets du bestiaire (onglet Butins du classeur), rien d'autre.
- Les boss ne craignent ni le poison ni le flétrissement, ni les dégâts de leurs propres sbires.

## Lot 1

### Reine-guêpe — 4000 PV, armure 22

Une reine des guêpes géante (≈ 5 blocs), **volante**, qui tourne autour de sa cible.
**Appât : rayon de miel royal**, à poser **en jungle ou en forêt**. Signe : « Un bourdonnement sourd monte de la
forêt… » ; elle arrive en vol et fond sur le rayon.

| Attaque | Ce qui se passe |
|---|---|
| **Taille** droite, gauche, haute, basse | un coup de dard du côté où se tient le joueur |
| **Hachage** | trois coups de dard en rafale devant elle |
| **Onde d'air** | un battement d'ailes qui repousse tout ce qui est devant elle (18 blocs) |
| **Essaim** | elle appelle 3 **guêpes soldats** (6 en phase 2), dont la piqûre empoisonne 4 s |
| **Regard brûlant** (phase 2) | immobile, elle balaie l'arène d'un rayon rouge qui suit lentement sa cible et enflamme |
| **Rafale de dards** (phase 2) | cinq dards empoisonnés en éventail |
| **Piqué** (phase 2) | elle monte au-dessus de sa cible puis fonce dard en avant : choc et poison à l'impact |

Butin : 1-2 **dards de reine**, 2-4 **gelées royales noires**, 3-6 **miels noirs**.

### Abomination, porteuse de Zobae — 3000 PV, armure 22

Un amas de chair qui marche. **Appât : charnier**, à poser **sous terre** (pas de ciel au-dessus). Signe :
« Des mouches tournent au-dessus du charnier… » ; elle **jaillit du sol** sous le charnier.

| Attaque | Ce qui se passe |
|---|---|
| **Coups** | griffes au contact ; **20 % de chances (40 % en phase 2) de transmettre une maladie du Hunter Virus** |
| **Bond** | elle se ramasse, saute sur sa cible et écrase tout autour en retombant |
| **Enfouissement** (phase 2) | elle s'enfonce dans le sol (intouchable), puis **ressort dans le dos** de sa cible et frappe autour d'elle |
| **Vomissure** (phase 2) | une flaque de bile empoisonnée là où se tient sa cible |

Butin : 1-2 **chairs d'abomination**.

### Nécromancien — 3500 PV, armure 22

Un mage squelettique de près de 7 blocs qui **flotte**, bâton à la main. **Appât : cercueil profané**, à poser au sol.
Signe : « Des mains décharnées sortent de terre autour du cercueil… » ; il **se matérialise** au-dessus du cercueil.

| Attaque | Ce qui se passe |
|---|---|
| **Coup de bâton** | au contact |
| **Frappe au sol** | il abat son bâton : onde de choc à 10 blocs, recul et lenteur |
| **Levée des morts** | 3 **archers squelettes** (6 en phase 2) sortent de terre ; **tant qu'ils vivent, ils le soignent** |
| **Boules d'âme** (phase 2) | trois âmes qui poursuivent leur cible |
| **Téléportation** (phase 2) | il disparaît et réapparaît ailleurs dans l'arène |

Butin : 2-4 **poussières d'os maudites**.

## Lot 2

### Xerra, mère-araignée — 4500 PV, trois phases

Une araignée géante (≈ 7 × 4 × 5 blocs) chevauchée par une femme pâle. **Appât : cocon de proie**, à poser **sous
terre**. Signe : « Des araignées sortent des murs, la soie tremble au plafond… » ; elle **descend du plafond** au bout
de son fil, droit sur le cocon, et le dévore.

| Attaque | Ce qui se passe |
|---|---|
| **Coup de patte** | balaye devant elle |
| **Griffures** | quatre lacérations qui font **saigner** |
| **Rugissement** | ralentit tous les joueurs proches |
| **Couvée** | 4 **araignées de Xerra** (des Xerra en petit) |
| **Jet de toile** | une boule de soie qui **immobilise** 3 s et laisse une **toile** là où elle touche |
| **Graines-pièges** (phase 2) | des bulbes plantés autour de la cible : qui marche dessus est **enraciné** |
| **Course** (phase 2) | elle fonce et percute |
| **Bond** (phase 2) | elle saute sur un joueur éloigné et écrase tout autour |
| **Renaissance** (à 0 PV) | elle s'effondre, puis se relève avec **toute sa vie** (phase 3) |
| **Grande couvée** (phase 3) | 12 araignées, et l'arène se couvre de **toiles** |

En phase 3, quand elle est blessée, **ses araignées la soignent** (2 % de sa vie chacune) : après 5 à 12 s de combat,
elles courent la rejoindre et se fondent en elle. Il faut les tuer d'abord.
**Les toiles brûlent** (feu, briquet) : c'est ainsi qu'on libère le terrain ; elles se défont aussi seules en une
minute environ. Les toiles et les graines ne remplacent jamais un bloc et ne lâchent rien.
Butin : 2-4 **soies de Xerra**.

### Roi des glaces — 4000 PV, trois phases

Un colosse de glace au bâton (≈ 4 blocs). **Appât : cerf pris dans la glace**, à poser **dans un biome froid**.
Signe : « La neige tombe plus fort, un grondement glacé approche… » ; il arrive **à pied**, dévore le cerf et appelle
aussitôt son escorte. Invoqué par commande ou par œuf, il arrive **seul**.

| Attaque | Ce qui se passe |
|---|---|
| **Coup de bâton** | grand coup en arc |
| **Écrasement** | il frappe le sol : onde de givre qui ralentit (10 blocs) |
| **Appel de l'escorte** | il lève les bras : **1 à 5 sbires de chaque type** (rats des glaces, gobelins boucliers, gobelins archers) ; le double en phase 2, le triple en phase 3 |
| **Lance de glace** | un pic de glace qui **transperce** tous les joueurs sur sa ligne et ralentit |
| **Rugissement glacial** (phase 2) | **le sol gèle** : les joueurs **glissent** comme sur de la glace pendant 10 s (effet « Sol gelé ») |
| **Pluie de stalactites** (phase 2) | des cercles de givre marquent le sol, puis des stalactites y tombent 1,5 s plus tard |
| **Couronne de givre** (à 0 PV) | il se fige dans la glace, puis la brise avec **toute sa vie** (phase 3) |
| **Blizzard** (phase 3) | 12 s de tempête : chaque seconde, tous les joueurs de l'arène sont blessés et **aveuglés** |

L'escorte : **rats des glaces** (rapides, en meute), **gobelins boucliers** (lents ; ils arrêtent presque tous les coups
de face et se postent entre leur cible et le Roi : il faut les contourner), **gobelins archers** (flèches de glace qui
ralentissent). Tous disparaissent à la mort du Roi.
Butin : 1 **cœur de givre éternel**, 2-4 **glaces éternelles**.

### Démon noir du Continent noir — 5500 PV, trois phases

Un colosse de 9 blocs aux bras griffus et aux tentacules dorsaux. **Appât : carcasse de sanglier**, à poser au sol.
Signe : « Des corbeaux tournent au-dessus de la carcasse… » ; il **bondit** de 40 à 60 blocs et **atterrit** sur la
carcasse (onde de choc), puis la dévore. Invoqué sur place, il sort de terre.

| Attaque | Ce qui se passe |
|---|---|
| **Combos 1 à 3** | enchaînements de griffes |
| **Saut écrasé** | il bondit et retombe sur un joueur : **onde de choc** qui repousse |
| **Charge d'épaule** | il fonce et **renverse** tous les joueurs sur son passage |
| **Combos 4 et 5** (phase 2) | plus longs, plus lourds |
| **Pluie de météores sombres** (phase 2) | des boules d'ombre tombent en biais autour de la cible et éclatent |
| **Orbe sombre** (phase 2) | une sphère lente qui **poursuit** sa cible |
| **Second souffle** (à 0 PV) | il s'agenouille, puis se relève avec **toute sa vie** (phase 3) |
| **Déchaînement** (phase 3) | trois combos d'affilée, **plus aucune pause** entre ses attaques, sauts deux fois plus fréquents |

S'il **heurte un mur** pendant un saut ou une charge, il reste **étourdi** près de 2 s et encaisse 50 % de dégâts en plus :
c'est la fenêtre pour le frapper.
Butin : 1-2 **cornes du démon noir**.

## Lot 3

### Kraken des abysses — 6000 PV, trois phases

Un poulpe géant (≈ 5 blocs) qui **ne quitte jamais l'eau** : il se tient à la surface, tête et tentacules dressés, et
s'arrête au bord. **Appât : tonneau d'appât de poisson**, à poser **sur l'eau profonde** (au moins 4 blocs d'eau
dessous ; clic droit en visant l'eau). Signe : « L'eau se met à bouillonner autour du tonneau… » ; il **émerge sous le
tonneau** et le broie.

| Attaque | Ce qui se passe |
|---|---|
| **Tentacules 1 à 3** | frappes de tentacules ; la troisième balaie large et **repousse** |
| **Plongée** | il disparaît sous l'eau et ressort ailleurs, près de sa cible |
| **Rayon** | un jet d'eau sous pression, droit sur un joueur |
| **Charge** | il fonce à la surface ; **frappé 3 fois pendant sa charge**, il s'arrête net, **étourdi** près de 5 s, et encaisse 50 % de dégâts en plus |
| **Double rayon** (phase 2) | deux jets qui **balaient tout l'horizon** |
| **Ponte** (phase 2) | des œufs éclosent : **2 à 4 petits krakens**, qui nagent |
| **Nuage d'encre** (phase 2) | une nappe noire qui **aveugle** 5 s autour de lui |
| **Saisie** (phase 2) | il attrape un joueur, le tient en l'air, puis le **projette** |
| **Réveil des abysses** (à 0 PV) | il coule, puis remonte avec **toute sa vie** (phase 3) |
| **Rage des abysses** (phase 3) | longue charge qui finit sous l'eau ; il ressort plus loin. Ses rayons reviennent **deux fois plus vite** |

Butin : 1-3 **encres des abysses**, 2-4 **tentacules de kraken**.

### Calamar des sables — 5000 PV

Un calamar géant (≈ 8 blocs) qui vit **sous le sable**. **Appât : tambour des sables**, à poser **sur le sable**.
Signe : « Le tambour bat tout seul… » ; il **surgit du sable sous le tambour** et l'avale.

| Attaque | Ce qui se passe |
|---|---|
| **Surgissement** | il s'enfouit, puis **jaillit sous un joueur** et le **projette** en l'air |
| **Tentacules** | une des trois frappes de tentacules |
| **Rayon de sable** | un jet de sable qui **aveugle** 2 s |
| **Enfouissement** | il plonge dans le sable et change de place |
| **Charge longue** (phase 2) | il traverse l'arène ; s'il **heurte un mur**, il reste **étourdi**, tentacules au sol |
| **Double rayon** (phase 2) | deux jets qui **se croisent** sur la cible |
| **Tempête de sable** (phase 2) | un tourbillon autour de lui qui blesse et **aveugle** |
| **Aspiration** (phase 2) | il **attire** les joueurs vers sa gueule |

Il ne peut surgir que sous un **sol meuble** (sable, terre, herbe, argile, neige) : sur du grès ou de la pierre, il
remonte à côté. C'est l'endroit où se mettre à l'abri.
Butin : 1-3 **biles des sables**, 2-4 **chairs de calamar des sables**.

### Démon de lave — 6000 PV

Un colosse de roche en fusion (≈ 9 blocs) qui **ne quitte jamais la lave** : il se tient dans le lac, la lave aux
genoux. **Immunisé au feu.** **Appât : idole d'obsidienne**, à poser **à moins de 3 blocs de la lave**. Signe :
« La lave se met à bouillonner… » ; il **sort du lac de lave** et fracasse l'idole.

| Attaque | Ce qui se passe |
|---|---|
| **Rayon de feu** | un rayon qui suit un joueur et l'**enflamme** |
| **Plongée dans la lave** | il disparaît dans le lac et ressort ailleurs |
| **Coup de poing** | pour ceux qui s'approchent du bord |
| **Balayage du bras** | un grand revers qui **projette** |
| **Double rayon** (phase 2) | deux rayons qui balaient l'arène |
| **Rugissement volcanique** (phase 2) | il rugit et **la lave pleut** autour du lac |
| **Jets de magma** (phase 2) | trois boules de magma ; chacune laisse une **flaque de feu** au sol pendant 8 s |

Les flaques de feu ne sont pas de vrais feux : elles **ne brûlent aucun bloc** et s'éteignent seules.
Butin : 1-3 **braises démoniaques**, 3-6 **sels de lave**.

### Aï, forme de vie gazeuse — 3500 PV

Une silhouette d'ombre très fine (≈ 6 blocs) qui flotte et **traverse les blocs** : impossible à coincer. **Appât :
lanterne des vœux**, à poser **la nuit, dans un marais**. Signe : « Le brouillard monte, la flamme noire de la lanterne
vacille… » ; il **se forme dans la brume** et éteint la lanterne.

| Attaque | Ce qui se passe |
|---|---|
| **Aspiration d'aura** | il **draine la vie et la faim** des joueurs proches |
| **Griffes d'ombre** | des griffes qui **aveuglent** 1 s |
| **Grande aspiration** (phase 2) | deux fois plus loin, et **chaque joueur drainé le soigne** |
| **Cri** (phase 2) | **nausée** et **faiblesse** |
| **Disparition** (phase 2) | il devient **invisible** 4 s, puis reparaît **dans le dos** d'un joueur et frappe |
| **Vœu maudit** (phase 2) | un effet néfaste au hasard : lenteur, faim, cécité, faiblesse, nausée ou **contrôles inversés** (effet « Vœu maudit ») |

**La lumière est son point faible** : plus l'endroit est éclairé, plus son aspiration est courte, et une lumière vive
(niveau 12 et plus : torches, feu) le **blesse**. Apparu la nuit, il **se dissipe à l'aube**, sans être vaincu (et sans butin).
Butin : 1-3 **ectoplasmes sombres**.

## Commandes (permission 2)

Chaque boss a sa commande (`/reine_guepe`, `/abomination`, `/necromancien`, `/xerra`, `/roi_des_glaces`, `/demon_noir`,
`/kraken`, `/calamar_des_sables`, `/demon_de_lave`, `/ai`) :

| Commande | Effet |
|---|---|
| `/<boss> invoquer [x y z]` | le fait apparaître (devant soi, aux coordonnées, ou — depuis la console — au point d'apparition du monde) |
| `/<boss> retirer [rayon]` | retire ce boss et ses sbires (tous, ou à moins de `rayon` blocs) |
| `/<boss> attaque <id>` | force une attaque du plus proche (ids du classeur, complétés par Tab) |
| `/<boss> info` | vie, phase, attaque en cours, cible, sbires de chaque boss chargé, et ses réglages |
| `/<boss> vie <pourcentage>` | ramène la vie du plus proche à ce pourcentage (pour essayer les phases) |
| `/<boss> oeuf [joueur]` · `/<boss> appat [joueur]` | donne son œuf ou son appât |
| `/bestiaire recharger` | relit `config/bestiaire_stats.xlsx` (le Phénix aussi, si son mod est là) ; les boss vivants prennent les nouvelles valeurs |
| `/bestiaire liste` | les boss, leur commande, leurs PV, leurs phases, combien sont chargés, et le nombre de sbires |

## Onglet créatif « HxRP · Bestiaire »

Les appâts, puis les œufs, puis les butins. Les butins sont des ingrédients : les autres mods y font référence par
leur id (`hxrpbestiaire:dard_de_reine`, `hxrpbestiaire:gelee_royale_noire`, `hxrpbestiaire:miel_noir`,
`hxrpbestiaire:chair_d_abomination`, `hxrpbestiaire:poussiere_d_os_maudite`, `hxrpbestiaire:soie_de_xerra`,
`hxrpbestiaire:coeur_de_givre_eternel`, `hxrpbestiaire:glace_eternelle`, `hxrpbestiaire:corne_du_demon_noir`,
`hxrpbestiaire:encre_des_abysses`, `hxrpbestiaire:tentacule_de_kraken`, `hxrpbestiaire:bile_des_sables`,
`hxrpbestiaire:chair_de_calamar_des_sables`, `hxrpbestiaire:braise_demoniaque`, `hxrpbestiaire:sel_de_lave`,
`hxrpbestiaire:ectoplasme_sombre`).

## Réglages : `config/bestiaire_stats.xlsx`

Le même classeur que le Phénix (il est créé au premier démarrage s'il manque, avec les valeurs du jar). Onglets :

- **Boss** : par phase, PV, armure, robustesse, résistance au recul, vitesse, multiplicateurs de dégâts et de
  portée, pause entre attaques, vie reprise (phase 3), rayons de ciblage et d'arène ;
- **Attaques** : phase d'apparition, dégâts, portée, largeur ou rayon, recharge, effet et sa durée, poids ;
- **Sbires** : PV, dégâts, vitesse, nombre par appel, multiplicateurs en phase 2 et 3 ;
- **Appâts** : délai d'arrivée (min-max), distance d'apparition (min-max) ;
- **Butins** : objet, quantité min-max, chance.

Une case vide ou illisible garde la valeur du jar (avertissement dans le journal). Après modification :
`/bestiaire recharger`, sans redémarrer.

## Développement

- `python3 tools/bestiaire/gen.py` régénère toutes les ressources : modèles GeckoLib convertis depuis
  `tools/bestiaire/blockbench/*.bbmodel` (`bbconv.py` : atlas de textures, cubes, animations), animations
  créées (`anims.py`), durées des animations (`durees.properties`, lues par le serveur pour caler les coups),
  icônes, appâts et œufs (`art.py`), sons (`sons.py`, ffmpeg), noms, et la copie du classeur.
- `python3 tools/bestiaire/verifier.py` vérifie la cohérence : espèces, modèles, animations demandées par le code,
  attaques du classeur, sbires, butins, objets, appâts, noms, sons.
- `tools/bestiaire/rendu.py` dessine les modèles posés (planches d'aperçu).
- `tools/test/demarrage.sh serveur <jar> <geckolib.jar> [autres jars]` : vrai serveur Forge, un scénario par boss
  (chaque attaque, sbires, rage, mort et butin, retrait, appâts) ; `tools/test/demarrage.sh client` : vrai client
  qui ouvre un monde plat et dessine les 17 espèces dans chacune de leurs animations (`client/EssaiRendu`, actif
  seulement avec `-Dhxrpbestiaire.essaiRendu=true`). La CI (`.github/workflows/hxrpbestiaire.yml`) fait tout cela à
  chaque modification du mod.
- Rendu : GeckoLib 3.0.31 plante sur un cube texturé face par face auquel il manque des faces (les plans très fins
  n'ont que l'avant et l'arrière) ; `RenduBestiaire.renderCube` saute ces faces (corrigé en 0.3.1 : Aï, Xerra, le
  Nécromancien, le Roi des glaces, les démons et l'escorte du Roi plantaient le client à l'affichage).
