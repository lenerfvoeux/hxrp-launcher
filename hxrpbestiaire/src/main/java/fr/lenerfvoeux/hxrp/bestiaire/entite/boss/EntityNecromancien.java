package fr.lenerfvoeux.hxrp.bestiaire.entite.boss;

import fr.lenerfvoeux.hxrp.bestiaire.Especes;
import fr.lenerfvoeux.hxrp.bestiaire.Sons;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntityBoss;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntityProjectileBoss;
import fr.lenerfvoeux.hxrp.bestiaire.stats.Stats;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Arrays;
import java.util.List;

/**
 * Nécromancien (cimetières, ruines) : il flotte au ras du sol. Coup de bâton, frappe au sol (onde qui repousse et
 * ralentit), levée des morts (archers squelettes) ; en phase 2, trois boules d'âme à tête chercheuse, téléportation
 * loin du groupe et grande levée. Tant que des archers sont en vie, il se soigne lentement.
 */
public class EntityNecromancien extends EntityBoss {
    private static final List<Coup> COUPS = Arrays.asList(
            new Coup("coup_baton", "basic_melee", Portee.CONTACT, 0.5),
            new Coup("frappe_sol", "staff_slam", Portee.PARTOUT, 0.55),
            new Coup("levee_morts", "invocation", Portee.PARTOUT, 0.6),
            new Coup("boules_ame", "boule_ame", Portee.DISTANCE, 0.55),
            new Coup("teleportation", "teleportation", Portee.PARTOUT, 0.5));

    public EntityNecromancien(World w) {
        super(w, Especes.NECROMANCIEN);
    }

    @Override
    protected List<Coup> coups() {
        return COUPS;
    }

    @Override
    protected boolean peut(Coup c, double d) {
        if (cible == null) return false;
        switch (c.id) {
            case "frappe_sol":
                return d <= rayon(c.id);
            case "levee_morts":
                return sbiresVivants() < 9;
            case "teleportation":
                return d < 8;
            default:
                return true;
        }
    }

    @Override
    protected void impact(Coup c, int n) {
        switch (c.id) {
            case "frappe_sol":
                frapperAutour(c, new Vec3d(posX, posY + 0.5, posZ), rayon(c.id));
                zone(posX, posY, posZ, rayon(c.id));
                son(SoundEvents.ENTITY_GENERIC_EXPLODE, 2F, 0.6F);
                break;
            case "levee_morts":
                if (invoquer("archer_squelette", Especes.ARCHER_SQUELETTE, 7) > 0) son(Sons.INCANTATION, 4F, 0.8F);
                break;
            case "boules_ame":
                boules(c);
                break;
            case "teleportation":
                teleporter(c);
                break;
            default:
                super.impact(c, n);
        }
    }

    @Override
    protected void executer(Coup c, int t) {
        if (cible != null && !"teleportation".equals(c.id)) regarder(cible.posX, cible.posZ, 10F);
    }

    private Vec3d baton() {
        Vec3d f = devant();
        return new Vec3d(posX + f.x * 1.5 - f.z * 1.6, posY + height * 0.62, posZ + f.z * 1.5 + f.x * 1.6);
    }

    private void boules(Coup c) {
        if (cible == null) return;
        Stats.Attaque a = st().attaque(c.id);
        Vec3d o = baton();
        for (int i = -1; i <= 1; i++) {
            EntityProjectileBoss p = new EntityProjectileBoss(world, this, EntityProjectileBoss.Type.AME, degats(c.id), a.effet, a.dureeEffet)
                    .depuis(o.x, o.y + i * 0.4, o.z);
            p.viser(cible, 0.7F, 0F);
            // éventail : elles partent écartées et se rabattent sur la cible
            Vec3d f = devant();
            p.motionX += -f.z * i * 0.35;
            p.motionZ += f.x * i * 0.35;
            world.spawnEntity(p);
        }
        son(SoundEvents.ENTITY_EVOCATION_ILLAGER_CAST_SPELL, 3F, 0.7F);
    }

