package fr.lenerfvoeux.hxrp.phenix.bloc;

import fr.lenerfvoeux.hxrp.phenix.HxrpPhenix;
import fr.lenerfvoeux.hxrp.phenix.Sons;
import fr.lenerfvoeux.hxrp.phenix.entite.EntityPhenix;
import fr.lenerfvoeux.hxrp.phenix.stats.StatsPhenix;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;

/**
 * Le nid de braises attend le Phénix : il fume de plus en plus fort, puis, 10 s avant l'arrivée, un cri perçant
 * vient du côté d'où le Phénix surgira (dans le dos du joueur le plus proche). Le Phénix apparaît entre 40 et
 * 60 blocs (classeur), haut dans le ciel, et descend en tournoyant jusqu'au nid. S'il ne l'a pas dévoré deux
 * minutes plus tard (tué ou retiré en route), le nid s'éteint.
 */
public class TileNidDeBraises extends TileEntity implements ITickable {
    /** Ticks avant l'arrivée où le cri retentit ; ticks après l'appel où un nid oublié s'éteint. */
    private static final int SIGNE = 200, EXTINCTION = 20 * 120;

    private int t, attente = -1;
    private boolean appele;
    /** Direction (radians, depuis le nid) d'où viendra le Phénix ; choisie au moment du cri. */
    private double cap = Double.NaN;

    @Override
    public void update() {
        if (world.isRemote || !(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;
        if (attente < 0) {
            double min = StatsPhenix.appatDelaiMin, max = StatsPhenix.appatDelaiMax;
            attente = Math.max(1, (int) Math.round((min + world.rand.nextDouble() * (max - min)) * 20));
            markDirty();
        }
        t++;
        double x = pos.getX() + 0.5, y = pos.getY() + 0.4, z = pos.getZ() + 0.5;
        if (appele) {
            if (t > attente + EXTINCTION) {
                ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, x, y, z, 12, 0.3, 0.2, 0.3, 0.02);
                world.setBlockToAir(pos);
            }
            return;
        }
        // la fumée s'épaissit à mesure que le Phénix approche
        float k = Math.min(1F, t / (float) attente);
        if (t % Math.max(3, Math.round(16 - 12 * k)) == 0)
            ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, x, y + 0.3, z, 1 + (int) (k * 3), 0.12, 0.25, 0.12, 0.03);
        if (t % 8 == 0) ws.spawnParticle(EnumParticleTypes.FLAME, x, y, z, 1 + (int) (k * 5), 0.25, 0.08, 0.25, 0.015);
        if (t == Math.max(1, attente - SIGNE)) signe(ws, x, y, z);
        if (t >= attente) appeler(x, z);
        if (t % 100 == 0) markDirty();
    }

    /** Le signe : une gerbe d'étincelles jaillit du nid et un cri lointain vient du côté d'où le Phénix arrivera. */
    private void signe(WorldServer ws, double x, double y, double z) {
        if (Double.isNaN(cap)) cap = choisirCap(x, z);
        ws.spawnParticle(EnumParticleTypes.FLAME, x, y, z, 40, 0.1, 0.1, 0.1, 0.25);
        ws.spawnParticle(EnumParticleTypes.LAVA, x, y, z, 10, 0.2, 0.1, 0.2, 0.0);
        double d = StatsPhenix.appatDistanceMax;
        world.playSound(null, x + Math.cos(cap) * d, y + 25, z + Math.sin(cap) * d, Sons.CRI, SoundCategory.HOSTILE, 14F, 0.75F);
        annoncer("§6Un cri perçant déchire le ciel au loin… §7le nid de braises s'embrase.");
        markDirty();
    }

    /** Dans le dos du joueur le plus proche (on ne le voit pas arriver) ; au hasard sans joueur. */
    private double choisirCap(double x, double z) {
        EntityPlayer p = world.getClosestPlayer(x, pos.getY(), z, 96, false);
        if (p == null) return world.rand.nextDouble() * Math.PI * 2;
        float lacet = p.rotationYaw * (float) Math.PI / 180F;
        return Math.atan2(-MathHelper.cos(lacet), MathHelper.sin(lacet)) + (world.rand.nextDouble() - 0.5) * 1.2;
    }

    private void appeler(double x, double z) {
        if (Double.isNaN(cap)) cap = choisirCap(x, z);
        double[] o = depart(x, z);
        EntityPhenix.attirer(world, o[0], o[1], o[2], pos);
        appele = true;
        markDirty();
        annoncer("§c§lLe Phénix de feu fond sur le nid de braises !");
    }

    /**
     * Point d'apparition : à la distance du classeur, dans la direction choisie, haut au-dessus du relief ; il faut que
     * le terrain soit chargé (sinon le Phénix resterait figé), quitte à se rapprocher ou à apparaître au-dessus du nid.
     */
    private double[] depart(double x, double z) {
        double min = StatsPhenix.appatDistanceMin, max = StatsPhenix.appatDistanceMax;
        double d0 = min + world.rand.nextDouble() * (max - min);
        int plafond = world.getHeight() - 8;
        for (double d = d0; d >= 12; d -= 8) {
            for (int essai = 0; essai < 5; essai++) {
                double an = cap + (essai == 0 ? 0 : (world.rand.nextDouble() - 0.5) * 1.6);
                BlockPos b = new BlockPos(x + Math.cos(an) * d, 64, z + Math.sin(an) * d);
                if (!world.isAreaLoaded(b, 32)) continue;
                double y = Math.max(pos.getY() + 28 + world.rand.nextDouble() * 6, world.getHeight(b).getY() + 12);
                return new double[]{b.getX() + 0.5, Math.min(y, plafond), b.getZ() + 0.5};
            }
        }
        return new double[]{x, Math.min(pos.getY() + 30, plafond), z};
    }

    private void annoncer(String msg) {
        HxrpPhenix.LOG.info("{} ({} {} {})", msg.replaceAll("§.", ""), pos.getX(), pos.getY(), pos.getZ());
        for (EntityPlayer p : world.playerEntities)
            if (p.getDistanceSq(pos) < 96 * 96) p.sendMessage(new TextComponentString(msg));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound c) {
        super.writeToNBT(c);
        c.setInteger("Temps", t);
        c.setInteger("Attente", attente);
        c.setBoolean("Appele", appele);
        if (!Double.isNaN(cap)) c.setDouble("Cap", cap);
        return c;
    }

    @Override
    public void readFromNBT(NBTTagCompound c) {
        super.readFromNBT(c);
        t = c.getInteger("Temps");
        attente = c.hasKey("Attente") ? c.getInteger("Attente") : -1;
        appele = c.getBoolean("Appele");
        cap = c.hasKey("Cap") ? c.getDouble("Cap") : Double.NaN;
    }
}
