package fr.lenerfvoeux.hxrp.phenix.stats;

import fr.lenerfvoeux.hxrp.phenix.HxrpPhenix;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Les réglages du Phénix, lus dans le classeur des boss {@code config/bestiaire_stats.xlsx} (lignes « phenix » des
 * onglets Boss, Attaques, Appâts et Butins), le même fichier que le mod hxrpbestiaire. Absent, il est créé avec la
 * copie rangée dans le jar ; illisible, le Phénix garde les valeurs de cette copie et le journal dit pourquoi.
 * Relu au démarrage du serveur et par /phenix recharger (ou /bestiaire recharger).
 */
public final class StatsPhenix {
    public static final String FICHIER = "bestiaire_stats.xlsx";
    private static final String ID = "phenix";

    /** Une phase du combat (onglet Boss). */
    public static final class Phase {
        public double declenche = 1, pv = 3000, armure = 22, robustesse = 8, recul = 1, vitesse = 0.28, degats = 1, portee = 1,
                pause = 3, vieReprise = 1;
        public boolean existe;
    }

    /** Une attaque (onglet Attaques). Dégâts avant le multiplicateur de la phase, portée en blocs, durées en secondes. */
    public static final class Attaque {
        public final String id;
        public int phase = 1;
        public double degats, portee, rayon, recharge, dureeEffet, poids = 1;

        Attaque(String id) {
            this.id = id;
        }
    }

    public static final Phase[] PHASES = {null, new Phase(), new Phase(), new Phase()};
    public static final Map<String, Attaque> ATTAQUES = new LinkedHashMap<>();
    public static double rayonCible = 24, rayonArene = 64;
    public static double appatDelaiMin = 30, appatDelaiMax = 90, appatDistanceMin = 40, appatDistanceMax = 60;
    public static int plumesMin = 1, plumesMax = 2;
    public static double plumesChance = 1;
    /** D'où viennent les valeurs en vigueur (pour /phenix info et le journal). */
    public static String source = "valeurs du jar";

    private StatsPhenix() {}

    public static Attaque attaque(String id) {
        Attaque a = ATTAQUES.get(id);
        return a != null ? a : new Attaque(id);
    }

    public static Phase phase(int n) {
        return PHASES[Math.max(1, Math.min(3, n))];
    }

    /** Le combat a-t-il une 3e phase (renaissance avec la vie reprise) ? */
    public static boolean troisPhases() {
        return PHASES[3].existe;
    }

    // ================================================================== chargement
    /** Charge config/bestiaire_stats.xlsx (le crée s'il manque) ; en cas d'erreur, les valeurs du jar. */
    public static synchronized void charger(File dossierConfig) {
        Classeur jar = null;
        try (InputStream in = StatsPhenix.class.getResourceAsStream("/assets/" + HxrpPhenix.MODID + "/" + FICHIER)) {
            if (in != null) jar = Classeur.lire(in);
        } catch (IOException e) {
            HxrpPhenix.LOG.error("Phénix : copie du classeur dans le jar illisible ({})", e.toString());
        }
        File f = new File(dossierConfig, FICHIER);
        Classeur c = null;
        try {
            if (!f.isFile()) {
                try (InputStream in = StatsPhenix.class.getResourceAsStream("/assets/" + HxrpPhenix.MODID + "/" + FICHIER)) {
                    if (in != null) {
                        Files.copy(in, f.toPath());
                        HxrpPhenix.LOG.info("Phénix : {} créé avec les réglages par défaut", f);
                    }
                }
            }
            if (f.isFile()) c = Classeur.lire(f);
        } catch (IOException e) {
            HxrpPhenix.LOG.error("Phénix : {} illisible ({}), réglages du jar utilisés", f, e.toString());
        }
        reinitialiser();
        if (jar != null) appliquer(jar);
        if (c != null) {
            appliquer(c);
            source = f.getPath();
        } else {
            source = "valeurs du jar";
        }
        HxrpPhenix.LOG.info("Phénix : réglages lus ({}) : {} PV, {} phase(s), {} attaques, appât en {}-{} s à {}-{} blocs",
                source, (int) PHASES[1].pv, PHASES[3].existe ? 3 : PHASES[2].existe ? 2 : 1, ATTAQUES.size(),
                (int) appatDelaiMin, (int) appatDelaiMax, (int) appatDistanceMin, (int) appatDistanceMax);
    }

    private static void reinitialiser() {
        for (int i = 1; i <= 3; i++) PHASES[i] = new Phase();
        ATTAQUES.clear();
    }

