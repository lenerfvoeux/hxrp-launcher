package fr.lenerfvoeux.hxrp.metiers.virus.block;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.block.BlockOriente;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * Présentoir laqué de l'apothicaire : deux étagères, quatre emplacements où exposer fioles, onguents et ingrédients.
 * Clic droit avec un objet : le poser (sur l'emplacement visé) ; main vide : le reprendre.
 */
public class BlockPresentoir extends BlockOriente {
    public BlockPresentoir() {
        super(Material.WOOD, new double[]{0, 0, 4, 16, 16, 16});
        setRegistryName(HxrpMetiers.MODID, "presentoir_de_l_apothicaire");
        setTranslationKey(HxrpMetiers.MODID + ".presentoir_de_l_apothicaire");
        setHardness(2f);
        setSoundType(SoundType.WOOD);
    }

    @Override public boolean hasTileEntity(IBlockState s) { return true; }
    @Override public TileEntity createTileEntity(World w, IBlockState s) { return new TilePresentoir(); }

    /** Emplacement visé : étagère du haut ou du bas, moitié gauche ou droite (vu de face). */
    private static int emplacement(IBlockState s, float x, float y, float z) {
        EnumFacing f = s.getValue(FACING);
        double u;
        switch (f) {
            case NORTH: u = 1 - x; break;
            case SOUTH: u = x; break;
            case WEST: u = z; break;
            default: u = 1 - z; break;
        }
        return (y >= 0.5f ? 0 : 2) + (u < 0.5 ? 0 : 1);
    }

    @Override
    public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float x, float y, float z) {
        if (hand != EnumHand.MAIN_HAND) return false;
        TileEntity te = w.getTileEntity(pos);
        if (!(te instanceof TilePresentoir)) return false;
        if (w.isRemote) return true;
        TilePresentoir t = (TilePresentoir) te;
        int k = emplacement(s, x, y, z);
        ItemStack tenu = p.getHeldItem(hand);
        if (!tenu.isEmpty() && t.objet(k).isEmpty()) {
            t.poser(k, tenu.splitStack(1));
            w.playSound(null, pos, SoundEvents.ENTITY_ITEMFRAME_ADD_ITEM, SoundCategory.BLOCKS, 0.8f, 1.1f);
        } else if (!t.objet(k).isEmpty()) {
            ItemHandlerHelper.giveItemToPlayer(p, t.prendre(k));
            w.playSound(null, pos, SoundEvents.ENTITY_ITEMFRAME_REMOVE_ITEM, SoundCategory.BLOCKS, 0.8f, 1.1f);
        }
        return true;
    }

    @Override
    public void breakBlock(World w, BlockPos pos, IBlockState s) {
        TileEntity te = w.getTileEntity(pos);
        if (te instanceof TilePresentoir) for (int k = 0; k < TilePresentoir.PLACES; k++) {
            ItemStack o = ((TilePresentoir) te).objet(k);
            if (!o.isEmpty()) InventoryHelper.spawnItemStack(w, pos.getX(), pos.getY(), pos.getZ(), o.copy());
        }
        super.breakBlock(w, pos, s);
    }
}