    /** Téléportation : il disparaît dans une fumée noire et reparaît loin du groupe (portée du classeur). */
    private void teleporter(Coup c) {
        if (!(world instanceof WorldServer) || cible == null) return;
        WorldServer ws = (WorldServer) world;
        double portee = Math.max(10, portee(c.id));
        for (int essai = 0; essai < 12; essai++) {
            double an = rand.nextDouble() * Math.PI * 2, d = portee * (0.5 + rand.nextDouble() * 0.4);
            double x = cible.posX + Math.cos(an) * d, z = cible.posZ + Math.sin(an) * d;
            if ((x - ax) * (x - ax) + (z - az) * (z - az) > st().rayonArene * st().rayonArene * 0.6) continue;
            BlockPos b = new BlockPos(x, 0, z);
            if (!world.isBlockLoaded(b)) continue;
            double y = sol(x, z);
            if (!world.getCollisionBoxes(this, getEntityBoundingBox().offset(x - posX, y - posY, z - posZ)).isEmpty()) continue;
            ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX, posY + height / 2, posZ, 60, width / 2, height / 3, width / 2, 0.05);
            ws.spawnParticle(EnumParticleTypes.SPELL_WITCH, posX, posY + height / 2, posZ, 40, width / 2, height / 3, width / 2, 0.1);
            son(SoundEvents.ENTITY_ENDERMEN_TELEPORT, 3F, 0.5F);
            setPositionAndUpdate(x, y, z);
            ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, x, y + height / 2, z, 60, width / 2, height / 3, width / 2, 0.05);
            son(SoundEvents.ENTITY_ENDERMEN_TELEPORT, 3F, 0.5F);
            return;
        }
    }

    /** Tant que des archers sont en vie, il se soigne lentement. */
    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        if (ticksExisted % 20 == 0 && isEntityAlive() && getHealth() < getMaxHealth()) {
            int n = sbiresVivants();
            if (n > 0) {
                heal(getMaxHealth() * 0.002F * n);
                if (world instanceof WorldServer)
                    ((WorldServer) world).spawnParticle(EnumParticleTypes.SPELL_MOB, posX, posY + height * 0.6, posZ, 4 + n, width / 2, height / 3,
                            width / 2, 0);
            }
        }
    }

    @Override
    protected void effetsClient(String anim) {
        // il flotte : traînée d'ombre sous la tunique
        if (rand.nextInt(2) == 0)
            world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, posX + (rand.nextDouble() - 0.5) * width, posY + 0.2,
                    posZ + (rand.nextDouble() - 0.5) * width, 0, 0.02, 0);
        switch (anim) {
            case "staff_slam": {
                if (tAnim < 20 || tAnim > 34) break;
                double r = (tAnim - 20) * rayonZone() / 14.0;
                Vec3d c = zone();
                for (int i = 0; i < 14; i++) {
                    double an = rand.nextDouble() * Math.PI * 2;
                    world.spawnParticle(EnumParticleTypes.SPELL_WITCH, c.x + Math.cos(an) * r, c.y + 0.2, c.z + Math.sin(an) * r, 0, 0.05, 0);
                }
                break;
            }
            case "invocation":
            case "boule_ame": {
                Vec3d b = baton();
                for (int i = 0; i < 3; i++)
                    world.spawnParticle(EnumParticleTypes.SPELL_WITCH, b.x + (rand.nextDouble() - 0.5), b.y + rand.nextDouble(),
                            b.z + (rand.nextDouble() - 0.5), 0, 0.1, 0);
                break;
            }
            case "teleportation":
            case "apparition": {
                for (int i = 0; i < 4; i++)
                    world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX + (rand.nextDouble() - 0.5) * width * 1.5, posY + rand.nextDouble() * height,
                            posZ + (rand.nextDouble() - 0.5) * width * 1.5, 0, 0.03, 0);
                break;
            }
            default:
        }
    }
}
