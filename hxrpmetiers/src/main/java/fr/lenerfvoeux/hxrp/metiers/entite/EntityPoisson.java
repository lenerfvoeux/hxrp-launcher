package fr.lenerfvoeux.hxrp.metiers.entite;

import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.List;

/**
 * Poisson (ou crevette) : nage dans l'eau en changeant de cap de temps en temps, fuit les joueurs
 * trop proches, suit ses congénères s'il vit en banc, reste près du fond s'il est benthique.
 * Hors de l'eau il se débat et finit par s'asphyxier. Donne son ingrédient quand on le pêche au combat.
 */
public abstract class EntityPoisson extends EntityWaterMob {
    private final String ingredient;
    private final double vitesse, sante;
    private final boolean banc, fond;
    private final int min, max;
    private double vx, vy, vz;
    private int changer;

    protected EntityPoisson(World w, String ingredient, float largeur, float hauteur, double vitesse, double sante,
                            boolean banc, boolean fond, int min, int max) {
        super(w);
        this.ingredient = ingredient;
        this.vitesse = vitesse;
        this.sante = sante;
        this.banc = banc;
        this.fond = fond;
        this.min = min;
        this.max = max;
        setSize(largeur, hauteur);
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(sante);
        setHealth((float) sante);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(4.0);
    }

    @Override protected boolean canTriggerWalking() { return false; }
    @Override protected SoundEvent getAmbientSound() { return null; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ENTITY_GUARDIAN_FLOP; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_GUARDIAN_FLOP; }
    @Override protected float getSoundVolume() { return 0.4f; }

    @Override
    public boolean getCanSpawnHere() {
        return posY > 30 && posY < world.getSeaLevel() + 1 && super.getCanSpawnHere();
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (isInWater()) {
            if (!world.isRemote) {
                boolean peur = fuir();
                if (!peur && (--changer <= 0 || collidedHorizontally)) choisir();
                motionX += (vx - motionX) * 0.1;
                motionY += (vy - motionY) * 0.1;
                motionZ += (vz - motionZ) * 0.1;
            }
            double h = Math.sqrt(motionX * motionX + motionZ * motionZ);
            if (h > 0.003) {
                float cap = (float) (MathHelper.atan2(motionZ, motionX) * (180.0 / Math.PI)) - 90.0f;
                renderYawOffset += MathHelper.wrapDegrees(cap - renderYawOffset) * 0.25f;
                rotationYaw = renderYawOffset;
                rotationYawHead = renderYawOffset;
            }
            rotationPitch = (float) (-MathHelper.atan2(motionY, Math.max(h, 0.01)) * (180.0 / Math.PI)) * 0.6f;
        } else if (!world.isRemote && onGround && rand.nextInt(12) == 0) {
            motionY = 0.3;
            motionX = (rand.nextFloat() * 2 - 1) * 0.15f;
            motionZ = (rand.nextFloat() * 2 - 1) * 0.15f;
            playSound(SoundEvents.ENTITY_GUARDIAN_FLOP, 0.5f, 1.2f);
        }
    }

    @Override
    public void travel(float strafe, float vertical, float forward) {
        if (isInWater()) move(MoverType.SELF, motionX, motionY, motionZ);
        else super.travel(strafe, vertical, forward);
    }

    /** Nouveau cap : au hasard (ou avec le banc), en restant dans l'eau. */
    private void choisir() {
        changer = 30 + rand.nextInt(70);
        double a = rand.nextDouble() * Math.PI * 2;
        double v = vitesse * (0.5 + rand.nextDouble() * 0.5);
        vx = Math.cos(a) * v;
        vz = Math.sin(a) * v;
        vy = (rand.nextDouble() - 0.5) * v * 0.5;
        if (fond) vy = -Math.abs(vy) * 0.6;
        if (banc) {
            List<EntityPoisson> voisins = world.getEntitiesWithinAABB(getClass(), getEntityBoundingBox().grow(8.0));
            EntityPoisson chef = null;
            for (EntityPoisson p : voisins) if (p != this && (chef == null || p.getEntityId() < chef.getEntityId())) chef = p;
            if (chef != null && chef.getEntityId() < getEntityId()) {
                vx = chef.vx * 0.8 + (chef.posX - posX) * 0.02;
                vy = chef.vy * 0.8 + (chef.posY - posY) * 0.02;
                vz = chef.vz * 0.8 + (chef.posZ - posZ) * 0.02;
                changer = 10 + rand.nextInt(10);
            }
        }
        garderDansLEau();
    }

    private void garderDansLEau() {
        BlockPos devant = new BlockPos(posX + vx * 20, posY + vy * 20, posZ + vz * 20);
        if (!world.getBlockState(devant).getMaterial().isLiquid()) {
            vx = -vx;
            vz = -vz;
            vy = -Math.abs(vy) - 0.01;
        }
        if (!world.getBlockState(new BlockPos(posX, posY + height + 0.4, posZ)).getMaterial().isLiquid() && vy > 0) vy = -vy;
    }

    /** Un joueur approche : on file dans la direction opposée. */
    private boolean fuir() {
        EntityPlayer p = world.getClosestPlayerToEntity(this, 4.0);
        if (p == null || p.isSpectator() || p.isSneaking()) return false;
        double dx = posX - p.posX, dz = posZ - p.posZ, d = Math.max(0.1, Math.sqrt(dx * dx + dz * dz));
        vx = dx / d * vitesse * 1.8;
        vz = dz / d * vitesse * 1.8;
        vy = (posY - p.posY) > 0 ? vitesse * 0.3 : -vitesse * 0.3;
        garderDansLEau();
        changer = 20;
        return true;
    }

    @Override
    protected void dropFewItems(boolean touche, int butin) {
        Item i = ModRegistry.FOOD.get(ingredient);
        if (i != null) entityDropItem(new net.minecraft.item.ItemStack(i, min + rand.nextInt(max - min + 1 + butin)), 0.0f);
    }

    // ------------------------------------------------------------------ les espèces
    public static class Saumon extends EntityPoisson {
        public Saumon(World w) { super(w, "saumon", 0.6f, 0.45f, 0.14, 6, false, false, 1, 2); }
    }

    public static class Truite extends EntityPoisson {
        public Truite(World w) { super(w, "truite", 0.5f, 0.4f, 0.12, 5, false, false, 1, 1); }
    }

    public static class Thon extends EntityPoisson {
        public Thon(World w) { super(w, "thon", 0.9f, 0.7f, 0.18, 10, false, false, 2, 3); }
    }

    public static class Cabillaud extends EntityPoisson {
        public Cabillaud(World w) { super(w, "cabillaud", 0.6f, 0.45f, 0.1, 6, false, true, 1, 2); }
    }

    public static class Sardine extends EntityPoisson {
        public Sardine(World w) { super(w, "sardine", 0.35f, 0.25f, 0.13, 3, true, false, 1, 1); }
    }

    public static class Maquereau extends EntityPoisson {
        public Maquereau(World w) { super(w, "maquereau", 0.4f, 0.3f, 0.15, 3, true, false, 1, 1); }
    }

    public static class Anchois extends EntityPoisson {
        public Anchois(World w) { super(w, "anchois", 0.3f, 0.2f, 0.13, 2, true, false, 1, 1); }
    }

    public static class Crevette extends EntityPoisson {
        public Crevette(World w) { super(w, "crevette", 0.35f, 0.25f, 0.06, 2, false, true, 1, 2); }
    }
}
