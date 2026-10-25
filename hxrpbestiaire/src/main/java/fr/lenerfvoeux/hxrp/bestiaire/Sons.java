package fr.lenerfvoeux.hxrp.bestiaire;

import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.registries.IForgeRegistry;

/** Sons du bestiaire (synthétisés par tools/bestiaire/sons.py), puis attribués aux espèces. */
public final class Sons {
    public static SoundEvent BOURDON, BOURDON_AIGU, STRIDULATION, REINE_MORT, RALE, GARGOUILLIS, CHAIR_MORT, MURMURE, PLAINTE, AME_MORT,
            INCANTATION, SIFFLEMENT, CRI_XERRA, XERRA_MORT, CRAQUEMENT_GLACE, GROGNEMENT_GLACIAL, ROI_MORT, COUINEMENT, GROGNEMENT_GOBELIN,
            GRONDEMENT_DEMON, RUGISSEMENT_DEMON, DEMON_MORT;

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
        SIFFLEMENT = son(r, "sifflement");
        CRI_XERRA = son(r, "cri_xerra");
        XERRA_MORT = son(r, "xerra_mort");
        CRAQUEMENT_GLACE = son(r, "craquement_glace");
        GROGNEMENT_GLACIAL = son(r, "grognement_glacial");
        ROI_MORT = son(r, "roi_mort");
        COUINEMENT = son(r, "couinement");
        GROGNEMENT_GOBELIN = son(r, "grognement_gobelin");
        GRONDEMENT_DEMON = son(r, "grondement_demon");
        RUGISSEMENT_DEMON = son(r, "rugissement_demon");
        DEMON_MORT = son(r, "demon_mort");
        Especes.REINE_GUEPE.sons(BOURDON, STRIDULATION, REINE_MORT, 1F);
        Especes.GUEPE_SOLDAT.sons(BOURDON_AIGU, STRIDULATION, REINE_MORT, 1.7F);
        Especes.ABOMINATION.sons(RALE, GARGOUILLIS, CHAIR_MORT, 1F);
        Especes.NECROMANCIEN.sons(MURMURE, PLAINTE, AME_MORT, 1F);
        Especes.ARCHER_SQUELETTE.sons(SoundEvents.ENTITY_SKELETON_AMBIENT, SoundEvents.ENTITY_SKELETON_HURT, SoundEvents.ENTITY_SKELETON_DEATH, 0.75F);
        Especes.XERRA.sons(SIFFLEMENT, CRI_XERRA, XERRA_MORT, 1F);
        Especes.ARAIGNEE_XERRA.sons(SoundEvents.ENTITY_SPIDER_AMBIENT, SoundEvents.ENTITY_SPIDER_HURT, SoundEvents.ENTITY_SPIDER_DEATH, 1.3F);
        Especes.ROI_DES_GLACES.sons(CRAQUEMENT_GLACE, GROGNEMENT_GLACIAL, ROI_MORT, 1F);
        Especes.RAT_DES_GLACES.sons(COUINEMENT, COUINEMENT, COUINEMENT, 1.2F);
        Especes.GOBELIN_BOUCLIER.sons(GROGNEMENT_GOBELIN, GROGNEMENT_GOBELIN, GROGNEMENT_GOBELIN, 0.85F);
        Especes.GOBELIN_ARCHER.sons(GROGNEMENT_GOBELIN, GROGNEMENT_GOBELIN, GROGNEMENT_GOBELIN, 1.1F);
        Especes.DEMON_NOIR.sons(GRONDEMENT_DEMON, RUGISSEMENT_DEMON, DEMON_MORT, 1F);
    }

    private static SoundEvent son(IForgeRegistry<SoundEvent> r, String nom) {
        ResourceLocation id = new ResourceLocation(HxrpBestiaire.MODID, nom);
        SoundEvent s = new SoundEvent(id).setRegistryName(id);
        r.register(s);
        return s;
    }
}
