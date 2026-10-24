package fr.lenerfvoeux.hxrp.metiers.virus.monde;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.monde.Recharge;
import fr.lenerfvoeux.hxrp.metiers.monde.TileRecharge;
import fr.lenerfvoeux.hxrp.metiers.virus.VirusConfig;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Tronc de bouleau ou de saule (chêne des marais) dont on vient de prélever l'écorce : on voit le bois nu.
 * L'écorce repousse en temps réel (voir Recharge) : le tronc redevient alors un tronc ordinaire. Incassable en survie.
 */
public class BlockTroncEcorce extends BlockLog implements Recharge.Bloc {
    public final boolean bouleau;

    public BlockTroncEcorce(boolean bouleau) {
        this.bouleau = bouleau;
        String id = bouleau ? "tronc_ecorce_bouleau" : "tronc_ecorce_saule";
        setRegistryName(HxrpMetiers.MODID, id);
        setTranslationKey(HxrpMetiers.MODID + "." + id);
        setDefaultState(blockState.getBaseState().withProperty(LOG_AXIS, EnumAxis.Y));
        setTickRandomly(true);
        setBlockUnbreakable();
        setResistance(6000000f);
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

    // ------------------------------------------------------------------ recharge : ce bloc est l'état vide du tronc
    @Override public boolean plein(IBlockState s) { return false; }
    @Override public IBlockState etat(IBlockState s, double a) { return a >= 1 ? vanilla(s) : s; }
    @Override public double avancement(IBlockState s) { return 0; }
    @Override public long duree(IBlockState s) { return VirusConfig.rechargeEcorceMinutes * Recharge.MINUTE; }

    @Override public boolean hasTileEntity(IBlockState s) { return true; }
    @Override public TileEntity createTileEntity(World w, IBlockState s) { return new TileRecharge(); }

    @Override
    public void updateTick(World w, BlockPos pos, IBlockState s, Random r) {
        Recharge.rattraper(w, pos, s, this);
    }

    @Override
    public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float hx, float hy, float hz) {
        if (hand != EnumHand.MAIN_HAND || !p.getHeldItem(hand).isEmpty()) return false;
        if (!w.isRemote && Recharge.rattraper(w, pos, s, this).getBlock() == this)
            Recharge.dire(p, "L'écorce repousse" + Recharge.reste(w, pos) + ".");
        return true;
    }

    @Override public void getDrops(NonNullList<ItemStack> drops, IBlockAccess w, BlockPos pos, IBlockState s, int fortune) {}

    @Override
    public ItemStack getPickBlock(IBlockState s, RayTraceResult t, World w, BlockPos pos, EntityPlayer p) {
        return new ItemStack(Blocks.LOG, 1, bouleau ? BlockPlanks.EnumType.BIRCH.getMetadata() : BlockPlanks.EnumType.OAK.getMetadata());
    }
}
