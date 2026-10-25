package fr.lenerfvoeux.hxrp.bestiaire.entite.boss;

import fr.lenerfvoeux.hxrp.bestiaire.Especes;
import fr.lenerfvoeux.hxrp.bestiaire.Sons;
import fr.lenerfvoeux.hxrp.bestiaire.entite.Effets;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntityBoss;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Kraken des abysses (océan profond). Un poulpe géant qui ne quitte pas l'eau : il se tient à la surface, la tête et
 * les tentacules dressés. Trois frappes de tentacules (la troisième balaie et repousse), plongée (il ressort ailleurs),
 * rayon d'eau sous pression, charge à la surface (frappé plusieurs fois pendant sa charge, il reste étourdi). En
 * phase 2 : rayon qui balaie tout l'horizon, ponte (petits krakens), nuage d'encre qui aveugle, saisie (il attrape un
 * joueur et le projette). À 0 PV il coule puis remonte avec toute sa vie (phase 3) : rage des abysses, longue charge
 * qui finit sous l'eau, et ses rayons reviennent deux fois plus vite.
 */
public class EntityKraken extends EntityBoss {
    private static final List<Coup> COUPS = Arrays.asList(
            new Coup("tentacule_1", "x1dr4_meleeattack_1", Portee.CONTACT, 0.5),
            new Coup("tentacule_2", "x1dr4_meleeattack_2", Portee.CONTACT, 0.5),
            new Coup("tentacule_3", "x1dr4_meleeattack_3", Portee.CONTACT, 0.5),
            new Coup("plongee", "immerse", Portee.PARTOUT).duree(110),
            new Coup("rayon", "x1dr4_beam_0", Portee.DISTANCE),
            new Coup("charge", "charge", Portee.DISTANCE).duree(36),
            new Coup("double_rayon", "x1dr4_beam_1", Portee.DISTANCE),
            new Coup("ponte", "eggs", Portee.PARTOUT, 0.6),
            new Coup("nuage_encre", "nuage_encre", Portee.PARTOUT, 0.4),
            new Coup("saisie", "saisie", Portee.DISTANCE).duree(50),
            new Coup("rage_abysses", "charge_2", Portee.DISTANCE).duree(130));

    /** Rayons : fenêtre où l'eau jaillit (part de l'animation). */
    private static final double RAYON_DEBUT = 0.27, RAYON_FIN = 0.8;
    /** Étourdi : coups encaissés pendant une charge, et durées (ticks) des trois temps (choc, sonné, réveil). */
    private static final int COUPS_POUR_ETOURDIR = 3, CHOC = 30, SONNE = 60, REVEIL = 20;
    private static final float VULNERABLE = 1.5F;

    private final Set<UUID> touches = new HashSet<>();
    private int coupsRecus, etourdiFin, encreFin, sonne = -1;
    private EntityPlayer saisi;
    private Vec3d visee;

