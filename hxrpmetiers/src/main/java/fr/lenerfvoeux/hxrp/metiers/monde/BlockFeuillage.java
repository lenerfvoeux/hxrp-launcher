package fr.lenerfvoeux.hxrp.metiers.monde;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.ModConfig;
import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockPlanks;
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
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * Feuillage d'arbre fruitier. Environ un bloc de feuilles sur trois porte des fruits : sans fruits (0), en fleurs (1),
 * fruits mûrs (2). Clic droit sur un feuillage mûr : 1 à 2 fruits, sans abîmer l'arbre ; il refleurit aussitôt et ses
 * fruits mûrissent en temps réel (voir Recharge). Incassable en survie, il ne donne jamais de pousses.
 */
public class BlockFeuillage extends BlockLeaves implements Recharge.Bloc {
    public static final PropertyInteger FRUIT = PropertyInteger.create("fruit", 0, 2);

    public final Recolte.Arbre arbre;
    BlockPousse pousse;

    public BlockFeuillage(Recolte.Arbre a) {
        this.arbre = a;
        setRegistryName(HxrpMetiers.MODID, "feuilles_" + a.id);
        setTranslationKey(HxrpMetiers.MODID + ".feuilles_" + a.id);
        setDefaultState(blockState.getBaseState().withProperty(CHECK_DECAY, true).withProperty(DECAYABLE, true).withProperty(FRUIT, 0));
        setBlockUnbreakable();
        setResistance(6000000f);
    }

    public Item fruit() { return ModRegistry.OBJETS_MONDE.containsKey(arbre.fruit) ? ModRegistry.OBJETS_MONDE.get(arbre.fruit) : ModRegistry.FOOD.get(arbre.fruit); }

    /** Ce bloc de feuilles porte-t-il des fruits ? (un sur trois, fixe pour une position donnée) */
    public static boolean porteur(BlockPos p) {
        long h = p.getX() * 3129871L ^ p.getZ() * 116129781L ^ p.getY() * 42317861L;
        h = h * h * 42317861L + h * 11L;
        return Math.floorMod(h >> 16, 3L) == 0;
    }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, CHECK_DECAY, DECAYABLE, FRUIT); }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FRUIT, Math.min(2, meta & 3)).withProperty(DECAYABLE, (meta & 4) == 0).withProperty(CHECK_DECAY, (meta & 8) > 0);
    }

    @Override
    public int getMetaFromState(IBlockState s) {
        int m = s.getValue(FRUIT);
        if (!s.getValue(DECAYABLE)) m |= 4;
        if (s.getValue(CHECK_DECAY)) m |= 8;
        return m;
    }

    @Override public BlockPlanks.EnumType getWoodType(int meta) { return BlockPlanks.EnumType.OAK; }

    // ------------------------------------------------------------------ rien ne tombe : ni pousse, ni fruit
    @Override public boolean isShearable(ItemStack item, IBlockAccess w, BlockPos pos) { return false; }
    @Override public NonNullList<ItemStack> onSheared(ItemStack item, IBlockAccess w, BlockPos pos, int fortune) { return NonNullList.create(); }
    @Override public Item getItemDropped(IBlockState s, Random r, int fortune) { return Items.AIR; }
    @Override protected void dropApple(World w, BlockPos pos, IBlockState s, int chance) {}
    @Override public void getDrops(NonNullList<ItemStack> drops, IBlockAccess w, BlockPos pos, IBlockState s, int fortune) {}

    // ------------------------------------------------------------------ recharge
    @Override public boolean plein(IBlockState s) { return s.getValue(FRUIT) == 2; }
    @Override public IBlockState etat(IBlockState s, double a) { return s.withProperty(FRUIT, a >= 1 ? 2 : 1); }
    @Override public double avancement(IBlockState s) { return s.getValue(FRUIT) == 2 ? 1 : 0.5; }
    @Override public long duree(IBlockState s) { return ModConfig.rechargeFruitsMinutes * Recharge.MINUTE; }

    /** Seuls les feuillages en fleurs ont une horloge (les feuilles sans fruits n'en ont jamais besoin). */
    @Override public boolean hasTileEntity(IBlockState s) { return s.getBlock() == this && s.getValue(FRUIT) == 1; }
    @Override public TileEntity createTileEntity(World w, IBlockState s) { return new TileRecharge(); }

    @Override
    public void updateTick(World w, BlockPos pos, IBlockState s, Random r) {
        super.updateTick(w, pos, s, r);
        if (w.isRemote) return;
        IBlockState now = w.getBlockState(pos);
        if (now.getBlock() != this || !porteur(pos)) return;
        // feuillage porteur encore sans fleurs (arbre poussé d'une pousse, ancien monde) : il fleurit
        if (now.getValue(FRUIT) == 0) w.setBlockState(pos, now.withProperty(FRUIT, 1), 2);
        else Recharge.rattraper(w, pos, now, this);
    }

    @Override
    public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float hx, float hy, float hz) {
        if (s.getValue(FRUIT) == 1) s = Recharge.rattraper(w, pos, s, this);
        if (!plein(s)) {
            if (hand != EnumHand.MAIN_HAND || !p.getHeldItem(hand).isEmpty() || !porteur(pos)) return false;
            if (!w.isRemote) Recharge.dire(p, "Pas encore mûr" + Recharge.reste(w, pos) + ".");
            return true;
        }
        if (!w.isRemote) {
            Item fruit = fruit();
            if (fruit != null) Recharge.donner(p, new ItemStack(fruit, 1 + w.rand.nextInt(2)));
            Recharge.effets(w, pos, s, 0.5, SoundEvents.BLOCK_GRASS_HIT, 1.0f, 1.2f);
            Recharge.vider(w, pos, s.withProperty(FRUIT, 1), duree(s));
        }
        return true;
    }

    // rendu : suit le réglage graphique des feuilles vanilla (détaillé = transparent, rapide = opaque)
    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        leavesFancy = Blocks.LEAVES.getRenderLayer() != BlockRenderLayer.SOLID;
        return super.getRenderLayer();
    }

    @Override public boolean isOpaqueCube(IBlockState s) { return false; }
}
