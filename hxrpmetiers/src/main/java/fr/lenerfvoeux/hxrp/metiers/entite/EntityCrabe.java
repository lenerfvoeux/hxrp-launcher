package fr.lenerfvoeux.hxrp.metiers.entite;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMate;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAITempt;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/** Crabe des plages : marche de côté (son modèle est tourné), fuit quand on le frappe, aime le poisson. */
public class EntityCrabe extends EntityGourmetAnimal {
    public EntityCrabe(World w) {
        super(w, "crabe", 0.7f, 0.4f, 6, 0.22, 1, 1);
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(1, new EntityAIPanic(this, 1.6));
        tasks.addTask(2, new EntityAIMate(this, 1.0));
        tasks.addTask(3, new EntityAITempt(this, 1.1, Items.FISH, false));
        tasks.addTask(5, new EntityAIWanderAvoidWater(this, 0.8));
        tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 5.0f));
        tasks.addTask(7, new EntityAILookIdle(this));
    }

    @Override public boolean isBreedingItem(ItemStack s) { return s.getItem() == Items.FISH; }
    @Override public EntityAgeable createChild(EntityAgeable o) { return new EntityCrabe(world); }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ENTITY_SILVERFISH_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_SILVERFISH_DEATH; }
    @Override protected void playStepSound(BlockPos pos, Block b) { playSound(SoundEvents.ENTITY_SILVERFISH_STEP, 0.1f, 1.4f); }

    /** Sur le sable ou le gravier, pas besoin d'herbe. */
    @Override
    public boolean getCanSpawnHere() {
        BlockPos p = new BlockPos(MathHelper.floor(posX), MathHelper.floor(getEntityBoundingBox().minY), MathHelper.floor(posZ));
        Block sol = world.getBlockState(p.down()).getBlock();
        return (sol == Blocks.SAND || sol == Blocks.GRAVEL) && world.getLight(p) > 7 && isNotColliding();
    }
}
