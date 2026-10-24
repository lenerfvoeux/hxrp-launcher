package fr.lenerfvoeux.hxrp.phenix.bloc;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * Nid de braises, l'appât du Phénix : un nid de branches rougeoyantes posé par un admin. Il fume de plus en plus,
 * un cri annonce le Phénix, qui arrive du ciel 30 à 90 s plus tard (classeur, onglet Appâts) et le dévore.
 * Incassable en survie (on ne vole pas l'appât) et sans butin ; un admin en créatif peut le retirer.
 */
public class BlockNidDeBraises extends Block {
    private static final AxisAlignedBB FORME = new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.375, 1.0);

    public BlockNidDeBraises() {
        super(Material.WOOD, MapColor.NETHERRACK);
        setSoundType(SoundType.WOOD);
        setBlockUnbreakable();
        setResistance(6000000F);
        setLightLevel(0.65F);
    }

    @Override
    public boolean hasTileEntity(IBlockState s) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World w, IBlockState s) {
        return new TileNidDeBraises();
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos pos) {
        return FORME;
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
        return face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean canPlaceBlockAt(World w, BlockPos pos) {
        return super.canPlaceBlockAt(w, pos) && w.getBlockState(pos.down()).isSideSolid(w, pos.down(), EnumFacing.UP);
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess w, BlockPos pos, IBlockState s, int fortune) {
    }

    @Override
    public ItemStack getPickBlock(IBlockState s, RayTraceResult cible, World w, BlockPos pos, EntityPlayer p) {
        return new ItemStack(Item.getItemFromBlock(this));
    }

    @Override
    public int getFlammability(IBlockAccess w, BlockPos pos, EnumFacing face) {
        return 0;
    }

    /** Côté client : braises qui crépitent et petite fumée (les gros signes viennent du serveur, voir TileNidDeBraises). */
    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState s, World w, BlockPos pos, Random r) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.25, z = pos.getZ() + 0.5;
        for (int i = 0; i < 2; i++)
            w.spawnParticle(EnumParticleTypes.FLAME, x + (r.nextDouble() - 0.5) * 0.6, y, z + (r.nextDouble() - 0.5) * 0.6, 0, 0.01, 0);
        w.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x + (r.nextDouble() - 0.5) * 0.5, y + 0.2, z + (r.nextDouble() - 0.5) * 0.5, 0, 0.03, 0);
        if (r.nextInt(5) == 0) w.spawnParticle(EnumParticleTypes.LAVA, x, y, z, 0, 0, 0);
        if (r.nextInt(8) == 0)
            w.playSound(x, y, z, SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.BLOCKS, 0.6F + r.nextFloat() * 0.4F, 0.8F + r.nextFloat() * 0.3F, false);
    }
}
