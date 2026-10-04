package fr.lenerfvoeux.hxrp.metiers.virus.monde;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Defs;
import fr.lenerfvoeux.hxrp.metiers.virus.item.Objets;
import net.minecraft.block.BlockBush;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Une plante ou un champignon du Virus, posé au sol, en trois stades : 0 pousse, 1 en croissance, 2 mûr.
 * Clic droit quand il est mûr : on cueille sans arracher (la plante repart du stade 0). Casser rend l'ingrédient.
 * L'ingrédient se replante sur le bon sol (voir Cueillette).
 */
public class BlockPlanteVirus extends BlockBush implements IGrowable {
    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 2);
    private static final AxisAlignedBB[] PLANTE = {new AxisAlignedBB(0.3, 0, 0.3, 0.7, 0.3, 0.7),
            new AxisAlignedBB(0.2, 0, 0.2, 0.8, 0.6, 0.8), new AxisAlignedBB(0.15, 0, 0.15, 0.85, 0.85, 0.85)};
    private static final AxisAlignedBB[] CHAMPIGNON = {new AxisAlignedBB(0.35, 0, 0.35, 0.65, 0.2, 0.65),
            new AxisAlignedBB(0.25, 0, 0.25, 0.75, 0.35, 0.75), new AxisAlignedBB(0.2, 0, 0.2, 0.8, 0.5, 0.8)};
    private static final AxisAlignedBB TAPIS = new AxisAlignedBB(0, 0, 0, 1, 0.0625, 1);

    public final Defs.Ingredient def;

    public BlockPlanteVirus(Defs.Ingredient d) {
        super(Material.PLANTS);
        this.def = d;
        setRegistryName(HxrpMetiers.MODID, "plante_" + d.id);
        setTranslationKey(HxrpMetiers.MODID + ".plante_" + d.id);
        setDefaultState(blockState.getBaseState().withProperty(AGE, 0));
        setTickRandomly(true);
        setHardness(0);
        setSoundType(d.recolte.champignon ? SoundType.CLOTH : SoundType.PLANT);
        if (d.recolte.lumiere > 0) setLightLevel(d.recolte.lumiere / 15f);
    }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, AGE); }
    @Override public IBlockState getStateFromMeta(int m) { return getDefaultState().withProperty(AGE, Math.min(2, m)); }
    @Override public int getMetaFromState(IBlockState s) { return s.getValue(AGE); }

    public int age(IBlockState s) { return s.getValue(AGE); }

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

    // ------------------------------------------------------------------ sol
    @Override
    protected boolean canSustainBush(IBlockState sol) {
        return Cueillette.solOk(def, sol);
    }

    @Override
    public boolean canBlockStay(World w, BlockPos pos, IBlockState s) {
        return Cueillette.solOk(def, w.getBlockState(pos.down()));
    }

    // ------------------------------------------------------------------ pousse
    @Override
    public void updateTick(World w, BlockPos pos, IBlockState s, Random r) {
        super.updateTick(w, pos, s, r);
        if (w.getBlockState(pos).getBlock() != this || age(s) >= 2) return;
        if (!conditionsDePousse(w, pos)) return;
        if (r.nextDouble() < Cueillette.chancePousse(def)) w.setBlockState(pos, s.withProperty(AGE, age(s) + 1), 2);
    }

    private boolean conditionsDePousse(World w, BlockPos pos) {
        if (def.recolte.pluie && !w.isRainingAt(pos.up()) && w.rand.nextInt(4) != 0) return false;
        if (def.recolte.champignon || def.recolte.ombre) return true;
        return w.getLightFromNeighbors(pos.up()) >= 8;
    }

    @Override public boolean canGrow(World w, BlockPos pos, IBlockState s, boolean client) { return age(s) < 2; }

    /** La poudre d'os aide les plantes communes, pas les raretés. */
    @Override public boolean canUseBonemeal(World w, Random r, BlockPos pos, IBlockState s) { return Cueillette.commune(def); }

    @Override
    public void grow(World w, Random r, BlockPos pos, IBlockState s) {
        w.setBlockState(pos, s.withProperty(AGE, Math.min(2, age(s) + 1)), 2);
    }

    // ------------------------------------------------------------------ récolte
    private Item produit() {
        return Objets.item(def.id);
    }

    @Override
    public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float hx, float hy, float hz) {
        if (age(s) < 2) return false;
        if (p.getHeldItem(hand).getItem() == Items.DYE) return false;
        if (w.isRemote) return true;
        String refus = Cueillette.refusCueillette(def, w);
        if (refus != null) {
            p.sendStatusMessage(new TextComponentString(TextFormatting.GRAY + "" + TextFormatting.ITALIC + refus), true);
            return true;
        }
        Item i = produit();
        if (i != null) spawnAsEntity(w, pos, new ItemStack(i, Cueillette.quantite(def, w.rand, 0)));
        w.setBlockState(pos, s.withProperty(AGE, 0), 2);
        w.playSound(null, pos, def.recolte.champignon ? SoundEvents.BLOCK_CLOTH_BREAK : SoundEvents.BLOCK_GRASS_BREAK, SoundCategory.BLOCKS, 0.8f, 1.1f);
        return true;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess w, BlockPos pos, IBlockState s, int fortune) {
        Item i = produit();
        if (i == null) return;
        Random r = w instanceof World ? ((World) w).rand : RANDOM;
        boolean mur = age(s) >= 2 && (!(w instanceof World) || Cueillette.refusCueillette(def, (World) w) == null);
        drops.add(new ItemStack(i, mur ? Cueillette.quantite(def, r, fortune) : 1));
    }

    @Override
    public ItemStack getPickBlock(IBlockState s, RayTraceResult t, World w, BlockPos pos, EntityPlayer p) {
        Item i = produit();
        return i == null ? ItemStack.EMPTY : new ItemStack(i);
    }
}
