package fr.lenerfvoeux.hxrp.metiers.client;

/** Logique d'un geste de cuisine : état, entrées souris, note finale. */
public abstract class Gestes {
    public int rang;
    public double note = -1;      // >= 0 quand l'étape est terminée
    public double valeur;         // position de la flèche sur la jauge (0-1)
    public double zoneLo = 0.4, zoneHi = 0.6;
    public String stat = "";
    public boolean commence;

    public void init() {}

    public double duree() { return 14; }
    public String consigne() { return "Clique pour commencer"; }

    public void tick(double dt) {}
    public void draw(GuiMiniJeu g) {}
    public void down(int x, int y) { commence = true; }
    public void drag(int x, int y) {}
    public void up(int x, int y) {}

    /** Appelé quand le temps est écoulé : donne la note de ce qui a été fait. */
    public abstract double noteFinale();

    protected double largeur() { return new double[]{1.0, 0.8, 0.62, 0.48}[Math.max(0, Math.min(3, rang))]; }
    protected void zone(double centre, double demi) { zoneLo = centre - demi * largeur(); zoneHi = centre + demi * largeur(); }
    protected static double clamp(double v) { return Math.max(0, Math.min(1, v)); }
    protected static double proche(double v, double lo, double hi, double pente) {
        if (v >= lo && v <= hi) return 1;
        double e = v < lo ? lo - v : v - hi;
        return Math.max(0, 1 - e * pente);
    }
}
