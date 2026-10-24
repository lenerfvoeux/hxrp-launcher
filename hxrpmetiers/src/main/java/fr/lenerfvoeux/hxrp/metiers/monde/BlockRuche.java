package fr.lenerfvoeux.hxrp.metiers.monde;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.ModConfig;
import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * Ruche sauvage accrochée aux troncs. Clic droit avec une fiole vide sur une ruche pleine pour récolter du miel,
 * ou main vide pour la cire et la propolis ; vidée, elle se remplit en temps réel (voir Recharge). Incassable en survie.
 */
public class BlockRuche extends Block implements Recharge.Bloc {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyBool PLEINE = PropertyBool.create("pleine");

    public BlockRuche() {
        super(Material.WOOD);
        setRegistryName(HxrpMetiers.MODID, "ruche_sauvage");
        setTranslationKey(HxrpMetiers.MODID + ".ruche_sauvage");
        setBlockUnbreakable();
        setResistance(6000000f);
        setSoundType(SoundType.WOOD);
        setTickRandomly(true);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(PLEINE, true));
    }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FACING, PLEINE); }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3)).withProperty(PLEINE, (meta & 4) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState s) {
        return s.getValue(FACING).getHorizontalIndex() | (s.getValue(PLEINE) ? 4 : 0);
    }

    @Override
    public IBlockState getStateForPlacement(World w, BlockPos pos, EnumFacing f, float x, float y, float z, int meta, EntityLivingBase placer) {
        // posée par un admin (créatif) : pleine, prête pour les joueurs
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite()).withProperty(PLEINE, true);
    }

    @Override public void getDrops(NonNullList<ItemStack> drops, IBlockAccess w, BlockPos pos, IBlockState s, int fortune) {}

    // ------------------------------------------------------------------ recharge
    @Override public boolean plein(IBlockState s) { return s.getValue(PLEINE); }
    @Override public IBlockState etat(IBlockState s, double a) { return s.withProperty(PLEINE, a >= 1); }
    @Override public double avancement(IBlockState s) { return plein(s) ? 1 : 0; }
    @Override public long duree(IBlockState s) { return ModConfig.rechargeRucheMinutes * Recharge.MINUTE; }

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
        boolean fiole = tenu.getItem() == Items.GLASS_BOTTLE;
        if (!tenu.isEmpty() && !fiole) return false;
        s = Recharge.rattraper(w, pos, s, this);
        if (!plein(s)) {
            if (!w.isRemote) Recharge.dire(p, "La ruche est vide, reviens plus tard" + Recharge.reste(w, pos) + ".");
            return true;
        }
        if (w.isRemote) return true;
        if (fiole) {
            // fiole vide : du miel
            if (!p.capabilities.isCreativeMode) tenu.shrink(1);
            Recharge.donner(p, new ItemStack(ModRegistry.FOOD.get("miel")));
            w.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0f, 1.0f);
        } else {
            // main vide : cire d'abeille et un peu de propolis pour l'apothicaire
            if (!fr.lenerfvoeux.hxrp.metiers.virus.VirusAPI.recolterRuche(p)) return true;
            w.playSound(null, pos, SoundEvents.BLOCK_SLIME_BREAK, SoundCategory.BLOCKS, 0.7f, 1.3f);
        }
        Recharge.effets(w, pos, s, 0.6, SoundEvents.BLOCK_WOOD_HIT, 0.6f, 1.2f);
        Recharge.vider(w, pos, s.withProperty(PLEINE, false), duree(s));
        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(IBlockState s, World w, BlockPos pos, Random r) {
        if (!s.getValue(PLEINE) || r.nextInt(3) != 0) return;
        EnumFacing f = s.getValue(FACING);
        double x = pos.getX() + 0.5 + f.getXOffset() * 0.6 + (r.nextDouble() - 0.5) * 0.4;
        double z = pos.getZ() + 0.5 + f.getZOffset() * 0.6 + (r.nextDouble() - 0.5) * 0.4;
        w.spawnParticle(EnumParticleTypes.FALLING_DUST, x, pos.getY() + 0.4, z, 0, 0, 0, Block.getStateId(net.minecraft.init.Blocks.SPONGE.getDefaultState()));
    }
}
