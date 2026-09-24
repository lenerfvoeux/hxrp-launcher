package fr.lenerfvoeux.hxrp.metiers.client.minijeu;

import fr.lenerfvoeux.hxrp.metiers.minijeu.Jeux;
import fr.lenerfvoeux.hxrp.metiers.minijeu.MiniJeu;

/** ÉTALER, PÉTRIR, FAÇONNER : la pâte sur le plan de travail fariné. */
final class Pate {
    static final int PATE = 0xFFF0DCA8;

    private Pate() {}

    // ================================================================== ÉTALER
    static void etaler(Toile t, Jeux.Etaler j, Contexte c, double tms) {
        Scenes.planche(t, 58, 58, 214, 84, 10, true, 11);
        // la pâte s'étale à chaque bonne passe
        double gain = 0;
        int dechirees = 0;
        for (int k = 0; k < j.fait; k++) {
            if (j.etendue[k] < 0) dechirees++;
            else gain += j.etendue[k];
        }
        double avance = gain / j.total;
        double rx = 34 + 46 * avance, ry = 15 + 17 * avance;
        double cx = 170, cy = 100;
        Toile p = new Toile(Jeux.W, Jeux.H);
        int[] r = Dessin.rampe(PATE);
        p.ellipse(cx + 1, cy + 3, rx + 1, ry, r[1]);
        Dessin.ellipseV(p, cx, cy, rx, ry, r, 0.95, 0.6);
        p.anneau(cx, cy, rx, ry, 2, r[2]);
        for (int k = 0; k < 18; k++) {
            int h = Scenes.hash(k * 13 + 5);
            double a = (Math.floorMod(h, 628)) / 100.0, d = Math.floorMod(h >> 10, 80) / 100.0;
            p.rect((int) (cx + Math.cos(a) * rx * d), (int) (cy + Math.sin(a) * ry * d), 3, 1, r[3]);
        }
        for (int k = 0; k < dechirees; k++) {
            double a = 0.6 + k * 2.1, x0 = cx + Math.cos(a) * rx * 0.4, y0 = cy + Math.sin(a) * ry * 0.4;
            double x = x0, y = y0;
            for (int s = 0; s < 6; s++) {
                double nx = x + Math.cos(a) * 5 + ((s % 2) * 2 - 1) * 2, ny = y + Math.sin(a) * 2.5 + ((s % 2) * 2 - 1);
                p.trait(x, y, nx, ny, 2, 0xFF6A4A2A);
                x = nx;
                y = ny;
            }
        }
        p.contour(Dessin.CONTOUR);
        t.coller(p, 0, 0);
        // rouleau : avance vers le fond quand on maintient
        double y = 136 - 66 * j.valeur;
        rouleau(t, 62, 268, y);
        // aide : maintenir
        boolean pousse = j.phase == Jeux.Etaler.POUSSE;
        Dessin.souris(t, 282, 150, 0, pousse);
        Police.centre(t, "MAINTENIR", 288, 172, pousse ? Dessin.OR_TEXTE : Dessin.CREME, Dessin.CONTOUR, 1);
    }

    static void rouleau(Toile t, int x0, int x1, double y) {
        t.ellipse((x0 + x1) / 2.0 + 6, y + 9, (x1 - x0) / 2.0, 3.5, 0x44000000);
        Toile s = new Toile(Jeux.W, Jeux.H);
        int[] bois = Dessin.rampe(0xFFD8A868), manche = Dessin.rampe(0xFF9A6334);
        Dessin.cylindreH(s, x0 + 18, x1 - 18, y, 6.5, 6.5, bois);
        for (int k = x0 + 24; k < x1 - 22; k += 23) s.rect(k, (int) y - 2, 6, 1, bois[3]);
        Dessin.cylindreH(s, x0, x0 + 16, y, 3.5, 3.5, manche);
        Dessin.cylindreH(s, x1 - 16, x1, y, 3.5, 3.5, manche);
        s.rect(x0 + 16, (int) y - 4, 2, 9, manche[1]);
        s.rect(x1 - 18, (int) y - 4, 2, 9, manche[1]);
        s.contour(Dessin.CONTOUR);
        t.coller(s, 0, 0);
    }

    // ================================================================== PÉTRIR
    private static final String[] LETTRES = {"Z", "Q", "S", "D"};

