package fr.lenerfvoeux.hxrp.bestiaire.entite.boss;

import fr.lenerfvoeux.hxrp.bestiaire.Especes;
import fr.lenerfvoeux.hxrp.bestiaire.Registre;
import fr.lenerfvoeux.hxrp.bestiaire.Sons;
import fr.lenerfvoeux.hxrp.bestiaire.entite.Effets;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntityBoss;
import fr.lenerfvoeux.hxrp.bestiaire.stats.Stats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Aï, forme de vie gazeuse (marais de Numere, la nuit). Une silhouette d'ombre très fine qui flotte et traverse les
 * blocs (impossible à coincer). Aspiration d'aura (draine la vie et la faim des joueurs proches), griffes d'ombre qui
 * aveuglent. En phase 2 : grande aspiration (portée doublée, chaque joueur drainé le soigne), cri (nausée, faiblesse),
 * disparition (invisible 4 s, il reparaît dans le dos d'un joueur), vœu maudit (un effet néfaste au hasard, dont les
 * contrôles inversés). La lumière le blesse et réduit la portée de son aspiration ; il se dissipe à l'aube.
 */
public class EntityAi extends EntityBoss {
    private static final List<Coup> COUPS = Arrays.asList(
            new Coup("aspiration_aura", "sucking_spirit", Portee.PARTOUT),
            new Coup("griffes_ombre", "griffes_ombre", Portee.CONTACT, 0.45),
            new Coup("grande_aspiration", "sucking_spirit", Portee.PARTOUT),
            new Coup("cri", "cri", Portee.PARTOUT, 0.45),
            new Coup("disparition", "disparition", Portee.PARTOUT).duree(122),
            new Coup("voeu_maudit", "voeu_maudit", Portee.DISTANCE, 0.55));

    /** Les vœux maudits : texte de l'effet (pour Effets) et nom montré au joueur. */
    private static final String[][] VOEUX = {
            {"lenteur", "lenteur"}, {"faim", "faim"}, {"inverse", "contrôles inversés"}, {"cecite", "cécité"}, {"faiblesse", "faiblesse"},
            {"nausee", "nausée"}};
    /** Disparition : fin du fondu, puis réapparition (ticks). */
    private static final int FONDU = 20, INVISIBLE = 80;

    private boolean nuitVue;

    public EntityAi(World w) {
        super(w, Especes.AI);
        noClip = true;
    }

    @Override
    protected List<Coup> coups() {
        return COUPS;
    }

    @Override
    protected double hauteurVol() {
        return 0.5;
    }

    /** Lumière des blocs (torches, lanternes) autour de lui, de 0 à 15. */
    private int lumiere() {
        BlockPos b = new BlockPos(posX, posY + 1, posZ);
        int m = 0;
        for (BlockPos q : new BlockPos[]{b, b.north(2), b.south(2), b.east(2), b.west(2)})
            if (world.isBlockLoaded(q)) m = Math.max(m, world.getLightFor(EnumSkyBlock.BLOCK, q));
        return m;
    }

    /** Portée de l'aspiration, réduite par la lumière (jusqu'à 60 % de moins en pleine lumière). */
    private double porteeAspiration(String id) {
        return (rayon(id) + width / 2) * (1 - 0.6 * lumiere() / 15.0);
    }

    @Override
    protected boolean peut(Coup c, double d) {
        if (cible == null) return false;
        switch (c.id) {
            case "aspiration_aura":
            case "grande_aspiration":
                return d <= porteeAspiration(c.id);
            case "cri":
                return d <= rayon(c.id);
            case "voeu_maudit":
                return d <= portee(c.id);
            default:
                return true;
        }
    }

