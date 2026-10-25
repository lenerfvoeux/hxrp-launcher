package fr.lenerfvoeux.hxrp.bestiaire.client;

import fr.lenerfvoeux.hxrp.bestiaire.Espece;
import fr.lenerfvoeux.hxrp.bestiaire.Especes;
import fr.lenerfvoeux.hxrp.bestiaire.HxrpBestiaire;
import fr.lenerfvoeux.hxrp.bestiaire.entite.Bestiole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import software.bernie.geckolib3.core.builder.Animation;
import software.bernie.geckolib3.file.AnimationFile;
import software.bernie.geckolib3.resource.GeckoLibCache;

import java.util.ArrayList;
import java.util.List;

/**
 * Essai du rendu, pour la CI (lancé avec -Dhxrpbestiaire.essaiRendu=true) : le client ouvre un monde plat, dessine
 * chaque espèce devant la caméra dans chacune de ses animations, puis se ferme. Une erreur de rendu GeckoLib plante le
 * client comme en jeu (« Rendering entity in world ») ; sinon le journal compte les espèces et animations dessinées.
 */
public class EssaiRendu {
    public static final boolean ACTIF = Boolean.getBoolean("hxrpbestiaire.essaiRendu");
    /** Images dessinées par animation, et images laissées au monde pour s'installer avant le premier dessin. */
    private static final int IMAGES = 6, ATTENTE = 60;

    private boolean monde;
    private int attente, espece, animation, images, total;
    private EntityLiving bete;
    private final List<String> animations = new ArrayList<>();

    @SubscribeEvent
    public void menu(TickEvent.ClientTickEvent ev) {
        Minecraft mc = Minecraft.getMinecraft();
        if (ev.phase != TickEvent.Phase.END || monde || !(mc.currentScreen instanceof GuiMainMenu)) return;
        monde = true;
        WorldSettings reglages = new WorldSettings(0L, GameType.CREATIVE, false, false, WorldType.FLAT);
        reglages.enableCommands();
        HxrpBestiaire.LOG.info("Bestiaire : essai du rendu dans un monde plat");
        mc.launchIntegratedServer("essai-bestiaire", "Essai du bestiaire", reglages);
    }

    @SubscribeEvent
    public void dessiner(RenderWorldLastEvent ev) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null || espece >= Especes.TOUTES.size() || ++attente < ATTENTE) return;
        Espece e = Especes.TOUTES.get(espece);
        if (bete == null) {
            bete = creer(e, mc.world);
            bete.setPosition(mc.player.posX, mc.player.posY, mc.player.posZ + 4 + e.largeur * e.echelle);
            animations.clear();
            animations.add("");
            AnimationFile f = GeckoLibCache.getInstance().getAnimations()
                    .get(new ResourceLocation(HxrpBestiaire.MODID, "animations/" + e.modele + ".animation.json"));
            String prefixe = "animation." + e.modele + ".";
            for (Animation a : f.getAllAnimations()) animations.add(a.animationName.substring(prefixe.length()));
            animation = 0;
            images = 0;
        }
        if (images == 0) ((Bestiole) bete).jouerAnimation(animations.get(animation));
        RenderManager rm = mc.getRenderManager();
        RenderHelper.enableStandardItemLighting();
        rm.renderEntity(bete, bete.posX - rm.viewerPosX, bete.posY - rm.viewerPosY, bete.posZ - rm.viewerPosZ, 0F,
                ev.getPartialTicks(), false);
        RenderHelper.disableStandardItemLighting();
        if (++images < IMAGES) return;
        images = 0;
        total++;
        if (++animation < animations.size()) return;
        HxrpBestiaire.LOG.info("Bestiaire : rendu de {} : {} animations dessinées", e.id, animations.size());
        bete = null;
        if (++espece < Especes.TOUTES.size()) return;
        HxrpBestiaire.LOG.info("Bestiaire : rendu essayé : {} espèces, {} animations dessinées sans erreur", espece, total);
        mc.shutdown();
    }

    private static EntityLiving creer(Espece e, World w) {
        try {
            return e.classe.getConstructor(World.class).newInstance(w);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Bestiaire : impossible de créer " + e.id, ex);
        }
    }
}
