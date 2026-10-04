package fr.lenerfvoeux.hxrp.metiers.virus.block;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.block.BlockOriente;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Machine;
import fr.lenerfvoeux.hxrp.metiers.virus.officine.Officine;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * Une machine de l'officine du Virus. Clic droit avec une préparation en cours : le mini-jeu de l'étape ;
 * à la table de préparation, main libre : le formulaire des préparations.
 */
public class BlockMachine extends BlockOriente {
    public final Machine machine;

    public BlockMachine(Machine m) {
        super(materiau(m), m.boite);
        this.machine = m;
        setRegistryName(HxrpMetiers.MODID, m.bloc);
        setTranslationKey(HxrpMetiers.MODID + "." + m.bloc);
        setHardness(2.5f);
        setResistance(8f);
        setSoundType(son(m));
        if (m.lumiere > 0) setLightLevel(m.lumiere / 15f);
    }

    private static Material materiau(Machine m) {
        switch (m) {
            case YAGEN: case CHAUDRON: case ALAMBIC: return Material.IRON;
            case JARRES: case MORTIER: return Material.ROCK;
            default: return Material.WOOD;
        }
    }

    private static SoundType son(Machine m) {
        switch (m) {
            case YAGEN: case CHAUDRON: case ALAMBIC: return SoundType.METAL;
            case JARRES: case MORTIER: return SoundType.STONE;
            default: return SoundType.WOOD;
        }
    }

    @Override
    public boolean onBlockActivated(World w, BlockPos pos, IBlockState st, EntityPlayer p, EnumHand hand, EnumFacing f, float x, float y, float z) {
        if (hand != EnumHand.MAIN_HAND) return false;
        if (!w.isRemote) Officine.utiliser((EntityPlayerMP) p, machine, pos);
        return true;
    }

    /** Le brasero rougeoie et fume, l'alambic laisse échapper un filet de vapeur. */
    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(IBlockState s, World w, BlockPos pos, Random r) {
        double x = pos.getX() + 0.5, y = pos.getY(), z = pos.getZ() + 0.5;
        if (machine == Machine.CHAUDRON) {
            if (r.nextInt(3) == 0) w.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x + (r.nextDouble() - 0.5) * 0.5, y + 0.95, z + (r.nextDouble() - 0.5) * 0.5, 0, 0.03, 0);
            if (r.nextInt(5) == 0) w.spawnParticle(EnumParticleTypes.FLAME, x + (r.nextDouble() - 0.5) * 0.4, y + 0.22, z + (r.nextDouble() - 0.5) * 0.4, 0, 0.005, 0);
        } else if (machine == Machine.ALAMBIC && r.nextInt(4) == 0) {
            w.spawnParticle(EnumParticleTypes.CLOUD, x + (r.nextDouble() - 0.5) * 0.2, y + 1.02, z + (r.nextDouble() - 0.5) * 0.2, 0, 0.02, 0);
        }
    }
}
