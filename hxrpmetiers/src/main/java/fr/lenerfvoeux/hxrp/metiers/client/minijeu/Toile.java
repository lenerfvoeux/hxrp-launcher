package fr.lenerfvoeux.hxrp.metiers.client.minijeu;

import java.util.Arrays;

/**
 * Une toile de pixels ARGB, redessinée à chaque image puis envoyée d'un bloc à la carte graphique.
 * Sert aussi de « sprite » : on dessine un objet sur une petite toile, on lui pose un contour, on le colle.
 * Aucune dépendance à Minecraft (testable hors jeu).
 */
public final class Toile {
    public final int w, h;
    public final int[] px;

    public Toile(int w, int h) {
        this(w, h, new int[w * h]);
    }

    public Toile(int w, int h, int[] px) {
        this.w = w;
        this.h = h;
        this.px = px;
    }

    public void vider() {
        Arrays.fill(px, 0);
    }

    public int get(int x, int y) {
        return x < 0 || y < 0 || x >= w || y >= h ? 0 : px[y * w + x];
    }

    /** Pose un pixel ; les couleurs semi-transparentes sont mélangées à ce qui est dessous. */
    public void set(int x, int y, int c) {
        if (x < 0 || y < 0 || x >= w || y >= h) return;
        int a = c >>> 24;
        if (a == 0) return;
        int i = y * w + x;
        px[i] = a == 255 ? c : melangeAlpha(px[i], c);
    }

    /** Remplace le pixel sans mélange (même transparent). */
    public void poser(int x, int y, int c) {
        if (x >= 0 && y >= 0 && x < w && y < h) px[y * w + x] = c;
    }

    public void rect(int x, int y, int rw, int rh, int c) {
        int x0 = Math.max(0, x), y0 = Math.max(0, y), x1 = Math.min(w, x + rw), y1 = Math.min(h, y + rh);
        for (int j = y0; j < y1; j++) for (int i = x0; i < x1; i++) set(i, j, c);
    }

    public void ligneH(int x0, int x1, int y, int c) {
        if (x1 < x0) { int t = x0; x0 = x1; x1 = t; }
        for (int x = x0; x <= x1; x++) set(x, y, c);
    }

    public void ligneV(int x, int y0, int y1, int c) {
        if (y1 < y0) { int t = y0; y0 = y1; y1 = t; }
        for (int y = y0; y <= y1; y++) set(x, y, c);
    }

    public void ellipse(double cx, double cy, double rx, double ry, int c) {
        if (rx <= 0 || ry <= 0) return;
        int y0 = (int) Math.floor(cy - ry), y1 = (int) Math.ceil(cy + ry);
        int x0 = (int) Math.floor(cx - rx), x1 = (int) Math.ceil(cx + rx);
        for (int y = y0; y <= y1; y++)
            for (int x = x0; x <= x1; x++) {
                double dx = (x + 0.5 - cx) / rx, dy = (y + 0.5 - cy) / ry;
                if (dx * dx + dy * dy <= 1) set(x, y, c);
            }
    }

    public void disque(double cx, double cy, double r, int c) {
        ellipse(cx, cy, r, r, c);
    }

    /** Anneau elliptique. */
    public void anneau(double cx, double cy, double rx, double ry, double ep, int c) {
        int y0 = (int) Math.floor(cy - ry), y1 = (int) Math.ceil(cy + ry);
        int x0 = (int) Math.floor(cx - rx), x1 = (int) Math.ceil(cx + rx);
        for (int y = y0; y <= y1; y++)
            for (int x = x0; x <= x1; x++) {
                double dx = (x + 0.5 - cx), dy = (y + 0.5 - cy);
                double e = (dx * dx) / (rx * rx) + (dy * dy) / (ry * ry);
                double ri = Math.max(0.1, rx - ep), rj = Math.max(0.1, ry - ep);
                double ei = (dx * dx) / (ri * ri) + (dy * dy) / (rj * rj);
                if (e <= 1 && ei > 1) set(x, y, c);
            }
    }

