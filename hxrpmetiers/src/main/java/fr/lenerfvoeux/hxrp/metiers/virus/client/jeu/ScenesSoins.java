package fr.lenerfvoeux.hxrp.metiers.virus.client.jeu;

import fr.lenerfvoeux.hxrp.metiers.client.minijeu.Dessin;
import fr.lenerfvoeux.hxrp.metiers.client.minijeu.Police;
import fr.lenerfvoeux.hxrp.metiers.client.minijeu.Toile;
import fr.lenerfvoeux.hxrp.metiers.minijeu.MiniJeu;
import fr.lenerfvoeux.hxrp.metiers.virus.minijeu.JeuxVirus;

import static fr.lenerfvoeux.hxrp.metiers.virus.client.jeu.ScenesVirus.*;

/** Scènes des gestes sur le patient : gorgées, seringue, onguent et bandage, et l'analyse au microscope. */
final class ScenesSoins {
    private ScenesSoins() {}

    // ================================================================== GORGÉES / FUMIGATION
    static void gorgees(Toile t, JeuxVirus.Gorgees j, ContexteVirus c, double tms) {
        tete(t, 252, 92);
        int[] lq = rampe(c.couleur);
        double der = j.fait > 0 ? j.tGorgee[j.fait - 1] : -10000;
        double boit = bosse(tms - der, 600);
        if (j.fumee) {
            // coupelle de fumigation : herbes qui fument vers le visage
            Toile s = sprite();
            int[] cu = Dessin.rampe(CUIVRE);
            Dessin.ellipseV(s, 170, 132, 26, 7, cu, 0.9, 0.4);
            s.rect(150, 132, 40, 10, cu[1]);
            s.ellipse(170, 142, 20, 4, cu[0]);
            poser(t, s);
            for (int k = 0; k < 10; k++) {
                int h = hash(k * 41L);
                t.rect(152 + Math.floorMod(h, 36), 129 + Math.floorMod(h >> 5, 4), 3, 2, lq[1 + Math.floorMod(h >> 3, 3)]);
            }
            double vers = clamp(0.3 + j.valeur * 0.7);
            for (int k = 0; k < 9; k++) {
                double q = ((tms / 1400.0) + k / 9.0) % 1;
                double x = 170 + q * (226 - 170) * vers + Math.sin(q * 6 + k) * 5;
                double y = 124 - q * (124 - 96) * (0.4 + vers * 0.6);
                volute(t, x, y, 4 + q * 5, Toile.melange(0xFFE8E4DC, c.couleur, 0.25), (1 - q) * 0.8);
            }
            if (boit > 0) for (int k = 0; k < 4; k++) volute(t, 232 - k * 4, 94 + k, 3, 0xFFF0F0F0, boit * 0.8);
        } else {
            // la main tient la fiole par le fond, le goulot vers la bouche ; plus la flèche monte, plus elle s'incline
            double inc = -0.55 + j.valeur * 0.75 + boit * 0.25;
            double fx = 200, fy = 100;
            double ca = Math.cos(inc), sa = Math.sin(inc);
            Toile s = sprite();
            // corps de la fiole (de -16 au fond à +6 à l'épaule), col jusqu'à +16
            double[][] corps = {{-16, -7}, {4, -7}, {8, -3}, {16, -3}, {16, 3}, {8, 3}, {4, 7}, {-16, 7}};
            double[] xs = new double[corps.length], ys = new double[corps.length];
            for (int k = 0; k < corps.length; k++) {
                xs[k] = fx + corps[k][0] * ca - corps[k][1] * sa;
                ys[k] = fy + corps[k][0] * sa + corps[k][1] * ca;
            }
            s.polygone(xs, ys, 0xFFD8ECF4);
            double niveau = 1 - j.fait / 3.0;
            if (niveau > 0) {
                double fin = -15 + 18 * niveau;
                double[][] liq = {{-15, -6}, {fin, -6}, {fin, 6}, {-15, 6}};
                double[] lx = new double[4], ly = new double[4];
                for (int k = 0; k < 4; k++) {
                    lx[k] = fx + liq[k][0] * ca - liq[k][1] * sa;
                    ly[k] = fy + liq[k][0] * sa + liq[k][1] * ca;
                }
                s.polygone(lx, ly, lq[2]);
            }
            double bx = fx + 17 * ca, by = fy + 17 * sa;
            s.disque(bx, by, 2.5, 0xFFB07A44);
            poser(t, s);
            t.trait(fx - 12 * ca + 4 * sa, fy - 12 * sa - 4 * ca, fx + 2 * ca + 4 * sa, fy + 2 * sa - 4 * ca, 1, 0xAAFFFFFF);
            main(t, fx - 14 * ca, fy - 14 * sa + 6);
            if (boit > 0.2 && niveau > 0) for (int k = 0; k < 5; k++) t.rect((int) (bx + 2 + k * 1.5), (int) (by + k), 2, 2, lq[3]);
        }
        int[] res = new int[3];
        for (int k = 0; k < 3; k++) res[k] = k < j.fait ? j.qualite[k] : -1;
        pastilles(t, res, 3, j.fait, 186, 174);
        if (!c.sujet.isEmpty()) etiquette(t, 262, 150, c.sujet);
    }

