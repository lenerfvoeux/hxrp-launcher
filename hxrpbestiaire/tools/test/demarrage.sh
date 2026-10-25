#!/usr/bin/env bash
# Démarre Minecraft avec le bestiaire (et GeckoLib) et vérifie le journal.
#
#   tools/test/demarrage.sh serveur build/libs/hxrpbestiaire-X.jar geckolib.jar [autres jars…]
#       vrai serveur dédié Forge 1.12.2 ; dans la console, pour chaque boss : invocation, chacune de ses attaques
#       (forcées), sbires, passage en 2e phase, mise à mort (butin, sbires dissipés) ; un retrait ; puis les appâts
#       posés au point d'apparition : le boss arrive (ou surgit) et les dévore. Le classeur du serveur de test est
#       celui du jar avec des appâts pressés (3 à 5 s). Java 8 : $JAVA8 ou $JAVA_HOME_8_X64.
#       Avec hxrpmetiers en plus : les maladies du Hunter Virus sont trouvées ; avec hxrpphenix : /bestiaire recharger
#       relit aussi le Phénix.
#   tools/test/demarrage.sh client
#       client obfusqué de RetroFuturaGradle (runObfClient, GeckoLib inclus) ; il faut un écran (xvfb-run).
set -u
cd "$(dirname "$0")/../.."
mode=${1:-serveur}
FORGE=1.12.2-14.23.5.2860
mkdir -p build
journal=$PWD/build/demarrage-$mode.log

arreter() {
    local p
    for p in $(pgrep -P "$1"); do arreter "$p"; done
    kill "$1" 2>/dev/null
}

attendre() {
    local motif=$1 pid=$2
    for _ in $(seq 1 240); do
        sleep 5
        grep -Eq "$motif" "$journal" && return 0
        kill -0 "$pid" 2>/dev/null || return 1
    done
    return 2
}

if [ "$mode" = client ]; then
    ./gradlew runObfClient --no-daemon > "$journal" 2>&1 &
    pid=$!
    attendre 'textures-atlas|successfully loaded|Sound engine started' $pid
    etat=$?
    sleep 45
    arreter $pid
    sleep 5
