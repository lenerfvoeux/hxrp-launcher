package fr.lenerfvoeux.hxrp.bestiaire.entite.sbire;

import fr.lenerfvoeux.hxrp.bestiaire.Especes;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntitySbire;
import net.minecraft.world.World;

/** Petit kraken, éclos des œufs du Kraken : le Kraken en petit ; il nage vers sa cible et la fouette de ses tentacules. */
public class EntityPetitKraken extends EntitySbire {
    private static final String[] ATTAQUES = {"x1dr4_meleeattack_2"};

    public EntityPetitKraken(World w) {
        super(w, Especes.PETIT_KRAKEN, Comportement.NAGE);
    }

    @Override
    protected String[] attaques() {
        return ATTAQUES;
    }
}
