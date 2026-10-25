package fr.lenerfvoeux.hxrp.bestiaire.client;

import fr.lenerfvoeux.hxrp.bestiaire.CommonProxy;
import fr.lenerfvoeux.hxrp.bestiaire.Espece;
import fr.lenerfvoeux.hxrp.bestiaire.Especes;
import fr.lenerfvoeux.hxrp.bestiaire.HxrpBestiaire;
import fr.lenerfvoeux.hxrp.bestiaire.Registre;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntityProjectileBoss;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import software.bernie.geckolib3.GeckoLib;
import software.bernie.geckolib3.file.AnimationFile;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.resource.GeckoLibCache;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit() {
        // GeckoLib lit modèles et animations à chaque rechargement des ressources (appel sans effet si le Phénix l'a déjà fait)
        GeckoLib.initialize();
        for (Espece e : Especes.TOUTES) rendu(e);
        RenderingRegistry.registerEntityRenderingHandler(EntityProjectileBoss.class, RenduProjectile::new);
        if (EssaiRendu.ACTIF) MinecraftForge.EVENT_BUS.register(new EssaiRendu());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void rendu(Espece e) {
        RenderingRegistry.registerEntityRenderingHandler((Class) e.classe, (IRenderFactory) m -> new RenduBestiaire(m, e));
    }

    @Override
    public void init() {
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(r -> controler());
    }

    /** À chaque rechargement des ressources : chaque modèle et ses animations de base sont-ils lus par GeckoLib ? */
    private static void controler() {
        try {
            List<String> manquants = new ArrayList<>();
            Set<String> modeles = new LinkedHashSet<>();
            int n = 0;
            for (Espece e : Especes.TOUTES) {
                GeoModel g = GeckoLibCache.getInstance().getGeoModels().get(new ResourceLocation(HxrpBestiaire.MODID, "geo/" + e.modele + ".geo.json"));
                AnimationFile a = GeckoLibCache.getInstance().getAnimations()
                        .get(new ResourceLocation(HxrpBestiaire.MODID, "animations/" + e.modele + ".animation.json"));
                if (g == null) manquants.add("modèle " + e.modele);
                if (a == null) {
                    manquants.add("animations " + e.modele);
                    continue;
                }
                for (String an : new String[]{e.repos, e.marche, e.mort, e.apparition}) {
                    if (an.isEmpty()) continue;
                    if (a.getAnimation(e.animation(an)) == null) manquants.add(e.animation(an));
                }
                if (modeles.add(e.modele)) n += a.getAllAnimations().size();
            }
            if (manquants.isEmpty())
                HxrpBestiaire.LOG.info("Bestiaire : {} modèles et {} animations GeckoLib chargés", modeles.size(), n);
            else
                HxrpBestiaire.LOG.error("Bestiaire : ressources GeckoLib incomplètes : {}", manquants);
        } catch (RuntimeException ex) {
            HxrpBestiaire.LOG.error("Bestiaire : GeckoLib n'a pas pu lire les ressources", ex);
        }
    }

    @Mod.EventBusSubscriber(modid = HxrpBestiaire.MODID, value = Side.CLIENT)
    public static class Modeles {
        @SubscribeEvent
        public static void modeles(ModelRegistryEvent e) {
            for (Item i : Registre.objets())
                ModelLoader.setCustomModelResourceLocation(i, 0, new ModelResourceLocation(i.getRegistryName(), "inventory"));
        }
    }
}
