package fr.lenerfvoeux.hxrp.phenix.client;

import fr.lenerfvoeux.hxrp.phenix.HxrpPhenix;
import fr.lenerfvoeux.hxrp.phenix.entite.EntityPlumeArdente;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

public class RenduPlumeArdente extends Render<EntityPlumeArdente> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(HxrpPhenix.MODID, "textures/entity/plume_ardente.png");

    public RenduPlumeArdente(RenderManager rm) {
        super(rm);
        shadowSize = 0F;
    }

    @Override
    public void doRender(EntityPlumeArdente e, double x, double y, double z, float cap, float pt) {
        bindEntityTexture(e);
        float tour = e.tourAvant + (e.tour - e.tourAvant) * pt;
        Panneau.dessiner(renderManager, e, x, y + 0.25, z, 0.9F, (e.ticksExisted / 2) % 4, 4, tour, true);
        super.doRender(e, x, y, z, cap, pt);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityPlumeArdente e) {
        return TEXTURE;
    }
}
