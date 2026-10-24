package fr.lenerfvoeux.hxrp.bestiaire.stats;

import fr.lenerfvoeux.hxrp.bestiaire.HxrpBestiaire;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Les réglages des boss, lus dans le classeur {@code config/bestiaire_stats.xlsx} (onglets Boss, Attaques, Sbires,
 * Appâts, Butins). Absent, il est créé avec la copie rangée dans le jar ; illisible, les valeurs du jar restent et le
 * journal dit pourquoi ; une case vide garde la valeur par défaut. Relu au démarrage du serveur et par
 * /bestiaire recharger, sans recompiler. Le Phénix (mod hxrpphenix) lit les lignes « phenix » du même fichier.
 */
public final class Stats {
    public static final String FICHIER = "bestiaire_stats.xlsx";

    /** Une phase du combat (onglet Boss). */
    public static final class Phase {
        public double declenche = 1, pv = 3000, armure = 22, robustesse = 8, recul = 1, vitesse = 0.28, degats = 1, portee = 1,
                pause = 3, vieReprise = 1;
        public boolean existe;

        void copier(Phase de) {
            declenche = de.declenche;
            pv = de.pv;
            armure = de.armure;
            robustesse = de.robustesse;
            recul = de.recul;
            vitesse = de.vitesse;
            degats = de.degats;
            portee = de.portee;
            pause = de.pause;
        }
    }

    /** Une attaque (onglet Attaques). Dégâts avant le multiplicateur de la phase, portée en blocs, durées en secondes. */
    public static final class Attaque {
        public final String id;
        public String nom = "";
        public int phase = 1;
        public double degats, portee, rayon, recharge, dureeEffet, poids = 1;
        public String effet = "";

        Attaque(String id) {
            this.id = id;
        }
    }

    /** Un type de sbire (onglet Sbires). */
    public static final class Sbire {
        public final String id;
        public String nom = "";
        public double pv = 60, degats = 10, vitesse = 0.3;
        public int min = 1, max = 1;
        public double mult2 = 2, mult3 = 3;
        /** Particularité du classeur (« poison 4 s », « bloque de face »…). */
        public String particularite = "";

        Sbire(String id) {
            this.id = id;
        }

        /** Nombre tiré pour un appel dans la phase donnée. */
        public int nombre(java.util.Random r, int phase) {
            int n = min + (max > min ? r.nextInt(max - min + 1) : 0);
            double k = phase >= 3 ? mult3 : phase == 2 ? mult2 : 1;
            return Math.max(0, (int) Math.round(n * k));
        }
    }

    /** Un butin (onglet Butins). */
    public static final class Butin {
        public final String objet;
        public int min = 1, max = 1;
        public double chance = 1;

        Butin(String objet) {
            this.objet = objet;
        }
    }

    /** Tout ce que le classeur dit d'un boss. */
    public static final class Boss {
        public final String id;
        public String nom = "";
        public final Phase[] phases = {null, new Phase(), new Phase(), new Phase()};
        public final Map<String, Attaque> attaques = new LinkedHashMap<>();
        public final Map<String, Sbire> sbires = new LinkedHashMap<>();
        public final List<Butin> butins = new ArrayList<>();
        public double rayonCible = 24, rayonArene = 64;
        public double appatDelaiMin = 30, appatDelaiMax = 90, appatDistanceMin = 40, appatDistanceMax = 60;

        Boss(String id) {
            this.id = id;
        }

        public Phase phase(int n) {
            return phases[Math.max(1, Math.min(3, n))];
        }

        public boolean troisPhases() {
            return phases[3].existe;
        }

        public boolean deuxPhases() {
            return phases[2].existe;
        }

        public Attaque attaque(String id) {
            Attaque a = attaques.get(id);
            return a != null ? a : new Attaque(id);
        }

        public Sbire sbire(String id) {
            Sbire s = sbires.get(id);
            return s != null ? s : new Sbire(id);
        }

        public int nombrePhases() {
            return phases[3].existe ? 3 : phases[2].existe ? 2 : 1;
        }
    }

