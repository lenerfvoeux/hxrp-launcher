package fr.lenerfvoeux.hxrp.metiers.cuisine;

import java.util.Locale;

/** Les stations de cuisine et les gestes qu'elles accueillent. */
public enum Station {
    PLAN_DE_TRAVAIL("Plan de travail", "Couper", "Étaler", "Pétrir", "Façonner"),
    BOL("Bol", "Fouetter", "Mélanger"),
    MORTIER("Mortier", "Piler"),
    TAMIS("Tamis", "Tamiser"),
    FOURNEAU("Fourneau", "Saisir", "Bouillir"),
    FOUR("Four", "Four"),
    GRILL("Grill", "Griller"),
    FRITEUSE("Friteuse", "Frire"),
    MARMITE("Marmite", "Mijoter"),
    PRESSE("Presse-agrumes", "Presser"),
    SHAKER("Shaker", "Secouer"),
    DOSEUR("Doseur", "Assaisonner");

    public final String label;
    public final String[] gestes;

    Station(String label, String... gestes) {
        this.label = label;
        this.gestes = gestes;
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static Station forGeste(String geste) {
        for (Station s : values()) for (String g : s.gestes) if (g.equalsIgnoreCase(geste)) return s;
        return PLAN_DE_TRAVAIL;
    }
}
