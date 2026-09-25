package fr.lenerfvoeux.hxrp.metiers.entite;

import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAIFollowParent;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMate;
import net.minecraft.entity.ai.EntityAIPanic;
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

/** Cerf des forêts : farouche, il s'enfuit dès qu'un joueur s'approche sans se cacher. */
public class EntityCerf extends EntityGourmetAnimal {
    public EntityCerf(World w) {
        super(w, "cerf", 0.9f, 1.6f, 14, 0.3, 1, 3);
        stepHeight = 1.0f;
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(1, new EntityAIPanic(this, 2.0));
        tasks.addTask(2, new EntityAIAvoidEntity<>(this, EntityPlayer.class, p -> p != null && !p.isSneaking() && !((EntityPlayer) p).isSpectator(), 10.0f, 1.3, 1.8));
        tasks.addTask(3, new EntityAIMate(this, 1.0));
        tasks.addTask(4, new EntityAITempt(this, 1.0, Items.APPLE, false));
        tasks.addTask(5, new EntityAIFollowParent(this, 1.1));
        tasks.addTask(6, new EntityAIWanderAvoidWater(this, 0.9));
        tasks.addTask(7, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0f));
        tasks.addTask(8, new EntityAILookIdle(this));
    }

    @Override public boolean isBreedingItem(ItemStack s) { return s.getItem() == Items.APPLE; }
    @Override public EntityAgeable createChild(EntityAgeable o) { return new EntityCerf(world); }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ENTITY_HORSE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_HORSE_DEATH; }
    @Override protected float getSoundPitch() { return 1.4f; }

    @Override
    protected void extras(int butin) {
        int n = rand.nextInt(2 + butin);
        if (n > 0) dropItem(Items.LEATHER, n);
    }
}