    private static final Map<String, Boss> BOSS = new LinkedHashMap<>();
    /** D'où viennent les valeurs en vigueur. */
    public static String source = "valeurs du jar";

    private Stats() {}

    public static Boss boss(String id) {
        Boss b = BOSS.get(id);
        if (b == null) {
            b = new Boss(id);
            BOSS.put(id, b);
        }
        return b;
    }

    // ================================================================== chargement
    /** Charge config/bestiaire_stats.xlsx (le crée s'il manque) par-dessus la copie du jar. */
    public static synchronized void charger(File dossierConfig) {
        Classeur jar = null;
        try (InputStream in = Stats.class.getResourceAsStream("/assets/" + HxrpBestiaire.MODID + "/" + FICHIER)) {
            if (in != null) jar = Classeur.lire(in);
        } catch (IOException e) {
            HxrpBestiaire.LOG.error("Bestiaire : copie du classeur dans le jar illisible ({})", e.toString());
        }
        File f = new File(dossierConfig, FICHIER);
        Classeur c = null;
        try {
            if (!f.isFile()) {
                try (InputStream in = Stats.class.getResourceAsStream("/assets/" + HxrpBestiaire.MODID + "/" + FICHIER)) {
                    if (in != null) {
                        Files.copy(in, f.toPath());
                        HxrpBestiaire.LOG.info("Bestiaire : {} créé avec les réglages par défaut", f);
                    }
                }
            }
            if (f.isFile()) c = Classeur.lire(f);
        } catch (IOException e) {
            HxrpBestiaire.LOG.error("Bestiaire : {} illisible ({}), réglages du jar utilisés", f, e.toString());
        }
        BOSS.clear();
        if (jar != null) appliquer(jar);
        if (c != null) {
            appliquer(c);
            source = f.getPath();
        } else {
            source = "valeurs du jar";
        }
        int attaques = 0;
        for (Boss b : BOSS.values()) attaques += b.attaques.size();
        HxrpBestiaire.LOG.info("Bestiaire : réglages lus ({}) : {} boss, {} attaques", source, BOSS.size(), attaques);
    }

