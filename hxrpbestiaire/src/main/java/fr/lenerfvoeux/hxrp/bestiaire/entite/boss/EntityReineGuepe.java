package fr.lenerfvoeux.hxrp.bestiaire.entite.boss;

import fr.lenerfvoeux.hxrp.bestiaire.Especes;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntityBoss;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntityProjectileBoss;
import fr.lenerfvoeux.hxrp.bestiaire.stats.Stats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Arrays;
import java.util.List;

/**
 * Reine-guêpe (jungles et forêts profondes). Elle vole autour de sa cible : quatre coups de dard selon le côté où se
 * trouve le joueur, hachage, onde d'air qui repousse, essaim de guêpes soldats. En phase 2 : regard brûlant (un rayon
 * depuis l'œil qui balaie l'arène, elle ne bouge plus : c'est la fenêtre pour la frapper), rafale de cinq dards
 * empoisonnés en éventail, piqué dard en avant, et un grand essaim.
 */
public class EntityReineGuepe extends EntityBoss {
    private static final List<Coup> COUPS = Arrays.asList(
            new Coup("taille_droite", "right_cute", Portee.CONTACT, 0.42),
            new Coup("taille_gauche", "left_cute", Portee.CONTACT, 0.42),
            new Coup("taille_haute", "up_cute", Portee.CONTACT, 0.45),
            new Coup("taille_basse", "below_cute", Portee.CONTACT, 0.45),
            new Coup("hachage", "minced", Portee.CONTACT, 0.3, 0.5, 0.7),
            new Coup("onde_air", "air_wave", Portee.DISTANCE, 0.5),
            new Coup("essaim", "summon", Portee.PARTOUT, 0.6),
            new Coup("regard_brulant", "eye_cast", Portee.DISTANCE),
            new Coup("rafale_dards", "tir_dards", Portee.DISTANCE, 0.55),
            new Coup("pique", "pique", Portee.DISTANCE).duree(70));

    /** Regard brûlant : début et fin du rayon (part de l'animation). */
    private static final double REGARD_DEBUT = 0.2, REGARD_FIN = 0.9;
    private Vec3d visee;
    private boolean piqueFaite;

    public EntityReineGuepe(World w) {
        super(w, Especes.REINE_GUEPE);
    }

    @Override
    protected List<Coup> coups() {
        return COUPS;
    }

    @Override
    protected double hauteurVol() {
        return 1.0;
    }

    /** Le joueur est-il à sa droite (positif) ou à sa gauche ? */
    private double cote(EntityPlayer p) {
        Vec3d f = devant();
        return (p.posX - posX) * -f.z + (p.posZ - posZ) * f.x;
    }

    @Override
    protected boolean peut(Coup c, double d) {
        if (cible == null) return false;
        switch (c.id) {
            case "taille_droite":
                return cote(cible) >= 0;
            case "taille_gauche":
                return cote(cible) < 0;
            case "taille_haute":
                return cible.posY > posY + height * 0.5;
            case "taille_basse":
                return cible.posY + cible.height < posY + height * 0.25;
            case "regard_brulant":
            case "pique":
                return d > 7;
            case "essaim":
                return sbiresVivants() < 8;
            default:
                return true;
        }
    }

    /** Point de départ des dards et du rayon : l'œil (haut du corps) et le bout de l'abdomen (bas). */
    private Vec3d oeil() {
        Vec3d f = devant();
        return new Vec3d(posX + f.x * 1.2, posY + height * 0.74, posZ + f.z * 1.2);
    }

    private Vec3d dard() {
        Vec3d f = devant();
        return new Vec3d(posX + f.x * 1.6, posY + height * 0.3, posZ + f.z * 1.6);
    }

    @Override
    protected void debutCoup(Coup c) {
        super.debutCoup(c);
        piqueFaite = false;
        Vec3d f = devant();
        visee = cible != null ? new Vec3d(cible.posX, cible.posY + 0.5, cible.posZ) : new Vec3d(posX + f.x * 12, sol(posX + f.x * 12, posZ + f.z * 12), posZ + f.z * 12);
        if ("regard_brulant".equals(c.id)) zone(visee.x, visee.y, visee.z, rayon(c.id));
    }

