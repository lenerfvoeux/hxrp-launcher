package fr.lenerfvoeux.hxrp.phenix.client;

import fr.lenerfvoeux.hxrp.phenix.HxrpPhenix;
import fr.lenerfvoeux.hxrp.phenix.entite.EntityPhenix;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedGeoModel;

/**
 * Modèle GeckoLib du Phénix (assets/hxrpphenix/geo, animations, textures/entity, générés par tools/phenix/phenix.py).
 * Quatre textures se relaient pour faire danser les flammes ; le corps s'incline dans les piqués et les virages ;
 * l'oiseau ou l'œuf de cendres est affiché selon l'état.
 */
public class ModelePhenix extends AnimatedGeoModel<EntityPhenix> {
    static final ResourceLocation MODELE = new ResourceLocation(HxrpPhenix.MODID, "geo/phenix.geo.json");
    static final ResourceLocation ANIMATIONS = new ResourceLocation(HxrpPhenix.MODID, "animations/phenix.animation.json");
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[4];

    static {
        for (int i = 0; i < TEXTURES.length; i++) TEXTURES[i] = new ResourceLocation(HxrpPhenix.MODID, "textures/entity/phenix_" + i + ".png");
    }

    @Override
    public ResourceLocation getModelLocation(EntityPhenix e) {
        return MODELE;
    }

    @Override
    public ResourceLocation getTextureLocation(EntityPhenix e) {
        return TEXTURES[((e.ticksExisted + e.getEntityId()) / 3) % TEXTURES.length];
    }

    @Override
    public ResourceLocation getAnimationFileLocation(EntityPhenix e) {
        return ANIMATIONS;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void setLivingAnimations(EntityPhenix e, Integer id, AnimationEvent ev) {
        super.setLivingAnimations(e, id, ev);
        IBone oeuf = getAnimationProcessor().getBone("oeuf");
        IBone assiette = getAnimationProcessor().getBone("assiette");
        if (oeuf == null || assiette == null) return;
        boolean enOeuf = e.etat() == EntityPhenix.OEUF;
        oeuf.setHidden(!enOeuf);
        assiette.setHidden(enOeuf);
        if (!enOeuf) {
            float pt = ev == null ? 1F : ev.getPartialTick();
            float tangage = e.prevTangage + (e.tangage - e.prevTangage) * pt;
            float roulis = e.prevRoulis + (e.roulis - e.prevRoulis) * pt;
            // GeckoLib : une rotation x positive (en interne) lève le bec ; z positive abaisse l'aile gauche
            assiette.setRotationX(tangage * ((float) Math.PI / 180F));
            assiette.setRotationZ(MathHelper.clamp(roulis, -40F, 40F) * ((float) Math.PI / 180F));
        }
    }
}