    public EntityKraken(World w) {
        super(w, Especes.KRAKEN);
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
            case "rayon":
            case "double_rayon":
            case "charge":
            case "rage_abysses":
                return d > 8;
            case "ponte":
                return sbiresVivants() < 12;
            default:
                return true;
        }
    }

    /** Phase 3 : ses rayons reviennent deux fois plus vite, et il enchaîne sans pause. */
    @Override
    protected double recharge(String id) {
        double r = super.recharge(id);
        return phase >= 3 && id.endsWith("rayon") ? r / 2 : r;
    }

    @Override
    protected void finCoup() {
        super.finCoup();
        if (mode() == CACHE) mode(NORMAL);
        if (saisi != null) relacher(false);
        if (phase >= 3) pause(8);
        if (etourdi()) pause(Math.max(4, etourdiFin - ticksExisted));
    }

    @Override
    protected String animRenaissance() {
        return "reveil_abysses";
    }

    @Override
    protected int dureeRenaissance() {
        return duree("reveil_abysses", 120);
    }

    /** Frappé pendant sa charge : au troisième coup, il s'arrête net, étourdi (et vulnérable). */
    @Override
    public boolean attackEntityFrom(DamageSource src, float n) {
        boolean ok = super.attackEntityFrom(src, etourdi() ? n * VULNERABLE : n);
        if (ok && coup != null && "charge".equals(coup.id) && src.getTrueSource() instanceof EntityPlayer && ++coupsRecus >= COUPS_POUR_ETOURDIR
                && !etourdi())
            etourdir(tCoup);
        return ok;
    }

    @Override
    protected void debutCoup(Coup c) {
        super.debutCoup(c);
        touches.clear();
        coupsRecus = 0;
        Vec3d f = devant();
        visee = cible != null ? new Vec3d(cible.posX, cible.posY + 0.5, cible.posZ) : new Vec3d(posX + f.x * 20, posY, posZ + f.z * 20);
        if ("double_rayon".equals(c.id) || "rage_abysses".equals(c.id)) son(Sons.CRI_KRAKEN, 5F, 0.7F);
    }

    @Override
    protected void impact(Coup c, int n) {
        switch (c.id) {
            case "ponte":
                if (world instanceof WorldServer)
                    ((WorldServer) world).spawnParticle(EnumParticleTypes.SLIME, posX, posY + 1, posZ, 40, 3, 0.5, 3, 0.1);
                invoquer("petit_kraken", Especes.PETIT_KRAKEN, 7);
                break;
            case "nuage_encre":
                frapperAutour(c, new Vec3d(posX, posY + 1, posZ), rayon(c.id) + width / 2);
                zone(posX, posY, posZ, rayon(c.id) + width / 2);
                encreFin = ticksExisted + 60;
                son(SoundEvents.ENTITY_GENERIC_SPLASH, 4F, 0.5F);
                break;
            case "tentacule_3":
                frapperDevant(c);
                break;
            case "plongee":
            case "rayon":
            case "double_rayon":
            case "charge":
            case "saisie":
            case "rage_abysses":
                break;
            default:
                super.impact(c, n);
        }
    }

    @Override
    protected void executer(Coup c, int t) {
        switch (c.id) {
            case "plongee":
                plongee(t, false);
                break;
            case "rayon":
                rayonEau(c, t, false);
                break;
            case "double_rayon":
                rayonEau(c, t, true);
                break;
            case "charge":
                charge(c, t, 6, 26, 0.9);
                break;
            case "rage_abysses":
                charge(c, t, 6, 46, 1.1);
                if (t >= 46) plongee(t - 30, true);
                break;
            case "saisie":
                saisie(c, t);
                break;
            default:
                if (cible != null) regarder(cible.posX, cible.posZ, 8F);
        }
    }

    // ================================================================== plongée
    /**
     * Il s'enfonce, file sous l'eau et ressort près de sa cible (dans l'eau, à 6-14 blocs). {@code dejaSous} : la
     * plongée suit une charge qui finit déjà sous l'eau.
     */
    private void plongee(int t, boolean dejaSous) {
        motionX *= 0.5;
        motionZ *= 0.5;
        int sous = dejaSous ? 16 : duree("immerse", 43) - 6;
        if (t == sous) {
            mode(CACHE);
            son(SoundEvents.ENTITY_GENERIC_SPLASH, 4F, 0.5F);
        }
        if (t == sous + 20) {
            Vec3d p = pointDansLEau();
            if (p != null) setPositionAndUpdate(p.x, p.y, p.z);
            if (cible != null) regarder(cible.posX, cible.posZ, 180F);
            anim("emerge");
            son(SoundEvents.ENTITY_GENERIC_SPLASH, 5F, 0.4F);
        }
        if (t == sous + 20 + duree("emerge", 45)) {
            mode(NORMAL);
            dureeCoup = Math.min(dureeCoup, tCoup + 1);
        }
    }

    /** Un point de l'arène où l'eau est assez profonde, près de la cible (ou de l'ancre) ; null s'il n'y en a pas. */
    private Vec3d pointDansLEau() {
        Vec3d c = cible != null ? new Vec3d(cible.posX, cible.posY, cible.posZ) : new Vec3d(ax, ay, az);
        for (int essai = 0; essai < 16; essai++) {
            double an = rand.nextDouble() * Math.PI * 2, d = 6 + rand.nextDouble() * 8;
            double x = c.x + Math.cos(an) * d, z = c.z + Math.sin(an) * d;
            if ((x - ax) * (x - ax) + (z - az) * (z - az) > st().rayonArene * st().rayonArene) continue;
            double s = surface(x, c.y, z);
            if (!Double.isNaN(s) && profond(x, s, z)) return new Vec3d(x, s + espece.flottaison, z);
        }
        return null;
    }

    /** Au moins trois blocs d'eau sous la surface. */
    private boolean profond(double x, double s, double z) {
        for (int k = 1; k <= 3; k++)
            if (world.getBlockState(new BlockPos(x, s - k, z)).getMaterial() != espece.liquide) return false;
        return true;
    }

    // ================================================================== rayons
    /**
     * Rayon d'eau sous pression depuis l'œil : il suit lentement sa cible (simple), ou balaie tout l'horizon (double).
     */
    private void rayonEau(Coup c, int t, boolean balaie) {
        int debut = (int) (dureeCoup * RAYON_DEBUT), fin = (int) (dureeCoup * RAYON_FIN);
        if (balaie) {
            if (t >= debut && t <= fin) tourner(rotationYaw + 360F / Math.max(1, fin - debut), 30F);
        } else if (cible != null) {
            regarder(cible.posX, cible.posZ, 3F);
            visee = new Vec3d(cible.posX, cible.posY + 0.8, cible.posZ);
        }
        if (t < debut || t > fin || t % 2 != 0) return;
        Vec3d o = oeil(), dir;
        if (balaie) {
            Vec3d f = devant();
            dir = new Vec3d(f.x, -0.12, f.z);
        } else {
            Vec3d v = visee.subtract(o);
            Vec3d f = devant();
            dir = new Vec3d(f.x * Math.sqrt(v.x * v.x + v.z * v.z), v.y, f.z * Math.sqrt(v.x * v.x + v.z * v.z));
        }
        Vec3d bout = rayonUneFois(c, o, dir, portee(c.id), Math.max(1, rayon(c.id)) / 2);
        zone(bout.x, bout.y, bout.z, 1);
        if (t % 8 == 0) son(SoundEvents.ENTITY_GENERIC_SPLASH, 3F, 1.4F);
    }

    /** Un rayon qui ne touche chaque joueur qu'une fois par attaque. */
    private Vec3d rayonUneFois(Coup c, Vec3d o, Vec3d dir, double portee, double largeur) {
        Vec3d fin = o.add(dir.normalize().scale(portee));
        net.minecraft.util.math.RayTraceResult r = world.rayTraceBlocks(o, fin, false, true, false);
        if (r != null && r.hitVec != null) fin = r.hitVec;
        for (EntityPlayer p : world.getEntitiesWithinAABB(EntityPlayer.class, new net.minecraft.util.math.AxisAlignedBB(o.x, o.y, o.z, fin.x, fin.y, fin.z)
                .grow(largeur + 1))) {
            if (!valable(p) || touches.contains(p.getUniqueID())) continue;
            Vec3d m = new Vec3d(p.posX, p.posY + p.height / 2, p.posZ);
            if (distanceSegment(m, o, fin) <= largeur + p.width / 2 && toucher(p, c, 1F)) touches.add(p.getUniqueID());
        }
        return fin;
    }

    private Vec3d oeil() {
        Vec3d f = devant();
        return new Vec3d(posX + f.x * (width / 2 + 0.5), posY + 3.5, posZ + f.z * (width / 2 + 0.5));
    }

    // ================================================================== charges
    /** Il fonce à la surface, droit devant lui, et percute chaque joueur sur son passage (une fois chacun). */
    private void charge(Coup c, int t, int debut, int fin, double vitesse) {
        if (etourdi()) {
            motionX = motionZ = 0;
            return;
        }
        if (t < debut) {
            if (cible != null) regarder(cible.posX, cible.posZ, 30F);
            return;
        }
        if (t > fin) return;
        Vec3d f = devant();
        double v = vitesse * allure();
        // jamais hors de l'eau : il s'arrête au bord
        if (Double.isNaN(surface(posX + f.x * (width / 2 + 2), posY, posZ + f.z * (width / 2 + 2)))) v = 0;
        motionX = f.x * v;
        motionZ = f.z * v;
        if (t % 4 == 0 && world instanceof WorldServer)
            ((WorldServer) world).spawnParticle(EnumParticleTypes.WATER_SPLASH, posX, posY + 0.5, posZ, 30, width / 2, 0.3, width / 2, 0.1);
        for (EntityPlayer p : world.getEntitiesWithinAABB(EntityPlayer.class, getEntityBoundingBox().grow(1.5, 1, 1.5)))
            if (valable(p) && touches.add(p.getUniqueID())) toucher(p, c, 1F);
    }

    /** Étourdi : il s'arrête net (choc), reste sonné, puis se reprend. */
    private void etourdir(int t) {
        etourdiFin = ticksExisted + CHOC + SONNE + REVEIL;
        motionX = motionZ = 0;
        anim("stun");
        dureeCoup = t + CHOC + SONNE + REVEIL;
        sonne = t;
        son(SoundEvents.ENTITY_GUARDIAN_HURT, 4F, 0.5F);
        annoncer("§e" + espece.nom + " est étourdi : ses tentacules retombent dans l'eau !");
    }

    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        if (etourdi() && coup != null && sonne >= 0) {
            if (tCoup == sonne + CHOC) anim("stun_idle");
            if (tCoup == sonne + CHOC + SONNE) anim("stun_end");
        }
        if (ticksExisted < encreFin && ticksExisted % 5 == 0 && world instanceof WorldServer) {
            Vec3d z = zone();
            ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_LARGE, z.x, posY + 1.5, z.z, 25, rayonZone() / 2, 1, rayonZone() / 2, 0.01);
        }
    }

    // ================================================================== saisie
    /** Saisie : un tentacule attrape le joueur le plus proche, le soulève, puis le projette au loin. */
    private void saisie(Coup c, int t) {
        if (t == 12) {
            EntityPlayer p = cible;
            double portee = portee(c.id) + width / 2;
            if (p == null || getDistance(p) > portee + 4) p = world.getClosestPlayer(posX, posY, posZ, portee, false);
            if (p != null && valable(p)) {
                saisi = p;
                son(SoundEvents.ENTITY_SQUID_HURT, 3F, 0.5F);
            }
        }
        if (saisi != null) {
            if (!valable(saisi) || saisi.world != world) {
                saisi = null;
                return;
            }
            Vec3d f = devant();
            double h = 4 + Math.min(4, (t - 12) * 0.25);
            saisi.setPositionAndUpdate(posX + f.x * (width / 2 + 3), posY + h, posZ + f.z * (width / 2 + 3));
            saisi.fallDistance = 0;
            if (t >= 32) relacher(true);
        }
        if (cible != null) regarder(cible.posX, cible.posZ, 10F);
    }

    private void relacher(boolean projeter) {
        EntityPlayer p = saisi;
        saisi = null;
        if (p == null || !projeter) return;
        Coup c = coup("saisie");
        toucher(p, c, 1F);
        Vec3d f = devant();
        Effets.repousser(p, f.x, f.z, 2.2, 1.0);
        son(SoundEvents.ENTITY_GENERIC_SPLASH, 3F, 0.7F);
    }

    // ================================================================== arrivée et effets
    /** Sous le tonneau : à la surface de l'eau, juste dessous. */
    @Override
    protected Vec3d pointDeSurgissement(BlockPos appat) {
        double s = surface(appat.getX() + 0.5, appat.getY(), appat.getZ() + 0.5);
        return new Vec3d(appat.getX() + 0.5, (Double.isNaN(s) ? appat.getY() : s) + espece.flottaison, appat.getZ() + 0.5);
    }

    @Override
    protected void effetsClient(String anim) {
        if (anim.startsWith("x1dr4_beam") && tAnim > 12 && tAnim < 38) {
            // le jet d'eau, de l'œil au point touché
            Vec3d o = oeil(), b = zone(), d = b.subtract(o);
            for (int i = 0; i < 8; i++) {
                double k = rand.nextDouble();
                world.spawnParticle(EnumParticleTypes.WATER_SPLASH, o.x + d.x * k, o.y + d.y * k, o.z + d.z * k, 0, 0, 0);
                world.spawnParticle(EnumParticleTypes.WATER_BUBBLE, o.x + d.x * k, o.y + d.y * k, o.z + d.z * k, 0, 0, 0);
            }
        }
        if ("nuage_encre".equals(anim) && tAnim > 6 && tAnim < 40) {
            float r = rayonZone();
            for (int i = 0; i < 8; i++)
                world.spawnParticle(EnumParticleTypes.SPELL_MOB, posX + (rand.nextDouble() - 0.5) * 2 * r, posY + rand.nextDouble() * 4,
                        posZ + (rand.nextDouble() - 0.5) * 2 * r, 0, 0, 0);
        }
        if (("charge".equals(anim) || "charge_2".equals(anim)) && tAnim > 5)
            for (int i = 0; i < 4; i++)
                world.spawnParticle(EnumParticleTypes.WATER_WAKE, posX + (rand.nextDouble() - 0.5) * width, posY + 0.1, posZ + (rand.nextDouble() - 0.5) * width,
                        0, 0, 0);
        if (("stun".equals(anim) || "stun_idle".equals(anim)) && tAnim % 4 == 0)
            for (int i = 0; i < 3; i++) {
                double an = tAnim * 0.3 + i * 2.1;
                world.spawnParticle(EnumParticleTypes.CRIT, posX + Math.cos(an) * 2, posY + 5, posZ + Math.sin(an) * 2, 0, 0, 0);
            }
        if (rand.nextInt(3) == 0)
            world.spawnParticle(EnumParticleTypes.WATER_SPLASH, posX + (rand.nextDouble() - 0.5) * width * 1.5, posY + 0.1,
                    posZ + (rand.nextDouble() - 0.5) * width * 1.5, 0, 0.1, 0);
    }

    @Override
    public String resume() {
        return super.resume() + (etourdi() ? ", étourdi" : "") + (saisi != null ? ", tient " + saisi.getName() : "");
    }
}
