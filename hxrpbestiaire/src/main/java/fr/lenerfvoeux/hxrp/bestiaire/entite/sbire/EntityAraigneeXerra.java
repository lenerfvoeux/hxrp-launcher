package fr.lenerfvoeux.hxrp.bestiaire.entite.sbire;

import fr.lenerfvoeux.hxrp.bestiaire.Especes;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntityBoss;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntitySbire;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * Araignée de Xerra : Xerra en petit (sans la femme qui la chevauche), mord au contact, ne se prend pas dans les
 * toiles. En phase 3, elle court rejoindre sa mère : en la touchant, elle la soigne (2 % de sa vie) et se fond en elle.
 */
public class EntityAraigneeXerra extends EntitySbire {
    private static final String[] ATTAQUES = {"swing_leg"};
    private static final float SOIN = 0.02F;

    public EntityAraigneeXerra(World w) {
        super(w, Especes.ARAIGNEE_XERRA, Comportement.CONTACT);
    }

    @Override
    protected String[] attaques() {
        return ATTAQUES;
    }

    @Override
    protected boolean comportementPropre() {
        EntityBoss b = boss();
        if (b == null || b.phase() < 3 || b.mode() != EntityBoss.NORMAL) return false;
        double dx = b.posX - posX, dz = b.posZ - posZ;
        if (Math.sqrt(dx * dx + dz * dz) < b.width / 2 + width / 2 + 0.8 && Math.abs(b.posY - posY) < b.height) {
            b.heal(b.getMaxHealth() * SOIN);
            if (world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.HEART, posX, posY + 0.5, posZ, 4, 0.3, 0.3, 0.3, 0);
                ((WorldServer) world).spawnParticle(EnumParticleTypes.SPELL_WITCH, posX, posY + 0.3, posZ, 15, 0.4, 0.3, 0.4, 0.05);
            }
            world.playSound(null, posX, posY, posZ, SoundEvents.ENTITY_SPIDER_DEATH, SoundCategory.HOSTILE, 1F, 1.6F);
            setDead();
            return true;
        }
        if (ticksExisted % 10 == 0 || getNavigator().noPath()) getNavigator().tryMoveToEntityLiving(b, 1.3);
        return true;
    }

    /** Les toiles ne la retiennent pas. */
    @Override
    public void setInWeb() {
    }
}
