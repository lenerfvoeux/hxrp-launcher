#!/bin/sh
# Compile et lance le banc d'essai des mini-jeux et les aperçus de scènes, hors Minecraft.
set -e
cd "$(dirname "$0")/../.."
rm -rf build/banc && mkdir -p build/banc
javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options -d build/banc \
  src/main/java/fr/lenerfvoeux/hxrp/metiers/minijeu/*.java \
  src/main/java/fr/lenerfvoeux/hxrp/metiers/data/FoodEntry.java \
  src/main/java/fr/lenerfvoeux/hxrp/metiers/client/minijeu/*.java \
  tools/test/fr/lenerfvoeux/hxrp/metiers/minijeu/*.java \
  tools/test/fr/lenerfvoeux/hxrp/metiers/client/minijeu/*.java
java -Dfile.encoding=UTF-8 -cp build/banc fr.lenerfvoeux.hxrp.metiers.minijeu.BancMiniJeux
java -Dfile.encoding=UTF-8 -cp build/banc fr.lenerfvoeux.hxrp.metiers.client.minijeu.ApercuMiniJeux "$@"
