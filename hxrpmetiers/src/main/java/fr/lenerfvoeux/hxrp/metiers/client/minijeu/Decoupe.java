package fr.lenerfvoeux.hxrp.metiers.client.minijeu;

import fr.lenerfvoeux.hxrp.metiers.minijeu.Jeux;
import fr.lenerfvoeux.hxrp.metiers.minijeu.MiniJeu;

/**
 * COUPER, le mini-jeu de référence : l'aliment posé sur une planche épaisse, un gros couteau de chef
 * en diagonale qui descend franchement à chaque coupe et remonte ; à chaque coupe une tranche tombe
 * sur la planche, l'aliment raccourcit et montre sa face coupée.
 * L'aliment est dessiné selon sa famille de découpe (rond, long, pavé, poisson, carapace, herbes, noyau)
 * avec la palette de l'ingrédient.
 */
final class Decoupe {
    static final int PX = 66, PY = 62, PW = 206, PH = 64, EP = 10;
    /** Sprite de l'aliment : origine (SX, SY) dans la scène. */
    static final int SX = 88, SY = 58, SW = 170, SH = 62;
    private static Toile couteau;

    private Decoupe() {}

    static void couper(Toile t, Jeux.Couper j, Contexte c, double tms) {
        Scenes.planche(t, PX, PY, PW, PH, EP, false, 7);
        Toile s = new Toile(SW, SH);
        aliment(s, c);
        int[] bord = bords(s);
        int x0 = bord[0], x1 = bord[1];
        int n = Math.min(5, j.fait);
        double pas = (x1 - x0) * ("Carapace".equals(c.famille) ? 0.2 : 0.105);
        int[] cx = new int[5];
        for (int k = 0; k < 5; k++) cx[k] = (int) Math.round(x1 - (k + 1) * pas);
        if ("Carapace".equals(c.famille)) carapaces(s, c, n);
        else if ("Herbes".equals(c.famille)) {
            if (n > 0) effacerDroite(s, cx[n - 1]);
        } else if (n > 0 && "Pavé".equals(c.famille)) {
            coupePave(s, c, cx[n - 1] - 10);
        } else if (n > 0) {
            effacerDroite(s, cx[n - 1]);
            faceCoupee(s, c, cx[n - 1]);
        }
        s.contour(Dessin.CONTOUR);
        // ombre sous l'aliment
        int yc = SY + SH - 12;
        t.ellipse(SX + (x0 + (n > 0 ? cx[n - 1] : x1)) / 2.0, yc + 2, ((n > 0 ? cx[n - 1] : x1) - x0) / 2.0 + 4, 5, 0x44000000);
        t.coller(s, SX, SY);
        // tranches tombées
        for (int k = 0; k < n; k++) {
            double age = (tms - j.tCoupe[k]) / 260.0;
            tranche(t, c, k, j.qualite[k], age, s.h, cx[k]);
        }
        // pointillés de la prochaine coupe
        if (n < 5 && !"Carapace".equals(c.famille)) {
            int x = SX + cx[n];
            for (int y = SY + 4; y < SY + SH - 6; y += 4) t.rect(x, y, 1, 2, 0xCCFFFFFF);
        } else if (n < 5) {
            int x = SX + cx[n] + (int) (pas / 2);
            for (int y = SY + 10; y < SY + SH - 10; y += 4) t.rect(x, y, 1, 2, 0xCCFFFFFF);
        }
        // couteau : au-dessus de la prochaine coupe ; descend puis remonte après chaque coupe
        int cible = n < 5 ? cx[n] : cx[4];
        if ("Carapace".equals(c.famille) && n < 5) cible += (int) (pas / 2);
        double descente = 0;
        int xCouteau = SX + cible;
        if (n > 0) {
            double age = tms - j.tCoupe[n - 1];
            if (age < Jeux.Couper.RECUL) {
                int xPrec = SX + cx[n - 1] + ("Carapace".equals(c.famille) ? (int) (pas / 2) : 0);
                if (age < 90) {
                    descente = age / 90.0;
                    xCouteau = xPrec;
                } else {
                    double k = (age - 90) / (Jeux.Couper.RECUL - 90);
                    descente = 1 - k;
                    xCouteau = (int) Math.round(xPrec + (SX + cible - xPrec) * k);
                }
            }
        }
        dessinerCouteau(t, xCouteau, (int) (SY - 6 + descente * 34), j.fini && n >= 5);
    }

