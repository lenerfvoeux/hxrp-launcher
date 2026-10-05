package fr.lenerfvoeux.hxrp.phenix;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.registries.IForgeRegistry;

/** Sons du Phénix (synthétisés par tools/phenix/sons.py). */
public final class Sons {
    public static SoundEvent CRI, CRI_COURT, AILES, SOUFFLE, BOULE, IMPACT, PLONGEE, ONDE, TEMPETE, OEUF, CRAQUEMENT, RENAISSANCE, MORT;

    private Sons() {}

    static void enregistrer(IForgeRegistry<SoundEvent> r) {
        CRI = son(r, "cri");
        CRI_COURT = son(r, "cri_court");
        AILES = son(r, "ailes");
        SOUFFLE = son(r, "souffle");
        BOULE = son(r, "boule");
        IMPACT = son(r, "impact");
        PLONGEE = son(r, "plongee");
        ONDE = son(r, "onde");
        TEMPETE = son(r, "tempete");
        OEUF = son(r, "oeuf");
        CRAQUEMENT = son(r, "craquement");
        RENAISSANCE = son(r, "renaissance");
        MORT = son(r, "mort");
    }

    private static SoundEvent son(IForgeRegistry<SoundEvent> r, String nom) {
        ResourceLocation id = new ResourceLocation(HxrpPhenix.MODID, nom);
        SoundEvent s = new SoundEvent(id).setRegistryName(id);
        r.register(s);
        return s;
    }
}
