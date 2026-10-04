package fr.lenerfvoeux.hxrp.metiers.virus.block;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.block.BlockOriente;
import fr.lenerfvoeux.hxrp.metiers.block.GuiHandler;
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

/**
 * Meuble à tiroirs de l'apothicaire : 54 cases de rangement, et un tiroir de seringues stériles
 * qu'on ne vide jamais (pas besoin de les fabriquer).
 */
public class BlockMeuble extends BlockOriente {
    public BlockMeuble() {
        super(Material.WOOD, new double[]{0, 0, 1, 16, 16, 16});
        setRegistryName(HxrpMetiers.MODID, "meuble_a_tiroirs");
        setTranslationKey(HxrpMetiers.MODID + ".meuble_a_tiroirs");
        setHardness(2.5f);
        setResistance(10f);
        setSoundType(SoundType.WOOD);
    }

    @Override public boolean hasTileEntity(IBlockState s) { return true; }
    @Override public TileEntity createTileEntity(World w, IBlockState s) { return new TileMeuble(); }

    @Override
    public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float x, float y, float z) {
        if (!w.isRemote) p.openGui(HxrpMetiers.instance, GuiHandler.MEUBLE, w, pos.getX(), pos.getY(), pos.getZ());
        return true;
    }

    @Override
    public void breakBlock(World w, BlockPos pos, IBlockState s) {
        TileEntity te = w.getTileEntity(pos);
        if (te instanceof TileMeuble) {
            TileMeuble m = (TileMeuble) te;
            for (int i = 0; i < m.inv.getSlots(); i++) {
                ItemStack st = m.inv.getStackInSlot(i);
                if (!st.isEmpty()) InventoryHelper.spawnItemStack(w, pos.getX(), pos.getY(), pos.getZ(), st.copy());
            }
        }
        super.breakBlock(w, pos, s);
    }
}