    // ------------------------------------------------------------------ aliment par famille
    static void aliment(Toile s, Contexte c) {
        int[] pe = Dessin.rampe(c.peau), ac = Dessin.rampe(c.accent);
        int cy = 36;
        switch (c.famille) {
            case "Long": {
                boolean feuilles = vert(c.accent);
                Dessin.cylindreH(s, 22, 160, cy, 15, 5, pe);
                s.disque(160, cy, 5, pe[2]);
                for (int k = 0; k < 7; k++) {
                    int x = 34 + k * 18;
                    s.rect(x, cy - 6 + (k % 3), 4, 1, pe[1]);
                }
                if (feuilles) {
                    for (int k = 0; k < 4; k++) {
                        double a = Math.toRadians(160 + k * 22);
                        s.trait(22, cy - 4 + k * 2, 22 + Math.cos(a) * 20, cy - 4 + k * 2 + Math.sin(a) * 12, 4, ac[2]);
                        s.trait(22, cy - 4 + k * 2, 22 + Math.cos(a) * 16, cy - 4 + k * 2 + Math.sin(a) * 10, 2, ac[3]);
                    }
                } else s.disque(22, cy, 13, pe[1]);
                break;
            }
            case "Pavé": {
                int[] ch = Dessin.rampe(c.chair);
                int[] dessus = Dessin.rampe(Toile.melange(c.peau, 0xFFFFFFFF, 0.08));
                // face avant
                Dessin.degradeV(s, 28, 26, 116, 22, pe, 0.62, 0.32);
                // dessus (parallélogramme qui fuit vers le fond)
                for (int y = 12; y < 26; y++) {
                    int dx = (26 - y) * 10 / 14;
                    for (int x = 28 + dx; x < 144 + dx; x++) s.set(x, y, Dessin.bande(dessus, 0.95 - (26 - y) * 0.012, x, y));
                }
                // côté droit
                for (int x = 144; x < 154; x++) {
                    int dy = (x - 144) * 14 / 10;
                    for (int y = 26 - dy; y < 48 - dy; y++) s.set(x, y, Dessin.bande(ch, 0.55, x, y));
                }
                if (c.motif == 5) {
                    for (int k = 0; k < 9; k++) s.rect(36 + k * 12, 16 + (k * 5) % 7, 7, 1, ac[4]);
                    for (int k = 0; k < 7; k++) s.rect(38 + k * 15, 31 + (k * 7) % 12, 6, 1, ac[3]);
                } else for (int k = 0; k < 8; k++) s.rect(36 + k * 14, 16 + (k * 5) % 7, 5, 1, dessus[4]);
                s.rect(28, 26, 116, 1, dessus[4]);
                break;
            }
            case "Poisson": {
                int ventre = Toile.melange(c.peau, 0xFFE8ECF0, 0.7);
                int[] vr = Dessin.rampe(ventre);
                for (int x = 22; x <= 146; x++) {
                    double k = (x - 22) / 124.0;
                    double hh = 17 * (k < 0.25 ? Math.sqrt(k / 0.25) * 0.9 + 0.1 : 1 - (k - 0.25) / 0.75 * 0.75);
                    for (int y = (int) (cy - hh); y <= cy + hh; y++) {
                        double sy = (y - cy) / Math.max(1, hh);
                        double L = 0.85 - (sy + 1) * 0.3;
                        s.set(x, y, Dessin.bande(sy < -0.1 ? pe : vr, L, x, y));
                    }
                }
                s.polygone(new double[]{146, 164, 158, 164}, new double[]{cy, cy - 14, cy, cy + 14}, pe[2]);
                s.rect(34, cy - 3, 2, 2, 0xFF101010);
                s.set(34, cy - 3, 0xFFFFFFFF);
                s.trait(42, cy - 9, 44, cy + 7, 1.2, pe[1]);
                for (int k = 0; k < 10; k++) s.set(56 + k * 9, cy - 1 + (k % 2), vr[4]);
                break;
            }
            case "Herbes": {
                int[] v1 = Dessin.rampe(c.peau), v2 = Dessin.rampe(c.chair);
                for (int k = 0; k < 6; k++) s.trait(14, cy + 3 + k, 60, cy - 6 + k * 3, 1.5, Dessin.rampe(c.accent)[2]);
                s.rect(30, cy - 2, 5, 9, 0xFFC83A2A);
                for (int k = 0; k < 16; k++) {
                    double x = 56 + (k % 8) * 13 + (k / 8) * 6, y = cy - 10 + (k / 8) * 13 + ((k * 7) % 5);
                    Dessin.sphere(s, x, y, 8, 5.5, k % 2 == 0 ? v1 : v2, 0);
                    s.trait(x - 6, y, x + 6, y, 1, (k % 2 == 0 ? v1 : v2)[1]);
                }
                break;
            }
            case "Carapace":
                break;
            case "Noyau": {
                Dessin.sphere(s, 88, cy + 2, 28, 25, pe, 3);
                s.trait(88, cy - 22, 91, cy - 30, 2, 0xFF6A4A2A);
                if (vert(c.accent)) s.ellipse(98, cy - 28, 7, 3, Dessin.rampe(c.accent)[2]);
                break;
            }
            default: {
                Dessin.sphere(s, 88, cy + 1, 30, 26, pe, 3);
                if ("tomate".equals(c.sujet) || "poivron".equals(c.sujet)) {
                    int[] v = Dessin.rampe(0xFF3F9A2A);
                    s.polygone(new double[]{88, 92, 102, 94, 98, 88, 78, 82, 74, 84}, new double[]{cy - 30, cy - 24, cy - 25, cy - 21, cy - 15, cy - 19, cy - 15, cy - 21, cy - 25, cy - 24}, v[2]);
                    s.trait(88, cy - 22, 89, cy - 30, 2.5, v[1]);
                } else if (vert(c.accent)) {
                    int[] v = Dessin.rampe(c.accent);
                    s.polygone(new double[]{88, 92, 102, 94, 98, 88, 78, 82, 74, 84}, new double[]{cy - 30, cy - 24, cy - 25, cy - 21, cy - 15, cy - 19, cy - 15, cy - 21, cy - 25, cy - 24}, v[2]);
                } else {
                    s.trait(88, cy - 24, 89, cy - 29, 2, 0xFF6A4A2A);
                }
            }
        }
    }

