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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * Projectile d'un boss ou d'un sbire : dard empoisonné, boule d'âme ou orbe sombre (à tête chercheuse), flèche, jet de
 * toile, lance de glace (qui transperce), météore sombre et stalactite (qui éclatent en zone en touchant le sol)… Il ne
 * touche que les joueurs (il traverse les autres créatures du bestiaire), applique les dégâts et l'effet du classeur, et
 * ne casse rien (la toile se pose seulement dans l'air, et se défait toute seule). Rendu comme un objet plat
 * (RenduProjectile), plus ou moins grand, avec une traînée de particules propre à son type.
 */
public class EntityProjectileBoss extends EntityThrowable {
    public enum Type {
        DARD(0.02F, EnumParticleTypes.CRIT, false, 0, false, 1F),
        AME(0F, EnumParticleTypes.SPELL_WITCH, true, 0, false, 1F),
        FLECHE(0.05F, EnumParticleTypes.CRIT, false, 0, false, 1F),
        TOILE(0.03F, EnumParticleTypes.SNOWBALL, false, 0, false, 1.3F),
        LANCE_GLACE(0.01F, EnumParticleTypes.SNOW_SHOVEL, false, 0, true, 1.9F),
        FLECHE_GLACE(0.05F, EnumParticleTypes.SNOW_SHOVEL, false, 0, false, 1F),
        ORBE(0F, EnumParticleTypes.SPELL_MOB, true, 0, false, 1.7F),
        METEORE(0.06F, EnumParticleTypes.SMOKE_LARGE, false, 3.5, false, 2.6F),
        STALACTITE(0.08F, EnumParticleTypes.SNOW_SHOVEL, false, 2.5, false, 2.2F),
        MAGMA(0.05F, EnumParticleTypes.FLAME, false, 2.5, false, 1.8F);

        public final float gravite;
        public final EnumParticleTypes trainee;
        public final boolean chercheur;
        /** Rayon de l'éclat (0 : il ne touche que le joueur atteint). */
        public final double rayonImpact;
        /** Il transperce les joueurs (chacun touché une fois) au lieu de s'arrêter au premier. */
        public final boolean percant;
        /** Taille du rendu (1 : comme une boule de neige). */
        public final float taille;

        Type(float gravite, EnumParticleTypes trainee, boolean chercheur, double rayonImpact, boolean percant, float taille) {
            this.gravite = gravite;
            this.trainee = trainee;
            this.chercheur = chercheur;
            this.rayonImpact = rayonImpact;
            this.percant = percant;
            this.taille = taille;
        }
    }

    private static final DataParameter<Byte> TYPE = EntityDataManager.createKey(EntityProjectileBoss.class, DataSerializers.BYTE);

    private float degats;
    private String effet = "";
    private double dureeEffet;
    private Entity guide;
    private int vie = 200;
    private final java.util.Set<java.util.UUID> transperces = new java.util.HashSet<>();

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

    /** Tire vers un point (attaque forcée sans cible). */
    public void viser(Vec3d p, float vitesse, float imprecision) {
        double dx = p.x - posX, dy = p.y + 0.5 - posY, dz = p.z - posZ;
        double h = Math.sqrt(dx * dx + dz * dz);
        if (type().gravite > 0) dy += h * type().gravite * 4;
        shoot(dx, dy, dz, vitesse, imprecision);
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

    /** Tombe tout droit d'un point (météores, stalactites), avec une petite vitesse de départ vers le bas. */
    public EntityProjectileBoss tomber() {
        motionX = (rand.nextDouble() - 0.5) * 0.04;
        motionY = -0.3;
        motionZ = (rand.nextDouble() - 0.5) * 0.04;
        return this;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (world.isRemote) {
            Type t = type();
            int n = t.taille > 1.5F ? 4 : 2;
            double e = 0.15 * t.taille;
            for (int i = 0; i < n; i++)
                world.spawnParticle(t.trainee, posX + (rand.nextDouble() - 0.5) * e, posY + (rand.nextDouble() - 0.5) * e, posZ + (rand.nextDouble() - 0.5) * e,
                        0, 0, 0);
            if (t == Type.METEORE) world.spawnParticle(EnumParticleTypes.SPELL_WITCH, posX, posY, posZ, 0, 0, 0);
            if (t == Type.MAGMA && rand.nextInt(3) == 0) world.spawnParticle(EnumParticleTypes.LAVA, posX, posY, posZ, 0, 0, 0);
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
        Type t = type();
        Entity e = r.entityHit;
        if (e != null) {
            if (e == getThrower() || e instanceof EntityBoss || e instanceof EntitySbire || e instanceof EntityProjectileBoss) return;
            if (!(e instanceof EntityPlayer) || !EntityBoss.valable((EntityPlayer) e)) return;
            EntityPlayer p = (EntityPlayer) e;
            if (t.rayonImpact <= 0) {
                if (t.percant && !transperces.add(p.getUniqueID())) return;
                toucher(p, 1F);
                if (t == Type.TOILE) toile(new BlockPos(p.posX, p.posY + 0.2, p.posZ));
                if (t.percant) {
                    eclat(6);
                    return;
                }
            }
        } else if (t == Type.TOILE && r.getBlockPos() != null) {
            toile(r.getBlockPos().offset(r.sideHit));
        }
        if (t.rayonImpact > 0) {
            // éclat en zone : dégâts dégressifs vers le bord
            for (EntityPlayer p : world.getEntitiesWithinAABB(EntityPlayer.class, getEntityBoundingBox().grow(t.rayonImpact))) {
                double d = p.getDistance(posX, posY, posZ);
                if (d <= t.rayonImpact && EntityBoss.valable(p)) toucher(p, (float) (1 - 0.5 * d / t.rayonImpact));
            }
        }
        eclat(t.rayonImpact > 0 ? 30 : 12);
        if (getThrower() instanceof EntityBoss) ((EntityBoss) getThrower()).surImpact(this, new net.minecraft.util.math.Vec3d(posX, posY, posZ));
        setDead();
    }

    private void toucher(EntityPlayer p, float part) {
        if (p.attackEntityFrom(DamageSource.causeThrownDamage(this, getThrower()).setProjectile(), degats * part))
            Effets.appliquer(p, this, effet, dureeEffet, true);
    }

    /** Une toile de Xerra dans l'air, au point d'impact (elle ne remplace aucun bloc). */
    private void toile(BlockPos b) {
        if (world.isAirBlock(b) && fr.lenerfvoeux.hxrp.bestiaire.Registre.TOILE != null)
            world.setBlockState(b, fr.lenerfvoeux.hxrp.bestiaire.Registre.TOILE.getDefaultState());
    }

    private void eclat(int n) {
        Type t = type();
        if (world instanceof WorldServer) {
            WorldServer ws = (WorldServer) world;
            switch (t) {
                case AME:
                case ORBE:
                    ws.spawnParticle(EnumParticleTypes.SPELL_WITCH, posX, posY, posZ, n, 0.3, 0.3, 0.3, 0.05);
                    break;
                case METEORE:
                    ws.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, posX, posY, posZ, 4, 1.0, 0.3, 1.0, 0.0);
                    ws.spawnParticle(EnumParticleTypes.SPELL_WITCH, posX, posY + 0.5, posZ, n, t.rayonImpact / 2, 0.4, t.rayonImpact / 2, 0.1);
                    break;
                case STALACTITE:
                case LANCE_GLACE:
                case FLECHE_GLACE:
                    ws.spawnParticle(EnumParticleTypes.BLOCK_CRACK, posX, posY + 0.3, posZ, n, 0.4, 0.3, 0.4, 0.1,
                            net.minecraft.block.Block.getStateId(net.minecraft.init.Blocks.PACKED_ICE.getDefaultState()));
                    ws.spawnParticle(EnumParticleTypes.SNOW_SHOVEL, posX, posY + 0.3, posZ, n, 0.6, 0.3, 0.6, 0.05);
                    break;
                case TOILE:
                    ws.spawnParticle(EnumParticleTypes.SNOWBALL, posX, posY, posZ, n, 0.4, 0.4, 0.4, 0.05);
                    break;
                case MAGMA:
                    ws.spawnParticle(EnumParticleTypes.LAVA, posX, posY + 0.3, posZ, n / 2, 0.8, 0.2, 0.8, 0.0);
                    ws.spawnParticle(EnumParticleTypes.FLAME, posX, posY + 0.3, posZ, n, 1.0, 0.3, 1.0, 0.05);
                    break;
                default:
                    ws.spawnParticle(EnumParticleTypes.CRIT, posX, posY, posZ, n, 0.2, 0.2, 0.2, 0.05);
            }
        }
        net.minecraft.util.SoundEvent s;
        switch (t) {
            case AME:
            case ORBE:
                s = SoundEvents.ENTITY_ENDERMEN_TELEPORT;
                break;
            case METEORE:
                s = SoundEvents.ENTITY_GENERIC_EXPLODE;
                break;
            case MAGMA:
                s = SoundEvents.BLOCK_LAVA_POP;
                break;
            case STALACTITE:
            case LANCE_GLACE:
            case FLECHE_GLACE:
                s = SoundEvents.BLOCK_GLASS_BREAK;
                break;
            case TOILE:
                s = SoundEvents.ENTITY_SLIME_SQUISH;
                break;
            default:
                s = SoundEvents.ENTITY_ARROW_HIT;
        }
        world.playSound(null, posX, posY, posZ, s, SoundCategory.HOSTILE, t.rayonImpact > 0 ? 2.5F : 0.8F, t == Type.METEORE ? 0.7F : 1.2F);
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