    /** Recopie un classeur par-dessus les valeurs en vigueur. */
    static void appliquer(Classeur c) {
        for (Map<String, String> l : c.feuille("Boss")) {
            String id = texte(l, "id");
            int n = (int) nombre(l, "phase", 0);
            if (id.isEmpty() || n < 1 || n > 3) continue;
            Boss b = boss(id);
            b.nom = texte(l, "boss").isEmpty() ? b.nom : texte(l, "boss");
            Phase p = b.phases[n];
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
                b.rayonCible = Math.max(4, nombre(l, "rayon de ciblage (blocs)", b.rayonCible));
                b.rayonArene = Math.max(8, nombre(l, "rayon d'arene (blocs)", b.rayonArene));
            }
        }
        // une phase sans ligne reprend les valeurs de la précédente
        for (Boss b : BOSS.values())
            for (int i = 2; i <= 3; i++) if (!b.phases[i].existe) b.phases[i].copier(b.phases[i - 1]);
        for (Map<String, String> l : c.feuille("Attaques")) {
            String boss = texte(l, "id boss"), id = texte(l, "id attaque");
            if (boss.isEmpty() || id.isEmpty()) continue;
            Attaque a = boss(boss).attaques.computeIfAbsent(id, Attaque::new);
            if (!texte(l, "attaque").isEmpty()) a.nom = texte(l, "attaque");
            a.phase = (int) nombre(l, "des la phase", a.phase);
            a.degats = Math.max(0, nombre(l, "degats (pv, avant multiplicateur)", a.degats));
            a.portee = Math.max(0, nombre(l, "portee (blocs)", a.portee));
            a.rayon = Math.max(0, nombre(l, "largeur ou rayon (blocs)", a.rayon));
            a.recharge = Math.max(0, nombre(l, "recharge (s)", a.recharge));
            a.dureeEffet = Math.max(0, nombre(l, "duree de l'effet (s)", a.dureeEffet));
            a.poids = Math.max(0, nombre(l, "poids (frequence)", a.poids));
            if (!texte(l, "effet").isEmpty()) a.effet = texte(l, "effet");
        }
        for (Map<String, String> l : c.feuille("Sbires")) {
            String boss = texte(l, "id boss"), id = texte(l, "id sbire");
            if (boss.isEmpty() || id.isEmpty()) continue;
            Sbire s = boss(boss).sbires.computeIfAbsent(id, Sbire::new);
            if (!texte(l, "sbire").isEmpty()) s.nom = texte(l, "sbire");
            s.pv = Math.max(1, nombre(l, "pv", s.pv));
            s.degats = Math.max(0, nombre(l, "degats (pv)", s.degats));
            s.vitesse = Math.max(0.01, nombre(l, "vitesse", s.vitesse));
            s.min = (int) Math.max(0, nombre(l, "nombre min par appel", s.min));
            s.max = (int) Math.max(s.min, nombre(l, "nombre max par appel", s.max));
            s.mult2 = Math.max(0, nombre(l, "multiplicateur du nombre en phase 2", s.mult2));
            s.mult3 = Math.max(0, nombre(l, "multiplicateur du nombre en phase 3", s.mult3));
            if (!texte(l, "particularite").isEmpty()) s.particularite = texte(l, "particularite");
        }
        for (Map<String, String> l : c.feuille("Appâts")) {
            String boss = texte(l, "id boss");
            if (boss.isEmpty() || texte(l, "id appat").isEmpty()) continue;
            Boss b = boss(boss);
            b.appatDelaiMin = Math.max(0, nombre(l, "delai min (s)", b.appatDelaiMin));
            b.appatDelaiMax = Math.max(b.appatDelaiMin, nombre(l, "delai max (s)", b.appatDelaiMax));
            b.appatDistanceMin = Math.max(8, nombre(l, "distance d'apparition min (blocs)", b.appatDistanceMin));
            b.appatDistanceMax = Math.max(b.appatDistanceMin, nombre(l, "distance d'apparition max (blocs)", b.appatDistanceMax));
        }
        // les butins d'un boss sont remplacés en bloc par ceux du classeur qui en parle
        Map<String, List<Butin>> butins = new LinkedHashMap<>();
        for (Map<String, String> l : c.feuille("Butins")) {
            String boss = texte(l, "id boss"), objet = texte(l, "id objet");
            if (boss.isEmpty() || objet.isEmpty()) continue;
            Butin u = new Butin(objet);
            u.min = (int) Math.max(0, nombre(l, "quantite min", 1));
            u.max = (int) Math.max(u.min, nombre(l, "quantite max", u.min));
            u.chance = Math.max(0, Math.min(1, nombre(l, "chance", 1)));
            butins.computeIfAbsent(boss, k -> new ArrayList<>()).add(u);
        }
        for (Map.Entry<String, List<Butin>> e : butins.entrySet()) {
            Boss b = boss(e.getKey());
            b.butins.clear();
            b.butins.addAll(e.getValue());
        }
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
            HxrpBestiaire.LOG.warn("Bestiaire : « {} » n'est pas un nombre (colonne « {} »), {} gardé", v, col, defaut);
            return defaut;
        }
    }

    /** Résumé d'un boss pour /<boss> info. */
    public static String resume(String id) {
        Boss b = boss(id);
        StringBuilder s = new StringBuilder(String.format(Locale.ROOT, "Réglages (%s) : %d PV · ciblage %d · arène %d · %d attaques",
                source, (int) b.phases[1].pv, (int) b.rayonCible, (int) b.rayonArene, b.attaques.size()));
        for (int i = 1; i <= b.nombrePhases(); i++) {
            Phase p = b.phases[i];
            s.append(String.format(Locale.ROOT, " · phase %d : dégâts ×%.2f, armure %d", i, p.degats, (int) p.armure));
        }
        return s.toString();
    }

    /** Pour les tests hors Minecraft. */
    static void appliquerSeul(Classeur c) {
        BOSS.clear();
        appliquer(c);
    }
}
