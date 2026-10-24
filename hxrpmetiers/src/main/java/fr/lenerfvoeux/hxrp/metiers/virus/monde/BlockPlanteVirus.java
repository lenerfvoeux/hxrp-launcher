package fr.lenerfvoeux.hxrp.metiers.virus.monde;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.monde.Recharge;
import fr.lenerfvoeux.hxrp.metiers.monde.TileRecharge;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Defs;
import fr.lenerfvoeux.hxrp.metiers.virus.item.Objets;
import net.minecraft.block.BlockBush;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.PropertyInteger;
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
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Une plante ou un champignon du Virus, posé au sol, en trois stades : 0 vient d'être cueilli, 1 repousse, 2 mûr.
 * Clic droit (main nue) quand il est mûr : on cueille sans arracher, la plante repart du stade 0 et redevient mûre
 * en temps réel, selon la rareté (voir Recharge et Cueillette.duree). Incassable en survie, l'eau ne l'emporte pas,
 * et l'ingrédient ne se replante pas : les admins posent les plantes depuis l'onglet « Blocs du monde ».
 */
public class BlockPlanteVirus extends BlockBush implements IGrowable, Recharge.Bloc {
    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 2);
    private static final AxisAlignedBB[] PLANTE = {new AxisAlignedBB(0.3, 0, 0.3, 0.7, 0.3, 0.7),
            new AxisAlignedBB(0.2, 0, 0.2, 0.8, 0.6, 0.8), new AxisAlignedBB(0.15, 0, 0.15, 0.85, 0.85, 0.85)};
    private static final AxisAlignedBB[] CHAMPIGNON = {new AxisAlignedBB(0.35, 0, 0.35, 0.65, 0.2, 0.65),
            new AxisAlignedBB(0.25, 0, 0.25, 0.75, 0.35, 0.75), new AxisAlignedBB(0.2, 0, 0.2, 0.8, 0.5, 0.8)};
    private static final AxisAlignedBB TAPIS = new AxisAlignedBB(0, 0, 0, 1, 0.0625, 1);

    public final Defs.Ingredient def;

    public BlockPlanteVirus(Defs.Ingredient d) {
        super(Recharge.TENACE);
        this.def = d;
        setRegistryName(HxrpMetiers.MODID, "plante_" + d.id);
        setTranslationKey(HxrpMetiers.MODID + ".plante_" + d.id);
        setDefaultState(blockState.getBaseState().withProperty(AGE, 2));
        setTickRandomly(true);
        setBlockUnbreakable();
        setResistance(6000000f);
        setSoundType(d.recolte.champignon ? SoundType.CLOTH : SoundType.PLANT);
        if (d.recolte.lumiere > 0) setLightLevel(d.recolte.lumiere / 15f);
    }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, AGE); }
    @Override public IBlockState getStateFromMeta(int m) { return getDefaultState().withProperty(AGE, Math.min(2, m)); }
    @Override public int getMetaFromState(IBlockState s) { return s.getValue(AGE); }

    public int age(IBlockState s) { return s.getValue(AGE); }

    /** Posée par un admin : mûre. */
    @Override
    public IBlockState getStateForPlacement(World w, BlockPos pos, EnumFacing f, float x, float y, float z, int meta, EntityLivingBase placer) {
        return getDefaultState();
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos pos) {
        if (def.recolte.tapis) return TAPIS;
        AxisAlignedBB b = (def.recolte.champignon ? CHAMPIGNON : PLANTE)[age(s)];
        return b.offset(s.getOffset(w, pos));
    }

    @Override
    public EnumOffsetType getOffsetType() {
        return def.recolte.tapis ? EnumOffsetType.NONE : EnumOffsetType.XZ;
    }

    /** On la traverse comme une fleur (sa matière arrête seulement l'eau). */
    @Override public boolean isPassable(IBlockAccess w, BlockPos pos) { return true; }
    @Override public boolean isFoliage(IBlockAccess w, BlockPos pos) { return true; }

    // ------------------------------------------------------------------ sol
    @Override
    protected boolean canSustainBush(IBlockState sol) {
        return Cueillette.solOk(def, sol);
    }

    @Override
    public boolean canBlockStay(World w, BlockPos pos, IBlockState s) {
        return Cueillette.solOk(def, w.getBlockState(pos.down()));
    }

    /** Sans son sol, elle disparaît sans rien donner. */
    @Override
    protected void checkAndDropBlock(World w, BlockPos pos, IBlockState s) {
        if (!canBlockStay(w, pos, s)) w.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
    }

    // ------------------------------------------------------------------ recharge
    @Override public boolean plein(IBlockState s) { return age(s) >= 2; }
    @Override public IBlockState etat(IBlockState s, double a) { return s.withProperty(AGE, a >= 1 ? 2 : a >= 0.5 ? 1 : 0); }
    @Override public double avancement(IBlockState s) { return age(s) / 2.0; }
    @Override public long duree(IBlockState s) { return Cueillette.duree(def); }

    @Override public boolean hasTileEntity(IBlockState s) { return s.getBlock() == this && !plein(s); }
    @Override public TileEntity createTileEntity(World w, IBlockState s) { return new TileRecharge(); }

    @Override
    public void updateTick(World w, BlockPos pos, IBlockState s, Random r) {
        checkAndDropBlock(w, pos, s);
        if (w.getBlockState(pos).getBlock() == this) Recharge.rattraper(w, pos, s, this);
    }

    @Override public boolean canGrow(World w, BlockPos pos, IBlockState s, boolean client) { return !plein(s); }

    /** La poudre d'os aide les plantes communes (un stade : une demi-recharge), pas les raretés. */
    @Override public boolean canUseBonemeal(World w, Random r, BlockPos pos, IBlockState s) { return Cueillette.commune(def); }

    @Override
    public void grow(World w, Random r, BlockPos pos, IBlockState s) {
        TileRecharge h = Recharge.horloge(w, pos);
        if (h == null || plein(s)) return;
        if (!h.demarree()) h.demarrer(duree(s), avancement(s));
        h.avancer(0.5);
        Recharge.rattraper(w, pos, s, this);
    }

    // ------------------------------------------------------------------ récolte
    @Override
    public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float hx, float hy, float hz) {
        ItemStack tenu = p.getHeldItem(hand);
        if (hand != EnumHand.MAIN_HAND || tenu.getItem() == Items.DYE) return false;
        s = Recharge.rattraper(w, pos, s, this);
        if (!plein(s)) {
            if (!tenu.isEmpty()) return false;
            if (!w.isRemote) Recharge.dire(p, "Pas encore mûr" + Recharge.reste(w, pos) + ".");
            return true;
        }
        if (w.isRemote) return true;
        String refus = Cueillette.refusCueillette(def, w);
        if (refus != null) {
            Recharge.dire(p, refus);
            return true;
        }
        Item i = Objets.item(def.id);
        if (i != null) Recharge.donner(p, new ItemStack(i, Cueillette.quantite(def, w.rand)));
        Recharge.effets(w, pos, s, 0.3, def.recolte.champignon ? SoundEvents.BLOCK_CLOTH_BREAK : SoundEvents.BLOCK_GRASS_BREAK, 0.8f, 1.1f);
        Recharge.vider(w, pos, s.withProperty(AGE, 0), duree(s));
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