    /** Visage de profil, tourné vers la gauche (bouche vers x - 26). */
    private static void tete(Toile t, double x, double y) {
        int[] p = Dessin.rampe(PEAU), ch = Dessin.rampe(0xFF4A2A1A);
        Toile s = sprite();
        Dessin.sphere(s, x, y - 6, 26, 30, p, 1);
        s.polygone(new double[]{x - 22, x - 30, x - 24, x - 20}, new double[]{y - 12, y - 2, y + 2, y}, p[2]);
        s.polygone(new double[]{x - 24, x - 27, x - 18}, new double[]{y + 8, y + 12, y + 14}, p[1]);
        s.rect((int) x - 4, (int) y + 18, 18, 12, p[1]);
        Dessin.sphere(s, x + 8, y + 44, 34, 16, Dessin.rampe(0xFF3A5A8A), 1);
        Dessin.sphere(s, x + 8, y - 2, 4, 6, p, 0);
        for (int k = 0; k < 22; k++) {
            double a = Math.PI * 1.05 + k * 0.075;
            s.disque(x + 4 + Math.cos(a) * 24, y - 10 + Math.sin(a) * 26, 7, ch[1 + (k % 2)]);
        }
        Dessin.sphere(s, x + 8, y - 22, 22, 16, ch, 1);
        poser(t, s);
        t.rect((int) x - 14, (int) y - 12, 3, 2, 0xFF2A1A12);
        t.ligne(x - 26, y + 8, x - 20, y + 9, 0xFF8A3A2A);
    }

    private static void main(Toile t, double x, double y) {
        Toile s = sprite();
        int[] p = Dessin.rampe(PEAU);
        Dessin.sphere(s, x, y, 9, 7, p, 1);
        for (int k = 0; k < 3; k++) Dessin.sphere(s, x + 6, y - 6 + k * 4, 4, 2.5, p, 0);
        s.rect((int) x - 30, (int) y - 4, 24, 9, p[1]);
        s.rect((int) x - 34, (int) y - 5, 6, 11, 0xFF5A3A6A);
        poser(t, s);
    }

