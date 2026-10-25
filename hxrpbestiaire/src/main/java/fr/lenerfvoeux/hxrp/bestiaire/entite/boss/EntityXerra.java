package fr.lenerfvoeux.hxrp.bestiaire.entite.boss;

import fr.lenerfvoeux.hxrp.bestiaire.Especes;
import fr.lenerfvoeux.hxrp.bestiaire.Registre;
import fr.lenerfvoeux.hxrp.bestiaire.Sons;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntityBoss;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntityProjectileBoss;
import fr.lenerfvoeux.hxrp.bestiaire.stats.Stats;
import net.minecraft.block.Block;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Arrays;
import java.util.List;

/**
 * Xerra, mère-araignée (grottes profondes). Une araignée géante chevauchée par une femme pâle. Au contact : coup de
 * patte, griffures qui font saigner ; rugissement qui ralentit ; couvée d'araignées ; jet de toile qui immobilise et
 * laisse une toile. En phase 2 : graines-pièges qui enracinent, course, bond sur un joueur éloigné. À 0 PV elle
 * s'effondre puis se relève avec toute sa vie (phase 3) : grande couvée qui couvre l'arène de toiles, et ses
 * araignées la soignent en la rejoignant. Les toiles brûlent.
 */
public class EntityXerra extends EntityBoss {
    private static final List<Coup> COUPS = Arrays.asList(
            new Coup("coup_patte", "swing_leg", Portee.CONTACT, 0.45),
            new Coup("griffures", "xerra_rake", Portee.CONTACT, 0.3, 0.45, 0.6, 0.75),
            new Coup("rugissement", "xerra_roar", Portee.PARTOUT, 0.4),
            new Coup("couvee", "xerra_summon", Portee.PARTOUT, 0.6),
            new Coup("jet_toile", "jet_toile", Portee.DISTANCE, 0.5),
            new Coup("graines_pieges", "xerra_summon_seed", Portee.PARTOUT, 0.45),
            new Coup("course", "fast_walk", Portee.DISTANCE).duree(50),
            new Coup("bond", "bond", Portee.DISTANCE).duree(40),
            new Coup("grande_couvee", "xerra_summon", Portee.PARTOUT, 0.6));

    /** Bond : envol (ticks) et durée du vol. */
    private static final int ENVOL_BOND = 8, VOL_BOND = 18;
    private boolean touchee;

    public EntityXerra(World w) {
        super(w, Especes.XERRA);
    }

    @Override
    protected List<Coup> coups() {
        return COUPS;
    }

    @Override
    protected boolean peut(Coup c, double d) {
        if (cible == null) return false;
        switch (c.id) {
            case "course":
            case "bond":
                return d > 8 && onGround;
            case "jet_toile":
                return d > 4;
            case "couvee":
                return sbiresVivants() < 10;
            case "grande_couvee":
                return sbiresVivants() < 24;
            default:
                return true;
        }
    }

    @Override
    protected String animRenaissance() {
        return "renaissance";
    }

    @Override
    protected int dureeRenaissance() {
        return duree("renaissance", 140);
    }

    @Override
    protected void debutCoup(Coup c) {
        super.debutCoup(c);
        touchee = false;
        if ("rugissement".equals(c.id)) son(Sons.CRI_XERRA, 5F, 0.7F);
    }

    @Override
    protected void impact(Coup c, int n) {
        switch (c.id) {
            case "griffures":
                frapperDevant(c, 1F / c.impacts.length);
                break;
            case "rugissement":
                frapperAutour(c, new Vec3d(posX, posY + 1, posZ), rayon(c.id) + width / 2);
                zone(posX, posY + 1, posZ, rayon(c.id) + width / 2);
                break;
            case "couvee":
                invoquer("araignee_xerra", Especes.ARAIGNEE_XERRA, 5);
                break;
            case "grande_couvee":
                invoquer("araignee_xerra", Especes.ARAIGNEE_XERRA, 8);
                toiles(25, Math.max(12, rayon(c.id)));
                break;
            case "jet_toile":
                jetToile(c);
                break;
            case "graines_pieges":
                planter(c);
                break;
            case "course":
            case "bond":
                break;
            default:
                super.impact(c, n);
        }
    }

