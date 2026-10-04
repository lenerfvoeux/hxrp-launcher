package fr.lenerfvoeux.hxrp.metiers.data;

import fr.lenerfvoeux.hxrp.metiers.item.IFoodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Péremption en temps réel.
 * - Hors frigo : l'item porte sa date d'expiration absolue (hxrp_exp, arrondie à l'heure pour l'empilement).
 * - Au frigo : l'item porte le temps restant à l'entrée (hxrp_frem) et l'heure d'entrée (hxrp_fin) ;
 *   il pourrit 3 fois moins vite. À la sortie on recalcule une date absolue.
 * Tout repose sur des horodatages : la nourriture pourrit même joueur déconnecté.
 */
public final class Fraicheur {
    public static final String EXP = "hxrp_exp", TOT = "hxrp_tot", FREM = "hxrp_frem", FIN = "hxrp_fin";
    public static final long HOUR = 3_600_000L;
    public static final int FRIDGE_FACTOR = 3;

    private Fraicheur() {}

    public static FoodEntry entry(ItemStack s) {
        return !s.isEmpty() && s.getItem() instanceof IFoodItem ? ((IFoodItem) s.getItem()).entry() : null;
    }

    public static boolean perishable(ItemStack s) {
        FoodEntry e = entry(s);
        return e != null && e.perishable() && !e.isWaterBottle();
    }

    /**
     * Palier d'empilement : la date de péremption est arrondie VERS LE BAS sur une grille
     * (5 min par défaut). Deux aliments récoltés dans la même tranche tombent donc sur la même
     * date et s'empilent. On n'arrondit jamais vers le haut : un aliment ne gagne jamais de temps.
     */
    public static long bucket(long t, long now) {
        long b = Math.max(0, fr.lenerfvoeux.hxrp.metiers.ModConfig.palierEmpilementMinutes) * 60_000L;
        if (b <= 0) return t;
        long r = (long) (Math.floor(t / (double) b) * b);
        return r <= now ? t : r; // jamais en dessous de l'instant présent (durées très courtes)
    }

    public static boolean stamped(ItemStack s) {
        NBTTagCompound t = s.getTagCompound();
        return t != null && (t.hasKey(EXP) || t.hasKey(FREM));
    }

    /** Durée de vie totale (h), pour le pourcentage de fraîcheur. */
    public static int totalHours(ItemStack s) {
        NBTTagCompound t = s.getTagCompound();
        if (t != null && t.hasKey(TOT)) return t.getInteger(TOT);
        FoodEntry e = entry(s);
        if (e == null) return 0;
        return e.isDish() && e.life <= 0 ? fr.lenerfvoeux.hxrp.metiers.ModConfig.peremptionPlatCommandeHeures : e.life;
    }

    /** Pose une date de péremption sur un item qui n'en a pas encore. */
    public static void stamp(ItemStack s, long now) {
        if (!perishable(s) || stamped(s)) return;
        int life = totalHours(s);
        if (life <= 0) return;
        setExpiration(s, bucket(now + life * HOUR, now), life);
    }

    /** Pose une date exacte (utilisée par la commande de test et par l'alignement des stacks). */
    public static void setExpiration(ItemStack s, long exp, int totalHours) {
        NBTTagCompound t = tag(s);
        t.removeTag(FREM);
        t.removeTag(FIN);
        t.setLong(EXP, exp);
        t.setInteger(TOT, totalHours);
    }

    public static boolean inFridgeMode(ItemStack s) {
        return s.hasTagCompound() && s.getTagCompound().hasKey(FREM);
    }

    /** Temps restant (ms) au rythme actuel de l'item (3x plus long au frigo). -1 si non daté. */
    public static long remaining(ItemStack s, long now) {
        NBTTagCompound t = s.getTagCompound();
        if (t == null) return -1;
        if (t.hasKey(FREM)) return t.getLong(FIN) + t.getLong(FREM) * FRIDGE_FACTOR - now;
        if (t.hasKey(EXP)) return t.getLong(EXP) - now;
        return -1;
    }

    /** Fraîcheur de 0 à 1 (1 = tout frais). */
    public static double fraction(ItemStack s, long now) {
        int tot = totalHours(s);
        if (tot <= 0 || !stamped(s)) return 1;
        NBTTagCompound t = s.getTagCompound();
        long rem = t.hasKey(FREM) ? t.getLong(FREM) - (now - t.getLong(FIN)) / FRIDGE_FACTOR : t.getLong(EXP) - now;
        return Math.max(0, Math.min(1, rem / (double) (tot * HOUR)));
    }

    public static boolean rotten(ItemStack s, long now) {
        return perishable(s) && stamped(s) && remaining(s, now) <= 0;
    }

    /** Pénalité de note (phase 2) : 0 pt jusqu'à 75 %, −5 à 50 %, −10 à 25 %, −20 à 1 %. */
    public static double penalty(double f) {
        if (f >= 0.75) return 0;
        if (f >= 0.50) return lerp(0, 5, (0.75 - f) / 0.25);
        if (f >= 0.25) return lerp(5, 10, (0.50 - f) / 0.25);
        return lerp(10, 20, Math.min(1, (0.25 - f) / 0.24));
    }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    public static ItemStack toFridge(ItemStack s, long now) {
        if (!perishable(s) || inFridgeMode(s)) return s;
        stamp(s, now);
        NBTTagCompound t = s.getTagCompound();
        if (t == null || !t.hasKey(EXP)) return s;
        long rem = t.getLong(EXP) - now;
        if (rem <= 0) return s;
        t.removeTag(EXP);
        t.setLong(FREM, rem);
        t.setLong(FIN, now);
        return s;
    }

    public static ItemStack fromFridge(ItemStack s, long now) {
        if (!inFridgeMode(s)) return s;
        NBTTagCompound t = s.getTagCompound();
        long rem = t.getLong(FREM) - (now - t.getLong(FIN)) / FRIDGE_FACTOR;
        t.removeTag(FREM);
        t.removeTag(FIN);
        t.setLong(EXP, bucket(now + rem, now));
        return s;
    }

    private static NBTTagCompound tag(ItemStack s) {
        if (!s.hasTagCompound()) s.setTagCompound(new NBTTagCompound());
        return s.getTagCompound();
    }

    /** "2 j 4 h", "5 h 12 min", "12 min". */
    public static String duree(long ms) {
        if (ms <= 0) return "0 min";
        long min = ms / 60000, h = min / 60, d = h / 24;
        if (d > 0) return d + " j " + (h % 24) + " h";
        if (h > 0) return h + " h " + (min % 60) + " min";
        return Math.max(1, min) + " min";
    }
}
