package fr.lenerfvoeux.hxrp.metiers.virus.monde;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.data.Fraicheur;
import fr.lenerfvoeux.hxrp.metiers.virus.VirusConfig;
import fr.lenerfvoeux.hxrp.metiers.virus.item.Objets;
import net.minecraft.block.BlockLilyPad;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
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
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * Le lotus de l'aube : une seule fleur par zone, sur l'eau calme des rivières. Cueillie, elle se referme en bouton
 * et refleurit au bout de quelques jours réels. La plante elle-même ne se déracine pas.
 */
public class BlockLotus extends BlockLilyPad {
    public static final PropertyBool FLEURI = PropertyBool.create("fleuri");
    private static final long JOUR = 86_400_000L;

    public BlockLotus() {
        setRegistryName(HxrpMetiers.MODID, "lotus_de_l_aube_plante");
        setTranslationKey(HxrpMetiers.MODID + ".lotus_de_l_aube_plante");
        setDefaultState(blockState.getBaseState().withProperty(FLEURI, true));
        setTickRandomly(true);
        setBlockUnbreakable();
        setResistance(6000f);
        setSoundType(SoundType.PLANT);
        setLightLevel(4 / 15f);
    }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FLEURI); }
    @Override public IBlockState getStateFromMeta(int m) { return getDefaultState().withProperty(FLEURI, (m & 1) == 1); }
    @Override public int getMetaFromState(IBlockState s) { return s.getValue(FLEURI) ? 1 : 0; }

    @Override public boolean hasTileEntity(IBlockState s) { return true; }
    @Override public TileEntity createTileEntity(World w, IBlockState s) { return new TileHorloge(); }

    /** Les bateaux ne le cassent pas. */
    @Override
    public void onEntityCollision(World w, BlockPos pos, IBlockState s, Entity e) {}

    @Override
    public void updateTick(World w, BlockPos pos, IBlockState s, Random r) {
        super.updateTick(w, pos, s, r);
        if (s.getValue(FLEURI) || w.getBlockState(pos).getBlock() != this) return;
        TileEntity te = w.getTileEntity(pos);
        if (!(te instanceof TileHorloge) || ((TileHorloge) te).pret()) w.setBlockState(pos, s.withProperty(FLEURI, true), 2);
    }

    @Override
    public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float hx, float hy, float hz) {
        if (hand != EnumHand.MAIN_HAND) return false;
        if (w.isRemote) return true;
        TileEntity te = w.getTileEntity(pos);
        TileHorloge h = te instanceof TileHorloge ? (TileHorloge) te : null;
        if (!s.getValue(FLEURI)) {
            if (h == null || h.pret()) {
                w.setBlockState(pos, s.withProperty(FLEURI, true), 2);
            } else {
                p.sendStatusMessage(new TextComponentString(TextFormatting.GRAY + "" + TextFormatting.ITALIC + "Le lotus dort en bouton. Il refleurira dans "
                        + Fraicheur.duree(h.prochaine - System.currentTimeMillis()) + "."), true);
            }
            return true;
        }
        Item i = Objets.item("lotus_de_l_aube");
        if (i != null) spawnAsEntity(w, pos.up(), new ItemStack(i));
        w.setBlockState(pos, s.withProperty(FLEURI, false), 2);
        te = w.getTileEntity(pos);
        if (te instanceof TileHorloge) ((TileHorloge) te).attendre((long) (VirusConfig.lotusJours * JOUR));
        w.playSound(null, pos, SoundEvents.BLOCK_WATERLILY_PLACE, SoundCategory.BLOCKS, 1f, 1.4f);
        return true;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess w, BlockPos pos, IBlockState s, int fortune) {}

    @Override
    public ItemStack getPickBlock(IBlockState s, RayTraceResult t, World w, BlockPos pos, EntityPlayer p) {
        Item i = Objets.item("lotus_de_l_aube");
        return i == null ? ItemStack.EMPTY : new ItemStack(i);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(IBlockState s, World w, BlockPos pos, Random r) {
        if (s.getValue(FLEURI) && r.nextInt(3) == 0)
            w.spawnParticle(EnumParticleTypes.END_ROD, pos.getX() + 0.3 + r.nextDouble() * 0.4, pos.getY() + 0.3, pos.getZ() + 0.3 + r.nextDouble() * 0.4, 0, 0.01, 0);
    }
}
