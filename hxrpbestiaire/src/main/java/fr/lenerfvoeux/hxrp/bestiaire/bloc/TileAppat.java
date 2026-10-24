package fr.lenerfvoeux.hxrp.bestiaire.bloc;

import fr.lenerfvoeux.hxrp.bestiaire.Espece;
import fr.lenerfvoeux.hxrp.bestiaire.HxrpBestiaire;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntityBoss;
import fr.lenerfvoeux.hxrp.bestiaire.stats.Stats;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;

/**
 * L'appât attend son boss : ses particules s'épaississent, puis, 10 s avant l'arrivée, un signe (message, son venu
 * de la direction d'arrivée : dans le dos du joueur le plus proche). Le boss apparaît après le délai du classeur
 * (onglet Appâts) ; s'il ne l'a pas dévoré deux minutes plus tard (tué ou retiré en route), l'appât se défait.
 */
public class TileAppat extends TileEntity implements ITickable {
    private static final int SIGNE = 200, EXTINCTION = 20 * 120;

    private int t, attente = -1;
    private boolean appele;
    private double cap = Double.NaN;

    private Espece espece() {
        Block b = world.getBlockState(pos).getBlock();
        return b instanceof BlockAppat ? ((BlockAppat) b).espece : null;
    }

    @Override
    public void update() {
        if (world.isRemote || !(world instanceof WorldServer)) return;
        Espece e = espece();
        if (e == null) return;
        WorldServer ws = (WorldServer) world;
        Stats.Boss s = Stats.boss(e.id);
        if (attente < 0) {
            attente = Math.max(1, (int) Math.round((s.appatDelaiMin + world.rand.nextDouble() * (s.appatDelaiMax - s.appatDelaiMin)) * 20));
            markDirty();
        }
        t++;
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        if (appele) {
            if (t > attente + EXTINCTION) {
                ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, x, y, z, 12, 0.3, 0.2, 0.3, 0.02);
                world.setBlockToAir(pos);
            }
            return;
        }
        float k = Math.min(1F, t / (float) attente);
        if (t % Math.max(3, Math.round(16 - 12 * k)) == 0)
            ws.spawnParticle(e.particuleAppat, x, y + 0.2, z, 1 + (int) (k * 4), 0.35, 0.3, 0.35, 0.02);
        if (t == Math.max(1, attente - SIGNE)) signe(ws, e, x, y, z);
        if (t >= attente) {
            if (Double.isNaN(cap)) cap = choisirCap(x, z);
            EntityBoss.attirer(e, world, pos, cap);
            appele = true;
            annoncer("§c§l" + e.nom + " arrive !");
            markDirty();
        }
        if (t % 100 == 0) markDirty();
    }

    private void signe(WorldServer ws, Espece e, double x, double y, double z) {
        if (Double.isNaN(cap)) cap = choisirCap(x, z);
        ws.spawnParticle(e.particuleAppat, x, y, z, 40, 0.6, 0.6, 0.6, 0.1);
        double d = e.arrivee == Espece.Arrivee.SUR_PLACE ? 0 : Stats.boss(e.id).appatDistanceMax * 0.6;
        world.playSound(null, x + Math.cos(cap) * d, y + (e.arrivee == Espece.Arrivee.VOL ? 15 : 0), z + Math.sin(cap) * d, e.sonAmbiant,
                SoundCategory.HOSTILE, 12F, e.hauteurSon * 0.8F);
        annoncer("§6" + e.signeAppat);
        markDirty();
    }

    /** Dans le dos du joueur le plus proche (on ne le voit pas arriver) ; au hasard sans joueur. */
    private double choisirCap(double x, double z) {
        EntityPlayer p = world.getClosestPlayer(x, pos.getY(), z, 96, false);
        if (p == null) return world.rand.nextDouble() * Math.PI * 2;
        float lacet = p.rotationYaw * (float) Math.PI / 180F;
        return Math.atan2(-MathHelper.cos(lacet), MathHelper.sin(lacet)) + (world.rand.nextDouble() - 0.5) * 1.2;
    }

    private void annoncer(String msg) {
        HxrpBestiaire.LOG.info("{} ({} {} {})", msg.replaceAll("§.", ""), pos.getX(), pos.getY(), pos.getZ());
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
