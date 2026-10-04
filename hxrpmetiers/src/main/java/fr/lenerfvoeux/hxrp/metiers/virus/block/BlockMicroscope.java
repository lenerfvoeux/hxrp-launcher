package fr.lenerfvoeux.hxrp.metiers.virus.block;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.block.BlockOriente;
import fr.lenerfvoeux.hxrp.metiers.virus.officine.Diagnostic;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Microscope d'analyse en laiton : on y analyse le sang d'une seringue pleine. */
public class BlockMicroscope extends BlockOriente {
    public BlockMicroscope() {
        super(Material.IRON, new double[]{4, 0, 4, 12, 15, 12});
        setRegistryName(HxrpMetiers.MODID, "microscope_d_analyse");
        setTranslationKey(HxrpMetiers.MODID + ".microscope_d_analyse");
        setHardness(2f);
        setSoundType(SoundType.METAL);
    }

    @Override
    public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float x, float y, float z) {
        if (hand != EnumHand.MAIN_HAND) return false;
        if (!w.isRemote) Diagnostic.microscope((EntityPlayerMP) p, pos);
        return true;
    }
}
