package fr.lenerfvoeux.hxrp.bestiaire.entite.sbire;

import fr.lenerfvoeux.hxrp.bestiaire.Especes;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntitySbire;
import net.minecraft.world.World;

/** Rat des glaces, de l'escorte du Roi : rapide, il mord en meute. */
public class EntityRatDesGlaces extends EntitySbire {
    private static final String[] ATTAQUES = {"attack_1", "attack_2"};

    public EntityRatDesGlaces(World w) {
        super(w, Especes.RAT_DES_GLACES, Comportement.CONTACT);
    }

    @Override
    protected String[] attaques() {
        return ATTAQUES;
    }
}
