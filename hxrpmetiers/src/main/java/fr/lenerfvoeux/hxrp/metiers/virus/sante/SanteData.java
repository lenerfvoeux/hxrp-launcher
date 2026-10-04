package fr.lenerfvoeux.hxrp.metiers.virus.sante;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tout ce que le serveur sait de la santé d'un joueur : groupe sanguin, rang de Hunter Virus, maladie en cours
 * et son avancée (horodatages absolus : elle progresse même joueur déconnecté), effets à vie, immunité,
 * blessures, traitement et convalescence. Rien de tout ça ne survit à la mort, sauf le groupe sanguin et le métier.
 */
public class SanteData {
    public static final String[] GROUPES = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};

    // ---------------------------------------------------------------- identité et métier
    public String groupe = "";
    /** Date de création de l'identité (mod Identity) à laquelle le groupe a été tiré. */
    public long identiteRef;
    /** Rang de Hunter Virus (0 à 3), -1 si le joueur n'est pas Hunter Virus. */
    public int rangVirus = -1;

    // ---------------------------------------------------------------- maladie
    public String maladie = "";
    public long maladieDebut;
    /** Dernier stade annoncé au joueur (-1 : aucun). */
    public int stadeAnnonce = -1;
    public long prochainEpisode, prochainEpisodeVie, prochainEpisodeBlessure;
    /** Prochaine maladie tirée au hasard (0 : pas encore planifiée). */
    public long prochaineMaladie;
    public long immuniteFin;
    /** Maladies dont le stade « à vie » est acquis (ses effets restent après la guérison). */
    public final Set<String> aVie = new LinkedHashSet<>();
    /** Effets d'épisode en cours : code -> fin (ms). */
    public final Map<String, Long> episodes = new HashMap<>();

    // ---------------------------------------------------------------- blessures
    public final Map<String, Blessure> blessures = new LinkedHashMap<>();

    public static final class Blessure {
        public int stade;
        /** Dernière montée de stade (ms) : fenêtre de 10 minutes pour la suivante. */
        public long dernierStade;
        /** Aggravations au stade 4 (une toutes les 30 min, jusqu'à 3) et heure de la dernière. */
        public int aggravation;
        public long derniereAggravation;
        /** Soin posé aux stades 3-4 : repos accumulé (ms) et repos exigé (ms). 0 : pas de soin en attente. */
        public long reposRequis, reposFait;
        public int qualiteSoin;

        NBTTagCompound ecrire() {
            NBTTagCompound t = new NBTTagCompound();
            t.setInteger("stade", stade);
            t.setLong("dernier", dernierStade);
            t.setInteger("aggrav", aggravation);
            t.setLong("derniereAggrav", derniereAggravation);
            t.setLong("reposRequis", reposRequis);
            t.setLong("reposFait", reposFait);
            t.setInteger("qualiteSoin", qualiteSoin);
            return t;
        }

        static Blessure lire(NBTTagCompound t) {
            Blessure b = new Blessure();
            b.stade = Math.max(0, Math.min(4, t.getInteger("stade")));
            b.dernierStade = t.getLong("dernier");
            b.aggravation = t.getInteger("aggrav");
            b.derniereAggravation = t.getLong("derniereAggrav");
            b.reposRequis = t.getLong("reposRequis");
            b.reposFait = t.getLong("reposFait");
            b.qualiteSoin = t.getInteger("qualiteSoin");
            return b;
        }
    }

    public Blessure blessure(String id) {
        return blessures.computeIfAbsent(id, k -> new Blessure());
    }

    public int stadeBlessure(String id) {
        Blessure b = blessures.get(id);
        return b == null ? 0 : b.stade;
    }

    // ---------------------------------------------------------------- traitement et convalescence
    public Traitement traitement;
    public long convalescenceFin;
    /** Bonus des remèdes réussis à 95 % : immunité prolongée (h). */
    public int bonusImmuniteHeures;

    // ---------------------------------------------------------------- divers
    public long dernierePriseDeSang;
    public long dernierRepas, dernierAlcool, dernierCombat, dernierSommeil;
    /** Exposition au froid (s), pour la gelure. */
    public double froid;

    // ---------------------------------------------------------------- non sauvegardé
    /** Multiplicateurs lus par le Gourmet (faim et soif) et part de la nourriture qui profite. */
    public transient double multFaim = 1, multSoif = 1, facteurNourriture = 1;
    public transient boolean pasDeRegen, recupLente, lait, muet;
    public transient final List<Long> coupsRecents = new ArrayList<>();
    public transient long sprintDepuis;
    public transient String dernierEnvoi = "";
    public transient int dernierEnvoiTick;
    public transient boolean dirty = true;
    public transient double dernierX, dernierY, dernierZ;
    public transient long dernierSaignement, dernierPoison, dernierSautDouloureux;
    public transient int marche;
    /** Effets actifs à la dernière seconde (fin en ms ; Long.MAX_VALUE : tant que dure la cause). */
    public transient java.util.EnumMap<Effet, Long> actifs = new java.util.EnumMap<>(Effet.class);

    public boolean actif(Effet e) {
        Long f = actifs.get(e);
        return f != null && f > System.currentTimeMillis();
    }

    public boolean estVirus() { return rangVirus >= 0; }

    public boolean malade() { return !maladie.isEmpty(); }

    // ---------------------------------------------------------------- mort
    /** Mourir efface tout : seuls restent le groupe sanguin et le métier. */
    public void copierApresMort(SanteData o) {
        groupe = o.groupe;
        identiteRef = o.identiteRef;
        rangVirus = o.rangVirus;
        dirty = true;
    }

    // ---------------------------------------------------------------- NBT
    public NBTTagCompound write() {
        NBTTagCompound t = new NBTTagCompound();
        t.setString("groupe", groupe);
        t.setLong("identite", identiteRef);
        t.setInteger("rangVirus", rangVirus);
        t.setString("maladie", maladie);
        t.setLong("maladieDebut", maladieDebut);
        t.setInteger("stadeAnnonce", stadeAnnonce);
        t.setLong("prochainEpisode", prochainEpisode);
        t.setLong("prochainEpisodeVie", prochainEpisodeVie);
        t.setLong("prochainEpisodeBlessure", prochainEpisodeBlessure);
        t.setLong("prochaineMaladie", prochaineMaladie);
        t.setLong("immunite", immuniteFin);
        NBTTagList v = new NBTTagList();
        for (String s : aVie) v.appendTag(new NBTTagString(s));
        t.setTag("aVie", v);
        NBTTagCompound ep = new NBTTagCompound();
        for (Map.Entry<String, Long> e : episodes.entrySet()) ep.setLong(e.getKey(), e.getValue());
        t.setTag("episodes", ep);
        NBTTagCompound bl = new NBTTagCompound();
        for (Map.Entry<String, Blessure> e : blessures.entrySet()) if (e.getValue().stade > 0) bl.setTag(e.getKey(), e.getValue().ecrire());
        t.setTag("blessures", bl);
        if (traitement != null) t.setTag("traitement", traitement.ecrire());
        t.setLong("convalescence", convalescenceFin);
        t.setInteger("bonusImmunite", bonusImmuniteHeures);
        t.setLong("priseDeSang", dernierePriseDeSang);
        t.setLong("repas", dernierRepas);
        t.setLong("alcool", dernierAlcool);
        t.setLong("combat", dernierCombat);
        t.setLong("sommeil", dernierSommeil);
        t.setDouble("froid", froid);
        return t;
    }

    public void read(NBTTagCompound t) {
        groupe = t.getString("groupe");
        identiteRef = t.getLong("identite");
        rangVirus = t.hasKey("rangVirus") ? t.getInteger("rangVirus") : -1;
        maladie = t.getString("maladie");
        maladieDebut = t.getLong("maladieDebut");
        stadeAnnonce = t.hasKey("stadeAnnonce") ? t.getInteger("stadeAnnonce") : -1;
        prochainEpisode = t.getLong("prochainEpisode");
        prochainEpisodeVie = t.getLong("prochainEpisodeVie");
        prochainEpisodeBlessure = t.getLong("prochainEpisodeBlessure");
        prochaineMaladie = t.getLong("prochaineMaladie");
        immuniteFin = t.getLong("immunite");
        aVie.clear();
        NBTTagList v = t.getTagList("aVie", 8);
        for (int i = 0; i < v.tagCount(); i++) aVie.add(v.getStringTagAt(i));
        episodes.clear();
        NBTTagCompound ep = t.getCompoundTag("episodes");
        for (String k : ep.getKeySet()) episodes.put(k, ep.getLong(k));
        blessures.clear();
        NBTTagCompound bl = t.getCompoundTag("blessures");
        for (String k : bl.getKeySet()) blessures.put(k, Blessure.lire(bl.getCompoundTag(k)));
        traitement = t.hasKey("traitement") ? Traitement.lire(t.getCompoundTag("traitement")) : null;
        convalescenceFin = t.getLong("convalescence");
        bonusImmuniteHeures = t.getInteger("bonusImmunite");
        dernierePriseDeSang = t.getLong("priseDeSang");
        dernierRepas = t.getLong("repas");
        dernierAlcool = t.getLong("alcool");
        dernierCombat = t.getLong("combat");
        dernierSommeil = t.getLong("sommeil");
        froid = t.getDouble("froid");
        dirty = true;
    }
}