    private static boolean vert(int c) {
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        return g > r + 15 && g > b + 15;
    }

    /** Colonnes extrêmes occupées par l'aliment. */
    private static int[] bords(Toile s) {
        int a = s.w, b = 0;
        for (int y = 0; y < s.h; y++)
            for (int x = 0; x < s.w; x++)
                if ((s.px[y * s.w + x] >>> 24) != 0) {
                    a = Math.min(a, x);
                    b = Math.max(b, x);
                }
        if (a > b) return new int[]{20, 150};
        return new int[]{a, b};
    }

    private static void effacerDroite(Toile s, int x) {
        for (int y = 0; y < s.h; y++) for (int i = Math.max(0, x + 1); i < s.w; i++) s.px[y * s.w + i] = 0;
    }

    /** Pavé coupé : on retire ce qui dépasse le plan de coupe (en biais sur le dessus) et on montre la tranche. */
    private static void coupePave(Toile s, Contexte c, int xf) {
        for (int y = 0; y < s.h; y++) {
            int lim = y >= 26 ? xf : xf + (26 - y) * 10 / 14;
            for (int x = Math.max(0, lim + 1); x < s.w; x++) s.px[y * s.w + x] = 0;
        }
        int[] ch = Dessin.rampe(c.chair), ac = Dessin.rampe(c.accent);
        for (int x = xf; x < xf + 10; x++) {
            int dy = (x - xf) * 14 / 10;
            for (int y = 26 - dy; y < 48 - dy; y++) s.set(x, y, Dessin.bande(ch, 0.8 - (y - 26 + dy) * 0.012, x, y));
        }
        if (c.motif == 5) for (int k = 0; k < 3; k++) s.ligne(xf + 2, 32 - k * 5 + 4, xf + 8, 26 - k * 5 + 4, ac[4]);
        s.ligne(xf, 26, xf + 10, 12, Toile.melange(c.chair, 0xFFFFFFFF, 0.4));
    }