    @Override
    protected void impact(Coup c, int n) {
        switch (c.id) {
            case "hachage":
                frapperDevant(c, 1F / 3F);
                break;
            case "onde_air":
                frapperDevant(c);
                zone(posX, posY + height * 0.5, posZ, portee(c.id));
                break;
            case "essaim":
                invoquer("guepe_soldat", Especes.GUEPE_SOLDAT, 4);
                break;
            case "rafale_dards":
                tirerDards(c);
                break;
            case "regard_brulant":
            case "pique":
                break;
            default:
                super.impact(c, n);
        }
    }

    @Override
    protected void executer(Coup c, int t) {
        if ("regard_brulant".equals(c.id)) regard(c, t);
        else if ("pique".equals(c.id)) pique(c, t);
        else if (cible != null) {
            // pendant ses coups elle reste face à sa cible, à distance de frappe
            regarder(cible.posX, cible.posZ, 8F);
            motionX *= 0.8;
            motionY *= 0.8;
            motionZ *= 0.8;
        }
    }

    private void tirerDards(Coup c) {
        if (cible == null) return;
        Stats.Attaque a = st().attaque(c.id);
        Vec3d o = dard();
        double base = Math.atan2(cible.posZ - o.z, cible.posX - o.x);
        for (int i = -2; i <= 2; i++) {
            double an = base + Math.toRadians(i * 11);
            double dx = Math.cos(an), dz = Math.sin(an);
            double dy = (cible.posY + cible.height * 0.5 - o.y) / Math.max(1, cible.getDistance(o.x, o.y, o.z));
            EntityProjectileBoss p = new EntityProjectileBoss(world, this, EntityProjectileBoss.Type.DARD, degats(c.id), a.effet, a.dureeEffet)
                    .depuis(o.x, o.y, o.z);
            p.shoot(dx, dy, dz, 1.7F, 0.5F);
            world.spawnEntity(p);
        }
        son(net.minecraft.init.SoundEvents.ENTITY_ARROW_SHOOT, 2F, 0.7F);
    }

    /** Regard brûlant : immobile, elle balaie l'arène d'un rayon qui suit lentement sa cible. */
    private void regard(Coup c, int t) {
        motionX = motionY = motionZ = 0;
        if (cible != null) {
            regarder(cible.posX, cible.posZ, 2.5F);
            Vec3d vise = new Vec3d(cible.posX, cible.posY + 0.5, cible.posZ);
            Vec3d d = vise.subtract(visee);
            double l = d.length(), max = 0.32 * allure();
            visee = l > max ? visee.add(d.scale(max / l)) : vise;
        }
        Vec3d o = oeil();
        Vec3d dir = visee.subtract(o).normalize();
        Vec3d bout = o.add(dir.scale(portee(c.id)));
        RayTraceResult r = world.rayTraceBlocks(o, bout, false, true, false);
        if (r != null && r.typeOfHit == RayTraceResult.Type.BLOCK) bout = r.hitVec;
        zone(bout.x, bout.y, bout.z, rayon(c.id));
        if (t < dureeCoup * REGARD_DEBUT || t > dureeCoup * REGARD_FIN || t % 10 != 0) return;
        double largeur = rayon(c.id);
        for (EntityPlayer p : world.getEntitiesWithinAABB(EntityPlayer.class, getEntityBoundingBox().grow(portee(c.id) + 2))) {
            if (!valable(p)) continue;
            Vec3d q = new Vec3d(p.posX, p.posY + p.height * 0.5, p.posZ);
            Vec3d oq = q.subtract(o);
            double proj = MathHelper.clamp(oq.dotProduct(dir), 0, bout.subtract(o).length());
            if (o.add(dir.scale(proj)).distanceTo(q) <= largeur) toucher(p, c, 1F);
        }
        if (world instanceof WorldServer)
            ((WorldServer) world).spawnParticle(EnumParticleTypes.LAVA, bout.x, bout.y, bout.z, 3, 0.4, 0.2, 0.4, 0);
    }

