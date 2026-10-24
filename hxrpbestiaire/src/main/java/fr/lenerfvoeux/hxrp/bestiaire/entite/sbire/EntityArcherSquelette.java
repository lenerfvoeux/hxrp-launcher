package fr.lenerfvoeux.hxrp.bestiaire.entite.sbire;

import fr.lenerfvoeux.hxrp.bestiaire.Especes;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntitySbire;
import net.minecraft.world.World;

/** Archer squelette, levé par le Nécromancien : il garde ses distances et tire ; tant qu'il vit, son maître se soigne. */
public class EntityArcherSquelette extends EntitySbire {
    private static final String[] ATTAQUES = {"tir"};

    public EntityArcherSquelette(World w) {
        super(w, Especes.ARCHER_SQUELETTE, Comportement.ARCHER);
    }

    @Override
    protected String[] attaques() {
        return ATTAQUES;
    }
}
