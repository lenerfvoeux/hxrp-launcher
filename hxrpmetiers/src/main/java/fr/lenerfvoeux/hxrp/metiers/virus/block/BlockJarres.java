package fr.lenerfvoeux.hxrp.metiers.virus.block;

import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Machine;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Les trois jarres de macération : chacune garde une préparation scellée le temps indiqué par la recette
 * (en heures réelles, même serveur éteint). Le nombre de jarres scellées se voit sur le modèle (couvercles et cordelettes).
 */
public class BlockJarres extends BlockMachine {
    public static final PropertyInteger SCELLEES = PropertyInteger.create("scellees", 0, 3);

    public BlockJarres() {
        super(Machine.JARRES);
        setDefaultState(getDefaultState().withProperty(SCELLEES, 0));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, SCELLEES);
    }

    @Override
    public IBlockState getActualState(IBlockState s, IBlockAccess w, BlockPos pos) {
        TileEntity te = w.getTileEntity(pos);
        return s.withProperty(SCELLEES, te instanceof TileJarres ? ((TileJarres) te).scellees() : 0);
    }

    @Override public boolean hasTileEntity(IBlockState s) { return true; }
    @Override public TileEntity createTileEntity(World w, IBlockState s) { return new TileJarres(); }

    /** Casser les jarres rend les préparations telles quelles : la macération est perdue, l'étape reste à refaire. */
    @Override
    public void breakBlock(World w, BlockPos pos, IBlockState s) {
        TileEntity te = w.getTileEntity(pos);
        if (te instanceof TileJarres) for (ItemStack st : ((TileJarres) te).viderPourCasse())
            InventoryHelper.spawnItemStack(w, pos.getX(), pos.getY(), pos.getZ(), st);
        super.breakBlock(w, pos, s);
    }
}
