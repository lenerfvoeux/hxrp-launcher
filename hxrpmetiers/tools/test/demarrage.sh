#!/usr/bin/env bash
# Démarre Minecraft avec le mod et vérifie dans le journal qu'il s'est chargé sans erreur :
# données, registres, réseau, modèles 3D et textures.
#
#   tools/test/demarrage.sh serveur build/libs/hxrpmetiers-X.jar
#       installe un vrai serveur dédié Forge 1.12.2 (comme en production), y dépose le jar,
#       le démarre puis l'arrête proprement avec « stop ». Java 8 : $JAVA8 ou $JAVA_HOME_8_X64.
#   tools/test/demarrage.sh client
#       lance le client obfusqué de RetroFuturaGradle (runObfClient) ; il faut un écran (xvfb-run).
set -u
cd "$(dirname "$0")/../.."
mode=${1:-serveur}
FORGE=1.12.2-14.23.5.2860
mkdir -p build
journal=$PWD/build/demarrage-$mode.log

# Arrête un processus et tous ses descendants.
arreter() {
    local p
    for p in $(pgrep -P "$1"); do arreter "$p"; done
    kill "$1" 2>/dev/null
}

# Attend que le motif apparaisse dans le journal ; 0 = prêt, 1 = arrêté tout seul, 2 = trop long.
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
    # atlas de textures assemblé (après les modèles) ou fin du chargement des mods
    attendre 'textures-atlas|successfully loaded|Sound engine started' $pid
    etat=$?
    sleep 45   # laisse passer les erreurs tardives (textures manquantes, etc.)
    arreter $pid
    sleep 5
else
    jar=$(readlink -f "${2:?chemin du jar du mod}")
    java8=${JAVA8:-${JAVA_HOME_8_X64:?Java 8 requis}/bin/java}
    srv=$PWD/build/serveur-test
    rm -rf "$srv" && mkdir -p "$srv/mods" && cd "$srv"
    curl -fsSL -o installer.jar "https://maven.minecraftforge.net/net/minecraftforge/forge/$FORGE/forge-$FORGE-installer.jar"
    "$java8" -jar installer.jar --installServer > installation.log 2>&1 || { tail -30 installation.log; exit 1; }
    cp "$jar" mods/
    echo 'eula=true' > eula.txt   # serveur de test jetable
    printf 'online-mode=false\nlevel-type=FLAT\nspawn-animals=false\nspawn-monsters=false\n' > server.properties
    mkfifo console
    "$java8" -Xmx2G -jar "forge-$FORGE.jar" nogui < console > "$journal" 2>&1 &
    pid=$!
    exec 3> console
    attendre 'Done \(' $pid
    etat=$?
    sleep 10
    echo stop >&3
    exec 3>&-
    for _ in $(seq 1 30); do kill -0 $pid 2>/dev/null || break; sleep 2; done
    kill -0 $pid 2>/dev/null && { echo "!! le serveur ne s'arrête pas"; arreter $pid; etat=3; }
    cd - > /dev/null
fi

echo "---- extrait du journal ($journal)"
grep -E 'hxrpmetiers|HxRP|Gourmet|Done \(|textures-atlas|successfully loaded|Stopping server' "$journal" | head -40

# Erreurs : modèles ou textures introuvables, plantage, message d'erreur ou exception passant par le code du mod.
erreurs=$(grep -E 'Exception loading|Model definition for location|texture errors were found|Caught exception from|Crash Report|crash-reports|/(ERROR|FATAL)\].*(hxrpmetiers|HxRP)|\[hxrpmetiers\]|at fr\.lenerfvoeux' "$journal" \
    | grep -v '\[hxrpmetiers\]: Gourmet : [0-9]' | head -60)
avertissements=$(grep -E '/WARN\].*(hxrpmetiers|HxRP)' "$journal" | head -30)
if [ -n "$avertissements" ]; then
    echo "---- avertissements mentionnant le mod"
    echo "$avertissements"
fi
if [ $etat != 0 ]; then
    echo "!! le $mode n'a pas fini de démarrer (code $etat)"
    tail -80 "$journal"
    exit 1
fi
if ! grep -q 'Gourmet : ' "$journal"; then
    echo "!! le mod n'a pas chargé ses recettes"
    exit 1
fi
if [ -n "$erreurs" ]; then
    echo "!! erreurs au chargement :"
    echo "$erreurs"
    exit 1
fi
echo "OK : $mode démarré avec le mod, aucune erreur"