    // ================================================================== SERINGUE : VEINE PUIS PISTON
    static void injection(Toile t, JeuxVirus.Injection j, ContexteVirus c, double tms) {
        int ax0 = 70, ax1 = 300, ay = 118;
        bras(t, ax0, ax1, ay);
        // garrot pour la prise de sang
        if (j.prise) {
            t.rect(92, ay - 15, 8, 31, CORDE_ROUGE);
            t.rect(92, ay - 15, 2, 31, 0xFFE0402A);
        }
        // la veine, et la zone verte qui y roule
        int v0 = 108, v1 = 286;
        for (int x = v0; x < v1; x++) {
            int y = (int) (ay - 4 + Math.sin(x / 13.0) * 2);
            t.set(x, y, 0xAA4A6AB8);
            t.set(x, y + 1, 0x664A6AB8);
        }
        if (j.phase == 0) {
            int z0 = (int) (v0 + j.zoneLo * (v1 - v0)), z1 = (int) (v0 + j.zoneHi * (v1 - v0));
            for (int x = z0; x < z1; x++) {
                int y = (int) (ay - 4 + Math.sin(x / 13.0) * 2);
                t.rect(x, y - 1, 1, 4, 0xCC66D136);
            }
        }
        // seringue : vise la veine, puis s'y pique
        double nx = j.phase == 0 ? v0 + j.aiguille * (v1 - v0) : v0 + (j.zoneLo + j.zoneHi) / 2 * (v1 - v0);
        double ny = ay - 3 + (j.phase == 0 ? -10 - 4 * Math.sin(tms / 200.0) : 1);
        if (j.phase == 1 && j.tPique >= 0) nx = v0 + j.aiguille * (v1 - v0);
        seringue(t, nx, ny, j, c);
        if (j.forts > 0) for (int k = 0; k < Math.min(3, j.forts); k++) t.ellipse(nx + 6 + k * 5, ay + 2, 3, 2, 0x887A2A6A);
        if (!c.sujet.isEmpty()) etiquette(t, 250, 150, c.sujet);
    }

    private static void bras(Toile t, int x0, int x1, int y) {
        Toile s = sprite();
        int[] p = Dessin.rampe(PEAU);
        Dessin.cylindreH(s, x0, x1 - 22, y, 17, 14, p);
        Dessin.sphere(s, x1 - 14, y + 1, 16, 13, p, 1);
        for (int k = 0; k < 4; k++) Dessin.sphere(s, x1 + 2, y - 8 + k * 5, 6, 2.5, p, 0);
        poser(t, s);
        t.rect(x0, y + 10, x1 - x0 - 30, 2, 0x22000000);
    }

    private static void seringue(Toile t, double nx, double ny, JeuxVirus.Injection j, ContexteVirus c) {
        // corps oblique : l'aiguille en bas à gauche, le piston en haut à droite
        double ang = -0.75, ca = Math.cos(ang), sa = Math.sin(ang);
        double bx = nx + 14 * ca, by = ny + 14 * sa;
        double L = 46;
        double ex = bx + L * ca, ey = by + L * sa;
        Toile s = sprite();
        s.trait(nx, ny, bx, by, 1, ACIER);
        s.trait(bx, by, ex, ey, 11, 0xFFE4F0F4);
        // contenu : sang qui monte (prise) ou remède qui descend (injection)
        double frac = j.prise ? Math.min(1, j.tenu / JeuxVirus.Injection.TENIR) : 1 - Math.min(1, j.tenu / JeuxVirus.Injection.TENIR);
        int col = j.prise ? 0xFF9A1A1A : c.couleur;
        if (frac > 0.01) s.trait(bx + ca, by + sa, bx + (L - 2) * frac * ca, by + (L - 2) * frac * sa, 8, col);
        for (int k = 1; k < 6; k++) {
            double gx = bx + k * L / 6 * ca, gy = by + k * L / 6 * sa;
            s.trait(gx - 3 * sa, gy + 3 * ca, gx - 5 * sa, gy + 5 * ca, 1, 0xFF6A7A88);
        }
        // piston
        double pl = 4 + j.piston * 14;
        double px = ex + pl * ca, py = ey + pl * sa;
        s.trait(ex, ey, px, py, 3, 0xFF8A9AA8);
        s.trait(px - 7 * sa, py + 7 * ca, px + 7 * sa, py - 7 * ca, 3, 0xFF5A6A78);
        s.trait(ex - 8 * sa, ey + 8 * ca, ex + 8 * sa, ey - 8 * ca, 2, 0xFF8A9AA8);
        poser(t, s);
        t.trait(bx + 2 * ca - 3 * sa, by + 2 * sa + 3 * ca, ex - 3 * ca - 3 * sa, ey - 3 * sa + 3 * ca, 1, 0x99FFFFFF);
    }

