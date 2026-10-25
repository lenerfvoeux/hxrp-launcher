package fr.lenerfvoeux.hxrp.bestiaire.entite.boss;

import fr.lenerfvoeux.hxrp.bestiaire.Especes;
import fr.lenerfvoeux.hxrp.bestiaire.Sons;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntityBoss;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntityProjectileBoss;
import fr.lenerfvoeux.hxrp.bestiaire.stats.Stats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Démon de lave (lac de lave). Un colosse de roche en fusion qui ne quitte jamais la lave : il se tient dans le lac, la
 * lave aux genoux. Rayon de feu qui suit un joueur, plongée (il disparaît et ressort ailleurs dans le lac), coup de
 * poing pour ceux qui s'approchent du bord, balayage du bras qui projette. En phase 2 : double rayon qui balaie
 * l'arène, rugissement volcanique (la lave pleut autour du lac), jets de magma qui laissent des flaques de feu.
 * Immunisé au feu. Les flaques de feu ne sont pas de vrais feux : elles ne brûlent aucun bloc et s'éteignent seules.
 */
public class EntityDemonDeLave extends EntityBoss {
    private static final List<Coup> COUPS = Arrays.asList(
            new Coup("rayon_feu", "rayon", Portee.DISTANCE),
            new Coup("plongee_lave", "plongee", Portee.PARTOUT).duree(150),
            new Coup("coup_poing", "coup_poing", Portee.CONTACT, 0.5),
            new Coup("balayage", "balayage", Portee.CONTACT, 0.45),
            new Coup("double_rayon", "double_rayon", Portee.DISTANCE),
            new Coup("pluie_lave", "rugissement", Portee.PARTOUT),
            new Coup("jets_magma", "jets_magma", Portee.DISTANCE, 0.35, 0.55, 0.75));

    /** Rayons : fenêtre où le rayon brûle (part de l'animation). */
    private static final double RAYON_DEBUT = 0.42, RAYON_FIN = 0.9;

    /** Flaques de feu laissées par les jets de magma : centre, rayon, fin (tick). */
    private static final class Flaque {
        final Vec3d c;
        final double r;
        final int fin;

        Flaque(Vec3d c, double r, int fin) {
            this.c = c;
            this.r = r;
            this.fin = fin;
        }
    }

    private final List<Flaque> flaques = new ArrayList<>();
    private Vec3d visee;

    public EntityDemonDeLave(World w) {
        super(w, Especes.DEMON_DE_LAVE);
        isImmuneToFire = true;
    }

    @Override
    protected List<Coup> coups() {
        return COUPS;
    }

    @Override
    protected boolean peut(Coup c, double d) {
        if (cible == null) return false;
        switch (c.id) {
            case "rayon_feu":
            case "double_rayon":
            case "jets_magma":
                return d > 6;
            default:
                return true;
        }
    }

    @Override
    protected void finCoup() {
        super.finCoup();
        if (mode() == CACHE) mode(NORMAL);
    }

    @Override
    protected void debutCoup(Coup c) {
        super.debutCoup(c);
        visee = visee(20).add(0, 0.8, 0);
        if ("pluie_lave".equals(c.id)) son(Sons.RUGISSEMENT_LAVE, 6F, 0.6F);
    }

    @Override
    protected void impact(Coup c, int n) {
        switch (c.id) {
            case "balayage":
                frapperDevant(c);
                break;
            case "jets_magma":
                jet(c);
                break;
            case "coup_poing":
                super.impact(c, n);
                break;
            default:
        }
    }

    @Override
    protected void executer(Coup c, int t) {
        switch (c.id) {
            case "rayon_feu":
                rayonFeu(c, t, false);
                break;
            case "double_rayon":
                rayonFeu(c, t, true);
                break;
            case "plongee_lave":
                plongee(t);
                break;
            case "pluie_lave":
                pluie(c, t);
                break;
            default:
                if (cible != null) regarder(cible.posX, cible.posZ, 10F);
        }
    }

    // ================================================================== rayons
    /** Rayon de feu des yeux : il suit lentement sa cible ; le double rayon balaie l'arène de part et d'autre. */
    private void rayonFeu(Coup c, int t, boolean double_) {
        int debut = (int) (dureeCoup * RAYON_DEBUT), fin = (int) (dureeCoup * RAYON_FIN);
        if (cible != null) {
            regarder(cible.posX, cible.posZ, double_ ? 1.5F : 2.5F);
            visee = new Vec3d(cible.posX, cible.posY + 0.8, cible.posZ);
        }
        if (t < debut || t > fin || t % 4 != 0) return;
        Vec3d o = yeux(), v = visee.subtract(o);
        double largeur = Math.max(1, rayon(c.id)) / 2;
        if (!double_) {
            Vec3d b = rayon(c, o, v, portee(c.id), largeur, 0.25F);
            zone(b.x, b.y, b.z, 1);
        } else {
            // deux rayons qui partent écartés et se croisent devant lui, puis s'écartent à nouveau
            double k = Math.cos(Math.PI * 2 * (t - debut) / Math.max(1, fin - debut)) * 45;
            Vec3d b1 = rayon(c, o, tourne(v, k), portee(c.id), largeur, 0.25F);
            Vec3d b2 = rayon(c, o, tourne(v, -k), portee(c.id), largeur, 0.25F);
            zone(b1.x, b1.y, b1.z, 1);
            zone2(b2);
        }
        if (t % 8 == 0) son(SoundEvents.ENTITY_BLAZE_SHOOT, 3F, 0.5F);
    }

