package fr.lenerfvoeux.hxrp.metiers.item;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;

/** Qualité d'un plat (0-100 %) et rang du cuisinier (0-3). */
public enum Qualite {
    ETRANGE("Plat étrange", TextFormatting.DARK_PURPLE, 0.3, 0.3),
    RATE("Plat raté", TextFormatting.GOLD, 0.5, 0.5),
    BON("Bon plat", TextFormatting.WHITE, 1.0, 1.0),
    TRES_BON("Très bon plat", TextFormatting.GREEN, 1.0, 1.0),
    EXCEPTION("Exception", TextFormatting.YELLOW, 2.0, 1.0);

    public static final String Q = "hxrp_q", R = "hxrp_r";
    public final String label;
    public final TextFormatting color;
    public final double faimMult, soifMult;

    Qualite(String label, TextFormatting color, double faimMult, double soifMult) {
        this.label = label; this.color = color; this.faimMult = faimMult; this.soifMult = soifMult;
    }

    public static Qualite of(int q) {
        if (q < 60) return ETRANGE;
        if (q < 80) return RATE;
        if (q < 90) return BON;
        if (q < 98) return TRES_BON;
        return EXCEPTION;
    }

    public static int quality(ItemStack s) {
        NBTTagCompound t = s.getTagCompound();
        return t != null && t.hasKey(Q) ? t.getInteger(Q) : 85;
    }

    public static int cookRank(ItemStack s) {
        NBTTagCompound t = s.getTagCompound();
        return t != null ? Math.max(0, Math.min(3, t.getInteger(R))) : 0;
    }

    public static void set(ItemStack s, int quality, int rank) {
        if (!s.hasTagCompound()) s.setTagCompound(new NBTTagCompound());
        s.getTagCompound().setInteger(Q, Math.max(0, Math.min(100, quality)));
        s.getTagCompound().setInteger(R, Math.max(0, Math.min(3, rank)));
    }

    /** Bonus de faim selon les étoiles du cuisinier : +0 / +10 / +25 / +40 %. */
    public static double rankBonus(int rank) {
        return new double[]{1.0, 1.10, 1.25, 1.40}[Math.max(0, Math.min(3, rank))];
    }
}
