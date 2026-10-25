package fr.lenerfvoeux.hxrp.bestiaire.entite.boss;

import fr.lenerfvoeux.hxrp.bestiaire.Especes;
import fr.lenerfvoeux.hxrp.bestiaire.Sons;
import fr.lenerfvoeux.hxrp.bestiaire.entite.Effets;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntityBoss;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Calamar des sables (désert). Il vit sous le sable : il surgit sous un joueur et le projette en l'air, frappe de ses
 * tentacules, crache un rayon de sable qui aveugle, s'enfouit et change de place. En phase 2 : charge longue qui
 * traverse l'arène (contre un mur, il reste étourdi, tentacules au sol), double rayon qui se croise, tempête de sable
 * qui tourne autour de lui, aspiration vers sa gueule. Sur un sol dur (grès, pierre), il ne peut pas surgir dessous.
 */
public class EntityCalamarDesSables extends EntityBoss {
    private static final String[] TENTACULES = {"x1dr4_meleeattack_1", "x1dr4_meleeattack_2", "x1dr4_meleeattack_3"};
    private static final List<Coup> COUPS = Arrays.asList(
            new Coup("surgissement", "immerse", Portee.DISTANCE).duree(90),
            new Coup("tentacules", "x1dr4_meleeattack_1", Portee.CONTACT, 0.5),
            new Coup("rayon_sable", "x1dr4_beam_0", Portee.DISTANCE),
            new Coup("enfouissement", "immerse", Portee.PARTOUT).duree(90),
            new Coup("charge_longue", "charge_2", Portee.DISTANCE).duree(120),
            new Coup("double_rayon", "x1dr4_beam_1", Portee.DISTANCE),
            new Coup("tempete_sable", "tempete_sable", Portee.PARTOUT).duree(70),
            new Coup("aspiration", "aspiration", Portee.DISTANCE).duree(80));

    private static final double RAYON_DEBUT = 0.27, RAYON_FIN = 0.8;
    private static final int CHOC = 30, SONNE = 50, REVEIL = 20;
    private static final float VULNERABLE = 1.5F;
    /** Sous le sable : il disparaît à ce tick et ressort 20 ticks plus tard. */
    private static final int SOUS = 18;

    private final Set<UUID> touches = new HashSet<>();
    private int etourdiFin, sonne = -1;
    private Vec3d visee;

    public EntityCalamarDesSables(World w) {
        super(w, Especes.CALAMAR_DES_SABLES);
    }

    @Override
    protected List<Coup> coups() {
        return COUPS;
    }

    public boolean etourdi() {
        return ticksExisted < etourdiFin;
    }

    @Override
    protected boolean peut(Coup c, double d) {
        if (cible == null || etourdi()) return false;
        switch (c.id) {
            case "surgissement":
                return onGround && meuble(new BlockPos(cible.posX, cible.posY - 0.5, cible.posZ));
            case "rayon_sable":
            case "double_rayon":
            case "charge_longue":
                return d > 8;
            case "tempete_sable":
                return d <= rayon(c.id) + width;
            default:
                return true;
        }
    }

    /** Sol meuble, où il peut surgir : sable, terre, herbe, argile, neige (pas le grès ni la pierre). */
    private boolean meuble(BlockPos b) {
        Material m = world.getBlockState(b).getMaterial();
        return m == Material.SAND || m == Material.GROUND || m == Material.GRASS || m == Material.CLAY || m == Material.CRAFTED_SNOW
                || m == Material.SNOW;
    }

    @Override
    public boolean attackEntityFrom(DamageSource src, float n) {
        return super.attackEntityFrom(src, etourdi() ? n * VULNERABLE : n);
    }

    @Override
    protected void finCoup() {
        super.finCoup();
        if (mode() == CACHE) mode(NORMAL);
        if (etourdi()) pause(Math.max(4, etourdiFin - ticksExisted));
    }

    @Override
    protected void debutCoup(Coup c) {
        super.debutCoup(c);
        touches.clear();
        sonne = -1;
        visee = visee(20).add(0, 0.5, 0);
        if ("tentacules".equals(c.id)) anim(TENTACULES[rand.nextInt(TENTACULES.length)]);
        if ("tempete_sable".equals(c.id)) {
            zone(posX, posY, posZ, rayon(c.id) + width / 2);
            son(Sons.CRI_CALAMAR, 5F, 0.8F);
        }
        if ("aspiration".equals(c.id)) son(Sons.CRI_CALAMAR, 5F, 0.5F);
    }

    @Override
    protected void impact(Coup c, int n) {
        if ("tentacules".equals(c.id)) super.impact(c, n);
    }

