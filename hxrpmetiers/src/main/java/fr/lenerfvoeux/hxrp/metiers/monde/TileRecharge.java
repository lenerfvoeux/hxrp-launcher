package fr.lenerfvoeux.hxrp.metiers.monde;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * L'horloge d'un bloc récolté (voir Recharge) : l'heure réelle où sa recharge a commencé et sa durée.
 * Elle n'existe que tant que le bloc est vide : redevenu plein, le bloc la perd.
 */
public class TileRecharge extends TileEntity {
    private long debut, duree;

    public boolean demarree() {
        return duree > 0;
    }

    /** Lance la recharge (durée en ms), en comptant comme déjà faite la part « fait » (0 à 1). */
    public void demarrer(long duree, double fait) {
        this.duree = Math.max(1, duree);
        this.debut = System.currentTimeMillis() - (long) (this.duree * Math.max(0, Math.min(1, fait)));
        markDirty();
    }

    /** Avance la recharge d'une part de sa durée (poudre d'os). */
    public void avancer(double part) {
        debut -= (long) (duree * part);
        markDirty();
    }

    /** 0 juste après la récolte, 1 quand le bloc est de nouveau plein. */
    public double avancement() {
        if (duree <= 0) return 0;
        return Math.max(0, Math.min(1, (System.currentTimeMillis() - debut) / (double) duree));
    }

    public long reste() {
        return duree <= 0 ? 0 : Math.max(0, debut + duree - System.currentTimeMillis());
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound t) {
        super.writeToNBT(t);
        t.setLong("debut", debut);
        t.setLong("duree", duree);
        return t;
    }

    @Override
    public void readFromNBT(NBTTagCompound t) {
        super.readFromNBT(t);
        debut = t.getLong("debut");
        duree = t.getLong("duree");
    }

    /** Gardée d'un état vide à l'autre (stades), retirée quand le bloc change ou redevient plein. */
    @Override
    public boolean shouldRefresh(World w, BlockPos pos, IBlockState ancien, IBlockState nouveau) {
        return ancien.getBlock() != nouveau.getBlock() || !nouveau.getBlock().hasTileEntity(nouveau);
    }
}
