package fr.lenerfvoeux.hxrp.metiers.virus.sante;

import fr.lenerfvoeux.hxrp.metiers.virus.VirusRegistre;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Defs;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.items.ItemHandlerHelper;

/** L'ordonnance : papier remis au patient après la première prise, qui récapitule le traitement. */
public final class Ordonnances {
    public static final String MALADIE = "maladie", NOM = "nom", PATIENT = "patient", PATIENT_ID = "patientId", VIRUS = "virus", DATE = "date",
            PRISES = "prises", CONSIGNE = "consigne";

    private Ordonnances() {}

    public static ItemStack creer(EntityPlayerMP patient, Defs.Maladie m, String nomVirus) {
        ItemStack s = new ItemStack(VirusRegistre.ORDONNANCE);
        NBTTagCompound t = new NBTTagCompound();
        t.setString(MALADIE, m.id);
        t.setString(NOM, m.nom);
        t.setString(PATIENT, Identite.nom(patient));
        t.setString(PATIENT_ID, patient.getUniqueID().toString());
        t.setString(VIRUS, nomVirus);
        t.setLong(DATE, System.currentTimeMillis());
        NBTTagList l = new NBTTagList();
        for (int i = 0; i < m.prises.size(); i++) {
            Defs.Prise p = m.prises.get(i);
            String quand = i == 0 ? "maintenant" : p.moment != null && !p.moment.isEmpty()
                    ? ("reveil".equals(p.moment) ? "au réveil" : "nuit".equals(p.moment) ? "la nuit" : "le soir")
                    : "+" + (p.delai == Math.floor(p.delai) ? String.valueOf((int) p.delai) : String.valueOf(p.delai)) + " h";
            if (i == 0 && p.moment != null && !p.moment.isEmpty()) quand = "nuit".equals(p.moment) ? "la nuit" : "le soir";
            l.appendTag(new NBTTagString("Prise " + (i + 1) + " (" + quand + ") : " + Traitements.libellePrise(p) + (p.dormirApres ? ", puis dormir" : "")));
        }
        t.setTag(PRISES, l);
        t.setString(CONSIGNE, m.consigneTexte == null || "aucune".equals(m.consigneTexte) ? "" : m.consigneTexte);
        s.setTagCompound(t);
        return s;
    }

    /** Donne l'ordonnance au patient (une seule par traitement). */
    public static void remettre(EntityPlayerMP virus, EntityPlayerMP patient, Defs.Maladie m, String nomVirus) {
        ItemHandlerHelper.giveItemToPlayer(patient, creer(patient, m, nomVirus));
        patient.sendMessage(new TextComponentString(TextFormatting.GREEN + nomVirus + " vous remet une ordonnance."
                + TextFormatting.GRAY + " Gardez-la sur vous : elle vous rappelle l'heure de la prochaine prise."));
    }
}
