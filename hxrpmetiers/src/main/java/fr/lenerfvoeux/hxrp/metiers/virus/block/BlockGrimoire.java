package fr.lenerfvoeux.hxrp.metiers.virus.block;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.block.BlockOriente;
import fr.lenerfvoeux.hxrp.metiers.virus.VirusSons;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Le Grimoire des maladies, ouvert sur son lutrin. Tout le monde peut le lire ; au-dessus du rang du lecteur,
 * les traitements sont à l'encre délavée.
 */
public class BlockGrimoire extends BlockOriente {
    public BlockGrimoire() {
        super(Material.WOOD, new double[]{2, 0, 4, 14, 13, 13});
        setRegistryName(HxrpMetiers.MODID, "grimoire_des_maladies");
        setTranslationKey(HxrpMetiers.MODID + ".grimoire_des_maladies");
        setHardness(2f);
        setSoundType(SoundType.WOOD);
    }

    @Override
    public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float x, float y, float z) {
        if (hand != EnumHand.MAIN_HAND) return false;
        if (w.isRemote) HxrpMetiers.proxy.ouvrirGrimoire();
        else w.playSound(null, pos, VirusSons.PAGE, SoundCategory.BLOCKS, 0.8f, 0.9f);
        return true;
    }
}
