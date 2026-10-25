package fr.lenerfvoeux.hxrp.bestiaire.entite;

import fr.lenerfvoeux.hxrp.bestiaire.Durees;
import fr.lenerfvoeux.hxrp.bestiaire.Espece;
import fr.lenerfvoeux.hxrp.bestiaire.HxrpBestiaire;
import fr.lenerfvoeux.hxrp.bestiaire.stats.Stats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Un sbire : appelé par son boss, il le défend et disparaît avec lui (ou s'il n'est plus là depuis 5 s). Vie, dégâts,
 * vitesse et particularité viennent de l'onglet Sbires du classeur. Pas de butin. Comportements : au contact, en vol
 * (dard), à l'arc (garde ses distances), au bouclier (bloque les coups de face).
 */
public abstract class EntitySbire extends EntityMob implements Bestiole {
    public enum Comportement { CONTACT, VOL, ARCHER, BOUCLIER, NAGE }

    private static final DataParameter<String> ANIM = EntityDataManager.createKey(EntitySbire.class, DataSerializers.STRING);
    private static final DataParameter<Integer> ANIM_N = EntityDataManager.createKey(EntitySbire.class, DataSerializers.VARINT);
    private static final Pattern SECONDES = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*s");

    protected final Espece espece;
    protected final Comportement comportement;
    private final AnimationFactory usine = new AnimationFactory(this);
    private UUID boss;
    private int sansBoss;
    protected double degats = 10;
    protected String particularite = "";
    protected EntityPlayer cible;
    private int recharge = 20, tAttaque = -1, dureeAttaque;
    private boolean coupEnCours;
    private int animVue = -1;

    protected EntitySbire(World w, Espece e, Comportement c) {
        super(w);
        this.espece = e;
        this.comportement = c;
        setSize(e.largeur, e.hauteur);
        experienceValue = 0;
        if (c == Comportement.VOL) setNoGravity(true);
    }

    public static EntitySbire creer(Espece e, World w) {
        try {
            return (EntitySbire) e.classe.getConstructor(World.class).newInstance(w);
        } catch (ReflectiveOperationException ex) {
            HxrpBestiaire.LOG.error("Bestiaire : impossible de créer le sbire {}", e.id, ex);
            return null;
        }
    }

    /** Animations d'attaque (une au hasard à chaque coup) ; la première sert au tir pour un archer. */
    protected abstract String[] attaques();

    /** Son boss, s'il est chargé et vivant. */
    protected EntityBoss boss() {
        if (boss == null || !(world instanceof WorldServer)) return null;
        Entity b = ((WorldServer) world).getEntityFromUuid(boss);
        return b instanceof EntityBoss && b.isEntityAlive() ? (EntityBoss) b : null;
    }

    /** Un comportement propre à l'espèce, avant le comportement commun ; vrai s'il remplace celui-ci ce tick. */
    protected boolean comportementPropre() {
        return false;
    }

    /** Rattache le sbire à son boss et lui donne ses stats. */
    void lier(EntityBoss b, Stats.Sbire s) {
        boss = b.getUniqueID();
        regler(s.pv, s.degats, s.vitesse, s.particularite);
        cible = b.cible;
        if (!espece.apparition.isEmpty()) anim(espece.apparition, Durees.ticks(espece.modele, espece.apparition, 20), false);
    }

    private void regler(double pv, double degats, double vitesse, String particularite) {
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(Math.min(1024, pv));
        setHealth(getMaxHealth());
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(vitesse);
        this.degats = degats;
        this.particularite = particularite == null ? "" : particularite;
    }

    @Override
    public Espece espece() {
        return espece;
    }

    @Override
    public float echelleRendu() {
        return espece.echelle;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(40.0);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(4.0);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(ANIM, "");
        dataManager.register(ANIM_N, 0);
    }

    @Override
    public String animationCourante() {
        return dataManager.get(ANIM);
    }

    private void anim(String a, int duree, boolean coup) {
        coupEnCours = coup;
        dureeAttaque = duree;
        tAttaque = 0;
        dataManager.set(ANIM, a);
        dataManager.set(ANIM_N, dataManager.get(ANIM_N) + 1);
    }

