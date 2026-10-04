package fr.lenerfvoeux.hxrp.metiers.virus.donnees;

/**
 * Les neuf machines de l'officine : identifiant dans les recettes, nom du bloc, mini-jeu,
 * boîte du modèle 3D (1/16 de bloc, face avant au nord) et lumière émise.
 */
public enum Machine {
    YAGEN("yagen", "yagen", "Yagen", "yagen", 0, 1, 0, 0.5, 15, 13, 15.5),
    HACHOIR("hachoir", "hachoir_a_levier", "Hachoir à levier", "hachoir", 0, 1, 0, 4, 15, 11, 12),
    CHAUDRON("chaudron", "chaudron_sur_brasero", "Chaudron sur brasero", "chaudron", 9, 1, 0, 1.5, 15, 13, 15),
    ALAMBIC("alambic", "alambic_de_cuivre", "Alambic de cuivre", "alambic", 6, 1, 0, 1, 15.5, 13, 12.5),
    JARRES("jarres", "jarres_de_maceration", "Jarres de macération", "jarres", 0, 1, 0, 1.5, 15, 10.5, 14.5),
    BALANCE("balance", "balance_d_apothicaire", "Balance d'apothicaire", "balance", 0, 2, 0, 5, 14, 14.5, 11),
    MORTIER("mortier", "mortier_d_apothicaire", "Mortier et pilon", "pilon", 0, 3, 0, 3, 13, 11, 13),
    PILULIER("pilulier", "pilulier", "Pilulier en bois", "pilulier", 0, 1, 0, 1.2, 15, 3, 14.8),
    TABLE("table", "table_de_preparation", "Table de préparation laquée", "table", 0, 0, 0, 0, 16, 16, 16);

    public final String id, bloc, label, jeu;
    public final int lumiere;
    public final double[] boite;

    Machine(String id, String bloc, String label, String jeu, int lumiere, double... boite) {
        this.id = id;
        this.bloc = bloc;
        this.label = label;
        this.jeu = jeu;
        this.lumiere = lumiere;
        this.boite = boite;
    }

    public static Machine de(String id) {
        for (Machine m : values()) if (m.id.equals(id)) return m;
        return null;
    }

    public static String libelle(String id) {
        Machine m = de(id);
        return m == null ? id : m.label;
    }

    /** Libellé lisible d'un mode de machine (« décoction, feu fort »…). */
    public static String libelleMode(String machine, String mode) {
        if (mode == null || mode.isEmpty()) return "";
        switch (machine + ":" + mode) {
            case "chaudron:douce": return "infusion, feu doux";
            case "chaudron:forte": return "décoction, feu fort";
            case "chaudron:forte_long": return "décoction longue";
            case "chaudron:reduction": return "réduction";
            case "chaudron:sec": return "à sec";
            case "chaudron:fondre": return "fondre";
            case "chaudron:ebullition": return "ébullition";
            case "alambic:eau_florale": return "eau florale";
            case "alambic:distiller": return "distillation";
            case "alambic:concentrer": return "concentrer";
            case "balance:stricte": return "dose stricte";
            case "table:emplatre": return "emplâtre";
            case "table:filtrer": return "filtrer";
            case "table:refroidir": return "refroidir";
            default: return mode;
        }
    }

    /** Paramètre du mini-jeu selon le mode de l'étape. */
    public int parametre(Defs.Etape e) {
        String m = e.mode == null ? "" : e.mode;
        switch (this) {
            case CHAUDRON:
                switch (m) {
                    case "douce": return 0;
                    case "forte": return 1;
                    case "reduction": return 2;
                    case "sec": return 3;
                    case "fondre": return 4;
                    case "ebullition": return 5;
                    case "forte_long": return 6;
                    default: return 1;
                }
            case ALAMBIC:
                return "distiller".equals(m) ? 1 : "concentrer".equals(m) ? 2 : 0;
            case BALANCE:
                return "stricte".equals(m) ? 1 : 0;
            case JARRES:
                return Math.max(1, Math.min(24, e.heures));
            case TABLE:
                switch (m) {
                    case "filtrer": return 1;
                    case "refroidir": return 2;
                    case "emplatre": case "attelle": case "pansement": case "bandage": return 3;
                    default: return 0;
                }
            default:
                return 0;
        }
    }
}
