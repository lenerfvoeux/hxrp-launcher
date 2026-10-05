package fr.lenerfvoeux.hxrp.phenix.entite;

import fr.lenerfvoeux.hxrp.phenix.Feu;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Boule de feu du Phénix : explose en flammes à l'impact sans casser ni allumer quoi que ce soit.
 * On peut la renvoyer d'un coup d'épée, comme une boule de Ghast : si elle revient sur le Phénix, elle le blesse.
 */
public class EntityBouleDeFeu extends EntityFireball implements IEntityAdditionalSpawnData {
    private EntityPhenix phenix;

    public EntityBouleDeFeu(World w) {
        super(w);
        setSize(1.0F, 1.0F);
    }

    public EntityBouleDeFeu(World w, EntityPhenix tireur, double dx, double dy, double dz) {
        super(w, tireur, dx, dy, dz);
        setSize(1.0F, 1.0F);
        phenix = tireur;
        // un peu plus lente qu'une boule de Ghast : on a le temps de l'esquiver
        accelerationX *= 0.65;
        accelerationY *= 0.65;
        accelerationZ *= 0.65;
        motionX = accelerationX * 4;
        motionY = accelerationY * 4;
        motionZ = accelerationZ * 4;
    }

    @Override
    protected void onImpact(RayTraceResult r) {
        if (world.isRemote) return;
        Entity touche = r.entityHit;
        if (touche != null && touche == phenix && !(shootingEntity instanceof EntityPlayer)) return;
        if (touche instanceof EntityPhenix && shootingEntity instanceof EntityPlayer) {
            touche.attackEntityFrom(new EntityDamageSourceIndirect("phenix_renvoi", this, shootingEntity).setProjectile(), 20F);
            Feu.explosion(world, null, this, posX, posY + 0.5, posZ, 0.1, 0F, true);
        } else {
            Feu.explosion(world, phenix, this, posX, posY + 0.5, posZ, 3.2, 6F, true);
        }
        setDead();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (world.isRemote) {
            if (rand.nextInt(2) == 0) world.spawnParticle(EnumParticleTypes.FLAME, posX + (rand.nextDouble() - 0.5) * 0.6, posY + 0.5 + (rand.nextDouble() - 0.5) * 0.6, posZ + (rand.nextDouble() - 0.5) * 0.6, 0, 0, 0);
            if (rand.nextInt(5) == 0) world.spawnParticle(EnumParticleTypes.LAVA, posX, posY + 0.5, posZ, 0, 0, 0);
        } else if (ticksExisted > 160) {
            setDead();
        }
    }

    @Override
    protected boolean isFireballFiery() {
        return false;
    }

    @Override
    protected EnumParticleTypes getParticleType() {
        return EnumParticleTypes.SMOKE_LARGE;
    }

    /** « En feu » côté client : éclairage dynamique d'OptiFine ; sans la surcouche vanilla. */
    @Override
    public boolean isBurning() {
        return world.isRemote;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean canRenderOnFire() {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getBrightnessForRender() {
        return 0xF000F0;
    }

    @Override
    public void writeSpawnData(ByteBuf b) {
        b.writeDouble(accelerationX);
        b.writeDouble(accelerationY);
        b.writeDouble(accelerationZ);
    }

    @Override
    public void readSpawnData(ByteBuf b) {
        accelerationX = b.readDouble();
        accelerationY = b.readDouble();
        accelerationZ = b.readDouble();
    }

    public EntityLivingBase tireur() {
        return shootingEntity;
    }
}