    // ================================================================== ÉTALER L'ONGUENT / SERRER LE BANDAGE
    static void etaler(Toile t, JeuxVirus.EtalerSoin j, ContexteVirus c, double tms) {
        int x0 = 80, x1 = 290, y = 112;
        bras(t, x0, x1, y);
        // la plaie
        t.ellipse(186, y - 2, 22, 7, 0xCC9A2A2A);
        t.ellipse(184, y - 3, 14, 4, 0xCCD04A3A);
        int[] o = rampe(c.couleur);
        double w = x1 - x0 - 70;
        if (!j.serrer) {
            for (int k = 0; k < 3; k++) {
                double e = j.etendue[k];
                if (k >= j.fait || e <= 0) continue;
                int ya = y - 9 + k * 6;
                for (int x = x0 + 30; x < x0 + 30 + w * e; x++) for (int yy = ya; yy < ya + 5; yy++) t.set(x, yy, Toile.alpha(Dessin.bande(o, 0.55 + ((x + yy) % 5) / 12.0, x, yy), 0.85));
            }
            if (j.phase == JeuxVirus.EtalerSoin.POUSSE || j.phase == JeuxVirus.EtalerSoin.RETOUR) {
                int ya = y - 9 + Math.min(2, j.fait) * 6;
                double sx = x0 + 30 + w * clamp(j.valeur);
                for (int x = x0 + 30; x < sx; x++) for (int yy = ya; yy < ya + 5; yy++) t.set(x, yy, Toile.alpha(o[3], 0.7));
                Toile sp = sprite();
                int[] b = Dessin.rampe(BOIS);
                sp.polygone(new double[]{sx - 2, sx + 4, sx + 4, sx - 2}, new double[]{ya - 4, ya - 4, ya + 8, ya + 8}, Dessin.rampe(ACIER)[3]);
                sp.trait(sx + 2, ya - 4, sx + 14, ya - 34, 4, b[2]);
                poser(t, sp);
            }
        } else {
            // bande de lin qui s'enroule ; trop serrée, elle rougit
            int[] lin = Dessin.rampe(0xFFEDE6D6);
            for (int k = 0; k < j.fait; k++) {
                if (j.etendue[k] < 0) continue;
                int bx = (int) (x0 + 40 + k * 46);
                for (int s = 0; s < 4; s++) t.trait(bx + s * 9, y - 17, bx + s * 9 + 12, y + 15, 6, lin[2 + (s % 2)]);
            }
            if (j.phase == JeuxVirus.EtalerSoin.POUSSE) {
                int bx = (int) (x0 + 40 + Math.min(2, j.fait) * 46);
                double tension = clamp(j.valeur);
                int col = Toile.melange(lin[3], 0xFFE04030, clamp((tension - 0.75) * 4));
                int n = (int) (1 + tension * 4);
                for (int s = 0; s < n; s++) t.trait(bx + s * 9, y - 17, bx + s * 9 + 12, y + 15, 6, col);
                // le rouleau de bande, posé sur le bras au bout de l'enroulement
                double rx = bx + n * 9 + 10;
                Toile r = sprite();
                Dessin.cylindreH(r, rx - 7, rx + 7, y - 2, 9, 9, lin);
                r.ellipse(rx + 7, y - 2, 3, 9, lin[3]);
                r.anneau(rx + 7, y - 2, 2, 6, 1, lin[1]);
                poser(t, r);
            }
        }
        double[] res = j.etendue;
        int[] p = new int[3];
        for (int k = 0; k < 3; k++) p[k] = k >= j.fait ? -1 : res[k] < 0 ? MiniJeu.RATE : res[k] >= 0.99 ? MiniJeu.PARFAIT : MiniJeu.BIEN;
        pastilles(t, p, 3, j.fait, 186, 174);
        if (!c.sujet.isEmpty()) etiquette(t, 250, 150, c.sujet);
    }

