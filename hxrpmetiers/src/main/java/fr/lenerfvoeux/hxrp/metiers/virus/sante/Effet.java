package fr.lenerfvoeux.hxrp.metiers.virus.sante;

import java.util.Random;

/**
 * Les effets des maladies et des blessures. Les effets « client » sont rendus par le client
 * (surcouches 2D, caméra, entrées) sur ordre du serveur ; les autres sont appliqués par le serveur.
 * La durée est celle d'un épisode (min-max, en secondes).
 */
public enum Effet {
    // ---------------------------------------------------------------- serveur
    LENTEUR0("lenteur0", false, 60, 120, "Lenteur légère"),
    LENTEUR1("lenteur1", false, 60, 120, "Lenteur I"),
    LENTEUR2("lenteur2", false, 50, 100, "Lenteur II"),
    FAIBLESSE1("faiblesse1", false, 60, 120, "Faiblesse I"),
    FAIBLESSE2("faiblesse2", false, 50, 100, "Faiblesse II"),
    NAUSEE("nausee", false, 10, 18, "Nausée"),
    CECITE("cecite", false, 3, 5, "Cécité"),
    DEGATS("degats", false, 0, 0, "Dégâts légers"),
    FAIM1("faim+", false, 120, 240, "Faim accélérée"),
    FAIM2("faim++", false, 120, 240, "Faim très accélérée"),
    SOIF1("soif+", false, 120, 240, "Soif accélérée"),
    SOIF2("soif++", false, 120, 240, "Soif très accélérée"),
    RECUP("recup-", false, 120, 240, "Récupération lente"),
    NOREGEN("noregen", false, 120, 240, "Plus de régénération"),
    COEUR1("coeur-1", false, 0, 0, "−1 cœur max"),
    COEUR2("coeur-2", false, 0, 0, "−2 cœurs max"),
    COEUR4("coeur-4", false, 0, 0, "−4 cœurs max"),
    NOURRITURE15("nourriture-15", false, 120, 240, "Nourriture −15 %"),
    NOURRITURE25("nourriture-25", false, 120, 240, "Nourriture −25 %"),
    LAIT("lait", false, 0, 0, "Le lait donne la nausée"),
    SAIGNEMENT("saignement", false, 0, 0, "Saignement"),
    HEMORRAGIE("hemorragie", false, 0, 0, "Hémorragie"),
    POISON("poison", false, 0, 0, "Empoisonnement"),
    DEGATS_SAUT("degats_saut", false, 0, 0, "Dégâts en sautant"),
    DEGATS_MARCHE("degats_marche", false, 0, 0, "Dégâts en marchant"),
    // ---------------------------------------------------------------- client
    SPRINT5("sprint5", true, 60, 120, "Sprint limité à 5 s"),
    SPRINT8("sprint8", true, 60, 120, "Sprint limité à 8 s"),
    NOSPRINT("nosprint", true, 30, 60, "Sprint impossible"),
    SAUT_REDUIT("saut-", true, 30, 60, "Saut réduit"),
    NOSAUT("nosaut", true, 6, 10, "Saut impossible"),
    MUET("muet", true, 0, 0, "Les coups ne font plus de bruit"),
    FLOUE("floue", true, 15, 30, "Vision floue"),
    PAUPIERES("paupieres", true, 20, 40, "Paupières lourdes"),
    ROUGE("rouge", true, 30, 60, "Bords rouges (fièvre)"),
    TOUX("toux", true, 1, 1, "Toux"),
    ETERNUE("eternue", true, 1, 1, "Éternuement"),
    TREMBLE("tremble", true, 10, 20, "Tremblements"),
    MAINS("mains", true, 30, 60, "Mains tremblantes"),
    VERTIGE("vertige", true, 8, 15, "Vertige"),
    LUMIERE("lumiere", true, 60, 120, "Sensibilité à la lumière"),
    ACOUPHENES("acouphenes", true, 20, 40, "Acouphènes"),
    HALLU("hallu", true, 30, 60, "Hallucinations"),
    TUNNEL("tunnel", true, 20, 40, "Vision tunnel"),
    GRIS("gris", true, 60, 120, "Daltonisme (gris)"),
    AMBRE("ambre", true, 60, 120, "Daltonisme (ambre)"),
    BLEU("bleu", true, 60, 120, "Daltonisme (bleuté)"),
    INVERSE("inverse", true, 20, 40, "Daltonisme (inversé)"),
    DESORIENTE("desoriente", true, 4, 6, "Désorientation"),
    IMMOBILE3("immobile3", true, 3, 3, "Immobilité 3 s"),
    IMMOBILE5("immobile5", true, 5, 5, "Immobilité 5 s");

    public final String code, libelle;
    public final boolean client;
    private final int min, max;

    Effet(String code, boolean client, int min, int max, String libelle) {
        this.code = code;
        this.client = client;
        this.min = min;
        this.max = max;
        this.libelle = libelle;
    }

    public static Effet de(String code) {
        for (Effet e : values()) if (e.code.equals(code)) return e;
        return null;
    }

    /** Durée d'un épisode (ms) ; 0 pour un effet instantané (dégâts). */
    public long dureeEpisode(Random r) {
        if (max <= 0) return 0;
        return (min + (max > min ? r.nextInt(max - min + 1) : 0)) * 1000L;
    }

    /** Effet qui immobilise brièvement (toux, éternuement, crise). */
    public boolean bloque() {
        return this == TOUX || this == ETERNUE || this == IMMOBILE3 || this == IMMOBILE5;
    }
}