    /** Piqué : elle monte au-dessus de sa cible, puis fonce dard en avant sur le point visé. */
    private void pique(Coup c, int t) {
        if (t <= 22) {
            if (cible != null) {
                voler(cible.posX, cible.posY + 11, cible.posZ, 0.9, 0.2);
                regarder(cible.posX, cible.posZ, 15F);
                visee = new Vec3d(cible.posX, cible.posY + 0.3, cible.posZ);
            }
            return;
        }
        if (!piqueFaite) {
            voler(visee.x, visee.y, visee.z, 1.6, 0.35);
            regarder(visee.x, visee.z, 20F);
            double d = getDistance(visee.x, visee.y, visee.z);
            if (d < 2.5 || collidedVertically || onGround || (collidedHorizontally && t > 30) || t > 55) {
                piqueFaite = true;
                motionX = motionY = motionZ = 0;
                frapperAutour(c, new Vec3d(posX, posY + 0.5, posZ), rayon(c.id) + width / 2);
                zone(posX, posY, posZ, rayon(c.id) + width / 2);
                son(net.minecraft.init.SoundEvents.ENTITY_GENERIC_EXPLODE, 1.5F, 1.4F);
                if (world instanceof WorldServer)
                    ((WorldServer) world).spawnParticle(EnumParticleTypes.SPELL_MOB, posX, posY + 0.5, posZ, 40, 1.5, 0.4, 1.5, 0);
            }
        } else {
            motionY = Math.max(motionY, 0.25);
        }
    }

    // ================================================================== particules côté client
    @Override
    protected void effetsClient(String anim) {
        switch (anim) {
            case "eye_cast": {
                // le rayon : de l'œil au point visé (synchronisé), rouge et brûlant
                int d = 100;
                if (tAnim < d * REGARD_DEBUT) {
                    Vec3d o = oeil();
                    for (int i = 0; i < 3; i++)
                        world.spawnParticle(EnumParticleTypes.FLAME, o.x + (rand.nextDouble() - 0.5) * 2, o.y + (rand.nextDouble() - 0.5) * 2,
                                o.z + (rand.nextDouble() - 0.5) * 2, 0, 0, 0);
                    break;
                }
                if (tAnim > d * REGARD_FIN) break;
                Vec3d o = oeil(), b = zone();
                Vec3d dir = b.subtract(o);
                double l = dir.length();
                for (double k = 0; k < l; k += 0.6) {
                    Vec3d p = o.add(dir.scale(k / l));
                    world.spawnParticle(EnumParticleTypes.REDSTONE, p.x, p.y, p.z, 1.0, 0.15, 0.05);
                }
                world.spawnParticle(EnumParticleTypes.FLAME, b.x, b.y, b.z, 0, 0.05, 0);
                break;
            }
            case "air_wave": {
                if (tAnim < 10 || tAnim > 26) break;
                Vec3d f = devant();
                double r = (tAnim - 10) * 1.2;
                for (int i = 0; i < 10; i++) {
                    double an = (rand.nextDouble() - 0.5) * 1.6;
                    double x = f.x * Math.cos(an) - f.z * Math.sin(an), z = f.z * Math.cos(an) + f.x * Math.sin(an);
                    world.spawnParticle(EnumParticleTypes.CLOUD, posX + x * r, posY + height * (0.3 + rand.nextDouble() * 0.4), posZ + z * r,
                            x * 0.3, 0, z * 0.3);
                }
                break;
            }
            case "tir_dards":
            case "summon": {
                if (rand.nextInt(2) == 0) world.spawnParticle(EnumParticleTypes.SPELL_MOB, posX + (rand.nextDouble() - 0.5) * width,
                        posY + height * 0.3, posZ + (rand.nextDouble() - 0.5) * width, 0.3, 0.6, 0.1);
                break;
            }
            default:
        }
    }
}