else
    jar=$(readlink -f "${2:?chemin du jar du mod}")
    gecko=$(readlink -f "${3:?chemin du jar de GeckoLib}")
    autres=()
    for a in "${@:4}"; do autres+=("$(readlink -f "$a")"); done
    java8=${JAVA8:-${JAVA_HOME_8_X64:?Java 8 requis}/bin/java}
    racine=$PWD
    srv=$PWD/build/serveur-test
    rm -rf "$srv" && mkdir -p "$srv/mods" && cd "$srv"
    curl -fsSL -o installer.jar "https://maven.minecraftforge.net/net/minecraftforge/forge/$FORGE/forge-$FORGE-installer.jar"
    "$java8" -jar installer.jar --installServer > installation.log 2>&1 || { tail -30 installation.log; exit 1; }
    cp "$jar" "$gecko" "${autres[@]}" mods/
    echo 'eula=true' > eula.txt   # serveur de test jetable
    printf 'online-mode=false\nlevel-type=FLAT\ngenerate-structures=false\nspawn-animals=false\nspawn-monsters=false\ndifficulty=2\n' > server.properties
    mkdir -p config
    presse=()
    for b in reine_guepe abomination necromancien xerra roi_des_glaces demon_noir; do
        presse+=("Appâts|id boss=$b|Délai min (s)=3" "Appâts|id boss=$b|Délai max (s)=5")
    done
    python3 "$racine/tools/test/classeur.py" "$racine/src/main/resources/assets/hxrpbestiaire/bestiaire_stats.xlsx" \
        config/bestiaire_stats.xlsx "${presse[@]}" || exit 1
    mkfifo console
    "$java8" -Xmx2G -jar "forge-$FORGE.jar" nogui < console > "$journal" 2>&1 &
    pid=$!
    exec 3> console
    attendre 'Done \(' $pid
    etat=$?
    if [ $etat = 0 ]; then
        # Sans joueur, le monde cesse de faire vivre entités et blocs au bout de 15 s : chaque commande du bestiaire les réveille.
        c() { echo "$1" >&3; sleep "$2"; }
        c 'bestiaire liste' 2
        # --- Reine-guêpe : ses dix attaques, l'essaim (3 guêpes soldats), la rage, la mort
        c 'reine_guepe invoquer' 3
        c 'reine_guepe info' 1
        for a in taille_droite taille_gauche taille_haute taille_basse hachage onde_air essaim; do c "reine_guepe attaque $a" 2.5; done
        c 'reine_guepe info' 1
        c 'reine_guepe attaque regard_brulant' 5.5
        c 'reine_guepe attaque rafale_dards' 2.5
        c 'reine_guepe attaque pique' 4.5
        c 'reine_guepe vie 40' 2
        c 'reine_guepe info' 1
        c 'kill @e[type=hxrpbestiaire:reine_guepe]' 2
        c 'bestiaire liste' 5
        c 'reine_guepe info' 1
        # --- Abomination : surgit du sol, coups, bond, enfouissement, vomissure
        c 'abomination invoquer' 3
        c 'abomination info' 1
        for a in coups bond enfouissement vomissure; do c "abomination attaque $a" 3.5; done
        c 'abomination vie 40' 2
        c 'abomination info' 1
        c 'kill @e[type=hxrpbestiaire:abomination]' 5
        # --- Nécromancien : apparition, bâton, frappe au sol, levée des morts (3 archers), boules d'âme, téléportation
        c 'necromancien invoquer' 4
        for a in coup_baton frappe_sol levee_morts boules_ame teleportation; do c "necromancien attaque $a" 3; done
        c 'necromancien info' 1
        c 'necromancien vie 40' 2
        c 'necromancien info' 1
        c 'kill @e[type=hxrpbestiaire:necromancien]' 5
        c 'bestiaire liste' 2
        # --- retrait : la reine et ses guêpes disparaissent ensemble
        c 'reine_guepe invoquer' 2
        c 'reine_guepe attaque essaim' 3
        c 'reine_guepe retirer' 2
        c 'bestiaire liste' 2
        # --- Xerra : toutes ses attaques (sans joueur, les tirs visent devant elle), la rage, puis la renaissance (phase 3),
        #     la grande couvée (araignées et toiles), et la mort pour de bon
        c 'xerra invoquer' 3
        c 'xerra info' 1
        for a in coup_patte rugissement couvee jet_toile bond; do c "xerra attaque $a" 3; done
        c 'xerra attaque griffures' 4
        c 'xerra attaque graines_pieges' 4
        c 'xerra attaque course' 3
        c 'xerra vie 40' 2
        c 'xerra info' 1
        c 'kill @e[type=hxrpbestiaire:xerra]' 4
        c 'xerra info' 4
        c 'xerra info' 1
        c 'xerra attaque grande_couvee' 3
        c 'xerra info' 1
        c 'kill @e[type=hxrpbestiaire:xerra]' 6
        c 'bestiaire liste' 2
        # --- Roi des glaces : escorte (rats, gobelins boucliers et archers), lance, écrasement, sol gelé, stalactites,
        #     couronne de givre (phase 3), blizzard
        c 'roi_des_glaces invoquer' 3
        c 'roi_des_glaces info' 1
        c 'roi_des_glaces attaque appel_escorte' 3
        c 'roi_des_glaces info' 1
        for a in coup_baton ecrasement lance_glace rugissement_glacial; do c "roi_des_glaces attaque $a" 3; done
        c 'roi_des_glaces attaque stalactites' 5
        c 'roi_des_glaces vie 40' 2
        c 'kill @e[type=hxrpbestiaire:roi_des_glaces]' 3
        c 'roi_des_glaces info' 3
        c 'roi_des_glaces info' 1
        c 'roi_des_glaces attaque blizzard' 4
        c 'roi_des_glaces info' 1
        c 'kill @e[type=hxrpbestiaire:roi_des_glaces]' 6
        c 'bestiaire liste' 2
        # --- Démon noir : combos, saut écrasé, charge d'épaule, météores, orbe, second souffle (phase 3), déchaînement
        c 'demon_noir invoquer' 5
        c 'demon_noir info' 1
        for a in combo_1 combo_2 combo_3 combo_4 combo_5; do c "demon_noir attaque $a" 2; done
        for a in saut_ecrase charge_epaule; do c "demon_noir attaque $a" 4; done
        c 'demon_noir attaque meteores' 5
        c 'demon_noir attaque orbe' 3
        c 'demon_noir vie 40' 2
        c 'kill @e[type=hxrpbestiaire:demon_noir]' 3
        c 'demon_noir info' 3
        c 'demon_noir info' 1
        c 'demon_noir attaque dechainement' 5
        c 'demon_noir info' 1
        c 'kill @e[type=hxrpbestiaire:demon_noir]' 6
        c 'bestiaire liste' 2
        # --- appâts au point d'apparition (classeur pressé : 3 à 5 s)
        c 'bestiaire recharger' 2
        c 'save-all' 3
        read -r X Y Z < <(python3 - world/level.dat <<'PY'
import gzip, struct, sys
d = gzip.open(sys.argv[1]).read()
def entier(nom):
    i = d.index(b'\x03' + struct.pack('>H', len(nom)) + nom)
    return struct.unpack('>i', d[i + 3 + len(nom):i + 7 + len(nom)])[0]
print(entier(b'SpawnX'), entier(b'SpawnY'), entier(b'SpawnZ'))
PY
)
        echo "appâts en $X $Y $Z"
        for b in reine_guepe:rayon_de_miel_royal:6 abomination:charnier:6 necromancien:cercueil_profane:6 xerra:cocon_de_proie:6 \
            roi_des_glaces:cerf_pris_dans_la_glace:12 demon_noir:carcasse_de_sanglier:6; do
            IFS=: read -r boss appat tours <<< "$b"
            c "setblock $X $Y $Z hxrpbestiaire:$appat" 2
            for _ in $(seq 1 "$tours"); do c "$boss info" 3; done
            c "$boss retirer" 2
        done
        c 'bestiaire liste' 2
    fi
    echo stop >&3
    exec 3>&-
    for _ in $(seq 1 30); do kill -0 $pid 2>/dev/null || break; sleep 2; done
    kill -0 $pid 2>/dev/null && { echo "!! le serveur ne s'arrête pas"; arreter $pid; etat=3; }
    cd - > /dev/null
