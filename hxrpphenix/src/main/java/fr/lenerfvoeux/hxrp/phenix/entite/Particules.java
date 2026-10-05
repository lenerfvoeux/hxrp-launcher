package fr.lenerfvoeux.hxrp.phenix.entite;

import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Particules du Phénix, lancées côté client à partir de l'état synchronisé (aucun paquet par particule) :
 * traînée de braises, souffle ardent, zone de la pluie, anneau de feu, tornade, œuf de cendres, cendres de la mort.
 */
final class Particules {
    private Particules() {}

    static void phenix(EntityPhenix e, int etat, int t) {
        World w = e.world;
        Random r = e.getRNG();
        double a = e.renderYawOffset * Math.PI / 180.0;
        double fx = -Math.sin(a), fz = Math.cos(a);          // avant
        double gx = Math.cos(a), gz = Math.sin(a);           // flanc gauche
        double x = e.posX, y = e.posY, z = e.posZ;

        if (e.deathTime > 0) {
            for (int i = 0; i < 6; i++) {
                double px = x + (r.nextDouble() - 0.5) * 3, py = y + r.nextDouble() * 2.2, pz = z + (r.nextDouble() - 0.5) * 3;
                w.spawnParticle(EnumParticleTypes.SMOKE_LARGE, px, py, pz, 0, 0.04, 0);
                w.spawnParticle(EnumParticleTypes.REDSTONE, px, py, pz, 0.35, 0.33, 0.32);
            }
            if (e.deathTime < 20) w.spawnParticle(EnumParticleTypes.FLAME, x + (r.nextDouble() - 0.5) * 2, y + 1 + r.nextDouble(), z + (r.nextDouble() - 0.5) * 2, 0, 0.05, 0);
            return;
        }

        if (etat == EntityPhenix.OEUF) {
            float presse = Math.min(1F, t / 200F);
            if (r.nextInt(3) == 0) w.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x + (r.nextDouble() - 0.5) * 0.6, y + 1.35, z + (r.nextDouble() - 0.5) * 0.6, 0, 0.05, 0);
            if (r.nextFloat() < 0.25F + presse * 0.6F) {
                double an = r.nextDouble() * Math.PI * 2;
                w.spawnParticle(EnumParticleTypes.FLAME, x + Math.cos(an) * 0.45, y + 0.2 + r.nextDouble() * 1.1, z + Math.sin(an) * 0.45, Math.cos(an) * 0.02, 0.03, Math.sin(an) * 0.02);
            }
            if (r.nextInt(12) == 0) w.spawnParticle(EnumParticleTypes.LAVA, x, y + 1.2, z, 0, 0, 0);
            return;
        }

        if (etat == EntityPhenix.RENAISSANCE && t < 12) {
            for (int i = 0; i < 26; i++) {
                double u = r.nextDouble() * Math.PI * 2, v = Math.acos(2 * r.nextDouble() - 1), s = 0.25 + r.nextDouble() * 0.35;
                double vx = Math.sin(v) * Math.cos(u) * s, vy = Math.cos(v) * s, vz = Math.sin(v) * Math.sin(u) * s;
                w.spawnParticle(EnumParticleTypes.FLAME, x, y + 1.2, z, vx, vy, vz);
            }
        }

        // traînée de braises : bout des ailes, queue, corps
        boolean pose = etat == EntityPhenix.PERCHE || etat == EntityPhenix.IMPACT;
        int n = pose ? 1 : (e.embrase() ? 4 : 2);
        for (int i = 0; i < n; i++) {
            double k = r.nextDouble();
            double px, py, pz;
            switch (r.nextInt(pose ? 2 : 4)) {
                case 0:                      // corps
                    px = x + (r.nextDouble() - 0.5) * 1.2;
                    py = y + 1.2 + r.nextDouble() * 0.8;
                    pz = z + (r.nextDouble() - 0.5) * 1.2;
                    break;
                case 1:                      // queue
                    px = x - fx * (1.5 + k * 2.2);
                    py = y + 1.55;
                    pz = z - fz * (1.5 + k * 2.2);
                    break;
                default:                     // ailes
                    double c = (r.nextBoolean() ? 1 : -1) * (1.5 + k * 1.9);
                    px = x + gx * c - fx * 0.4;
                    py = y + 1.75 + (r.nextDouble() - 0.5) * 0.8;
                    pz = z + gz * c - fz * 0.4;
            }
            w.spawnParticle(EnumParticleTypes.FLAME, px, py, pz, (r.nextDouble() - 0.5) * 0.02, 0.02, (r.nextDouble() - 0.5) * 0.02);
            if (r.nextInt(3) == 0) w.spawnParticle(EnumParticleTypes.REDSTONE, px, py, pz, 1.0, 0.45 + r.nextDouble() * 0.35, 0.05);
        }
        if (!pose && r.nextInt(e.embrase() ? 4 : 9) == 0)
            w.spawnParticle(EnumParticleTypes.LAVA, x, y + 1.3, z, 0, 0, 0);
        if (e.embrase() && r.nextInt(3) == 0)
            w.spawnParticle(EnumParticleTypes.SMOKE_LARGE, x + (r.nextDouble() - 0.5), y + 2.2, z + (r.nextDouble() - 0.5), 0, 0.05, 0);