    // ================================================================== MICROSCOPE
    static void microscope(Toile t, JeuxVirus.Microscope j, ContexteVirus c, double tms) {
        double cx = 172, cy = 106, R = 62;
        // oculaire : bague de laiton, champ lumineux
        t.disque(cx + 3, cy + 4, R + 7, 0x55000000);
        t.disque(cx, cy, R + 6, C);
        Dessin.sphere(t, cx, cy, R + 5, R + 5, Dessin.rampe(0xFFD89A48), 0);
        t.disque(cx, cy, R + 1, C);
        double net = j.bonZoom() ? j.nettete() : j.nettete() * 0.4;
        int fond = 0xFFF4E8D8;
        t.disque(cx, cy, R, fond);
        int zoom = JeuxVirus.Microscope.ZOOMS[j.zoom];
        double echelle = zoom / 10.0;
        double rc = Math.max(1.5, Math.min(15, 2.6 * echelle));
        double flou = (1 - net) * 5;
        int n = (int) Math.max(6, Math.min(160, 160 / (echelle * echelle)));
        long g = j.graine;
        for (int k = 0; k < n; k++) {
            int h = hash(g + k * 7919L);
            double px = cx + (Math.floorMod(h, 2000) / 1000.0 - 1) * R, py = cy + (Math.floorMod(h >> 11, 2000) / 1000.0 - 1) * R;
            px += Math.sin(tms / 1700.0 + k) * 0.6;
            if (Math.hypot(px - cx, py - cy) > R - rc) continue;
            int type = Math.floorMod(h >> 22, 23);
            if (type == 0) cellule(t, px, py, rc * 1.4, 0xFFB8A8E0, 0xFF6A4AA8, flou);
            else if (type < 3) cellule(t, px, py, rc * 0.35, 0xFFC8A0C8, 0xFFC8A0C8, flou);
            else cellule(t, px, py, rc, 0xFFD8504A, 0xFFF0A098, flou);
        }
        // voile de flou et vignettage
        if (flou > 0.3) t.disque(cx, cy, R, Toile.alpha(fond, Math.min(0.55, flou / 9)));
        t.anneau(cx, cy, R, R, 6, 0x33000000);
        // tourelle d'objectifs : ×4 ×10 ×40 ×100, la cible marquée
        for (int k = 0; k < 4; k++) {
            int ox = 268, oy = 58 + k * 26;
            boolean cour = k == j.zoom, cible = k == j.etape + 1 && j.etape < 3, fait = k >= 1 && k <= j.etape;
            t.disque(ox, oy, 10, C);
            t.disque(ox, oy, 9, cour ? Dessin.HL : 0xFF8A5530);
            Police.centre(t, "×" + JeuxVirus.Microscope.ZOOMS[k], ox, oy - 3, cour ? 0xFF2A1508 : Dessin.CREME, 0, 1);
            if (cible) {
                t.polygone(new double[]{ox - 18, ox - 12, ox - 18}, new double[]{oy - 4, oy, oy + 4}, Dessin.G);
            }
            if (fait) t.rect(ox + 12, oy - 1, 5, 3, Dessin.G);
        }
        // molette de mise au point : tourne avec le réglage
        double ang = j.mise * Math.PI * 4;
        int kx = 268, ky = 162;
        t.disque(kx, ky, 11, C);
        Dessin.sphere(t, kx, ky, 10, 10, Dessin.rampe(0xFF6A6E78), 1);
        for (int k = 0; k < 8; k++) {
            double a = ang + k * Math.PI / 4;
            t.set((int) (kx + Math.cos(a) * 8), (int) (ky + Math.sin(a) * 8), 0xFF2A2C34);
        }
        Police.centre(t, "NETTETÉ " + Math.round(net * 100) + " %", 172, 174, net > 0.85 ? Dessin.VERT_TEXTE : Dessin.CREME, C, 1);
    }

    /** Une cellule sanguine, plus ou moins floue : disque, centre plus pâle, halo. */
    private static void cellule(Toile t, double x, double y, double r, int bord, int centre, double flou) {
        if (flou > 0.4) t.disque(x, y, r + flou, Toile.alpha(bord, Math.max(0.12, 0.45 - flou * 0.06)));
        t.disque(x, y, r, Toile.alpha(bord, Math.max(0.25, 1 - flou * 0.16)));
        if (r > 2.5) t.disque(x - r * 0.1, y - r * 0.1, r * 0.45, Toile.alpha(centre, Math.max(0.2, 0.9 - flou * 0.15)));
    }
}