    private static Vec3d tourne(Vec3d v, double deg) {
        double a = Math.toRadians(deg), c = Math.cos(a), s = Math.sin(a);
        return new Vec3d(v.x * c - v.z * s, v.y, v.x * s + v.z * c);
    }

    private Vec3d yeux() {
        Vec3d f = devant();
        return new Vec3d(posX + f.x * 1.5, posY + height * 0.82, posZ + f.z * 1.5);
    }

    // ================================================================== plongée
    /** Il s'enfonce dans la lave, file dessous et ressort ailleurs dans le lac, face à sa cible. */
    private void plongee(int t) {
        motionX = motionZ = 0;
        int sous = duree("plongee", 80) - 10;
        if (t == sous) {
            mode(CACHE);
            son(SoundEvents.BLOCK_LAVA_POP, 4F, 0.5F);
        }
        if (t == sous + 20) {
            Vec3d p = pointDansLaLave();
            if (p != null) setPositionAndUpdate(p.x, p.y, p.z);
            if (cible != null) regarder(cible.posX, cible.posZ, 180F);
            anim("remontee");
            son(SoundEvents.BLOCK_LAVA_EXTINGUISH, 4F, 0.5F);
        }
        if (t == sous + 20 + 30) {
            mode(NORMAL);
            dureeCoup = Math.min(dureeCoup, t + 1);
        }
    }

    /** Un point du lac (lave sous lui), dans l'arène, du côté de sa cible si possible. */
    private Vec3d pointDansLaLave() {
        Vec3d c = cible != null ? new Vec3d(cible.posX, cible.posY, cible.posZ) : new Vec3d(ax, ay, az);
        for (int essai = 0; essai < 20; essai++) {
            double an = rand.nextDouble() * Math.PI * 2, d = 6 + rand.nextDouble() * 12;
            double x = c.x + Math.cos(an) * d, z = c.z + Math.sin(an) * d;
            if ((x - ax) * (x - ax) + (z - az) * (z - az) > st().rayonArene * st().rayonArene) continue;
            double s = surface(x, posY, z);
            if (!Double.isNaN(s)) return new Vec3d(x, s + espece.flottaison, z);
        }
        return null;
    }

    /** Il surgit dans la lave la plus proche de l'idole (8 blocs), sinon sur l'idole elle-même. */
    @Override
    protected Vec3d pointDeSurgissement(BlockPos appat) {
        for (int r = 1; r <= 8; r++)
            for (int k = 0; k < 16; k++) {
                double an = k * Math.PI / 8, x = appat.getX() + 0.5 + Math.cos(an) * r, z = appat.getZ() + 0.5 + Math.sin(an) * r;
                double s = surface(x, appat.getY(), z);
                if (!Double.isNaN(s) && Math.abs(s - appat.getY()) <= 3) return new Vec3d(x, s + espece.flottaison, z);
            }
        return super.pointDeSurgissement(appat);
    }

    // ================================================================== magma
    /** Rugissement volcanique : des gouttes de lave retombent tout autour du lac pendant qu'il rugit. */
    private void pluie(Coup c, int t) {
        if (cible != null) regarder(cible.posX, cible.posZ, 5F);
        if (t < dureeCoup * 0.2 || t > dureeCoup * 0.85 || t % 3 != 0) return;
        Stats.Attaque a = st().attaque(c.id);
        double r = Math.max(6, rayon(c.id));
        // une goutte sur deux tombe près de sa cible, les autres au hasard autour du lac
        boolean surCible = cible != null && rand.nextBoolean();
        Vec3d centre = surCible ? new Vec3d(cible.posX, cible.posY, cible.posZ) : new Vec3d(posX, posY, posZ);
        double an = rand.nextDouble() * Math.PI * 2, d = rand.nextDouble() * (surCible ? 6 : r);
        double x = centre.x + Math.cos(an) * d, z = centre.z + Math.sin(an) * d;
        EntityProjectileBoss p = new EntityProjectileBoss(world, this, EntityProjectileBoss.Type.MAGMA, degats(c.id), a.effet, a.dureeEffet)
                .depuis(x, Math.max(posY + height, sol(x, z)) + 14, z).tomber();
        world.spawnEntity(p);
    }

