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
    for b in reine_guepe abomination necromancien; do
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
        for b in reine_guepe:rayon_de_miel_royal abomination:charnier necromancien:cercueil_profane; do
            c "setblock $X $Y $Z hxrpbestiaire:${b#*:}" 2
            for _ in $(seq 1 6); do c "${b%%:*} info" 3; done
            c "${b%%:*} retirer" 2
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
grep -E 'hxrpbestiaire|Bestiaire|Reine|Abomination|Nécromancien|GeckoLib|geckolib|Done \(|textures-atlas|successfully loaded|Stopping server|Sbires' "$journal" \
    | grep -Ev $'^\tat |STDERR' | grep -v 'Réglages (' | head -200

erreurs=$(grep -E 'Exception loading|Model definition for location|texture errors were found|Caught exception from|Crash Report|crash-reports|/(ERROR|FATAL)\].*(hxrpbestiaire|HxRP|Bestiaire|GeckoLib|geckolib)|Error loading (model|animation) file|Could not load animation|at fr\.lenerfvoeux|at software\.bernie|Unable to play unknown soundEvent|Missing (sound|texture)' "$journal" | head -60)
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
if [ "$mode" = client ] && ! grep -Eq 'Bestiaire : 4 mod.+les et [0-9]+ animations GeckoLib charg' "$journal"; then
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
    )
    for attendu in "${attendus[@]}"; do
        grep -Eq "$attendu" "$journal" || { echo "!! scénario : « $attendu » n'apparaît pas dans le journal"; tail -80 "$journal"; exit 1; }
    done
    # chaque mort et chaque retrait dissipe les sbires : aucun ne reste aux cinq relevés (début, morts, retrait, fin)
    test "$(grep -c 'Sbires charg.s : 0' "$journal")" -ge 5 || { echo "!! des sbires survivent à leur boss"; grep 'Sbires charg' "$journal"; exit 1; }
fi
if [ -n "$erreurs" ]; then
    echo "!! erreurs :"
    echo "$erreurs"
    exit 1
fi
echo "OK : $mode démarré avec le bestiaire, aucune erreur"
