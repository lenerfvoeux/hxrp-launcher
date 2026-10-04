package fr.lenerfvoeux.hxrp.metiers.virus.officine;

import fr.lenerfvoeux.hxrp.metiers.virus.VirusRegistre;
import fr.lenerfvoeux.hxrp.metiers.virus.VirusSons;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.DonneesVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Defs;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Identite;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Sante;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.SanteData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.items.ItemHandlerHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Le diagnostic : la seringue pleine (valeurs sanguines lisibles selon le rang du Virus et la réussite de la prise),
 * l'analyse au microscope (résultat dans le chat) et le carnet de consultation du patient, qui garde tout.
 */
public final class Diagnostic {
    /** Valeurs lues par rang : 0★ groupe + globules blancs, 1★ + rouges + plaquettes, 2★ + sucre + fer, 3★ + toxines + parasites + marqueur rare. */
    public static final String[] CLES = {"GS", "GB", "GR", "PL", "SU", "FE", "TX", "PA", "MR"};
    public static final String[] NOMS = {"Groupe sanguin", "Globules blancs", "Globules rouges", "Plaquettes", "Sucre", "Fer", "Toxines", "Parasites", "Marqueur rare"};
    public static final String ILLISIBLE = "illisible";
    // étiquettes NBT communes à la seringue pleine et au carnet
    public static final String PATIENT = "v_patient", PID = "v_pid", SEXE = "v_sexe", DATE = "v_date", SANG = "v_sang",
            SYMPTOMES = "v_symptomes", ANALYSTE = "v_analyste", RANG = "v_rang";
    private static final Random RNG = new Random();

    private Diagnostic() {}

    public static String nom(String cle) {
        for (int i = 0; i < CLES.length; i++) if (CLES[i].equals(cle)) return NOMS[i];
        return cle;
    }

    /** « élevé ↑ », « très élevé ↑↑ », « bas ↓ », « présent », « normal »… */
    public static String libelle(String cle, String v) {
        if (v == null) return "?";
        if ("GS".equals(cle) || "MR".equals(cle) || ILLISIBLE.equals(v)) return v;
        switch (v) {
            case "↑": return "élevé ↑";
            case "↑↑": return "très élevé ↑↑";
            case "↓": return "bas ↓";
            case "↓↓": return "très bas ↓↓";
            case "+": return "présent +";
            case "normal": return "normal";
            default: return v;
        }
    }