    /** Jet de magma : une boule lancée en cloche vers sa cible ; là où elle tombe, une flaque de feu (effet du classeur). */
    private void jet(Coup c) {
        Stats.Attaque a = st().attaque(c.id);
        Vec3d f = devant();
        EntityProjectileBoss p = new EntityProjectileBoss(world, this, EntityProjectileBoss.Type.MAGMA, degats(c.id), "feu", a.dureeEffet)
                .depuis(posX + f.x * 2 + f.z * 2, posY + height * 0.75, posZ + f.z * 2 - f.x * 2);
        Vec3d v = visee(14);
        p.viser(v.add((rand.nextDouble() - 0.5) * 4, 0, (rand.nextDouble() - 0.5) * 4), 1.3F, 0F);
        world.spawnEntity(p);
        son(SoundEvents.ENTITY_GHAST_SHOOT, 3F, 0.5F);
    }

    /** Un jet de magma laisse une flaque de feu là où il retombe (la pluie de lave, non). */
    @Override
    public void surImpact(EntityProjectileBoss p, Vec3d ou) {
        if (p.type() != EntityProjectileBoss.Type.MAGMA || coup == null || !"jets_magma".equals(coup.id)) return;
        Stats.Attaque a = st().attaque("jets_magma");
        flaques.add(new Flaque(new Vec3d(ou.x, sol(ou.x, ou.z), ou.z), Math.max(2, rayon("jets_magma")), ticksExisted + (int) (a.dureeEffet * 20)));
    }

    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        if (flaques.isEmpty() || !(world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) world;
        for (Iterator<Flaque> it = flaques.iterator(); it.hasNext(); ) {
            Flaque f = it.next();
            if (ticksExisted >= f.fin) {
                ws.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, f.c.x, f.c.y + 0.2, f.c.z, 20, f.r / 2, 0.1, f.r / 2, 0.02);
                it.remove();
                continue;
            }
            if (ticksExisted % 4 == 0) ws.spawnParticle(EnumParticleTypes.FLAME, f.c.x, f.c.y + 0.1, f.c.z, 10, f.r / 2, 0.05, f.r / 2, 0.01);
            if (ticksExisted % 10 != 0) continue;
            for (EntityPlayer p : world.getEntitiesWithinAABB(EntityPlayer.class, new net.minecraft.util.math.AxisAlignedBB(f.c.x - f.r, f.c.y - 1, f.c.z - f.r,
                    f.c.x + f.r, f.c.y + 2, f.c.z + f.r)))
                if (valable(p) && p.getDistanceSq(f.c.x, p.posY, f.c.z) <= f.r * f.r) {
                    p.setFire(3);
                    p.attackEntityFrom(DamageSource.IN_FIRE, 2F);
                }
        }
    }

    // ================================================================== effets
    @Override
    protected void effetsClient(String anim) {
        if (("rayon".equals(anim) || "double_rayon".equals(anim)) && tAnim > dureeAnim(anim) * RAYON_DEBUT && tAnim < dureeAnim(anim) * RAYON_FIN) {
            Vec3d o = yeux();
            ligne(o, zone());
            if ("double_rayon".equals(anim)) ligne(o, zone2());
        }
        if ("rugissement".equals(anim) && tAnim > 10 && rand.nextInt(2) == 0)
            world.spawnParticle(EnumParticleTypes.LAVA, posX + (rand.nextDouble() - 0.5) * 8, posY + 1, posZ + (rand.nextDouble() - 0.5) * 8, 0, 0, 0);
        if (rand.nextInt(3) == 0)
            world.spawnParticle(EnumParticleTypes.LAVA, posX + (rand.nextDouble() - 0.5) * width * 1.5, posY + 2.8, posZ + (rand.nextDouble() - 0.5) * width * 1.5,
                    0, 0, 0);
        if (rand.nextInt(2) == 0)
            world.spawnParticle(EnumParticleTypes.FLAME, posX + (rand.nextDouble() - 0.5) * width, posY + 3 + rand.nextDouble() * (height - 3),
                    posZ + (rand.nextDouble() - 0.5) * width, 0, 0.02, 0);
    }

    private int dureeAnim(String anim) {
        return duree(anim, 150);
    }

    private void ligne(Vec3d o, Vec3d b) {
        Vec3d d = b.subtract(o);
        for (int i = 0; i < 10; i++) {
            double k = rand.nextDouble();
            world.spawnParticle(EnumParticleTypes.FLAME, o.x + d.x * k, o.y + d.y * k, o.z + d.z * k, 0, 0, 0);
        }
        world.spawnParticle(EnumParticleTypes.LAVA, b.x, b.y, b.z, 0, 0, 0);
    }

    @Override
    public String resume() {
        return super.resume() + (flaques.isEmpty() ? "" : ", " + flaques.size() + " flaque(s) de feu") + (mode() == CACHE ? ", sous la lave" : "");
    }
}