fi

echo "---- extrait du journal ($journal)"
grep -E 'hxrpbestiaire|Bestiaire|Reine|Abomination|Nécromancien|Xerra|Roi des glaces|Démon|GeckoLib|geckolib|Done \(|textures-atlas|successfully loaded|Stopping server|Sbires|ne peut pas|inconnue' "$journal" \
    | grep -Ev $'^\tat |STDERR' | grep -v 'Réglages (' | head -400

erreurs=$(grep -E 'Exception loading|Model definition for location|texture errors were found|Caught exception from|Crash Report|crash-reports|/(ERROR|FATAL)\].*(hxrpbestiaire|HxRP|Bestiaire|GeckoLib|geckolib)|Error loading (model|animation) file|Could not load animation|at fr\.lenerfvoeux|at software\.bernie|Unable to play unknown soundEvent|Missing (sound|texture)|ne peut pas attaquer maintenant|Attaque inconnue' "$journal" | head -60)
avertissements=$(grep -E '/WARN\].*(hxrpbestiaire|HxRP|Bestiaire)' "$journal" | head -30)
if [ -n "$avertissements" ]; then
    echo "---- avertissements mentionnant le mod"
    echo "$avertissements"
fi
if [ $etat != 0 ]; then
    echo "!! le $mode n'a pas fini de démarrer (code $etat)"
    tail -80 "$journal"
    exit 1
fi
if ! grep -q 'Bestiaire : pr' "$journal"; then
    echo "!! le bestiaire ne s'est pas initialisé"
    exit 1
fi
if [ "$mode" = client ] && ! grep -Eq 'Bestiaire : 10 mod.+les et [0-9]+ animations GeckoLib charg' "$journal"; then
    echo "!! GeckoLib n'a pas lu les modèles et animations du bestiaire"
    exit 1
