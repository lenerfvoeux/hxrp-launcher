package fr.lenerfvoeux.hxrp.metiers.virus.monde;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.virus.item.Objets;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/** Nid d'aigle-araignée accroché au bord des falaises : jusqu'à deux œufs, qui reviennent lentement. */
public class BlockNid extends Block {
    public static final PropertyInteger OEUFS = PropertyInteger.create("oeufs", 0, 2);
    private static final AxisAlignedBB BOITE = new AxisAlignedBB(0.06, 0, 0.06, 0.94, 0.38, 0.94);

    public BlockNid() {
        super(Material.PLANTS);
        setRegistryName(HxrpMetiers.MODID, "nid_d_aigle_araignee");
        setTranslationKey(HxrpMetiers.MODID + ".nid_d_aigle_araignee");
        setDefaultState(blockState.getBaseState().withProperty(OEUFS, 0));
        setTickRandomly(true);
        setHardness(0.6f);
        setSoundType(SoundType.PLANT);
    }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, OEUFS); }
    @Override public IBlockState getStateFromMeta(int m) { return getDefaultState().withProperty(OEUFS, Math.min(2, m)); }
    @Override public int getMetaFromState(IBlockState s) { return s.getValue(OEUFS); }
    @Override public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos pos) { return BOITE; }
    @Override public boolean isOpaqueCube(IBlockState s) { return false; }
    @Override public boolean isFullCube(IBlockState s) { return false; }
    @Override public BlockFaceShape getBlockFaceShape(IBlockAccess w, IBlockState s, BlockPos pos, EnumFacing f) { return BlockFaceShape.UNDEFINED; }

    @SideOnly(Side.CLIENT)
    @Override public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }

    @Override
    public boolean canPlaceBlockAt(World w, BlockPos pos) {
        return super.canPlaceBlockAt(w, pos) && w.getBlockState(pos.down()).isSideSolid(w, pos.down(), EnumFacing.UP);
    }

    @Override
    public void neighborChanged(IBlockState s, World w, BlockPos pos, Block b, BlockPos from) {
        if (!w.getBlockState(pos.down()).isSideSolid(w, pos.down(), EnumFacing.UP)) {
            dropBlockAsItem(w, pos, s, 0);
            w.setBlockToAir(pos);
        }
    }

    /** Un œuf revient en une heure environ (en moyenne) tant que le nid n'est pas plein. */
    @Override
    public void updateTick(World w, BlockPos pos, IBlockState s, Random r) {
        if (s.getValue(OEUFS) < 2 && r.nextInt(55) == 0) w.setBlockState(pos, s.withProperty(OEUFS, s.getValue(OEUFS) + 1), 2);
    }

    @Override
    public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float hx, float hy, float hz) {
        if (hand != EnumHand.MAIN_HAND) return false;
        if (w.isRemote) return true;
        int n = s.getValue(OEUFS);
        if (n <= 0) {
            p.sendStatusMessage(new TextComponentString(TextFormatting.GRAY + "" + TextFormatting.ITALIC + "Le nid est vide. L'aigle-araignée y pondra de nouveau."), true);
            return true;
        }
        Item o = Objets.item("oeuf_d_aigle_araignee");
        if (o != null) spawnAsEntity(w, pos, new ItemStack(o));
        w.setBlockState(pos, s.withProperty(OEUFS, n - 1), 2);
        w.playSound(null, pos, SoundEvents.BLOCK_GRASS_HIT, SoundCategory.BLOCKS, 0.8f, 1.3f);
        return true;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess w, BlockPos pos, IBlockState s, int fortune) {
        Item o = Objets.item("oeuf_d_aigle_araignee");
        if (o != null && s.getValue(OEUFS) > 0) drops.add(new ItemStack(o, s.getValue(OEUFS)));
        drops.add(new ItemStack(Items.STICK, 2));
    }
}
