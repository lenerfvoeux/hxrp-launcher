package fr.lenerfvoeux.hxrp.bestiaire;

import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.registries.IForgeRegistry;

/** Sons du bestiaire (synthétisés par tools/bestiaire/sons.py), puis attribués aux espèces. */
public final class Sons {
    public static SoundEvent BOURDON, BOURDON_AIGU, STRIDULATION, REINE_MORT, RALE, GARGOUILLIS, CHAIR_MORT, MURMURE, PLAINTE, AME_MORT,
            INCANTATION;

    private Sons() {}

    static void enregistrer(IForgeRegistry<SoundEvent> r) {
        BOURDON = son(r, "bourdon");
        BOURDON_AIGU = son(r, "bourdon_aigu");
        STRIDULATION = son(r, "stridulation");
        REINE_MORT = son(r, "reine_mort");
        RALE = son(r, "rale");
        GARGOUILLIS = son(r, "gargouillis");
        CHAIR_MORT = son(r, "chair_mort");
        MURMURE = son(r, "murmure");
        PLAINTE = son(r, "plainte");
        AME_MORT = son(r, "ame_mort");
        INCANTATION = son(r, "incantation");
        Especes.REINE_GUEPE.sons(BOURDON, STRIDULATION, REINE_MORT, 1F);
        Especes.GUEPE_SOLDAT.sons(BOURDON_AIGU, STRIDULATION, REINE_MORT, 1.7F);
        Especes.ABOMINATION.sons(RALE, GARGOUILLIS, CHAIR_MORT, 1F);
        Especes.NECROMANCIEN.sons(MURMURE, PLAINTE, AME_MORT, 1F);
        Especes.ARCHER_SQUELETTE.sons(SoundEvents.ENTITY_SKELETON_AMBIENT, SoundEvents.ENTITY_SKELETON_HURT, SoundEvents.ENTITY_SKELETON_DEATH, 0.75F);
    }

    private static SoundEvent son(IForgeRegistry<SoundEvent> r, String nom) {
        ResourceLocation id = new ResourceLocation(HxrpBestiaire.MODID, nom);
        SoundEvent s = new SoundEvent(id).setRegistryName(id);
        r.register(s);
        return s;
    }
}
