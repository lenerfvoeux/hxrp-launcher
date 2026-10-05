#!/usr/bin/env bash
# Démarre Minecraft avec le Phénix (et GeckoLib) et vérifie le journal.
#
#   tools/test/demarrage.sh serveur build/libs/hxrpphenix-X.jar geckolib.jar [autres jars…]
#       vrai serveur dédié Forge 1.12.2 ; dans la console : invocation, tempête de feu, perchoir,
#       mise à mort (œuf de cendres), renaissance, retrait. Java 8 : $JAVA8 ou $JAVA_HOME_8_X64.
#       Avec le jar de hxrpmetiers en plus, vérifie aussi que le Phénix trouve la Brûlure du Hunter Virus.
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
    srv=$PWD/build/serveur-test
    rm -rf "$srv" && mkdir -p "$srv/mods" && cd "$srv"
    curl -fsSL -o installer.jar "https://maven.minecraftforge.net/net/minecraftforge/forge/$FORGE/forge-$FORGE-installer.jar"
    "$java8" -jar installer.jar --installServer > installation.log 2>&1 || { tail -30 installation.log; exit 1; }
    cp "$jar" "$gecko" "${autres[@]}" mods/
    echo 'eula=true' > eula.txt   # serveur de test jetable
    printf 'online-mode=false\nlevel-type=FLAT\nspawn-animals=false\nspawn-monsters=false\n' > server.properties
    mkfifo console
    "$java8" -Xmx2G -jar "forge-$FORGE.jar" nogui < console > "$journal" 2>&1 &
    pid=$!
    exec 3> console
    attendre 'Done \(' $pid
    etat=$?
    if [ $etat = 0 ]; then
        # scénario sans joueur : le Phénix tourne au-dessus de son arène, lance une tempête de feu,
        # se pose, meurt une première fois (œuf de cendres), renaît, puis on le retire
        c() { echo "$1" >&3; sleep "$2"; }
        c 'phenix invoquer' 8
        c 'phenix info' 2
        c 'phenix attaque tempete' 14
        c 'phenix attaque perche' 10
        c 'phenix info' 2
        c 'kill @e[type=hxrpphenix:phenix]' 3
        c 'phenix info' 13
        c 'phenix info' 3
        c 'phenix attaque pluie' 3
        c 'phenix attaque plongee' 3
        c 'phenix attaque souffle' 3
        c 'phenix attaque boule' 3
        c 'kill @e[type=hxrpphenix:phenix]' 2
        c 'kill @e[type=hxrpphenix:phenix]' 4
        c 'phenix invoquer' 3
        c 'phenix retirer' 2
        c 'phenix info' 2
    fi
    echo stop >&3
    exec 3>&-
    for _ in $(seq 1 30); do kill -0 $pid 2>/dev/null || break; sleep 2; done
    kill -0 $pid 2>/dev/null && { echo "!! le serveur ne s'arrête pas"; arreter $pid; etat=3; }
    cd - > /dev/null
fi

echo "---- extrait du journal ($journal)"
grep -E 'hxrpphenix|Phénix|phénix|GeckoLib|geckolib|Done \(|textures-atlas|successfully loaded|Stopping server|Œuf|œuf' "$journal" \
    | grep -Ev $'^\tat |STDERR' | head -60

erreurs=$(grep -E 'Exception loading|Model definition for location|texture errors were found|Caught exception from|Crash Report|crash-reports|/(ERROR|FATAL)\].*(hxrpphenix|HxRP|GeckoLib|geckolib)|Error loading (model|animation) file|Could not load animation|at fr\.lenerfvoeux|at software\.bernie' "$journal" | head -60)
avertissements=$(grep -E '/WARN\].*(hxrpphenix|HxRP|Phénix)' "$journal" | head -30)
if [ -n "$avertissements" ]; then
    echo "---- avertissements mentionnant le mod"
    echo "$avertissements"
fi
if [ $etat != 0 ]; then
    echo "!! le $mode n'a pas fini de démarrer (code $etat)"
    tail -80 "$journal"
    exit 1
fi
if ! grep -q 'nix : pr' "$journal"; then
    echo "!! le mod Phénix ne s'est pas initialisé"
    exit 1
fi
if [ "$mode" = client ] && ! grep -q 'animations GeckoLib charg' "$journal"; then
    echo "!! GeckoLib n'a pas lu le modèle et les animations du Phénix"
    exit 1
fi
if [ "$mode" = serveur ] && ls "$PWD/build/serveur-test/mods" | grep -q hxrpmetiers; then
    grep -q 'Hunter Virus : oui' "$journal" || { echo "!! le Phénix ne trouve pas VirusAPI.brulure"; exit 1; }
    grep -q 'Virus : [0-9]' "$journal" || { echo "!! le Hunter Virus n'a pas chargé ses données"; exit 1; }
fi
if [ "$mode" = serveur ]; then
    for attendu in 'feu s.+veille' 'nix de feu : [0-9]+ / [0-9]+ PV' 'uf de cendres' 'de ses cendres' 'nix retir'; do
        grep -Eq "$attendu" "$journal" || { echo "!! scénario : « $attendu » n'apparaît pas dans le journal"; tail -60 "$journal"; exit 1; }
    done
fi
if [ -n "$erreurs" ]; then
    echo "!! erreurs :"
    echo "$erreurs"
    exit 1
fi
echo "OK : $mode démarré avec le Phénix, aucune erreur"
