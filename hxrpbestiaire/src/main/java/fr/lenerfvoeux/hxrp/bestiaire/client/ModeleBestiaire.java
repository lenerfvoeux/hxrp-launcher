package fr.lenerfvoeux.hxrp.bestiaire.client;

import fr.lenerfvoeux.hxrp.bestiaire.Espece;
import fr.lenerfvoeux.hxrp.bestiaire.Especes;
import fr.lenerfvoeux.hxrp.bestiaire.HxrpBestiaire;
import fr.lenerfvoeux.hxrp.bestiaire.entite.Bestiole;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedGeoModel;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Modèle GeckoLib commun : geo, animations et texture portent le nom du modèle de l'espèce (générés par
 * tools/bestiaire/gen.py). Les os d'effets (rayons, lames…) restent cachés hors des animations qui les montrent.
 */
public class ModeleBestiaire<T extends EntityLiving & Bestiole> extends AnimatedGeoModel<T> {
    private final Espece espece;
    private final ResourceLocation modele, animations, texture;
    /** Os cachés par l'une des espèces qui partagent ce modèle. */
    private final Set<String> os = new LinkedHashSet<>();

    public ModeleBestiaire(Espece e) {
        this.espece = e;
        for (Espece x : Especes.TOUTES) if (x.modele.equals(e.modele)) os.addAll(x.cachees.keySet());
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

    /**
     * Les os de GeckoLib sont partagés par toutes les espèces d'un même modèle (Xerra et ses araignées) : chaque rendu
     * repose donc l'état de tous les os qu'une de ces espèces cache, pas seulement des siens.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void setLivingAnimations(T e, Integer id, AnimationEvent ev) {
        super.setLivingAnimations(e, id, ev);
        if (os.isEmpty()) return;
        String a = e.animationCourante();
        for (String nom : os) {
            IBone b = getAnimationProcessor().getBone(nom);
            if (b == null) continue;
            List<String> visible = espece.cachees.get(nom);
            b.setHidden(visible != null && !visible.contains(a));
        }
    }
}
