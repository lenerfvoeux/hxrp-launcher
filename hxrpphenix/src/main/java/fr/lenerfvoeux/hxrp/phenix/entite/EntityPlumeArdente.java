package fr.lenerfvoeux.hxrp.phenix.entite;

import fr.lenerfvoeux.hxrp.phenix.Feu;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Plume enflammée de la pluie du Phénix : tombe en tournoyant et s'embrase en touchant le sol (sans rien allumer). */
public class EntityPlumeArdente extends Entity {
    private EntityPhenix phenix;
    public float tour, tourAvant;

    public EntityPlumeArdente(World w) {
        super(w);
        setSize(0.4F, 0.4F);
        isImmuneToFire = true;
    }

    public EntityPlumeArdente(World w, EntityPhenix phenix) {
        this(w);
        this.phenix = phenix;
    }

    @Override
    protected void entityInit() {
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        tourAvant = tour;
        tour += 23F;
        motionY = Math.max(-0.75, motionY - 0.015);
        motionX *= 0.9;
        motionZ *= 0.9;
        move(MoverType.SELF, motionX, motionY, motionZ);
        if (world.isRemote) {
            world.spawnParticle(EnumParticleTypes.FLAME, posX + (rand.nextDouble() - 0.5) * 0.3, posY + 0.2, posZ + (rand.nextDouble() - 0.5) * 0.3, 0, 0.02, 0);
            if (rand.nextInt(3) == 0) world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, posX, posY + 0.3, posZ, 0, 0.03, 0);
        } else if (onGround || collidedVertically || collidedHorizontally || isInWater() || isInLava() || ticksExisted > 120) {
            Feu.explosion(world, phenix, this, posX, posY + 0.2, posZ, 1.7, 4F, false);
            setDead();
        }
    }

    @Override
    public boolean writeToNBTOptional(NBTTagCompound c) {
        return false;   // éphémère : jamais sauvegardée avec le tronçon
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound c) {
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound c) {
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getBrightnessForRender() {
        return 0xF000F0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean isInRangeToRenderDist(double d) {
        return d < 96 * 96;
    }

    @Override
    public void applyEntityCollision(Entity e) {
    }
}
