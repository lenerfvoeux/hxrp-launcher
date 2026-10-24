package fr.lenerfvoeux.hxrp.metiers.monde;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.ModConfig;
import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import fr.lenerfvoeux.hxrp.metiers.virus.item.ItemPioche;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Minerai de sel : dans la pierre, entre les couches 20 et 70. Clic droit avec une pioche de prospecteur : 2 à 4 sels,
 * sans rien casser ; le filon reste épuisé (roche grise, veines vides) puis se recharge en temps réel (voir Recharge).
 */
public class BlockMineraiSel extends Block implements Recharge.Bloc {
    /** Vrai une fois exploité (méta 0 = plein, comme le minerai d'avant la v0.7). */
    public static final PropertyBool EPUISE = PropertyBool.create("epuise");

    public BlockMineraiSel() {
        super(Material.ROCK);
        setRegistryName(HxrpMetiers.MODID, "minerai_de_sel");
        setTranslationKey(HxrpMetiers.MODID + ".minerai_de_sel");
        setBlockUnbreakable();
        setResistance(6000000f);
        setSoundType(SoundType.STONE);
        setTickRandomly(true);
        setDefaultState(blockState.getBaseState().withProperty(EPUISE, false));
    }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, EPUISE); }
    @Override public IBlockState getStateFromMeta(int m) { return getDefaultState().withProperty(EPUISE, (m & 1) == 1); }
    @Override public int getMetaFromState(IBlockState s) { return s.getValue(EPUISE) ? 1 : 0; }

    @Override public void getDrops(NonNullList<ItemStack> drops, IBlockAccess w, BlockPos pos, IBlockState s, int fortune) {}

    @Override
    public ItemStack getPickBlock(IBlockState s, net.minecraft.util.math.RayTraceResult t, World w, BlockPos pos, EntityPlayer p) {
        return new ItemStack(this);
    }

    // ------------------------------------------------------------------ recharge
    @Override public boolean plein(IBlockState s) { return !s.getValue(EPUISE); }
    @Override public IBlockState etat(IBlockState s, double a) { return s.withProperty(EPUISE, a < 1); }
    @Override public double avancement(IBlockState s) { return plein(s) ? 1 : 0; }
    @Override public long duree(IBlockState s) { return ModConfig.rechargeSelMinutes * Recharge.MINUTE; }

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
            if (!p.getHeldItem(hand).isEmpty() && !ItemPioche.est(p.getHeldItem(hand))) return false;
            if (!w.isRemote) Recharge.dire(p, "Le filon est épuisé, reviens plus tard" + Recharge.reste(w, pos) + ".");
            return true;
        }
        ItemStack pioche = Recharge.pioche(p, hand);
        if (pioche.isEmpty()) return Recharge.sansPioche(p, hand);
        if (!w.isRemote) {
            Item sel = ModRegistry.FOOD.get("sel");
            if (sel != null) Recharge.donner(p, new ItemStack(sel, 2 + w.rand.nextInt(3)));
            ItemPioche.user(pioche);
            Recharge.eclatsFace(w, pos, f, s, SoundEvents.BLOCK_STONE_BREAK, 0.9f, 1.25f);
            w.playSound(null, pos, SoundEvents.BLOCK_ANVIL_PLACE, net.minecraft.util.SoundCategory.BLOCKS, 0.15f, 2.0f);
            Recharge.vider(w, pos, s.withProperty(EPUISE, true), duree(s));
        }
        return true;
    }
}
