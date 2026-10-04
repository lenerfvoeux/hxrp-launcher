package fr.lenerfvoeux.hxrp.metiers.event;

import fr.lenerfvoeux.hxrp.metiers.ModConfig;
import fr.lenerfvoeux.hxrp.metiers.capability.Nutrition;
import fr.lenerfvoeux.hxrp.metiers.capability.NutritionData;
import fr.lenerfvoeux.hxrp.metiers.data.FoodEntry;
import fr.lenerfvoeux.hxrp.metiers.data.Fraicheur;
import fr.lenerfvoeux.hxrp.metiers.item.Qualite;
import fr.lenerfvoeux.hxrp.metiers.network.Network;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Tout ce qui se passe quand on mange ou boit. */
public final class FoodEffects {
    private static final Map<String, Potion> BUFFS = new HashMap<>();
    static {
        BUFFS.put("Vitesse", MobEffects.SPEED);
        BUFFS.put("Célérité", MobEffects.HASTE);
        BUFFS.put("Force", MobEffects.STRENGTH);
        BUFFS.put("Saut", MobEffects.JUMP_BOOST);
        BUFFS.put("Régénération", MobEffects.REGENERATION);
        BUFFS.put("Résistance", MobEffects.RESISTANCE);
        BUFFS.put("Résistance au feu", MobEffects.FIRE_RESISTANCE);
        BUFFS.put("Respiration aquatique", MobEffects.WATER_BREATHING);
        BUFFS.put("Vision nocturne", MobEffects.NIGHT_VISION);
        BUFFS.put("Absorption", MobEffects.ABSORPTION);
        BUFFS.put("Chance", MobEffects.LUCK);
        BUFFS.put("Bonus de vie", MobEffects.HEALTH_BOOST);
    }

    private static final String[] PHRASES_MANGER = {
            "Tu en as mangé il y a peu… ton estomac réclame autre chose.",
            "Encore ça ? Tu as envie de changer.",
            "Pas encore… tu as envie d'autre chose.",
            "Tu as déjà mangé ce plat, tu as envie d'autre chose."};
    private static final String[] PHRASES_BOIRE = {
            "Tu en as bu il y a peu, tu as envie d'autre chose.",
            "Encore ça ? Tu as envie de changer de boisson."};

    private FoodEffects() {}

    /** Clé de cooldown : nom de registre (+ meta pour les items à variantes). */
    public static String key(ItemStack s) {
        String k = String.valueOf(s.getItem().getRegistryName());
        return s.getHasSubtypes() ? k + "@" + s.getMetadata() : k;
    }

    public static void message(EntityPlayer p, String msg) {
        p.sendMessage(new TextComponentString(msg));
    }

    public static void cooldownMessage(EntityPlayer p, boolean drink, long left) {
        String[] set = drink ? PHRASES_BOIRE : PHRASES_MANGER;
        message(p, TextFormatting.GOLD + set[p.getRNG().nextInt(set.length)] + TextFormatting.GRAY + " (encore " + Fraicheur.duree(left) + ")");
    }

    public static int buffMinutes(int recipeRank, Qualite q) {
        int m = new int[]{2, 3, 5, 8}[Math.max(0, Math.min(3, recipeRank))];
        return q == Qualite.EXCEPTION ? m * 2 : m;
    }

    public static PotionEffect parseBuff(String buff, int ticks) {
        if (buff == null || buff.isEmpty()) return null;
        String name = buff.trim();
        int amp = 0;
        if (name.endsWith(" II")) { amp = 1; name = name.substring(0, name.length() - 3); }
        else if (name.endsWith(" I")) name = name.substring(0, name.length() - 2);
        Potion p = BUFFS.get(name);
        return p == null ? null : new PotionEffect(p, ticks, amp);
    }

    public static void eatPlat(EntityPlayerMP p, ItemStack s, FoodEntry e) {
        NutritionData d = Nutrition.get(p);
        if (d == null) return;
        long now = System.currentTimeMillis();
        Qualite q = Qualite.of(Qualite.quality(s));
        int rank = Qualite.cookRank(s);
        d.addFaim(e.faim * q.faimMult * Qualite.rankBonus(rank));
        d.addSoif(e.soif * q.soifMult);
        Random r = p.getRNG();
        switch (q) {
            case ETRANGE:
                negatif(p, r);
                negatif(p, r);
                message(p, TextFormatting.DARK_PURPLE + "Beurk… ce plat avait un drôle de goût.");
                break;
            case RATE:
                negatif(p, r);
                break;
            case TRES_BON:
            case EXCEPTION:
                PotionEffect b = parseBuff(e.buff, buffMinutes(e.rank, q) * 60 * 20);
                if (b != null) p.addPotionEffect(b);
                if (q == Qualite.EXCEPTION) message(p, TextFormatting.YELLOW + "Un plat d'exception ! Tu te sens rassasié comme jamais.");
                break;
            default:
                break;
        }
        int h = e.isDrink() ? ModConfig.cooldownBoissonsHeures : ModConfig.cooldownPlatsHeures;
        d.setCooldown(key(s), now + h * Fraicheur.HOUR);
        Network.sync(p);
    }

    private static void negatif(EntityPlayer p, Random r) {
        switch (r.nextInt(4)) {
            case 0: p.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 20 * 15, 0)); break;
            case 1: p.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 20 * 30, 0)); break;
            case 2: p.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 20 * 30, 0)); break;
            default: p.addPotionEffect(new PotionEffect(MobEffects.POISON, 20 * 5, 0)); break;
        }
    }
}
