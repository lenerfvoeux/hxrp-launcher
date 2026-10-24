package fr.lenerfvoeux.hxrp.metiers.virus.monde;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.monde.Recharge;
import fr.lenerfvoeux.hxrp.metiers.monde.TileRecharge;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Defs;
import fr.lenerfvoeux.hxrp.metiers.virus.item.Objets;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Un champignon accroché au flanc d'un tronc (pleurote, shiitake, chaga…), comme une cabosse de cacao :
 * FACING pointe vers le tronc qui le porte. Trois stades ; clic droit (main nue) quand il est mûr pour le cueillir,
 * il repart du stade 0 et repousse en temps réel selon sa rareté (voir Recharge). Incassable en survie.
 * Les admins le posent sur le flanc d'un tronc du bon bois (onglet « Blocs du monde »).
 */
public class BlockChampiTronc extends Block implements Recharge.Bloc {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 2);
    private static final double[] SAILLIE = {4, 6, 8}, LARGEUR = {5, 8, 11}, HAUT = {4, 6, 9};

    public final Defs.Ingredient def;

    public BlockChampiTronc(Defs.Ingredient d) {
        super(Recharge.TENACE);
        this.def = d;
        setRegistryName(HxrpMetiers.MODID, "tronc_" + d.id);
        setTranslationKey(HxrpMetiers.MODID + ".tronc_" + d.id);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(AGE, 2));
        setTickRandomly(true);
        setBlockUnbreakable();
        setResistance(6000000f);
        setSoundType(SoundType.CLOTH);
    }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FACING, AGE); }

    @Override
    public IBlockState getStateFromMeta(int m) {
        return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(m & 3)).withProperty(AGE, Math.min(2, m >> 2));
    }

    @Override
    public int getMetaFromState(IBlockState s) {
        return s.getValue(FACING).getHorizontalIndex() | s.getValue(AGE) << 2;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos pos) {
        int a = s.getValue(AGE);
        double sa = SAILLIE[a] / 16, la = LARGEUR[a] / 32, h = HAUT[a] / 16, y0 = 0.5 - h / 2;
        switch (s.getValue(FACING)) {
            case NORTH: return new AxisAlignedBB(0.5 - la, y0, 0, 0.5 + la, y0 + h, sa);
            case SOUTH: return new AxisAlignedBB(0.5 - la, y0, 1 - sa, 0.5 + la, y0 + h, 1);
            case WEST: return new AxisAlignedBB(0, y0, 0.5 - la, sa, y0 + h, 0.5 + la);
            default: return new AxisAlignedBB(1 - sa, y0, 0.5 - la, 1, y0 + h, 0.5 + la);
        }
    }

    @Nullable @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState s, IBlockAccess w, BlockPos pos) { return NULL_AABB; }
    @Override public boolean isOpaqueCube(IBlockState s) { return false; }
    @Override public boolean isFullCube(IBlockState s) { return false; }
    @Override public boolean isPassable(IBlockAccess w, BlockPos pos) { return true; }
    @Override public BlockFaceShape getBlockFaceShape(IBlockAccess w, IBlockState s, BlockPos pos, EnumFacing f) { return BlockFaceShape.UNDEFINED; }

    @SideOnly(Side.CLIENT)
    @Override public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }

    public boolean tient(World w, BlockPos pos, IBlockState s) {
        return Cueillette.boisOk(def.recolte.bois, w.getBlockState(pos.offset(s.getValue(FACING))));
    }

    // ------------------------------------------------------------------ pose par un admin : sur le flanc d'un tronc
    @Override
    public boolean canPlaceBlockOnSide(World w, BlockPos pos, EnumFacing face) {
        return face.getAxis().isHorizontal() && w.getBlockState(pos).getBlock().isReplaceable(w, pos)
                && Cueillette.boisOk(def.recolte.bois, w.getBlockState(pos.offset(face.getOpposite())));
    }

    @Override
    public IBlockState getStateForPlacement(World w, BlockPos pos, EnumFacing face, float x, float y, float z, int meta, EntityLivingBase placer) {
        return getDefaultState().withProperty(FACING, face.getAxis().isHorizontal() ? face.getOpposite() : EnumFacing.NORTH);
    }

    /** Sans son tronc, il disparaît sans rien donner. */
    @Override
    public void neighborChanged(IBlockState s, World w, BlockPos pos, Block b, BlockPos from) {
        if (!tient(w, pos, s)) w.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
    }

    // ------------------------------------------------------------------ recharge
    @Override public boolean plein(IBlockState s) { return s.getValue(AGE) >= 2; }
    @Override public IBlockState etat(IBlockState s, double a) { return s.withProperty(AGE, a >= 1 ? 2 : a >= 0.5 ? 1 : 0); }
    @Override public double avancement(IBlockState s) { return s.getValue(AGE) / 2.0; }
    @Override public long duree(IBlockState s) { return Cueillette.duree(def); }

    @Override public boolean hasTileEntity(IBlockState s) { return s.getBlock() == this && !plein(s); }
    @Override public TileEntity createTileEntity(World w, IBlockState s) { return new TileRecharge(); }

    @Override
    public void updateTick(World w, BlockPos pos, IBlockState s, Random r) {
        if (!tient(w, pos, s)) {
            w.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
            return;
        }
        Recharge.rattraper(w, pos, s, this);
    }

    @Override
    public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float hx, float hy, float hz) {
        ItemStack tenu = p.getHeldItem(hand);
        if (hand != EnumHand.MAIN_HAND) return false;
        s = Recharge.rattraper(w, pos, s, this);
        if (!plein(s)) {
            if (!tenu.isEmpty()) return false;
            if (!w.isRemote) Recharge.dire(p, "Pas encore mûr" + Recharge.reste(w, pos) + ".");
            return true;
        }
        if (!w.isRemote) {
            Item i = Objets.item(def.id);
            if (i != null) Recharge.donner(p, new ItemStack(i, Cueillette.quantite(def, w.rand)));
            Recharge.effets(w, pos, s, 0.5, SoundEvents.BLOCK_CLOTH_BREAK, 0.8f, 1.2f);
            Recharge.vider(w, pos, s.withProperty(AGE, 0), duree(s));
        }
        return true;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess w, BlockPos pos, IBlockState s, int fortune) {}

    @Override
    public ItemStack getPickBlock(IBlockState s, RayTraceResult t, World w, BlockPos pos, EntityPlayer p) {
        Item i = Item.getItemFromBlock(this);
        return i == Items.AIR ? ItemStack.EMPTY : new ItemStack(i);
    }
}