    @Override
    protected void initEntityAI() {
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    public void fall(float distance, float multiplicateur) {
        if (comportement != Comportement.VOL && comportement != Comportement.NAGE) super.fall(distance, multiplicateur);
    }

    @Override
    public boolean isPushedByWater() {
        return comportement != Comportement.NAGE;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return comportement == Comportement.NAGE;
    }

    @Override
    protected boolean canDropLoot() {
        return false;
    }

    @Override
    protected void dropFewItems(boolean touche, int butin) {
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return espece.sonAmbiant;
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
    protected float getSoundPitch() {
        return espece.hauteurSon * (0.9F + rand.nextFloat() * 0.2F);
    }

    @Override
    public void travel(float strafe, float vertical, float forward) {
        if (comportement == Comportement.NAGE && isServerWorld()) {
            // dans l'eau il nage librement ; hors de l'eau, il retombe et se traîne
            setNoGravity(isInWater());
            if (isInWater()) {
                move(MoverType.SELF, motionX, motionY, motionZ);
                motionX *= 0.85;
                motionY *= 0.85;
                motionZ *= 0.85;
                return;
            }
        }
        if (comportement == Comportement.VOL && isServerWorld()) {
            move(MoverType.SELF, motionX, motionY, motionZ);
            motionX *= 0.9;
            motionY *= 0.9;
            motionZ *= 0.9;
            return;
        }
        super.travel(strafe, vertical, forward);
    }

    @Override
    public boolean attackEntityFrom(DamageSource src, float n) {
        if (src.getTrueSource() instanceof EntityBoss || src.getTrueSource() instanceof EntitySbire) return false;
        if (comportement == Comportement.BOUCLIER && src.getTrueSource() != null && !src.isUnblockable()) {
            // bouclier : un coup venu de face est presque entièrement arrêté
            Entity a = src.getImmediateSource() != null ? src.getImmediateSource() : src.getTrueSource();
            Vec3d f = getLook(1F);
            double dx = a.posX - posX, dz = a.posZ - posZ, l = Math.sqrt(dx * dx + dz * dz);
            if (l > 0.01 && (dx * f.x + dz * f.z) / l > 0.5) {
                world.playSound(null, posX, posY, posZ, SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.HOSTILE, 1F, 0.9F + rand.nextFloat() * 0.2F);
                n *= 0.1F;
            }
        }
        Entity e = src.getTrueSource();
        if (e instanceof EntityPlayer && EntityBoss.valable((EntityPlayer) e)) cible = (EntityPlayer) e;
        return super.attackEntityFrom(src, n);
    }

    // ================================================================== boucle
    @Override
    protected void updateAITasks() {
        if (boss != null && ticksExisted % 20 == 0) {
            Entity b = world instanceof WorldServer ? ((WorldServer) world).getEntityFromUuid(boss) : null;
            if (b == null || !b.isEntityAlive()) {
                if (++sansBoss >= 5) {
                    if (world instanceof WorldServer)
                        ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_NORMAL, posX, posY + height / 2, posZ, 12, 0.3, 0.3, 0.3, 0.02);
                    setDead();
                    return;
                }
            } else {
                sansBoss = 0;
                if (cible == null && b instanceof EntityBoss) cible = ((EntityBoss) b).cible;
            }
        }
        if (cible != null && (!EntityBoss.valable(cible) || cible.world != world || getDistanceSq(cible) > 48 * 48)) cible = null;
        if (cible == null && ticksExisted % 10 == 0) cible = world.getNearestAttackablePlayer(this, 24, 12);
        setAttackTarget(cible);
        if (tAttaque >= 0) {
            tAttaque++;
            if (coupEnCours && tAttaque == Math.max(1, (int) (dureeAttaque * 0.45))) frapper();
            if (tAttaque >= dureeAttaque) {
                tAttaque = -1;
                dataManager.set(ANIM, "");
            }
        }
        recharge--;
        if (comportementPropre()) return;
        if (cible == null) {
            if (comportement == Comportement.VOL || (comportement == Comportement.NAGE && isInWater())) planer(posX, posY, posZ);
            return;
        }
        double d = getDistance(cible);
        switch (comportement) {
            case NAGE:
                if (!isInWater()) {
                    if (ticksExisted % 10 == 0 || getNavigator().noPath()) getNavigator().tryMoveToEntityLiving(cible, 0.6);
                } else {
                    planer(cible.posX, Math.min(cible.posY + cible.height * 0.4, posY + 1), cible.posZ);
                }
                faceA(cible);
                if (d < width / 2 + 1.8 && recharge <= 0 && tAttaque < 0) attaquer();
                break;
            case VOL:
                planer(cible.posX, cible.posY + cible.height * 0.6, cible.posZ);
                faceA(cible);
                if (d < width / 2 + 1.8 && recharge <= 0 && tAttaque < 0) attaquer();
                break;
            case ARCHER:
                if (d > 16) getNavigator().tryMoveToEntityLiving(cible, 1.0);
                else if (d < 7) fuir();
                else getNavigator().clearPath();
                faceA(cible);
                if (d < 22 && recharge <= 0 && tAttaque < 0 && canEntityBeSeen(cible)) attaquer();
                break;
            default:
                if (d > width / 2 + 1.5) {
                    if (ticksExisted % 10 == 0 || getNavigator().noPath()) getNavigator().tryMoveToEntityLiving(cible, 1.0);
                } else {
                    getNavigator().clearPath();
                    faceA(cible);
                    if (recharge <= 0 && tAttaque < 0) attaquer();
                }
        }
    }

    private void attaquer() {
        String[] a = attaques();
        String nom = comportement == Comportement.ARCHER ? a[0] : a[rand.nextInt(a.length)];
        anim(nom, Durees.ticks(espece.modele, nom, 16), true);
        recharge = comportement == Comportement.ARCHER ? 50 + rand.nextInt(30) : dureeAttaque + 10 + rand.nextInt(15);
    }

    /** Au moment du coup (ou du tir). */
    private void frapper() {
        if (cible == null || !EntityBoss.valable(cible)) return;
        if (comportement == Comportement.ARCHER) {
            tirer(cible);
            return;
        }
        if (getDistance(cible) > width / 2 + 2.5) return;
        if (cible.attackEntityFrom(DamageSource.causeMobDamage(this), (float) degats)) {
            Effets.appliquer(cible, this, particularite, secondes(particularite, 4), false);
            Effets.repousser(cible, cible.posX - posX, cible.posZ - posZ, 0.3, 0.2);
        }
    }

    /** Tir d'un archer : une flèche (de glace pour les gobelins), selon l'espèce. */
    protected void tirer(EntityPlayer c) {
        EntityProjectileBoss p = new EntityProjectileBoss(world, this, typeDeTir(), (float) degats, particularite, secondes(particularite, 3));
        p.viser(c, 1.6F, 2F);
        world.spawnEntity(p);
        playSound(SoundEvents.ENTITY_SKELETON_SHOOT, 1F, 1F / (rand.nextFloat() * 0.4F + 0.8F));
    }

    protected EntityProjectileBoss.Type typeDeTir() {
        return EntityProjectileBoss.Type.FLECHE;
    }

    static double secondes(String texte, double defaut) {
        Matcher m = SECONDES.matcher(texte == null ? "" : texte);
        return m.find() ? Double.parseDouble(m.group(1).replace(',', '.')) : defaut;
    }

    private void planer(double x, double y, double z) {
        double dx = x - posX, dy = y - posY, dz = z - posZ, d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double v = Math.min(getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue() * 1.4, d * 0.25 + 0.01);
        if (d > 0.05) {
            motionX += (dx / d * v - motionX) * 0.2;
            motionY += (dy / d * v - motionY) * 0.2 + Math.sin(ticksExisted * 0.3) * 0.004;
            motionZ += (dz / d * v - motionZ) * 0.2;
        }
        if (collidedHorizontally) motionY = Math.max(motionY, 0.12);
    }

    private void fuir() {
        double dx = posX - cible.posX, dz = posZ - cible.posZ, l = Math.max(0.1, Math.sqrt(dx * dx + dz * dz));
        getNavigator().tryMoveToXYZ(posX + dx / l * 6, posY, posZ + dz / l * 6, 1.1);
    }

    private void faceA(Entity e) {
        float cap = (float) (MathHelper.atan2(e.posZ - posZ, e.posX - posX) * (180.0 / Math.PI)) - 90F;
        float d = MathHelper.wrapDegrees(cap - rotationYaw);
        rotationYaw += MathHelper.clamp(d, -20F, 20F);
        renderYawOffset = rotationYaw;
        rotationYawHead = rotationYaw;
        getLookHelper().setLookPositionWithEntity(e, 30F, 30F);
    }

    // ================================================================== GeckoLib
    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<EntitySbire>(this, "corps", 3, this::predicat));
    }

    private <E extends software.bernie.geckolib3.core.IAnimatable> PlayState predicat(AnimationEvent<E> ev) {
        AnimationBuilder b = new AnimationBuilder();
        String fond = ev.isMoving() || (comportement == Comportement.VOL && motionX * motionX + motionZ * motionZ > 0.004) ? espece.marche : espece.repos;
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

    @Override
    protected void onDeathUpdate() {
        ++deathTime;
        if (deathTime >= Math.min(60, Durees.ticks(espece.modele, espece.mort, 20)) && !world.isRemote) setDead();
    }

    // ================================================================== sauvegarde
    @Override
    public void writeEntityToNBT(NBTTagCompound c) {
        super.writeEntityToNBT(c);
        if (boss != null) c.setUniqueId("Boss", boss);
        c.setDouble("Degats", degats);
        c.setString("Particularite", particularite);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound c) {
        super.readEntityFromNBT(c);
        boss = c.hasUniqueId("Boss") ? c.getUniqueId("Boss") : null;
        degats = c.hasKey("Degats") ? c.getDouble("Degats") : degats;
        particularite = c.getString("Particularite");
    }
}
