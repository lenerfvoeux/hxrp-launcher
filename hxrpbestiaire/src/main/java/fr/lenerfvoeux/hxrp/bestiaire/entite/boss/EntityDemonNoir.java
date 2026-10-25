package fr.lenerfvoeux.hxrp.bestiaire.entite.boss;

import fr.lenerfvoeux.hxrp.bestiaire.Especes;
import fr.lenerfvoeux.hxrp.bestiaire.Sons;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntityBoss;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntityProjectileBoss;
import fr.lenerfvoeux.hxrp.bestiaire.stats.Stats;
import net.minecraft.block.Block;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Démon noir du Continent noir (ruines). Un colosse de 9 blocs aux bras griffus. Combos de griffes (1 à 3, puis 4 et 5
 * en phase 2), saut écrasé (il bondit et retombe sur un joueur : onde de choc), charge d'épaule qui renverse. En phase 2 :
 * pluie de météores sombres, orbe sombre qui poursuit sa cible. À 0 PV il s'agenouille puis se relève avec toute sa vie
 * (second souffle, phase 3) : déchaînement, ses combos s'enchaînent sans pause et ses sauts reviennent deux fois plus
 * vite. S'il heurte un mur pendant un saut ou une charge, il reste étourdi un moment (et encaisse davantage).
 */
public class EntityDemonNoir extends EntityBoss {
    private static final String[] COMBOS = {"combo_1", "combo_2", "combo_3", "combo_4", "combo_5"};
    private static final List<Coup> COUPS = Arrays.asList(
            new Coup("combo_1", "meleeattack_1", Portee.CONTACT, 0.35, 0.6),
            new Coup("combo_2", "meleeattack_2", Portee.CONTACT, 0.35, 0.6),
            new Coup("combo_3", "meleeattack_3", Portee.CONTACT, 0.3, 0.5, 0.7),
            new Coup("saut_ecrase", "jump", Portee.DISTANCE).duree(70),
            new Coup("charge_epaule", "charge_epaule", Portee.DISTANCE).duree(45),
            new Coup("combo_4", "meleeattack_4", Portee.CONTACT, 0.3, 0.5, 0.7),
            new Coup("combo_5", "meleeattack_5", Portee.CONTACT, 0.25, 0.45, 0.65, 0.8),
            new Coup("meteores", "cast", Portee.DISTANCE),
            new Coup("orbe", "orbe", Portee.DISTANCE, 0.5),
            new Coup("dechainement", "meleeattack_1", Portee.CONTACT).duree(80));

    /** Saut : envol (ticks, quand il quitte le sol dans l'animation) et durée du vol (il garde la pose en l'air). */
    private static final int ENVOL_SAUT = 12, VOL_SAUT = 22, ELAN_CHARGE = 7;
    /** Étourdi : durée (ticks) et dégâts reçus en plus. */
    private static final int ETOURDI = 35;
    private static final float VULNERABLE = 1.5F;

    private int etourdiFin;
    private final Set<UUID> renverses = new HashSet<>();
    /** Déchaînement : les combos tirés au sort, leurs débuts (ticks) et le combo en cours. */
    private final List<Coup> enchainement = new ArrayList<>();
    private final List<Integer> debuts = new ArrayList<>();
    private int segment;

