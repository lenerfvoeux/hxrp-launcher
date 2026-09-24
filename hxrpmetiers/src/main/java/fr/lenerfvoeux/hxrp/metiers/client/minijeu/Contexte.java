package fr.lenerfvoeux.hxrp.metiers.client.minijeu;

import fr.lenerfvoeux.hxrp.metiers.data.FoodEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Ce que la scène doit montrer pour une étape donnée : l'aliment à couper (famille de découpe et palette),
 * le fruit à presser, la pièce dans la poêle, le plat au four, les condiments à doser…
 * Tout est déduit de la recette ; aucune dépendance à Minecraft (les icônes sont fournies par une fonction).
 */
public final class Contexte {
    public String recette = "", geste = "";
    public int etape = 1, etapes = 1, rang;

    /** Aliment principal de l'étape (découpe, presse, mortier…). */
    public String sujet = "";
    public String famille = "Rond";
    public int peau = 0xFFE0321F, chair = 0xFFF05A40, accent = 0xFFF6E8A0, motif = 3;

    /** Icônes 32x32 ARGB : sujet (poêle), plat fini (four, friteuse), pièces du grill. */
    public int[] icone, iconePlat;
    public final List<int[]> iconesGrill = new ArrayList<>();

    /** Couleur de la préparation (bol, marmite, shaker) et petits morceaux qui y flottent. */
    public int couleurMelange = 0xFFF0DCA8;
    public int[] couleursBouts = {0xFFF07A1A, 0xFF3A9A2A};
    /** Condiments à doser (ids). */
    public final List<String> condiments = new ArrayList<>();

    private static final String[] FAMILLES = {"Rond", "Long", "Pavé", "Poisson", "Carapace", "Herbes", "Noyau"};

    /**
     * Construit le contexte d'une étape à partir de la recette.
     *
     * @param db     recherche d'une entrée par id
     * @param icones chargement d'une icône 32x32 (null si absente)
     */
    public static Contexte construire(FoodEntry r, String geste, int etape, int rang, Function<String, FoodEntry> db, Function<String, int[]> icones) {
        Contexte c = new Contexte();
        c.geste = geste;
        c.etape = etape;
        c.rang = rang;
        if (r == null) return c;
        c.recette = r.name;
        c.etapes = Math.max(1, r.steps.size());
        List<FoodEntry> ing = new ArrayList<>();
        for (String id : r.ingredients) {
            FoodEntry e = db.apply(id);
            if (e != null) ing.add(e);
        }
        String g = fr.lenerfvoeux.hxrp.metiers.minijeu.Jeux.cle(geste);
        FoodEntry sujet = choisirSujet(g, ing);
        if (sujet != null) c.appliquer(sujet);
        c.iconePlat = icones.apply(r.id);
        c.icone = sujet != null ? icones.apply(sujet.id) : c.iconePlat;
        if ("saisir".equals(g) && sujet == null) c.icone = c.iconePlat;
        // grill : jusqu'à 3 ingrédients différents, sinon le plat
        for (FoodEntry e : ing)
            if (proteine(e) || "Légumes".equals(e.cat)) {
                int[] ic = icones.apply(e.id);
                if (ic != null && c.iconesGrill.size() < 3) c.iconesGrill.add(ic);
            }
        if (c.iconesGrill.isEmpty() && c.iconePlat != null) c.iconesGrill.add(c.iconePlat);
        // couleur de la préparation : moyenne de l'icône du plat, relevée
        int m = c.iconePlat != null ? moyenne(c.iconePlat) : 0;
        if (m != 0) c.couleurMelange = m;
        List<Integer> bouts = new ArrayList<>();
        for (FoodEntry e : ing) if (e.pal != null && e.pal.length > 0 && bouts.size() < 4) bouts.add(Dessin.hex(e.pal[0]));
        if (!bouts.isEmpty()) {
            c.couleursBouts = new int[bouts.size()];
            for (int i = 0; i < bouts.size(); i++) c.couleursBouts[i] = bouts.get(i);
        }
        for (FoodEntry e : ing) if ("epice".equals(e.kind) || condimentPrep(e.id)) c.condiments.add(e.id);
        if (c.condiments.isEmpty()) {
            c.condiments.add("sel");
            c.condiments.add("poivre_noir");
            c.condiments.add("huile_d_olive");
        }
        return c;
    }

    private void appliquer(FoodEntry e) {
        sujet = e.id;
        if (e.pal != null && e.pal.length >= 3) {
            peau = Dessin.hex(e.pal[0]);
            chair = Dessin.hex(e.pal[1]);
            accent = Dessin.hex(e.pal[2]);
        }
        motif = e.motif;
        famille = "Rond";
        if (e.famille != null) for (String f : FAMILLES) if (f.equals(e.famille)) famille = f;
    }

    private static boolean proteine(FoodEntry e) {
        return "Viandes".equals(e.cat) || "Poissons et fruits de mer".equals(e.cat);
    }

    private static boolean condimentPrep(String id) {
        switch (id) {
            case "vinaigrette": case "mayonnaise": case "ketchup": case "moutarde": case "sauce_tomate": case "pesto":
            case "sauce_barbecue": case "miel": case "sucre":
                return true;
            default:
                return false;
        }
    }

    private static FoodEntry choisirSujet(String geste, List<FoodEntry> ing) {
        switch (geste) {
            case "couper": {
                for (FoodEntry e : ing) if ("ingredient".equals(e.kind) && e.famille != null && !"Herbes".equals(e.famille) && e.pal != null) return e;
                for (FoodEntry e : ing) if (e.famille != null && e.pal != null) return e;
                return ing.isEmpty() ? null : ing.get(0);
            }
            case "presser": {
                for (FoodEntry e : ing) if ("Fruits".equals(e.cat)) return e;
                return ing.isEmpty() ? null : ing.get(0);
            }
            case "saisir": case "griller": {
                for (FoodEntry e : ing) if (proteine(e)) return e;
                for (FoodEntry e : ing) if ("oeuf".equals(e.id)) return e;
                return null;
            }
            case "piler": {
                for (FoodEntry e : ing)
                    if ("epice".equals(e.kind) || "Fruits secs et légumineuses".equals(e.cat) || "Bases de boissons".equals(e.cat) || "Céréales".equals(e.cat))
                        return e;
                return ing.isEmpty() ? null : ing.get(0);
            }
            default:
                return ing.isEmpty() ? null : ing.get(0);
        }
    }

    /** Couleur moyenne d'une icône (sans contour ni transparence). */
    public static int moyenne(int[] ic) {
        long r = 0, g = 0, b = 0, n = 0;
        for (int c : ic) {
            if ((c >>> 24) < 255) continue;
            int rr = (c >> 16) & 255, gg = (c >> 8) & 255, bb = c & 255;
            if (rr + gg + bb < 120) continue;
            r += rr;
            g += gg;
            b += bb;
            n++;
        }
        if (n == 0) return 0;
        return 0xFF000000 | (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
    }
}
