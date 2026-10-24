package fr.lenerfvoeux.hxrp.bestiaire.client;

import fr.lenerfvoeux.hxrp.bestiaire.Espece;
import fr.lenerfvoeux.hxrp.bestiaire.HxrpBestiaire;
import fr.lenerfvoeux.hxrp.bestiaire.entite.Bestiole;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedGeoModel;

import java.util.List;
import java.util.Map;

/**
 * Modèle GeckoLib commun : geo, animations et texture portent le nom du modèle de l'espèce (générés par
 * tools/bestiaire/gen.py). Les os d'effets (rayons, lames…) restent cachés hors des animations qui les montrent.
 */
public class ModeleBestiaire<T extends EntityLiving & Bestiole> extends AnimatedGeoModel<T> {
    private final Espece espece;
    private final ResourceLocation modele, animations, texture;

    public ModeleBestiaire(Espece e) {
        this.espece = e;
        modele = new ResourceLocation(HxrpBestiaire.MODID, "geo/" + e.modele + ".geo.json");
        animations = new ResourceLocation(HxrpBestiaire.MODID, "animations/" + e.modele + ".animation.json");
        texture = new ResourceLocation(HxrpBestiaire.MODID, "textures/entity/" + e.modele + ".png");
    }

    @Override
    public ResourceLocation getModelLocation(T e) {
        return modele;
    }

    @Override
    public ResourceLocation getTextureLocation(T e) {
        return texture;
    }

    @Override
    public ResourceLocation getAnimationFileLocation(T e) {
        return animations;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void setLivingAnimations(T e, Integer id, AnimationEvent ev) {
        super.setLivingAnimations(e, id, ev);
        if (espece.cachees.isEmpty()) return;
        String a = e.animationCourante();
        for (Map.Entry<String, List<String>> c : espece.cachees.entrySet()) {
            IBone b = getAnimationProcessor().getBone(c.getKey());
            if (b != null) b.setHidden(!c.getValue().contains(a));
        }
    }
}
