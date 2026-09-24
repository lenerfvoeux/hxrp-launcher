package fr.lenerfvoeux.hxrp.metiers.client;

/**
 * Le décor des mini-jeux, dessiné pixel par pixel comme dans les prototypes :
 * plan de travail en planches, farine, plaque de cuisson, jauge en laiton, plaque de consigne.
 */
public final class Decor {
    public static final int OL = 0xFF5C2A08, OR = 0xFFE8871E, HL = 0xFFFFC35C, HL2 = 0xFFF7A63C, SH = 0xFFB25710,
            TR = 0xFFF3D6A2, TRL = 0xFFFBE7C0, SEG = 0xFFDDB577, SEGL = 0xFFFFF1D6,
            GD = 0xFF2C7410, G = 0xFF66D136, GH = 0xFFA9F272, GS = 0xFF48A824;

    private static long sd = 11;

    private static double rnd() { sd = (sd * 16807) % 2147483647L; return sd / 2147483647.0; }

    private Decor() {}

    /** Plan de travail : planches, veinures, nœuds, clous. Dessiné une fois puis mis en cache. */
    public static int[] bois(int w, int h, boolean farine, int cx, int cy) {
        sd = 11;
        Toile t = new Toile("hxrp_tmp_" + (farine ? "f" : "b"), w, h);
        int[] base = {0xFF7A4A26, 0xFF734524, 0xFF7F4E29, 0xFF76472A};
        for (int p = 0; p * 18 < h; p++) {
            int y = p * 18;
            t.rect(0, y, w, 18, base[p % 4]);
            t.rect(0, y, w, 1, 0xFF8E5A30);
            t.rect(0, y + 15, w, 1, 0xFF5E3819);
            t.rect(0, y + 16, w, 2, 0xFF3E2210);
            for (int g = 0; g < 26; g++) t.rect((int) (rnd() * w), (int) (y + 2 + rnd() * 12), (int) (4 + rnd() * 12), 1, 0xFF663D1F);
            for (int g = 0; g < 10; g++) t.rect((int) (rnd() * w), (int) (y + 2 + rnd() * 12), (int) (3 + rnd() * 6), 1, 0xFF86542D);
            int sx = (p * 83) % 120;
            while (sx < w) {
                t.rect(sx, y + 1, 2, 15, 0xFF3E2210);
                t.rect(sx + 2, y + 1, 1, 15, 0xFF8E5A30);
                t.set(sx + 5, y + 3, 0xFF2A1508);
                t.set(sx + 4, y + 2, 0xFFA67445);
                t.set(sx + 5, y + 12, 0xFF2A1508);
                t.set(sx + 4, y + 11, 0xFFA67445);
                sx += 110 + (int) (rnd() * 70);
            }
            if (rnd() < 0.7) {
                int kx = (int) (rnd() * (w - 6)), ky = (int) (y + 5 + rnd() * 5);
                t.rect(kx, ky, 4, 3, 0xFF4A2A14);
                t.rect(kx - 1, ky + 1, 6, 1, 0xFF5E3819);
                t.rect(kx + 1, ky + 1, 2, 1, 0xFF2A1508);
            }
        }
        if (farine) for (int i = 0; i < 260; i++) {
            double a = rnd() * 6.283, r = Math.sqrt(rnd());
            t.set((int) (cx + Math.cos(a) * r * 110), (int) (cy + Math.sin(a) * r * 70), rnd() < 0.3 ? 0xFFF4EDDD : 0xFFD9C7A2);
        }
        return t.px.clone();
    }

    /** Plaque de cuisson en acier brossé. */
    public static int[] acier(int w, int h) {
        sd = 29;
        Toile t = new Toile("hxrp_tmp_a", w, h);
        t.rect(0, 0, w, h, 0xFF34373C);
        for (int i = 0; i < 620; i++) t.rect((int) (rnd() * w), (int) (rnd() * h), (int) (6 + rnd() * 16), 1, rnd() < 0.5 ? 0xFF3B3E44 : 0xFF2D3035);
        for (int s = 0; s < w; s += 107) { t.rect(s, 0, 1, h, 0xFF1E2024); t.rect(s + 1, 0, 1, h, 0xFF4A4E55); }
        return t.px.clone();
    }

    /** Jauge verticale en laiton : cadre, piste segmentée, rail à rivets, zone verte, flèche. */
    public static void jauge(Toile t, int X, int Y, int L, double v, double lo, double hi) {
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
        for (int s = Y + 13; s < Y + L - 3; s += 10) { t.rect(X + 3, s, w - 6, 1, SEG); t.rect(X + 3, s + 1, w - 6, 1, SEGL); }
        int bas = Y + L - 3;
        int a = (int) (bas - Math.min(1, hi) * (L - 6)), b = (int) (bas - Math.max(0, lo) * (L - 6));
        t.rect(X + 2, a - 1, w - 4, b - a + 2, GD);
        t.rect(X + 3, a, w - 6, b - a, G);
        t.rect(X + 3, a, w - 6, 2, GH);
        t.rect(X + 3, b - 2, w - 6, 2, GS);
        t.rect(X + w + 3, Y + 7, 5, L - 14, OL);
        t.rect(X + w + 4, Y + 8, 3, L - 16, OR);
        t.rect(X + w + 4, Y + 8, 1, L - 16, HL);
        for (int s = Y + 16; s < Y + L - 12; s += 24) { t.rect(X + w + 4, s, 2, 2, OL); t.set(X + w + 4, s, 0xFFFFE2A6); }
        int fy = (int) (bas - Math.max(0, Math.min(1, v)) * (L - 6));
        for (int k = 0; k < 5; k++) {
            t.rect(X + w + 1 + k, fy - k - 1, 1, 2 * k + 3, OL);
            t.rect(X + w + 1 + k, fy - k, 1, 2 * k + 1, k == 0 ? HL : OR);
        }
        t.rect(X + w + 6, fy - 2, 7, 5, OL);
        t.rect(X + w + 6, fy - 1, 6, 3, OR);
        t.rect(X + w + 6, fy - 1, 6, 1, HL);
        t.rect(X + w + 6, fy + 1, 6, 1, SH);
    }

