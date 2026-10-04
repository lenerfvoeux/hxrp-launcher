package fr.lenerfvoeux.hxrp.metiers.virus.sante;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.data.FoodDatabase;
import fr.lenerfvoeux.hxrp.metiers.data.FoodEntry;
import fr.lenerfvoeux.hxrp.metiers.virus.VirusConfig;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Defs;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.DonneesVirus;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Apparition, avancée et guérison des maladies. */
public final class Maladies {
    public static final long JOUR = 86_400_000L, HEURE = 3_600_000L, MINUTE = 60_000L;
    private static final Random RNG = new Random();

    private Maladies() {}

    // ================================================================== éligibilité
    /** La maladie peut-elle toucher ce joueur (groupe sanguin, sexe) ? Sexe inconnu : aucune maladie réservée à un sexe. */
    public static boolean eligible(EntityPlayerMP p, SanteData d, Defs.Maladie m) {
        if (m.touche == null) return true;
        if (m.touche.groupes != null && !m.touche.groupes.isEmpty() && !m.touche.groupes.contains(d.groupe)) return false;
        if (m.touche.sexe != null && !m.touche.sexe.isEmpty()) return m.touche.sexe.equals(Identite.sexe(p));
        return true;
    }

    // ================================================================== stades
    /** Durée totale jusqu'au dernier stade (ms). */
    public static double dureeTotale(Defs.Maladie m) {
        return Math.max(1, m.jours) * JOUR * VirusConfig.vitesseStades;
    }

    /** Stade atteint (0…n-1) d'après le temps écoulé depuis le début. */
    public static int stade(Defs.Maladie m, long debut, long now) {
        int n = m.stades.size();
        if (n <= 1) return 0;
        double pas = dureeTotale(m) / (n - 1);
        int s = (int) Math.floor((now - debut) / pas);
        return Math.max(0, Math.min(n - 1, s));
    }

    /** Début fictif pour qu'un stade donné soit atteint maintenant. */
    public static long debutPourStade(Defs.Maladie m, int stade, long now) {
        int n = m.stades.size();
        if (n <= 1 || stade <= 0) return now;
        double pas = dureeTotale(m) / (n - 1);
        return now - (long) Math.ceil(pas * Math.min(stade, n - 1)) - 1000;
    }

    // ================================================================== contracter
    /**
     * Le joueur tombe malade. Refusé s'il est déjà malade (une seule maladie à la fois).
     * L'immunité n'arrête que les maladies naturelles (hasard, nourriture), pas un admin.
     */
    public static boolean contracter(EntityPlayerMP p, SanteData d, Defs.Maladie m, int stade, boolean force) {
        long now = System.currentTimeMillis();
        if (d.malade() && !force) return false;
        stade = Math.max(0, Math.min(m.stades.size() - 1, stade));
        d.maladie = m.id;
        d.maladieDebut = debutPourStade(m, stade, now);
        d.stadeAnnonce = stade;
        d.traitement = null;
        d.convalescenceFin = 0;
        d.prochainEpisode = now + intervalleEpisode();
        d.prochaineMaladie = 0;
        d.episodes.clear();
        d.dirty = true;
        if (stade == 0) message(p, m.premierMessage);
        else message(p, m.stades.get(stade).message);
        if (m.stades.get(stade).vie) acquerirVie(p, d, m);
        HxrpMetiers.LOG.info("Virus : {} contracte {} (stade {})", p.getName(), m.id, stade + 1);
        return true;
    }

    public static void acquerirVie(EntityPlayerMP p, SanteData d, Defs.Maladie m) {
        if (d.aVie.add(m.id)) {
            p.sendMessage(new TextComponentString(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "Ce mal a laissé en vous une trace qui ne s'effacera plus."));
            d.dirty = true;
        }
    }

    /** Message RP d'un symptôme, en italique gris foncé. */
    public static void message(EntityPlayerMP p, String m) {
        if (m == null || m.isEmpty()) return;
        p.sendMessage(new TextComponentString(TextFormatting.GRAY + "" + TextFormatting.ITALIC + m));
    }