        switch (etat) {
            case EntityPhenix.SOUFFLE: souffle(e, w, r, t); break;
            case EntityPhenix.BOULE:
                if (t > 6 && t < 16) aspirer(e, w, r, 4);
                break;
            case EntityPhenix.PLUIE: pluie(e, w, r, t); break;
            case EntityPhenix.PLONGEE:
                if (t > 25) {
                    for (int i = 0; i < 6; i++)
                        w.spawnParticle(EnumParticleTypes.FLAME, x + (r.nextDouble() - 0.5) * 1.6, y + 0.6 + r.nextDouble() * 1.4, z + (r.nextDouble() - 0.5) * 1.6, 0, 0.02, 0);
                    w.spawnParticle(EnumParticleTypes.SMOKE_LARGE, x, y + 1.4, z, 0, 0, 0);
                    cercle(w, r, e.zone(), 2.4 - Math.min(1.6, (t - 25) * 0.05), 6, EnumParticleTypes.FLAME, 0.0);
                }
                break;
            case EntityPhenix.IMPACT:
                if (t <= 22) {
                    double rr = 1.0 + t * 0.42;
                    cercle(w, r, e.zone(), rr, 30, EnumParticleTypes.FLAME, 0.04);
                    cercle(w, r, e.zone(), rr, 4, EnumParticleTypes.LAVA, 0.0);
                    if (t % 2 == 0) cercle(w, r, e.zone(), rr - 0.3, 6, EnumParticleTypes.SMOKE_LARGE, 0.02);
                }
                break;
            case EntityPhenix.TEMPETE: tempete(e, w, r, t); break;
            default:
        }
    }

    /** Les flammes convergent vers le bec : il prend son souffle. */
    private static void aspirer(EntityPhenix e, World w, Random r, int n) {
        Vec3d b = e.bec();
        for (int i = 0; i < n; i++) {
            double ox = (r.nextDouble() - 0.5) * 3, oy = (r.nextDouble() - 0.5) * 2, oz = (r.nextDouble() - 0.5) * 3;
            w.spawnParticle(EnumParticleTypes.FLAME, b.x + ox, b.y + oy, b.z + oz, -ox * 0.12, -oy * 0.12, -oz * 0.12);
        }
    }

    private static void souffle(EntityPhenix e, World w, Random r, int t) {
        if (t < EntityPhenix.SOUFFLE_DEBUT) {
            aspirer(e, w, r, 5);
            return;
        }
        if (t >= EntityPhenix.SOUFFLE_FIN) return;
        Vec3d b = e.bec(), d = e.getLook(1.0F);
        for (int i = 0; i < 12; i++) {
            double s = 0.65 + r.nextDouble() * 0.45;
            double vx = d.x + (r.nextDouble() - 0.5) * 0.42, vy = d.y + (r.nextDouble() - 0.5) * 0.42, vz = d.z + (r.nextDouble() - 0.5) * 0.42;
            double l = Math.sqrt(vx * vx + vy * vy + vz * vz);
            w.spawnParticle(EnumParticleTypes.FLAME, b.x, b.y, b.z, vx / l * s, vy / l * s, vz / l * s);
        }
        if (r.nextInt(2) == 0) w.spawnParticle(EnumParticleTypes.SMOKE_LARGE, b.x + d.x * 4, b.y + d.y * 4, b.z + d.z * 4, d.x * 0.2, d.y * 0.2 + 0.05, d.z * 0.2);
        if (r.nextInt(4) == 0) w.spawnParticle(EnumParticleTypes.LAVA, b.x + d.x * 3, b.y + d.y * 3, b.z + d.z * 3, 0, 0, 0);
    }

    private static void pluie(EntityPhenix e, World w, Random r, int t) {
        Vec3d z = e.zone();
        double rayon = e.embrase() ? 5.0 : 4.0;
        boolean alerte = t < EntityPhenix.PLUIE_DEBUT;
        cercle(w, r, z, rayon, alerte ? 14 : 5, EnumParticleTypes.FLAME, 0.01);
        if (alerte) {
            for (int i = 0; i < 3; i++) {
                double an = r.nextDouble() * Math.PI * 2, rr = Math.sqrt(r.nextDouble()) * rayon;
                w.spawnParticle(EnumParticleTypes.REDSTONE, z.x + Math.cos(an) * rr, z.y + 0.15, z.z + Math.sin(an) * rr, 1.0, 0.3, 0.02);
            }
        } else if (t < EntityPhenix.PLUIE_FIN) {
            for (int i = 0; i < 3; i++) {
                double an = r.nextDouble() * Math.PI * 2, rr = Math.sqrt(r.nextDouble()) * rayon;
                w.spawnParticle(EnumParticleTypes.DRIP_LAVA, z.x + Math.cos(an) * rr, z.y + 8 + r.nextDouble() * 4, z.z + Math.sin(an) * rr, 0, 0, 0);
            }
        }
    }

    private static void tempete(EntityPhenix e, World w, Random r, int t) {
        if (t < EntityPhenix.TORNADE_DEBUT || t >= EntityPhenix.TORNADE_FIN) {
            if (t < EntityPhenix.TORNADE_DEBUT) aspirer(e, w, r, 3);
            return;
        }
        Vec3d c = e.zone();
        double th = e.angleTornade() + (t - EntityPhenix.TORNADE_DEBUT) * EntityPhenix.TORNADE_VITESSE;
        double tx = c.x + Math.cos(th) * EntityPhenix.TORNADE_RAYON, tz = c.z + Math.sin(th) * EntityPhenix.TORNADE_RAYON;
        // colonne de flammes en spirale qui s'évase vers le haut
        for (int k = 0; k < 16; k++) {
            double h = k * 0.7, an = t * 0.55 + k * 0.62, rad = 0.45 + h * 0.2;
            double px = tx + Math.cos(an) * rad, pz = tz + Math.sin(an) * rad;
            w.spawnParticle(EnumParticleTypes.FLAME, px, c.y + h, pz, -Math.sin(an) * 0.12, 0.06, Math.cos(an) * 0.12);
            if (k % 4 == 0) w.spawnParticle(EnumParticleTypes.REDSTONE, px, c.y + h, pz, 1.0, 0.55, 0.05);
        }
        w.spawnParticle(EnumParticleTypes.SMOKE_LARGE, tx, c.y + 11, tz, 0, 0.08, 0);
        if (r.nextInt(2) == 0) w.spawnParticle(EnumParticleTypes.LAVA, tx, c.y + 0.3, tz, 0, 0, 0);
        // les braises sont aspirées vers le centre de l'arène, en spirale
        for (int i = 0; i < 5; i++) {
            double an = r.nextDouble() * Math.PI * 2, rr = 6 + r.nextDouble() * 16;
            double px = c.x + Math.cos(an) * rr, pz = c.z + Math.sin(an) * rr;
            double vx = (c.x - px) * 0.05 - Math.sin(an) * 0.25, vz = (c.z - pz) * 0.05 + Math.cos(an) * 0.25;
            w.spawnParticle(EnumParticleTypes.FLAME, px, c.y + 0.3 + r.nextDouble() * 2, pz, vx, 0.01, vz);
        }
        cercle(w, r, c, 2.6, 6, EnumParticleTypes.FLAME, 0.05);
    }

    private static void cercle(World w, Random r, Vec3d c, double rayon, int n, EnumParticleTypes type, double monte) {
        for (int i = 0; i < n; i++) {
            double an = r.nextDouble() * Math.PI * 2;
            w.spawnParticle(type, c.x + Math.cos(an) * rayon, c.y + 0.12, c.z + Math.sin(an) * rayon, 0, monte, 0);
        }
    }

    static float lerp(float a, float b, float t) {
        return a + MathHelper.wrapDegrees(b - a) * t;
    }
}
