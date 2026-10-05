package fr.lenerfvoeux.hxrp.phenix.client;

import fr.lenerfvoeux.hxrp.phenix.entite.EntityPhenix;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

/** Le Phénix brille de son propre feu : rendu à pleine lumière, de jour comme de nuit, même à l'ombre. */
public class RenduPhenix extends GeoEntityRenderer<EntityPhenix> {
    public RenduPhenix(RenderManager rm) {
        super(rm, new ModelePhenix());
        shadowSize = 1.1F;
    }

    @Override
    public void doRender(EntityPhenix e, double x, double y, double z, float cap, float pt) {
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        GlStateManager.enableAlpha();
        super.doRender(e, x, y, z, cap, pt);
        Panneau.lumiereDuMonde(e);
    }

    /** Pas de bascule sur le flanc à la mort : l'animation « mort » s'en charge (il retombe en cendres). */
    @Override
    protected float getDeathMaxRotation(EntityPhenix e) {
        return 0F;
    }
}