    @Override
    protected void executer(Coup c, int t) {
        switch (c.id) {
            case "surgissement":
                sousLeSable(c, t, true);
                break;
            case "enfouissement":
                sousLeSable(c, t, false);
                break;
            case "rayon_sable":
                rayonSable(c, t, false);
                break;
            case "double_rayon":
                rayonSable(c, t, true);
                break;
            case "charge_longue":
                charge(c, t);
                break;
            case "tempete_sable":
                tempete(c, t);
                break;
            case "aspiration":
                aspiration(c, t);
                break;
            default:
                if (cible != null) regarder(cible.posX, cible.posZ, 10F);
                getNavigator().clearPath();
        }
    }

    // ================================================================== sous le sable
    /**
     * Il plonge dans le sable, file dessous et ressort : sous sa cible (surgissement, qui la projette en l'air) ou
     * ailleurs dans l'arène (enfouissement). Sur un sol dur, il ressort à côté, sur le sol meuble le plus proche.
     */
    private void sousLeSable(Coup c, int t, boolean surgir) {
        getNavigator().clearPath();
        motionX = motionZ = 0;
        if (t == SOUS) {
            mode(CACHE);
            poussiere(posX, posZ, 40);
            son(SoundEvents.BLOCK_SAND_BREAK, 4F, 0.6F);
        }
        if (t == SOUS + 20) {
            Vec3d p = surgir ? sousLaCible() : ailleurs();
            if (p != null) setPositionAndUpdate(p.x, p.y, p.z);
            if (cible != null) regarder(cible.posX, cible.posZ, 180F);
            anim("emerge");
            son(SoundEvents.BLOCK_SAND_BREAK, 5F, 0.4F);
        }
        if (surgir && t == SOUS + 32) {
            // il jaillit : tout ce qui est au-dessus de lui est projeté en l'air
            frapperAutour(c, new Vec3d(posX, posY + 1, posZ), rayon(c.id) + width / 2);
            zone(posX, posY, posZ, rayon(c.id) + width / 2);
            poussiere(posX, posZ, 80);
        }
        if (t == SOUS + 20 + duree("emerge", 45)) {
            mode(NORMAL);
            dureeCoup = Math.min(dureeCoup, t + 1);
        }
    }

    /** Sous les pieds de sa cible si le sol y est meuble, sinon sur le sol meuble le plus proche (8 blocs). */
    private Vec3d sousLaCible() {
        if (cible == null) return ailleurs();
        BlockPos o = new BlockPos(cible.posX, cible.posY - 0.5, cible.posZ);
        if (meuble(o)) return new Vec3d(cible.posX, o.getY() + 1, cible.posZ);
        for (int r = 1; r <= 8; r++)
            for (int k = 0; k < 8; k++) {
                double an = k * Math.PI / 4;
                BlockPos b = new BlockPos(cible.posX + Math.cos(an) * r, cible.posY - 0.5, cible.posZ + Math.sin(an) * r);
                if (meuble(b) && world.isAirBlock(b.up())) return new Vec3d(b.getX() + 0.5, b.getY() + 1, b.getZ() + 0.5);
            }
        return null;
    }

    /** Un point de sol meuble dans l'arène, à 8-20 blocs de sa cible (ou de son ancre). */
    private Vec3d ailleurs() {
        Vec3d c = cible != null ? new Vec3d(cible.posX, cible.posY, cible.posZ) : new Vec3d(ax, ay, az);
        for (int essai = 0; essai < 16; essai++) {
            double an = rand.nextDouble() * Math.PI * 2, d = 8 + rand.nextDouble() * 12;
            double x = c.x + Math.cos(an) * d, z = c.z + Math.sin(an) * d;
            if ((x - ax) * (x - ax) + (z - az) * (z - az) > st().rayonArene * st().rayonArene) continue;
            double y = sol(x, z);
            if (meuble(new BlockPos(x, y - 0.5, z))) return new Vec3d(x, y, z);
        }
        return null;
    }

    private void poussiere(double x, double z, int n) {
        if (world instanceof WorldServer)
            ((WorldServer) world).spawnParticle(EnumParticleTypes.BLOCK_DUST, x, posY + 0.2, z, n, width / 2, 0.3, width / 2, 0.15,
                    Block.getStateId(Blocks.SAND.getDefaultState()));
    }

