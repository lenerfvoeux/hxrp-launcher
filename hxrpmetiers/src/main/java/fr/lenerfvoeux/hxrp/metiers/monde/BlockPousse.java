package fr.lenerfvoeux.hxrp.metiers.monde;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import net.minecraft.block.BlockBush;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/** Pousse d'arbre fruitier : grandit à la lumière (ou à la poudre d'os) puis devient un arbre. */
public class BlockPousse extends BlockBush implements IGrowable {
    public static final PropertyInteger STAGE = PropertyInteger.create("stage", 0, 1);
    private static final AxisAlignedBB BOITE = new AxisAlignedBB(0.1, 0, 0.1, 0.9, 0.8, 0.9);

    public final Recolte.Arbre arbre;
    BlockFeuillage feuilles;

    public BlockPousse(Recolte.Arbre a) {
        this.arbre = a;
        setRegistryName(HxrpMetiers.MODID, "pousse_" + a.id);
        setTranslationKey(HxrpMetiers.MODID + ".pousse_" + a.id);
        setSoundType(SoundType.PLANT);
        setDefaultState(blockState.getBaseState().withProperty(STAGE, 0));
    }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, STAGE); }
    @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(STAGE, meta & 1); }
    @Override public int getMetaFromState(IBlockState s) { return s.getValue(STAGE); }
    @Override public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos pos) { return BOITE; }

    @Override
    public void updateTick(World w, BlockPos pos, IBlockState s, Random r) {
        if (w.isRemote) return;
        super.updateTick(w, pos, s, r);
        if (w.getBlockState(pos).getBlock() == this && w.getLightFromNeighbors(pos.up()) >= 9 && r.nextInt(7) == 0) grow(w, r, pos, s);
    }

    @Override public boolean canGrow(World w, BlockPos pos, IBlockState s, boolean client) { return true; }
    @Override public boolean canUseBonemeal(World w, Random r, BlockPos pos, IBlockState s) { return r.nextFloat() < 0.45f; }

    @Override
    public void grow(World w, Random r, BlockPos pos, IBlockState s) {
        if (s.getValue(STAGE) == 0) w.setBlockState(pos, s.withProperty(STAGE, 1), 4);
        else ArbreFruitier.pousser(w, pos, this, r);
    }
}
