package fr.lenerfvoeux.hxrp.metiers.virus.monde;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.data.Fraicheur;
import fr.lenerfvoeux.hxrp.metiers.virus.VirusConfig;
import fr.lenerfvoeux.hxrp.metiers.virus.item.Objets;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * Source de sève de l'Arbre-Monde : jamais générée, posée par les admins (événement, lieu sacré).
 * Une récolte par jour réel (réglable), pour qui la trouve.
 */
public class BlockSourceSeve extends Block {
    private static final long HEURE = 3_600_000L;

    public BlockSourceSeve() {
        super(Material.WOOD, MapColor.GOLD);
        setRegistryName(HxrpMetiers.MODID, "source_de_seve");
        setTranslationKey(HxrpMetiers.MODID + ".source_de_seve");
        setBlockUnbreakable();
        setResistance(6000f);
        setSoundType(SoundType.WOOD);
        setLightLevel(9 / 15f);
    }

    @Override public boolean hasTileEntity(IBlockState s) { return true; }
    @Override public TileEntity createTileEntity(World w, IBlockState s) { return new TileHorloge(); }

    @Override
    public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float hx, float hy, float hz) {
        if (hand != EnumHand.MAIN_HAND) return false;
        if (w.isRemote) return true;
        TileEntity te = w.getTileEntity(pos);
        if (!(te instanceof TileHorloge)) return true;
        TileHorloge h = (TileHorloge) te;
        if (!h.pret()) {
            p.sendStatusMessage(new TextComponentString(TextFormatting.GOLD + "" + TextFormatting.ITALIC + "La sève perle à peine. Elle coulera de nouveau dans "
                    + Fraicheur.duree(h.prochaine - System.currentTimeMillis()) + "."), true);
            return true;
        }
        Item i = Objets.item("seve_de_l_arbre_monde");
        if (i != null) spawnAsEntity(w, pos.offset(f), new ItemStack(i));
        h.attendre((long) (VirusConfig.seveHeures * HEURE));
        w.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1f, 0.7f);
        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(IBlockState s, World w, BlockPos pos, Random r) {
        if (r.nextInt(2) != 0) return;
        EnumFacing f = EnumFacing.Plane.HORIZONTAL.random(r);
        double x = pos.getX() + 0.5 + f.getXOffset() * 0.52, z = pos.getZ() + 0.5 + f.getZOffset() * 0.52;
        w.spawnParticle(EnumParticleTypes.DRIP_LAVA, x + (r.nextDouble() - 0.5) * 0.6 * (f.getXOffset() == 0 ? 1 : 0), pos.getY() + 0.2 + r.nextDouble() * 0.6,
                z + (r.nextDouble() - 0.5) * 0.6 * (f.getZOffset() == 0 ? 1 : 0), 0, 0, 0);
    }
}