    /** Face coupée : ellipse de chair avec le motif de l'ingrédient, à la hauteur de la silhouette. */
    private static void faceCoupee(Toile s, Contexte c, int x) {
        int top = -1, bot = -1;
        for (int y = 0; y < s.h; y++)
            if ((s.px[y * s.w + Math.max(0, x)] >>> 24) != 0) {
                if (top < 0) top = y;
                bot = y;
            }
        if (top < 0) return;
        double cy = (top + bot) / 2.0, ry = (bot - top) / 2.0 + 0.5, rx = Math.max(2.5, ry * 0.32);
        int[] ch = Dessin.rampe(c.chair), pe = Dessin.rampe(c.peau);
        s.ellipse(x, cy, rx + 1, ry, pe[1]);
        Dessin.ellipseV(s, x, cy, rx, ry - 1, ch, 0.95, 0.6);
        motif(s, c, x, cy, rx, ry - 1);
    }

    static void motif(Toile s, Contexte c, double cx, double cy, double rx, double ry) {
        int[] ch = Dessin.rampe(c.chair), ac = Dessin.rampe(c.accent);
        switch (c.motif) {
            case 1:
                s.ellipse(cx, cy, rx * 0.45, ry * 0.45, ch[1]);
                break;
            case 2:
                s.anneau(cx, cy, rx * 0.7, ry * 0.7, 1, ch[4]);
                s.anneau(cx, cy, rx * 0.4, ry * 0.4, 1, ch[4]);
                break;
            case 3:
                for (int k = 0; k < 6; k++) {
                    double a = k * Math.PI / 3 + 0.4;
                    s.set((int) (cx + Math.cos(a) * rx * 0.5), (int) (cy + Math.sin(a) * ry * 0.5), ac[3]);
                }
                break;
            case 4:
                for (int k = 0; k < 8; k++) {
                    double a = k * Math.PI / 4;
                    s.ligne(cx, cy, cx + Math.cos(a) * rx * 0.9, cy + Math.sin(a) * ry * 0.9, ch[4]);
                }
                break;
            case 5:
                for (int k = 0; k < 4; k++) s.ligne(cx - rx * 0.6, cy - ry * 0.6 + k * ry * 0.4, cx + rx * 0.5, cy - ry * 0.4 + k * ry * 0.4, ac[4]);
                break;
            case 6:
                s.ellipse(cx, cy, rx * 0.5, ry * 0.3, ac[1]);
                s.ellipse(cx - 0.5, cy - 0.5, rx * 0.3, ry * 0.18, ac[3]);
                break;
            case 7:
                for (int k = 0; k < 5; k++) s.set((int) (cx - rx * 0.4 + (k % 3) * rx * 0.4), (int) (cy - ry * 0.5 + k * ry * 0.25), ch[1]);
                break;
            default:
                break;
        }
    }

