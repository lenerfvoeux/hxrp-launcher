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
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import software.bernie.geckolib3.GeckoLib;
import software.bernie.geckolib3.file.AnimationFile;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.resource.GeckoLibCache;

import java.util.ArrayList;
import java.util.List;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit() {
        // GeckoLib lit modèles et animations à chaque rechargement des ressources. Appelé ici et non dans le
        // constructeur du mod : à ce moment-là, les ressources des mods ne sont pas encore montées.
        GeckoLib.initialize();
        RenderingRegistry.registerEntityRenderingHandler(EntityPhenix.class, RenduPhenix::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityBouleDeFeu.class, RenduBouleDeFeu::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityPlumeArdente.class, RenduPlumeArdente::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityPlumeObjet.class, m -> new RenderEntityItem(m, Minecraft.getMinecraft().getRenderItem()));
    }

    @Override
    public void init() {
        // Contrôle à chaque rechargement des ressources : le modèle et toutes les animations du Phénix sont-ils lus ?
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(r -> controler());
    }

    private static void controler() {
        try {
            GeoModel modele = GeckoLibCache.getInstance().getGeoModels().get(ModelePhenix.MODELE);
            AnimationFile anims = GeckoLibCache.getInstance().getAnimations().get(ModelePhenix.ANIMATIONS);
            List<String> manquantes = new ArrayList<>();
            for (String a : ANIMATIONS)
                if (anims == null || anims.getAnimation("animation.phenix." + a) == null) manquantes.add(a);
            if (modele != null && manquantes.isEmpty())
                HxrpPhenix.LOG.info("Phénix : modèle et {} animations GeckoLib chargés", ANIMATIONS.length);
            else
                HxrpPhenix.LOG.error("Phénix : ressources GeckoLib incomplètes (modèle {}, animations manquantes {})",
                        modele != null ? "lu" : "absent", manquantes);
        } catch (RuntimeException ex) {
            HxrpPhenix.LOG.error("Phénix : GeckoLib n'a pas pu lire les ressources", ex);
        }
    }

    private static final String[] ANIMATIONS = {"vol", "plane", "plongee", "souffle", "boule", "pluie", "tempete", "perche",
            "oeuf", "renaissance", "mort"};

    @Mod.EventBusSubscriber(modid = HxrpPhenix.MODID, value = Side.CLIENT)
    public static class Modeles {
        @SubscribeEvent
        public static void modeles(ModelRegistryEvent e) {
            for (Item i : new Item[]{Registre.PLUME, Registre.OEUF})
                ModelLoader.setCustomModelResourceLocation(i, 0, new ModelResourceLocation(i.getRegistryName(), "inventory"));
        }
    }
}
