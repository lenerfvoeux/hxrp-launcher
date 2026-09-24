#!/usr/bin/env bash
# Démarre un vrai serveur ou client Forge 1.12.2 avec le jar final (obfusqué) du mod, puis vérifie dans
# le journal que le mod s'est chargé sans erreur : données, registres, réseau, modèles 3D et textures.
# Usage : tools/test/demarrage.sh serveur|client   (le client a besoin d'un écran : xvfb-run)
set -u
cd "$(dirname "$0")/../.."
mode=${1:-serveur}
if [ "$mode" = client ]; then
    tache=runObfClient
    # atlas de textures assemblé (après le chargement des modèles) ou fin du chargement des mods
    pret='textures-atlas|successfully loaded|Sound engine started'
else
    tache=runObfServer
    pret='Done \('
fi
journal=build/demarrage-$mode.log
mkdir -p build

# Arrête Gradle et le jeu qu'il a lancé (tout l'arbre de processus).
arreter() {
    local p
    for p in $(pgrep -P "$1"); do arreter "$p"; done
    kill "$1" 2>/dev/null
}

# Lance la tâche ; 0 = prêt, 1 = arrêté tout seul, 2 = trop long.
lancer() {
    ./gradlew "$tache" --no-daemon > "$journal" 2>&1 &
    local pid=$!
    for _ in $(seq 1 240); do
        sleep 5
        if grep -Eq "$pret" "$journal"; then
            sleep 45   # laisse passer les erreurs tardives (textures manquantes, etc.)
            arreter "$pid"
            sleep 5
            return 0
        fi
        kill -0 "$pid" 2>/dev/null || return 1
    done
    arreter "$pid"
    return 2
}

lancer
etat=$?
if [ $etat = 1 ] && grep -q 'agree to the EULA' "$journal"; then
    # Serveur de test jetable : on accepte l'EULA de Minecraft et on relance.
    find . -name eula.txt -not -path './src/*' -exec sed -i 's/eula=false/eula=true/' {} +
    lancer
    etat=$?
fi

echo "---- extrait du journal ($journal)"
grep -E 'hxrpmetiers|HxRP|Gourmet|Done \(|textures-atlas|successfully loaded' "$journal" | head -40

erreurs=$(grep -E 'Exception loading|Model definition for location|texture errors were found|Caught exception from|Crash Report|crash-reports|hxrpmetiers.*(ERROR|Exception|missing)|(ERROR|Exception|Missing|missing).*hxrpmetiers' "$journal" | head -60)
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