    // ================================================================== rayons
    /** Rayon de sable depuis la gueule (simple : vers la cible ; double : deux rayons qui se croisent sur elle). */
    private void rayonSable(Coup c, int t, boolean double_) {
        int debut = (int) (dureeCoup * RAYON_DEBUT), fin = (int) (dureeCoup * RAYON_FIN);
        if (cible != null) {
            regarder(cible.posX, cible.posZ, 4F);
            visee = new Vec3d(cible.posX, cible.posY + 0.8, cible.posZ);
        }
        if (t < debut || t > fin || t % 2 != 0) return;
        Vec3d o = gueule();
        double largeur = Math.max(1, rayon(c.id)) / 2;
        if (!double_) {
            Vec3d b = rayonUneFois(c, o, visee.subtract(o), portee(c.id), largeur);
            zone(b.x, b.y, b.z, 1);
        } else {
            // deux rayons partis à ±35° qui se referment sur la cible
            double k = 1 - (t - debut) / (double) Math.max(1, fin - debut);
            Vec3d v = visee.subtract(o);
            Vec3d b1 = rayonUneFois(c, o, tourne(v, 35 * k), portee(c.id), largeur);
            Vec3d b2 = rayonUneFois(c, o, tourne(v, -35 * k), portee(c.id), largeur);
            zone(b1.x, b1.y, b1.z, 1);
            zone2(b2);
        }
        if (t % 6 == 0) son(SoundEvents.BLOCK_SAND_STEP, 3F, 0.5F);
    }

    /** Le vecteur tourné de {@code deg} degrés autour de la verticale. */
    private static Vec3d tourne(Vec3d v, double deg) {
        double a = Math.toRadians(deg), c = Math.cos(a), s = Math.sin(a);
        return new Vec3d(v.x * c - v.z * s, v.y, v.x * s + v.z * c);
    }

    /** Un rayon qui ne touche chaque joueur qu'une fois par attaque (aveuglé par le sable : effet du classeur). */
    private Vec3d rayonUneFois(Coup c, Vec3d o, Vec3d dir, double portee, double largeur) {
        Vec3d fin = o.add(dir.normalize().scale(portee));
        RayTraceResult r = world.rayTraceBlocks(o, fin, false, true, false);
        if (r != null && r.hitVec != null) fin = r.hitVec;
        for (EntityPlayer p : world.getEntitiesWithinAABB(EntityPlayer.class, new AxisAlignedBB(o.x, o.y, o.z, fin.x, fin.y, fin.z).grow(largeur + 1))) {
            if (!valable(p) || touches.contains(p.getUniqueID())) continue;
            Vec3d m = new Vec3d(p.posX, p.posY + p.height / 2, p.posZ);
            if (distanceSegment(m, o, fin) <= largeur + p.width / 2 && toucher(p, c, 1F)) touches.add(p.getUniqueID());
        }
        return fin;
    }

    private Vec3d gueule() {
        Vec3d f = devant();
        return new Vec3d(posX + f.x * (width / 2 + 0.5), posY + height * 0.55, posZ + f.z * (width / 2 + 0.5));
    }

    // ================================================================== charge longue
    /** Il traverse l'arène en ligne droite, percute tout sur son passage, puis s'enfonce et ressort ; contre un mur, il est sonné. */
    private void charge(Coup c, int t) {
        getNavigator().clearPath();
        if (etourdi()) {
            motionX = motionZ = 0;
            if (sonne >= 0 && t == sonne + CHOC) anim("stun_idle");
            if (sonne >= 0 && t == sonne + CHOC + SONNE) anim("stun_end");
            return;
        }
        if (t < 8) {
            if (cible != null) regarder(cible.posX, cible.posZ, 30F);
            return;
        }
        if (t <= 46) {
            Vec3d f = devant();
            double v = 1.1 * allure();
            motionX = f.x * v;
            motionZ = f.z * v;
            if (t % 3 == 0) poussiere(posX, posZ, 15);
            for (EntityPlayer p : world.getEntitiesWithinAABB(EntityPlayer.class, getEntityBoundingBox().grow(1.5, 0.5, 1.5)))
                if (valable(p) && touches.add(p.getUniqueID())) toucher(p, c, 1F);
            if (collidedHorizontally && t > 12) {
                etourdir(t);
                return;
            }
            return;
        }
        // fin de la charge : il s'enfonce (l'animation finit sous le sable), puis ressort sur place
        if (t == 60) mode(CACHE);
        if (t == 72) {
            anim("emerge");
            poussiere(posX, posZ, 40);
        }
        if (t == 72 + duree("emerge", 45)) {
            mode(NORMAL);
            dureeCoup = Math.min(dureeCoup, t + 1);
        }
    }

    private void etourdir(int t) {
        etourdiFin = ticksExisted + CHOC + SONNE + REVEIL;
        motionX = motionZ = 0;
        sonne = t;
        anim("stun");
        dureeCoup = t + CHOC + SONNE + REVEIL;
        son(SoundEvents.ENTITY_IRONGOLEM_HURT, 4F, 0.6F);
        annoncer("§e" + espece.nom + " heurte le mur et s'effondre, tentacules au sol !");
    }

