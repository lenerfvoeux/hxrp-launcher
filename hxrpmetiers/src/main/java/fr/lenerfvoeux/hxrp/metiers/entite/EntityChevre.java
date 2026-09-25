package fr.lenerfvoeux.hxrp.metiers.entite;

import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import net.minecraft.entity.EntityAgeable;
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
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemHandlerHelper;

/** Chèvre des montagnes : grimpe d'un bloc sans sauter ; se trait avec une fiole vide. */
public class EntityChevre extends EntityGourmetAnimal {
    public EntityChevre(World w) {
        super(w, null, 0.8f, 1.2f, 10, 0.27, 0, 0);
        stepHeight = 1.0f;
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(1, new EntityAIPanic(this, 1.6));
        tasks.addTask(2, new EntityAIMate(this, 1.0));
        tasks.addTask(3, new EntityAITempt(this, 1.1, Items.WHEAT, false));
        tasks.addTask(4, new EntityAIFollowParent(this, 1.1));
        tasks.addTask(5, new EntityAIWanderAvoidWater(this, 1.0));
        tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0f));
        tasks.addTask(7, new EntityAILookIdle(this));
    }

    @Override
    public boolean processInteract(EntityPlayer p, EnumHand hand) {
        ItemStack tenu = p.getHeldItem(hand);
        if (tenu.getItem() == Items.GLASS_BOTTLE && !isChild()) {
            if (!world.isRemote) {
                if (!p.capabilities.isCreativeMode) tenu.shrink(1);
                ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(ModRegistry.FOOD.get("lait_de_chevre")));
                playSound(SoundEvents.ENTITY_COW_MILK, 1.0f, 1.3f);
            }
            return true;
        }
        return super.processInteract(p, hand);
    }

    @Override public boolean isBreedingItem(ItemStack s) { return s.getItem() == Items.WHEAT; }
    @Override public EntityAgeable createChild(EntityAgeable o) { return new EntityChevre(world); }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_SHEEP_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ENTITY_SHEEP_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_SHEEP_DEATH; }
    @Override protected float getSoundPitch() { return 1.3f + (rand.nextFloat() - rand.nextFloat()) * 0.1f; }

    @Override
    protected void dropFewItems(boolean touche, int butin) {
        if (!isChild() && rand.nextInt(2) == 0) dropItem(Items.LEATHER, 1);
    }
}
