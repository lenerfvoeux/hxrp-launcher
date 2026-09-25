package fr.lenerfvoeux.hxrp.metiers.monde;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
 * Feuillage d'arbre fruitier. Environ un bloc de feuilles sur trois porte des fruits :
 * il fleurit (1) puis ses fruits mûrissent (2). Taper (clic gauche) ou clic droit sur un bloc mûr
 * fait tomber les fruits sans abîmer l'arbre ; ils repoussent ensuite.
 */
public class BlockFeuillage extends BlockLeaves {
    public static final PropertyInteger FRUIT = PropertyInteger.create("fruit", 0, 2);

    public final Recolte.Arbre arbre;
    BlockPousse pousse;

    public BlockFeuillage(Recolte.Arbre a) {
        this.arbre = a;
        setRegistryName(HxrpMetiers.MODID, "feuilles_" + a.id);
        setTranslationKey(HxrpMetiers.MODID + ".feuilles_" + a.id);
        setDefaultState(blockState.getBaseState().withProperty(CHECK_DECAY, true).withProperty(DECAYABLE, true).withProperty(FRUIT, 0));
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

    @Override
    public NonNullList<ItemStack> onSheared(ItemStack item, IBlockAccess w, BlockPos pos, int fortune) {
        return NonNullList.withSize(1, new ItemStack(this));
    }

    @Override public Item getItemDropped(IBlockState s, Random r, int fortune) { return Item.getItemFromBlock(pousse); }
    @Override protected int getSaplingDropChance(IBlockState s) { return 20; }
    @Override protected void dropApple(World w, BlockPos pos, IBlockState s, int chance) {}
    @Override public int damageDropped(IBlockState s) { return 0; }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess w, BlockPos pos, IBlockState s, int fortune) {
        super.getDrops(drops, w, pos, s, fortune);
        if (s.getValue(FRUIT) == 2 && fruit() != null) drops.add(new ItemStack(fruit(), 1));
    }

    @Override
    public void updateTick(World w, BlockPos pos, IBlockState s, Random r) {
        super.updateTick(w, pos, s, r);
        if (w.isRemote) return;
        IBlockState now = w.getBlockState(pos);
        if (now.getBlock() != this || !porteur(pos)) return;
        int f = now.getValue(FRUIT);
        if (f < 2 && r.nextInt(f == 0 ? 5 : 8) == 0) w.setBlockState(pos, now.withProperty(FRUIT, f + 1), 2);
    }

    /** Fait tomber les fruits mûrs ; renvoie vrai s'il y en avait. */
    public boolean recolter(World w, BlockPos pos, IBlockState s, EntityPlayer p) {
        if (s.getValue(FRUIT) != 2) return false;
        if (!w.isRemote) {
            Item f = fruit();
            if (f != null) spawnAsEntity(w, pos, new ItemStack(f, 1 + (w.rand.nextInt(3) == 0 ? 1 : 0)));
            w.setBlockState(pos, s.withProperty(FRUIT, 0), 2);
            w.playSound(null, pos, SoundEvents.BLOCK_GRASS_HIT, SoundCategory.BLOCKS, 1.0f, 1.2f);
        }
        return true;
    }

    @Override
    public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float hx, float hy, float hz) {
        return recolter(w, pos, s, p);
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
