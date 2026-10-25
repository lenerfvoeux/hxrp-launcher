package fr.lenerfvoeux.hxrp.bestiaire.entite.boss;

import fr.lenerfvoeux.hxrp.bestiaire.Especes;
import fr.lenerfvoeux.hxrp.bestiaire.Sons;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntityBoss;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntityProjectileBoss;
import fr.lenerfvoeux.hxrp.bestiaire.stats.Stats;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Roi des glaces (pics glacés). Il arrive seul, à pied ; il lève les bras pour appeler son escorte (rats des glaces,
 * gobelins boucliers, gobelins archers : 1 à 5 de chaque, le double en phase 2, le triple en phase 3). Coup de bâton,
 * écrasement qui ralentit, lance de glace qui transperce. En phase 2 : rugissement glacial (le sol gèle, on glisse),
 * pluie de stalactites sur des zones marquées. À 0 PV il se fige dans la glace (couronne de givre) puis se libère avec
 * toute sa vie (phase 3) : blizzard qui aveugle et blesse toute l'arène.
 */
public class EntityRoiDesGlaces extends EntityBoss {
    private static final List<Coup> COUPS = Arrays.asList(
            new Coup("coup_baton", "meleeattack_1", Portee.CONTACT, 0.45),
            new Coup("ecrasement", "stomp", Portee.PARTOUT, 0.49),
            new Coup("appel_escorte", "invocation_bras_leves", Portee.PARTOUT, 0.6),
            new Coup("lance_glace", "lance_glace", Portee.DISTANCE, 0.5),
            new Coup("rugissement_glacial", "roar", Portee.PARTOUT, 0.3),
            new Coup("stalactites", "stalactites", Portee.DISTANCE, 0.6).duree(80),
            new Coup("blizzard", "roar", Portee.PARTOUT, 0.3));

    private static final DataParameter<Boolean> BLIZZARD = EntityDataManager.createKey(EntityRoiDesGlaces.class, DataSerializers.BOOLEAN);
    /** Stalactites : délai entre le marquage des zones et la chute (ticks), hauteur de chute. */
    private static final int MARQUAGE = 30;
    private static final double CHUTE = 14;

    private final List<Vec3d> zones = new ArrayList<>();
    private int tZones = -1, blizzardFin;

    public EntityRoiDesGlaces(World w) {
        super(w, Especes.ROI_DES_GLACES);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(BLIZZARD, false);
    }

    @Override
    protected List<Coup> coups() {
        return COUPS;
    }

    public boolean blizzard() {
        return dataManager.get(BLIZZARD);
    }

    @Override
    protected boolean peut(Coup c, double d) {
        if (cible == null) return false;
        switch (c.id) {
            case "ecrasement":
                return d <= rayon(c.id);
            case "appel_escorte":
                return sbiresVivants() < 24;
            case "lance_glace":
                return d > 5;
            case "blizzard":
                return !blizzard();
            default:
                return true;
        }
    }

    @Override
    protected String animRenaissance() {
        return "couronne_de_givre";
    }

    @Override
    protected int dureeRenaissance() {
        return duree("couronne_de_givre", 100);
    }

    @Override
    protected void renaitre() {
        super.renaitre();
        if (world instanceof WorldServer)
            ((WorldServer) world).spawnParticle(EnumParticleTypes.BLOCK_CRACK, posX, posY + height / 2, posZ, 120, width / 2, height / 3, width / 2, 0.3,
                    Block.getStateId(Blocks.PACKED_ICE.getDefaultState()));
        son(SoundEvents.BLOCK_GLASS_BREAK, 4F, 0.5F);
    }

    /** Arrivé à l'appât, il appelle aussitôt son escorte. */
    @Override
    protected void finirArrivee(boolean devorer) {
        super.finirArrivee(devorer);
        if (devorer) forcer("appel_escorte");
    }

    @Override
    protected void debutCoup(Coup c) {
        super.debutCoup(c);
        if ("rugissement_glacial".equals(c.id) || "blizzard".equals(c.id)) son(Sons.GROGNEMENT_GLACIAL, 5F, 0.6F);
    }

