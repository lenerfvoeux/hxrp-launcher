package fr.lenerfvoeux.hxrp.phenix.entite;

import fr.lenerfvoeux.hxrp.phenix.Feu;
import fr.lenerfvoeux.hxrp.phenix.HxrpPhenix;
import fr.lenerfvoeux.hxrp.phenix.PhenixConfig;
import fr.lenerfvoeux.hxrp.phenix.Registre;
import fr.lenerfvoeux.hxrp.phenix.Sons;
import fr.lenerfvoeux.hxrp.phenix.stats.StatsPhenix;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.core.IAnimatable;
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
 * Le Phénix de feu. Il tourne au-dessus de son arène (le lieu de son éclosion, ou son appât), prend pour cible tout
 * joueur proche et enchaîne ses attaques ; il se pose de temps en temps sur un perchoir (c'est là qu'on peut le frapper
 * à l'épée), puis repart. Trois phases (barre rouge, orange, violette) : sous 50 % de vie il s'embrase (tempête de feu,
 * boules en rafale) ; à 0 PV il devient un œuf de cendres : 10 secondes pour le briser, sinon il renaît avec toute sa
 * vie et lâche une nova solaire toutes les 20 s. Vie, armure, dégâts, portées et recharges : config/bestiaire_stats.xlsx.
 *
 * Il ne casse aucun bloc et n'allume aucun feu : ses attaques ne touchent que les joueurs.
 * La logique tourne sur le serveur ; l'état est synchronisé et le client en tire animations et particules.
 */
public class EntityPhenix extends EntityLiving implements IMob, IAnimatable {
    // ------------------------------------------------------------------ états
    public static final int RONDE = 0, BOULE = 1, SOUFFLE = 2, PLUIE = 3, PLONGEE = 4, TEMPETE = 5, APPROCHE = 6,
            PERCHE = 7, DECOLLAGE = 8, IMPACT = 9, OEUF = 10, RENAISSANCE = 11, NOVA = 12, ARRIVEE = 13;
    public static final String[] NOMS = {"ronde", "boule", "souffle", "pluie", "plongee", "tempete", "approche", "perche",
            "decollage", "impact", "oeuf", "renaissance", "nova", "arrivee"};
    /** Nova solaire : le Phénix se ramasse (CHARGE ticks), explose, puis se reprend. */
    public static final int NOVA_CHARGE = 20, NOVA_FIN = 40;

    private static final DataParameter<Byte> ETAT = EntityDataManager.createKey(EntityPhenix.class, DataSerializers.BYTE);
    private static final DataParameter<Boolean> EMBRASE = EntityDataManager.createKey(EntityPhenix.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Float> ZX = EntityDataManager.createKey(EntityPhenix.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> ZY = EntityDataManager.createKey(EntityPhenix.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> ZZ = EntityDataManager.createKey(EntityPhenix.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> ANGLE = EntityDataManager.createKey(EntityPhenix.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> OEUF_PV = EntityDataManager.createKey(EntityPhenix.class, DataSerializers.FLOAT);
    private static final DataParameter<Byte> PHASE = EntityDataManager.createKey(EntityPhenix.class, DataSerializers.BYTE);
    /** Rayon de la zone marquée (pluie de plumes, nova), pour les particules du client. */
    private static final DataParameter<Float> RAYON = EntityDataManager.createKey(EntityPhenix.class, DataSerializers.FLOAT);

    /** Rayon de la tornade autour du centre de l'arène, vitesse angulaire (rad/tick), durée (ticks). */
    public static final double TORNADE_RAYON = 10.0, TORNADE_VITESSE = 0.06;
    public static final int TORNADE_DEBUT = 30, TORNADE_FIN = 230;
    public static final int SOUFFLE_DEBUT = 15, SOUFFLE_FIN = 75;
    public static final int PLUIE_DEBUT = 30, PLUIE_FIN = 95;

    private final AnimationFactory usine = new AnimationFactory(this);
    private final BossInfoServer barre = new BossInfoServer(new TextComponentString("Phénix de feu"), BossInfo.Color.RED, BossInfo.Overlay.NOTCHED_10);
    private final Set<EntityPlayerMP> suivis = new HashSet<>();
    private final Set<UUID> touchesAnneau = new HashSet<>();

    // serveur
    private double ax, ay, az;
    private boolean ancre;
    private int etat = RONDE, tEtat, recharge = 60, attaques, derniereAttaque = -1, sansCible, depuisDegats = 1000, coince;
    private EntityPlayer cible;
    private boolean renaissanceFaite, embrase, definitif;
    private int phase = 1;
    /**
     * PV par point de vie Minecraft : la vie maximale vanilla plafonne à 1024, le Phénix du classeur en a 3000. Sa vie
     * est donc tenue en unités de « echelle » PV, et les dégâts reçus sont divisés d'autant après l'armure.
     * Sans initialiseur : la valeur est posée pendant le constructeur parent (applyEntityAttributes) et doit rester.
     */
    private double echelle;
    /** Dernier usage de chaque attaque (ticksExisted), pour leurs recharges. */
    private final Map<String, Integer> dernierUsage = new HashMap<>();
    /** Appât : position du nid de braises vers lequel il descend, puis qu'il dévore. */
    private BlockPos appat;
    private float pvOeuf;
    private double angleRonde;
    private int sensRonde = 1, dureePerche;
    private double px, py, pz;          // perchoir

    // client
    private int etatClient = -1;
    public int tClient;
    public float tangage, prevTangage, roulis, prevRoulis;

    public EntityPhenix(World w) {
        super(w);
        setSize(2.2F, 2.4F);
        isImmuneToFire = true;
        experienceValue = 0;
        ignoreFrustumCheck = true;
        setNoGravity(true);
        enablePersistence();
    }

    /** Œuf de phénix posé, ou /phenix invoquer : le Phénix sort de sa coquille au milieu des flammes. */
    public static EntityPhenix eclore(World w, double x, double y, double z, float cap) {
        EntityPhenix e = new EntityPhenix(w);
        e.setLocationAndAngles(x, y, z, cap, 0F);
        e.rotationYawHead = cap;
        e.renderYawOffset = cap;
        e.definirAncre(x, y, z);
        e.setEtat(RENAISSANCE);
        w.spawnEntity(e);
        e.son(Sons.RENAISSANCE, 4F, 1F);
        Feu.explosion(w, e, e, x, y + 0.5, z, 0.1, 0F, true);
        e.annoncer("§6Une chaleur terrible monte de l'œuf… §lle Phénix de feu s'éveille !");
        return e;
    }

    /**
     * Appât : le Phénix apparaît haut dans le ciel, loin du nid de braises, et descend vers lui en tournoyant ;
     * arrivé, il le dévore et attaque. Son arène est le nid.
     */
    public static EntityPhenix attirer(World w, double x, double y, double z, BlockPos nid) {
        EntityPhenix e = new EntityPhenix(w);
        float cap = (float) (MathHelper.atan2(nid.getZ() + 0.5 - z, nid.getX() + 0.5 - x) * (180.0 / Math.PI)) - 90F;
        e.setLocationAndAngles(x, y, z, cap, 0F);
        e.rotationYawHead = cap;
        e.renderYawOffset = cap;
        e.definirAncre(nid.getX() + 0.5, nid.getY(), nid.getZ() + 0.5);
        e.appat = nid;
        e.sensRonde = w.rand.nextBoolean() ? 1 : -1;
        e.setEtat(ARRIVEE);
        w.spawnEntity(e);
        e.son(Sons.CRI, 8F, 0.85F);
        return e;
    }

    public void definirAncre(double x, double y, double z) {
        ax = x;
        ay = y;
        az = z;
        ancre = true;
    }

    // ------------------------------------------------------------------ attributs, données
    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(64.0);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(StatsPhenix.phase(1).vitesse);
        attributsDePhase(1);
    }

    /** Vie maximale, armure, robustesse et recul de la phase (onglet Boss du classeur). */
    private void attributsDePhase(int n) {
        StatsPhenix.Phase p = StatsPhenix.phase(n);
        double avant = echelle;
        echelle = Math.max(1, Math.ceil(p.pv / 1024.0));
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(p.pv / echelle);
        if (avant > 0 && avant != echelle) setHealth((float) (getHealth() * avant / echelle));
        getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(Math.min(30, p.armure));
        getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).setBaseValue(Math.min(20, p.robustesse));
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(Math.min(1, p.recul));
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(p.vitesse);
    }

    private void entrerPhase(int n) {
        phase = n;
        dataManager.set(PHASE, (byte) n);
        attributsDePhase(n);
    }

    /** Après /phenix recharger : vie maximale, armure et vitesse de la phase en cours relues dans le classeur. */
    public void reglagesRelus() {
        attributsDePhase(phase);
        if (getHealth() > getMaxHealth()) setHealth(getMaxHealth());
    }

    /** Vie et vie maximale en PV du classeur (pour /phenix info). */
    public double pv() {
        return getHealth() * Math.max(1, echelle);
    }

    public double pvMax() {
        return getMaxHealth() * Math.max(1, echelle);
    }

    public int phase() {
        return world.isRemote ? dataManager.get(PHASE) : phase;
    }

    /** Dégâts d'une attaque dans la phase en cours (classeur : dégâts × multiplicateur de la phase). */
    private float degats(String attaque) {
        return (float) (StatsPhenix.attaque(attaque).degats * StatsPhenix.phase(phase).degats);
    }

    /** Secondes de feu laissées par une attaque (au moins 1). */
    private int feu(String attaque) {
        return Math.max(1, (int) Math.round(StatsPhenix.attaque(attaque).dureeEffet));
    }

    /** Distance du classeur sans le multiplicateur : là où il se place pour tirer (la portée réelle, elle, s'allonge). */
    private double porteeBase(String attaque, double defaut) {
        double d = StatsPhenix.attaque(attaque).portee;
        return d > 0 ? d : defaut;
    }

    /** Une distance de tir du classeur, allongée par le multiplicateur de portée de la phase. */
    private double portee(String attaque, double defaut) {
        double d = StatsPhenix.attaque(attaque).portee;
        return (d > 0 ? d : defaut) * StatsPhenix.phase(phase).portee;
    }

    /** Facteur de vitesse de vol par rapport aux réglages d'origine (vitesse 0,28). */
    private double allure() {
        return StatsPhenix.phase(phase).vitesse / 0.28;
    }

    private boolean pret(String attaque) {
        Integer t = dernierUsage.get(attaque);
        return t == null || ticksExisted - t >= StatsPhenix.attaque(attaque).recharge * 20;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(ETAT, (byte) RONDE);
        dataManager.register(EMBRASE, false);
        dataManager.register(ZX, 0F);
        dataManager.register(ZY, 0F);
        dataManager.register(ZZ, 0F);
        dataManager.register(ANGLE, 0F);
        dataManager.register(OEUF_PV, 1F);
        dataManager.register(PHASE, (byte) 1);
        dataManager.register(RAYON, 4F);
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> cle) {
        super.notifyDataManagerChange(cle);
        if (ETAT.equals(cle)) {
            int e = dataManager.get(ETAT);
            if (world.isRemote && e != etatClient) {
                etatClient = e;
                tClient = 0;
            }
            if (e == OEUF) setSize(1.0F, 1.4F);
            else setSize(2.2F, 2.4F);
        }
    }

    public int etat() {
        return world.isRemote ? dataManager.get(ETAT) : etat;
    }

    public boolean embrase() {
        return dataManager.get(EMBRASE);
    }

    public Vec3d zone() {
        return new Vec3d(dataManager.get(ZX), dataManager.get(ZY), dataManager.get(ZZ));
    }

    public float rayonZone() {
        return dataManager.get(RAYON);
    }

    public float angleTornade() {
        return dataManager.get(ANGLE);
    }

    public float fractionOeuf() {
        return dataManager.get(OEUF_PV);
    }

    private void setEtat(int e) {
        etat = e;
        tEtat = 0;
        dataManager.set(ETAT, (byte) e);
        if (e == RONDE) angleRonde = Math.atan2(posZ - centreZ(), posX - centreX());
    }

    private void setZone(double x, double y, double z) {
        dataManager.set(ZX, (float) x);
        dataManager.set(ZY, (float) y);
        dataManager.set(ZZ, (float) z);
    }

    // ------------------------------------------------------------------ comportement de base
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
    public boolean canTrample(World w, Block b, BlockPos pos, float distance) {
        return false;
    }

    @Override
    public boolean isPushedByWater() {
        return false;
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    @Override
    public float getEyeHeight() {
        return etat() == OEUF ? 0.8F : 1.9F;
    }

    /** Côté client : « en feu » pour l'éclairage dynamique d'OptiFine, sans la surcouche de flammes vanilla. */
    @Override
    public boolean isBurning() {
        return world.isRemote || super.isBurning();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean canRenderOnFire() {
        return false;
    }

    @Override
    public void travel(float strafe, float vertical, float forward) {
        if (!isServerWorld()) {
            super.travel(strafe, vertical, forward);
            return;
        }
        if (hasNoGravity()) {
            move(MoverType.SELF, motionX, motionY, motionZ);
            if (collidedHorizontally && etat != PERCHE && etat != IMPACT) {
                motionY = Math.max(motionY, 0.18);
                if (++coince > 60) {
                    motionY = 0.5;
                    coince = 0;
                }
            } else {
                coince = 0;
            }
        } else {
            motionY -= 0.06;
            motionX *= 0.6;
            motionZ *= 0.6;
            move(MoverType.SELF, motionX, motionY, motionZ);
            motionY *= 0.98;
            if (onGround) {
                motionX = 0;
                motionZ = 0;
            }
        }
    }

    // ------------------------------------------------------------------ dégâts, œuf, mort
    @Override
    public boolean attackEntityFrom(DamageSource src, float n) {
        if (world.isRemote || isEntityInvulnerable(src)) return false;
        if (src.isFireDamage() || src == DamageSource.IN_WALL || src == DamageSource.DROWN || src == DamageSource.FALL
                || src == DamageSource.CRAMMING || src == DamageSource.CACTUS || src == DamageSource.FLY_INTO_WALL)
            return false;
        if (src.getTrueSource() instanceof EntityPhenix) return false;
        if (etat == RENAISSANCE) return false;
        if (etat == OEUF && !definitif) {
            pvOeuf -= n;
            dataManager.set(OEUF_PV, Math.max(0F, (float) (pvOeuf / PhenixConfig.pointsDeVieOeuf)));
            world.setEntityState(this, (byte) 2);
            son(Sons.CRAQUEMENT, 1.6F, 0.8F + rand.nextFloat() * 0.4F);
            if (pvOeuf <= 0) {
                definitif = true;
                annoncer("§7L'œuf de cendres vole en éclats. §6Le Phénix de feu s'éteint pour de bon.");
                return super.attackEntityFrom(src, Float.MAX_VALUE / 8);
            }
            return true;
        }
        Entity e = src.getTrueSource();
        if (e instanceof EntityPlayer && Feu.cibleValable((EntityPlayer) e) && getDistanceSq(e) < 48 * 48) cible = (EntityPlayer) e;
        depuisDegats = 0;
        if (etat == PERCHE && tEtat > 10) decoller();
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
        if (getHealth() <= 0 && !definitif && StatsPhenix.troisPhases() && !renaissanceFaite && etat != OEUF) {
            setHealth(1F);
            entrerOeuf();
        }
    }

    private void entrerOeuf() {
        setEtat(OEUF);
        pvOeuf = (float) PhenixConfig.pointsDeVieOeuf;
        dataManager.set(OEUF_PV, 1F);
        setNoGravity(false);
        motionX = motionZ = 0;
        cible = null;
        son(Sons.OEUF, 3F, 0.9F);
        annoncer("§6Le Phénix s'effondre en un œuf de cendres… §cbrisez-le avant qu'il ne renaisse !");
    }

    /** L'œuf n'a pas été brisé : 3e phase, toute la vie reprise (part du classeur), encore plus enragé. */
    private void renaitre() {
        renaissanceFaite = true;
        setNoGravity(true);
        embraser(false);
        entrerPhase(3);
        setHealth((float) (getMaxHealth() * StatsPhenix.phase(3).vieReprise));
        dernierUsage.put("nova", ticksExisted);
        setEtat(RENAISSANCE);
        son(Sons.RENAISSANCE, 4F, 1F);
        Feu.explosion(world, this, this, posX, posY + 1, posZ, 6, degats("nova") * 0.25F, true, feu("nova"));
        annoncer("§5§lLe Phénix renaît de ses cendres, plus ardent que jamais !");
    }

    @Override
    public void onDeath(DamageSource src) {
        super.onDeath(src);
        setNoGravity(false);
        if (!world.isRemote) {
            son(Sons.MORT, 4F, 1F);
            if (!definitif) annoncer("§6Le Phénix de feu s'éteint…");
        }
    }

    @Override
    protected void onDeathUpdate() {
        ++deathTime;
        if (deathTime >= 40 && !world.isRemote) {
            if (world instanceof WorldServer)
                ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_LARGE, posX, posY + 1, posZ, 40, 1, 0.8, 1, 0.05);
            setDead();
        }
    }

    @Override
    protected void dropFewItems(boolean touche, int butin) {
        if (rand.nextDouble() >= StatsPhenix.plumesChance) return;
        int min = Math.min(StatsPhenix.plumesMin, StatsPhenix.plumesMax), max = Math.max(StatsPhenix.plumesMin, StatsPhenix.plumesMax);
        int n = min + rand.nextInt(max - min + 1);
        for (int i = 0; i < n; i++) entityDropItem(new ItemStack(Registre.PLUME), 0.5F);
    }

    @Override
    protected void dropEquipment(boolean touche, int butin) {
    }

    // ------------------------------------------------------------------ sons
    @Override
    protected SoundEvent getAmbientSound() {
        return etat() == OEUF ? null : Sons.CRI;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return etat() == OEUF ? Sons.CRAQUEMENT : Sons.CRI_COURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected float getSoundVolume() {
        return 4.0F;
    }

    @Override
    public int getTalkInterval() {
        return 220;
    }

    private void son(SoundEvent s, float volume, float hauteur) {
        if (s != null) world.playSound(null, posX, posY + 1, posZ, s, SoundCategory.HOSTILE, volume, hauteur);
    }

    private void annoncer(String msg) {
        if (world.isRemote) return;
        HxrpPhenix.LOG.info("{} ({} {} {})", msg.replaceAll("§.", ""), MathHelper.floor(posX), MathHelper.floor(posY), MathHelper.floor(posZ));
        for (EntityPlayer p : world.playerEntities)
            if (p.getDistanceSq(this) < 96 * 96) p.sendMessage(new TextComponentString(msg));
    }

    // ------------------------------------------------------------------ barre de boss
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
        if (etat == OEUF) {
            int s = Math.max(0, PhenixConfig.dureeOeufSecondes - tEtat / 20);
            barre.setName(new TextComponentString("Œuf de cendres — brisez-le ! (" + s + " s)"));
            barre.setColor(BossInfo.Color.WHITE);
            barre.setPercent(Math.max(0F, (float) (pvOeuf / PhenixConfig.pointsDeVieOeuf)));
        } else {
            barre.setName(getDisplayName());
            // rouge, puis orange (jaune vanilla, peinte en orange par le client : voir client.BarreOrange), puis violette
            barre.setColor(phase >= 3 ? BossInfo.Color.PURPLE : phase == 2 ? BossInfo.Color.YELLOW : BossInfo.Color.RED);
            barre.setPercent(MathHelper.clamp(getHealth() / getMaxHealth(), 0F, 1F));
        }
        if (ticksExisted % 10 == 0) {
            for (EntityPlayerMP p : new ArrayList<>(suivis)) {
                if (p.isEntityAlive() && p.world == world && p.getDistanceSq(this) < 64 * 64) barre.addPlayer(p);
                else barre.removePlayer(p);
            }
        }
    }

    // ------------------------------------------------------------------ boucle
    @Override
    public void onUpdate() {
        super.onUpdate();
        if (world.isRemote) {
            tClient++;
            renderYawOffset = rotationYaw;
            prevRenderYawOffset = prevRotationYaw;
            effetsClient();
        } else {
            if (!ancre) definirAncre(posX, posY, posZ);
            majBarre();
            if (isEntityAlive() && etat != OEUF && etat != PERCHE && etat != IMPACT && ticksExisted % 24 == 0)
                son(Sons.AILES, 2.2F, 0.85F + rand.nextFloat() * 0.25F);
        }
    }

    @Override
    protected void updateAITasks() {
        tEtat++;
        depuisDegats++;
        if (etat == OEUF) {
            motionX = motionZ = 0;
            if (tEtat >= PhenixConfig.dureeOeufSecondes * 20) renaitre();
            return;
        }
        if (etat == RENAISSANCE) {
            motionX *= 0.5;
            motionZ *= 0.5;
            motionY = tEtat < 24 ? 0.07 : 0.02;
            if (tEtat >= 30) setEtat(DECOLLAGE);
            return;
        }
        if (etat == ARRIVEE) {
            arrivee();
            return;
        }
        choisirCible();
        if (!embrase && phase < 2 && StatsPhenix.PHASES[2].existe && getHealth() < getMaxHealth() * StatsPhenix.phase(2).declenche) embraser(true);
        // 3e phase : la nova solaire part dès qu'elle est prête, entre deux attaques
        if (phase >= 3 && pret("nova") && (etat == RONDE || etat == DECOLLAGE) && cible != null) commencer(NOVA);
        switch (etat) {
            case RONDE: ronde(); break;
            case BOULE: boule(); break;
            case SOUFFLE: souffle(); break;
            case PLUIE: pluie(); break;
            case PLONGEE: plongee(); break;
            case TEMPETE: tempete(); break;
            case APPROCHE: approche(); break;
            case PERCHE: perche(); break;
            case DECOLLAGE: decollage(); break;
            case IMPACT: impact(); break;
            case NOVA: nova(); break;
            default: setEtat(RONDE);
        }
        // un phénix laissé seul se refait une santé
        if (cible == null && depuisDegats > 1200 && ticksExisted % 20 == 0 && getHealth() < getMaxHealth()) heal(2F);
    }

    private void embraser(boolean tempeteTout) {
        embrase = true;
        dataManager.set(EMBRASE, true);
        if (phase < 2) entrerPhase(2);
        son(Sons.CRI, 5F, 0.8F);
        if (tempeteTout) {
            annoncer("§6Le Phénix s'embrase ! §eUne tempête de feu se lève…");
            if (etat != APPROCHE && etat != PERCHE) commencer(TEMPETE);
        }
    }

    // ------------------------------------------------------------------ cible et arène
    private double centreX() {
        return cible != null ? cible.posX : ax;
    }

    private double centreZ() {
        return cible != null ? cible.posZ : az;
    }

    private double distAncreSq(Entity e) {
        double dx = e.posX - ax, dz = e.posZ - az;
        return dx * dx + dz * dz;
    }

    private void choisirCible() {
        double arene = StatsPhenix.rayonArene, rayon = StatsPhenix.rayonCible;
        if (cible != null && (!Feu.cibleValable(cible) || cible.world != world || getDistanceSq(cible) > (rayon + 10) * (rayon + 10)
                || distAncreSq(cible) > arene * arene))
            cible = null;
        if (cible == null && ticksExisted % 10 == 0) {
            EntityPlayer mieux = null;
            double md = rayon * rayon;
            for (EntityPlayer p : world.playerEntities) {
                if (!Feu.cibleValable(p)) continue;
                double d = getDistanceSq(p);
                if (d < md && distAncreSq(p) < arene * arene) {
                    mieux = p;
                    md = d;
                }
            }
            if (mieux != null && sansCible > 40) son(Sons.CRI, 4F, 1.0F);
            cible = mieux;
        }
        sansCible = cible == null ? sansCible + 1 : 0;
    }

    /** Hauteur du sol (premier bloc qui arrête la vue, feuillages compris) ; la position actuelle si le tronçon n'est pas chargé. */
    private double sol(double x, double z) {
        BlockPos p = new BlockPos(x, 0, z);
        if (!world.isBlockLoaded(p)) return posY - 8;
        return world.getHeight(p).getY();
    }

    // ------------------------------------------------------------------ déplacements
    private void voler(double tx, double ty, double tz, double vitesse, double agilite) {
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

    private void tourner(float cap, float max) {
        float d = MathHelper.wrapDegrees(cap - rotationYaw);
        rotationYaw += MathHelper.clamp(d, -max, max);
        renderYawOffset = rotationYaw;
        rotationYawHead = rotationYaw;
    }

    private void suivreCap() {
        if (motionX * motionX + motionZ * motionZ > 0.0025)
            tourner((float) (MathHelper.atan2(motionZ, motionX) * (180.0 / Math.PI)) - 90F, 9F);
    }

    private void regarder(double x, double z, float max) {
        tourner((float) (MathHelper.atan2(z - posZ, x - posX) * (180.0 / Math.PI)) - 90F, max);
    }

    /** Point au bout du bec. */
    public Vec3d bec() {
        float a = rotationYawHead * (float) Math.PI / 180F;
        return new Vec3d(posX - MathHelper.sin(a) * 2.0, posY + 1.85, posZ + MathHelper.cos(a) * 2.0);
    }

    // ------------------------------------------------------------------ ronde et choix des attaques
    private void ronde() {
        double cx = centreX(), cz = centreZ(), r, h;
        if (cible != null) {
            r = 11 + 2 * Math.sin(ticksExisted * 0.013);
            h = cible.posY + 9;
        } else {
            r = 14;
            h = Math.max(ay, sol(ax, az)) + 13;
        }
        angleRonde += sensRonde * 0.045;
        double tx = cx + Math.cos(angleRonde) * r, tz = cz + Math.sin(angleRonde) * r;
        double ty = Math.max(h, sol(tx, tz) + 6) + Math.sin(ticksExisted * 0.05) * 1.5;
        voler(tx, ty, tz, 0.55, 0.12);
        suivreCap();
        if (cible != null) {
            if (--recharge <= 0) lancerAttaque();
        } else if (sansCible > 300 && rand.nextInt(240) == 0) {
            commencerPerche();
        }
        if (rand.nextInt(900) == 0) sensRonde = -sensRonde;
    }

    private void lancerAttaque() {
        if (++attaques > 4 + rand.nextInt(3) && pret("perche")) {
            attaques = 0;
            commencerPerche();
            return;
        }
        List<Integer> sac = new ArrayList<>();
        ajouter(sac, BOULE);
        ajouter(sac, SOUFFLE);
        ajouter(sac, PLUIE);
        ajouter(sac, PLONGEE);
        // la tempête de feu part dès qu'il s'embrase, puis à chaque fois que sa recharge le permet
        if (embrase && pret("tempete")) ajouter(sac, TEMPETE, 5);
        if (sac.isEmpty()) ajouter(sac, BOULE, 1);
        commencer(sac.get(rand.nextInt(sac.size())));
    }

    /** Ajoute une attaque selon son poids dans le classeur, si elle est de cette phase et rechargée. */
    private void ajouter(List<Integer> sac, int attaque) {
        StatsPhenix.Attaque a = StatsPhenix.attaque(NOMS[attaque]);
        if (a.phase > phase || !pret(NOMS[attaque])) return;
        ajouter(sac, attaque, (int) Math.max(0, Math.round(a.poids)));
    }

    private void ajouter(List<Integer> sac, int attaque, int poids) {
        if (attaque == derniereAttaque) poids = Math.min(poids, 1);
        for (int i = 0; i < poids; i++) sac.add(attaque);
    }

    /** Force une attaque (commande /phenix attaque). */
    public boolean forcer(int attaque) {
        if (etat == OEUF || etat == RENAISSANCE || etat == ARRIVEE || !isEntityAlive()) return false;
        if (attaque == PERCHE) commencerPerche();
        else commencer(attaque);
        return true;
    }

    private void commencer(int attaque) {
        derniereAttaque = attaque;
        dernierUsage.put(NOMS[attaque], ticksExisted);
        touchesAnneau.clear();
        setEtat(attaque);
    }

    /** Retour à la ronde ; la pause avant l'attaque suivante vient du classeur (plus courte à chaque phase). */
    private void finAttaque() {
        int pause = (int) Math.round(StatsPhenix.phase(phase).pause * 20);
        recharge = Math.max(10, pause + rand.nextInt(Math.max(1, pause / 2 + 1)));
        setEtat(RONDE);
    }

    // ------------------------------------------------------------------ boule de feu
    private void boule() {
        if (cible == null) {
            finAttaque();
            return;
        }
        double a = Math.atan2(posZ - cible.posZ, posX - cible.posX), d = Math.max(8, porteeBase("boule", 26) / 2);
        voler(cible.posX + Math.cos(a) * d, cible.posY + 6.5, cible.posZ + Math.sin(a) * d, 0.4, 0.15);
        regarder(cible.posX, cible.posZ, 14F);
        if (tEtat == 4) son(Sons.CRI_COURT, 3F, 1.3F);
        if (tEtat == 15 || (embrase && (tEtat == 19 || tEtat == 23))) tirer();
        if (tEtat >= 40) finAttaque();
    }

    private void tirer() {
        Vec3d b = bec();
        double dx = cible.posX - b.x, dy = cible.posY + cible.height * 0.5 - b.y, dz = cible.posZ - b.z;
        StatsPhenix.Attaque st = StatsPhenix.attaque("boule");
        EntityBouleDeFeu f = new EntityBouleDeFeu(world, this, dx, dy, dz).reglages(degats("boule"), st.rayon > 0 ? st.rayon : 3.2, feu("boule"));
        f.setPosition(b.x, b.y - 0.5, b.z);
        world.spawnEntity(f);
        son(Sons.BOULE, 3F, 0.9F + rand.nextFloat() * 0.2F);
    }

    // ------------------------------------------------------------------ souffle ardent
    private void souffle() {
        if (cible == null && tEtat < SOUFFLE_DEBUT) {
            finAttaque();
            return;
        }
        if (cible != null) {
            double a = Math.atan2(posZ - cible.posZ, posX - cible.posX), d = Math.max(5, porteeBase("souffle", 12) * 0.6);
            voler(cible.posX + Math.cos(a) * d, cible.posY + 5, cible.posZ + Math.sin(a) * d, 0.3, 0.1);
            // pendant le souffle il tourne lentement : on peut esquiver en courant de côté
            getLookHelper().setLookPosition(cible.posX, cible.posY + 0.8, cible.posZ, tEtat < SOUFFLE_DEBUT ? 20F : 2.6F, 40F);
        }
        rotationYaw = rotationYawHead;
        renderYawOffset = rotationYawHead;
        if (tEtat == 2) son(Sons.CRI_COURT, 3F, 0.8F);
        if (tEtat >= SOUFFLE_DEBUT && tEtat < SOUFFLE_FIN) {
            if ((tEtat - SOUFFLE_DEBUT) % 20 == 0) son(Sons.SOUFFLE, 3.5F, 0.9F + rand.nextFloat() * 0.15F);
            if ((tEtat - SOUFFLE_DEBUT) % 4 == 0) brulerCone();
        }
        if (tEtat >= SOUFFLE_FIN + 10) finAttaque();
    }

    private void brulerCone() {
        Vec3d o = bec(), dir = getLook(1.0F);
        StatsPhenix.Attaque st = StatsPhenix.attaque("souffle");
        double longueur = portee("souffle", 12) + 1, largeur = st.rayon > 0 ? st.rayon : 5;
        double cos = Math.cos(Math.atan2(largeur, Math.max(1, longueur)));
        for (EntityPlayer p : world.getEntitiesWithinAABB(EntityPlayer.class, getEntityBoundingBox().grow(longueur + 2))) {
            Vec3d v = new Vec3d(p.posX - o.x, p.posY + p.height * 0.5 - o.y, p.posZ - o.z);
            double d = v.length();
            if (d > longueur || d < 0.1 || v.dotProduct(dir) / d < cos) continue;
            if (Feu.vueDegagee(world, o, p)) Feu.toucher(p, this, this, degats("souffle"), feu("souffle"));
        }
    }

    // ------------------------------------------------------------------ pluie de plumes enflammées
    private double rayonPluie() {
        double r = StatsPhenix.attaque("pluie").rayon;
        return (r > 0 ? r : 4.0) * (embrase ? 1.25 : 1.0);
    }

    private void pluie() {
        if (tEtat == 1) {
            if (cible == null) {
                finAttaque();
                return;
            }
            setZone(cible.posX, Math.floor(cible.posY + 0.01), cible.posZ);
            dataManager.set(RAYON, (float) rayonPluie());
            son(Sons.CRI, 4F, 1.15F);
        }
        Vec3d z = zone();
        voler(z.x + Math.cos(ticksExisted * 0.05) * 3, z.y + 13, z.z + Math.sin(ticksExisted * 0.05) * 3, 0.45, 0.12);
        regarder(z.x, z.z, 10F);
        if (tEtat >= PLUIE_DEBUT && tEtat < PLUIE_FIN) {
            int n = embrase ? 2 : 1;
            for (int i = 0; i < n; i++) {
                double a = rand.nextDouble() * Math.PI * 2, r = Math.sqrt(rand.nextDouble()) * rayonPluie();
                EntityPlumeArdente f = new EntityPlumeArdente(world, this).reglages(degats("pluie"), feu("pluie"));
                f.setLocationAndAngles(z.x + Math.cos(a) * r, z.y + 11 + rand.nextDouble() * 3, z.z + Math.sin(a) * r, rand.nextFloat() * 360F, 0F);
                f.motionY = -0.45 - rand.nextDouble() * 0.15;
                world.spawnEntity(f);
            }
            if (tEtat % 15 == 0) son(Sons.SOUFFLE, 2F, 1.4F);
        }
        if (tEtat >= PLUIE_FIN + 30) finAttaque();
    }

    // ------------------------------------------------------------------ plongée de braise
    private void plongee() {
        if (tEtat < 25) {
            if (cible == null) {
                finAttaque();
                return;
            }
            double a = Math.atan2(posZ - cible.posZ, posX - cible.posX);
            voler(cible.posX + Math.cos(a) * 9, cible.posY + 14, cible.posZ + Math.sin(a) * 9, 0.6, 0.15);
            regarder(cible.posX, cible.posZ, 15F);
            if (tEtat == 2) son(Sons.CRI, 4F, 1.25F);
            return;
        }
        if (tEtat == 25) {
            if (cible != null) setZone(cible.posX, Math.floor(cible.posY + 0.01), cible.posZ);
            else setZone(posX, sol(posX, posZ), posZ);
            son(Sons.PLONGEE, 4F, 1F);
        }
        Vec3d z = zone();
        voler(z.x, z.y, z.z, 1.5, 0.35);
        suivreCap();
        double dx = z.x - posX, dy = z.y - posY, dz = z.z - posZ;
        if (dx * dx + dy * dy + dz * dz < 3.2 || onGround || collidedVertically || (collidedHorizontally && tEtat > 32) || tEtat > 85) {
            motionX = motionY = motionZ = 0;
            setZone(posX, posY, posZ);
            setEtat(IMPACT);
            son(Sons.ONDE, 4F, 0.8F);
            double r = StatsPhenix.attaque("plongee").rayon;
            Feu.explosion(world, this, this, posX, posY + 0.3, posZ, r > 0 ? r : 4.5, degats("plongee"), true, feu("plongee"));
        }
    }

    /** Au sol après la plongée : un anneau de feu s'élargit (on le saute ou on s'en éloigne), puis il repart. */
    private void impact() {
        motionX = motionZ = 0;
        motionY = onGround ? 0 : -0.1;
        Vec3d z = zone();
        if (tEtat <= 22) {
            double r = 1.0 + tEtat * 0.42;
            for (EntityPlayer p : world.getEntitiesWithinAABB(EntityPlayer.class, new AxisAlignedBB(z.x - r - 2, z.y - 2, z.z - r - 2, z.x + r + 2, z.y + 3, z.z + r + 2))) {
                if (touchesAnneau.contains(p.getUniqueID())) continue;
                double d = Math.sqrt((p.posX - z.x) * (p.posX - z.x) + (p.posZ - z.z) * (p.posZ - z.z));
                if (Math.abs(d - r) < 0.9 && p.posY - z.y < 1.2 && p.posY - z.y > -1.5) {
                    touchesAnneau.add(p.getUniqueID());
                    Feu.toucher(p, this, this, degats("plongee") * 0.5F, feu("plongee"));
                }
            }
        }
        if (tEtat >= 44) decoller();
    }

    // ------------------------------------------------------------------ tempête de feu (embrasé)
    private void tempete() {
        if (tEtat == 1) {
            double cx = ax, cz = az;
            if (cible != null && distAncreSq(cible) > 20 * 20) {
                cx = cible.posX;
                cz = cible.posZ;
            }
            setZone(cx, sol(cx, cz), cz);
            dataManager.set(ANGLE, rand.nextFloat() * (float) Math.PI * 2);
            son(Sons.CRI, 5F, 0.75F);
        }
        Vec3d c = zone();
        voler(c.x, c.y + 7, c.z, 0.5, 0.15);
        if (tEtat >= TORNADE_DEBUT && tEtat < TORNADE_FIN) {
            if ((tEtat - TORNADE_DEBUT) % 40 == 0) son(Sons.TEMPETE, 4F, 1F);
            double th = angleTornade() + (tEtat - TORNADE_DEBUT) * TORNADE_VITESSE;
            double tx = c.x + Math.cos(th) * TORNADE_RAYON, tz = c.z + Math.sin(th) * TORNADE_RAYON;
            double zone = Math.max(26, StatsPhenix.attaque("tempete").rayon * StatsPhenix.phase(phase).portee + 2);
            List<EntityPlayer> proches = world.getEntitiesWithinAABB(EntityPlayer.class, new AxisAlignedBB(c.x - zone, c.y - 6, c.z - zone, c.x + zone, c.y + 16, c.z + zone));
            for (EntityPlayer p : proches) {
                if (!Feu.cibleValable(p)) continue;
                double hx = p.posX - tx, hz = p.posZ - tz;
                boolean hauteur = p.posY > c.y - 2 && p.posY < c.y + 11;
                if (hauteur && hx * hx + hz * hz < 2.8 * 2.8) {
                    if (Feu.toucher(p, this, this, degats("tempete"), feu("tempete"))) {
                        p.motionY = Math.max(p.motionY, 0.45);
                        p.velocityChanged = true;
                    }
                }
                double cx = c.x - p.posX, cz = c.z - p.posZ, d = Math.sqrt(cx * cx + cz * cz);
                if (hauteur && d < 3.0) Feu.toucher(p, this, this, degats("tempete") * 0.6F, feu("tempete"));
                // aspiration vers le centre de l'arène
                double aspire = Math.max(12, StatsPhenix.attaque("tempete").rayon * StatsPhenix.phase(phase).portee);
                if (tEtat % 4 == 0 && d > 1.5 && d < aspire) {
                    double f = 0.14 + 0.12 * (1 - d / aspire);
                    p.motionX += cx / d * f;
                    p.motionZ += cz / d * f;
                    p.velocityChanged = true;
                }
            }
        }
        if (tEtat >= TORNADE_FIN + 15) finAttaque();
    }

    // ------------------------------------------------------------------ nova solaire (3e phase)
    /**
     * Il fond vers sa cible en se ramassant, ailes levées, et aspire les flammes (une seconde pour s'abriter ou
     * s'éloigner), puis explose : tout joueur à portée (classeur, 15 blocs) est brûlé et projeté au loin. Un mur protège.
     */
    private void nova() {
        if (tEtat == 1) {
            if (cible != null) setZone(cible.posX, cible.posY + 4.5, cible.posZ);
            else setZone(posX, posY, posZ);
            son(Sons.NOVA, 5F, 1F);
        }
        Vec3d z = zone();
        if (tEtat < NOVA_CHARGE) {
            voler(z.x, Math.max(z.y, sol(z.x, z.z) + 3), z.z, 0.8, 0.2);
            if (cible != null) regarder(cible.posX, cible.posZ, 12F);
        } else {
            motionX *= 0.5;
            motionY *= 0.5;
            motionZ *= 0.5;
        }
        if (tEtat == NOVA_CHARGE) eclaterNova();
        if (tEtat >= NOVA_FIN) finAttaque();
    }

    private void eclaterNova() {
        double r = StatsPhenix.attaque("nova").rayon;
        if (r <= 0) r = 15;
        Vec3d c = new Vec3d(posX, posY + 1.2, posZ);
        for (EntityPlayer p : world.getEntitiesWithinAABB(EntityPlayer.class, getEntityBoundingBox().grow(r))) {
            double dx = p.posX - c.x, dy = p.posY + p.height * 0.5 - c.y, dz = p.posZ - c.z;
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d > r || !Feu.cibleValable(p) || !Feu.vueDegagee(world, c, p)) continue;
            double k = 1 - 0.5 * d / r;
            if (Feu.toucher(p, this, this, (float) (degats("nova") * k), feu("nova")))
                Feu.repousser(p, dx, dz, 0.6 + 1.4 * k, 0.6);
        }
        setZone(posX, sol(posX, posZ), posZ);
        dataManager.set(RAYON, (float) r);
        son(Sons.ONDE, 5F, 0.7F);
    }

    // ------------------------------------------------------------------ arrivée sur l'appât
    /** Descente en spirale vers le nid : à peu près un tour complet, de plus en plus serré et de plus en plus bas. */
    private void arrivee() {
        if (appat == null) {
            setEtat(DECOLLAGE);
            return;
        }
        double nx = appat.getX() + 0.5, ny = appat.getY(), nz = appat.getZ() + 0.5;
        double dx = posX - nx, dz = posZ - nz, h = Math.sqrt(dx * dx + dz * dz);
        double an = Math.atan2(dz, dx) + sensRonde * Math.min(0.5, 2.5 / Math.max(1.0, h));
        double r = Math.max(0, h - 1.2);
        voler(nx + Math.cos(an) * r, ny + 1.2 + r * 0.5, nz + Math.sin(an) * r, 0.75, 0.2);
        suivreCap();
        if (tEtat % 60 == 1) son(Sons.CRI, 6F, 0.85F + rand.nextFloat() * 0.1F);
        if ((h < 1.3 && posY - ny < 3.0) || tEtat > 1200) devorer();
    }

    /** Il se pose sur le nid, le dévore dans une gerbe de braises, crie et attaque. */
    private void devorer() {
        BlockPos n = appat;
        appat = null;
        if (world.getBlockState(n).getBlock() == Registre.NID) {
            world.setBlockToAir(n);
            if (world instanceof WorldServer) {
                WorldServer ws = (WorldServer) world;
                ws.spawnParticle(EnumParticleTypes.LAVA, n.getX() + 0.5, n.getY() + 0.3, n.getZ() + 0.5, 20, 0.4, 0.2, 0.4, 0.0);
                ws.spawnParticle(EnumParticleTypes.FLAME, n.getX() + 0.5, n.getY() + 0.3, n.getZ() + 0.5, 60, 0.5, 0.3, 0.5, 0.12);
                ws.spawnParticle(EnumParticleTypes.SMOKE_LARGE, n.getX() + 0.5, n.getY() + 0.5, n.getZ() + 0.5, 20, 0.5, 0.4, 0.5, 0.04);
            }
        }
        definirAncre(n.getX() + 0.5, n.getY(), n.getZ() + 0.5);
        son(Sons.CRI, 6F, 0.8F);
        annoncer("§6Le Phénix de feu dévore le nid de braises… §cet se tourne vers vous !");
        setEtat(DECOLLAGE);
    }

    // ------------------------------------------------------------------ perchoir
    private void commencerPerche() {
        BlockPos p = chercherPerchoir();
        if (p == null) {
            finAttaque();
            return;
        }
        px = p.getX() + 0.5;
        py = p.getY();
        pz = p.getZ() + 0.5;
        double vulnerable = StatsPhenix.attaque("perche").dureeEffet;
        dureePerche = cible != null ? (int) Math.round((vulnerable > 0 ? vulnerable : 4) * 20) : 120 + rand.nextInt(80);
        dernierUsage.put("perche", ticksExisted);
        setEtat(APPROCHE);
    }

    /** Le point le plus haut et dégagé autour de l'arène : cime d'arbre, faîte d'un toit, rocher… */
    private BlockPos chercherPerchoir() {
        BlockPos mieux = null;
        for (int i = 0; i < 28; i++) {
            double a = rand.nextDouble() * Math.PI * 2, r = 3 + rand.nextDouble() * 13;
            int x = MathHelper.floor(ax + Math.cos(a) * r), z = MathHelper.floor(az + Math.sin(a) * r);
            BlockPos haut = new BlockPos(x, 0, z);
            if (!world.isBlockLoaded(haut)) continue;
            haut = world.getHeight(haut);
            if (haut.getY() <= 1 || haut.getY() > world.getHeight() - 6) continue;
            if (!appui(haut.down()) || !degage(haut)) continue;
            if (mieux == null || haut.getY() > mieux.getY()) mieux = haut;
        }
        return mieux;
    }

    private boolean appui(BlockPos p) {
        IBlockState s = world.getBlockState(p);
        Material m = s.getMaterial();
        if (m.isLiquid() || m == Material.FIRE || m == Material.CACTUS || m == Material.LAVA) return false;
        return s.isSideSolid(world, p, EnumFacing.UP) || m == Material.LEAVES;
    }

    private boolean degage(BlockPos p) {
        for (BlockPos q : BlockPos.getAllInBox(p.add(-1, 0, -1), p.add(1, 3, 1)))
            if (!world.isAirBlock(q) && world.getBlockState(q).getMaterial().blocksMovement()) return false;
        return true;
    }

    private void approche() {
        double dx = px - posX, dz = pz - posZ, h = Math.sqrt(dx * dx + dz * dz);
        double ty = h > 3 ? py + 3 + Math.min(6, h * 0.3) : py;
        voler(px, ty, pz, h > 3 ? 0.5 : 0.18, 0.15);
        suivreCap();
        double dy = py - posY;
        if (h < 0.6 && Math.abs(dy) < 0.6) {
            setPosition(px, py, pz);
            motionX = motionY = motionZ = 0;
            setEtat(PERCHE);
        } else if (tEtat > 260) {
            finAttaque();
        }
    }

    private void perche() {
        motionX = motionY = motionZ = 0;
        setPosition(px, py, pz);
        if (ticksExisted % 20 == 0 && !appui(new BlockPos(px, py - 0.5, pz))) {
            decoller();
            return;
        }
        if (cible != null) regarder(cible.posX, cible.posZ, 3F);
        else if (tEtat % 60 == 0) tourner(rotationYaw + (rand.nextFloat() - 0.5F) * 90F, 45F);
        boolean proche = cible != null && getDistanceSq(cible) < 6 * 6;
        if (tEtat >= dureePerche || (proche && tEtat > 30)) decoller();
    }

    private void decoller() {
        // coup d'ailes : repousse les joueurs collés à lui
        for (EntityPlayer p : world.getEntitiesWithinAABB(EntityPlayer.class, getEntityBoundingBox().grow(4)))
            if (Feu.cibleValable(p)) Feu.repousser(p, p.posX - posX, p.posZ - posZ, 0.9, 0.35);
        if (world instanceof WorldServer)
            ((WorldServer) world).spawnParticle(EnumParticleTypes.FLAME, posX, posY + 0.5, posZ, 40, 1.5, 0.2, 1.5, 0.08);
        son(Sons.AILES, 3F, 0.7F);
        setEtat(DECOLLAGE);
    }

    private void decollage() {
        float a = rotationYaw * (float) Math.PI / 180F;
        voler(posX - MathHelper.sin(a) * 3, Math.max(posY + 3, sol(posX, posZ) + 7), posZ + MathHelper.cos(a) * 3, 0.5, 0.2);
        if (tEtat >= 22) {
            recharge = 20 + rand.nextInt(20);
            setEtat(RONDE);
        }
    }

    // ------------------------------------------------------------------ effets côté client
    private void effetsClient() {
        int e = etat();
        // tangage (piqué / montée) et roulis dans les virages, pour le modèle
        prevTangage = tangage;
        prevRoulis = roulis;
        double dx = posX - prevPosX, dy = posY - prevPosY, dz = posZ - prevPosZ;
        boolean vole = e != PERCHE && e != IMPACT && e != OEUF && deathTime == 0;
        float tg = 0, rl = 0;
        if (vole) {
            tg = (float) Math.toDegrees(Math.atan2(dy, Math.max(Math.sqrt(dx * dx + dz * dz), 0.08)));
            tg = MathHelper.clamp(tg, e == PLONGEE ? -70F : -40F, 40F);
            rl = MathHelper.clamp(-MathHelper.wrapDegrees(rotationYaw - prevRotationYaw) * 4F, -38F, 38F);
        }
        tangage += (tg - tangage) * 0.18F;
        roulis += (rl - roulis) * 0.15F;
        Particules.phenix(this, e, tClient);
    }

    // ------------------------------------------------------------------ GeckoLib
    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<EntityPhenix>(this, "corps", 5, this::predicat));
    }

    private <E extends IAnimatable> PlayState predicat(AnimationEvent<E> ev) {
        AnimationBuilder b = new AnimationBuilder();
        if (deathTime > 0 || getHealth() <= 0) {
            b.addAnimation("animation.phenix.mort", false);
        } else {
            switch (etat()) {
                case BOULE:
                    b.addAnimation("animation.phenix.boule", false).addAnimation("animation.phenix.vol", true);
                    break;
                case SOUFFLE:
                    b.addAnimation("animation.phenix.souffle", true);
                    break;
                case PLUIE:
                    b.addAnimation("animation.phenix.pluie", true);
                    break;
                case PLONGEE:
                    b.addAnimation(tClient > 25 ? "animation.phenix.plongee" : "animation.phenix.vol", true);
                    break;
                case TEMPETE:
                    b.addAnimation("animation.phenix.tempete", true);
                    break;
                case PERCHE:
                case IMPACT:
                    b.addAnimation("animation.phenix.perche", true);
                    break;
                case OEUF:
                    b.addAnimation("animation.phenix.oeuf", true);
                    break;
                case RENAISSANCE:
                    b.addAnimation("animation.phenix.renaissance", false).addAnimation("animation.phenix.vol", true);
                    break;
                case APPROCHE:
                case ARRIVEE:
                    b.addAnimation(motionY < -0.05 ? "animation.phenix.plane" : "animation.phenix.vol", true);
                    break;
                case NOVA:
                    b.addAnimation("animation.phenix.nova", false).addAnimation("animation.phenix.vol", true);
                    break;
                default:
                    b.addAnimation((ticksExisted / 70) % 3 == 2 ? "animation.phenix.plane" : "animation.phenix.vol", true);
            }
        }
        ev.getController().setAnimation(b);
        return PlayState.CONTINUE;
    }

    @Override
    public AnimationFactory getFactory() {
        return usine;
    }

    // ------------------------------------------------------------------ sauvegarde
    @Override
    public void writeEntityToNBT(NBTTagCompound c) {
        super.writeEntityToNBT(c);
        c.setDouble("AncreX", ax);
        c.setDouble("AncreY", ay);
        c.setDouble("AncreZ", az);
        c.setBoolean("Ancre", ancre);
        c.setBoolean("Renaissance", renaissanceFaite);
        c.setBoolean("Embrase", embrase);
        c.setBoolean("Oeuf", etat == OEUF);
        c.setInteger("TempsOeuf", etat == OEUF ? tEtat : 0);
        c.setFloat("PvOeuf", pvOeuf);
        c.setByte("Phase", (byte) phase);
        c.setDouble("Echelle", echelle);
        if (appat != null) c.setLong("Appat", appat.toLong());
        c.setBoolean("Arrivee", etat == ARRIVEE);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound c) {
        super.readEntityFromNBT(c);
        ax = c.getDouble("AncreX");
        ay = c.getDouble("AncreY");
        az = c.getDouble("AncreZ");
        ancre = c.getBoolean("Ancre");
        renaissanceFaite = c.getBoolean("Renaissance");
        embrase = c.getBoolean("Embrase");
        dataManager.set(EMBRASE, embrase);
        // monde de la 1.0 : pas de phase enregistrée, on la déduit
        // la vie enregistrée est en unités de l'échelle d'alors (1 en 1.0) ; les PV du classeur ont pu changer depuis
        echelle = c.hasKey("Echelle") ? Math.max(1, c.getDouble("Echelle")) : 1;
        entrerPhase(c.hasKey("Phase") ? MathHelper.clamp(c.getByte("Phase"), 1, 3) : renaissanceFaite ? 3 : embrase ? 2 : 1);
        if (getHealth() > getMaxHealth()) setHealth(getMaxHealth());
        appat = c.hasKey("Appat") ? BlockPos.fromLong(c.getLong("Appat")) : null;
        if (c.getBoolean("Oeuf")) {
            setEtat(OEUF);
            tEtat = c.getInteger("TempsOeuf");
            pvOeuf = c.getFloat("PvOeuf");
            dataManager.set(OEUF_PV, Math.max(0F, (float) (pvOeuf / PhenixConfig.pointsDeVieOeuf)));
            setNoGravity(false);
        } else {
            setEtat(c.getBoolean("Arrivee") && appat != null ? ARRIVEE : RONDE);
            setNoGravity(true);
        }
        if (hasCustomName()) barre.setName(getDisplayName());
    }

    // ------------------------------------------------------------------ pour la commande
    public String resume() {
        return String.format("%s : %.0f / %.0f PV, phase %d, %s%s, à %d %d %d", getDisplayName().getUnformattedText(), pv(), pvMax(),
                phase, NOMS[etat()], embrase ? " (embrasé)" : "", MathHelper.floor(posX), MathHelper.floor(posY), MathHelper.floor(posZ));
    }
}
