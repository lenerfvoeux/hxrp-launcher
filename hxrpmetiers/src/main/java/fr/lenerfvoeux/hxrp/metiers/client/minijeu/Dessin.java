package fr.lenerfvoeux.hxrp.metiers.client.minijeu;

/**
 * Outils de dessin pixel art : rampes de couleurs à décalage de teinte, volumes éclairés
 * (sphère, cylindre, pavé trois-quarts), et les éléments communs à tous les mini-jeux :
 * jauge en laiton, plaques, étoiles, bande de feu, touches et souris.
 * Lumière en haut à gauche, contours sombres marqués.
 */
public final class Dessin {
    public static final int CONTOUR = 0xFF1B100C;
    // laiton
    public static final int OL = 0xFF5C2A08, OR = 0xFFE8871E, HL = 0xFFFFC35C, HL2 = 0xFFF7A63C, SH = 0xFFB25710,
            TR = 0xFFF3D6A2, TRL = 0xFFFBE7C0, SEG = 0xFFDDB577, SEGL = 0xFFFFF1D6,
            GD = 0xFF2C7410, G = 0xFF66D136, GH = 0xFFA9F272, GS = 0xFF48A824,
            RD = 0xFF7A1408, RG = 0xFFE0402A, RH = 0xFFFF8A6A;
    public static final int BLANC = 0xFFFFFFFF, CREME = 0xFFFBF1D8, OR_TEXTE = 0xFFFFD35A, VERT_TEXTE = 0xFF8CF06A, ROUGE_TEXTE = 0xFFFF6A50;

    private static final double LX = -0.55, LY = -0.68, LZ = 0.49;
    private static final double[][] BAYER = {{0, 8, 2, 10}, {12, 4, 14, 6}, {3, 11, 1, 9}, {15, 7, 13, 5}};

    private Dessin() {}

    // ================================================================== couleurs
    public static int hex(String s) {
        if (s == null || s.length() < 7) return 0xFF999999;
        return 0xFF000000 | Integer.parseInt(s.substring(1, 7), 16);
    }

    /** Rampe de 5 couleurs (sombre -> clair), ombres vers le violet, lumières vers le jaune. */
    public static int[] rampe(int base) {
        float[] hsv = rgbToHsv(base);
        int[] out = new int[5];
        for (int i = 0; i < 5; i++) {
            double k = (i - 2) / 2.0;
            double h = hsv[0], s = hsv[1], v = hsv[2];
            if (k < 0) {
                v = v * (1 + k * 0.42);
                if (s > 0.05) s = Math.min(1, s * (1 - k * 0.25));
                if (s > 0.08) h = versTeinte(h, 0.72, -k * 0.10);
            } else if (k > 0) {
                v = Math.min(1, v * (1 + k * 0.2) + k * 0.06);
                s = s * (1 - k * 0.32);
                if (s > 0.08) h = versTeinte(h, 0.15, k * 0.07);
            }
            out[i] = hsvToRgb(h, s, v);
        }
        return out;
    }

    private static double versTeinte(double h, double cible, double t) {
        double d = ((cible - h + 1.5) % 1.0) - 0.5;
        return (h + d * t + 1) % 1.0;
    }

