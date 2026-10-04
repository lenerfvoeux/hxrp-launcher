package fr.lenerfvoeux.hxrp.metiers.virus.sante;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Un traitement en cours chez le patient : la prochaine prise attendue, ce qui a déjà été donné pour la prise
 * en cours, l'heure et le lieu de la prise précédente, et le suivi des consignes.
 */
public class Traitement {
    public String maladie = "";
    /** Index (0…) de la prise attendue. */
    public int prise;
    /** Première administration de la prise en cours (ms, 0 : rien donné encore). */
    public long debutPrise;
    /** Préparations déjà données pour la prise en cours. */
    public final Set<String> faits = new LinkedHashSet<>();
    /** Validation de la prise précédente (ms) et début de la cure. */
    public long dernierePrise, debutCure;
    public double px, py, pz;
    public int dimension;
    /** Rang du Virus qui a donné la dernière prise (durée de la convalescence). */
    public int rangVirus;
    public String nomVirus = "";
    /** Bonus obtenus par des remèdes réussis à 95 % ou plus. */
    public boolean bonusConvalescence;
    // suivi des consignes
    /** Sommeil (ou repos au lit) depuis la dernière prise. */
    public boolean dormiDepuisPrise;
    public boolean platMange;
    /** Temps passé hors consigne (s) : au froid, au soleil, mouillé, sous l'altitude… */
    public double horsConsigne;
    /** Temps passé au soleil / près d'un feu / à l'ombre depuis la dernière prise (s). */
    public double soleil, feu, ombre;
    /** Temps passé à courir (s). */
    public double sprint;
    /** Une prise dont la consigne « dormir après » est en attente (convalescence suspendue). */
    public boolean attenteSommeil;

    public NBTTagCompound ecrire() {
        NBTTagCompound t = new NBTTagCompound();
        t.setString("maladie", maladie);
        t.setInteger("prise", prise);
        t.setLong("debutPrise", debutPrise);
        NBTTagList l = new NBTTagList();
        for (String s : faits) l.appendTag(new NBTTagString(s));
        t.setTag("faits", l);
        t.setLong("dernierePrise", dernierePrise);
        t.setLong("debutCure", debutCure);
        t.setDouble("px", px);
        t.setDouble("py", py);
        t.setDouble("pz", pz);
        t.setInteger("dim", dimension);
        t.setInteger("rangVirus", rangVirus);
        t.setString("nomVirus", nomVirus);
        t.setBoolean("bonusConv", bonusConvalescence);
        t.setBoolean("dormi", dormiDepuisPrise);
        t.setBoolean("plat", platMange);
        t.setDouble("hors", horsConsigne);
        t.setDouble("soleil", soleil);
        t.setDouble("feu", feu);
        t.setDouble("ombre", ombre);
        t.setDouble("sprint", sprint);
        t.setBoolean("attenteSommeil", attenteSommeil);
        return t;
    }

    public static Traitement lire(NBTTagCompound t) {
        Traitement r = new Traitement();
        r.maladie = t.getString("maladie");
        r.prise = t.getInteger("prise");
        r.debutPrise = t.getLong("debutPrise");
        NBTTagList l = t.getTagList("faits", 8);
        for (int i = 0; i < l.tagCount(); i++) r.faits.add(l.getStringTagAt(i));
        r.dernierePrise = t.getLong("dernierePrise");
        r.debutCure = t.getLong("debutCure");
        r.px = t.getDouble("px");
        r.py = t.getDouble("py");
        r.pz = t.getDouble("pz");
        r.dimension = t.getInteger("dim");
        r.rangVirus = t.getInteger("rangVirus");
        r.nomVirus = t.getString("nomVirus");
        r.bonusConvalescence = t.getBoolean("bonusConv");
        r.dormiDepuisPrise = t.getBoolean("dormi");
        r.platMange = t.getBoolean("plat");
        r.horsConsigne = t.getDouble("hors");
        r.soleil = t.getDouble("soleil");
        r.feu = t.getDouble("feu");
        r.ombre = t.getDouble("ombre");
        r.sprint = t.getDouble("sprint");
        r.attenteSommeil = t.getBoolean("attenteSommeil");
        return r;
    }

    /** Nouvelle prise : on remet à zéro ce qui se mesure « depuis la dernière prise ». */
    public void nouvellePrise(long now) {
        dernierePrise = now;
        debutPrise = 0;
        faits.clear();
        dormiDepuisPrise = false;
        soleil = 0;
        feu = 0;
        ombre = 0;
        prise++;
    }
}
