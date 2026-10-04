package fr.lenerfvoeux.hxrp.metiers.virus;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.registries.IForgeRegistry;

/** Sons du Hunter Virus (assets/hxrpmetiers/sounds, générés par tools/virus/sons.py). */
public final class VirusSons {
    public static SoundEvent TOUX, ETERNUEMENT, SIFFLEMENT, MURMURE, BATTEMENT, PILON, BOUILLON, VERRE, SERINGUE, LAME, PAGE, MEULE;

    private VirusSons() {}

    static void enregistrer(IForgeRegistry<SoundEvent> r) {
        TOUX = son(r, "virus.toux");
        ETERNUEMENT = son(r, "virus.eternuement");
        SIFFLEMENT = son(r, "virus.sifflement");
        MURMURE = son(r, "virus.murmure");
        BATTEMENT = son(r, "virus.battement");
        PILON = son(r, "virus.pilon");
        BOUILLON = son(r, "virus.bouillon");
        VERRE = son(r, "virus.verre");
        SERINGUE = son(r, "virus.seringue");
        LAME = son(r, "virus.lame");
        PAGE = son(r, "virus.page");
        MEULE = son(r, "virus.meule");
    }

    private static SoundEvent son(IForgeRegistry<SoundEvent> r, String nom) {
        ResourceLocation rl = new ResourceLocation(HxrpMetiers.MODID, nom);
        SoundEvent s = new SoundEvent(rl).setRegistryName(rl);
        r.register(s);
        return s;
    }
}
