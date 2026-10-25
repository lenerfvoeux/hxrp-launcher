package fr.lenerfvoeux.hxrp.bestiaire.bloc;

import fr.lenerfvoeux.hxrp.bestiaire.entite.EntityBoss;
import fr.lenerfvoeux.hxrp.bestiaire.entite.Effets;
import fr.lenerfvoeux.hxrp.bestiaire.stats.Stats;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Graine-piège de Xerra (phase 2) : un bulbe rouge à peine visible, planté au sol. Le joueur qui marche dessus est
 * enraciné (effet et durée de l'attaque « graines_pieges » du classeur) et la graine éclate. Seule, elle se flétrit
 * au bout de 30 s. Ne lâche rien.
 */
public class BlockGrainePiege extends Block {
    private static final AxisAlignedBB FORME = new AxisAlignedBB(0.25, 0, 0.25, 0.75, 0.3125, 0.75);
    private static final int VIE = 600;

    public BlockGrainePiege() {
        super(Material.PLANTS);
        setHardness(0F);
        setSoundType(SoundType.PLANT);
    }

    @Override
    public void onBlockAdded(World w, BlockPos pos, IBlockState s) {
        if (!w.isRemote) w.scheduleUpdate(pos, this, VIE + w.rand.nextInt(60));
    }

    @Override
    public void updateTick(World w, BlockPos pos, IBlockState s, Random r) {
        if (!w.isRemote) w.setBlockToAir(pos);
    }

    @Override
    public void neighborChanged(IBlockState s, World w, BlockPos pos, Block voisin, BlockPos de) {
        if (!w.isRemote && !w.getBlockState(pos.down()).isSideSolid(w, pos.down(), EnumFacing.UP)) w.setBlockToAir(pos);
    }

    @Override
    public void onEntityCollision(World w, BlockPos pos, IBlockState s, Entity e) {
        if (w.isRemote || !(e instanceof EntityPlayer) || !EntityBoss.valable((EntityPlayer) e)) return;
        EntityPlayer p = (EntityPlayer) e;
        Stats.Attaque a = Stats.boss("xerra").attaque("graines_pieges");
        if (a.degats > 0) p.attackEntityFrom(DamageSource.CACTUS, (float) a.degats);
        Effets.appliquer(p, null, a.effet, a.dureeEffet, false);
        if (w instanceof WorldServer)
            ((WorldServer) w).spawnParticle(EnumParticleTypes.BLOCK_CRACK, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 25, 0.3, 0.1, 0.3, 0.1,
                    Block.getStateId(net.minecraft.init.Blocks.NETHER_WART_BLOCK.getDefaultState()));
        w.playSound(null, pos, SoundEvents.BLOCK_SLIME_BREAK, SoundCategory.HOSTILE, 1F, 0.7F);
        w.setBlockToAir(pos);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos pos) {
        return FORME;
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState s, IBlockAccess w, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public boolean isOpaqueCube(IBlockState s) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState s) {
        return false;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess w, IBlockState s, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess w, BlockPos pos, IBlockState s, int fortune) {
    }

    @Override
    public ItemStack getPickBlock(IBlockState s, net.minecraft.util.math.RayTraceResult cible, World w, BlockPos pos,
                                  net.minecraft.entity.player.EntityPlayer joueur) {
        return ItemStack.EMPTY;
    }
}
