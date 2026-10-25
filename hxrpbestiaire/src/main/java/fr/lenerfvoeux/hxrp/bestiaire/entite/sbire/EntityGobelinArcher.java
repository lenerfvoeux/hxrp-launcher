package fr.lenerfvoeux.hxrp.bestiaire.entite.sbire;

import fr.lenerfvoeux.hxrp.bestiaire.Especes;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntityProjectileBoss;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntitySbire;
import net.minecraft.world.World;

/** Gobelin archer, de l'escorte du Roi : il garde ses distances et tire des flèches de glace qui ralentissent. */
public class EntityGobelinArcher extends EntitySbire {
    private static final String[] ATTAQUES = {"tir"};

    public EntityGobelinArcher(World w) {
        super(w, Especes.GOBELIN_ARCHER, Comportement.ARCHER);
    }

    @Override
    protected String[] attaques() {
        return ATTAQUES;
    }

    @Override
    protected EntityProjectileBoss.Type typeDeTir() {
        return EntityProjectileBoss.Type.FLECHE_GLACE;
    }
}