    public EntityDemonNoir(World w) {
        super(w, Especes.DEMON_NOIR);
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
            case "saut_ecrase":
            case "charge_epaule":
                return d > 7 && onGround;
            case "meteores":
            case "orbe":
                return d > 4;
            default:
                return true;
        }
    }

    /** Phase 3 : ses sauts reviennent deux fois plus vite. */
    @Override
    protected double recharge(String id) {
        double r = super.recharge(id);
        return phase >= 3 && "saut_ecrase".equals(id) ? r / 2 : r;
    }

    /** Phase 3 : plus de pause entre deux attaques (déchaînement). */
    @Override
    protected void finCoup() {
        if (enSaut()) finSaut();
        super.finCoup();
        if (phase >= 3) pause(4);
        if (etourdi()) pause(Math.max(4, etourdiFin - ticksExisted));
    }

    @Override
    protected String animRenaissance() {
        return "second_souffle";
    }

    @Override
    protected int dureeRenaissance() {
        return duree("second_souffle", 100);
    }

    @Override
    public boolean attackEntityFrom(DamageSource src, float n) {
        return super.attackEntityFrom(src, etourdi() ? n * VULNERABLE : n);
    }

    @Override
    protected void debutSautArrivee() {
        anim("jump");
    }

    @Override
    protected void atterrissage() {
        super.atterrissage();
        anim("jump_end");
        onde(Math.max(4, rayon("saut_ecrase")), false);
    }

    @Override
    protected void debutCoup(Coup c) {
        super.debutCoup(c);
        renverses.clear();
        if ("dechainement".equals(c.id)) {
            // trois combos au hasard, enchaînés sans temps mort
            enchainement.clear();
            debuts.clear();
            int t = 0;
            for (int i = 0; i < 3; i++) {
                Coup k = coup(COMBOS[rand.nextInt(COMBOS.length)]);
                enchainement.add(k);
                debuts.add(t);
                t += Math.max(10, duree(k.anim, 24) - 4);
            }
            segment = 0;
            dureeCoup = t;
            anim(enchainement.get(0).anim);
            son(Sons.RUGISSEMENT_DEMON, 5F, 0.8F);
        }
        if ("meteores".equals(c.id)) son(Sons.GRONDEMENT_DEMON, 5F, 0.6F);
    }

    @Override
    protected void impact(Coup c, int n) {
        switch (c.id) {
            case "combo_1":
            case "combo_2":
            case "combo_3":
            case "combo_4":
            case "combo_5":
                frapperDevant(c, 1F / c.impacts.length);
                break;
            case "orbe":
                orbe(c);
                break;
            case "saut_ecrase":
            case "charge_epaule":
            case "meteores":
            case "dechainement":
                break;
            default:
                super.impact(c, n);
        }
    }

    @Override
    protected void executer(Coup c, int t) {
        switch (c.id) {
            case "saut_ecrase":
                saut(c, t);
                break;
            case "charge_epaule":
                charge(c, t);
                break;
            case "meteores":
                meteores(c, t);
                break;
            case "dechainement":
                dechainement(t);
                break;
            default:
                if (cible != null) regarder(cible.posX, cible.posZ, 12F);
                getNavigator().clearPath();
        }
    }

    // ================================================================== saut écrasé
    private void saut(Coup c, int t) {
        if (t < ENVOL_SAUT) {
            if (cible != null) regarder(cible.posX, cible.posZ, 30F);
            return;
        }
        if (t == ENVOL_SAUT) {
            Vec3d vers = cible != null ? new Vec3d(cible.posX, cible.posY, cible.posZ) : getPositionVector().add(devant().scale(12));
            sauter(vers, VOL_SAUT, 6 + getDistance(vers.x, vers.y, vers.z) * 0.2, false);
            son(Sons.RUGISSEMENT_DEMON, 4F, 0.9F);
            return;
        }
        if (enSaut()) {
            int r = bondir();
            if (r == 1) {
                anim("jump_end");
                onde(rayon(c.id), true);
                dureeCoup = t + duree("jump_end", 20);
            } else if (r == 2) {
                etourdir(t);
            }
        }
    }

    /** Onde de choc à la réception : dégâts et effets du saut écrasé (aucun dégât à l'arrivée sur l'appât). */
    private void onde(double rayon, boolean frappe) {
        double r = rayon + width / 2;
        if (frappe) frapperAutour(coup("saut_ecrase"), new Vec3d(posX, posY + 0.5, posZ), r);
        zone(posX, posY, posZ, r);
        son(SoundEvents.ENTITY_GENERIC_EXPLODE, 4F, 0.5F);
        if (world instanceof WorldServer)
            ((WorldServer) world).spawnParticle(EnumParticleTypes.BLOCK_DUST, posX, posY + 0.1, posZ, 80, r / 2, 0.1, r / 2, 0.15,
                    Block.getStateId(world.getBlockState(new BlockPos(posX, posY - 0.5, posZ))));
    }

    /** Heurté un mur : il titube, sonné ; c'est la fenêtre pour le frapper. */
    private void etourdir(int t) {
        etourdiFin = ticksExisted + ETOURDI;
        anim("stun");
        dureeCoup = t + ETOURDI;
        son(SoundEvents.ENTITY_IRONGOLEM_HURT, 4F, 0.5F);
        annoncer("§e" + espece.nom + " heurte le mur et reste étourdi !");
        if (world instanceof WorldServer)
            ((WorldServer) world).spawnParticle(EnumParticleTypes.CRIT, posX, posY + height * 0.9, posZ, 30, 1, 0.3, 1, 0.1);
    }

    // ================================================================== charge d'épaule
    private void charge(Coup c, int t) {
        getNavigator().clearPath();
        if (etourdi()) {
            motionX = motionZ = 0;
            return;
        }
        if (t < ELAN_CHARGE) {
            if (cible != null) regarder(cible.posX, cible.posZ, 30F);
            return;
        }
        if (cible != null) regarder(cible.posX, cible.posZ, 4F);
        Vec3d f = devant();
        double v = 0.75 * allure();
        motionX = f.x * v;
        motionZ = f.z * v;
        if (t % 5 == 0) son(SoundEvents.ENTITY_IRONGOLEM_STEP, 3F, 0.6F);
        // tous les joueurs sur son passage sont renversés, chacun une fois
        for (EntityPlayer p : world.getEntitiesWithinAABB(EntityPlayer.class, getEntityBoundingBox().grow(1.2, 0.5, 1.2)))
            if (valable(p) && renverses.add(p.getUniqueID())) toucher(p, c, 1F);
        if (collidedHorizontally && t > ELAN_CHARGE + 3) {
            motionX = motionZ = 0;
            etourdir(t);
            return;
        }
        if (cible != null && distance(cible) < 1 && renverses.contains(cible.getUniqueID())) dureeCoup = Math.min(dureeCoup, t + 6);
    }

    // ================================================================== pluie de météores
    private void meteores(Coup c, int t) {
        getNavigator().clearPath();
        if (cible != null) regarder(cible.posX, cible.posZ, 6F);
        int debut = (int) (dureeCoup * 0.25), fin = (int) (dureeCoup * 0.8);
        if (t < debut || t > fin || (t - debut) % 6 != 0) return;
        Stats.Attaque a = st().attaque(c.id);
        double r = Math.max(4, rayon(c.id));
        Vec3d centre = cible != null ? new Vec3d(cible.posX, cible.posY, cible.posZ) : getPositionVector();
        double an = rand.nextDouble() * Math.PI * 2, d = rand.nextDouble() * r;
        double x = centre.x + Math.cos(an) * d, z = centre.z + Math.sin(an) * d, y = sol(x, z);
        // la boule d'ombre tombe en biais, depuis le côté du démon
        Vec3d f = devant();
        EntityProjectileBoss p = new EntityProjectileBoss(world, this, EntityProjectileBoss.Type.METEORE, degats(c.id), a.effet, a.dureeEffet)
                .depuis(x - f.x * 6, y + 20, z - f.z * 6);
        p.shoot(f.x * 6, -20, f.z * 6, 1.1F, 0F);
        world.spawnEntity(p);
        if (world instanceof WorldServer)
            ((WorldServer) world).spawnParticle(EnumParticleTypes.SPELL_WITCH, x, y + 0.2, z, 20, 1, 0.05, 1, 0.02);
    }

    // ================================================================== orbe sombre
    private void orbe(Coup c) {
        Stats.Attaque a = st().attaque(c.id);
        Vec3d f = devant();
        EntityProjectileBoss p = new EntityProjectileBoss(world, this, EntityProjectileBoss.Type.ORBE, degats(c.id), a.effet, a.dureeEffet)
                .depuis(posX + f.x * 2 - f.z * 1.5, posY + height * 0.7, posZ + f.z * 2 + f.x * 1.5);
        if (cible != null) p.viser(cible, 0.55F, 0F);
        else p.viser(visee(16), 0.55F, 0F);
        world.spawnEntity(p);
        son(SoundEvents.ENTITY_WITHER_SHOOT, 3F, 0.5F);
    }

    // ================================================================== déchaînement (phase 3)
    private void dechainement(int t) {
        if (cible != null) regarder(cible.posX, cible.posZ, 15F);
        getNavigator().clearPath();
        if (enchainement.isEmpty()) return;
        if (segment + 1 < debuts.size() && t >= debuts.get(segment + 1)) {
            segment++;
            anim(enchainement.get(segment).anim);
        }
        Coup k = enchainement.get(segment);
        int d = Math.max(10, duree(k.anim, 24) - 4), local = t - debuts.get(segment);
        for (double i : k.impacts)
            if (local == Math.max(1, (int) Math.round(i * d))) frapperDevant(k, 1F / k.impacts.length);
    }

    @Override
    protected void effetsClient(String anim) {
        if ("cast".equals(anim) && tAnim > 15 && tAnim < 70) {
            for (int i = 0; i < 4; i++)
                world.spawnParticle(EnumParticleTypes.SPELL_WITCH, posX + (rand.nextDouble() - 0.5) * 3, posY + height + rand.nextDouble() * 2,
                        posZ + (rand.nextDouble() - 0.5) * 3, 0, 0.1, 0);
        }
        if ("orbe".equals(anim) && tAnim > 4 && tAnim < 18) {
            Vec3d f = devant();
            world.spawnParticle(EnumParticleTypes.SPELL_MOB, posX + f.x * 2 - f.z * 1.5, posY + height * 0.75, posZ + f.z * 2 + f.x * 1.5, 0, 0, 0);
        }
        if ("charge_epaule".equals(anim) && tAnim > 7)
            world.spawnParticle(EnumParticleTypes.BLOCK_DUST, posX, posY + 0.1, posZ, 0, 0.1, 0,
                    Block.getStateId(world.getBlockState(new BlockPos(posX, posY - 0.5, posZ))));
        if ("stun".equals(anim) && tAnim % 4 == 0)
            for (int i = 0; i < 3; i++) {
                double an = tAnim * 0.3 + i * 2.1;
                world.spawnParticle(EnumParticleTypes.CRIT, posX + Math.cos(an) * 1.2, posY + height + 0.3, posZ + Math.sin(an) * 1.2, 0, 0, 0);
            }
        if ("second_souffle".equals(anim) && rand.nextInt(2) == 0)
            world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX + (rand.nextDouble() - 0.5) * width, posY + rand.nextDouble() * height * 0.6,
                    posZ + (rand.nextDouble() - 0.5) * width, 0, 0.05, 0);
        if (phase() >= 3 && rand.nextInt(3) == 0)
            world.spawnParticle(EnumParticleTypes.SPELL_MOB, posX + (rand.nextDouble() - 0.5) * width, posY + rand.nextDouble() * height,
                    posZ + (rand.nextDouble() - 0.5) * width, 0, 0, 0);
    }

    @Override
    public String resume() {
        return super.resume() + (etourdi() ? ", étourdi" : "");
    }
}
