package fr.lenerfvoeux.hxrp.bestiaire.entite.boss;

import fr.lenerfvoeux.hxrp.bestiaire.Especes;
import fr.lenerfvoeux.hxrp.bestiaire.entite.Effets;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntityBoss;
import fr.lenerfvoeux.hxrp.bestiaire.stats.Stats;
import net.minecraft.entity.EntityAreaEffectCloud;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Arrays;
import java.util.List;

/**
 * Abomination, porteuse de Zobae (souterrains) : petite, rapide, et c'est le boss du Hunter Virus. Ses coups ont 20 %
 * de chances (40 % en phase 2) de transmettre une maladie du Virus au hasard. Bond sur un joueur éloigné ; en phase 2,
 * elle s'enfouit et ressort derrière un joueur, et vomit une flaque de bile empoisonnée.
 */
public class EntityAbomination extends EntityBoss {
    private static final List<Coup> COUPS = Arrays.asList(
            new Coup("coups", "meleeattack", Portee.CONTACT, 0.42),
            new Coup("bond", "bond", Portee.DISTANCE).duree(40),
            new Coup("enfouissement", "enfouissement", Portee.PARTOUT),
            new Coup("vomissure", "vomissure", Portee.DISTANCE, 0.5));

    private boolean atterrie;
    private int sortie;

    public EntityAbomination(World w) {
        super(w, Especes.ABOMINATION);
    }

    @Override
    protected List<Coup> coups() {
        return COUPS;
    }

    @Override
    protected boolean peut(Coup c, double d) {
        if (cible == null) return false;
        switch (c.id) {
            case "bond":
                return d > 5 && onGround;
            case "enfouissement":
                return onGround;
            default:
                return true;
        }
    }

    /** Ses coups transmettent une maladie du Virus : 20 % en phase 1, 40 % ensuite (contagion). */
    @Override
    protected void touche(EntityPlayer p, Coup c) {
        if ("coups".equals(c.id) && Effets.maladie(p, phase >= 2 ? 0.4 : 0.2, rand))
            p.sendMessage(new net.minecraft.util.text.TextComponentString("§2Les griffes de l'Abomination vous ont infecté…"));
    }

    @Override
    protected void debutCoup(Coup c) {
        super.debutCoup(c);
        atterrie = false;
        if ("enfouissement".equals(c.id)) {
            // durée : s'enfouir, puis ressortir (animation d'apparition jouée de nouveau)
            sortie = duree("enfouissement", 30);
            dureeCoup = sortie + duree("spawn", 25) + 4;
        }
    }

    @Override
    protected void impact(Coup c, int n) {
        switch (c.id) {
            case "vomissure":
                vomir(c);
                break;
            case "bond":
            case "enfouissement":
                break;
            default:
                super.impact(c, n);
        }
    }

    @Override
    protected void executer(Coup c, int t) {
        switch (c.id) {
            case "bond":
                bond(c, t);
                break;
            case "enfouissement":
                enfouissement(c, t);
                break;
            default:
                if (cible != null) regarder(cible.posX, cible.posZ, 15F);
        }
    }

    /** Bond : elle se ramasse, saute sur sa cible et retombe en écrasant tout autour. */
    private void bond(Coup c, int t) {
        if (t < 8) {
            if (cible != null) regarder(cible.posX, cible.posZ, 30F);
            return;
        }
        if (t == 8 && cible != null) {
            double dx = cible.posX - posX, dz = cible.posZ - posZ, d = Math.sqrt(dx * dx + dz * dz);
            double duree = 14;
            motionX = dx / duree;
            motionZ = dz / duree;
            motionY = 0.55 + Math.min(0.5, d * 0.02);
            velocityChanged = true;
            son(SoundEvents.ENTITY_SLIME_JUMP, 2F, 0.6F);
            return;
        }
        if (!atterrie && t > 12 && onGround) {
            atterrie = true;
            frapperAutour(c, new Vec3d(posX, posY + 0.5, posZ), rayon(c.id) + width / 2);
            zone(posX, posY, posZ, rayon(c.id) + width / 2);
            son(SoundEvents.ENTITY_SLIME_SQUISH, 3F, 0.5F);
            if (world instanceof WorldServer)
                ((WorldServer) world).spawnParticle(EnumParticleTypes.BLOCK_DUST, posX, posY + 0.1, posZ, 40, 1.5, 0.1, 1.5, 0.1,
                        net.minecraft.block.Block.getStateId(world.getBlockState(new BlockPos(posX, posY - 0.5, posZ))));
            dureeCoup = Math.min(dureeCoup, t + 8);
        }
    }

