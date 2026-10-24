package fr.lenerfvoeux.hxrp.metiers.virus.monde;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.monde.Recharge;
import fr.lenerfvoeux.hxrp.metiers.monde.TileRecharge;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Defs;
import fr.lenerfvoeux.hxrp.metiers.virus.item.ItemPioche;
import fr.lenerfvoeux.hxrp.metiers.virus.item.Objets;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Bloc plein du monde qui renferme un ingrédient. Clic droit pour le prendre, sans rien casser :
 * on fouille à la main la terre truffière et la vase à algue noire (le bloc reste « fouillé », terre retournée),
 * on gratte la pierre moussue humide à la pioche de prospecteur (elle reste « épuisée », sans mousse).
 * Le bloc se recharge en temps réel selon la rareté de l'ingrédient (voir Recharge). Incassable en survie.
 */
public class BlockSolVirus extends Block implements Recharge.Bloc {
    /** Vrai une fois fouillé ou gratté (méta 0 = plein, comme les blocs d'avant la v0.7). */
    public static final PropertyBool VIDE = PropertyBool.create("vide");

    public final Defs.Ingredient def;
    public final String id;
    /** Roche (pioche de prospecteur) ou terre (à la main). */
    public final boolean roche;

    public BlockSolVirus(Defs.Ingredient d) {
        super(materiau(d.recolte.bloc), couleur(d.recolte.bloc));
        this.def = d;
        this.id = d.recolte.bloc;
        this.roche = materiau(d.recolte.bloc) == Material.ROCK;
        setRegistryName(HxrpMetiers.MODID, id);
        setTranslationKey(HxrpMetiers.MODID + "." + id);
        setBlockUnbreakable();
        setResistance(6000000f);
        setSoundType(roche ? SoundType.STONE : "vase_a_algue_noire".equals(id) ? SoundType.SLIME : SoundType.GROUND);
        setTickRandomly(true);
        setDefaultState(blockState.getBaseState().withProperty(VIDE, false));
    }

    private static Material materiau(String b) {
        switch (b == null ? "" : b) {
            case "terre_truffiere": return Material.GROUND;
            case "vase_a_algue_noire": return Material.CLAY;
            default: return Material.ROCK;
        }
    }

    private static MapColor couleur(String b) {
        switch (b == null ? "" : b) {
            case "terre_truffiere": return MapColor.BROWN;
            case "vase_a_algue_noire": return MapColor.BLACK;
            default: return MapColor.GREEN;
        }
    }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, VIDE); }
    @Override public IBlockState getStateFromMeta(int m) { return getDefaultState().withProperty(VIDE, (m & 1) == 1); }
    @Override public int getMetaFromState(IBlockState s) { return s.getValue(VIDE) ? 1 : 0; }

    @Override public void getDrops(NonNullList<ItemStack> drops, IBlockAccess w, BlockPos pos, IBlockState s, int fortune) {}

    @Override
    public ItemStack getPickBlock(IBlockState s, RayTraceResult t, World w, BlockPos pos, EntityPlayer p) {
        return new ItemStack(this);
    }

    // ------------------------------------------------------------------ recharge
    @Override public boolean plein(IBlockState s) { return !s.getValue(VIDE); }
    @Override public IBlockState etat(IBlockState s, double a) { return s.withProperty(VIDE, a < 1); }
    @Override public double avancement(IBlockState s) { return plein(s) ? 1 : 0; }
    @Override public long duree(IBlockState s) { return Cueillette.duree(def); }

    @Override public boolean hasTileEntity(IBlockState s) { return s.getBlock() == this && !plein(s); }
    @Override public TileEntity createTileEntity(World w, IBlockState s) { return new TileRecharge(); }

    @Override
    public void updateTick(World w, BlockPos pos, IBlockState s, Random r) {
        Recharge.rattraper(w, pos, s, this);
    }

    @Override
    public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float hx, float hy, float hz) {
        if (hand != EnumHand.MAIN_HAND) return false;
        ItemStack tenu = p.getHeldItem(hand);
        s = Recharge.rattraper(w, pos, s, this);
        if (!plein(s)) {
            if (!tenu.isEmpty() && !ItemPioche.est(tenu)) return false;
            if (!w.isRemote) Recharge.dire(p, (roche ? "La mousse n'a pas encore repoussé" : "Déjà fouillé, reviens plus tard") + Recharge.reste(w, pos) + ".");
            return true;
        }
        ItemStack pioche = ItemStack.EMPTY;
        if (roche) {
            pioche = Recharge.pioche(p, hand);
            if (pioche.isEmpty()) return Recharge.sansPioche(p, hand);
        } else if (!tenu.isEmpty()) {
            return false;
        }
        if (w.isRemote) return true;
        Item i = Objets.item(def.id);
        if (i != null) Recharge.donner(p, new ItemStack(i, Cueillette.quantite(def, w.rand)));
        if (roche) {
            ItemPioche.user(pioche);
            Recharge.eclatsFace(w, pos, f, s, SoundEvents.BLOCK_STONE_BREAK, 0.9f, 1.25f);
            w.playSound(null, pos, SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS, 0.15f, 2.0f);
        } else if ("vase_a_algue_noire".equals(id)) {
            Recharge.eclatsFace(w, pos, f, Blocks.CLAY.getDefaultState(), SoundEvents.BLOCK_SLIME_BREAK, 0.8f, 0.9f);
            Recharge.gouttes(w, pos, EnumParticleTypes.WATER_BUBBLE, 10);
        } else {
            Recharge.eclatsFace(w, pos, f, Blocks.DIRT.getDefaultState(), SoundEvents.BLOCK_GRAVEL_BREAK, 0.9f, 0.9f);
        }
        Recharge.vider(w, pos, s.withProperty(VIDE, true), duree(s));
        return true;
    }
}