    private static float[] rgbToHsv(int c) {
        float r = ((c >> 16) & 255) / 255f, g = ((c >> 8) & 255) / 255f, b = (c & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        float h = 0;
        if (d > 0) {
            if (max == r) h = ((g - b) / d) % 6;
            else if (max == g) h = (b - r) / d + 2;
            else h = (r - g) / d + 4;
            h /= 6;
            if (h < 0) h += 1;
        }
        return new float[]{h, max == 0 ? 0 : d / max, max};
    }

    private static int hsvToRgb(double h, double s, double v) {
        s = Math.max(0, Math.min(1, s));
        v = Math.max(0, Math.min(1, v));
        double c = v * s, x = c * (1 - Math.abs((h * 6) % 2 - 1)), m = v - c;
        double r, g, b;
        int seg = (int) Math.floor(h * 6) % 6;
        switch (seg) {
            case 0: r = c; g = x; b = 0; break;
            case 1: r = x; g = c; b = 0; break;
            case 2: r = 0; g = c; b = x; break;
            case 3: r = 0; g = x; b = c; break;
            case 4: r = x; g = 0; b = c; break;
            default: r = c; g = 0; b = x; break;
        }
        return 0xFF000000 | (int) Math.round((r + m) * 255) << 16 | (int) Math.round((g + m) * 255) << 8 | (int) Math.round((b + m) * 255);
    }

    static int bande(int[] r, double L, int x, int y) {
        L += (BAYER[y & 3][x & 3] / 16.0 - 0.5) * 0.07;
        int i = L < 0.2 ? 0 : L < 0.42 ? 1 : L < 0.66 ? 2 : L < 0.86 ? 3 : 4;
        return r[i];
    }

    // ================================================================== volumes éclairés
    /** Ellipsoïde éclairé (tomate, oignon, boulette…). */
    public static void sphere(Toile t, double cx, double cy, double rx, double ry, int[] r, int reflets) {
        int x0 = (int) Math.floor(cx - rx), x1 = (int) Math.ceil(cx + rx), y0 = (int) Math.floor(cy - ry), y1 = (int) Math.ceil(cy + ry);
        double best = -1;
        int bx = 0, by = 0;
        for (int y = y0; y <= y1; y++)
            for (int x = x0; x <= x1; x++) {
                double nx = (x + 0.5 - cx) / rx, ny = (y + 0.5 - cy) / ry, d = nx * nx + ny * ny;
                if (d > 1) continue;
                double nz = Math.sqrt(1 - d);
                double L = Math.pow(Math.max(0, (nx * LX + ny * LY + nz * LZ) * 0.5 + 0.5), 1.15);
                t.set(x, y, bande(r, L, x, y));
                if (L > best) { best = L; bx = x; by = y; }
            }
        if (reflets > 0) {
            int hc = Toile.melange(r[4], 0xFFFFFFFF, 0.55);
            if (reflets > 2 && rx > 8) {
                t.ellipse(bx + 0.5, by + 1, Math.max(1.5, rx * 0.16), Math.max(1, ry * 0.1), Toile.alpha(hc, 0.85));
                t.set(bx, by, 0xFFFFFFFF);
                t.set(bx + 1, by, 0xFFFFFFFF);
            } else {
                t.set(bx, by, hc);
                if (reflets > 1) t.set(bx + 1, by, hc);
                if (reflets > 2) t.set(bx, by + 1, hc);
            }
        }
    }

    /** Cylindre couché horizontal (rouleau, carotte, saucisse…), rayon variable de r0 à r1. */
    public static void cylindreH(Toile t, double x0, double x1, double cy, double r0, double r1, int[] r) {
        for (int x = (int) Math.floor(x0); x <= (int) Math.ceil(x1); x++) {
            double k = (x - x0) / Math.max(1, x1 - x0), rr = r0 + (r1 - r0) * k;
            if (rr <= 0) continue;
            for (int y = (int) Math.floor(cy - rr); y <= (int) Math.ceil(cy + rr); y++) {
                double s = (y + 0.5 - cy) / rr;
                if (Math.abs(s) > 1) continue;
                double nz = Math.sqrt(1 - s * s);
                double L = Math.max(0, (s * LY + nz * LZ) * 0.5 + 0.5 + 0.08);
                t.set(x, y, bande(r, L, x, y));
            }
        }
    }

    /** Dégradé vertical dans un rectangle (faces planes éclairées d'en haut). */
    public static void degradeV(Toile t, int x, int y, int w, int h, int[] r, double haut, double bas) {
        for (int j = 0; j < h; j++) {
            double L = haut + (bas - haut) * j / Math.max(1, h - 1);
            for (int i = 0; i < w; i++) t.set(x + i, y + j, bande(r, L, x + i, y + j));
        }
    }

    public static void degradeH(Toile t, int x, int y, int w, int h, int[] r, double gauche, double droite) {
        for (int i = 0; i < w; i++) {
            double L = gauche + (droite - gauche) * i / Math.max(1, w - 1);
            for (int j = 0; j < h; j++) t.set(x + i, y + j, bande(r, L, x + i, y + j));
        }
    }

    /** Ellipse plate éclairée de haut en bas (dessus d'un liquide, d'une assiette…). */
    public static void ellipseV(Toile t, double cx, double cy, double rx, double ry, int[] r, double haut, double bas) {
        for (int y = (int) Math.floor(cy - ry); y <= (int) Math.ceil(cy + ry); y++)
            for (int x = (int) Math.floor(cx - rx); x <= (int) Math.ceil(cx + rx); x++) {
                double dx = (x + 0.5 - cx) / rx, dy = (y + 0.5 - cy) / ry;
                if (dx * dx + dy * dy > 1) continue;
                double L = haut + (bas - haut) * (dy + 1) / 2;
                t.set(x, y, bande(r, L, x, y));
            }
    }

    /** Ombre portée douce posée sur le monde (semi-transparente). */
    public static void ombre(Toile t, double cx, double cy, double rx, double ry) {
        t.ellipse(cx, cy, rx, ry, 0x55000000);
        t.ellipse(cx, cy, rx * 0.8, ry * 0.75, 0x22000000);
    }

    // ================================================================== éléments communs
    /** Jauge verticale en laiton : cadre, piste crème segmentée, zones verte et rouge, trait cible, flèche. */
    public static void jauge(Toile t, int X, int Y, int L, double v, double lo, double hi, double rouge, double trait, boolean clignote) {
        int w = 16;
        t.rect(X + 2, Y - 5, w - 4, 5, OL);
        t.rect(X + 3, Y - 4, w - 6, 4, OR);
        t.rect(X + 3, Y - 4, w - 6, 1, HL);
        t.rect(X + 2, Y + L, w - 4, 5, OL);
        t.rect(X + 3, Y + L, w - 6, 4, OR);
        t.rect(X + 3, Y + L, w - 6, 1, HL);
        t.rect(X, Y, w, L, OL);
        t.rect(X + 1, Y + 1, w - 2, L - 2, OR);
        t.rect(X + 1, Y + 1, 1, L - 2, HL);
        t.rect(X + 2, Y + 1, 1, L - 2, HL2);
        t.rect(X + w - 2, Y + 1, 1, L - 2, SH);
        t.rect(X + 2, Y + 2, w - 4, L - 4, OL);
        t.rect(X + 3, Y + 3, w - 6, L - 6, TR);
        t.rect(X + 3, Y + 3, 1, L - 6, TRL);
        t.rect(X + w - 4, Y + 3, 1, L - 6, 0xFFE4C38E);
        for (int s = Y + 13; s < Y + L - 3; s += 10) {
            t.rect(X + 3, s, w - 6, 1, SEG);
            t.rect(X + 3, s + 1, w - 6, 1, SEGL);
        }
        int bas = Y + L - 3, H = L - 6;
        if (rouge <= 1) {
            int a = Y + 3, b = (int) (bas - Math.max(0, rouge) * H);
            if (b > a) {
                t.rect(X + 2, a, w - 4, b - a + 1, RD);
                t.rect(X + 3, a, w - 6, b - a, RG);
                t.rect(X + 3, a, w - 6, 1, RH);
            }
        }
        int a = (int) (bas - Math.min(1, hi) * H), b = (int) (bas - Math.max(0, lo) * H);
        if (b - a < 2) { a -= 1; b += 1; }
        int g = clignote ? GH : G;
        t.rect(X + 2, a - 1, w - 4, b - a + 2, GD);
        t.rect(X + 3, a, w - 6, b - a, g);
        t.rect(X + 3, a, w - 6, Math.min(2, b - a), GH);
        t.rect(X + 3, b - 2, w - 6, 2, GS);
        if (trait >= 0) {
            int ty = (int) (bas - trait * H);
            t.rect(X - 2, ty, w + 4, 1, 0xFF2A1508);
            t.rect(X - 3, ty - 1, 2, 3, 0xFF2A1508);
            t.rect(X + w + 1, ty - 1, 2, 3, 0xFF2A1508);
        }
        // rail à rivets
        t.rect(X + w + 3, Y + 7, 5, L - 14, OL);
        t.rect(X + w + 4, Y + 8, 3, L - 16, OR);
        t.rect(X + w + 4, Y + 8, 1, L - 16, HL);
        for (int s = Y + 16; s < Y + L - 12; s += 24) {
            t.rect(X + w + 4, s, 2, 2, OL);
            t.set(X + w + 4, s, 0xFFFFE2A6);
        }
        // flèche en laiton
        int fy = (int) (bas - Math.max(0, Math.min(1, v)) * H);
        for (int k = 0; k < 5; k++) {
            t.rect(X + w + 1 + k, fy - k - 1, 1, 2 * k + 3, OL);
            t.rect(X + w + 1 + k, fy - k, 1, 2 * k + 1, k == 0 ? HL : OR);
        }
        t.rect(X + w + 6, fy - 2, 7, 5, OL);
        t.rect(X + w + 6, fy - 1, 6, 3, OR);
        t.rect(X + w + 6, fy - 1, 6, 1, HL);
        t.rect(X + w + 6, fy + 1, 6, 1, SH);
    }

    /** Plaque en bois cerclée de laiton (consignes, résultat). */
    public static void plaque(Toile t, int cx, int y, int w, int h) {
        int x = cx - w / 2;
        t.rect(x + 3, y + 4, w, h, 0x55000000);
        t.rect(x - 1, y - 1, w + 2, h + 2, OL);
        t.rect(x, y, w, h, OR);
        t.rect(x, y, w, 1, HL);
        t.rect(x, y, 1, h, HL);
        t.rect(x, y + h - 1, w, 1, SH);
        t.rect(x + w - 1, y, 1, h, SH);
        t.rect(x + 2, y + 2, w - 4, h - 4, OL);
        t.rect(x + 3, y + 3, w - 6, h - 6, 0xFF8A5530);
        for (int j = y + 6; j < y + h - 4; j += 5) t.rect(x + 4, j, w - 8, 1, 0xFF7E4B28);
        t.rect(x + 3, y + 3, w - 6, 1, 0xFF9C6438);
        int[][] riv = {{x + 5, y + 5}, {x + w - 6, y + 5}, {x + 5, y + h - 6}, {x + w - 6, y + h - 6}};
        for (int[] p : riv) {
            t.set(p[0], p[1], 0xFFFFD08A);
            t.set(p[0] + 1, p[1] + 1, 0xFF7A4A1A);
        }
    }

    private static final String[] ETOILE = {"...#...", "..###..", "#######", ".#####.", "..###..", ".##.##.", "##...##"};

    public static void etoile(Toile t, int x, int y, boolean pleine) {
        for (int j = 0; j < 7; j++)
            for (int i = 0; i < 7; i++)
                if (ETOILE[j].charAt(i) == '#') t.set(x + i, y + j, pleine ? (j < 2 ? 0xFFFFF3A0 : 0xFFFFC93A) : 0xFF4E2E16);
        for (int j = 0; j < 7; j++)
            for (int i = 0; i < 7; i++)
                if (ETOILE[j].charAt(i) == '#') {
                    if (i + 1 < 7 && ETOILE[j].charAt(i + 1) != '#' || i == 6) t.set(x + i + 1, y + j, pleine ? 0xFF8A4A08 : 0xFF2A1508);
                }
    }

    /** Petite horloge (HUD). */
    public static void horloge(Toile t, int x, int y) {
        t.disque(x + 3.5, y + 3.5, 4, CONTOUR);
        t.disque(x + 3.5, y + 3.5, 3, CREME);
        t.rect(x + 3, y + 1, 1, 3, CONTOUR);
        t.rect(x + 3, y + 3, 2, 1, CONTOUR);
    }

    /** Touche de clavier en relief avec sa lettre. */
    public static void touche(Toile t, int x, int y, String lettre, boolean enfoncee) {
        int d = enfoncee ? 1 : 0;
        t.rect(x, y + 2, 14, 13, CONTOUR);
        t.rect(x + 1, y + 3 + d, 12, 11 - d, 0xFF8E8A82);
        t.rect(x, y + d, 14, 13, CONTOUR);
        t.rect(x + 1, y + 1 + d, 12, 11, 0xFFE8E2D4);
        t.rect(x + 1, y + 1 + d, 12, 1, BLANC);
        t.rect(x + 1, y + 10 + d, 12, 2, 0xFFC8C0B0);
        Police.centre(t, lettre, x + 7, y + 1 + d, 0xFF2A2420, 0, 1);
    }

    /** Souris avec le bouton gauche ou droit mis en évidence. */
    public static void souris(Toile t, int x, int y, int bouton, boolean actif) {
        t.ellipse(x + 6, y + 9, 7, 9, CONTOUR);
        t.ellipse(x + 6, y + 9, 6, 8, 0xFFE8E2D4);
        t.rect(x + 6, y + 1, 1, 8, CONTOUR);
        t.rect(x, y + 8, 13, 1, CONTOUR);
        int c = actif ? 0xFFFFC35C : 0xFFB8B0A0;
        if (bouton == 0) for (int j = 2; j < 8; j++) t.rect(x + 1, y + j, 5, 1, c);
        if (bouton == 1) for (int j = 2; j < 8; j++) t.rect(x + 7, y + j, 5, 1, c);
        if (bouton == 2) t.rect(x + 5, y + 3, 3, 4, c);
    }

    /** Flammes de brûleur, animées, de taille 0-1. */
    public static void flammes(Toile t, double cx, double cy, double largeur, double taille, int temps, boolean gaz) {
        if (taille <= 0.01) return;
        int n = (int) Math.max(3, largeur / 4);
        for (int i = 0; i < n; i++) {
            double x = cx - largeur / 2 + largeur * (i + 0.5) / n;
            double h = taille * (7 + 3 * Math.sin(temps / 90.0 + i * 1.7));
            int c1 = gaz ? 0xFF2A5AE8 : 0xFFE8501A, c2 = gaz ? 0xFF6AA0FF : 0xFFF8A030, c3 = gaz ? 0xFFCFE4FF : 0xFFFFF0A0;
            t.polygone(new double[]{x - 2, x, x + 2}, new double[]{cy, cy - h, cy}, c1);
            t.polygone(new double[]{x - 1.2, x, x + 1.2}, new double[]{cy, cy - h * 0.65, cy}, c2);
            t.polygone(new double[]{x - 0.6, x, x + 0.6}, new double[]{cy, cy - h * 0.3, cy}, c3);
        }
    }

    /**
     * Bande de réglage du feu (crans cliquables), en laiton, avec le cran courant allumé.
     * libelles : texte sous chaque cran (peut être null).
     */
    public static void bandeFeu(Toile t, int n, int cran, int ideal, String[] libelles, int x0, int x1, int y) {
        t.rect(x0 - 10, y - 8, x1 - x0 + 20, 16, 0x66000000);
        t.rect(x0 - 8, y - 2, x1 - x0 + 16, 5, OL);
        t.rect(x0 - 7, y - 1, x1 - x0 + 14, 3, 0xFF2A1508);
        int fx = x0 + (x1 - x0) * cran / (n - 1);
        for (int x = x0 - 6; x < fx; x++) {
            double k = (x - x0 + 6) / (double) (x1 - x0 + 12);
            t.rect(x, y - 1, 1, 3, Toile.melange(0xFFE0641E, 0xFFFFB03A, k));
        }
        for (int i = 0; i < n; i++) {
            int x = x0 + (x1 - x0) * i / (n - 1);
            boolean on = i == cran;
            t.disque(x + 0.5, y + 0.5, on ? 6 : 4.5, OL);
            t.disque(x + 0.5, y + 0.5, on ? 5 : 3.5, on ? HL : (i < cran ? OR : 0xFF8A5530));
            if (on) {
                t.disque(x - 0.5, y - 0.5, 2, 0xFFFFF0C0);
                t.rect(x, y - 5, 1, 4, OL);
            }
            if (i == ideal) {
                t.polygone(new double[]{x - 4, x + 5, x + 0.5}, new double[]{y - 13, y - 13, y - 8}, Dessin.CONTOUR);
                t.polygone(new double[]{x - 3, x + 4, x + 0.5}, new double[]{y - 12, y - 12, y - 8.5}, 0xFF66D136);
            }
            if (libelles != null && i < libelles.length) Police.centre(t, libelles[i], x + 1, y + 7, CREME, CONTOUR, 1);
        }
    }

    /** Retour flottant « PARFAIT / BIEN / RATÉ », qui monte et s'efface. */
    public static void retour(Toile t, String texte, int qualite, int x, int y, double age, boolean grand) {
        if (age < 0 || age > 1) return;
        int c = qualite == 2 ? OR_TEXTE : qualite == 1 ? VERT_TEXTE : qualite == 0 ? ROUGE_TEXTE : CREME;
        int dy = (int) (-age * 14);
        int ech = grand && qualite != 3 && texte.length() <= 10 ? 2 : 1;
        if (age > 0.75) c = Toile.alpha(c, 1 - (age - 0.75) / 0.25);
        int ct = age > 0.75 ? Toile.alpha(CONTOUR, 1 - (age - 0.75) / 0.25) : CONTOUR;
        Police.centre(t, texte, x, y + dy, c, ct, ech);
    }
}