fi
if [ "$mode" = serveur ]; then
    mods=$(ls "$PWD/build/serveur-test/mods")
    if echo "$mods" | grep -q hxrpmetiers; then
        grep -q 'Hunter Virus : oui' "$journal" || { echo "!! le bestiaire ne trouve pas VirusAPI.maladieAleatoire"; exit 1; }
    fi
    if echo "$mods" | grep -q hxrpphenix; then
        grep -q 'nix compris' "$journal" || { echo "!! /bestiaire recharger n'a pas relu le Phénix"; exit 1; }
    fi
    attendus=(
        'glages lus \([^)]*config/bestiaire_stats\.xlsx\) : [0-9]+ boss'
        'Reine-gu.+pe \(/reine_guepe\) : 4000 PV, 2 phase'
        'Reine-gu.+pe appara.+t'
        'Reine-gu.+pe : 4000 / 4000 PV, phase 1'
        'Reine-gu.+pe lance : taille_droite' 'lance : hachage' 'lance : onde_air' 'lance : essaim'
        'lance : regard_brulant' 'lance : rafale_dards' 'lance : pique'
        'phase 1, [a-z_]+, 3 sbire\(s\)'
        'Reine-gu.+pe entre en rage' 'Reine-gu.+pe : 1600 / 4000 PV, phase 2'
        'butin de Reine-gu.+pe : [1-9].+Dard de reine.+Gel.+e royale noire.+Miel noir'
        'Reine-gu.+pe est vaincu'
        'Abomination, porteuse de Zobae : 3000 / 3000 PV, phase 1'
        'Abomination, porteuse de Zobae lance : bond' 'lance : enfouissement' 'lance : vomissure'
        'Abomination, porteuse de Zobae entre en rage' 'butin de Abomination.+Chair d.abomination'
        'cromancien lance : levee_morts' 'lance : boules_ame' 'lance : teleportation'
        'cromancien : [0-9]+ / 3500 PV, phase 1, [a-z_ ]+, 3 sbire\(s\)'
        'cromancien entre en rage' 'butin de N.+cromancien : [1-9].+Poussi.+re d.os maudite'
        '1 Reine-gu.+pe retir'
        'glages du bestiaire relus \([^)]*config/bestiaire_stats\.xlsx\)'
        'Un bourdonnement sourd' 'Reine-gu.+pe attir.+e par .+ \((4[0-9]|5[0-9]|60) blocs\)' 'Reine-gu.+pe arrive' 'vore le rayon de miel royal'
        'Des mouches tournent' 'Abomination, porteuse de Zobae jaillit du sol'
        'Des mains d.+charn.+es' 'cromancien se mat.+rialise'
        # lot 2
        'Xerra, m.re-araign.e : 4500 / 4500 PV, phase 1'
        'Xerra, m.re-araign.e lance : jet_toile' 'lance : griffures' 'lance : graines_pieges' 'lance : course' 'lance : bond'
        'Xerra, m.re-araign.e : 1800 / 4500 PV, phase 2, [a-z_ ]+, 4 sbire'
        'Xerra, m.re-araign.e s.effondre' 'Xerra, m.re-araign.e se rel.ve avec toute sa vie, plus enrag.e'
        'Xerra, m.re-araign.e : 4500 / 4500 PV, phase 3'
        'Xerra, m.re-araign.e : [0-9]+ / 4500 PV, phase 3.+, [1-9][0-9]* toile\(s\) proche'
        'butin de Xerra, m.re-araign.e : [1-9].+Soie de Xerra' 'Xerra, m.re-araign.e est vaincue'
        'Roi des glaces : 4000 / 4000 PV, phase 1, [a-z_ ]+, ([3-9]|1[0-5]) sbire'
        'Roi des glaces lance : lance_glace' 'lance : rugissement_glacial' 'lance : stalactites' 'lance : ecrasement'
        'Roi des glaces entre en rage' 'Roi des glaces se rel.ve avec toute sa vie'
        'Roi des glaces : 4000 / 4000 PV, phase 3' 'Roi des glaces d.cha.ne le blizzard' 'phase 3.+blizzard [0-9]+ s'
        'butin de Roi des glaces : [1-9].+C.ur de givre .ternel.+Glace .ternelle' 'Roi des glaces est vaincu\.'
        'D.mon noir du Continent noir : 5500 / 5500 PV, phase 1'
        'D.mon noir du Continent noir lance : saut_ecrase' 'lance : charge_epaule' 'lance : meteores' 'lance : orbe' 'lance : combo_5'
        'D.mon noir du Continent noir entre en rage' 'D.mon noir du Continent noir : 5500 / 5500 PV, phase 3' 'lance : dechainement'
        'butin de D.mon noir du Continent noir : [1-9].+Corne du d.mon noir'
        'Des araign.es sortent des murs' 'Xerra, m.re-araign.e attir.e par l.app.t' 'descend du plafond et d.vore le cocon'
        'La neige tombe plus fort' 'Roi des glaces attir. par .+ \((4[0-9]|5[0-9]|60) blocs\)' 'brise la glace, d.vore le cerf'
        'Des corbeaux tournent' 'D.mon noir du Continent noir attir. par .+ \((4[0-9]|5[0-9]|60) blocs\)' 'atterrit d.un saut sur la carcasse'
    )
    for attendu in "${attendus[@]}"; do
        grep -Eq "$attendu" "$journal" || { echo "!! scénario : « $attendu » n'apparaît pas dans le journal"; tail -80 "$journal"; exit 1; }
    done
    # chaque mort et chaque retrait dissipe les sbires : aucun ne reste aux huit relevés (début, morts, retrait, fin)
    test "$(grep -c 'Sbires charg.s : 0' "$journal")" -ge 8 || { echo "!! des sbires survivent à leur boss"; grep 'Sbires charg' "$journal"; exit 1; }
fi
if [ -n "$erreurs" ]; then
    echo "!! erreurs :"
    echo "$erreurs"
    exit 1
fi
echo "OK : $mode démarré avec le bestiaire, aucune erreur"
