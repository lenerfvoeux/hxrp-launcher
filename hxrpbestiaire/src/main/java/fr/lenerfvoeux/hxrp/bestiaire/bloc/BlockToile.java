package fr.lenerfvoeux.hxrp.bestiaire.bloc;

import fr.lenerfvoeux.hxrp.bestiaire.entite.Bestiole;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Toile de Xerra : posée dans l'air par ses jets de toile et sa grande couvée. Elle englue les joueurs comme une toile
 * d'araignée (pas les créatures du bestiaire), brûle très bien (le feu libère le terrain), ne lâche rien, et se défait
 * d'elle-même en une minute environ.
 */
public class BlockToile extends Block {
    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 3);
    /** Ticks entre deux âges : la toile tient 4 × 20 s. */
    private static final int ETAPE = 400;

    public BlockToile() {
        super(Material.WEB);
        setHardness(0.8F);
        setSoundType(SoundType.CLOTH);
        setDefaultState(blockState.getBaseState().withProperty(AGE, 0));
    }

    @Override
    public void onBlockAdded(World w, BlockPos pos, IBlockState s) {
        if (!w.isRemote) w.scheduleUpdate(pos, this, ETAPE + w.rand.nextInt(100));
    }

    @Override
    public void updateTick(World w, BlockPos pos, IBlockState s, Random r) {
        if (w.isRemote) return;
        int age = s.getValue(AGE);
        if (age >= 3) {
            w.setBlockToAir(pos);
        } else {
            w.setBlockState(pos, s.withProperty(AGE, age + 1), 2);
            w.scheduleUpdate(pos, this, ETAPE + r.nextInt(100));
        }
    }

    @Override
    public void onEntityCollision(World w, BlockPos pos, IBlockState s, Entity e) {
        if (!(e instanceof Bestiole)) e.setInWeb();
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState s, IBlockAccess w, BlockPos pos) {
        return NULL_AABB;
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

    @Override
    public boolean isReplaceable(IBlockAccess w, BlockPos pos) {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess w, BlockPos pos, IBlockState s, int fortune) {
    }

    @Override
    public ItemStack getPickBlock(IBlockState s, net.minecraft.util.math.RayTraceResult cible, World w, BlockPos pos,
                                  net.minecraft.entity.player.EntityPlayer joueur) {
        return ItemStack.EMPTY;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AGE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(AGE, meta & 3);
    }

    @Override
    public int getMetaFromState(IBlockState s) {
        return s.getValue(AGE);
    }
}
