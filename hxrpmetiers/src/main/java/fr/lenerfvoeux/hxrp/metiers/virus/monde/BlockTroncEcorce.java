package fr.lenerfvoeux.hxrp.metiers.virus.monde;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Tronc de bouleau ou de saule (chêne des marais) dont on vient de prélever l'écorce. L'écorce repousse :
 * au bout d'un moment le tronc redevient un tronc ordinaire, et on peut y revenir.
 */
public class BlockTroncEcorce extends BlockLog {
    public final boolean bouleau;

    public BlockTroncEcorce(boolean bouleau) {
        this.bouleau = bouleau;
        String id = bouleau ? "tronc_ecorce_bouleau" : "tronc_ecorce_saule";
        setRegistryName(HxrpMetiers.MODID, id);
        setTranslationKey(HxrpMetiers.MODID + "." + id);
        setDefaultState(blockState.getBaseState().withProperty(LOG_AXIS, EnumAxis.Y));
        setTickRandomly(true);
    }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, LOG_AXIS); }

    @Override
    public IBlockState getStateFromMeta(int m) {
        EnumAxis a;
        switch (m & 3) {
            case 1: a = EnumAxis.X; break;
            case 2: a = EnumAxis.Z; break;
            case 3: a = EnumAxis.NONE; break;
            default: a = EnumAxis.Y; break;
        }
        return getDefaultState().withProperty(LOG_AXIS, a);
    }

    @Override
    public int getMetaFromState(IBlockState s) {
        switch (s.getValue(LOG_AXIS)) {
            case X: return 1;
            case Z: return 2;
            case NONE: return 3;
            default: return 0;
        }
    }

    /** Le tronc ordinaire, même orientation. */
    public IBlockState vanilla(IBlockState s) {
        BlockPlanks.EnumType t = bouleau ? BlockPlanks.EnumType.BIRCH : BlockPlanks.EnumType.OAK;
        return Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, t).withProperty(LOG_AXIS, s.getValue(LOG_AXIS));
    }

    @Override
    public void updateTick(World w, BlockPos pos, IBlockState s, Random r) {
        if (!w.isRemote && r.nextInt(40) == 0) w.setBlockState(pos, vanilla(s), 2);
    }

    @Override
    public Item getItemDropped(IBlockState s, Random r, int fortune) {
        return Item.getItemFromBlock(Blocks.LOG);
    }

    @Override
    public int damageDropped(IBlockState s) {
        return bouleau ? BlockPlanks.EnumType.BIRCH.getMetadata() : BlockPlanks.EnumType.OAK.getMetadata();
    }

    @Override
    public ItemStack getPickBlock(IBlockState s, RayTraceResult t, World w, BlockPos pos, EntityPlayer p) {
        return new ItemStack(Blocks.LOG, 1, damageDropped(s));
    }
}
