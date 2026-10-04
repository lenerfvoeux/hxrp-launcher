package fr.lenerfvoeux.hxrp.metiers.virus.block;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;

/** Enregistrement des tile entities du Virus. */
public final class VirusTiles {
    private VirusTiles() {}

    public static void enregistrer() {
        GameRegistry.registerTileEntity(TileMeuble.class, new ResourceLocation(HxrpMetiers.MODID, "meuble_a_tiroirs"));
    }
}
