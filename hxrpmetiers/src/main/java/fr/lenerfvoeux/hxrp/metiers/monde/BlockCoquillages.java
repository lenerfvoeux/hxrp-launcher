package fr.lenerfvoeux.hxrp.metiers.monde;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.ModConfig;
import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * Banc de moules ou d'huîtres, sur le sable au bord de la mer. Clic droit (main nue) sur un banc plein : 1 à 3
 * coquillages ; le banc reste vide (coquilles ouvertes, sans chair) puis se remplit en temps réel (voir Recharge).
 */
public class BlockCoquillages extends Block implements Recharge.Bloc {
    /** Vrai une fois ramassé (méta 0 = plein, comme les bancs d'avant la v0.7). */
    public static final PropertyBool VIDE = PropertyBool.create("vide");
    private static final AxisAlignedBB BOITE = new AxisAlignedBB(0, 0, 0, 1, 0.125, 1);
    private final String produit;

    public BlockCoquillages(String id, String produit) {
        super(Material.GROUND);
        this.produit = produit;
        setRegistryName(HxrpMetiers.MODID, id);
        setTranslationKey(HxrpMetiers.MODID + "." + id);
        setBlockUnbreakable();
        setResistance(6000000f);
        setSoundType(SoundType.STONE);
        setTickRandomly(true);
        setDefaultState(blockState.getBaseState().withProperty(VIDE, false));
    }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, VIDE); }
    @Override public IBlockState getStateFromMeta(int m) { return getDefaultState().withProperty(VIDE, (m & 1) == 1); }
    @Override public int getMetaFromState(IBlockState s) { return s.getValue(VIDE) ? 1 : 0; }

    @Override public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos pos) { return BOITE; }
    @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState s, IBlockAccess w, BlockPos pos) { return NULL_AABB; }
    @Override public boolean isOpaqueCube(IBlockState s) { return false; }
    @Override public boolean isFullCube(IBlockState s) { return false; }
    @Override public boolean isPassable(IBlockAccess w, BlockPos pos) { return true; }
    @Override public BlockFaceShape getBlockFaceShape(IBlockAccess w, IBlockState s, BlockPos pos, EnumFacing f) { return BlockFaceShape.UNDEFINED; }

    @Override
    public boolean canPlaceBlockAt(World w, BlockPos pos) {
        return super.canPlaceBlockAt(w, pos) && w.getBlockState(pos.down()).isSideSolid(w, pos.down(), EnumFacing.UP);
    }

    @Override
    public void neighborChanged(IBlockState s, World w, BlockPos pos, Block b, BlockPos from) {
        if (!w.getBlockState(pos.down()).isSideSolid(w, pos.down(), EnumFacing.UP)) w.setBlockToAir(pos);
    }

    @Override public void getDrops(NonNullList<ItemStack> drops, IBlockAccess w, BlockPos pos, IBlockState s, int fortune) {}

    @Override
    public ItemStack getPickBlock(IBlockState s, net.minecraft.util.math.RayTraceResult t, World w, BlockPos pos, EntityPlayer p) {
        return new ItemStack(this);
    }

    // ------------------------------------------------------------------ recharge
    @Override public boolean plein(IBlockState s) { return !s.getValue(VIDE); }
    @Override public IBlockState etat(IBlockState s, double a) { return s.withProperty(VIDE, a < 1); }
    @Override public double avancement(IBlockState s) { return plein(s) ? 1 : 0; }
    @Override public long duree(IBlockState s) { return ModConfig.rechargeCoquillagesMinutes * Recharge.MINUTE; }

    @Override public boolean hasTileEntity(IBlockState s) { return s.getBlock() == this && !plein(s); }
    @Override public TileEntity createTileEntity(World w, IBlockState s) { return new TileRecharge(); }

    @Override
    public void updateTick(World w, BlockPos pos, IBlockState s, Random r) {
        Recharge.rattraper(w, pos, s, this);
    }

    @Override
    public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float hx, float hy, float hz) {
        if (hand != EnumHand.MAIN_HAND) return false;
        s = Recharge.rattraper(w, pos, s, this);
        if (!plein(s)) {
            if (!p.getHeldItem(hand).isEmpty()) return false;
            if (!w.isRemote) Recharge.dire(p, "Le banc est vide, reviens plus tard" + Recharge.reste(w, pos) + ".");
            return true;
        }
        if (!w.isRemote) {
            Item i = ModRegistry.FOOD.get(produit);
            if (i != null) Recharge.donner(p, new ItemStack(i, 1 + w.rand.nextInt(3)));
            Recharge.effets(w, pos, s, 0.15, SoundEvents.BLOCK_GRAVEL_BREAK, 0.8f, 1.3f);
            w.playSound(null, pos, SoundEvents.ENTITY_GENERIC_SPLASH, net.minecraft.util.SoundCategory.BLOCKS, 0.3f, 1.6f);
            Recharge.vider(w, pos, s.withProperty(VIDE, true), duree(s));
        }
        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }
}
