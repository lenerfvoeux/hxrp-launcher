package fr.lenerfvoeux.hxrp.phenix.client;

import fr.lenerfvoeux.hxrp.phenix.CommonProxy;
import fr.lenerfvoeux.hxrp.phenix.HxrpPhenix;
import fr.lenerfvoeux.hxrp.phenix.Registre;
import fr.lenerfvoeux.hxrp.phenix.entite.EntityBouleDeFeu;
import fr.lenerfvoeux.hxrp.phenix.entite.EntityPhenix;
import fr.lenerfvoeux.hxrp.phenix.entite.EntityPlumeArdente;
import fr.lenerfvoeux.hxrp.phenix.entite.EntityPlumeObjet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit() {
        RenderingRegistry.registerEntityRenderingHandler(EntityPhenix.class, RenduPhenix::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityBouleDeFeu.class, RenduBouleDeFeu::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityPlumeArdente.class, RenduPlumeArdente::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityPlumeObjet.class, m -> new RenderEntityItem(m, Minecraft.getMinecraft().getRenderItem()));
    }

    @Mod.EventBusSubscriber(modid = HxrpPhenix.MODID, value = Side.CLIENT)
    public static class Modeles {
        @SubscribeEvent
        public static void modeles(ModelRegistryEvent e) {
            for (Item i : new Item[]{Registre.PLUME, Registre.OEUF})
                ModelLoader.setCustomModelResourceLocation(i, 0, new ModelResourceLocation(i.getRegistryName(), "inventory"));
        }
    }
}
