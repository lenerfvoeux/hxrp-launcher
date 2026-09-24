package fr.lenerfvoeux.hxrp.metiers.cuisine;

import fr.lenerfvoeux.hxrp.metiers.data.FoodDatabase;
import fr.lenerfvoeux.hxrp.metiers.data.FoodEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Lecture/écriture de l'état d'une préparation en cours (recette, étape, notes, fraîcheur). */
public final class EnCours {
    public static final String RECETTE = "hxrp_rec", ETAPE = "hxrp_etape", NOTES = "hxrp_notes",
            FRAICHEUR = "hxrp_frais", RANG = "hxrp_rang", LIFE = "hxrp_life";

    private EnCours() {}

    public static ItemStack create(FoodEntry recette, double fraicheur, int rangCuisinier, int lifeHours) {
        ItemStack s = new ItemStack(fr.lenerfvoeux.hxrp.metiers.ModRegistry.EN_COURS);
        NBTTagCompound t = new NBTTagCompound();
        t.setString(RECETTE, recette.id);
        t.setInteger(ETAPE, 0);
        t.setTag(NOTES, new net.minecraft.nbt.NBTTagList());
        t.setDouble(FRAICHEUR, fraicheur);
        t.setInteger(RANG, rangCuisinier);
        t.setInteger(LIFE, lifeHours);
        s.setTagCompound(t);
        return s;
    }

    public static FoodEntry recette(ItemStack s) {
        NBTTagCompound t = s.getTagCompound();
        return t == null ? null : FoodDatabase.get(t.getString(RECETTE));
    }

    public static int etape(ItemStack s) {
        NBTTagCompound t = s.getTagCompound();
        return t == null ? 0 : t.getInteger(ETAPE);
    }

    public static String geste(ItemStack s) {
        FoodEntry r = recette(s);
        int i = etape(s);
        return r == null || i >= r.steps.size() ? null : r.steps.get(i);
    }

    public static Station station(ItemStack s) {
        String g = geste(s);
        return g == null ? null : Station.forGeste(g);
    }

    /** Enregistre la note d'une étape (0-100) et passe à la suivante. */
    public static void addNote(ItemStack s, double note) {
        NBTTagCompound t = s.getTagCompound();
        if (t == null) return;
        net.minecraft.nbt.NBTTagList l = t.getTagList(NOTES, 6);
        l.appendTag(new net.minecraft.nbt.NBTTagDouble(Math.max(0, Math.min(100, note))));
        t.setTag(NOTES, l);
        t.setInteger(ETAPE, t.getInteger(ETAPE) + 1);
    }

    public static double moyenne(ItemStack s) {
        NBTTagCompound t = s.getTagCompound();
        if (t == null) return 0;
        net.minecraft.nbt.NBTTagList l = t.getTagList(NOTES, 6);
        if (l.tagCount() == 0) return 0;
        double sum = 0;
        for (int i = 0; i < l.tagCount(); i++) sum += l.getDoubleAt(i);
        return sum / l.tagCount();
    }

    /** Moyenne des étapes pondérée par leur importance (la cuisson compte plus que la découpe). */
    public static double moyennePonderee(ItemStack s, FoodEntry r) {
        NBTTagCompound t = s.getTagCompound();
        if (t == null || r == null) return 0;
        net.minecraft.nbt.NBTTagList l = t.getTagList(NOTES, 6);
        double somme = 0, poids = 0;
        for (int i = 0; i < l.tagCount(); i++) {
            double w = i < r.steps.size() ? fr.lenerfvoeux.hxrp.metiers.minijeu.Jeux.poids(r.steps.get(i)) : 1;
            somme += l.getDoubleAt(i) * w;
            poids += w;
        }
        return poids <= 0 ? 0 : somme / poids;
    }

    public static double fraicheur(ItemStack s) {
        NBTTagCompound t = s.getTagCompound();
        return t == null ? 1 : t.getDouble(FRAICHEUR);
    }

    public static int rang(ItemStack s) {
        NBTTagCompound t = s.getTagCompound();
        return t == null ? 0 : t.getInteger(RANG);
    }

    public static int life(ItemStack s) {
        NBTTagCompound t = s.getTagCompound();
        return t == null ? 24 : Math.max(1, t.getInteger(LIFE));
    }

    public static boolean fini(ItemStack s) {
        FoodEntry r = recette(s);
        return r != null && etape(s) >= r.steps.size();
    }
}
