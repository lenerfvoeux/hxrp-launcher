package fr.lenerfvoeux.hxrp.bestiaire.client;

import fr.lenerfvoeux.hxrp.bestiaire.Espece;
import fr.lenerfvoeux.hxrp.bestiaire.entite.Bestiole;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLiving;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

/** Rendu GeckoLib commun ; un sbire « boss en petit » est dessiné à l'échelle de son espèce. */
public class RenduBestiaire<T extends EntityLiving & Bestiole> extends GeoEntityRenderer<T> {
    public RenduBestiaire(RenderManager rm, Espece e) {
        super(rm, new ModeleBestiaire<T>(e));
        shadowSize = Math.min(2.5F, e.largeur * 0.6F);
    }

    @Override
    protected void applyRotations(T e, float age, float lacet, float pt) {
        super.applyRotations(e, age, lacet, pt);
        float s = e.echelleRendu();
        if (s != 1F) GlStateManager.scale(s, s, s);
    }

    /** Pas de bascule sur le flanc à la mort : l'animation de mort s'en charge. */
    @Override
    protected float getDeathMaxRotation(T e) {
        return 0F;
    }
}
