package fr.lenerfvoeux.hxrp.bestiaire.entite.sbire;

import fr.lenerfvoeux.hxrp.bestiaire.Especes;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntitySbire;
import net.minecraft.world.World;

/** Guêpe soldat : la Reine-guêpe en petit. Rapide, elle vole autour des joueurs et pique (poison, classeur). */
public class EntityGuepeSoldat extends EntitySbire {
    private static final String[] ATTAQUES = {"right_cute", "left_cute", "below_cute"};

    public EntityGuepeSoldat(World w) {
        super(w, Especes.GUEPE_SOLDAT, Comportement.VOL);
    }

    @Override
    protected String[] attaques() {
        return ATTAQUES;
    }
}
