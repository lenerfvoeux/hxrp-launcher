package fr.lenerfvoeux.hxrp.metiers.monde;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/** Banc de moules ou d'huîtres, sur le sable au bord de la mer ; se ramasse à la main. */
public class BlockCoquillages extends Block {
    private static final AxisAlignedBB BOITE = new AxisAlignedBB(0, 0, 0, 1, 0.125, 1);
    private final String produit;

    public BlockCoquillages(String id, String produit) {
        super(Material.GROUND);
        this.produit = produit;
        setRegistryName(HxrpMetiers.MODID, id);
        setTranslationKey(HxrpMetiers.MODID + "." + id);
        setHardness(0.4f);
        setSoundType(SoundType.STONE);
    }

    @Override public Item getItemDropped(IBlockState s, Random r, int fortune) { return ModRegistry.FOOD.get(produit); }
    @Override public int quantityDropped(Random r) { return 1 + r.nextInt(3); }
    @Override public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos pos) { return BOITE; }
    @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState s, IBlockAccess w, BlockPos pos) { return NULL_AABB; }
    @Override public boolean isOpaqueCube(IBlockState s) { return false; }
    @Override public boolean isFullCube(IBlockState s) { return false; }
    @Override public BlockFaceShape getBlockFaceShape(IBlockAccess w, IBlockState s, BlockPos pos, EnumFacing f) { return BlockFaceShape.UNDEFINED; }

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

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }
}
