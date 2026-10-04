package fr.lenerfvoeux.hxrp.metiers.virus.officine;

import fr.lenerfvoeux.hxrp.metiers.virus.VirusRegistre;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Defs;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.DonneesVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Machine;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagList;

/** L'état d'une préparation en cours à l'officine : recette, étape, notes des étapes, fraîcheur, rang du Virus. */
public final class EnCoursVirus {
    public static final String PREP = "v_prep", ETAPE = "v_etape", NOTES = "v_notes", FRAIS = "v_frais", RANG = "v_rang";

    private EnCoursVirus() {}

    public static ItemStack creer(Defs.Preparation p, double fraicheur, int rang) {
        ItemStack s = new ItemStack(VirusRegistre.EN_COURS);
        NBTTagCompound t = new NBTTagCompound();
        t.setString(PREP, p.id);
        t.setInteger(ETAPE, 0);
        t.setTag(NOTES, new NBTTagList());
        t.setDouble(FRAIS, fraicheur);
        t.setInteger(RANG, rang);
        s.setTagCompound(t);
        return s;
    }

    public static boolean est(ItemStack s) {
        return !s.isEmpty() && s.getItem() == VirusRegistre.EN_COURS && s.hasTagCompound();
    }

    public static Defs.Preparation preparation(ItemStack s) {
        NBTTagCompound t = s.getTagCompound();
        return t == null ? null : DonneesVirus.preparation(t.getString(PREP));
    }

    public static int etape(ItemStack s) {
        NBTTagCompound t = s.getTagCompound();
        return t == null ? 0 : t.getInteger(ETAPE);
    }

    public static Defs.Etape etapeEnCours(ItemStack s) {
        Defs.Preparation p = preparation(s);
        int i = etape(s);
        return p == null || i >= p.etapes.size() ? null : p.etapes.get(i);
    }

    public static Machine machine(ItemStack s) {
        Defs.Etape e = etapeEnCours(s);
        return e == null ? null : Machine.de(e.machine);
    }

    public static int rang(ItemStack s) {
        NBTTagCompound t = s.getTagCompound();
        return t == null ? 0 : Math.max(0, Math.min(3, t.getInteger(RANG)));
    }

    public static double fraicheur(ItemStack s) {
        NBTTagCompound t = s.getTagCompound();
        return t == null ? 1 : t.getDouble(FRAIS);
    }

    public static double[] notes(ItemStack s) {
        NBTTagCompound t = s.getTagCompound();
        if (t == null) return new double[0];
        NBTTagList l = t.getTagList(NOTES, 6);
        double[] n = new double[l.tagCount()];
        for (int i = 0; i < n.length; i++) n[i] = l.getDoubleAt(i);
        return n;
    }

    /** Enregistre la note d'une étape et passe à la suivante. */
    public static void noter(ItemStack s, double note) {
        NBTTagCompound t = s.getTagCompound();
        if (t == null) return;
        NBTTagList l = t.getTagList(NOTES, 6);
        l.appendTag(new NBTTagDouble(Math.max(0, Math.min(100, note))));
        t.setTag(NOTES, l);
        t.setInteger(ETAPE, t.getInteger(ETAPE) + 1);
    }

    /** Retire la note de la dernière étape jouée et revient à cette étape (jarre cassée avant la fin). */
    public static void annulerDerniereNote(ItemStack s) {
        NBTTagCompound t = s.getTagCompound();
        if (t == null) return;
        NBTTagList l = t.getTagList(NOTES, 6);
        if (l.tagCount() == 0) return;
        l.removeTag(l.tagCount() - 1);
        t.setTag(NOTES, l);
        t.setInteger(ETAPE, Math.max(0, t.getInteger(ETAPE) - 1));
    }

    public static boolean fini(ItemStack s) {
        Defs.Preparation p = preparation(s);
        return p != null && etape(s) >= p.etapes.size();
    }
}