    @Override
    protected void impact(Coup c, int n) {
        switch (c.id) {
            case "ecrasement":
                frapperAutour(c, new Vec3d(posX, posY + 0.5, posZ), rayon(c.id) + width / 2);
                zone(posX, posY, posZ, rayon(c.id) + width / 2);
                son(SoundEvents.BLOCK_GLASS_BREAK, 3F, 0.5F);
                break;
            case "appel_escorte":
                int faits = invoquer("ice_rat", Especes.RAT_DES_GLACES, 6) + invoquer("ice_tank", Especes.GOBELIN_BOUCLIER, 5)
                        + invoquer("ice_distance", Especes.GOBELIN_ARCHER, 7);
                if (faits > 0) son(Sons.GROGNEMENT_GLACIAL, 4F, 0.8F);
                break;
            case "lance_glace":
                lance(c);
                break;
            case "rugissement_glacial":
                // tout le sol de l'arène gèle : chaque joueur proche glisse (effet « sol gelé » du classeur)
                frapperAutour(c, new Vec3d(posX, posY + 1, posZ), Math.max(rayon(c.id), 8));
                zone(posX, posY, posZ, Math.max(rayon(c.id), 8));
                break;
            case "stalactites":
                marquer(c);
                break;
            case "blizzard":
                blizzardFin = ticksExisted + (int) Math.max(100, st().attaque(c.id).dureeEffet * 20);
                dataManager.set(BLIZZARD, true);
                zone(posX, posY, posZ, Math.max(rayon(c.id), 16));
                annoncer("§b" + espece.nom + " déchaîne le blizzard !");
                break;
            default:
                super.impact(c, n);
        }
    }

    @Override
    protected void executer(Coup c, int t) {
        if (cible != null && !"stalactites".equals(c.id)) regarder(cible.posX, cible.posZ, 10F);
        getNavigator().clearPath();
    }

    /** Lance de glace : jetée de la main libre, elle transperce tous les joueurs sur son passage. */
    private void lance(Coup c) {
        Stats.Attaque a = st().attaque(c.id);
        Vec3d f = devant();
        // main gauche : en avant et sur le côté gauche du corps
        double x = posX + f.x * (width / 2) + f.z * 1.2, z = posZ + f.z * (width / 2) - f.x * 1.2;
        EntityProjectileBoss p = new EntityProjectileBoss(world, this, EntityProjectileBoss.Type.LANCE_GLACE, degats(c.id), a.effet, a.dureeEffet)
                .depuis(x, posY + height * 0.75, z);
        if (cible != null) p.viser(cible, 2.0F, 0.5F);
        else p.viser(visee(14), 2.0F, 0.5F);
        world.spawnEntity(p);
        son(SoundEvents.ENTITY_SNOWBALL_THROW, 3F, 0.5F);
    }