    public static long intervalleEpisode() {
        double a = Math.min(VirusConfig.episodeMinutesMin, VirusConfig.episodeMinutesMax);
        double b = Math.max(VirusConfig.episodeMinutesMin, VirusConfig.episodeMinutesMax);
        return (long) ((a + RNG.nextDouble() * (b - a)) * MINUTE);
    }

    // ================================================================== guérir
    /** Guérison : la maladie s'en va, les effets à vie restent, 24 h d'immunité. */
    public static void guerir(EntityPlayerMP p, SanteData d, boolean immunite) {
        long now = System.currentTimeMillis();
        d.maladie = "";
        d.maladieDebut = 0;
        d.stadeAnnonce = -1;
        d.traitement = null;
        d.convalescenceFin = 0;
        d.episodes.clear();
        if (immunite) d.immuniteFin = now + (VirusConfig.immuniteHeures + d.bonusImmuniteHeures) * HEURE;
        d.bonusImmuniteHeures = 0;
        d.prochaineMaladie = Math.max(now, d.immuniteFin) + delaiAleatoire();
        d.dirty = true;
    }

    public static long delaiAleatoire() {
        double a = Math.min(VirusConfig.maladieJoursMin, VirusConfig.maladieJoursMax);
        double b = Math.max(VirusConfig.maladieJoursMin, VirusConfig.maladieJoursMax);
        return (long) ((a + RNG.nextDouble() * (b - a)) * JOUR);
    }

    // ================================================================== hasard
    /** Tirage au hasard quand l'heure prévue est passée (appelé chaque seconde). */
    public static void hasard(EntityPlayerMP p, SanteData d, long now) {
        if (!VirusConfig.maladiesAleatoires || p.capabilities.isCreativeMode || p.isSpectator()) return;
        if (d.prochaineMaladie == 0) {
            if (!d.malade()) {
                d.prochaineMaladie = Math.max(now, d.immuniteFin) + delaiAleatoire();
                d.dirty = true;
            }
            return;
        }
        if (now < d.prochaineMaladie || d.malade() || now < d.immuniteFin || d.groupe.isEmpty()) return;
        Defs.Maladie m = tirer(p, d, null);
        if (m != null) contracter(p, d, m, 0, false);
        else d.prochaineMaladie = now + delaiAleatoire();
    }

    /**
     * Tire une maladie que le joueur peut attraper, pondérée par le rang (les maladies graves sont rares)
     * et par le lieu où il se trouve. origine : ne garder que ces origines (« alimentaire »…), null = toutes
     * sauf celles qui ne viennent que de l'alcool.
     */
    public static Defs.Maladie tirer(EntityPlayerMP p, SanteData d, Set<String> origine) {
        Set<String> ici = contexte(p, d);
        List<Defs.Maladie> l = new ArrayList<>();
        List<Double> poids = new ArrayList<>();
        double total = 0;
        for (Defs.Maladie m : DonneesVirus.MALADIES.values()) {
            if (!eligible(p, d, m)) continue;
            double w;
            if (origine == null) {
                if (m.origine.contains("alcool")) continue;
                w = poidsRang(m.rang);
                for (String c : m.contexte) if (ici.contains(c)) { w *= VirusConfig.poidsContexte; break; }
            } else {
                if (!m.origine.contains("alimentaire") && !m.origine.contains("alcool")) continue;
                w = 0;
                for (String o : m.origine) if (origine.contains(o)) w += o.equals("alimentaire") ? 1 : 3;
                if (w <= 0) continue;
                w *= poidsRang(m.rang);
            }
            if (w <= 0) continue;
            l.add(m);
            poids.add(w);
            total += w;
        }
        if (l.isEmpty()) return null;
        double r = RNG.nextDouble() * total;
        for (int i = 0; i < l.size(); i++) {
            r -= poids.get(i);
            if (r <= 0) return l.get(i);
        }
        return l.get(l.size() - 1);
    }

