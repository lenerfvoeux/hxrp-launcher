package fr.lenerfvoeux.hxrp.bestiaire.entite;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * Projectile d'un boss ou d'un sbire : dard empoisonné, boule d'âme (à tête chercheuse), flèche… Il ne touche que les
 * joueurs (il traverse les autres créatures du bestiaire), applique les dégâts et l'effet du classeur, et ne casse rien.
 * Rendu comme un objet plat (RenduProjectile), avec une traînée de particules propre à son type.
 */
public class EntityProjectileBoss extends EntityThrowable {
    public enum Type {
        DARD(0.02F, EnumParticleTypes.CRIT, false),
        AME(0F, EnumParticleTypes.SPELL_WITCH, true),
        FLECHE(0.05F, EnumParticleTypes.CRIT, false);

        public final float gravite;
        public final EnumParticleTypes trainee;
        public final boolean chercheur;

        Type(float gravite, EnumParticleTypes trainee, boolean chercheur) {
            this.gravite = gravite;
            this.trainee = trainee;
            this.chercheur = chercheur;
        }
    }

    private static final DataParameter<Byte> TYPE = EntityDataManager.createKey(EntityProjectileBoss.class, DataSerializers.BYTE);

    private float degats;
    private String effet = "";
    private double dureeEffet;
    private Entity guide;
    private int vie = 200;

    public EntityProjectileBoss(World w) {
        super(w);
        setSize(0.4F, 0.4F);
    }

    public EntityProjectileBoss(World w, EntityLivingBase tireur, Type t, float degats, String effet, double dureeEffet) {
        super(w, tireur);
        setSize(0.4F, 0.4F);
        dataManager.set(TYPE, (byte) t.ordinal());
        this.degats = degats;
        this.effet = effet == null ? "" : effet;
        this.dureeEffet = dureeEffet;
        setPosition(tireur.posX, tireur.posY + tireur.height * 0.7, tireur.posZ);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(TYPE, (byte) 0);
    }

    public Type type() {
        Type[] t = Type.values();
        return t[Math.max(0, Math.min(t.length - 1, dataManager.get(TYPE)))];
    }

    /** Part depuis un point précis (bout du dard, bâton…). */
    public EntityProjectileBoss depuis(double x, double y, double z) {
        setPosition(x, y, z);
        return this;
    }

    /** Tire vers une entité (en visant un peu au-dessus de ses pieds), à la vitesse et à l'imprécision données. */
    public void viser(Entity c, float vitesse, float imprecision) {
        double dx = c.posX - posX, dy = c.posY + c.height * 0.5 - posY, dz = c.posZ - posZ;
        double h = Math.sqrt(dx * dx + dz * dz);
        if (type().gravite > 0) dy += h * type().gravite * 4;
        shoot(dx, dy, dz, vitesse, imprecision);
        if (type().chercheur) guide = c;
    }

    @Override
    protected float getGravityVelocity() {
        return type().gravite;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (world.isRemote) {
            for (int i = 0; i < 2; i++)
                world.spawnParticle(type().trainee, posX + (rand.nextDouble() - 0.5) * 0.2, posY + (rand.nextDouble() - 0.5) * 0.2,
                        posZ + (rand.nextDouble() - 0.5) * 0.2, 0, 0, 0);
            return;
        }
        if (--vie <= 0) {
            setDead();
            return;
        }
        if (guide != null && guide.isEntityAlive() && guide.world == world) {
            // tête chercheuse : la trajectoire s'infléchit vers la cible
            double dx = guide.posX - posX, dy = guide.posY + guide.height * 0.5 - posY, dz = guide.posZ - posZ;
            double l = Math.sqrt(dx * dx + dy * dy + dz * dz), v = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
            if (l > 0.1 && v > 0.01) {
                motionX += (dx / l * v - motionX) * 0.12;
                motionY += (dy / l * v - motionY) * 0.12;
                motionZ += (dz / l * v - motionZ) * 0.12;
                velocityChanged = true;
            }
        }
    }

    @Override
    protected void onImpact(RayTraceResult r) {
        if (world.isRemote) return;
        Entity e = r.entityHit;
        if (e != null) {
            if (e == getThrower() || e instanceof EntityBoss || e instanceof EntitySbire || e instanceof EntityProjectileBoss) return;
            if (!(e instanceof EntityPlayer) || !EntityBoss.valable((EntityPlayer) e)) return;
            EntityPlayer p = (EntityPlayer) e;
            if (p.attackEntityFrom(DamageSource.causeThrownDamage(this, getThrower()).setProjectile(), degats))
                Effets.appliquer(p, this, effet, dureeEffet, true);
        }
        if (world instanceof WorldServer) {
            EnumParticleTypes eclat = type() == Type.AME ? EnumParticleTypes.SPELL_WITCH : EnumParticleTypes.CRIT;
            ((WorldServer) world).spawnParticle(eclat, posX, posY, posZ, 12, 0.2, 0.2, 0.2, 0.05);
        }
        world.playSound(null, posX, posY, posZ, type() == Type.AME ? SoundEvents.ENTITY_ENDERMEN_TELEPORT : SoundEvents.ENTITY_ARROW_HIT,
                SoundCategory.HOSTILE, 0.8F, 1.2F);
        setDead();
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound c) {
        super.writeEntityToNBT(c);
        c.setByte("Type", dataManager.get(TYPE));
        c.setFloat("Degats", degats);
        c.setString("Effet", effet);
        c.setDouble("DureeEffet", dureeEffet);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound c) {
        super.readEntityFromNBT(c);
        dataManager.set(TYPE, c.getByte("Type"));
        degats = c.getFloat("Degats");
        effet = c.getString("Effet");
        dureeEffet = c.getDouble("DureeEffet");
    }
}
