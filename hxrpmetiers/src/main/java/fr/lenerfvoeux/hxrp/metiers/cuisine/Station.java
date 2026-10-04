package fr.lenerfvoeux.hxrp.metiers.cuisine;

import java.util.Locale;

/**
 * Les stations de cuisine, les gestes qu'elles accueillent, et la forme de leur modèle 3D
 * (boîte de sélection en 1/16 de bloc, face avant au nord ; lumière émise).
 */
public enum Station {
    PLAN_DE_TRAVAIL("Plan de travail", 0, box(0, 0, 0, 16, 16, 16), "Couper", "Étaler", "Pétrir", "Façonner"),
    BOL("Bol", 0, box(1, 0, 1, 15, 10, 15), "Fouetter", "Mélanger"),
    MORTIER("Mortier", 0, box(2, 0, 2, 14, 11, 14), "Piler"),
    TAMIS("Tamis", 0, box(1, 0, 1, 15, 9, 15), "Tamiser"),
    FOURNEAU("Fourneau", 0, box(0, 0, 0, 16, 16, 16), "Saisir", "Bouillir"),
    FOUR("Four", 0, box(0, 0, 0, 16, 16, 16), "Four"),
    GRILL("Grill", 5, box(1, 0, 2, 15, 13, 14), "Griller"),
    FRITEUSE("Friteuse", 0, box(1, 0, 2, 15, 12, 14), "Frire"),
    MARMITE("Marmite", 4, box(1, 0, 1, 15, 13, 15), "Mijoter"),
    PRESSE("Presse-agrumes", 0, box(1, 0, 1, 15, 8, 15), "Presser"),
    SHAKER("Shaker", 0, box(1, 0, 1, 15, 11, 15), "Secouer"),
    DOSEUR("Doseur", 0, box(0.5, 0, 1, 15.5, 10, 15), "Assaisonner");

    public final String label;
    public final String[] gestes;
    /** Lumière émise (0-15). */
    public final int lumiere;
    /** Boîte du modèle orienté au nord : x0, y0, z0, x1, y1, z1 en 1/16 de bloc. */
    public final double[] boite;

    Station(String label, int lumiere, double[] boite, String... gestes) {
        this.label = label;
        this.lumiere = lumiere;
        this.boite = boite;
        this.gestes = gestes;
    }

    private static double[] box(double... v) {
        return v;
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }

    public boolean pleine() {
        return boite[0] == 0 && boite[1] == 0 && boite[2] == 0 && boite[3] == 16 && boite[4] == 16 && boite[5] == 16;
    }

    public static Station forGeste(String geste) {
        for (Station s : values()) for (String g : s.gestes) if (g.equalsIgnoreCase(geste)) return s;
        return PLAN_DE_TRAVAIL;
    }

    /**
     * Boîte tournée selon l'orientation du bloc (index horizontal : 0 sud, 1 ouest, 2 nord, 3 est),
     * en fractions de bloc : x0, y0, z0, x1, y1, z1.
     */
    public static double[] tourner(double[] b, int horizontal) {
        double x0 = b[0], z0 = b[2], x1 = b[3], z1 = b[5];
        double[] r;
        switch (horizontal) {
            case 0: r = new double[]{16 - x1, b[1], 16 - z1, 16 - x0, b[4], 16 - z0}; break;   // sud : 180°
            case 1: r = new double[]{z0, b[1], 16 - x1, z1, b[4], 16 - x0}; break;             // ouest : 270°
            case 3: r = new double[]{16 - z1, b[1], x0, 16 - z0, b[4], x1}; break;             // est : 90°
            default: r = b.clone(); break;                                                     // nord
        }
        for (int i = 0; i < 6; i++) r[i] /= 16.0;
        return r;
    }
}