    /** Enfouissement : elle s'enfonce dans le sol, file sous terre et ressort derrière un joueur, griffes en avant. */
    private void enfouissement(Coup c, int t) {
        motionX = motionZ = 0;
        if (t == sortie - 2) mode(CACHE);
        if (t == sortie) {
            if (cible != null) {
                // derrière le joueur (à l'opposé de son regard), sur le sol
                Vec3d r = cible.getLookVec();
                double x = cible.posX - r.x * 2.5, z = cible.posZ - r.z * 2.5;
                setPositionAndUpdate(x, sol(x, z), z);
                regarder(cible.posX, cible.posZ, 180F);
            }
            anim("spawn");
            son(SoundEvents.BLOCK_GRAVEL_BREAK, 3F, 0.6F);
        }
        if (t == sortie + 12) {
            mode(NORMAL);
            frapperAutour(c, new Vec3d(posX, posY + 0.5, posZ), 2.5 + width / 2);
            if (world instanceof WorldServer)
                ((WorldServer) world).spawnParticle(EnumParticleTypes.BLOCK_DUST, posX, posY + 0.2, posZ, 50, 1.2, 0.3, 1.2, 0.15,
                        net.minecraft.block.Block.getStateId(world.getBlockState(new BlockPos(posX, posY - 0.5, posZ))));
        }
    }

    @Override
    protected void finCoup() {
        if (mode() == CACHE) mode(NORMAL);
        super.finCoup();
    }

    /** Vomissure : une flaque de bile empoisonnée là où se tient sa cible. */
    private void vomir(Coup c) {
        Vec3d f = devant();
        Vec3d centre = cible != null ? new Vec3d(cible.posX, cible.posY, cible.posZ) : new Vec3d(posX + f.x * 4, posY, posZ + f.z * 4);
        Stats.Attaque a = st().attaque(c.id);
        frapperAutour(c, centre.add(0, 0.5, 0), rayon(c.id));
        EntityAreaEffectCloud flaque = new EntityAreaEffectCloud(world, centre.x, sol(centre.x, centre.z), centre.z);
        flaque.setOwner(this);
        flaque.setRadius((float) rayon(c.id));
        flaque.setRadiusOnUse(0F);
        flaque.setRadiusPerTick(-(float) rayon(c.id) / (float) Math.max(40, a.dureeEffet * 20) * 0.3F);
        flaque.setDuration((int) Math.max(40, a.dureeEffet * 20));
        flaque.setWaitTime(5);
        flaque.setColor(0x6E8B1E);
        flaque.addEffect(new PotionEffect(MobEffects.POISON, 80, 1));
        world.spawnEntity(flaque);
        zone(centre.x, centre.y, centre.z, rayon(c.id));
        son(SoundEvents.ENTITY_PLAYER_BURP, 3F, 0.4F);
    }

    @Override
    protected void effetsClient(String anim) {
        if ("vomissure".equals(anim) && tAnim > 8 && tAnim < 22) {
            // jet de bile de la gueule vers la zone visée
            Vec3d o = new Vec3d(posX, posY + height * 0.75, posZ), b = zone();
            Vec3d d = b.subtract(o);
            for (int i = 0; i < 4; i++) {
                double k = rand.nextDouble();
                world.spawnParticle(EnumParticleTypes.SLIME, o.x + d.x * k, o.y + d.y * k + Math.sin(k * Math.PI) * 1.5, o.z + d.z * k, 0, 0, 0);
            }
        }
        if (rand.nextInt(6) == 0)
            world.spawnParticle(EnumParticleTypes.SPELL_MOB, posX + (rand.nextDouble() - 0.5) * width, posY + rand.nextDouble() * height,
                    posZ + (rand.nextDouble() - 0.5) * width, 0.35, 0.55, 0.15);
    }
}
