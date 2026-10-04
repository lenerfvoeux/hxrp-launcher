package fr.lenerfvoeux.hxrp.metiers.virus.client;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.virus.VirusRegistre;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Modèles des objets du Virus (models/item/&lt;id&gt;.json). */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = HxrpMetiers.MODID, value = Side.CLIENT)
public final class ModelesVirus {
    private ModelesVirus() {}

    @SubscribeEvent
    public static void modeles(ModelRegistryEvent e) {
        for (Item i : VirusRegistre.OBJETS)
            ModelLoader.setCustomModelResourceLocation(i, 0, new ModelResourceLocation(i.getRegistryName(), "inventory"));
        fr.lenerfvoeux.hxrp.metiers.virus.client.jeu.ClientJeux.modeles();
    }
}