    /** Une tranche : elle tombe (age 0 -> 1) puis repose à plat sur la planche. */
    private static void tranche(Toile t, Contexte c, int k, int qualite, double age, int sh, int xCoupe) {
        if (age < 0) return;
        double a = Math.min(1, age);
        int restX = PX + PW - 58 + (k % 3) * 15, restY = PY + PH - 20 - (k / 3) * 9 + (k % 2) * 3;
        int fromX = SX + xCoupe + 6, fromY = SY + 30;
        double x = fromX + (restX - fromX) * a, y = fromY + (restY - fromY) * a - Math.sin(a * Math.PI) * 10;
        Toile s = new Toile(30, 22);
        int[] ch = Dessin.rampe(c.chair), pe = Dessin.rampe(c.peau);
        boolean irreguliere = qualite == MiniJeu.RATE;
        switch (c.famille) {
            case "Herbes": {
                int[] v = Dessin.rampe(c.peau), w = Dessin.rampe(c.chair);
                for (int i = 0; i < 14; i++) {
                    int h = Scenes.hash(k * 97 + i);
                    s.rect(6 + Math.floorMod(h, 16), 6 + Math.floorMod(h >> 8, 9), 2, 2, (i % 2 == 0 ? v : w)[2 + (i % 3 == 0 ? 1 : 0)]);
                }
                break;
            }
            case "Pavé": {
                int ep = irreguliere ? 5 : 3;
                double[] xs = {3, 23, 27, 7}, ys = {16, 16, 6, 6};
                s.polygone(xs, ys, ch[3]);
                for (int q = 0; q < 3; q++) s.ligne(6 + q * 6, 14, 10 + q * 6, 8, ch[4]);
                if (c.motif == 5) s.ligne(8, 12, 18, 9, Dessin.rampe(c.accent)[4]);
                s.rect(3, 16, 20, ep, pe[1]);
                s.polygone(new double[]{23, 27, 27, 23}, new double[]{16, 6, 6 + ep, 16 + ep}, pe[0]);
                if (irreguliere) s.ligne(4, 15, 22, 8, ch[1]);
                break;
            }
            case "Carapace": {
                Dessin.sphere(s, 15, 11, 6, 4, Dessin.rampe(c.peau), 1);
                s.trait(10, 8, 20, 14, 1, pe[0]);
                break;
            }
            default: {
                double rx = irreguliere ? 11 : 9, ry = irreguliere ? 8 : 6;
                s.ellipse(15, 12, rx + 1, ry + 1, pe[1]);
                s.ellipse(15, 13, rx + 1, ry + 1, pe[0]);
                Dessin.ellipseV(s, 15, 11, rx, ry, ch, 0.95, 0.6);
                motif(s, c, 15, 11, rx, ry);
                if (irreguliere) s.ligne(6, 9, 22, 14, pe[1]);
            }
        }
        s.contour(Dessin.CONTOUR);
        if (a >= 1) t.ellipse(x + 15, y + 17, 11, 3, 0x33000000);
        t.coller(s, (int) Math.round(x), (int) Math.round(y));
    }

