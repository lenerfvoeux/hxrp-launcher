package fr.lenerfvoeux.hxrp.metiers.client.entite;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.entite.EntityPoisson;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;

/** Rendu commun des animaux du Gourmet : modèle JSON, texture, échelle, petits. */
public class RenduAnimal<T extends EntityLiving> extends RenderLiving<T> {
    private final ResourceLocation texture;
    private final float echelle;

    public RenduAnimal(RenderManager rm, String id) {
        this(rm, id, ModeleJson.charger(id));
    }

    private RenduAnimal(RenderManager rm, String id, ModeleJson m) {
        super(rm, m, m.ombre);
        this.texture = new ResourceLocation(HxrpMetiers.MODID, "textures/entity/" + id + ".png");
        this.echelle = m.echelle;
    }

    @Override
    protected ResourceLocation getEntityTexture(T e) {
        return texture;
    }

    @Override
    protected void preRenderCallback(T e, float pt) {
        float s = echelle * (e.isChild() ? 0.55f : 1.0f);
        GlStateManager.scale(s, s, s);
    }

    @Override
    protected void applyRotations(T e, float age, float yaw, float pt) {
        super.applyRotations(e, age, yaw, pt);
        if (e instanceof EntityPoisson) {
            if (!e.isInWater()) {
                // hors de l'eau, le poisson est couché sur le flanc et se débat
                GlStateManager.translate(0.0f, e.height * 0.5f, 0.0f);
                GlStateManager.rotate(90.0f, 0.0f, 0.0f, 1.0f);
            } else {
                GlStateManager.rotate(e.prevRotationPitch + (e.rotationPitch - e.prevRotationPitch) * pt, 1.0f, 0.0f, 0.0f);
            }
        }
    }
}
