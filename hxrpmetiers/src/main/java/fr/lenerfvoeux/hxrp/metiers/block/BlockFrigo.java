package fr.lenerfvoeux.hxrp.metiers.block;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import fr.lenerfvoeux.hxrp.metiers.data.Fraicheur;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockFrigo extends Block {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;

    public BlockFrigo() {
        super(Material.IRON);
        setRegistryName(HxrpMetiers.MODID, "frigo");
        setTranslationKey(HxrpMetiers.MODID + ".frigo");
        setHardness(2.5f);
        setSoundType(SoundType.METAL);
        setCreativeTab(ModRegistry.TAB_INGREDIENTS);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FACING); }
    @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3)); }
    @Override public int getMetaFromState(IBlockState s) { return s.getValue(FACING).getHorizontalIndex(); }

    @Override
    public IBlockState getStateForPlacement(World w, BlockPos pos, EnumFacing f, float x, float y, float z, int meta, EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override public boolean hasTileEntity(IBlockState s) { return true; }
    @Override public TileEntity createTileEntity(World w, IBlockState s) { return new TileFrigo(); }

    @Override
    public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float x, float y, float z) {
        if (!w.isRemote) p.openGui(HxrpMetiers.instance, GuiHandler.FRIGO, w, pos.getX(), pos.getY(), pos.getZ());
        return true;
    }

    @Override
    public void breakBlock(World w, BlockPos pos, IBlockState s) {
        TileEntity te = w.getTileEntity(pos);
        if (te instanceof TileFrigo) {
            long now = System.currentTimeMillis();
            TileFrigo f = (TileFrigo) te;
            for (int i = 0; i < f.inv.getSlots(); i++) {
                ItemStack st = f.inv.getStackInSlot(i).copy();
                if (!st.isEmpty()) InventoryHelper.spawnItemStack(w, pos.getX(), pos.getY(), pos.getZ(), Fraicheur.fromFridge(st, now));
            }
        }
        super.breakBlock(w, pos, s);
    }
}
