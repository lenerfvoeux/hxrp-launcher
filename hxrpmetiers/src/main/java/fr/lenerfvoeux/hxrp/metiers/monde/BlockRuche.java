package fr.lenerfvoeux.hxrp.metiers.monde;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
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
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.Random;

/**
 * Ruche sauvage accrochée aux troncs. Elle se remplit de miel avec le temps ;
 * clic droit avec une fiole vide sur une ruche pleine pour récolter du miel.
 */
public class BlockRuche extends Block {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyBool PLEINE = PropertyBool.create("pleine");

    public BlockRuche() {
        super(Material.WOOD);
        setRegistryName(HxrpMetiers.MODID, "ruche_sauvage");
        setTranslationKey(HxrpMetiers.MODID + ".ruche_sauvage");
        setHardness(0.8f);
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
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite()).withProperty(PLEINE, false);
    }

    @Override
    public void updateTick(World w, BlockPos pos, IBlockState s, Random r) {
        if (!w.isRemote && !s.getValue(PLEINE) && r.nextInt(6) == 0) w.setBlockState(pos, s.withProperty(PLEINE, true), 2);
    }

    @Override
    public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float hx, float hy, float hz) {
        ItemStack tenu = p.getHeldItem(hand);
        if (tenu.getItem() != Items.GLASS_BOTTLE || !s.getValue(PLEINE)) return false;
        if (!w.isRemote) {
            if (!p.capabilities.isCreativeMode) tenu.shrink(1);
            ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(ModRegistry.FOOD.get("miel")));
            w.setBlockState(pos, s.withProperty(PLEINE, false), 2);
            w.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0f, 1.0f);
        }
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
