package fr.lenerfvoeux.hxrp.metiers.client.minijeu;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Police pixel du Gourmet : capitales 5x7 avec accents français, dessinées directement sur la toile
 * (même « gros pixel » que la scène). Hauteur de ligne 10 px (2 px d'accent au-dessus).
 */
public final class Police {
    public static final int HAUT = 10, AVANCE = 6;
    private static final Map<Character, String[]> G = new HashMap<>();
    private static final Map<Character, Object[]> ACCENTS = new HashMap<>();

    private Police() {}

    private static void g(char c, String... rows) {
        G.put(c, rows);
    }

    static {
        g('A', ".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#");
        g('B', "####.", "#...#", "#...#", "####.", "#...#", "#...#", "####.");
        g('C', ".###.", "#...#", "#....", "#....", "#....", "#...#", ".###.");
        g('D', "####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####.");
        g('E', "#####", "#....", "#....", "####.", "#....", "#....", "#####");
        g('F', "#####", "#....", "#....", "####.", "#....", "#....", "#....");
        g('G', ".###.", "#...#", "#....", "#.###", "#...#", "#...#", ".####");
        g('H', "#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#");
        g('I', "###", ".#.", ".#.", ".#.", ".#.", ".#.", "###");
        g('J', "..###", "....#", "....#", "....#", "#...#", "#...#", ".###.");
        g('K', "#...#", "#..#.", "#.#..", "##...", "#.#..", "#..#.", "#...#");
        g('L', "#....", "#....", "#....", "#....", "#....", "#....", "#####");
        g('M', "#...#", "##.##", "#.#.#", "#.#.#", "#...#", "#...#", "#...#");
        g('N', "#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#", "#...#");
        g('O', ".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###.");
        g('P', "####.", "#...#", "#...#", "####.", "#....", "#....", "#....");
        g('Q', ".###.", "#...#", "#...#", "#...#", "#.#.#", "#..#.", ".##.#");
        g('R', "####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#");
        g('S', ".####", "#....", "#....", ".###.", "....#", "....#", "####.");
        g('T', "#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#..");
        g('U', "#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###.");
        g('V', "#...#", "#...#", "#...#", "#...#", "#...#", ".#.#.", "..#..");
        g('W', "#...#", "#...#", "#...#", "#.#.#", "#.#.#", "##.##", "#...#");
        g('X', "#...#", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", "#...#");
        g('Y', "#...#", "#...#", ".#.#.", "..#..", "..#..", "..#..", "..#..");
        g('Z', "#####", "....#", "...#.", "..#..", ".#...", "#....", "#####");
        g('0', ".###.", "#...#", "#..##", "#.#.#", "##..#", "#...#", ".###.");
        g('1', "..#..", ".##..", "..#..", "..#..", "..#..", "..#..", ".###.");
        g('2', ".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####");
        g('3', "####.", "....#", "....#", ".###.", "....#", "....#", "####.");
        g('4', "...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#.");
        g('5', "#####", "#....", "####.", "....#", "....#", "#...#", ".###.");
        g('6', ".###.", "#....", "#....", "####.", "#...#", "#...#", ".###.");
        g('7', "#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#...");
        g('8', ".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###.");
        g('9', ".###.", "#...#", "#...#", ".####", "....#", "....#", ".###.");
        g(' ', "...", "...", "...", "...", "...", "...", "...");
        g('.', ".", ".", ".", ".", ".", ".", "#");
        g(',', "..", "..", "..", "..", "..", ".#", "#.");
        g(':', ".", ".", "#", ".", ".", "#", ".");
        g(';', "..", "..", ".#", "..", "..", ".#", "#.");
        g('!', "#", "#", "#", "#", "#", ".", "#");
        g('?', ".###.", "#...#", "....#", "...#.", "..#..", ".....", "..#..");
        g('%', "##..#", "##..#", "...#.", "..#..", ".#...", "#..##", "#..##");
        g('/', "....#", "...#.", "...#.", "..#..", ".#...", ".#...", "#....");
        g('-', "....", "....", "....", "####", "....", "....", "....");
        g('+', ".....", "..#..", "..#..", "#####", "..#..", "..#..", ".....");
        g('\'', "#", "#", ".", ".", ".", ".", ".");
        g('(', ".#", "#.", "#.", "#.", "#.", "#.", ".#");
        g(')', "#.", ".#", ".#", ".#", ".#", ".#", "#.");
        g('°', ".#.", "#.#", ".#.", "...", "...", "...", "...");
        g('×', ".....", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", ".....");
        g('★', "..#..", "..#..", "#####", ".###.", ".#.#.", "#...#", ".....");
        g('·', ".", ".", ".", "#", ".", ".", ".");
        g('Œ', ".####", "#.#..", "#.#..", "#.###", "#.#..", "#.#..", ".####");
        // accents : lettre de base + marque au-dessus
        accent('É', 'E', "..#..", ".#...");
        accent('È', 'E', ".#...", "..#..");
        accent('Ê', 'E', "..#..", ".#.#.");
        accent('Ë', 'E', ".#.#.", ".....");
        accent('À', 'A', ".#...", "..#..");
        accent('Â', 'A', "..#..", ".#.#.");
        accent('Î', 'I', ".#.", "#.#");
        accent('Ï', 'I', "#.#", "...");
        accent('Ô', 'O', "..#..", ".#.#.");
        accent('Ù', 'U', ".#...", "..#..");
        accent('Û', 'U', "..#..", ".#.#.");
        accent('Ü', 'U', ".#.#.", ".....");
    }

    private static void accent(char c, char base, String l1, String l2) {
        ACCENTS.put(c, new Object[]{base, new String[]{l1, l2}});
    }

    private static char norm(char c) {
        if (c == 'Ç' || c == 'ç') return 'Ç';
        if (c == 'œ') return 'Œ';
        return Character.toUpperCase(c);
    }

    public static int largeur(String s, int echelle) {
        int w = 0;
        for (char c0 : s.toCharArray()) w += avance(norm(c0)) * echelle;
        return Math.max(0, w - echelle);
    }

    private static int avance(char c) {
        if (c == 'Ç') return AVANCE;
        Object[] ac = ACCENTS.get(c);
        String[] g = G.get(ac != null ? (Character) ac[0] : c);
        if (g == null) g = G.get('?');
        return g[0].length() + 1;
    }

    /** Dessine un texte (x, y = coin haut-gauche de la ligne, accents compris). */
    public static void texte(Toile t, String s, int x, int y, int couleur, int contour, int echelle) {
        s = s.toUpperCase(Locale.ROOT);
        if ((contour >>> 24) != 0) {
            for (int dy = -1; dy <= 1; dy++)
                for (int dx = -1; dx <= 1; dx++)
                    if (dx != 0 || dy != 0) brut(t, s, x + dx, y + dy, contour, echelle);
            brut(t, s, x + echelle, y + echelle, contour, echelle);
        }
        brut(t, s, x, y, couleur, echelle);
    }

    /** Texte centré horizontalement sur cx. */
    public static void centre(Toile t, String s, int cx, int y, int couleur, int contour, int echelle) {
        texte(t, s, cx - largeur(s.toUpperCase(Locale.ROOT), echelle) / 2, y, couleur, contour, echelle);
    }

    /** Texte aligné à droite sur x. */
    public static void droite(Toile t, String s, int x, int y, int couleur, int contour, int echelle) {
        texte(t, s, x - largeur(s.toUpperCase(Locale.ROOT), echelle), y, couleur, contour, echelle);
    }

    private static void brut(Toile t, String s, int x, int y, int c, int e) {
        int cx = x;
        for (char c0 : s.toCharArray()) {
            char ch = norm(c0);
            String[] g;
            String[] haut = null;
            boolean cedille = false;
            if (ch == 'Ç') {
                g = G.get('C');
                cedille = true;
            } else if (ACCENTS.containsKey(ch)) {
                Object[] ac = ACCENTS.get(ch);
                g = G.get((Character) ac[0]);
                haut = (String[]) ac[1];
            } else {
                g = G.get(ch);
                if (g == null) g = G.get('?');
            }
            int gw = g[0].length();
            for (int j = 0; j < 7; j++)
                for (int i = 0; i < gw; i++)
                    if (g[j].charAt(i) == '#') t.rect(cx + i * e, y + (j + 2) * e, e, e, c);
            if (haut != null)
                for (int j = 0; j < 2; j++)
                    for (int i = 0; i < haut[j].length() && i < gw; i++)
                        if (haut[j].charAt(i) == '#') t.rect(cx + i * e, y + j * e, e, e, c);
            if (cedille) {
                t.rect(cx + 2 * e, y + 9 * e, e, e, c);
                t.rect(cx + 1 * e, y + 10 * e, e, e, c);
            }
            cx += (gw + 1) * e;
        }
    }
}
