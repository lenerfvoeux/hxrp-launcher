package fr.lenerfvoeux.hxrp.metiers.virus.sante;

import fr.lenerfvoeux.hxrp.metiers.virus.VirusConfig;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Defs;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.DonneesVirus;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.monster.EntityPolarBear;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.Iterator;
import java.util.Locale;

/**
 * Les 7 blessures : reconnaître le type de dégât, monter les stades (premier dégât : stade 1, chaque nouveau dégât du
 * même type dans les 10 minutes : +1, très gros coup ou très grosse chute : +2), aggraver le stade 4 toutes les
 * 30 minutes, la gelure par exposition au froid, et les soins (repos de 15 minutes aux stades 3 et 4).
 */
public final class Blessures {
    public static final String CONTUSIONS = "contusions", COMMOTION = "commotion", JAMBE = "jambe", PLAIE = "plaie",
            BRULURE = "brulure", MORSURE = "morsure", GELURE = "gelure";
    private static final String[] VENIMEUX = {"spider", "scorpion", "snake", "serpent", "vipere", "viper", "araignee", "venom", "wasp", "guepe", "centipede", "mille_pattes"};
    private static final String[] LAMES = {"sword", "katana", "knife", "couteau", "dague", "dagger", "blade", "lame", "axe", "hache", "sabre", "epee", "scythe", "faux", "lance", "spear"};
    private static final String[] GRIFFES = {"wolf", "loup", "bear", "ours", "tiger", "tigre", "lion", "claw", "griffe", "panther", "panthere", "fox", "renard"};
    private static final String[] BALLES = {"bullet", "balle", "gun", "shot", "arrow", "fleche"};

    private Blessures() {}

    // ================================================================== dégâts reçus
    /** Classe un dégât reçu par le joueur et monte les blessures correspondantes. */
    public static void degat(EntityPlayerMP p, SanteData d, DamageSource s, float a) {
        if (a <= 0 || p.capabilities.isCreativeMode || p.isSpectator()) return;
        long now = System.currentTimeMillis();
        Entity src = s.getTrueSource();
        String type = s.getDamageType() == null ? "" : s.getDamageType().toLowerCase(Locale.ROOT);
        boolean gros = a >= 12;
        if (s == DamageSource.FALL) {
            blesser(p, d, JAMBE, a >= 10 ? 2 : 1, false);
            return;
        }
        if (s.isFireDamage()) {
            blesser(p, d, BRULURE, 1, false);
            return;
        }
        if (s == DamageSource.CACTUS || "thorns".equals(type)) {
            blesser(p, d, PLAIE, 1, false);
            return;
        }
        if (src instanceof EntityLivingBase) d.dernierCombat = now;
        boolean projectile = s.isProjectile() || contient(type, BALLES);
        if (projectile) {
            blesser(p, d, PLAIE, gros ? 2 : 1, false);
        } else if (src instanceof EntityLivingBase && !s.isExplosion() && !s.isMagicDamage() && s.getImmediateSource() == src) {
            EntityLivingBase l = (EntityLivingBase) src;
            if (venimeux(l)) blesser(p, d, MORSURE, gros ? 2 : 1, false);
            else if (tranchant(l)) blesser(p, d, PLAIE, gros ? 2 : 1, false);
            else coup(p, d, now);
        }
        if (a >= 6 && (src != null || s.isExplosion())) blesser(p, d, COMMOTION, gros ? 2 : 1, false);
    }

    /** Contusions : 4 coups reçus en 30 secondes. */
    private static void coup(EntityPlayerMP p, SanteData d, long now) {
        d.coupsRecents.add(now);
        for (Iterator<Long> it = d.coupsRecents.iterator(); it.hasNext(); ) if (now - it.next() > 30_000) it.remove();
        if (d.coupsRecents.size() >= 4) {
            d.coupsRecents.clear();
            blesser(p, d, CONTUSIONS, 1, true);
        }
    }

    private static boolean venimeux(EntityLivingBase l) {
        return l instanceof EntitySpider || contient(nomEntite(l), VENIMEUX);
    }

