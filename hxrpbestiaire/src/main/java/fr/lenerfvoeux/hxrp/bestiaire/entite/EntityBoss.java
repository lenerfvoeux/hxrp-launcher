package fr.lenerfvoeux.hxrp.bestiaire.entite;

import fr.lenerfvoeux.hxrp.bestiaire.Durees;
import fr.lenerfvoeux.hxrp.bestiaire.Espece;
import fr.lenerfvoeux.hxrp.bestiaire.HxrpBestiaire;
import fr.lenerfvoeux.hxrp.bestiaire.Registre;
import fr.lenerfvoeux.hxrp.bestiaire.stats.Stats;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Un boss du bestiaire. Règles communes (celles du Phénix) : il ne s'en prend qu'aux joueurs en survie, ne poursuit
 * pas au-delà de son arène (le point d'arrivée ou l'appât), enchaîne ses attaques selon le classeur (poids, recharge,
 * phase, portée), barre rouge puis orange (phase 2, sous 50 %) puis violette (phase 3 : à 0 PV il reprend toute sa
 * vie, pour les boss qui en ont une). Sa vie dépasse le plafond vanilla de 1024 : elle est tenue « à l'échelle ».
 * Ses sbires disparaissent avec lui. Le butin vient du classeur (onglet Butins).
 *
 * Chaque boss décrit ses attaques ({@link #coups()}) et ce qu'elles font ({@link #executer}) ; l'animation de chaque
 * attaque est synchronisée avec le client et sa durée lue dans durees.properties.
 */
public abstract class EntityBoss extends EntityMob implements Bestiole {
    public static final byte NORMAL = 0, ARRIVEE = 1, RENAISSANCE = 2, CACHE = 3;

    private static final DataParameter<String> ANIM = EntityDataManager.createKey(EntityBoss.class, DataSerializers.STRING);
    private static final DataParameter<Integer> ANIM_N = EntityDataManager.createKey(EntityBoss.class, DataSerializers.VARINT);
    private static final DataParameter<Byte> PHASE = EntityDataManager.createKey(EntityBoss.class, DataSerializers.BYTE);
    private static final DataParameter<Byte> MODE = EntityDataManager.createKey(EntityBoss.class, DataSerializers.BYTE);
    /** Point et rayon d'un effet en cours (rayon, onde, zone visée…), pour les particules du client. */
    private static final DataParameter<Float> ZX = EntityDataManager.createKey(EntityBoss.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> ZY = EntityDataManager.createKey(EntityBoss.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> ZZ = EntityDataManager.createKey(EntityBoss.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> ZR = EntityDataManager.createKey(EntityBoss.class, DataSerializers.FLOAT);
    /** Second point (le bout du second rayon d'une attaque à deux rayons). */
    private static final DataParameter<Float> Z2X = EntityDataManager.createKey(EntityBoss.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> Z2Y = EntityDataManager.createKey(EntityBoss.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> Z2Z = EntityDataManager.createKey(EntityBoss.class, DataSerializers.FLOAT);

    /** Une attaque : l'id du classeur (onglet Attaques), son animation et le moment de son effet. */
    public static final class Coup {
        public final String id, anim;
        public final Portee portee;
        /** Moments de l'effet, en part de la durée de l'animation. */
        public final double[] impacts;
        /** Durée imposée en ticks (sinon celle de l'animation). */
        int duree = -1;

        public Coup(String id, String anim, Portee portee, double... impacts) {
            this.id = id;
            this.anim = anim;
            this.portee = portee;
            this.impacts = impacts.length == 0 ? new double[]{0.5} : impacts;
        }

        public Coup duree(int ticks) {
            this.duree = ticks;
            return this;
        }
    }

    /** CONTACT : à portée de coup ; DISTANCE : à portée de tir ; PARTOUT : sans condition de distance. */
    public enum Portee { CONTACT, DISTANCE, PARTOUT }

    protected final Espece espece;
    private final AnimationFactory usine = new AnimationFactory(this);
    private final BossInfoServer barre;
    private final Set<EntityPlayerMP> suivis = new HashSet<>();

    // serveur
    protected double ax, ay, az;
    protected boolean ancre;
    protected int phase = 1;
    /** PV par point de vie Minecraft (voir la classe). Sans initialiseur : posé pendant le constructeur. */
    private double echelle;
    protected EntityPlayer cible;
    protected Coup coup;
    protected int tCoup, dureeCoup;
    private int pause = 40;
    private final Map<String, Integer> dernierUsage = new HashMap<>();
    private final Set<UUID> sbires = new HashSet<>();
    protected BlockPos appat;
    protected boolean renaissanceFaite;
    protected int tMode;
    private int sansCible, depuisDegats = 1000;
    private String derniere = "";
    /** Saut en cloche en cours (attaques bondissantes, arrivée d'un saut) : départ, arrivée, avancement. */
    private Vec3d bondDe, bondVers;
    private int bondT, bondDuree, atterri = -1;
    private double bondHaut;
    private boolean bondTraverse;

    // client
    private int animVue = -1, animEffets = -1;
    /** Côté client : ticks depuis le début de l'animation ponctuelle en cours. */
    protected int tAnim;

    protected EntityBoss(World w, Espece e) {
        super(w);
        this.espece = e;
        setSize(e.largeur, e.hauteur);
        barre = new BossInfoServer(new TextComponentString(e.nom), BossInfo.Color.RED, BossInfo.Overlay.NOTCHED_10);
        experienceValue = 0;
        enablePersistence();
        attributsDePhase(1);
        setHealth(getMaxHealth());
        if (e.sansGravite()) setNoGravity(true);
        stepHeight = Math.max(1.0F, e.hauteur / 4F);
    }

    @Override
    public Espece espece() {
        return espece;
    }

    public Stats.Boss st() {
        return Stats.boss(espece.id);
    }

    /** Les attaques du boss (l'id doit exister dans l'onglet Attaques). */
    protected abstract List<Coup> coups();

    /** Chaque tick d'une attaque ; t compte depuis 1. Aux moments d'impact, {@link #impact} est appelé avant. */
    protected void executer(Coup c, int t) {
    }

    /** Au moment de l'effet : par défaut, les attaques au contact frappent devant le boss. */
    protected void impact(Coup c, int numero) {
        if (c.portee == Portee.CONTACT) frapperDevant(c);
    }

    /** Peut-il lancer cette attaque maintenant ? (en plus de la phase, de la recharge et de la portée) */
    protected boolean peut(Coup c, double distance) {
        return true;
    }

    // ================================================================== attributs, données
    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(64.0);
    }

    /** Vie maximale (à l'échelle), armure, robustesse, recul et vitesse de la phase (onglet Boss). */
    protected void attributsDePhase(int n) {
        Stats.Phase p = st().phase(n);
        double avant = echelle;
        echelle = Math.max(1, Math.ceil(p.pv / 1024.0));
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(p.pv / echelle);
        getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(Math.min(30, p.armure));
        getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).setBaseValue(Math.min(20, p.robustesse));
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(Math.min(1, p.recul));
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(p.vitesse);
        if (avant > 0 && avant != echelle) setHealth((float) (getHealth() * avant / echelle));
    }

    protected void entrerPhase(int n) {
        phase = n;
        dataManager.set(PHASE, (byte) n);
        attributsDePhase(n);
    }

    /** Après /bestiaire recharger. */
    public void reglagesRelus() {
        attributsDePhase(phase);
        if (getHealth() > getMaxHealth()) setHealth(getMaxHealth());
    }

    public int phase() {
        return world.isRemote ? dataManager.get(PHASE) : phase;
    }

    public byte mode() {
        return dataManager.get(MODE);
    }

    protected void mode(byte m) {
        dataManager.set(MODE, m);
        tMode = 0;
        atterri = -1;
    }

    public double pv() {
        return getHealth() * Math.max(1, echelle);
    }

    public double pvMax() {
        return getMaxHealth() * Math.max(1, echelle);
    }

    /** Dégâts d'une attaque dans la phase en cours (dégâts × multiplicateur de la phase). */
    protected float degats(String id) {
        return (float) (st().attaque(id).degats * st().phase(phase).degats);
    }

    /** Portée d'une attaque, allongée par le multiplicateur de la phase. */
    protected double portee(String id) {
        return st().attaque(id).portee * st().phase(phase).portee;
    }

    protected double rayon(String id) {
        return st().attaque(id).rayon;
    }

    protected boolean pret(String id) {
        Integer t = dernierUsage.get(id);
        return t == null || ticksExisted - t >= recharge(id) * 20;
    }

    /** Recharge d'une attaque en secondes (classeur) ; un boss peut la raccourcir selon sa phase. */
    protected double recharge(String id) {
        return st().attaque(id).recharge;
    }

    /** Pause avant la prochaine attaque (ticks). */
    protected void pause(int ticks) {
        pause = ticks;
    }

    /** Vitesse de déplacement par rapport à la vitesse « 0,28 » du classeur. */
    protected double allure() {
        return st().phase(phase).vitesse / 0.28;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(ANIM, "");
        dataManager.register(ANIM_N, 0);
        dataManager.register(PHASE, (byte) 1);
        dataManager.register(MODE, NORMAL);
        dataManager.register(ZX, 0F);
        dataManager.register(ZY, 0F);
        dataManager.register(ZZ, 0F);
        dataManager.register(ZR, 0F);
        dataManager.register(Z2X, 0F);
        dataManager.register(Z2Y, 0F);
        dataManager.register(Z2Z, 0F);
    }

    protected void zone(double x, double y, double z, double r) {
        dataManager.set(ZX, (float) x);
        dataManager.set(ZY, (float) y);
        dataManager.set(ZZ, (float) z);
        dataManager.set(ZR, (float) r);
    }

    public Vec3d zone() {
        return new Vec3d(dataManager.get(ZX), dataManager.get(ZY), dataManager.get(ZZ));
    }

    public float rayonZone() {
        return dataManager.get(ZR);
    }

    protected void zone2(Vec3d p) {
        dataManager.set(Z2X, (float) p.x);
        dataManager.set(Z2Y, (float) p.y);
        dataManager.set(Z2Z, (float) p.z);
    }

    public Vec3d zone2() {
        return new Vec3d(dataManager.get(Z2X), dataManager.get(Z2Y), dataManager.get(Z2Z));
    }

    @Override
    public String animationCourante() {
        return dataManager.get(ANIM);
    }

    /** Lance une animation ponctuelle chez les clients (chaîne vide : retour au repos ou à la marche). */
    protected void anim(String court) {
        dataManager.set(ANIM, court == null ? "" : court);
        dataManager.set(ANIM_N, dataManager.get(ANIM_N) + 1);
    }

    protected int duree(String anim, int defaut) {
        return Durees.ticks(espece.modele, anim, defaut);
    }

    // ================================================================== comportement de base
    @Override
    protected void initEntityAI() {
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    public boolean isNonBoss() {
        return false;
    }

    @Override
    public boolean canBeLeashedTo(EntityPlayer p) {
        return false;
    }

    @Override
    public void fall(float distance, float multiplicateur) {
    }

    @Override
    public boolean isPushedByWater() {
        return espece.deplacement == Espece.Deplacement.SOL;
    }

    @Override
    protected boolean canBeRidden(Entity e) {
        return false;
    }

    @Override
    public void travel(float strafe, float vertical, float forward) {
        if (espece.sansGravite() && isServerWorld() && hasNoGravity()) {
            move(MoverType.SELF, motionX, motionY, motionZ);
            motionX *= 0.91;
            motionY *= 0.91;
            motionZ *= 0.91;
            if (collidedHorizontally) motionY = Math.max(motionY, 0.15);
            return;
        }
        super.travel(strafe, vertical, forward);
    }

    // ================================================================== dégâts, renaissance, mort
    @Override
    public boolean attackEntityFrom(DamageSource src, float n) {
        if (world.isRemote || isEntityInvulnerable(src)) return false;
        byte m = mode();
        // /kill (hors du monde) passe toujours
        if (src != DamageSource.OUT_OF_WORLD && (m == RENAISSANCE || m == CACHE || (m == ARRIVEE && espece.arrivee == Espece.Arrivee.SUR_PLACE)))
            return false;
        if (src == DamageSource.IN_WALL || src == DamageSource.CRAMMING || src == DamageSource.DROWN) return false;
        if (src.getTrueSource() instanceof EntitySbire || src.getTrueSource() instanceof EntityBoss) return false;
        Entity e = src.getTrueSource();
        if (e instanceof EntityPlayer && valable((EntityPlayer) e) && getDistanceSq(e) < 48 * 48) {
            cible = (EntityPlayer) e;
            if (m == ARRIVEE) finirArrivee(false);
        }
        depuisDegats = 0;
        return super.attackEntityFrom(src, n);
    }

    /** Après l'armure (calculée sur les vrais dégâts) : ramenés à l'échelle de sa vie. */
    @Override
    protected float applyPotionDamageCalculations(DamageSource src, float n) {
        return super.applyPotionDamageCalculations(src, n) / (float) Math.max(1, echelle);
    }

    @Override
    protected void damageEntity(DamageSource src, float n) {
        super.damageEntity(src, n);
        if (getHealth() <= 0 && st().troisPhases() && !renaissanceFaite) {
            setHealth(1F);
            commencerRenaissance();
        }
    }

    /** 3e phase : il s'effondre (animation de mort simulée), puis se relève avec toute sa vie. */
    protected void commencerRenaissance() {
        renaissanceFaite = true;
        if (enSaut()) finSaut();
        coup = null;
        mode(RENAISSANCE);
        anim(animRenaissance());
        son(espece.sonMort, 3F, espece.hauteurSon * 0.8F);
        annoncer("§5" + espece.nom + " s'effondre… §cmais quelque chose remue encore.");
    }

    protected String animRenaissance() {
        return espece.mort;
    }

    protected int dureeRenaissance() {
        return 80;
    }

    protected void renaitre() {
        entrerPhase(3);
        setHealth((float) (getMaxHealth() * st().phase(3).vieReprise));
        mode(NORMAL);
        anim("");
        pause = 20;
        son(espece.sonAmbiant, 5F, espece.hauteurSon * 0.7F);
        annoncer("§5§l" + espece.nom + " se relève avec toute sa vie, plus enragé" + espece.e() + " que jamais !");
    }

    @Override
    public void onDeath(DamageSource src) {
        super.onDeath(src);
        if (!world.isRemote) {
            retirerSbires();
            annoncer("§6" + espece.nom + " est vaincu" + espece.e() + ".");
        }
    }

    @Override
    protected void onDeathUpdate() {
        ++deathTime;
        int fin = Math.min(100, duree(espece.mort, 40));
        if (deathTime >= fin && !world.isRemote) {
            if (world instanceof WorldServer)
                ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX, posY + height / 2, posZ, 40, width / 2, height / 3, width / 2, 0.05);
            setDead();
        }
    }

    @Override
    protected void dropFewItems(boolean touche, int butin) {
        List<String> lache = new ArrayList<>();
        for (Stats.Butin b : st().butins) {
            if (rand.nextDouble() >= b.chance) continue;
            Item i = Registre.butin(b.objet);
            if (i == null) {
                HxrpBestiaire.LOG.warn("Bestiaire : butin inconnu « {} » pour {}", b.objet, espece.id);
                continue;
            }
            int n = b.min + (b.max > b.min ? rand.nextInt(b.max - b.min + 1) : 0);
            for (int k = 0; k < n; k++) entityDropItem(new ItemStack(i), 0.5F);
            if (n > 0) lache.add(n + " × " + new ItemStack(i).getDisplayName());
        }
        HxrpBestiaire.LOG.info("Bestiaire : butin de {} : {}", espece.nom, lache.isEmpty() ? "rien" : String.join(", ", lache));
    }

    @Override
    protected void dropEquipment(boolean touche, int butin) {
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return mode() == CACHE ? null : espece.sonAmbiant;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return espece.sonBlesse;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return espece.sonMort;
    }

    @Override
    protected float getSoundVolume() {
        return 3.0F;
    }

    @Override
    protected float getSoundPitch() {
        return espece.hauteurSon * (0.9F + rand.nextFloat() * 0.2F);
    }

    @Override
    public int getTalkInterval() {
        return 260;
    }

    protected void son(SoundEvent s, float volume, float hauteur) {
        if (s != null) world.playSound(null, posX, posY + height / 2, posZ, s, SoundCategory.HOSTILE, volume, hauteur);
    }

    protected void annoncer(String msg) {
        if (world.isRemote) return;
        HxrpBestiaire.LOG.info("{} ({} {} {})", msg.replaceAll("§.", ""), MathHelper.floor(posX), MathHelper.floor(posY), MathHelper.floor(posZ));
        for (EntityPlayer p : world.playerEntities)
            if (p.getDistanceSq(this) < 96 * 96) p.sendMessage(new TextComponentString(msg));
    }

    // ================================================================== barre de boss
    @Override
    public void addTrackingPlayer(EntityPlayerMP p) {
        super.addTrackingPlayer(p);
        suivis.add(p);
    }

    @Override
    public void removeTrackingPlayer(EntityPlayerMP p) {
        super.removeTrackingPlayer(p);
        suivis.remove(p);
        barre.removePlayer(p);
    }

    @Override
    public void setCustomNameTag(String nom) {
        super.setCustomNameTag(nom);
        barre.setName(getDisplayName());
    }

    private void majBarre() {
        barre.setName(hasCustomName() ? getDisplayName() : new TextComponentString(espece.nom));
        // rouge, puis orange (jaune vanilla, peinte en orange par le client), puis violette
        barre.setColor(phase >= 3 ? BossInfo.Color.PURPLE : phase == 2 ? BossInfo.Color.YELLOW : BossInfo.Color.RED);
        barre.setPercent(MathHelper.clamp(getHealth() / getMaxHealth(), 0F, 1F));
        if (ticksExisted % 10 == 0) {
            for (EntityPlayerMP p : new ArrayList<>(suivis)) {
                if (p.isEntityAlive() && p.world == world && p.getDistanceSq(this) < 64 * 64) barre.addPlayer(p);
                else barre.removePlayer(p);
            }
        }
    }

    // ================================================================== boucle
    public void definirAncre(double x, double y, double z) {
        ax = x;
        ay = y;
        az = z;
        ancre = true;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!world.isRemote) {
            if (!ancre) definirAncre(posX, posY, posZ);
            if (espece.deplacement == Espece.Deplacement.NAGE && mode() != CACHE && !enSaut()) flotter();
            majBarre();
        } else if (isEntityAlive()) {
            int n = dataManager.get(ANIM_N);
            if (n != animEffets) {
                animEffets = n;
                tAnim = 0;
            }
            tAnim++;
            effetsClient(animationCourante());
        }
    }

    /** Côté client, chaque tick : les particules de l'attaque en cours (d'après l'animation et la zone synchronisées). */
    protected void effetsClient(String anim) {
    }

    /** Ses propres poisons (bile, dards) ne le touchent pas. */
    @Override
    public boolean isPotionApplicable(net.minecraft.potion.PotionEffect e) {
        return e.getPotion() != net.minecraft.init.MobEffects.POISON && e.getPotion() != net.minecraft.init.MobEffects.WITHER
                && super.isPotionApplicable(e);
    }

    @Override
    protected void updateAITasks() {
        tMode++;
        depuisDegats++;
        byte m = mode();
        if (m == RENAISSANCE) {
            motionX = motionZ = 0;
            getNavigator().clearPath();
            if (tMode >= dureeRenaissance()) renaitre();
            return;
        }
        if (m == ARRIVEE) {
            arriver();
            return;
        }
        choisirCible();
        if (phase < 2 && st().deuxPhases() && getHealth() < getMaxHealth() * st().phase(2).declenche) passerPhase2();
        if (coup != null) {
            tCoup++;
            for (int k = 0; k < coup.impacts.length; k++)
                if (tCoup == Math.max(1, (int) Math.round(coup.impacts[k] * dureeCoup))) impact(coup, k);
            if (coup != null) executer(coup, tCoup);
            if (coup != null && tCoup >= dureeCoup) finCoup();
            return;
        }
        deplacer();
        if (cible != null && --pause <= 0) {
            Coup c = choisirCoup();
            if (c != null) commencer(c);
            else pause = 10;
        }
        if (cible == null && depuisDegats > 1200 && ticksExisted % 20 == 0 && getHealth() < getMaxHealth()) heal(getMaxHealth() * 0.005F);
    }

    protected void passerPhase2() {
        entrerPhase(2);
        son(espece.sonAmbiant, 5F, espece.hauteurSon * 0.75F);
        annoncer("§6" + espece.nom + " entre en rage !");
        if (world instanceof WorldServer)
            ((WorldServer) world).spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, posX, posY + height / 2, posZ, 6, width / 2, height / 3, width / 2, 0);
    }

    // ================================================================== cible et arène
    public static boolean valable(EntityPlayer p) {
        return p != null && p.isEntityAlive() && !p.isSpectator() && !p.isCreative();
    }

    protected double distAncreSq(Entity e) {
        double dx = e.posX - ax, dz = e.posZ - az;
        return dx * dx + dz * dz;
    }

    protected void choisirCible() {
        double arene = st().rayonArene, rayon = st().rayonCible;
        if (cible != null && (!valable(cible) || cible.world != world || getDistanceSq(cible) > (rayon + 12) * (rayon + 12)
                || distAncreSq(cible) > arene * arene))
            cible = null;
        if (cible == null && ticksExisted % 10 == 0) {
            EntityPlayer mieux = null;
            double md = rayon * rayon;
            for (EntityPlayer p : world.playerEntities) {
                if (!valable(p)) continue;
                double d = getDistanceSq(p);
                if (d < md && distAncreSq(p) < arene * arene) {
                    mieux = p;
                    md = d;
                }
            }
            if (mieux != null && sansCible > 40) son(espece.sonAmbiant, 4F, espece.hauteurSon);
            cible = mieux;
        }
        setAttackTarget(cible);
        sansCible = cible == null ? sansCible + 1 : 0;
    }

    /** Distance horizontale entre les bords (les boss sont larges). */
    protected double distance(Entity e) {
        double dx = e.posX - posX, dz = e.posZ - posZ;
        return Math.max(0, Math.sqrt(dx * dx + dz * dz) - width / 2);
    }

    // ================================================================== déplacements
    /** Hors attaque : rejoindre la cible (ou revenir à l'arène). */
    protected void deplacer() {
        if (espece.deplacement == Espece.Deplacement.NAGE) {
            if (cible != null) {
                double a = Math.atan2(posZ - cible.posZ, posX - cible.posX), d = distanceNage();
                nager(cible.posX + Math.cos(a) * d, cible.posZ + Math.sin(a) * d, 0.35);
                regarder(cible.posX, cible.posZ, 8F);
            } else {
                nager(ax, az, 0.25);
                suivreCap();
            }
            return;
        }
        if (espece.deplacement == Espece.Deplacement.VOL) {
            if (cible != null) {
                double a = Math.atan2(posZ - cible.posZ, posX - cible.posX) + 0.02;
                double d = distanceVol();
                voler(cible.posX + Math.cos(a) * d, cible.posY + hauteurVol(), cible.posZ + Math.sin(a) * d, 0.5, 0.12);
                regarder(cible.posX, cible.posZ, 12F);
            } else {
                double y = Math.max(ay + 4, sol(ax, az) + 4);
                voler(ax + Math.cos(ticksExisted * 0.02) * 6, y, az + Math.sin(ticksExisted * 0.02) * 6, 0.3, 0.08);
                suivreCap();
            }
            return;
        }
        if (cible != null) {
            if (distance(cible) > distanceContact() * 0.8) {
                if (ticksExisted % 10 == 0 || getNavigator().noPath()) getNavigator().tryMoveToEntityLiving(cible, 1.0);
            } else {
                getNavigator().clearPath();
            }
            getLookHelper().setLookPositionWithEntity(cible, 30F, 30F);
            regarder(cible.posX, cible.posZ, 10F);
        } else if (ancre && (posX - ax) * (posX - ax) + (posZ - az) * (posZ - az) > 6 * 6) {
            if (ticksExisted % 20 == 0) getNavigator().tryMoveToXYZ(ax, ay, az, 0.8);
        }
    }

    /** Distance à laquelle il peut frapper au contact : la portée la plus courte de ses coups au contact. */
    protected double distanceContact() {
        double m = 4;
        boolean trouve = false;
        for (Coup c : coups()) {
            if (c.portee != Portee.CONTACT) continue;
            double p = portee(c.id);
            if (!trouve || p < m) m = p;
            trouve = true;
        }
        return m;
    }

    /** Boss nageur : distance horizontale qu'il garde avec sa cible. */
    protected double distanceNage() {
        return Math.max(3, distanceContact() * 0.8);
    }

    /**
     * Surface du liquide de l'espèce dans la colonne (x, z), en cherchant de y + 6 vers le bas sur 20 blocs : le haut du
     * premier bloc de liquide rencontré ; NaN s'il n'y en a pas (ou si le tronçon n'est pas chargé).
     */
    public double surface(double x, double y, double z) {
        BlockPos.MutableBlockPos b = new BlockPos.MutableBlockPos(MathHelper.floor(x), MathHelper.floor(y) + 6, MathHelper.floor(z));
        for (int k = 0; k < 20; k++) {
            if (!world.isBlockLoaded(b)) return Double.NaN;
            if (world.getBlockState(b).getMaterial() == espece.liquide) return b.getY() + 1;
            b.setY(b.getY() - 1);
        }
        return Double.NaN;
    }

    /** Nage vers (tx, tz) sans jamais quitter son liquide : il s'arrête au bord. */
    protected void nager(double tx, double tz, double vitesse) {
        double dx = tx - posX, dz = tz - posZ, d = Math.sqrt(dx * dx + dz * dz);
        double vx = 0, vz = 0;
        if (d > 0.5) {
            double v = Math.min(vitesse * allure(), d * 0.2);
            vx = dx / d * v;
            vz = dz / d * v;
            // la colonne où il va (bord compris) doit contenir son liquide
            double bord = width / 2 + 1;
            if (Double.isNaN(surface(posX + dx / d * bord + vx * 4, posY, posZ + dz / d * bord + vz * 4))) vx = vz = 0;
        }
        motionX += (vx - motionX) * 0.15;
        motionZ += (vz - motionZ) * 0.15;
    }

    /** Boss nageur : il se tient à sa hauteur de flottaison (sinon, hors de son liquide, il tombe). */
    protected void flotter() {
        double s = surface(posX, posY, posZ);
        if (Double.isNaN(s)) {
            motionY = Math.max(motionY - 0.08, -0.6);
            return;
        }
        double cible = s + espece.flottaison;
        motionY += ((cible - posY) * 0.2 - motionY) * 0.4;
    }

    /** Boss volant : distance horizontale à sa cible pendant les déplacements. */
    protected double distanceVol() {
        return Math.max(3, distanceContact() * 0.7);
    }

    /** Boss volant : hauteur au-dessus de sa cible. */
    protected double hauteurVol() {
        return 1.5;
    }

    protected void voler(double tx, double ty, double tz, double vitesse, double agilite) {
        vitesse *= allure();
        double dx = tx - posX, dy = ty - posY, dz = tz - posZ;
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double vx = 0, vy = 0, vz = 0;
        if (d > 0.05) {
            double v = Math.min(vitesse, d * 0.3 + 0.02);
            vx = dx / d * v;
            vy = dy / d * v;
            vz = dz / d * v;
        }
        motionX += (vx - motionX) * agilite;
        motionY += (vy - motionY) * agilite;
        motionZ += (vz - motionZ) * agilite;
    }

    protected void tourner(float cap, float max) {
        float d = MathHelper.wrapDegrees(cap - rotationYaw);
        rotationYaw += MathHelper.clamp(d, -max, max);
        renderYawOffset = rotationYaw;
        rotationYawHead = rotationYaw;
    }

    protected void regarder(double x, double z, float max) {
        tourner((float) (MathHelper.atan2(z - posZ, x - posX) * (180.0 / Math.PI)) - 90F, max);
    }

    protected void suivreCap() {
        if (motionX * motionX + motionZ * motionZ > 0.0025)
            tourner((float) (MathHelper.atan2(motionZ, motionX) * (180.0 / Math.PI)) - 90F, 9F);
    }

    /**
     * Lance un saut en cloche vers un point : il s'y pose en {@code duree} ticks, le sommet de la courbe à {@code haut}
     * blocs au-dessus de la droite. {@code traverser} : il passe à travers ce qui se trouve en chemin (arrivée de loin).
     */
    protected void sauter(Vec3d vers, int duree, double haut, boolean traverser) {
        bondDe = getPositionVector();
        bondVers = vers;
        bondT = 0;
        bondDuree = Math.max(4, duree);
        bondHaut = haut;
        bondTraverse = traverser;
        setNoGravity(true);
        noClip = traverser;
        motionX = motionY = motionZ = 0;
        getNavigator().clearPath();
    }

    public boolean enSaut() {
        return bondVers != null;
    }

    /** Un tick du saut : 0 en l'air, 1 posé, 2 arrêté net par un mur (le saut est alors fini). */
    protected int bondir() {
        if (bondVers == null) return 1;
        bondT++;
        double u = Math.min(1, bondT / (double) bondDuree);
        double x = bondDe.x + (bondVers.x - bondDe.x) * u, z = bondDe.z + (bondVers.z - bondDe.z) * u;
        double y = bondDe.y + (bondVers.y - bondDe.y) * u + bondHaut * 4 * u * (1 - u);
        tourner((float) (MathHelper.atan2(bondVers.z - bondDe.z, bondVers.x - bondDe.x) * (180.0 / Math.PI)) - 90F, 20F);
        if (bondTraverse) {
            setPosition(x, y, z);
        } else {
            move(MoverType.SELF, x - posX, y - posY, z - posZ);
            if (collidedHorizontally && u < 0.85) {
                finSaut();
                return 2;
            }
        }
        if (u >= 1) {
            finSaut();
            return 1;
        }
        return 0;
    }

    protected void finSaut() {
        bondVers = null;
        noClip = false;
        if (!espece.sansGravite()) setNoGravity(false);
        motionX = motionY = motionZ = 0;
    }

    /** Hauteur du sol (premier bloc qui arrête la vue) ; la position actuelle si le tronçon n'est pas chargé. */
    protected double sol(double x, double z) {
        BlockPos p = new BlockPos(x, 0, z);
        if (!world.isBlockLoaded(p)) return posY - 4;
        return world.getHeight(p).getY();
    }

    /** Le point visé : les pieds de la cible, ou (attaque forcée sans cible) le sol à {@code d} blocs devant lui. */
    protected Vec3d visee(double d) {
        if (cible != null) return new Vec3d(cible.posX, cible.posY, cible.posZ);
        Vec3d f = devant();
        double x = posX + f.x * (width / 2 + d), z = posZ + f.z * (width / 2 + d);
        return new Vec3d(x, sol(x, z), z);
    }

    /** Vecteur unitaire devant le boss (à plat). */
    protected Vec3d devant() {
        float a = rotationYaw * (float) Math.PI / 180F;
        return new Vec3d(-MathHelper.sin(a), 0, MathHelper.cos(a));
    }

    // ================================================================== choix et déroulement des attaques
    protected Coup choisirCoup() {
        double d = cible == null ? 999 : distance(cible);
        List<Coup> sac = new ArrayList<>();
        for (Coup c : coups()) {
            Stats.Attaque a = st().attaque(c.id);
            if (a.phase > phase || !pret(c.id) || !peut(c, d)) continue;
            if (c.portee == Portee.CONTACT && d > portee(c.id)) continue;
            if (c.portee == Portee.DISTANCE && d > Math.max(portee(c.id), 6)) continue;
            int w = (int) Math.max(0, Math.round(a.poids));
            if (c.id.equals(derniere)) w = Math.min(w, 1);
            for (int i = 0; i < w; i++) sac.add(c);
        }
        return sac.isEmpty() ? null : sac.get(rand.nextInt(sac.size()));
    }

    public Coup coup(String id) {
        for (Coup c : coups()) if (c.id.equals(id)) return c;
        return null;
    }

    /** Force une attaque (commande /<boss> attaque). */
    public boolean forcer(String id) {
        Coup c = coup(id);
        if (c == null || !isEntityAlive() || mode() != NORMAL) return false;
        commencer(c);
        return true;
    }

    protected void commencer(Coup c) {
        coup = c;
        tCoup = 0;
        dureeCoup = c.duree > 0 ? c.duree : duree(c.anim, 30);
        dernierUsage.put(c.id, ticksExisted);
        derniere = c.id;
        getNavigator().clearPath();
        anim(c.anim);
        debutCoup(c);
    }

    /** Au début d'une attaque (repérer la cible, crier…). */
    protected void debutCoup(Coup c) {
        if (cible != null) regarder(cible.posX, cible.posZ, 180F);
    }

    /** Fin de l'attaque, puis la pause du classeur (plus courte à chaque phase). */
    protected void finCoup() {
        coup = null;
        anim("");
        int p = (int) Math.round(st().phase(phase).pause * 20);
        pause = Math.max(10, p + rand.nextInt(Math.max(1, p / 2 + 1)));
    }

    // ================================================================== effets des attaques
    /** Frappe les joueurs devant lui : à « portée » blocs de son bord, sur une largeur de « rayon » blocs. */
    protected int frapperDevant(Coup c) {
        return frapperDevant(c, 1F);
    }

    /** Idem, avec une part des dégâts (coups en rafale). */
    protected int frapperDevant(Coup c, float part) {
        double portee = portee(c.id) + width / 2, demiLargeur = Math.max(1, rayon(c.id)) + 0.5;
        Vec3d f = devant();
        int n = 0;
        for (EntityPlayer p : world.getEntitiesWithinAABB(EntityPlayer.class, getEntityBoundingBox().grow(portee + 2, 3, portee + 2))) {
            if (!valable(p)) continue;
            double dx = p.posX - posX, dz = p.posZ - posZ;
            double avant = dx * f.x + dz * f.z, cote = Math.abs(dx * f.z - dz * f.x);
            if (avant < -1 || avant > portee || cote > demiLargeur + width / 2) continue;
            if (p.posY > posY + height + 1 || p.posY + p.height < posY - 1) continue;
            if (toucher(p, c, part)) n++;
        }
        return n;
    }

    /** Frappe les joueurs dans un rayon autour d'un point (dégâts dégressifs vers le bord). */
    protected int frapperAutour(Coup c, Vec3d centre, double r) {
        int n = 0;
        for (EntityPlayer p : world.getEntitiesWithinAABB(EntityPlayer.class, new AxisAlignedBB(centre.x - r, centre.y - r, centre.z - r,
                centre.x + r, centre.y + r, centre.z + r))) {
            if (!valable(p)) continue;
            double d = p.getDistance(centre.x, centre.y, centre.z);
            if (d > r) continue;
            if (toucher(p, c, (float) (1 - 0.4 * d / r))) n++;
        }
        return n;
    }

    /** Dégâts de l'attaque (× part) et ses effets (classeur) sur un joueur. */
    protected boolean toucher(EntityPlayer p, Coup c, float part) {
        if (!valable(p)) return false;
        float d = degats(c.id) * part;
        boolean ok = d <= 0 || p.attackEntityFrom(DamageSource.causeMobDamage(this), d);
        if (ok) {
            Stats.Attaque a = st().attaque(c.id);
            Effets.appliquer(p, this, a.effet, a.dureeEffet, true);
            touche(p, c);
        }
        return ok;
    }

    /** Après un coup réussi (maladie, soin…). */
    protected void touche(EntityPlayer p, Coup c) {
    }

    /** Appelle des sbires (onglet Sbires) autour de lui ; rend le nombre apparu. */
    protected int invoquer(String idSbire, Espece especeSbire, double rayon) {
        Stats.Sbire s = st().sbire(idSbire);
        int n = s.nombre(rand, phase);
        int faits = 0;
        for (int i = 0; i < n && sbires.size() < 40; i++) {
            double a = rand.nextDouble() * Math.PI * 2, r = 2 + rand.nextDouble() * rayon;
            double x = posX + Math.cos(a) * r, z = posZ + Math.sin(a) * r;
            double y = especeSbire.deplacement == Espece.Deplacement.VOL ? posY + 1 + rand.nextDouble() * 2 : sol(x, z);
            EntitySbire e = EntitySbire.creer(especeSbire, world);
            if (e == null) break;
            e.setLocationAndAngles(x, y, z, rand.nextFloat() * 360F, 0);
            e.lier(this, s);
            if (world.spawnEntity(e)) {
                sbires.add(e.getUniqueID());
                faits++;
                if (world instanceof WorldServer)
                    ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_LARGE, x, y + 0.5, z, 8, 0.4, 0.4, 0.4, 0.02);
            }
        }
        return faits;
    }

    public int sbiresVivants() {
        int n = 0;
        if (world instanceof WorldServer)
            for (UUID u : new ArrayList<>(sbires)) {
                Entity e = ((WorldServer) world).getEntityFromUuid(u);
                if (e != null && e.isEntityAlive()) n++;
                else sbires.remove(u);
            }
        return n;
    }

    public void retirerSbires() {
        if (!(world instanceof WorldServer)) return;
        for (UUID u : sbires) {
            Entity e = ((WorldServer) world).getEntityFromUuid(u);
            if (e != null) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_NORMAL, e.posX, e.posY + 0.5, e.posZ, 10, 0.3, 0.3, 0.3, 0.02);
                e.setDead();
            }
        }
        sbires.clear();
    }

    // ================================================================== appât
    /**
     * L'appât a fait son effet : le boss apparaît, selon son espèce, loin de l'appât (en vol ou à pied) ou dessus
     * (il surgit), puis le dévore et attaque. {@code cap} : direction (radians, depuis l'appât) d'où il arrive.
     */
    public static EntityBoss attirer(Espece e, World w, BlockPos nid, double cap) {
        EntityBoss b = creer(e, w);
        if (b == null) return null;
        Stats.Boss s = b.st();
        double nx = nid.getX() + 0.5, ny = nid.getY(), nz = nid.getZ() + 0.5;
        double x = nx, y = ny, z = nz;
        if (e.arrivee == Espece.Arrivee.SUR_PLACE) {
            Vec3d p = b.pointDeSurgissement(nid);
            x = p.x;
            y = p.y;
            z = p.z;
        } else if (e.arrivee == Espece.Arrivee.PLAFOND) {
            // sous le premier bloc plein au-dessus de l'appât (24 blocs au plus) ; à ciel ouvert, 16 blocs plus haut
            int haut = 16;
            for (int k = 3; k <= 24; k++)
                if (w.getBlockState(nid.up(k)).getMaterial().blocksMovement()) {
                    haut = k;
                    break;
                }
            y = ny + Math.max(1, haut - e.hauteur - 0.2);
            b.setNoGravity(true);
        } else if (e.arrivee != Espece.Arrivee.SUR_PLACE) {
            double[] p = depart(w, nid, cap, s.appatDistanceMin, s.appatDistanceMax, e.arrivee == Espece.Arrivee.VOL);
            x = p[0];
            y = p[1];
            z = p[2];
        }
        float lacet = (float) (MathHelper.atan2(nz - z, nx - x) * (180.0 / Math.PI)) - 90F;
        boolean surPlace = e.arrivee == Espece.Arrivee.SUR_PLACE || e.arrivee == Espece.Arrivee.PLAFOND;
        b.setLocationAndAngles(x, y, z, surPlace ? w.rand.nextFloat() * 360F : lacet, 0F);
        b.rotationYawHead = b.rotationYaw;
        b.renderYawOffset = b.rotationYaw;
        if (e.deplacement == Espece.Deplacement.NAGE) b.definirAncre(x, y, z);
        else b.definirAncre(nx, ny, nz);
        b.appat = nid;
        b.mode(ARRIVEE);
        if (e.arrivee == Espece.Arrivee.SUR_PLACE && !e.apparition.isEmpty()) b.anim(e.apparition);
        w.spawnEntity(b);
        b.son(e.sonAmbiant, 6F, e.hauteurSon * 0.8F);
        HxrpBestiaire.LOG.info("Bestiaire : {} attiré{} par l'appât en {} {} {}, part de {} {} {} ({} blocs)", e.nom, e.e(), nid.getX(), nid.getY(),
                nid.getZ(), MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z), (int) Math.round(Math.sqrt((x - nx) * (x - nx) + (z - nz) * (z - nz))));
        return b;
    }

    /** Arrivée sur place : où il surgit (sous l'appât, par défaut ; dans son liquide, pour un nageur). */
    protected Vec3d pointDeSurgissement(BlockPos appat) {
        return new Vec3d(appat.getX() + 0.5, appat.getY(), appat.getZ() + 0.5);
    }

    /** Un de ses projectiles vient de toucher (un bloc ou un joueur) en ce point. */
    public void surImpact(EntityProjectileBoss p, Vec3d ou) {
    }

    /**
     * Un rayon droit depuis {@code o} vers {@code dir}, arrêté par le premier bloc plein : chaque joueur à moins de
     * {@code largeur} blocs de la ligne est touché (part des dégâts de l'attaque). Rend le bout du rayon.
     */
    protected Vec3d rayon(Coup c, Vec3d o, Vec3d dir, double portee, double largeur, float part) {
        Vec3d fin = o.add(dir.normalize().scale(portee));
        net.minecraft.util.math.RayTraceResult r = world.rayTraceBlocks(o, fin, false, true, false);
        if (r != null && r.hitVec != null) fin = r.hitVec;
        AxisAlignedBB boite = new AxisAlignedBB(o.x, o.y, o.z, fin.x, fin.y, fin.z).grow(largeur + 1);
        for (EntityPlayer p : world.getEntitiesWithinAABB(EntityPlayer.class, boite)) {
            if (!valable(p)) continue;
            Vec3d m = new Vec3d(p.posX, p.posY + p.height / 2, p.posZ);
            if (distanceSegment(m, o, fin) <= largeur + p.width / 2) toucher(p, c, part);
        }
        return fin;
    }

    protected static double distanceSegment(Vec3d p, Vec3d a, Vec3d b) {
        Vec3d ab = b.subtract(a);
        double l2 = ab.lengthSquared();
        double k = l2 < 1e-9 ? 0 : MathHelper.clamp(p.subtract(a).dotProduct(ab) / l2, 0, 1);
        return p.distanceTo(a.add(ab.scale(k)));
    }

    /** Fait apparaître le boss sur place (œuf, /<boss> invoquer). */
    public static EntityBoss invoquer(Espece e, World w, double x, double y, double z, float cap) {
        EntityBoss b = creer(e, w);
        if (b == null) return null;
        b.setLocationAndAngles(x, y, z, cap, 0F);
        b.rotationYawHead = cap;
        b.renderYawOffset = cap;
        b.definirAncre(x, y, z);
        if (!e.apparition.isEmpty()) {
            b.mode(ARRIVEE);
            b.anim(e.apparition);
        }
        w.spawnEntity(b);
        b.son(e.sonAmbiant, 5F, e.hauteurSon * 0.8F);
        b.annoncer("§c" + e.nom + " apparaît !");
        return b;
    }

    public static EntityBoss creer(Espece e, World w) {
        try {
            return (EntityBoss) e.classe.getConstructor(World.class).newInstance(w);
        } catch (ReflectiveOperationException ex) {
            HxrpBestiaire.LOG.error("Bestiaire : impossible de créer {}", e.id, ex);
            return null;
        }
    }

    /** Point de départ à la distance du classeur, terrain chargé (sinon il resterait figé), sinon au-dessus de l'appât. */
    static double[] depart(World w, BlockPos nid, double cap, double min, double max, boolean vol) {
        double x0 = nid.getX() + 0.5, z0 = nid.getZ() + 0.5;
        double d0 = min + w.rand.nextDouble() * (max - min);
        int plafond = w.getHeight() - 8;
        for (double d = d0; d >= 12; d -= 8) {
            for (int essai = 0; essai < 5; essai++) {
                double an = cap + (essai == 0 ? 0 : (w.rand.nextDouble() - 0.5) * 1.6);
                BlockPos b = new BlockPos(x0 + Math.cos(an) * d, 64, z0 + Math.sin(an) * d);
                if (!w.isAreaLoaded(b, 8)) continue;
                double sol = w.getHeight(b).getY();
                double y = vol ? Math.max(nid.getY() + 24 + w.rand.nextDouble() * 6, sol + 12) : sol;
                return new double[]{b.getX() + 0.5, Math.min(y, plafond), b.getZ() + 0.5};
            }
        }
        return new double[]{x0, Math.min(nid.getY() + (vol ? 24 : 0), plafond), z0};
    }

    /** Pendant l'arrivée : rejoindre l'appât, ou finir de surgir. */
    protected void arriver() {
        if (appat == null) {
            int d = espece.apparition.isEmpty() ? 1 : duree(espece.apparition, 30);
            if (tMode >= d) finirArrivee(false);
            return;
        }
        double nx = appat.getX() + 0.5, ny = appat.getY(), nz = appat.getZ() + 0.5;
        double dx = nx - posX, dz = nz - posZ, h = Math.sqrt(dx * dx + dz * dz);
        switch (espece.arrivee) {
            case SUR_PLACE: {
                int d = espece.apparition.isEmpty() ? 40 : duree(espece.apparition, 40);
                if (tMode >= d) finirArrivee(true);
                return;
            }
            case PLAFOND: {
                // il descend au bout de son fil, lentement, jusqu'à l'appât
                motionX = motionY = motionZ = 0;
                move(MoverType.SELF, 0, -0.14, 0);
                if (posY <= ny + 0.05 || onGround || tMode > 600) finirArrivee(true);
                return;
            }
            case SAUT: {
                // il se ramasse, bondit par-dessus tout, retombe sur l'appât, puis se redresse
                if (tMode == 1) debutSautArrivee();
                if (tMode == ENVOL) sauter(new Vec3d(nx, ny, nz), (int) MathHelper.clamp(h / 2.2, 16, 30), 8 + h * 0.2, true);
                else if (tMode > ENVOL && atterri < 0) {
                    if (bondir() != 0) {
                        atterri = tMode;
                        atterrissage();
                    }
                } else if (atterri >= 0 && tMode >= atterri + 20) finirArrivee(true);
                return;
            }
            case VOL: {
                double ty = h > 6 ? ny + 3 + Math.min(20, h * 0.4) : ny + 1;
                voler(nx, ty, nz, 0.7, 0.15);
                suivreCap();
                break;
            }
            default: {
                if (tMode % 20 == 1 || getNavigator().noPath()) getNavigator().tryMoveToXYZ(nx, ny, nz, 1.2);
                if (tMode > 200 && getNavigator().noPath() && h > 6 && tMode % 100 == 0) {
                    // bloqué : il franchit l'obstacle d'un bond
                    double[] p = depart(world, appat, Math.atan2(posZ - nz, posX - nx), 6, 10, false);
                    setPositionAndUpdate(p[0], p[1], p[2]);
                }
            }
        }
        if (tMode % 80 == 1) son(espece.sonAmbiant, 6F, espece.hauteurSon * 0.85F);
        if ((h < Math.max(2.5, width / 2 + 1) && Math.abs(posY - ny) < 3) || tMode > 1200) finirArrivee(true);
    }

    /** Arrivée d'un saut : moment où il quitte le sol (ticks). */
    protected static final int ENVOL = 12;

    /** Arrivée d'un saut : il se ramasse (animation du saut). */
    protected void debutSautArrivee() {
    }

    /** Arrivée d'un saut : il retombe (onde de choc, réception). */
    protected void atterrissage() {
        if (world instanceof WorldServer)
            ((WorldServer) world).spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, posX, posY + 0.5, posZ, 8, width, 0.3, width, 0);
        son(net.minecraft.init.SoundEvents.ENTITY_GENERIC_EXPLODE, 3F, 0.6F);
    }

    /** Arrivé : il dévore l'appât (gerbe de particules), pousse un cri, et le combat commence. */
    protected void finirArrivee(boolean devorer) {
        if (enSaut()) finSaut();
        noClip = false;
        if (!espece.sansGravite()) setNoGravity(false);
        if (devorer && appat != null) {
            IBlockState s = world.getBlockState(appat);
            if (Registre.estAppat(s.getBlock())) {
                world.setBlockToAir(appat);
                if (world instanceof WorldServer) {
                    WorldServer ws = (WorldServer) world;
                    ws.spawnParticle(EnumParticleTypes.BLOCK_CRACK, appat.getX() + 0.5, appat.getY() + 0.4, appat.getZ() + 0.5, 60, 0.4, 0.3, 0.4, 0.1,
                            net.minecraft.block.Block.getStateId(s));
                    ws.spawnParticle(espece.particuleAppat, appat.getX() + 0.5, appat.getY() + 0.5, appat.getZ() + 0.5, 30, 0.6, 0.4, 0.6, 0.05);
                }
                annoncer("§c" + espece.nom + " " + espece.arriveeAppat);
            }
            // un nageur garde son arène dans son liquide (l'appât est sur la rive ou à la surface)
            if (espece.deplacement != Espece.Deplacement.NAGE) definirAncre(appat.getX() + 0.5, appat.getY(), appat.getZ() + 0.5);
        }
        appat = null;
        mode(NORMAL);
        anim("");
        pause = 30;
        son(espece.sonAmbiant, 6F, espece.hauteurSon * 0.75F);
    }

    // ================================================================== GeckoLib
    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<EntityBoss>(this, "corps", 4, this::predicat));
    }

    private <E extends software.bernie.geckolib3.core.IAnimatable> PlayState predicat(AnimationEvent<E> ev) {
        AnimationBuilder b = new AnimationBuilder();
        String fond = ev.isMoving() || (espece.sansGravite() && motionX * motionX + motionZ * motionZ > 0.01)
                ? espece.marche : espece.repos;
        if (getHealth() <= 0 || deathTime > 0) {
            b.addAnimation(espece.animation(espece.mort), false);
        } else {
            String a = dataManager.get(ANIM);
            int n = dataManager.get(ANIM_N);
            if (!a.isEmpty()) {
                if (n != animVue) ev.getController().markNeedsReload();
                b.addAnimation(espece.animation(a), false).addAnimation(espece.animation(fond), true);
            } else {
                b.addAnimation(espece.animation(fond), true);
            }
            animVue = n;
        }
        ev.getController().setAnimation(b);
        return PlayState.CONTINUE;
    }

    @Override
    public AnimationFactory getFactory() {
        return usine;
    }

    // ================================================================== sauvegarde
    @Override
    public void writeEntityToNBT(NBTTagCompound c) {
        super.writeEntityToNBT(c);
        c.setDouble("AncreX", ax);
        c.setDouble("AncreY", ay);
        c.setDouble("AncreZ", az);
        c.setBoolean("Ancre", ancre);
        c.setByte("Phase", (byte) phase);
        c.setDouble("Echelle", echelle);
        c.setBoolean("Renaissance", renaissanceFaite);
        if (appat != null) c.setLong("Appat", appat.toLong());
        c.setBoolean("Arrivee", mode() == ARRIVEE);
        NBTTagList l = new NBTTagList();
        for (UUID u : sbires) l.appendTag(new NBTTagString(u.toString()));
        c.setTag("Sbires", l);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound c) {
        super.readEntityFromNBT(c);
        ax = c.getDouble("AncreX");
        ay = c.getDouble("AncreY");
        az = c.getDouble("AncreZ");
        ancre = c.getBoolean("Ancre");
        renaissanceFaite = c.getBoolean("Renaissance");
        // la vie enregistrée est en unités de l'échelle d'alors ; les PV du classeur ont pu changer depuis
        echelle = c.hasKey("Echelle") ? Math.max(1, c.getDouble("Echelle")) : 1;
        entrerPhase(MathHelper.clamp(c.getByte("Phase"), 1, 3));
        if (getHealth() > getMaxHealth()) setHealth(getMaxHealth());
        appat = c.hasKey("Appat") ? BlockPos.fromLong(c.getLong("Appat")) : null;
        mode(c.getBoolean("Arrivee") && appat != null ? ARRIVEE : NORMAL);
        boolean suspendu = mode() == ARRIVEE && espece.arrivee == Espece.Arrivee.PLAFOND;
        if (!espece.sansGravite() && !suspendu) setNoGravity(false);
        if (mode() == ARRIVEE && espece.arrivee == Espece.Arrivee.SAUT) {
            // le saut d'arrivée ne reprend pas en cours : il est déjà là
            mode(NORMAL);
            appat = null;
        }
        NBTTagList l = c.getTagList("Sbires", 8);
        for (int i = 0; i < l.tagCount(); i++) {
            try {
                sbires.add(UUID.fromString(l.getStringTagAt(i)));
            } catch (IllegalArgumentException ignore) {
            }
        }
        if (hasCustomName()) barre.setName(getDisplayName());
    }

    // ================================================================== pour les commandes
    /** /<boss> vie : pour essayer les phases (compte comme un coup reçu : pas de régénération tout de suite). */
    public void fixerVie(double part) {
        setHealth((float) (getMaxHealth() * part));
        depuisDegats = 0;
    }

    public String resume() {
        return String.format("%s : %.0f / %.0f PV, phase %d, %s%s, %d sbire(s), à %d %d %d", getDisplayName().getUnformattedText(), pv(), pvMax(),
                phase, coup != null ? coup.id : mode() == ARRIVEE ? "arrivée" : mode() == RENAISSANCE ? "renaissance" : "libre",
                cible != null ? " → " + cible.getName() : "", sbiresVivants(), MathHelper.floor(posX), MathHelper.floor(posY), MathHelper.floor(posZ));
    }
}