    /** Crevettes, moules, huîtres, crabes : une rangée ; chaque coupe décortique (ou ouvre) une pièce. */
    private static void carapaces(Toile s, Contexte c, int n) {
        int[] pe = Dessin.rampe(c.peau), ch = Dessin.rampe(c.chair), ac = Dessin.rampe(c.accent);
        for (int i = 0; i < 5; i++) {
            double x = 30 + i * 26, y = 36;
            boolean ouvert = i >= 5 - n;
            switch (c.sujet) {
                case "moule":
                    Dessin.sphere(s, x, y, 10, 6, Dessin.rampe(0xFF2A2A4A), 2);
                    s.ligne(x - 8, y + 1, x + 8, y - 1, 0xFF4A4A7A);
                    if (ouvert) Dessin.sphere(s, x, y - 1, 6.5, 3.5, Dessin.rampe(0xFFF09A4A), 1);
                    break;
                case "huitre":
                    Dessin.sphere(s, x, y, 11, 7.5, pe, 1);
                    for (int k = 0; k < 3; k++) s.anneau(x, y, 11 - k * 3, 7.5 - k * 2, 1, pe[1]);
                    if (ouvert) {
                        s.ellipse(x, y - 1, 8, 5, 0xFFE8E4DC);
                        Dessin.sphere(s, x - 1, y - 1, 5.5, 3.5, ch, 2);
                    }
                    break;
                case "crabe":
                    if (!ouvert) {
                        Dessin.sphere(s, x - 2, y + 2, 8, 6, pe, 2);
                        Dessin.sphere(s, x + 5, y - 5, 5, 4, pe, 1);
                        s.trait(x + 3, y - 9, x + 9, y - 3, 1.5, pe[0]);
                    } else {
                        Dessin.sphere(s, x, y + 1, 8, 5, ch, 2);
                        s.ligne(x - 5, y, x + 5, y + 2, ch[1]);
                    }
                    break;
                default: {
                    int[] r = ouvert ? ch : pe;
                    for (int k = 0; k < 6; k++) {
                        double a = Math.toRadians(200 - k * 34), rr = 6.5 - k * 0.55;
                        Dessin.sphere(s, x + Math.cos(a) * 7, y + Math.sin(a) * 7, rr, rr * 0.9, r, 1);
                        s.ligne(x + Math.cos(a) * 3, y + Math.sin(a) * 3, x + Math.cos(a) * 12, y + Math.sin(a) * 12, r[1]);
                    }
                    if (!ouvert) {
                        s.polygone(new double[]{x + 5, x + 11, x + 12}, new double[]{y + 5, y + 11, y + 4}, pe[1]);
                        s.ligne(x - 7, y - 3, x - 14, y - 12, pe[0]);
                        s.set((int) x - 8, (int) y - 1, 0xFF101010);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ couteau de chef
    private static Toile couteau() {
        if (couteau != null) return couteau;
        Toile k = new Toile(104, 86);
        double tx = 6, ty = 74, ux = 0.78, uy = -0.63, nx = 0.63, ny = 0.78, L = 70;
        int[] lame = {0xFF8E969F, 0xFFC9D0D8, 0xFFF4F8FB};
        int N = 60;
        double[] ex = new double[N * 2 + 2], ey = new double[N * 2 + 2];
        for (int i = 0; i <= N; i++) {
            double s = i / (double) N, w = 17 * Math.min(1, s / 0.3 + 0.12);
            double sx = tx + ux * L * s, sy = ty + uy * L * s;
            ex[i] = sx - nx * w * 0.15;
            ey[i] = sy - ny * w * 0.15;
            ex[2 * N + 1 - i] = sx + nx * w;
            ey[2 * N + 1 - i] = sy + ny * w;
        }
        k.polygone(ex, ey, lame[1]);
        // bandes : dos clair, fil sombre, biseau brillant
        for (int i = 0; i <= N; i++) {
            double s = i / (double) N, w = 17 * Math.min(1, s / 0.3 + 0.12);
            double sx = tx + ux * L * s, sy = ty + uy * L * s;
            k.set((int) (sx + nx * w * 0.15), (int) (sy + ny * w * 0.15), lame[2]);
            k.set((int) (sx + nx * w * 0.3), (int) (sy + ny * w * 0.3), lame[2]);
            k.set((int) (sx + nx * (w - 1)), (int) (sy + ny * (w - 1)), lame[0]);
            k.set((int) (sx + nx * (w - 2.5)), (int) (sy + ny * (w - 2.5)), 0xFFFFFFFF);
        }
        // mitre et manche à rivets
        double hx = tx + ux * L, hy = ty + uy * L;
        k.trait(hx + nx * 1, hy + ny * 1, hx + nx * 15, hy + ny * 15, 5, 0xFFB8BEC6);
        k.trait(hx + nx * 1, hy + ny * 1, hx + nx * 15, hy + ny * 15, 1.5, 0xFFE8ECF0);
        k.trait(hx + ux * 4 + nx * 4.5, hy + uy * 4 + ny * 4.5, hx + ux * 28 + nx * 4.5, hy + uy * 28 + ny * 4.5, 11, 0xFF3A2418);
        k.trait(hx + ux * 4 + nx * 2, hy + uy * 4 + ny * 2, hx + ux * 28 + nx * 2, hy + uy * 28 + ny * 2, 2.5, 0xFF5E3A24);
        for (int r = 0; r < 3; r++) {
            double px = hx + ux * (9 + r * 7.5) + nx * 4.5, py = hy + uy * (9 + r * 7.5) + ny * 4.5;
            k.disque(px, py, 1.3, 0xFFD8DDE2);
            k.set((int) px, (int) py, 0xFFFFFFFF);
        }
        k.contour(Dessin.CONTOUR);
        couteau = k;
        return k;
    }

    /** Le couteau est placé pour que son fil touche le point (x, y). */
    private static void dessinerCouteau(Toile t, int x, int y, boolean pose) {
        Toile k = couteau();
        int ox = x - 30, oy = y - 70;
        t.ellipse(x + 24, PY + PH - 6, 26, 3, 0x2A000000);
        t.coller(k, ox, oy);
    }
}
