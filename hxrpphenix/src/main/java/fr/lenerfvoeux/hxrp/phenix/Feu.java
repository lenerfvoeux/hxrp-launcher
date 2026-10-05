package fr.lenerfvoeux.hxrp.phenix;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Le feu du Phénix : il ne touche que les joueurs (ni les blocs, ni les bêtes, ni les PNJ), n'allume rien,
 * et chaque coup fait monter la Brûlure du Hunter Virus quand hxrpmetiers est installé (appel par réflexion).
 */
public final class Feu {
    /** Un même joueur n'est pas touché plus d'une fois toutes les 10 ticks (temps d'invulnérabilité vanilla). */
    private static final Map<UUID, Long> DERNIER_COUP = new HashMap<>();

    private static Method brulure;
    private static boolean cherche;

    private Feu() {}

    public static DamageSource source(Entity phenix, Entity direct) {
        if (phenix == null) return new DamageSource("phenix_seul");
        if (direct == null || direct == phenix) return new EntityDamageSource("phenix", phenix);
        return new EntityDamageSourceIndirect("phenix", direct, phenix);
    }

    public static boolean cibleValable(EntityPlayer p) {
        return p != null && p.isEntityAlive() && !p.isSpectator() && !p.isCreative();
    }

    /** Touche un joueur : dégâts, quelques secondes à brûler, Brûlure du Virus. */
    public static boolean toucher(EntityPlayer p, Entity phenix, Entity direct, float degats, int secondesFeu) {
        if (!cibleValable(p) || p.world.isRemote) return false;
        long t = p.world.getTotalWorldTime();
        Long avant = DERNIER_COUP.get(p.getUniqueID());
        if (avant != null && t - avant < 10 && t >= avant) return false;
        DERNIER_COUP.put(p.getUniqueID(), t);
        if (DERNIER_COUP.size() > 256) DERNIER_COUP.clear();
        float d = (float) (degats * PhenixConfig.degats);
        boolean ok = d <= 0 || p.attackEntityFrom(source(phenix, direct), d);
        if (ok) {
            if (secondesFeu > 0) p.setFire(secondesFeu);
            brulure(p);
        }
        return ok;
    }

    /** Explosion de flammes : touche les joueurs dans le rayon et les repousse ; ne casse rien. */
    public static void explosion(World w, Entity phenix, Entity direct, double x, double y, double z, double rayon, float degats, boolean grosse) {
        if (w.isRemote) return;
        if (w instanceof WorldServer) {
            WorldServer ws = (WorldServer) w;
            int n = grosse ? 70 : 18;
            ws.spawnParticle(EnumParticleTypes.FLAME, x, y + 0.3, z, n, rayon * 0.3, 0.4, rayon * 0.3, 0.09);
            ws.spawnParticle(EnumParticleTypes.LAVA, x, y + 0.3, z, grosse ? 14 : 4, rayon * 0.25, 0.2, rayon * 0.25, 0.0);
            ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, x, y + 0.5, z, grosse ? 12 : 3, rayon * 0.25, 0.3, rayon * 0.25, 0.03);
            if (grosse) ws.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, x, y + 0.5, z, 1, 0, 0, 0, 0.0);
        }
        if (Sons.IMPACT != null)
            w.playSound(null, x, y, z, Sons.IMPACT, SoundCategory.HOSTILE, grosse ? 2.5F : 1.2F, 0.85F + w.rand.nextFloat() * 0.3F);
        AxisAlignedBB zone = new AxisAlignedBB(x - rayon, y - rayon, z - rayon, x + rayon, y + rayon, z + rayon);
        for (EntityPlayer p : w.getEntitiesWithinAABB(EntityPlayer.class, zone)) {
            double dx = p.posX - x, dy = p.posY + p.height * 0.5 - y, dz = p.posZ - z;
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d > rayon || !cibleValable(p)) continue;
            if (!vueDegagee(w, new Vec3d(x, y + 0.2, z), p)) continue;
            float k = (float) (1.0 - 0.5 * d / rayon);
            if (toucher(p, phenix, direct, degats * k, 2)) repousser(p, dx, dz, 0.5 * k, 0.25 * k);
        }
    }

    public static void repousser(EntityPlayer p, double dx, double dz, double force, double haut) {
        double h = Math.max(0.1, MathHelper.sqrt(dx * dx + dz * dz));
        p.motionX += dx / h * force;
        p.motionZ += dz / h * force;
        p.motionY = Math.max(p.motionY, haut);
        p.velocityChanged = true;
    }

    /** Rien de solide entre le feu et le joueur (on peut s'abriter derrière un mur). */
    public static boolean vueDegagee(World w, Vec3d de, EntityPlayer p) {
        Vec3d vers = new Vec3d(p.posX, p.posY + p.getEyeHeight() * 0.6, p.posZ);
        RayTraceResult r = w.rayTraceBlocks(de, vers, false, true, false);
        return r == null || r.typeOfHit != RayTraceResult.Type.BLOCK;
    }

    // ------------------------------------------------------------------ Hunter Virus (dépendance souple)
    public static boolean virusPresent() {
        chercher();
        return brulure != null;
    }

    private static void chercher() {
        if (cherche) return;
        cherche = true;
        if (!Loader.isModLoaded("hxrpmetiers")) return;
        try {
            brulure = Class.forName("fr.lenerfvoeux.hxrp.metiers.virus.VirusAPI").getMethod("brulure", EntityPlayer.class, int.class);
        } catch (Throwable t) {
            HxrpPhenix.LOG.info("Phénix : hxrpmetiers est là mais sans VirusAPI.brulure ({}), la Brûlure ne sera pas suivie", t.toString());
        }
    }

    /** La Brûlure du Hunter Virus monte d'un stade (le Virus limite lui-même la cadence). */
    public static void brulure(EntityPlayer p) {
        if (!PhenixConfig.brulureVirus) return;
        chercher();
        if (brulure == null) return;
        try {
            brulure.invoke(null, p, 1);
        } catch (Throwable t) {
            HxrpPhenix.LOG.warn("Phénix : appel à VirusAPI.brulure impossible, abandonné", t);
            brulure = null;
        }
    }
}
