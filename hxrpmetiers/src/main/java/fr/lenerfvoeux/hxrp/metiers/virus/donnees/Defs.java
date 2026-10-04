package fr.lenerfvoeux.hxrp.metiers.virus.donnees;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Les définitions du Hunter Virus, telles qu'écrites dans config/hxrpmetiers/virus/*.json
 * (générées par tools/virus/donnees.py). Champs publics lus par Gson, noms français du cahier des charges.
 */
public final class Defs {
    private Defs() {}

    // ================================================================== ingrédients
    public static final class Ingredient {
        public String id, nom, categorie, source, rarete;
        /** Durée de vie en heures réelles (0 = ne périme pas). */
        public int vie;
        public Recolte recolte = new Recolte();

        public boolean perissable() { return vie > 0; }
    }

    public static final class Recolte {
        public String type = "";
        public List<String> milieux = Collections.emptyList();
        public String sol = "herbe";
        public String bois = "";
        public String bloc = "";
        public List<String> de = Collections.emptyList();
        public boolean arbuste, champignon, nuit, aube, ombre, tapis, haut, pluie;
        @SerializedName("pres_chene") public boolean presChene;
        public int lumiere, ymin;
    }

    public static final class FichierIngredients {
        @SerializedName("repris_du_gourmet") public Map<String, String> reprisDuGourmet = Collections.emptyMap();
        public List<Ingredient> ingredients = new ArrayList<>();
    }

    // ================================================================== préparations
    public static final class Etape {
        public String machine = "";
        public String mode = "";
        /** Jarres : heures d'attente réelle. */
        public int heures;

        public String libelle() {
            String m = Machine.libelle(machine);
            if ("jarres".equals(machine)) return m + " (" + heures + " h" + ("ouverte".equals(mode) ? ", ouvertes" : "") + ")";
            String md = Machine.libelleMode(machine, mode);
            return md.isEmpty() ? m : m + " (" + md + ")";
        }
    }

    public static final class Preparation {
        public String id, nom, forme, texte, produit, bonus, soin;
        public List<String> ingredients = new ArrayList<>();
        public List<Etape> etapes = new ArrayList<>();
        public int rang, quantite = 1;
        public boolean secret;

        public boolean administrable() {
            switch (forme == null ? "" : forme) {
                case "fiole": case "seringue": case "pilules": case "onguent": case "bandage": case "attelle": case "fumigation": return true;
                default: return false;
            }
        }

        public String produit() { return produit == null || produit.isEmpty() ? id : produit; }
    }

    public static final class FichierPreparations {
        public List<Preparation> preparations = new ArrayList<>();
    }

    // ================================================================== maladies
    public static final class EffetSpec {
        public String code = "";
        /** « episode » ou « continu ». */
        public String mode = "episode";
        /** nuit, jour, eau, y100, combat (vide : toujours). */
        public String condition = "";

        public boolean continu() { return "continu".equals(mode); }
    }

    public static final class Stade {
        public String message = "";
        public List<EffetSpec> effets = new ArrayList<>();
        public List<String> symptomes = new ArrayList<>();
        public boolean vie;
    }

    public static final class Prise {
        public double delai;
        public List<String> items = new ArrayList<>();
        /** soir, nuit, reveil (vide : n'importe quand). */
        public String moment = "";
        @SerializedName("dormir_apres") public boolean dormirApres;
    }

    public static final class Touche {
        public List<String> groupes;
        /** homme ou femme. */
        public String sexe;
    }

    public static final class Maladie {
        public int numero, rang, jours;
        public String id, nom, remede;
        public Touche touche;
        @SerializedName("touche_texte") public String toucheTexte;
        @SerializedName("premier_message") public String premierMessage;
        public Map<String, String> sang = Collections.emptyMap();
        @SerializedName("sang_texte") public String sangTexte;
        public List<Stade> stades = new ArrayList<>();
        public List<String> accompagnements = new ArrayList<>();
        public List<Prise> prises = new ArrayList<>();
        public List<JsonObject> consignes = new ArrayList<>();
        @SerializedName("consigne_texte") public String consigneTexte;
        @SerializedName("traitement_texte") public String traitementTexte;
        public List<String> origine = new ArrayList<>();
        public List<String> contexte = new ArrayList<>();

        public Stade dernierStade() { return stades.get(stades.size() - 1); }

        /** Toutes les préparations du traitement (remède spécifique + accompagnements). */
        public List<String> preparations() {
            List<String> l = new ArrayList<>();
            l.add(remede);
            l.addAll(accompagnements);
            return l;
        }
    }

    public static final class Symptome {
        public String id, nom;
    }

    public static final class FichierMaladies {
        public List<Symptome> symptomes = new ArrayList<>();
        public List<Maladie> maladies = new ArrayList<>();
    }

    // ================================================================== blessures
    public static final class StadeBlessure {
        public String message = "";
        public List<EffetSpec> effets = new ArrayList<>();
    }

    public static final class Blessure {
        public String id, nom, declencheur, soin;
        public List<StadeBlessure> stades = new ArrayList<>();
        @SerializedName("stades_texte") public List<String> stadesTexte = new ArrayList<>();
    }

    public static final class FichierBlessures {
        public List<Blessure> blessures = new ArrayList<>();
    }
}