    /** Stalactites : il marque des zones autour de sa cible ; elles tombent du ciel 1,5 s plus tard. */
    private void marquer(Coup c) {
        zones.clear();
        Vec3d v = visee(8);
        double r = Math.max(3, rayon(c.id));
        zones.add(new Vec3d(v.x, sol(v.x, v.z), v.z));
        for (int i = 0; i < 3 + phase * 2; i++) {
            double an = rand.nextDouble() * Math.PI * 2, d = 2 + rand.nextDouble() * r;
            double x = v.x + Math.cos(an) * d, z = v.z + Math.sin(an) * d;
            zones.add(new Vec3d(x, sol(x, z), z));
        }
        tZones = 0;
        son(SoundEvents.BLOCK_GLASS_HIT, 3F, 0.6F);
    }

    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        if (tZones >= 0 && world instanceof WorldServer) zonesEtChute((WorldServer) world);
        if (blizzard()) tempete();
    }

    /** Pendant le marquage : cercles de givre au sol ; puis une stalactite au-dessus de chaque zone. */
    private void zonesEtChute(WorldServer ws) {
        tZones++;
        if (tZones < MARQUAGE) {
            if (tZones % 4 == 0)
                for (Vec3d z : zones)
                    for (int i = 0; i < 12; i++) {
                        double an = i * Math.PI / 6;
                        ws.spawnParticle(EnumParticleTypes.SNOW_SHOVEL, z.x + Math.cos(an) * 1.6, z.y + 0.1, z.z + Math.sin(an) * 1.6, 1, 0, 0, 0, 0);
                    }
            return;
        }
        Coup c = coup("stalactites");
        Stats.Attaque a = st().attaque("stalactites");
        for (Vec3d z : zones) {
            EntityProjectileBoss p = new EntityProjectileBoss(world, this, EntityProjectileBoss.Type.STALACTITE, degats(c.id), a.effet, a.dureeEffet)
                    .depuis(z.x, z.y + CHUTE, z.z).tomber();
            world.spawnEntity(p);
        }
        zones.clear();
        tZones = -1;
    }

    /** Blizzard : chaque seconde, chaque joueur de l'arène est blessé et aveuglé ; la neige tourbillonne. */
    private void tempete() {
        if (ticksExisted >= blizzardFin || !isEntityAlive()) {
            dataManager.set(BLIZZARD, false);
            return;
        }
        Coup c = coup("blizzard");
        double r = Math.max(rayon("blizzard"), 16);
        for (EntityPlayer p : new ArrayList<>(world.playerEntities)) {
            if (!valable(p) || p.getDistanceSq(ax, p.posY, az) > r * r) continue;
            if (ticksExisted % 20 == 0) toucher(p, c, 1F);
            if (ticksExisted % 4 == 0 && world instanceof WorldServer)
                ((WorldServer) world).spawnParticle(EnumParticleTypes.SNOW_SHOVEL, p.posX, p.posY + 1, p.posZ, 6, 2, 1, 2, 0.15);
        }
        if (ticksExisted % 40 == 0) son(SoundEvents.ENTITY_HORSE_BREATHE, 2F, 0.4F);
    }

    @Override
    protected void effetsClient(String anim) {
        if (blizzard()) {
            // neige à l'horizontale autour des joueurs proches, poussée par le vent
            for (EntityPlayer p : world.playerEntities) {
                if (p.getDistanceSq(this) > 48 * 48) continue;
                for (int i = 0; i < 8; i++)
                    world.spawnParticle(EnumParticleTypes.SNOW_SHOVEL, p.posX + (rand.nextDouble() - 0.5) * 12, p.posY + rand.nextDouble() * 4,
                            p.posZ + (rand.nextDouble() - 0.5) * 12, 0.4, -0.05, 0.15);
            }
        }
        if ("roar".equals(anim) && tAnim > 6 && tAnim < 30) {
            Vec3d z = zone();
            float r = rayonZone() * Math.min(1F, tAnim / 30F);
            for (int i = 0; i < 10; i++) {
                double an = rand.nextDouble() * Math.PI * 2;
                world.spawnParticle(EnumParticleTypes.SNOW_SHOVEL, z.x + Math.cos(an) * r, z.y + 0.2, z.z + Math.sin(an) * r, 0, 0.05, 0);
            }
        }
        if ("stomp".equals(anim) && tAnim > 18 && tAnim < 26) {
            Vec3d z = zone();
            float r = rayonZone() * (tAnim - 18) / 8F;
            for (int i = 0; i < 20; i++) {
                double an = rand.nextDouble() * Math.PI * 2;
                world.spawnParticle(EnumParticleTypes.CLOUD, z.x + Math.cos(an) * r, z.y + 0.2, z.z + Math.sin(an) * r, 0, 0.02, 0);
            }
        }
        if ("couronne_de_givre".equals(anim))
            for (int i = 0; i < 3; i++)
                world.spawnParticle(EnumParticleTypes.SNOW_SHOVEL, posX + (rand.nextDouble() - 0.5) * width * 1.4, posY + rand.nextDouble() * height,
                        posZ + (rand.nextDouble() - 0.5) * width * 1.4, 0, 0.02, 0);
        if (rand.nextInt(5) == 0)
            world.spawnParticle(EnumParticleTypes.SNOW_SHOVEL, posX + (rand.nextDouble() - 0.5) * width, posY + rand.nextDouble() * height,
                    posZ + (rand.nextDouble() - 0.5) * width, 0, -0.02, 0);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound c) {
        super.writeEntityToNBT(c);
        if (blizzard()) c.setInteger("Blizzard", Math.max(0, blizzardFin - ticksExisted));
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound c) {
        super.readEntityFromNBT(c);
        if (c.getInteger("Blizzard") > 0) {
            blizzardFin = ticksExisted + c.getInteger("Blizzard");
            dataManager.set(BLIZZARD, true);
        }
    }

    @Override
    public String resume() {
        return super.resume() + (blizzard() ? ", blizzard " + Math.max(0, (blizzardFin - ticksExisted) / 20) + " s" : "");
    }
}