    /** Recopie les lignes « phenix » d'un classeur par-dessus les valeurs en vigueur (une case vide ne change rien). */
    static void appliquer(Classeur c) {
        for (Map<String, String> l : c.feuille("Boss")) {
            if (!ID.equals(texte(l, "id"))) continue;
            int n = (int) nombre(l, "phase", 0);
            if (n < 1 || n > 3) continue;
            Phase p = PHASES[n];
            p.existe = true;
            p.declenche = nombre(l, "declenchee a (part de vie restante)", p.declenche);
            p.pv = Math.max(1, nombre(l, "pv max", p.pv));
            p.armure = nombre(l, "armure", p.armure);
            p.robustesse = nombre(l, "robustesse armure", p.robustesse);
            p.recul = nombre(l, "resistance au recul", p.recul);
            p.vitesse = Math.max(0.01, nombre(l, "vitesse", p.vitesse));
            p.degats = Math.max(0, nombre(l, "multiplicateur de degats", p.degats));
            p.portee = Math.max(0.1, nombre(l, "multiplicateur de portee", p.portee));
            p.pause = Math.max(0, nombre(l, "pause entre attaques (s)", p.pause));
            p.vieReprise = Math.max(0.01, Math.min(1, nombre(l, "vie reprise en entrant dans la phase (part)", p.vieReprise)));
            if (n == 1) {
                rayonCible = Math.max(4, nombre(l, "rayon de ciblage (blocs)", rayonCible));
                rayonArene = Math.max(8, nombre(l, "rayon d'arene (blocs)", rayonArene));
            }
        }
        // une phase sans ligne reprend les valeurs de la précédente
        for (int i = 2; i <= 3; i++) if (!PHASES[i].existe) copier(PHASES[i - 1], PHASES[i]);
        for (Map<String, String> l : c.feuille("Attaques")) {
            String id = texte(l, "id attaque");
            if (!ID.equals(texte(l, "id boss")) || id.isEmpty()) continue;
            Attaque a = ATTAQUES.computeIfAbsent(id, Attaque::new);
            a.phase = (int) nombre(l, "des la phase", a.phase);
            a.degats = Math.max(0, nombre(l, "degats (pv, avant multiplicateur)", a.degats));
            a.portee = Math.max(0, nombre(l, "portee (blocs)", a.portee));
            a.rayon = Math.max(0, nombre(l, "largeur ou rayon (blocs)", a.rayon));
            a.recharge = Math.max(0, nombre(l, "recharge (s)", a.recharge));
            a.dureeEffet = Math.max(0, nombre(l, "duree de l'effet (s)", a.dureeEffet));
            a.poids = Math.max(0, nombre(l, "poids (frequence)", a.poids));
        }
        for (Map<String, String> l : c.feuille("Appâts")) {
            if (!ID.equals(texte(l, "id boss"))) continue;
            appatDelaiMin = Math.max(0, nombre(l, "delai min (s)", appatDelaiMin));
            appatDelaiMax = Math.max(appatDelaiMin, nombre(l, "delai max (s)", appatDelaiMax));
            appatDistanceMin = Math.max(8, nombre(l, "distance d'apparition min (blocs)", appatDistanceMin));
            appatDistanceMax = Math.max(appatDistanceMin, nombre(l, "distance d'apparition max (blocs)", appatDistanceMax));
        }
        for (Map<String, String> l : c.feuille("Butins")) {
            if (!ID.equals(texte(l, "id boss")) || !"plume_de_phenix".equals(texte(l, "id objet"))) continue;
            plumesMin = (int) Math.max(0, nombre(l, "quantite min", plumesMin));
            plumesMax = (int) Math.max(plumesMin, nombre(l, "quantite max", plumesMax));
            plumesChance = Math.max(0, Math.min(1, nombre(l, "chance", plumesChance)));
        }
    }

    private static void copier(Phase de, Phase vers) {
        vers.declenche = de.declenche;
        vers.pv = de.pv;
        vers.armure = de.armure;
        vers.robustesse = de.robustesse;
        vers.recul = de.recul;
        vers.vitesse = de.vitesse;
        vers.degats = de.degats;
        vers.portee = de.portee;
        vers.pause = de.pause;
    }

    private static String texte(Map<String, String> l, String col) {
        String v = l.get(col);
        return v == null ? "" : v.trim();
    }

    /** Un nombre d'une case (virgule ou point décimal), ou la valeur par défaut si la case est vide ou illisible. */
    static double nombre(Map<String, String> l, String col, double defaut) {
        String v = l.get(col);
        if (v == null || v.trim().isEmpty()) return defaut;
        try {
            return Double.parseDouble(v.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            HxrpPhenix.LOG.warn("Phénix : « {} » n'est pas un nombre (colonne « {} »), {} gardé", v, col, defaut);
            return defaut;
        }
    }

    /** Résumé pour /phenix info. */
    public static String resume() {
        StringBuilder b = new StringBuilder(String.format(Locale.ROOT, "Réglages : %s · %d PV · ciblage %d · arène %d",
                source, (int) PHASES[1].pv, (int) rayonCible, (int) rayonArene));
        for (int i = 1; i <= 3; i++) {
            if (i > 1 && !PHASES[i].existe) continue;
            Phase p = PHASES[i];
            b.append(String.format(Locale.ROOT, " · phase %d : dégâts ×%.2f, portée ×%.2f, armure %d", i, p.degats, p.portee, (int) p.armure));
        }
        return b.toString();
    }

    /** Pour les tests hors Minecraft : applique un classeur sans toucher au disque. */
    static void appliquerSeul(Classeur c) {
        reinitialiser();
        appliquer(c);
    }
}
