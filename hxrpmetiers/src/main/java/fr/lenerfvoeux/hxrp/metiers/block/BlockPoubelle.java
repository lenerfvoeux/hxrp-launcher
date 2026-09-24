package fr.lenerfvoeux.hxrp.metiers.block;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Poubelle : ce qu'on y dépose disparaît quand on la referme. */
public class BlockPoubelle extends Block {
    public BlockPoubelle() {
        super(Material.IRON);
        setRegistryName(HxrpMetiers.MODID, "poubelle");
        setTranslationKey(HxrpMetiers.MODID + ".poubelle");
        setHardness(2.0f);
        setSoundType(SoundType.METAL);
        setCreativeTab(ModRegistry.TAB_INGREDIENTS);
    }

    @Override
    public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float x, float y, float z) {
        if (!w.isRemote) p.openGui(HxrpMetiers.instance, GuiHandler.POUBELLE, w, pos.getX(), pos.getY(), pos.getZ());
        return true;
    }
}