    /** Plaque en bois cerclée de laiton, pour les consignes et le résultat. */
    public static void plaque(Toile t, int cx, int cy, int w, int h) {
        int x = cx - w / 2;
        t.rect(x - 1, cy - 1, w + 2, h + 2, OL);
        t.rect(x, cy, w, h, OR);
        t.rect(x, cy, w, 1, HL);
        t.rect(x, cy, 1, h, HL);
        t.rect(x, cy + h - 1, w, 1, SH);
        t.rect(x + w - 1, cy, 1, h, SH);
        t.rect(x + 2, cy + 2, w - 4, h - 4, OL);
        t.rect(x + 3, cy + 3, w - 6, h - 6, 0xFF8A5530);
        t.rect(x + 3, cy + 3, w - 6, 1, 0xFF9C6438);
        t.set(x + 4, cy + 4, 0xFFFFD08A);
        t.set(x + w - 5, cy + 4, 0xFFFFD08A);
        t.set(x + 4, cy + h - 5, 0xFFFFD08A);
        t.set(x + w - 5, cy + h - 5, 0xFFFFD08A);
    }

    private static final String[] ETOILE = {"...#...", "..###..", "#######", ".#####.", "..###..", ".##.##.", "##...##"};

    public static void etoile(Toile t, int x, int y, boolean pleine) {
        for (int j = 0; j < 7; j++)
            for (int i = 0; i < 7; i++)
                if (ETOILE[j].charAt(i) == '#') t.set(x + i, y + j, pleine ? (j < 2 ? 0xFFFFF3A0 : 0xFFFFC93A) : 0xFF4E2E16);
    }

    /** Planche à découper avec son ombre portée, son grain et son trou. */
    public static void planche(Toile t, int x, int y, int w, int h) {
        for (int j = y + 6; j < y + h + 6; j++) for (int i = x + 5; i < x + w + 5; i++) t.ombre(i, j, 0.62);
        t.rect(x, y, w, h, 0xFF4A2A12);
        t.rect(x + 1, y + 1, w - 2, h - 2, 0xFFD9A566);
        t.rect(x + 1, y + 1, w - 2, 1, 0xFFEBC089);
        t.rect(x + 1, y + 1, 1, h - 2, 0xFFEBC089);
        t.rect(x + 1, y + h - 3, w - 2, 2, 0xFFB07A42);
        t.rect(x + w - 3, y + 1, 2, h - 2, 0xFFB07A42);
        sd = 47;
        for (int g = 0; g < 46; g++) t.rect((int) (x + 3 + rnd() * (w - 12)), (int) (y + 3 + rnd() * (h - 6)), (int) (3 + rnd() * 10), 1, 0xFFC48E50);
        t.disc(x + w - 15, y + h / 2, 5, 0xFF4A2A12);
        t.disc(x + w - 15, y + h / 2, 4, 0xFF7A4A26);
        t.set(x + w - 18, y + h / 2 - 3, 0xFF8E5A30);
    }

    /** Carotte vue du dessus : corps dégradé, stries, fanes. */
    public static void carotte(Toile t, int x, int yc, int len) {
        for (int i = 0; i < len; i++) {
            int X = x + i, hh = Math.max(2, (int) Math.round(8 - i * 0.065));
            t.rect(X, yc - hh - 1, 1, 2 * hh + 3, 0xFF5C2A08);
            t.rect(X, yc - hh, 1, 2 * hh + 1, 0xFFE8781E);
            t.rect(X, yc - hh, 1, 2, 0xFFFFA24A);
            t.rect(X, yc + hh - 1, 1, 2, 0xFFB45510);
            if (i % 9 == 4) t.rect(X, yc - hh + 3, 1, 2, 0xFFC8601A);
        }
        t.rect(x - 12, yc - 10, 12, 5, 0xFF2C7410);
        t.rect(x - 10, yc - 9, 10, 3, 0xFF48A824);
        t.rect(x - 14, yc - 2, 14, 4, 0xFF2C7410);
        t.rect(x - 12, yc - 1, 10, 2, 0xFF66D136);
        t.rect(x - 12, yc + 5, 12, 5, 0xFF2C7410);
        t.rect(x - 10, yc + 6, 10, 3, 0xFF48A824);
    }

    /** Couteau de chef, lame vers le bas, pointe au curseur. */
    public static void couteau(Toile t, int x, int y) {
        for (int j = y - 26; j < y + 4; j++) for (int i = x + 3; i < x + 10; i++) t.ombre(i, j, 0.7);
        t.rect(x - 3, y - 47, 7, 17, 0xFF2A1508);
        t.rect(x - 2, y - 46, 5, 15, 0xFF5E3819);
        t.rect(x - 2, y - 46, 1, 15, 0xFF8A5530);
        t.set(x, y - 43, 0xFFD8DDE2);
        t.set(x, y - 36, 0xFFD8DDE2);
        t.rect(x - 3, y - 30, 7, 30, 0xFF3A3D42);
        t.rect(x - 2, y - 29, 5, 28, 0xFFD8DDE2);
        t.rect(x - 2, y - 29, 1, 28, 0xFFFFFFFF);
        t.rect(x + 2, y - 29, 1, 28, 0xFF9AA0A8);
    }
}