    private static boolean tranchant(EntityLivingBase l) {
        if (l instanceof EntityWolf || l instanceof EntityOcelot || l instanceof EntityPolarBear || contient(nomEntite(l), GRIFFES)) return true;
        ItemStack arme = l.getHeldItemMainhand();
        if (arme.isEmpty()) return false;
        if (arme.getItem() instanceof ItemSword || arme.getItem() instanceof ItemAxe) return true;
        ResourceLocation r = arme.getItem().getRegistryName();
        return r != null && contient(r.getPath().toLowerCase(Locale.ROOT), LAMES);
    }

    private static String nomEntite(Entity e) {
        ResourceLocation r = EntityList.getKey(e);
        return r == null ? "" : r.getPath().toLowerCase(Locale.ROOT);
    }

    private static boolean contient(String s, String[] mots) {
        for (String m : mots) if (s.contains(m)) return true;
        return false;
    }

    // ================================================================== stades
    /**
     * Monte une blessure. n : stades gagnés. sansAntiRepetition : la source n'est pas continue (une rafale de coups
     * a déjà été comptée, une exposition au froid…).
     */
    public static void blesser(EntityPlayerMP p, SanteData d, String id, int n, boolean sansAntiRepetition) {
        Defs.Blessure def = DonneesVirus.blessure(id);
        if (def == null || n <= 0) return;
        long now = System.currentTimeMillis();
        SanteData.Blessure b = d.blessure(id);
        int avant = b.stade;
        if (b.stade == 0) {
            b.stade = Math.min(4, n);
        } else {
            if (b.stade >= 4) return;
            if (now - b.dernierStade > VirusConfig.fenetreBlessureMinutes * Maladies.MINUTE) return;
            if (!sansAntiRepetition && now - b.dernierStade < VirusConfig.antiRepetitionSecondes * 1000L) return;
            b.stade = Math.min(4, b.stade + n);
        }
        if (b.stade == avant) return;
        b.dernierStade = now;
        b.reposRequis = 0;
        b.reposFait = 0;
        if (b.stade == 4) {
            b.aggravation = 0;
            b.derniereAggravation = now;
        }
        d.dirty = true;
        p.sendMessage(new TextComponentString(TextFormatting.RED + "" + TextFormatting.ITALIC + def.stades.get(b.stade - 1).message
                + TextFormatting.DARK_RED + " (" + def.nom + ", stade " + b.stade + ")"));
    }

    /** Fixe directement une blessure (commande admin). */
    public static void fixer(SanteData d, String id, int stade) {
        SanteData.Blessure b = d.blessure(id);
        b.stade = Math.max(0, Math.min(4, stade));
        b.dernierStade = System.currentTimeMillis();
        b.reposRequis = 0;
        b.reposFait = 0;
        b.aggravation = 0;
        b.derniereAggravation = b.dernierStade;
        if (b.stade == 0) d.blessures.remove(id);
        d.dirty = true;
    }

    // ================================================================== chaque seconde
    public static void seconde(EntityPlayerMP p, SanteData d, long now, boolean auRepos) {
        for (java.util.Map.Entry<String, SanteData.Blessure> e : d.blessures.entrySet()) {
            SanteData.Blessure b = e.getValue();
            Defs.Blessure def = DonneesVirus.blessure(e.getKey());
            if (b.stade <= 0 || def == null) continue;
            if (b.reposRequis > 0) {
                if (!auRepos) {
                    if (b.reposFait > 10_000)
                        p.sendStatusMessage(new TextComponentString(TextFormatting.GOLD + "Le soin de votre " + def.nom.toLowerCase(Locale.ROOT)
                                + " demande du repos : ni course ni combat."), true);
                    b.reposFait = 0;
                } else {
                    b.reposFait += 1000;
                    if (b.reposFait >= b.reposRequis) {
                        b.stade = 0;
                        b.reposRequis = 0;
                        d.dirty = true;
                        p.sendMessage(new TextComponentString(TextFormatting.GREEN + "Le soin a fait son œuvre : votre " + def.nom.toLowerCase(Locale.ROOT) + " est guérie."));
                    }
                }
                continue;
            }
            if (b.stade == 4 && b.aggravation < 3 && now - b.derniereAggravation >= VirusConfig.aggravationMinutes * Maladies.MINUTE) {
                b.aggravation++;
                b.derniereAggravation = now;
                d.dirty = true;
                p.sendMessage(new TextComponentString(TextFormatting.DARK_RED + "" + TextFormatting.ITALIC + "Votre " + def.nom.toLowerCase(Locale.ROOT)
                        + " s'aggrave faute de soins."));
            }
        }
        d.blessures.entrySet().removeIf(e -> e.getValue().stade <= 0);
        gelure(p, d);
    }

