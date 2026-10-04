package fr.lenerfvoeux.hxrp.metiers.entite;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.ai.EntityAIFollowParent;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMate;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAITempt;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import com.google.common.collect.Sets;
import java.util.Set;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Volaille (dinde, canard) : picore, se laisse planer en tombant, suit qui tient des graines. */
public abstract class EntityVolaille extends EntityGourmetAnimal {
    private static final Set<Item> GRAINES = Sets.newHashSet(Items.WHEAT_SEEDS, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS, Items.BEETROOT_SEEDS);
    private final float voix;
    private final boolean nageur;

    protected EntityVolaille(World w, String ingredient, float largeur, float hauteur, double sante, float voix, boolean nageur) {
        super(w, ingredient, largeur, hauteur, sante, 0.25, 1, 2);
        this.voix = voix;
        this.nageur = nageur;
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(1, new EntityAIPanic(this, 1.4));
        tasks.addTask(2, new EntityAIMate(this, 1.0));
        tasks.addTask(3, new EntityAITempt(this, 1.0, false, GRAINES));
        tasks.addTask(4, new EntityAIFollowParent(this, 1.1));
        tasks.addTask(5, nageurIA());
        tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0f));
        tasks.addTask(7, new EntityAILookIdle(this));
    }

    private net.minecraft.entity.ai.EntityAIBase nageurIA() {
        // le canard aime l'eau ; la dinde l'évite
        return getClass() == Canard.class ? new EntityAIWander(this, 1.0) : new EntityAIWanderAvoidWater(this, 1.0);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!onGround && motionY < 0) motionY *= 0.6; // plane en battant des ailes
    }

    @Override public void fall(float distance, float mult) {}
    @Override public boolean isBreedingItem(ItemStack s) { return GRAINES.contains(s.getItem()); }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_CHICKEN_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ENTITY_CHICKEN_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_CHICKEN_DEATH; }
    @Override protected void playStepSound(BlockPos pos, Block b) { playSound(SoundEvents.ENTITY_CHICKEN_STEP, 0.15f, 1.0f); }
    @Override protected float getSoundPitch() { return voix + (rand.nextFloat() - rand.nextFloat()) * 0.1f; }

    @Override
    protected void extras(int butin) {
        int n = rand.nextInt(2 + butin);
        if (n > 0) dropItem(Items.FEATHER, n);
    }

    public boolean nageur() { return nageur; }

    public static class Dinde extends EntityVolaille {
        public Dinde(World w) { super(w, "dinde", 0.7f, 1.0f, 8, 0.55f, false); }
        @Override public EntityAgeable createChild(EntityAgeable o) { return new Dinde(world); }
    }

    public static class Canard extends EntityVolaille {
        public Canard(World w) { super(w, "canard", 0.5f, 0.8f, 5, 1.35f, true); }
        @Override public EntityAgeable createChild(EntityAgeable o) { return new Canard(world); }
    }
}
