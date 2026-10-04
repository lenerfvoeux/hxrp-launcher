package fr.lenerfvoeux.hxrp.metiers.virus.monde;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Une date réelle attachée à un bloc : quand le lotus refleurit, quand la sève coule de nouveau. */
public class TileHorloge extends TileEntity {
    public long prochaine;

    public boolean pret() {
        return System.currentTimeMillis() >= prochaine;
    }

    public void attendre(long ms) {
        prochaine = System.currentTimeMillis() + Math.max(0, ms);
        markDirty();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound t) {
        super.writeToNBT(t);
        t.setLong("prochaine", prochaine);
        return t;
    }

    @Override
    public void readFromNBT(NBTTagCompound t) {
        super.readFromNBT(t);
        prochaine = t.getLong("prochaine");
    }

    @Override
    public boolean shouldRefresh(World w, BlockPos p, IBlockState ancien, IBlockState nouveau) {
        return ancien.getBlock() != nouveau.getBlock();
    }
}