    public static String date(long t) {
        return new SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE).format(new Date(t));
    }

    // ================================================================== prise de sang
    /**
     * Seringue pleine au nom du patient : les valeurs du rang du Virus (80 % et plus), la moitié au hasard (50 à 79 %).
     * Les valeurs viennent de la maladie en cours (valeur non citée = normale).
     */
    static ItemStack remplir(EntityPlayerMP virus, EntityPlayerMP patient, SanteData d, int rang, double note) {
        List<String> cles = new ArrayList<>();
        for (int i = 0; i < Math.min(8, 2 * (rang + 1)); i++) cles.add(CLES[i]);
        if (rang >= 3) cles.add("MR");
        if (note < 80) {
            List<String> l = new ArrayList<>(cles);
            Collections.shuffle(l, RNG);
            int garder = (cles.size() + 1) / 2;
            cles.retainAll(l.subList(0, garder));
        }
        Defs.Maladie m = d.malade() && d.convalescenceFin <= 0 ? DonneesVirus.maladie(d.maladie) : null;
        NBTTagCompound sang = new NBTTagCompound();
        for (String k : cles) sang.setString(k, valeur(k, d, m));
        ItemStack s = new ItemStack(VirusRegistre.SERINGUE_PLEINE);
        NBTTagCompound t = new NBTTagCompound();
        t.setString(PATIENT, Identite.nom(patient));
        t.setString(PID, patient.getUniqueID().toString());
        t.setString(SEXE, Identite.sexe(patient) == null ? "" : Identite.sexe(patient));
        t.setLong(DATE, System.currentTimeMillis());
        t.setTag(SANG, sang);
        t.setInteger(RANG, rang);
        s.setTagCompound(t);
        return s;
    }

    private static String valeur(String k, SanteData d, Defs.Maladie m) {
        if ("GS".equals(k)) return d.groupe == null || d.groupe.isEmpty() ? "?" : d.groupe;
        if ("MR".equals(k)) {
            String v = m == null ? null : m.sang.get("MR");
            return v == null || v.isEmpty() ? "aucun" : v;
        }
        String v = m == null ? null : m.sang.get(k);
        return v == null || v.isEmpty() ? "normal" : v;
    }

    // ================================================================== microscope
    /** Clic droit sur le microscope avec une seringue pleine. */
    public static void microscope(EntityPlayerMP p, BlockPos pos) {
        ItemStack held = p.getHeldItemMainhand();
        if (held.getItem() != VirusRegistre.SERINGUE_PLEINE || !held.hasTagCompound()) {
            p.sendMessage(new TextComponentString(TextFormatting.GOLD + "Microscope d'analyse" + TextFormatting.GRAY
                    + " · viens avec une seringue pleine (prise de sang) pour l'analyser."));
            return;
        }
        if (!Soins.verifierVirus(p)) return;
        SeancesVirus.ouvrir(p, SeancesVirus.MICRO, "microscope", "", held.getTagCompound().getString(PATIENT), 0, 1,
                Math.max(0, Officine.rang(p)), 0, pos, null, EnumHand.MAIN_HAND);
    }

    static void finMicroscope(EntityPlayerMP p, SeancesVirus.Seance s, double note) {
        ItemStack held = p.getHeldItem(s.main);
        if (held.getItem() != VirusRegistre.SERINGUE_PLEINE || !held.hasTagCompound()) {
            p.sendMessage(new TextComponentString(TextFormatting.RED + "Garde la seringue en main pendant l'analyse."));
            return;
        }
        NBTTagCompound t = held.getTagCompound().copy();
        held.shrink(1);
        p.inventoryContainer.detectAndSendChanges();
        p.world.playSound(null, s.pos, VirusSons.VERRE, SoundCategory.BLOCKS, 0.6f, 1.4f);
        if (note < 50) {
            p.sendMessage(new TextComponentString(TextFormatting.RED + "Analyse ratée (" + Math.round(note) + " %) : l'image est restée floue, l'échantillon est perdu."));
            return;
        }
        NBTTagCompound sang = t.getCompoundTag(SANG);
        if (note < 80 && !sang.getKeySet().isEmpty()) {
            List<String> k = new ArrayList<>(sang.getKeySet());
            sang.setString(k.get(RNG.nextInt(k.size())), ILLISIBLE);
        }
        String patient = t.getString(PATIENT);
        p.sendMessage(new TextComponentString(TextFormatting.GOLD + "✚ Analyse de sang · " + patient + TextFormatting.GRAY
                + " (prélevé le " + date(t.getLong(DATE)) + ", analyse " + Math.round(note) + " %)"));
        for (String k : CLES) {
            if (!sang.hasKey(k)) continue;
            String v = sang.getString(k);
            TextFormatting c = ILLISIBLE.equals(v) ? TextFormatting.DARK_GRAY : "normal".equals(v) || "aucun".equals(v) ? TextFormatting.GRAY : TextFormatting.YELLOW;
            p.sendMessage(new TextComponentString(TextFormatting.DARK_AQUA + "  " + nom(k) + " : " + c + libelle(k, v)));
        }
        if (sang.getKeySet().isEmpty()) p.sendMessage(new TextComponentString(TextFormatting.GRAY + "  Rien de lisible dans cet échantillon."));
        noterAuCarnet(p, t, sang);
    }

    /** Inscrit l'analyse dans le carnet du patient que porte le Virus, ou lui en ouvre un nouveau. */
    private static void noterAuCarnet(EntityPlayerMP p, NBTTagCompound seringue, NBTTagCompound sang) {
        String pid = seringue.getString(PID);
        ItemStack carnet = ItemStack.EMPTY;
        for (int i = 0; i < p.inventory.getSizeInventory(); i++) {
            ItemStack c = p.inventory.getStackInSlot(i);
            if (c.getItem() == VirusRegistre.CARNET && c.hasTagCompound() && pid.equals(c.getTagCompound().getString(PID))) {
                carnet = c;
                break;
            }
        }
        boolean nouveau = carnet.isEmpty();
        if (nouveau) {
            carnet = new ItemStack(VirusRegistre.CARNET);
            NBTTagCompound t = new NBTTagCompound();
            t.setString(PATIENT, seringue.getString(PATIENT));
            t.setString(PID, pid);
            t.setTag(SYMPTOMES, new NBTTagList());
            carnet.setTagCompound(t);
        }
        NBTTagCompound t = carnet.getTagCompound();
        t.setString(SEXE, seringue.getString(SEXE));
        t.setLong(DATE, seringue.getLong(DATE));
        t.setString(ANALYSTE, Identite.nom(p));
        // les nouvelles valeurs remplacent les anciennes ; une valeur illisible n'efface pas une valeur déjà connue
        NBTTagCompound ancien = t.getCompoundTag(SANG);
        for (String k : sang.getKeySet()) {
            String v = sang.getString(k);
            if (ILLISIBLE.equals(v) && ancien.hasKey(k)) continue;
            ancien.setString(k, v);
        }
        t.setTag(SANG, ancien);
        if (nouveau) ItemHandlerHelper.giveItemToPlayer(p, carnet);
        p.sendMessage(new TextComponentString(TextFormatting.GRAY + (nouveau ? "Nouveau carnet de consultation ouvert" : "Résultat inscrit au carnet de consultation")
                + " de " + seringue.getString(PATIENT) + "."));
    }

    /** Le Virus coche les symptômes que lui raconte son patient (message du GUI du carnet). */
    public static void cocher(EntityPlayer p, int slot, List<String> symptomes) {
        if (slot < 0 || slot >= p.inventory.getSizeInventory()) return;
        ItemStack c = p.inventory.getStackInSlot(slot);
        if (c.getItem() != VirusRegistre.CARNET || !c.hasTagCompound()) return;
        SanteData d = Sante.get(p);
        if (d == null || !d.estVirus()) return;
        NBTTagList l = new NBTTagList();
        for (String s : symptomes) if (DonneesVirus.estSymptome(s) && l.tagCount() < 40) l.appendTag(new NBTTagString(s));
        c.getTagCompound().setTag(SYMPTOMES, l);
        p.inventory.markDirty();
    }
}
