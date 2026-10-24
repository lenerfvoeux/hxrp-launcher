#!/usr/bin/env bash
# Un monde de la v0.6.0 rouvert avec la nouvelle version : les préparations renommées sont remappées (rien ne se perd
# dans les coffres), les objets et blocs retirés disparaissent, et le serveur démarre sans poser la question des
# « blocs manquants » (sinon il attendrait « /fml confirm » et ce test expirerait).
#
#   tools/test/migration.sh ancien/hxrpmetiers-0_6_0.jar build/libs/hxrpmetiers-0.7.0.jar
#       Java 8 : $JAVA8 ou $JAVA_HOME_8_X64.
set -u
cd "$(dirname "$0")/../.."
ancien=$(readlink -f "${1:?jar de la v0.6.0}")
nouveau=$(readlink -f "${2:?jar à tester}")
FORGE=1.12.2-14.23.5.2860
java8=${JAVA8:-${JAVA_HOME_8_X64:?Java 8 requis}/bin/java}
srv=$PWD/build/serveur-migration
journal=$PWD/build/migration.log
rm -rf "$srv" && mkdir -p "$srv/mods" && cd "$srv" && : > "$journal"
curl -fsSL -o installer.jar "https://maven.minecraftforge.net/net/minecraftforge/forge/$FORGE/forge-$FORGE-installer.jar"
"$java8" -jar installer.jar --installServer > installation.log 2>&1 || { tail -30 installation.log; exit 1; }
echo 'eula=true' > eula.txt
printf 'online-mode=false\nlevel-type=FLAT\nspawn-animals=false\nspawn-monsters=false\n' > server.properties

# Lance le serveur avec un jar, envoie les commandes (une par argument) puis « stop ».
partie() {
    local jar=$1 nom=$2; shift 2
    rm -f mods/*.jar console && cp "$jar" mods/ && mkfifo console
    echo "==== $nom" >> "$journal"
    "$java8" -Xmx2G -jar "forge-$FORGE.jar" nogui < console >> "$journal" 2>&1 &
    local pid=$! debut
    exec 3> console
    debut=$(wc -l < "$journal")
    for _ in $(seq 1 120); do
        sleep 5
        tail -n +"$debut" "$journal" | grep -q 'Done (' && break
        kill -0 $pid 2>/dev/null || { echo "!! $nom : le serveur s'est arrêté"; tail -60 "$journal"; exit 1; }
    done
    tail -n +"$debut" "$journal" | grep -q 'Done (' || { echo "!! $nom : le serveur n'a pas fini de démarrer"; tail -60 "$journal"; exit 1; }
    sleep 5
    for c in "$@"; do echo "$c" >&3; sleep 2; done
    echo stop >&3
    exec 3>&-
    for _ in $(seq 1 30); do kill -0 $pid 2>/dev/null || break; sleep 2; done
    kill -0 $pid 2>/dev/null && { echo "!! $nom : le serveur ne s'arrête pas"; kill $pid; exit 1; }
}

# 1. Monde créé par la v0.6.0, puis point d'apparition lu dans level.dat (monde plat : il est tiré au hasard).
partie "$ancien" "v0.6.0 : création du monde" "save-all"
read -r X Z < <(python3 - world/level.dat <<'PY'
import gzip, struct, sys
d = gzip.open(sys.argv[1]).read()
def entier(nom):
    i = d.index(b'\x03' + struct.pack('>H', len(nom)) + nom)
    return struct.unpack('>i', d[i + 3 + len(nom):i + 7 + len(nom)])[0]
print(entier(b'SpawnX'), entier(b'SpawnZ'))
PY
)
Y=200
echo "point d'apparition : $X $Z"

# 2. Toujours en v0.6.0 : un coffre avec deux préparations renommées et un ingrédient retiré, et un bloc retiré.
partie "$ancien" "v0.6.0 : objets anciens" \
    "setblock $X $Y $Z minecraft:chest" \
    "replaceitem block $X $Y $Z slot.container.0 hxrpmetiers:sirop_de_fer_sang 3" \
    "replaceitem block $X $Y $Z slot.container.1 hxrpmetiers:essence_des_quinze_raretes 1" \
    "replaceitem block $X $Y $Z slot.container.2 hxrpmetiers:feuille_de_kukuroo 5" \
    "setblock $X $((Y + 1)) $Z hxrpmetiers:roche_a_fer_sang" \
    "testforblock $X $((Y + 1)) $Z hxrpmetiers:roche_a_fer_sang" \
    "save-all"
grep -q "Successfully found the block at $X, $((Y + 1)), $Z" "$journal" || { echo "!! la v0.6.0 n'a pas posé ses objets"; tail -40 "$journal"; exit 1; }

# 3. Le même monde avec la nouvelle version. Un bloc retiré devient de l'air « fantôme » de Forge (BlockDummyAir, qui
#    garde l'ancien nom au cas où le bloc reviendrait) : testforblock minecraft:air échoue, mais « setblock … keep »,
#    qui ne remplace que de l'air, réussit.
avant=$(wc -l < "$journal")
partie "$nouveau" "nouvelle version" \
    "testforblock $X $Y $Z minecraft:chest -1 {Items:[{Slot:0b,id:\"hxrpmetiers:sirop_de_gentiane_et_de_bardane\",Count:3b}]}" \
    "testforblock $X $Y $Z minecraft:chest -1 {Items:[{Slot:1b,id:\"hxrpmetiers:essence_des_dix_raretes\"}]}" \
    "testforblock $X $Y $Z minecraft:chest -1 {Items:[{Slot:2b}]}" \
    "setblock $X $((Y + 1)) $Z minecraft:stone 0 keep" \
    "testforblock $X $((Y + 1)) $Z minecraft:stone"
cd - > /dev/null
apres=$(tail -n +"$avant" "$journal")
echo "---- nouvelle version"
echo "$apres" | grep -E 'devient|Successfully found|did not have|is .* \(expected|Done \(|missing|Missing|confirm' | grep -v 'Found a missing id' | head -40
echo "$apres" | grep -q 'fml confirm' && { echo "!! le serveur demande de confirmer des blocs manquants"; exit 1; }
n=$(echo "$apres" | grep -c 'Successfully found the block')
[ "$n" -eq 3 ] || { echo "!! $n vérification(s) réussie(s) sur 3 attendues (coffre remappé, bloc retiré devenu air)"; exit 1; }
echo "$apres" | grep -q "did not have the required NBT keys" || { echo "!! l'ingrédient retiré est encore dans le coffre"; exit 1; }
for v in sirop_de_gentiane_et_de_bardane essence_des_dix_raretes pilules_de_champignon_luminescent elixir_de_truffe_noire decoction_de_pied_bleu_et_de_chaga; do
    echo "$apres" | grep -q "devient hxrpmetiers:$v" || { echo "!! pas de remappage vers $v"; exit 1; }
done
erreurs=$(echo "$apres" | grep -E 'Caught exception|Crash Report|/(ERROR|FATAL)\].*(hxrpmetiers|HxRP)|at fr\.lenerfvoeux' | head -20)
[ -z "$erreurs" ] || { echo "!! erreurs :"; echo "$erreurs"; exit 1; }
echo "OK : monde v0.6.0 rouvert, préparations remappées, objets et blocs retirés effacés"
