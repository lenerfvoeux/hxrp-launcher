package fr.lenerfvoeux.hxrp.metiers.virus.client.jeu;

import java.util.ArrayList;
import java.util.List;

/**
 * Ce que la scène d'un mini-jeu du Virus doit montrer : la préparation (nom, couleur, icônes des ingrédients),
 * l'étape, le patient. Aucune dépendance à Minecraft (les icônes sont fournies par le client).
 */
public final class ContexteVirus {
    public String titre = "", sujet = "", forme = "";
    public int type, etape = 1, etapes = 1, rang, param;
    /** Icônes 32x32 ARGB des ingrédients, dans l'ordre de la recette (sans doublons). */
    public final List<int[]> icones = new ArrayList<>();
    /** Icône de la préparation administrée (fiole, onguent…), ou null. */
    public int[] iconePrep;
    /** Couleur dominante de la préparation (liquide, poudre, onguent) et couleur d'accent. */
    public int couleur = 0xFF8FB860, accent = 0xFFD8B060;

    public int[] icone(int k) {
        return icones.isEmpty() ? null : icones.get(Math.floorMod(k, icones.size()));
    }

    /** Couleur moyenne d'une icône (sans contour ni transparence), 0 si vide. */
    public static int moyenne(int[] ic) {
        if (ic == null) return 0;
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
