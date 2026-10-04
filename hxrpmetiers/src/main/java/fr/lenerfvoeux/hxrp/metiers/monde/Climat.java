package fr.lenerfvoeux.hxrp.metiers.monde;

import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;

import java.util.Set;

/** Classe un biome dans un des climats de recolte.json. */
public final class Climat {
    public static final String TEMPERE = "tempere", CHAUD = "chaud", TROPICAL = "tropical", HUMIDE = "humide", FROID = "froid",
            PLAGE = "plage", OCEAN = "ocean", OCEAN_FROID = "ocean_froid", RIVIERE = "riviere", MONTAGNE = "montagne";

    private Climat() {}

    public static String de(Biome b) {
        Set<Type> t = BiomeDictionary.getTypes(b);
        if (t.contains(Type.OCEAN)) return t.contains(Type.COLD) ? OCEAN_FROID : OCEAN;
        if (t.contains(Type.RIVER)) return RIVIERE;
        if (t.contains(Type.BEACH)) return PLAGE;
        if (t.contains(Type.JUNGLE)) return TROPICAL;
        if (t.contains(Type.SWAMP) || t.contains(Type.WET) && !t.contains(Type.HOT)) return HUMIDE;
        if (t.contains(Type.MOUNTAIN) || t.contains(Type.HILLS) && !t.contains(Type.FOREST)) return MONTAGNE;
        if (t.contains(Type.COLD) || t.contains(Type.CONIFEROUS) || t.contains(Type.SNOWY)) return FROID;
        if (t.contains(Type.SAVANNA) || t.contains(Type.MESA) || t.contains(Type.HOT) && t.contains(Type.DRY)) return CHAUD;
        return TEMPERE;
    }

    /** Un animal ou une culture de ce milieu a-t-il sa place dans ce biome ? */
    public static boolean convient(String milieu, Biome b) {
        String c = de(b);
        if (milieu.equals(c)) return true;
        if (milieu.equals(OCEAN) && c.equals(OCEAN_FROID)) return true;
        if (milieu.equals(MONTAGNE) && c.equals(FROID) && BiomeDictionary.hasType(b, Type.HILLS)) return true;
        return false;
    }
}