    /** Froid prolongé : biome glacé, dehors, la nuit ou par mauvais temps ; ou eau glacée. Près d'un feu, ça passe. */
    private static void gelure(EntityPlayerMP p, SanteData d) {
        if (p.capabilities.isCreativeMode || p.isSpectator()) return;
        BlockPos pos = p.getPosition();
        float t = p.world.getBiome(pos).getTemperature(pos);
        boolean dehors = p.world.canSeeSky(pos);
        long h = p.world.getWorldTime() % 24000;
        boolean nuit = h >= 13000 && h < 23000;
        boolean expose = (t < 0.15f && dehors && (nuit || p.world.isRaining())) || (p.isInWater() && t < 0.2f);
        if (expose && presDUneSourceDeChaleur(p)) expose = false;
        if (expose) {
            int pieces = 0;
            for (ItemStack a : p.inventory.armorInventory) if (!a.isEmpty()) pieces++;
            d.froid += pieces >= 4 ? 0.5 : 1;
            if (d.froid >= VirusConfig.froidSecondes) {
                d.froid = 0;
                blesser(p, d, GELURE, 1, true);
            }
        } else if (d.froid > 0) {
            d.froid = Math.max(0, d.froid - 2);
        }
    }

    /** Feu, lave, four allumé, magma ou machine chauffée à moins de 3 blocs. */
    public static boolean presDUneSourceDeChaleur(EntityPlayerMP p) {
        BlockPos c = p.getPosition();
        for (BlockPos q : BlockPos.getAllInBoxMutable(c.add(-3, -2, -3), c.add(3, 2, 3))) {
            IBlockState s = p.world.getBlockState(q);
            Block b = s.getBlock();
            if (b == Blocks.FIRE || b == Blocks.LAVA || b == Blocks.FLOWING_LAVA || b == Blocks.LIT_FURNACE || b == Blocks.MAGMA) return true;
            if (b.getRegistryName() != null && "hxrpmetiers".equals(b.getRegistryName().getNamespace()) && s.getLightValue(p.world, q) >= 4
                    && !(b instanceof net.minecraft.block.BlockBush)) return true;
        }
        return false;
    }

    // ================================================================== soins
    /**
     * Un soin vient d'être posé par un Virus. Stades 1-2 : guéri sur-le-champ. Stades 3-4 : il faut du repos
     * (ni sprint ni combat) pour qu'il agisse ; un soin réussi à 95 % raccourcit ce repos.
     */
    public static void soigner(EntityPlayerMP p, SanteData d, String id, int qualite, boolean bonus) {
        Defs.Blessure def = DonneesVirus.blessure(id);
        SanteData.Blessure b = d.blessures.get(id);
        if (def == null || b == null || b.stade <= 0) return;
        if (b.stade <= 2) {
            d.blessures.remove(id);
            p.sendMessage(new TextComponentString(TextFormatting.GREEN + "Votre " + def.nom.toLowerCase(Locale.ROOT) + " est soignée."));
        } else {
            long repos = VirusConfig.reposSoinMinutes * Maladies.MINUTE;
            if (bonus) repos = repos / 2;
            b.reposRequis = Math.max(1000, repos);
            b.reposFait = 0;
            b.qualiteSoin = qualite;
            p.sendMessage(new TextComponentString(TextFormatting.GREEN + "Le soin est posé. " + TextFormatting.GOLD + "Reposez-vous "
                    + Math.max(1, repos / Maladies.MINUTE) + " minutes (ni course ni combat) pour qu'il agisse."));
        }
        d.dirty = true;
    }
}