    private static double poidsRang(int r) {
        int[] p = VirusConfig.poidsParRang;
        return p == null || p.length == 0 ? 1 : Math.max(0, p[Math.max(0, Math.min(p.length - 1, r))]);
    }

    /** Où se trouve le joueur : marais, désert, froid, altitude, grotte, eau, jungle, nuit, combat… */
    public static Set<String> contexte(EntityPlayerMP p, SanteData d) {
        Set<String> s = new HashSet<>();
        BlockPos pos = p.getPosition();
        Biome b = p.world.getBiome(pos);
        if (BiomeDictionary.hasType(b, Type.SWAMP)) s.add("marais");
        if (BiomeDictionary.hasType(b, Type.SANDY) && BiomeDictionary.hasType(b, Type.HOT)) s.add("desert");
        if (BiomeDictionary.hasType(b, Type.SAVANNA)) s.add("savane");
        if (BiomeDictionary.hasType(b, Type.MESA) || BiomeDictionary.hasType(b, Type.SAVANNA) || (BiomeDictionary.hasType(b, Type.DRY) && BiomeDictionary.hasType(b, Type.HOT))) s.add("aride");
        if (BiomeDictionary.hasType(b, Type.JUNGLE)) s.add("jungle");
        if (BiomeDictionary.hasType(b, Type.FOREST)) s.add("foret");
        if (BiomeDictionary.hasType(b, Type.PLAINS)) s.add("plaines");
        if (BiomeDictionary.hasType(b, Type.COLD) || BiomeDictionary.hasType(b, Type.SNOWY) || b.getTemperature(pos) < 0.2f) s.add("froid");
        if (BiomeDictionary.hasType(b, Type.RIVER) || p.isInWater()) s.add("eau");
        if (p.posY > 100) s.add("altitude");
        if (p.posY < 50 && !p.world.canSeeSky(pos)) s.add("grotte");
        long t = p.world.getWorldTime() % 24000;
        if (t >= 13000 && t < 23000) s.add("nuit");
        if (System.currentTimeMillis() - d.dernierCombat < 10 * MINUTE) s.add("combat");
        return s;
    }

    // ================================================================== nourriture
    /**
     * Un plat du Gourmet vient d'être mangé : plat étrange, raté ou presque périmé, chance de tomber malade
     * (maladie d'origine alimentaire, de préférence liée à ce qu'il contient).
     */
    public static void platMange(EntityPlayerMP p, SanteData d, FoodEntry e, int qualite, double fraicheur) {
        long now = System.currentTimeMillis();
        if (d.malade() || now < d.immuniteFin || p.capabilities.isCreativeMode) return;
        int chance = 0;
        if (qualite < 60) chance = VirusConfig.chancePlatEtrange;
        else if (qualite < 80) chance = VirusConfig.chancePlatRate;
        if (fraicheur < 0.25) chance = Math.max(chance, VirusConfig.chancePlatPerime);
        if (chance <= 0 || RNG.nextInt(100) >= chance) return;
        Set<String> o = new HashSet<>();
        o.add("alimentaire");
        for (String id : ingredients(e)) {
            FoodEntry f = FoodDatabase.get(id);
            if (id.startsWith("lait") || id.equals("fromage") || id.equals("creme") || id.equals("yaourt") || id.equals("beurre")) o.add("lait");
            if (id.equals("champignon")) o.add("champignon");
            if (f != null && "Poissons et fruits de mer".equals(f.cat)) o.add("poisson");
        }
        Defs.Maladie m = tirer(p, d, o);
        if (m != null) contracter(p, d, m, 0, false);
    }

    /** Ingrédients d'un plat, préparations comprises (dépliées). */
    private static List<String> ingredients(FoodEntry e) {
        List<String> out = new ArrayList<>();
        ajouter(e, out, 0);
        return out;
    }

    private static void ajouter(FoodEntry e, List<String> out, int prof) {
        if (e == null || prof > 4) return;
        for (String id : e.ingredients) {
            out.add(id);
            FoodEntry f = FoodDatabase.get(id);
            if (f != null && f.isPreparation()) ajouter(f, out, prof + 1);
        }
    }
}