    // ================================================================== tempête, aspiration
    /** Tempête de sable : un anneau de sable tourne autour de lui ; il blesse et aveugle ceux qu'il traverse. */
    private void tempete(Coup c, int t) {
        getNavigator().clearPath();
        tourner(rotationYaw + 6F, 6F);
        if (t < 10 || t % 10 != 0) return;
        double r = rayon(c.id) + width / 2;
        for (EntityPlayer p : world.getEntitiesWithinAABB(EntityPlayer.class, getEntityBoundingBox().grow(r, 4, r))) {
            double d = Math.sqrt(p.getDistanceSq(posX, p.posY, posZ));
            if (valable(p) && d <= r && d >= 2) toucher(p, c, 0.34F);
        }
        son(SoundEvents.BLOCK_SAND_STEP, 4F, 0.4F);
    }

    /** Aspiration : il ouvre la gueule et attire les joueurs proches vers lui ; ceux qui l'atteignent sont mordus. */
    private void aspiration(Coup c, int t) {
        getNavigator().clearPath();
        if (cible != null) regarder(cible.posX, cible.posZ, 5F);
        if (t < 12 || t > 70) return;
        Vec3d g = gueule();
        double r = rayon(c.id) + width / 2;
        for (EntityPlayer p : world.getEntitiesWithinAABB(EntityPlayer.class, getEntityBoundingBox().grow(r, 6, r))) {
            if (!valable(p)) continue;
            double dx = g.x - p.posX, dz = g.z - p.posZ, d = Math.sqrt(dx * dx + dz * dz);
            if (d > r) continue;
            if (d > 1) {
                p.motionX += dx / d * 0.09;
                p.motionZ += dz / d * 0.09;
                p.velocityChanged = true;
            }
            if (d < 3 && t % 10 == 0) toucher(p, c, 1F);
            if (t % 20 == 0) Effets.appliquer(p, this, "lenteur", 1, false);
        }
        zone(g.x, g.y, g.z, r);
    }

    // ================================================================== effets
    @Override
    protected void effetsClient(String anim) {
        if (anim.startsWith("x1dr4_beam") && tAnim > 12 && tAnim < 38) {
            Vec3d o = gueule();
            ligne(o, zone());
            if (anim.endsWith("_1")) ligne(o, zone2());
        }
        if ("tempete_sable".equals(anim) && tAnim > 8) {
            float r = rayonZone();
            for (int i = 0; i < 14; i++) {
                double an = tAnim * 0.25 + rand.nextDouble() * Math.PI * 2;
                world.spawnParticle(EnumParticleTypes.BLOCK_DUST, posX + Math.cos(an) * r, posY + rand.nextDouble() * 3, posZ + Math.sin(an) * r,
                        -Math.sin(an) * 0.4, 0.02, Math.cos(an) * 0.4, Block.getStateId(Blocks.SAND.getDefaultState()));
            }
        }
        if ("aspiration".equals(anim) && tAnim > 10 && tAnim < 70) {
            Vec3d g = zone();
            float r = rayonZone();
            for (int i = 0; i < 6; i++) {
                double an = rand.nextDouble() * Math.PI * 2, d = 2 + rand.nextDouble() * r;
                double x = g.x + Math.cos(an) * d, z = g.z + Math.sin(an) * d;
                world.spawnParticle(EnumParticleTypes.CLOUD, x, posY + 0.5 + rand.nextDouble() * 3, z, (g.x - x) * 0.06, 0, (g.z - z) * 0.06);
            }
        }
        if ("charge_2".equals(anim) && tAnim > 8 && tAnim < 50)
            world.spawnParticle(EnumParticleTypes.BLOCK_DUST, posX, posY + 0.2, posZ, 0, 0.2, 0, Block.getStateId(Blocks.SAND.getDefaultState()));
    }

    private void ligne(Vec3d o, Vec3d b) {
        Vec3d d = b.subtract(o);
        for (int i = 0; i < 8; i++) {
            double k = rand.nextDouble();
            world.spawnParticle(EnumParticleTypes.BLOCK_DUST, o.x + d.x * k, o.y + d.y * k, o.z + d.z * k, 0, 0, 0,
                    Block.getStateId(Blocks.SAND.getDefaultState()));
        }
    }

    @Override
    public String resume() {
        return super.resume() + (etourdi() ? ", étourdi" : "") + (mode() == CACHE ? ", sous le sable" : "");
    }
}
