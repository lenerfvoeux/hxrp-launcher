package fr.lenerfvoeux.hxrp.metiers.monde;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Une culture en trois stades visibles : 0 vient d'être plantée, 1 pousse, 2 prête à récolter.
 * Se sème sur de la terre labourée avec ses graines ; clic droit sur une plante mûre pour la récolter
 * sans l'arracher (elle repart du stade 0), ou casser pour récupérer aussi des graines.
 */
public class BlockCulture extends BlockCrops {
    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 2);
    private static final AxisAlignedBB[] BOITES = {
            new AxisAlignedBB(0, 0, 0, 1, 0.25, 1), new AxisAlignedBB(0, 0, 0, 1, 0.6, 1), new AxisAlignedBB(0, 0, 0, 1, 0.9, 1)};

    public final Recolte.Culture def;
    Item graine;

    public BlockCulture(Recolte.Culture c) {
        this.def = c;
        setRegistryName(HxrpMetiers.MODID, "culture_" + c.id);
        setTranslationKey(HxrpMetiers.MODID + ".culture_" + c.id);
    }

    public Item graine() { return graine; }

    @Override protected PropertyInteger getAgeProperty() { return AGE; }
    @Override public int getMaxAge() { return 2; }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, AGE); }
    @Override protected int getBonemealAgeIncrease(World w) { return 1; }
    @Override protected Item getSeed() { return graine; }
    @Override protected Item getCrop() { return ModRegistry.FOOD.get(def.id); }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos pos) {
        return BOITES[Math.min(2, getAge(s))];
    }

    /** Trois stades seulement : on ralentit la pousse pour garder la durée d'une culture vanilla. */
    @Override
    public void updateTick(World w, BlockPos pos, IBlockState s, Random r) {
        if (r.nextInt(3) == 0) super.updateTick(w, pos, s, r);
        else checkAndDropBlock(w, pos, s);
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess w, BlockPos pos, IBlockState s, int fortune) {
        Random r = w instanceof World ? ((World) w).rand : RANDOM;
        if (getAge(s) >= getMaxAge()) {
            Item crop = getCrop();
            if (crop != null) drops.add(new ItemStack(crop, quantite(r, fortune)));
            drops.add(new ItemStack(graine, 1 + (r.nextInt(3) == 0 ? 1 : 0)));
        } else {
            drops.add(new ItemStack(graine));
        }
    }

    private int quantite(Random r, int fortune) {
        return def.min + r.nextInt(Math.max(1, def.max - def.min + 1)) + (fortune > 0 ? r.nextInt(fortune + 1) : 0);
    }

    @Override
    public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float hx, float hy, float hz) {
        if (getAge(s) < getMaxAge()) return false;
        ItemStack tenu = p.getHeldItem(hand);
        if (tenu.getItem() == Items.DYE) return false; // laisse la poudre d'os se comporter normalement
        if (!w.isRemote) {
            Item crop = getCrop();
            if (crop != null) spawnAsEntity(w, pos, new ItemStack(crop, quantite(w.rand, 0)));
            w.setBlockState(pos, withAge(0), 2);
            w.playSound(null, pos, SoundEvents.BLOCK_GRASS_BREAK, SoundCategory.BLOCKS, 0.8f, 1.1f);
        }
        return true;
    }
}
