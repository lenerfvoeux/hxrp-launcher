package fr.lenerfvoeux.hxrp.metiers.block;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import fr.lenerfvoeux.hxrp.metiers.data.Fraicheur;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Frigo rétro : 27 emplacements, la nourriture y pourrit 3 fois moins vite. */
public class BlockFrigo extends BlockOriente {
    public BlockFrigo() {
        super(Material.IRON, new double[]{0, 0, 0, 16, 16, 16});
        setRegistryName(HxrpMetiers.MODID, "frigo");
        setTranslationKey(HxrpMetiers.MODID + ".frigo");
        setHardness(2.5f);
        setSoundType(SoundType.METAL);
        setCreativeTab(ModRegistry.TAB_INGREDIENTS);
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