    @Override
    protected void executer(Coup c, int t) {
        switch (c.id) {
            case "course":
                course(c, t);
                break;
            case "bond":
                bond(c, t);
                break;
            default:
                if (cible != null) regarder(cible.posX, cible.posZ, 12F);
                getNavigator().clearPath();
        }
    }

    /** Jet de toile : une boule de soie qui immobilise et laisse une toile là où elle touche. */
    private void jetToile(Coup c) {
        Stats.Attaque a = st().attaque(c.id);
        Vec3d f = devant();
        EntityProjectileBoss p = new EntityProjectileBoss(world, this, EntityProjectileBoss.Type.TOILE, degats(c.id), a.effet, a.dureeEffet)
                .depuis(posX + f.x * (width / 2 + 0.5), posY + height * 0.35, posZ + f.z * (width / 2 + 0.5));
        if (cible != null) p.viser(cible, 1.4F, 1F);
        else p.viser(visee(10), 1.4F, 1F);
        world.spawnEntity(p);
        son(SoundEvents.ENTITY_SLIME_ATTACK, 2F, 0.6F);
    }

    /** Graines-pièges : elle sème des bulbes autour de sa cible (dans l'air, sur un sol plein). */
    private void planter(Coup c) {
        Vec3d v = visee(6);
        double r = Math.max(3, rayon(c.id));
        int n = 4 + phase * 2;
        for (int i = 0; i < n; i++) {
            double an = rand.nextDouble() * Math.PI * 2, d = 1 + rand.nextDouble() * r;
            BlockPos b = surSol(v.x + Math.cos(an) * d, v.y, v.z + Math.sin(an) * d);
            if (b != null && Registre.GRAINE != null) world.setBlockState(b, Registre.GRAINE.getDefaultState());
        }
        if (world instanceof WorldServer)
            ((WorldServer) world).spawnParticle(EnumParticleTypes.BLOCK_DUST, v.x, v.y + 0.1, v.z, 30, r / 2, 0.1, r / 2, 0.05,
                    Block.getStateId(net.minecraft.init.Blocks.NETHER_WART_BLOCK.getDefaultState()));
        zone(v.x, v.y, v.z, r);
    }

    /** Grande couvée : des toiles un peu partout dans l'arène, au ras du sol (jamais à la place d'un bloc). */
    private void toiles(int n, double r) {
        if (Registre.TOILE == null) return;
        for (int i = 0; i < n * 3 && n > 0; i++) {
            double an = rand.nextDouble() * Math.PI * 2, d = 2 + rand.nextDouble() * r;
            BlockPos b = surSol(posX + Math.cos(an) * d, posY, posZ + Math.sin(an) * d);
            if (b == null) continue;
            world.setBlockState(b, Registre.TOILE.getDefaultState());
            n--;
        }
        son(SoundEvents.ENTITY_SPIDER_AMBIENT, 4F, 0.5F);
    }

    /** Une case d'air posée sur un sol plein, près de la hauteur donnée ; null si rien ne convient. */
    private BlockPos surSol(double x, double y, double z) {
        BlockPos b = new BlockPos(x, y + 2, z);
        for (int k = 0; k < 6; k++, b = b.down()) {
            if (!world.isBlockLoaded(b)) return null;
            if (world.isAirBlock(b) && world.getBlockState(b.down()).isSideSolid(world, b.down(), EnumFacing.UP)) return b;
        }
        return null;
    }

    /** Course : elle fonce droit sur sa cible et la percute (une fois), jusqu'à la toucher ou buter sur un mur. */
    private void course(Coup c, int t) {
        if (cible == null) {
            dureeCoup = t;
            return;
        }
        regarder(cible.posX, cible.posZ, 10F);
        getNavigator().clearPath();
        Vec3d f = devant();
        double v = 0.55 * allure();
        motionX = f.x * v;
        motionZ = f.z * v;
        if (t % 6 == 0) son(SoundEvents.ENTITY_SPIDER_STEP, 2F, 0.6F);
        if (!touchee && distance(cible) < 1.5) {
            touchee = true;
            frapperDevant(c);
            dureeCoup = t + 8;
        }
        if (collidedHorizontally && t > 6) dureeCoup = Math.min(dureeCoup, t + 4);
    }

