package fr.lenerfvoeux.hxrp.metiers.virus.item;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Note d'une préparation du Virus (0-100 %) et rang du Virus qui l'a faite.
 * Sous 80 % elle est ratée (inutilisable) ; à 95 % et plus elle donne son effet bonus.
 */
public final class Qualites {
    public static final String NOTE = "hxrp_vq", RANG = "hxrp_vr";

    private Qualites() {}

    public static boolean estPreparation(ItemStack s) {
        return !s.isEmpty() && s.getItem() instanceof ItemPreparation;
    }

    public static boolean notee(ItemStack s) {
        NBTTagCompound t = s.getTagCompound();
        return t != null && t.hasKey(NOTE);
    }

    /** Note de la préparation ; une préparation jamais notée (créative) vaut 85 %. */
    public static int note(ItemStack s) {
        NBTTagCompound t = s.getTagCompound();
        return t != null && t.hasKey(NOTE) ? t.getInteger(NOTE) : 85;
    }

    public static int rang(ItemStack s) {
        NBTTagCompound t = s.getTagCompound();
        return t == null ? 0 : Math.max(0, Math.min(3, t.getInteger(RANG)));
    }

    public static void noter(ItemStack s, int note, int rang) {
        if (!s.hasTagCompound()) s.setTagCompound(new NBTTagCompound());
        s.getTagCompound().setInteger(NOTE, Math.max(0, Math.min(100, note)));
        s.getTagCompound().setInteger(RANG, Math.max(0, Math.min(3, rang)));
    }
}
