package fr.lenerfvoeux.hxrp.metiers.entite;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIFollowParent;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMate;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAITempt;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

/** Sanglier : paisible tant qu'on ne l'attaque pas, il charge alors celui qui l'a blessé. */
public class EntitySanglier extends EntityGourmetAnimal {
    public EntitySanglier(World w) {
        super(w, "sanglier", 0.9f, 0.9f, 16, 0.28, 1, 3);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(4.0);
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(1, new EntityAIAttackMelee(this, 1.4, true));
        tasks.addTask(2, new EntityAIMate(this, 1.0));
        tasks.addTask(3, new EntityAITempt(this, 1.1, Items.CARROT, false));
        tasks.addTask(4, new EntityAIFollowParent(this, 1.1));
        tasks.addTask(5, new EntityAIWanderAvoidWater(this, 0.9));
        tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0f));
        tasks.addTask(7, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
    }

    @Override
    public boolean attackEntityAsMob(Entity cible) {
        boolean ok = cible.attackEntityFrom(DamageSource.causeMobDamage(this), (float) getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue());
        if (ok) {
            cible.motionX += -Math.sin(Math.toRadians(rotationYaw)) * 0.5;
            cible.motionZ += Math.cos(Math.toRadians(rotationYaw)) * 0.5;
            cible.motionY += 0.2;
            applyEnchantments(this, cible);
        }
        return ok;
    }

    @Override public boolean isBreedingItem(ItemStack s) { return s.getItem() == Items.CARROT; }
    @Override public EntityAgeable createChild(EntityAgeable o) { return new EntitySanglier(world); }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_PIG_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ENTITY_PIG_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_PIG_DEATH; }
    @Override protected float getSoundPitch() { return 0.7f; }

    @Override
    protected void extras(int butin) {
        if (rand.nextInt(2) == 0) dropItem(Items.LEATHER, 1);
    }
}
