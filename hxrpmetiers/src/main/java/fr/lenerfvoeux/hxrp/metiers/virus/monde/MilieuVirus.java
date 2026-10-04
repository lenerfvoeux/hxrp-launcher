package fr.lenerfvoeux.hxrp.metiers.virus.monde;

import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;

import java.util.HashSet;
import java.util.Set;

/**
 * Les milieux où poussent les ingrédients du Virus (voir tools/virus/ingredients.py), déduits du biome
 * (types du BiomeDictionary, donc aussi les biomes des autres mods) et de l'altitude.
 */
public final class MilieuVirus {
    private MilieuVirus() {}

    public static Set<String> de(Biome b, BlockPos pos) {
        Set<String> s = new HashSet<>();
        s.add("partout");
        boolean froid = BiomeDictionary.hasType(b, Type.COLD) || BiomeDictionary.hasType(b, Type.SNOWY);
        boolean neige = BiomeDictionary.hasType(b, Type.SNOWY);
        boolean foret = BiomeDictionary.hasType(b, Type.FOREST);
        boolean montagne = BiomeDictionary.hasType(b, Type.MOUNTAIN) || BiomeDictionary.hasType(b, Type.HILLS);
        if (BiomeDictionary.hasType(b, Type.PLAINS) && !froid && !BiomeDictionary.hasType(b, Type.SAVANNA)) {
            s.add("plaines");
            s.add("prairies");
            s.add("lisiere");
        }
        if (BiomeDictionary.hasType(b, Type.HILLS) && !froid) s.add("collines");
        if (montagne) {
            s.add("montagne");
            if (foret || BiomeDictionary.hasType(b, Type.CONIFEROUS) || b == Biomes.EXTREME_HILLS_WITH_TREES) s.add("montagne_boisee");
        }
        if (neige && (montagne || b == Biomes.ICE_PLAINS || b == Biomes.MUTATED_ICE_FLATS || pos.getY() > 100)) s.add("pics_glaces");
        if (BiomeDictionary.hasType(b, Type.SAVANNA)) {
            s.add("savane");
            s.add("aride");
        }
        if (BiomeDictionary.hasType(b, Type.SANDY) && BiomeDictionary.hasType(b, Type.HOT) && !BiomeDictionary.hasType(b, Type.BEACH)) s.add("desert");
        if (BiomeDictionary.hasType(b, Type.MESA) || (BiomeDictionary.hasType(b, Type.DRY) && BiomeDictionary.hasType(b, Type.HOT))) s.add("aride");
        if (foret && !froid) {
            s.add("foret");
            s.add("lisiere");
            if (!BiomeDictionary.hasType(b, Type.DENSE)) s.add("foret_claire");
        }
        if (b == Biomes.BIRCH_FOREST || b == Biomes.BIRCH_FOREST_HILLS || b == Biomes.MUTATED_BIRCH_FOREST || b == Biomes.MUTATED_BIRCH_FOREST_HILLS)
            s.add("foret_bouleau");
        if (b == Biomes.ROOFED_FOREST || b == Biomes.MUTATED_ROOFED_FOREST || (foret && BiomeDictionary.hasType(b, Type.SPOOKY))) s.add("foret_sombre");
        if ((foret && BiomeDictionary.hasType(b, Type.WET)) || BiomeDictionary.hasType(b, Type.SWAMP)) s.add("foret_humide");
        if (BiomeDictionary.hasType(b, Type.CONIFEROUS)) {
            if (neige) s.add("taiga_enneigee");
            else s.add("taiga");
        }
        if (BiomeDictionary.hasType(b, Type.SWAMP)) s.add("marais");
        if (BiomeDictionary.hasType(b, Type.RIVER)) s.add("riviere");
        if (BiomeDictionary.hasType(b, Type.JUNGLE)) s.add("jungle");
        if (BiomeDictionary.hasType(b, Type.BEACH)) s.add("plage");
        return s;
    }

    /** Milieux souterrains d'une position (sans ciel). */
    public static void sousTerre(Set<String> s, int y) {
        s.add("grotte");
        if (y < 30) s.add("grotte_profonde");
        if (y < 12) s.add("abysses");
    }
}