    static void petrir(Toile t, Jeux.Petrir j, Contexte c, double tms) {
        Scenes.planche(t, 62, 70, 206, 76, 10, true, 13);
        double lisse = j.fait / (double) Math.max(1, j.total);
        double cx = 168, cy = 108, rx = 38, ry = 26;
        // déformation après chaque pli, dans le sens du geste
        if (j.tDernier >= 0 && j.dernierQualite != MiniJeu.RATE) {
            double k = Scenes.bosse(tms - j.tDernier, 260) * 0.28;
            int d = j.dernierSens;
            if (d == MiniJeu.GAUCHE || d == MiniJeu.DROITE) {
                rx *= 1 + k;
                ry *= 1 - k * 0.5;
                cx += (d == MiniJeu.DROITE ? 1 : -1) * k * 16;
            } else {
                ry *= 1 + k * 0.7;
                rx *= 1 - k * 0.4;
                cy += (d == MiniJeu.BAS ? 1 : -1) * k * 10;
            }
        }
        Toile p = new Toile(Jeux.W, Jeux.H);
        Dessin.ombre(t, cx + 4, cy + ry - 2, rx + 4, 6);
        int[] r = Dessin.rampe(Toile.melange(0xFFE8CC90, PATE, lisse));
        Dessin.sphere(p, cx, cy, rx, ry, r, 3);
        int bosses = (int) Math.round(9 * (1 - lisse));
        for (int k = 0; k < bosses; k++) {
            int h = Scenes.hash(k * 29 + 3);
            double a = Math.floorMod(h, 628) / 100.0, d = 0.3 + Math.floorMod(h >> 9, 55) / 100.0;
            p.rect((int) (cx + Math.cos(a) * rx * d), (int) (cy + Math.sin(a) * ry * d), 2, 1, r[1]);
        }
        p.contour(Dessin.CONTOUR);
        t.coller(p, 0, 0);
        // la flèche à suivre et sa touche
        if (j.phase == 1 && !j.fini) {
            int bx = 168, by = 44;
            double reste = j.valeur;
            t.disque(bx, by, 17, Dessin.OL);
            t.disque(bx, by, 15.5, Dessin.OR);
            // minuterie en arc
            for (int a = 0; a < 360; a += 4) {
                if (a / 360.0 > reste) break;
                double ang = Math.toRadians(a - 90);
                t.rect((int) (bx + Math.cos(ang) * 14), (int) (by + Math.sin(ang) * 14), 2, 2, reste < 0.3 ? Dessin.RG : Dessin.G);
            }
            t.disque(bx, by, 11, 0xFF8A5530);
            fleche(t, bx, by, j.dir, Dessin.CREME);
            Dessin.touche(t, bx + 22, by - 7, LETTRES[j.dir], false);
            Police.texte(t, "OU", bx + 40, by - 3, Dessin.CREME, Dessin.CONTOUR, 1);
            Dessin.touche(t, bx + 55, by - 7, " ", false);
            fleche(t, bx + 62, by, j.dir, 0xFF2A2420, 4);
        }
        // mémo des quatre touches
        int mx = 262, my = 104;
        Dessin.touche(t, mx + 8, my - 16, "Z", j.phase == 1 && j.dir == 0);
        Dessin.touche(t, mx - 8, my, "Q", j.phase == 1 && j.dir == 1);
        Dessin.touche(t, mx + 8, my, "S", j.phase == 1 && j.dir == 2);
        Dessin.touche(t, mx + 24, my, "D", j.phase == 1 && j.dir == 3);
    }

    static void fleche(Toile t, int cx, int cy, int dir, int c) {
        fleche(t, cx, cy, dir, c, 8);
    }

    static void fleche(Toile t, int cx, int cy, int dir, int c, int s) {
        double[][] base = {{0, -s}, {s, 0}, {s * 0.4, 0}, {s * 0.4, s}, {-s * 0.4, s}, {-s * 0.4, 0}, {-s, 0}};
        double ang = new double[]{0, -Math.PI / 2, Math.PI, Math.PI / 2}[dir];
        double[] xs = new double[7], ys = new double[7], xo = new double[7], yo = new double[7];
        for (int i = 0; i < 7; i++) {
            double x = base[i][0], y = base[i][1];
            xs[i] = cx + x * Math.cos(ang) - y * Math.sin(ang);
            ys[i] = cy + x * Math.sin(ang) + y * Math.cos(ang);
            xo[i] = xs[i] + 1;
            yo[i] = ys[i] + 1;
        }
        t.polygone(xo, yo, Dessin.CONTOUR);
        t.polygone(xs, ys, c);
    }

    // ================================================================== FAÇONNER
    static void faconner(Toile t, Jeux.Faconner j, Contexte c, double tms, int mx, int my) {
        Scenes.planche(t, 70, 66, 206, 82, 10, true, 17);
        double k = j.fait / (double) Math.max(1, j.total);
        int cx = Jeux.Faconner.CX, cy = Jeux.Faconner.CY;
        Dessin.ombre(t, cx + 4, cy + 22, Jeux.Faconner.RX + 4, 6);
        Toile p = new Toile(Jeux.W, Jeux.H);
        int[] r = Dessin.rampe(PATE);
        // une masse irrégulière qui s'arrondit à chaque appui
        double irr = 1 - k;
        for (int b = 0; b < 4; b++) {
            double a = b * 1.7 + 0.4;
            double bx = cx + Math.cos(a) * 16 * irr, by = cy + Math.sin(a) * 6 * irr;
            Dessin.sphere(p, bx, by, Jeux.Faconner.RX * (0.7 + 0.3 * k), Jeux.Faconner.RY * (0.75 + 0.25 * k), r, 0);
        }
        Dessin.sphere(p, cx, cy - 1, Jeux.Faconner.RX * (0.55 + 0.45 * k), Jeux.Faconner.RY * (0.7 + 0.3 * k), r, 3);
        for (int i = 0; i < j.fait; i++) {
            p.ellipse(j.faitsX[i], j.faitsY[i], 3.5, 2, r[1]);
            p.ellipse(j.faitsX[i], j.faitsY[i] - 0.5, 2.5, 1.2, r[0]);
            p.rect(j.faitsX[i] - 2, j.faitsY[i] + 2, 4, 1, r[4]);
        }
        p.contour(Dessin.CONTOUR);
        t.coller(p, 0, 0);
        if (j.phase == 1 && !j.fini) {
            double rr = j.rayon, ext = rr + 16 * j.valeur;
            t.anneau(j.px, j.py, ext + 1, ext + 1, 3, Dessin.CONTOUR);
            t.anneau(j.px, j.py, ext, ext, 1.5, j.valeur < 0.3 ? Dessin.RG : 0xFFFFF4D0);
            t.anneau(j.px, j.py, rr + 1, rr + 1, 3, Dessin.CONTOUR);
            t.anneau(j.px, j.py, rr, rr, 1.5, Dessin.OR);
            t.disque(j.px + 0.5, j.py + 0.5, rr * 0.4, Dessin.HL);
            t.disque(j.px + 0.5, j.py + 0.5, rr * 0.4 - 1, 0xFFFFF4D0);
        }
    }
}