    @Override
    public void onUpdate() {
        // gazeux : il traverse tout, même après une arrivée ou un saut qui remettent les collisions
        noClip = true;
        super.onUpdate();
        if (world.isRemote || !isEntityAlive() || ticksExisted % 20 != 0) return;
        if (!world.isDaytime()) nuitVue = true;
        else if (nuitVue && mode() == NORMAL) {
            // il est apparu la nuit et le jour se lève : il se dissipe, sans être vaincu (pas de butin)
            annoncer("§8" + espece.nom + " se dissipe dans la lumière de l'aube…");
            if (world instanceof WorldServer)
                ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX, posY + height / 2, posZ, 80, width, height / 3, width, 0.05);
            retirerSbires();
            setDead();
            return;
        }
        // la lumière vive le brûle
        if (lumiere() >= 12 && mode() == NORMAL) {
            attackEntityFrom(DamageSource.MAGIC, (float) (pvMax() * 0.005));
            if (world instanceof WorldServer)
                ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_NORMAL, posX, posY + height / 2, posZ, 15, 0.4, height / 3, 0.4, 0.02);
        }
    }

    @Override
    protected void debutCoup(Coup c) {
        super.debutCoup(c);
        if ("cri".equals(c.id) || "grande_aspiration".equals(c.id)) son(Sons.CRI_AI, 5F, 1F);
    }

    @Override
    protected void impact(Coup c, int n) {
        switch (c.id) {
            case "cri":
                frapperAutour(c, new Vec3d(posX, posY + height / 2, posZ), rayon(c.id));
                zone(posX, posY + height / 2, posZ, rayon(c.id));
                break;
            case "voeu_maudit":
                voeu(c);
                break;
            case "griffes_ombre":
                super.impact(c, n);
                break;
            default:
        }
    }

    @Override
    protected void executer(Coup c, int t) {
        switch (c.id) {
            case "aspiration_aura":
                aspiration(c, t, false);
                break;
            case "grande_aspiration":
                aspiration(c, t, true);
                break;
            case "disparition":
                disparition(c, t);
                break;
            default:
                if (cible != null) regarder(cible.posX, cible.posZ, 12F);
                motionX *= 0.8;
                motionZ *= 0.8;
        }
    }

    // ================================================================== aspiration
    /** Il aspire l'aura des joueurs proches : vie et faim ; la grande aspiration porte deux fois plus loin et le soigne. */
    private void aspiration(Coup c, int t, boolean grande) {
        motionX *= 0.7;
        motionZ *= 0.7;
        if (cible != null) regarder(cible.posX, cible.posZ, 6F);
        if (t < dureeCoup * 0.25 || t > dureeCoup * 0.85 || t % 10 != 0) return;
        double r = porteeAspiration(c.id);
        zone(posX, posY + height / 2, posZ, r);
        for (EntityPlayer p : new ArrayList<>(world.playerEntities)) {
            if (!valable(p) || p.getDistanceSq(this) > r * r) continue;
            if (!toucher(p, c, 0.5F)) continue;
            p.addExhaustion(3F);
            Effets.effet(p, MobEffects.HUNGER, 60, 1);
            if (grande) heal(getMaxHealth() * 0.01F);
            if (world instanceof WorldServer) {
                // l'aura s'arrache du joueur et file vers lui
                Vec3d de = new Vec3d(p.posX, p.posY + 1, p.posZ), vers = new Vec3d(posX, posY + height * 0.6, posZ), d = vers.subtract(de);
                for (int i = 0; i < 6; i++) {
                    double k = i / 6.0;
                    ((WorldServer) world).spawnParticle(EnumParticleTypes.SPELL_MOB, de.x + d.x * k, de.y + d.y * k, de.z + d.z * k, 0, 0.1, 0.1, 0.12, 1);
                }
            }
        }
        if (t % 20 == 0) son(Sons.CHUCHOTEMENT, 3F, 0.6F);
    }

    // ================================================================== disparition
    /** Il s'efface, reste invisible 4 s (intouchable), puis reparaît dans le dos d'un joueur, griffes en avant. */
    private void disparition(Coup c, int t) {
        motionX = motionY = motionZ = 0;
        if (t == FONDU) {
            setInvisible(true);
            mode(CACHE);
            son(SoundEvents.ENTITY_ENDERMEN_TELEPORT, 3F, 0.6F);
        }
        if (t == FONDU + INVISIBLE) {
            EntityPlayer p = cible != null ? cible : world.getClosestPlayer(posX, posY, posZ, 32, false);
            if (p != null && valable(p)) {
                Vec3d r = p.getLookVec();
                setPositionAndUpdate(p.posX - r.x * 2.5, p.posY, p.posZ - r.z * 2.5);
                regarder(p.posX, p.posZ, 180F);
            }
            setInvisible(false);
            mode(NORMAL);
            anim("griffes_ombre");
            son(SoundEvents.ENTITY_ENDERMEN_TELEPORT, 3F, 1.2F);
        }
        if (t == FONDU + INVISIBLE + 9) frapperDevant(coup("griffes_ombre"));
    }

    @Override
    protected void finCoup() {
        if (mode() == CACHE) mode(NORMAL);
        if (isInvisible()) setInvisible(false);
        super.finCoup();
    }

    // ================================================================== vœu maudit
    /** Il exauce un « vœu » à un joueur : un effet néfaste au hasard. */
    private void voeu(Coup c) {
        EntityPlayer p = cible != null && valable(cible) ? cible : world.getClosestPlayer(posX, posY, posZ, portee(c.id), false);
        if (p == null || !valable(p)) return;
        String[] v = VOEUX[rand.nextInt(VOEUX.length)];
        Stats.Attaque a = st().attaque(c.id);
        int ticks = (int) Math.max(40, a.dureeEffet * 20);
        if ("inverse".equals(v[0])) {
            if (Registre.INVERSE != null) Effets.effet(p, Registre.INVERSE, ticks, 0);
        } else {
            Effets.appliquer(p, this, v[0], a.dureeEffet, false);
        }
        p.sendMessage(new TextComponentString("§5" + espece.nom + " exauce votre vœu… §d" + v[1] + " !"));
        annoncer("§5" + espece.nom + " exauce un vœu maudit à " + p.getName() + " : " + v[1]);
        if (world instanceof WorldServer)
            ((WorldServer) world).spawnParticle(EnumParticleTypes.SPELL_WITCH, p.posX, p.posY + 1, p.posZ, 30, 0.4, 0.8, 0.4, 0.05);
        son(Sons.CHUCHOTEMENT, 4F, 0.8F);
    }

    // ================================================================== effets
    @Override
    protected void effetsClient(String anim) {
        if (isInvisible()) return;
        if ("cri".equals(anim) && tAnim > 8 && tAnim < 24) {
            Vec3d z = zone();
            float r = rayonZone() * (tAnim - 8) / 16F;
            for (int i = 0; i < 16; i++) {
                double an = rand.nextDouble() * Math.PI * 2;
                world.spawnParticle(EnumParticleTypes.SPELL_WITCH, z.x + Math.cos(an) * r, z.y + (rand.nextDouble() - 0.5) * 2, z.z + Math.sin(an) * r, 0, 0, 0);
            }
        }
        // brume noire autour de lui
        for (int i = 0; i < 2; i++)
            world.spawnParticle(EnumParticleTypes.SPELL_MOB, posX + (rand.nextDouble() - 0.5) * width * 2, posY + rand.nextDouble() * height,
                    posZ + (rand.nextDouble() - 0.5) * width * 2, 0, 0, 0);
        if (rand.nextInt(3) == 0)
            world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX + (rand.nextDouble() - 0.5) * width, posY + rand.nextDouble() * height * 0.4,
                    posZ + (rand.nextDouble() - 0.5) * width, 0, 0.01, 0);
    }

    @Override
    public String resume() {
        return super.resume() + ", lumière " + lumiere() + (isInvisible() ? ", invisible" : "");
    }
}