    public void ligne(double x0, double y0, double x1, double y1, int c) {
        int n = (int) Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)) * 1.5) + 1;
        for (int k = 0; k <= n; k++) {
            double t = k / (double) n;
            set((int) Math.floor(x0 + (x1 - x0) * t), (int) Math.floor(y0 + (y1 - y0) * t), c);
        }
    }

    public void trait(double x0, double y0, double x1, double y1, double ep, int c) {
        double dx = x1 - x0, dy = y1 - y0, L = Math.hypot(dx, dy);
        if (L < 1e-6) { disque(x0, y0, ep / 2, c); return; }
        int bx0 = (int) Math.floor(Math.min(x0, x1) - ep), bx1 = (int) Math.ceil(Math.max(x0, x1) + ep);
        int by0 = (int) Math.floor(Math.min(y0, y1) - ep), by1 = (int) Math.ceil(Math.max(y0, y1) + ep);
        for (int y = by0; y <= by1; y++)
            for (int x = bx0; x <= bx1; x++) {
                double px_ = x + 0.5, py_ = y + 0.5;
                double t = Math.max(0, Math.min(1, ((px_ - x0) * dx + (py_ - y0) * dy) / (L * L)));
                double d = Math.hypot(px_ - (x0 + t * dx), py_ - (y0 + t * dy));
                if (d <= ep / 2) set(x, y, c);
            }
    }

    /** Polygone plein (règle pair-impair). */
    public void polygone(double[] xs, double[] ys, int c) {
        double miny = ys[0], maxy = ys[0];
        for (double v : ys) { miny = Math.min(miny, v); maxy = Math.max(maxy, v); }
        int n = xs.length;
        double[] inter = new double[n];
        for (int y = (int) Math.floor(miny); y <= (int) Math.ceil(maxy); y++) {
            double yy = y + 0.5;
            int k = 0;
            for (int i = 0; i < n; i++) {
                double ax = xs[i], ay = ys[i], bx = xs[(i + 1) % n], by = ys[(i + 1) % n];
                if ((ay <= yy && yy < by) || (by <= yy && yy < ay)) inter[k++] = ax + (yy - ay) * (bx - ax) / (by - ay);
            }
            Arrays.sort(inter, 0, k);
            for (int j = 0; j + 1 < k; j += 2)
                for (int x = (int) Math.ceil(inter[j] - 0.5); x <= (int) Math.floor(inter[j + 1] - 0.5); x++) set(x, y, c);
        }
    }

    /** Assombrit (ou éclaircit) ce qui est déjà dessiné dans une ellipse : ombres portées, lueurs. */
    public void voile(double cx, double cy, double rx, double ry, int c) {
        ellipse(cx, cy, rx, ry, c);
    }

    // ------------------------------------------------------------------ sprites
    /** Colle une toile (avec transparence). */
    public void coller(Toile s, int x, int y) {
        for (int j = 0; j < s.h; j++) {
            int yy = y + j;
            if (yy < 0 || yy >= h) continue;
            for (int i = 0; i < s.w; i++) {
                int c = s.px[j * s.w + i];
                if ((c >>> 24) != 0) set(x + i, yy, c);
            }
        }
    }

    /** Colle une toile agrandie d'un facteur entier. */
    public void coller(Toile s, int x, int y, int echelle) {
        for (int j = 0; j < s.h; j++)
            for (int i = 0; i < s.w; i++) {
                int c = s.px[j * s.w + i];
                if ((c >>> 24) == 0) continue;
                for (int dy = 0; dy < echelle; dy++) for (int dx = 0; dx < echelle; dx++) set(x + i * echelle + dx, y + j * echelle + dy, c);
            }
    }

    /** Colle une icône teintée : t=0 couleurs d'origine, t=1 entièrement vers la teinte (cru, doré, brûlé…). */
    public void collerTeinte(int[] icone, int iw, int ih, int x, int y, int teinte, double t, boolean miroirV) {
        for (int j = 0; j < ih; j++)
            for (int i = 0; i < iw; i++) {
                int c = icone[(miroirV ? ih - 1 - j : j) * iw + i];
                if ((c >>> 24) == 0) continue;
                if (t > 0) c = (c & 0xFF000000) | (melange(c, teinte, t) & 0xFFFFFF);
                set(x + i, y + j, c);
            }
    }

    /** Pose un contour d'une couleur autour de tout ce qui est opaque (sur cette toile). */
    public void contour(int c) {
        int[] src = px.clone();
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                if ((src[y * w + x] >>> 24) != 0) continue;
                boolean v = (x > 0 && (src[y * w + x - 1] >>> 24) > 128) || (x < w - 1 && (src[y * w + x + 1] >>> 24) > 128)
                        || (y > 0 && (src[(y - 1) * w + x] >>> 24) > 128) || (y < h - 1 && (src[(y + 1) * w + x] >>> 24) > 128);
                if (v) px[y * w + x] = c;
            }
    }

    // ------------------------------------------------------------------ couleurs
    public static int argb(int a, int r, int g, int b) {
        return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | (b & 255);
    }

    public static int alpha(int c, double a) {
        return ((int) Math.round(Math.max(0, Math.min(1, a)) * 255) << 24) | (c & 0xFFFFFF);
    }

    public static int melange(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return 0xFF000000 | ((int) Math.round(ar + (br - ar) * t) << 16) | ((int) Math.round(ag + (bg - ag) * t) << 8) | (int) Math.round(ab + (bb - ab) * t);
    }

    public static int sombre(int c, double f) {
        return melange(c, 0xFF000000, 1 - f);
    }

    public static int clair(int c, double f) {
        return melange(c, 0xFFFFFFFF, f);
    }

    private static int melangeAlpha(int dst, int src) {
        int sa = src >>> 24, da = dst >>> 24;
        if (da == 0) return src;
        double a = sa / 255.0;
        int r = (int) (((src >> 16) & 255) * a + ((dst >> 16) & 255) * (1 - a));
        int g = (int) (((src >> 8) & 255) * a + ((dst >> 8) & 255) * (1 - a));
        int b = (int) ((src & 255) * a + (dst & 255) * (1 - a));
        int oa = Math.min(255, sa + da * (255 - sa) / 255);
        return oa << 24 | r << 16 | g << 8 | b;
    }
}