    /** Bond : elle se ramasse, saute sur sa cible et retombe en écrasant tout autour. */
    private void bond(Coup c, int t) {
        if (t < ENVOL_BOND) {
            if (cible != null) regarder(cible.posX, cible.posZ, 30F);
            return;
        }
        if (t == ENVOL_BOND) {
            Vec3d vers = cible != null ? new Vec3d(cible.posX, cible.posY, cible.posZ) : getPositionVector().add(devant().scale(10));
            sauter(vers, VOL_BOND, 5 + getDistance(vers.x, vers.y, vers.z) * 0.15, false);
            son(SoundEvents.ENTITY_SPIDER_AMBIENT, 3F, 0.5F);
            return;
        }
        if (enSaut()) {
            if (bondir() != 0) atterrir(c, t);
        }
    }

    private void atterrir(Coup c, int t) {
        frapperAutour(c, new Vec3d(posX, posY + 0.5, posZ), rayon(c.id) + width / 2);
        zone(posX, posY, posZ, rayon(c.id) + width / 2);
        son(SoundEvents.ENTITY_GENERIC_EXPLODE, 2F, 0.7F);
        if (world instanceof WorldServer)
            ((WorldServer) world).spawnParticle(EnumParticleTypes.BLOCK_DUST, posX, posY + 0.1, posZ, 50, 2, 0.1, 2, 0.1,
                    Block.getStateId(world.getBlockState(new BlockPos(posX, posY - 0.5, posZ))));
        dureeCoup = Math.min(dureeCoup, t + 10);
    }

    @Override
    protected void finCoup() {
        if (enSaut()) finSaut();
        super.finCoup();
    }

    @Override
    protected void effetsClient(String anim) {
        if (mode() == ARRIVEE) {
            // le fil de soie qui la retient au plafond
            for (int i = 0; i < 3; i++)
                world.spawnParticle(EnumParticleTypes.END_ROD, posX, posY + height + rand.nextDouble() * 12, posZ, 0, 0, 0);
            return;
        }
        if ("xerra_roar".equals(anim) && tAnim > 6 && tAnim < 20) {
            Vec3d z = zone();
            float r = rayonZone();
            for (int i = 0; i < 6; i++) {
                double an = rand.nextDouble() * Math.PI * 2;
                world.spawnParticle(EnumParticleTypes.CLOUD, z.x + Math.cos(an) * r * tAnim / 20, posY + 0.3, z.z + Math.sin(an) * r * tAnim / 20, 0, 0.02, 0);
            }
        }
        if ("renaissance".equals(anim) && rand.nextInt(2) == 0)
            world.spawnParticle(EnumParticleTypes.SPELL_WITCH, posX + (rand.nextDouble() - 0.5) * width, posY + rand.nextDouble() * height,
                    posZ + (rand.nextDouble() - 0.5) * width, 0, 0.05, 0);
        if (phase() >= 3 && rand.nextInt(4) == 0)
            world.spawnParticle(EnumParticleTypes.DRIP_LAVA, posX + (rand.nextDouble() - 0.5) * width, posY + height * 0.7,
                    posZ + (rand.nextDouble() - 0.5) * width, 0, 0, 0);
    }

    @Override
    public void setInWeb() {
    }

    /** Pour /xerra info : les toiles autour d'elle. */
    public int toilesProches() {
        int n = 0;
        BlockPos o = getPosition();
        for (BlockPos b : BlockPos.getAllInBoxMutable(o.add(-16, -4, -16), o.add(16, 4, 16)))
            if (Registre.TOILE != null && world.getBlockState(b).getBlock() == Registre.TOILE) n++;
        return n;
    }

    @Override
    public String resume() {
        return super.resume() + ", " + toilesProches() + " toile(s) proche(s)";
    }
}
