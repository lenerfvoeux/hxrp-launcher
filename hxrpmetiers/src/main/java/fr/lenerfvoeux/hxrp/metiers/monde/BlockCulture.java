package fr.lenerfvoeux.hxrp.metiers.monde;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.ModConfig;
import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
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
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Une culture en trois stades visibles : 0 vient d'être plantée, 1 pousse, 2 prête à récolter.
 * Clic droit (main nue) sur une plante mûre : on récolte sans arracher, elle repart du stade 0 et redevient mûre
 * en temps réel (voir Recharge). Rien ne la casse en survie et elle ne donne jamais de graines : les graines ne
 * s'obtiennent qu'en créatif, pour que les admins plantent les potagers.
 */
public class BlockCulture extends BlockCrops implements Recharge.Bloc {
    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 2);
    private static final AxisAlignedBB[] BOITES = {
            new AxisAlignedBB(0, 0, 0, 1, 0.25, 1), new AxisAlignedBB(0, 0, 0, 1, 0.6, 1), new AxisAlignedBB(0, 0, 0, 1, 0.9, 1)};

    public final Recolte.Culture def;
    Item graine;

    public BlockCulture(Recolte.Culture c) {
        this.def = c;
        setRegistryName(HxrpMetiers.MODID, "culture_" + c.id);
        setTranslationKey(HxrpMetiers.MODID + ".culture_" + c.id);
        setBlockUnbreakable();
        setResistance(6000000f);
    }

    public Item graine() { return graine; }

    @Override protected PropertyInteger getAgeProperty() { return AGE; }
    @Override public int getMaxAge() { return 2; }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, AGE); }
    @Override protected Item getSeed() { return graine; }
    @Override protected Item getCrop() { return ModRegistry.FOOD.get(def.id); }

    /** L'eau ne l'emporte pas (voir Recharge.CULTURE). */
    @SuppressWarnings("deprecation")
    @Override public Material getMaterial(IBlockState s) { return Recharge.CULTURE; }

    @Override public boolean isFoliage(IBlockAccess w, BlockPos pos) { return true; }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos pos) {
        return BOITES[Math.min(2, getAge(s))];
    }

    // ------------------------------------------------------------------ recharge
    @Override public boolean plein(IBlockState s) { return getAge(s) >= getMaxAge(); }
    @Override public IBlockState etat(IBlockState s, double a) { return withAge(a >= 1 ? 2 : a >= 0.5 ? 1 : 0); }
    @Override public double avancement(IBlockState s) { return getAge(s) / 2.0; }
    @Override public long duree(IBlockState s) { return ModConfig.rechargeCulturesMinutes * Recharge.MINUTE; }

    @Override public boolean hasTileEntity(IBlockState s) { return s.getBlock() == this && !plein(s); }
    @Override public TileEntity createTileEntity(World w, IBlockState s) { return new TileRecharge(); }

    /** Tient sur la terre labourée (ou la terre, si elle a été piétinée) ; dans le noir aussi. */
    @Override
    public boolean canBlockStay(World w, BlockPos pos, IBlockState s) {
        IBlockState sol = w.getBlockState(pos.down());
        return sol.getBlock() == Blocks.FARMLAND || sol.getBlock() == Blocks.DIRT || sol.getBlock() == Blocks.GRASS;
    }

    @Override
    public void updateTick(World w, BlockPos pos, IBlockState s, Random r) {
        checkAndDropBlock(w, pos, s);
        if (w.getBlockState(pos).getBlock() == this) Recharge.rattraper(w, pos, s, this);
    }

    /** La poudre d'os fait gagner un stade (une demi-recharge). */
    @Override
    public void grow(World w, BlockPos pos, IBlockState s) {
        TileRecharge h = Recharge.horloge(w, pos);
        if (h == null || plein(s)) return;
        if (!h.demarree()) h.demarrer(duree(s), avancement(s));
        h.avancer(0.5);
        Recharge.rattraper(w, pos, s, this);
    }

    @Override protected int getBonemealAgeIncrease(World w) { return 1; }

    // ------------------------------------------------------------------ récolte
    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess w, BlockPos pos, IBlockState s, int fortune) {}

    @Override
    public ItemStack getPickBlock(IBlockState s, net.minecraft.util.math.RayTraceResult t, World w, BlockPos pos, EntityPlayer p) {
        return graine == null ? ItemStack.EMPTY : new ItemStack(graine);
    }

    private int quantite(Random r) {
        return def.min + r.nextInt(Math.max(1, def.max - def.min + 1));
    }

    @Override
    public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float hx, float hy, float hz) {
        ItemStack tenu = p.getHeldItem(hand);
        if (tenu.getItem() == Items.DYE) return false; // laisse la poudre d'os se comporter normalement
        s = Recharge.rattraper(w, pos, s, this);
        if (!plein(s)) {
            if (hand != EnumHand.MAIN_HAND || !tenu.isEmpty()) return false;
            if (!w.isRemote) Recharge.dire(p, "Pas encore mûr" + Recharge.reste(w, pos) + ".");
            return true;
        }
        if (!w.isRemote) {
            Item crop = getCrop();
            if (crop != null) Recharge.donner(p, new ItemStack(crop, quantite(w.rand)));
            Recharge.effets(w, pos, s, 0.4, SoundEvents.BLOCK_GRASS_BREAK, 0.8f, 1.1f);
            Recharge.vider(w, pos, withAge(0), duree(s));
        }
        return true;
    }
}
