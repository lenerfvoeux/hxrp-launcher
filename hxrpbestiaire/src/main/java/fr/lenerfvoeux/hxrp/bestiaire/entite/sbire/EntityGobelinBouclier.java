package fr.lenerfvoeux.hxrp.bestiaire.entite.sbire;

import fr.lenerfvoeux.hxrp.bestiaire.Especes;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntityBoss;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntitySbire;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * Gobelin bouclier, de l'escorte du Roi : lent, il arrête presque tous les coups de face. Tant que sa cible n'est pas
 * sur lui, il se tient entre elle et le Roi : il faut le contourner.
 */
public class EntityGobelinBouclier extends EntitySbire {
    private static final String[] ATTAQUES = {"meleeattack"};

    public EntityGobelinBouclier(World w) {
        super(w, Especes.GOBELIN_BOUCLIER, Comportement.BOUCLIER);
    }

    @Override
    protected String[] attaques() {
        return ATTAQUES;
    }

    @Override
    protected boolean comportementPropre() {
        EntityBoss b = boss();
        if (b == null || cible == null || getDistance(cible) < 3.5) return false;
        double dx = cible.posX - b.posX, dz = cible.posZ - b.posZ, l = Math.sqrt(dx * dx + dz * dz);
        if (l < 1) return false;
        // poste de garde : à 3 blocs du bord du Roi, sur la ligne qui le relie à la cible (décalé selon le gobelin)
        double r = b.width / 2 + 3, ecart = ((getEntityId() % 5) - 2) * 1.2;
        double gx = b.posX + dx / l * r - dz / l * ecart, gz = b.posZ + dz / l * r + dx / l * ecart;
        if ((gx - posX) * (gx - posX) + (gz - posZ) * (gz - posZ) > 1.5) {
            if (ticksExisted % 10 == 0 || getNavigator().noPath()) getNavigator().tryMoveToXYZ(gx, b.posY, gz, 1.2);
        } else {
            getNavigator().clearPath();
        }
        float cap = (float) (MathHelper.atan2(cible.posZ - posZ, cible.posX - posX) * (180.0 / Math.PI)) - 90F;
        rotationYaw = renderYawOffset = rotationYawHead = cap;
        return true;
    }
}
