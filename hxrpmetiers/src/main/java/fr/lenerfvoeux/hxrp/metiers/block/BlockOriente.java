package fr.lenerfvoeux.hxrp.metiers.block;

import fr.lenerfvoeux.hxrp.metiers.cuisine.Station;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Bloc à modèle 3D détaillé, tourné vers le joueur à la pose.
 * Non opaque (on voit à travers les parties ajourées), boîte de sélection ajustée au modèle
 * et tournée avec lui.
 */
public abstract class BlockOriente extends Block {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    private final AxisAlignedBB[] boites = new AxisAlignedBB[4];

    protected BlockOriente(Material m, double[] boiteNord) {
        super(m);
        for (int h = 0; h < 4; h++) {
            double[] b = Station.tourner(boiteNord, h);
            boites[h] = new AxisAlignedBB(b[0], b[1], b[2], b[3], b[4], b[5]);
        }
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
        setLightOpacity(0);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
    }

    @Override
    public int getMetaFromState(IBlockState s) {
        return s.getValue(FACING).getHorizontalIndex();
    }

    @Override
    public IBlockState getStateForPlacement(World w, BlockPos pos, EnumFacing f, float x, float y, float z, int meta, EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public IBlockState withRotation(IBlockState s, Rotation rot) {
        return s.withProperty(FACING, rot.rotate(s.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState s, Mirror mirror) {
        return s.withRotation(mirror.toRotation(s.getValue(FACING)));
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos pos) {
        return boites[s.getValue(FACING).getHorizontalIndex()];
    }

    @Override
    public boolean isOpaqueCube(IBlockState s) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState s) {
        return false;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess w, IBlockState s, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
