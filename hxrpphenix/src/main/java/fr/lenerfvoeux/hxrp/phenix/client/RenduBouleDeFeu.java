package fr.lenerfvoeux.hxrp.phenix.client;

import fr.lenerfvoeux.hxrp.phenix.HxrpPhenix;
import fr.lenerfvoeux.hxrp.phenix.entite.EntityBouleDeFeu;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

public class RenduBouleDeFeu extends Render<EntityBouleDeFeu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(HxrpPhenix.MODID, "textures/entity/boule_de_feu.png");

    public RenduBouleDeFeu(RenderManager rm) {
        super(rm);
        shadowSize = 0F;
    }

    @Override
    public void doRender(EntityBouleDeFeu e, double x, double y, double z, float cap, float pt) {
        bindEntityTexture(e);
        Panneau.dessiner(renderManager, e, x, y + 0.5, z, 1.7F, (e.ticksExisted / 2) % 4, 4, (e.ticksExisted + pt) * 9F, true);
        super.doRender(e, x, y, z, cap, pt);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityBouleDeFeu e) {
        return TEXTURE;
    }
}
