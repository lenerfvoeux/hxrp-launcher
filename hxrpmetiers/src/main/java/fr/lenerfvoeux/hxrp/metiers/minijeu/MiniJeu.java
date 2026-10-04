package fr.lenerfvoeux.hxrp.metiers.minijeu;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Un mini-jeu de cuisine, simulé par pas fixes de 10 ms.
 * <p>
 * Tout est déterministe (graine tirée par le serveur, java.util.Random, StrictMath) : le client joue,
 * enregistre ses entrées horodatées en temps simulé, et le serveur rejoue exactement la même partie
 * pour calculer la note lui-même. Aucune dépendance à Minecraft ici, pour que les deux côtés
 * partagent le même code.
 * <p>
 * Entrées fiables uniquement : clic au bon moment, clic sur une cible, maintenir/relâcher,
 * touches (Z Q S D, flèches, espace), molette.
 */
public abstract class MiniJeu {
    /** Durée d'un pas de simulation, en millisecondes. */
    public static final int PAS = 10;

    // types d'entrée
    public static final int CLIC = 0, RELACHE = 1, CLIC_DROIT = 2, RELACHE_DROIT = 3, TOUCHE = 4, TOUCHE_RELACHE = 5, MOLETTE = 6;
    // touches
    public static final int HAUT = 0, GAUCHE = 1, BAS = 2, DROITE = 3, ESPACE = 4, CHIFFRE = 10;
    // qualité d'un retour
    public static final int RATE = 0, BIEN = 1, PARFAIT = 2, INFO = 3;

    /** Un retour visuel immédiat : « PARFAIT », « BIEN », « RATÉ »… */
    public static final class Retour {
        public final int t, qualite, x, y;
        public final String texte;

        Retour(int t, int qualite, String texte, int x, int y) {
            this.t = t;
            this.qualite = qualite;
            this.texte = texte;
            this.x = x;
            this.y = y;
        }
    }

    public final int rang;
    public final long graine;
    public final int param;
    protected final Random rng;

    /** Temps simulé écoulé depuis le début (ms). */
    public int t;
    public boolean fini, abandon;

    /** Jauge en laiton : position de la flèche et zone verte (0-1). */
    public double valeur, zoneLo = 0.4, zoneHi = 0.6;
    /** Début de la zone rouge sur la jauge (> 1 : pas de zone rouge). */
    public double rouge = 2;
    /** Trait cible sur la jauge (< 0 : aucun). */
    public double trait = -1;

    /** Compteur de progression affiché en haut à droite (« COUPES 2/5 »). */
    public int fait, total;
    public final List<Retour> retours = new ArrayList<>();

    protected double somme;

    protected MiniJeu(int rang, long graine, int param) {
        this.rang = Math.max(0, Math.min(3, rang));
        this.graine = graine;
        this.param = param;
        this.rng = new Random(graine);
    }

    // ------------------------------------------------------------------ à définir
    public abstract String titre();

    /** Consigne affichée avant de commencer. */
    public abstract String consigne();

    /** Rappel des commandes (« CLIC OU ESPACE »). */
    public abstract String commandes();

    /** Nom du compteur (« COUPES », « PASSES »…). */
    public abstract String compteur();

    /** Durée maximale (ms). */
    public abstract int duree();

    protected abstract void tick();

    protected abstract void action(int type, int a, int b);

    /** Note finale (0-100), valable à tout moment (les actions manquantes comptent 0). */
    public double note() {
        return total <= 0 ? 0 : clamp(somme / total / 100) * 100;
    }

    // ------------------------------------------------------------------ moteur
    public final void avancer() {
        if (fini) return;
        t += PAS;
        tick();
        if (!fini && t >= duree()) fini = true;
    }

    public final void entree(int type, int a, int b) {
        if (fini) return;
        action(type, a, b);
    }

    /** Le joueur ferme le mini-jeu avant la fin : ce qui n'est pas fait compte zéro. */
    public final void abandonner() {
        if (fini) return;
        abandon = true;
        fini = true;
    }

    public final double noteFinale() {
        return Math.max(0, Math.min(100, note()));
    }

    public double reste() {
        return Math.max(0, duree() - t) / 1000.0;
    }

    // ------------------------------------------------------------------ outils
    protected static final double DT = PAS / 1000.0;

    protected static double clamp(double v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }

    protected static double tri(double tms, double periode) {
        double p = (tms % periode) / periode;
        return p < 0.5 ? p * 2 : 2 - p * 2;
    }

    protected static boolean appuiType(int type, int a) {
        return type == CLIC || (type == TOUCHE && a == ESPACE);
    }

    protected static boolean relacheType(int type, int a) {
        return type == RELACHE || (type == TOUCHE_RELACHE && a == ESPACE);
    }

    protected void retour(int qualite, String texte, int x, int y) {
        retours.add(new Retour(t, qualite, texte, x, y));
        if (retours.size() > 64) retours.remove(0);
    }

    protected void retour(int qualite, String texte) {
        retour(qualite, texte, -1, -1);
    }

    /**
     * Juge une valeur par rapport à une zone : PARFAIT au centre, BIEN dans le reste du vert,
     * RATÉ à côté (un peu à côté rapporte encore un peu, pour ne pas bloquer un débutant).
     */
    protected double juger(double v, double lo, double hi) {
        double c = (lo + hi) / 2, h = Math.max(1e-6, (hi - lo) / 2), d = Math.abs(v - c);
        if (d <= h * 0.35) {
            retour(PARFAIT, "PARFAIT");
            return 100;
        }
        if (d <= h) {
            retour(BIEN, "BIEN");
            return 95 - 25 * (d - 0.35 * h) / (0.65 * h);
        }
        retour(RATE, "RATÉ");
        return Math.max(0, 40 * (1 - (d - h) / 0.15));
    }

    /** Juge un écart de temps (ms) par rapport à une demi-fenêtre. */
    protected double jugerEcart(double ecart, double fenetre) {
        if (ecart <= fenetre * 0.4) {
            retour(PARFAIT, "PARFAIT");
            return 100;
        }
        if (ecart <= fenetre) {
            retour(BIEN, "BIEN");
            return 95 - 25 * (ecart - 0.4 * fenetre) / (0.6 * fenetre);
        }
        retour(RATE, ecart <= fenetre * 2 ? "PRESQUE" : "RATÉ");
        return ecart <= fenetre * 2 ? 40 * (1 - (ecart - fenetre) / fenetre) : 0;
    }

    /** Zone centrée, de largeur totale donnée. */
    protected void zone(double centre, double largeur) {
        zoneLo = centre - largeur / 2;
        zoneHi = centre + largeur / 2;
    }

    /** Valeur selon le rang du cuisinier (0★ à 3★). */
    protected double r(double... parRang) {
        return parRang[rang];
    }

    protected int ri(int... parRang) {
        return parRang[rang];
    }
}
