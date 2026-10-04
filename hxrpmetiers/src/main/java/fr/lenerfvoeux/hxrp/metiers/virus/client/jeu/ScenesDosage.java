package fr.lenerfvoeux.hxrp.metiers.virus.client.jeu;

import fr.lenerfvoeux.hxrp.metiers.client.minijeu.Dessin;
import fr.lenerfvoeux.hxrp.metiers.client.minijeu.Police;
import fr.lenerfvoeux.hxrp.metiers.client.minijeu.Toile;
import fr.lenerfvoeux.hxrp.metiers.virus.minijeu.JeuxVirus;

import static fr.lenerfvoeux.hxrp.metiers.virus.client.jeu.ScenesVirus.*;

/** Scènes du dosage et de la mise en forme : balance, mortier, pilulier, table de préparation. */
final class ScenesDosage {
    private ScenesDosage() {}

    // ================================================================== BALANCE D'APOTHICAIRE
    static void balance(Toile t, JeuxVirus.Balance j, ContexteVirus c, double tms, int mx, int my) {
        plateau(t, 60, 132, 312, 146);
        double px = 200, py = 58, demi = 64;
        double ang = j.inclinaison() * 0.28;
        int[] l = Dessin.rampe(0xFFE8A040);
        // colonne et socle en laiton
        Toile s = sprite();
        s.rect(186, 128, 28, 8, Dessin.OL);
        s.rect(188, 126, 24, 4, Dessin.OR);
        Dessin.degradeH(s, 197, (int) py, 6, 70, l, 0.9, 0.4);
        Dessin.sphere(s, px, py - 2, 5, 5, l, 1);
        // fléau
        double ex = Math.cos(ang) * demi, ey = Math.sin(ang) * demi;
        s.trait(px - ex, py - ey, px + ex, py + ey, 3, l[2]);
        s.trait(px - ex, py - ey - 1, px + ex, py + ey - 1, 1, l[4]);
        // aiguille et cadran
        s.trait(px, py, px + Math.sin(ang) * 22, py - Math.cos(ang) * 22, 1.5, C);
        poser(t, s);
        t.anneau(px, py - 18, 14, 8, 1, 0x88FFE8B0);
        t.rect((int) px, (int) py - 27, 1, 3, Dessin.G);
        // plateaux suspendus
        double gx = px - ex, gy = py - ey, dx = px + ex, dy = py + ey;
        plateauBalance(t, gx, gy, 46);
        plateauBalance(t, dx, dy, 46);
        // à gauche : l'ingrédient à peser ; à droite : les poids posés
        int[] ic = c.icone(0);
        if (ic != null) icone(t, ic, (int) gx - 16, (int) gy + 18, 1);
        else Dessin.sphere(t, gx, gy + 40, 12, 6, rampe(c.couleur), 1);
        int[] pile = new int[4];
        for (int k = 0; k < j.n; k++) for (int q = 0; q < 4; q++) if (JeuxVirus.Balance.POIDS[q] == j.plateau[k]) pile[q]++;
        double wx = dx - 14, base = dy + 44;
        for (int q = 3; q >= 0; q--) {
            for (int k = 0; k < pile[q]; k++) {
                int r = 3 + q * 2;
                poids(t, wx + r, base - r, r, q);
                base -= r * 1.4;
                if (base < dy + 14) {
                    base = dy + 44;
                    wx += 12;
                }
            }
        }
        etiquette(t, (int) gx, (int) gy + 52, "CIBLE " + j.cible + " G");
        etiquette(t, (int) dx, (int) dy + 52, "MASSE " + j.masse + " G");
        if (j.stricte) etiquette(t, 96, 44, "DOSE STRICTE");
        // boutons de poids (cliquables)
        for (int k = 0; k < 4; k++) {
            int bx = JeuxVirus.Balance.boutonX(k), by = JeuxVirus.Balance.BY;
            boolean survol = Math.hypot(mx - bx, my - by) <= JeuxVirus.Balance.BR + 2;
            t.disque(bx + 1.5, by + 2, JeuxVirus.Balance.BR, 0x55000000);
            t.disque(bx, by, JeuxVirus.Balance.BR + 1, C);
            Dessin.sphere(t, bx, by, JeuxVirus.Balance.BR, JeuxVirus.Balance.BR, Dessin.rampe(survol ? 0xFFFFC860 : 0xFFE09838), 2);
            t.anneau(bx, by, JeuxVirus.Balance.BR - 2, JeuxVirus.Balance.BR - 2, 1, 0xFF9A5A14);
            Police.centre(t, JeuxVirus.Balance.POIDS[k] + "G", bx, by - 3, 0xFF3A1A04, 0, 1);
            Police.centre(t, String.valueOf(k + 1), bx, by + JeuxVirus.Balance.BR + 4, Dessin.CREME, C, 1);
        }
    }

    private static void plateauBalance(Toile t, double x, double y, double longueur) {
        for (int k : new int[]{-1, 0, 1}) t.ligne(x, y, x + k * 16, y + longueur - 4, 0xFF8A6A3A);
        Toile s = sprite();
        Dessin.ellipseV(s, x, y + longueur, 22, 5, Dessin.rampe(0xFFD89A48), 0.9, 0.4);
        s.ellipse(x, y + longueur + 2, 20, 3, 0xFFA86A28);
        poser(t, s);
    }

    private static void poids(Toile t, double x, double y, int r, int q) {
        Toile s = sprite();
        int[] l = Dessin.rampe(0xFFC89038);
        Dessin.cylindreH(s, x - r, x + r, y, r * 0.55, r * 0.55, l);
        s.ellipse(x, y - r * 0.6, r * 0.5, r * 0.25, l[4]);
        poser(t, s);
    }

    // ================================================================== MORTIER ET PILON
    static void pilon(Toile t, JeuxVirus.Pilon j, ContexteVirus c, double tms) {
        plateau(t, 60, 150, 312, 166);
        double cx = 182, cy = 104, rx = 50, ry = 16;
        Dessin.ombre(t, cx + 6, cy + 46, rx * 0.9, 6);
        Toile s = sprite();
        int[] p = Dessin.rampe(PORCELAINE), bl = Dessin.rampe(BLEU);
        for (int y = (int) cy; y <= cy + 40; y++) {
            double k = (y - cy) / 40.0, w = rx * (1 - k * k * 0.45);
            for (int x = (int) (cx - w); x <= cx + w; x++) {
                double nx = (x - cx) / w;
                boolean bande = y > cy + 12 && y < cy + 17;
                s.set(x, y, Dessin.bande(bande ? bl : p, 0.85 - nx * 0.35 - k * 0.2, x, y));
            }
        }
        // motif de vagues bleues
        for (int x = (int) (cx - 40); x < cx + 40; x += 8) s.ligne(x, cy + 24, x + 4, cy + 21, bl[2]);
        s.rect((int) (cx - rx * 0.55), (int) (cy + 40), (int) (rx * 1.1), 5, p[1]);
        s.ellipse(cx, cy, rx, ry, p[4]);
        s.ellipse(cx, cy + 1, rx - 6, ry - 4, p[1]);
        poser(t, s);
        // contenu : pâte qui s'affine
        double fin = j.fait / (double) Math.max(1, j.total);
        int[] g = rampe(c.couleur);
        Dessin.ellipseV(t, cx, cy + 3, rx - 10, ry - 7, rampe(Toile.melange(c.couleur, 0xFFF0E8D0, fin * 0.3)), 0.9, 0.5);
        for (int k = 0; k < (int) ((1 - fin) * 50); k++) {
            int h = hash(k * 13L + 77);
            double a = Math.floorMod(h, 628) / 100.0, d = Math.floorMod(h >> 9, 90) / 100.0;
            t.rect((int) (cx + Math.cos(a) * (rx - 12) * d), (int) (cy + 3 + Math.sin(a) * (ry - 8) * d), 2, 2, g[1 + (k % 3)]);
        }
        // cercle qui se referme sur le pilon
        if (j.cible < j.total) {
            double dt = j.temps(j.cible) - tms;
            double k = Math.max(0, Math.min(1.4, dt / j.periode));
            double r0 = 9, r = r0 + 42 * k;
            t.anneau(cx, cy + 3, r0 + 1.5, (r0 + 1.5) * 0.5, 3, C);
            t.anneau(cx, cy + 3, r0, r0 * 0.5, 2, Dessin.OR);
            if (k < 1.3) {
                t.anneau(cx, cy + 3, r + 1, (r + 1) * 0.5, 3, Toile.alpha(C, 0.8));
                t.anneau(cx, cy + 3, r, r * 0.5, 1.5, Math.abs(dt) < j.fenetre ? Dessin.G : 0xFFFFF4D0);
            }
        }
        // pilon de porcelaine à manche de bois : frappe à chaque clic
        double age = tms - j.tFrappe;
        double d = age < 60 ? age / 60.0 : age < 220 ? 1 - (age - 60) / 160.0 : 0;
        if (age < 0) d = 0;
        double x = cx + 8, y = cy - 40 + d * 34;
        Toile pl = sprite();
        int[] b = Dessin.rampe(BOIS);
        pl.trait(x, y, x + 18, y - 44, 7, b[2]);
        pl.trait(x - 1, y - 1, x + 17, y - 45, 2, b[3]);
        Dessin.sphere(pl, x, y + 2, 7, 6, p, 2);
        Dessin.sphere(pl, x + 18, y - 45, 5, 4.5, b, 1);
        poser(t, pl);
        if (age >= 0 && age < 350) {
            double a = age / 350.0;
            for (int k = 0; k < 14; k++) {
                double ang = k * 0.45 + 3.3, rr = 8 + a * 30;
                t.rect((int) (cx + Math.cos(ang) * rr), (int) (cy + Math.sin(ang) * rr * 0.4 - a * 12), 2, 2, Toile.alpha(g[4], 1 - a));
            }
        }
        pastilles(t, j.resultats, j.total, j.cible, 186, 176);
    }

    // ================================================================== PILULIER
    static void pilulier(Toile t, JeuxVirus.Pilulier j, ContexteVirus c, double tms) {
        plateau(t, 60, 150, 312, 166);
        int x0 = 78, x1 = 296, y0 = 92, y1 = 128;
        int[] b = Dessin.rampe(BOIS_CLAIR), bf = Dessin.rampe(BOIS);
        Toile s = sprite();
        Dessin.degradeV(s, x0, y0, x1 - x0, y1 - y0, b, 0.85, 0.55);
        Dessin.degradeV(s, x0, y1, x1 - x0, 8, bf, 0.5, 0.3);
        poser(t, s);
        // boudin de pâte posé en long
        int[] pa = rampe(c.couleur);
        double coupe = j.lame(tms);
        int xl = (int) (x0 + 8 + coupe * (x1 - x0 - 16));
        Dessin.cylindreH(t, Math.max(x0 + 8, xl), x1 - 10, 110, 5, 5, pa);
        // rainures (irrégulières) ; la prochaine s'éclaire à l'approche
        for (int k = 0; k < 6; k++) {
            int rx = (int) (x0 + 8 + j.rainures[k] * (x1 - x0 - 16));
            boolean faite = k < j.cible;
            int col = faite ? (j.resultats[k] == 0 ? Dessin.RD : Dessin.GD) : bf[1];
            if (k == j.cible) {
                double dt = Math.abs(tms - j.temps(k));
                if (dt < j.fenetre * 3) col = dt < j.fenetre ? Dessin.G : Dessin.HL;
            }
            t.rect(rx - 1, y0 + 2, 3, y1 - y0 - 4, col);
            t.rect(rx - 1, y0 + 2, 1, y1 - y0 - 4, Toile.sombre(col, 0.7));
        }
        // la lame (barre d'acier à deux poignées) glisse sur la planche
        Toile l = sprite();
        int[] ac = Dessin.rampe(ACIER);
        l.rect(xl - 2, y0 - 10, 5, y1 - y0 + 20, ac[2]);
        l.rect(xl - 2, y0 - 10, 1, y1 - y0 + 20, ac[4]);
        Dessin.sphere(l, xl, y0 - 14, 5, 5, bf, 1);
        Dessin.sphere(l, xl, y1 + 14, 5, 5, bf, 1);
        poser(t, l);
        // pilules coupées dans le bac
        int n = 0;
        for (int k = 0; k < j.cible; k++) if (j.resultats[k] != 0) n++;
        for (int k = 0; k < n; k++) Dessin.sphere(t, 120 + k * 14, 146, 4.5, 3.5, pa, 1);
        pastilles(t, j.resultats, j.total, j.cible, 186, 176);
    }

    // ================================================================== TABLE DE PRÉPARATION
    static void table(Toile t, JeuxVirus.Table j, ContexteVirus c, double tms, int mx, int my) {
        plateau(t, 56, 112, 314, 166);
        // bande de papier : l'ordre à retenir (montré un instant), puis des cartes retournées
        boolean montre = j.montre(tms);
        int n = j.nb, w = n * 36 + 16, x0 = 180 - w / 2;
        if (j.phase < 2) {
            t.rect(x0 + 3, 44, w, 46, 0x44000000);
            t.rect(x0 - 1, 40, w + 2, 46, C);
            t.rect(x0, 41, w, 44, PAPIER);
            t.rect(x0, 41, w, 1, 0xFFFFF8E4);
            for (int k = 0; k < n; k++) {
                int x = x0 + 10 + k * 36;
                Police.centre(t, String.valueOf(k + 1), x + 14, 44, 0xFF7A5A3A, 0, 1);
                if (montre) ingredient(t, c, j.ordre[k], x - 2, 52);
                else {
                    t.rect(x + 2, 54, 24, 28, 0xFF9A2A1A);
                    t.rect(x + 3, 55, 22, 26, LAQUE);
                    Police.centre(t, "?", x + 14, 62, Dessin.OR_TEXTE, C, 1);
                }
            }
            if (j.phase == 0) {
                int bw = (int) ((1 - j.valeur) * (w - 8));
                t.rect(x0 + 4, 87, w - 8, 3, 0xFF6A4A2A);
                t.rect(x0 + 4, 87, bw, 3, Dessin.HL);
            }
        }
        // godets de porcelaine, chacun avec son ingrédient
        for (int i = 0; i < n; i++) {
            int gx = JeuxVirus.Table.godetX(i, n), gy = JeuxVirus.Table.SY;
            int place = -1;
            for (int k = 0; k < j.place; k++) if (j.places[k] == i) place = k;
            boolean survol = j.phase == 1 && place < 0 && Math.abs(mx - gx) <= JeuxVirus.Table.SR && Math.abs(my - gy) <= JeuxVirus.Table.SR + 4;
            Dessin.ombre(t, gx + 2, gy + 12, 16, 4);
            Dessin.ellipseV(t, gx, gy + 8, 16, 6, Dessin.rampe(survol ? 0xFFFFF4D8 : PORCELAINE), 0.9, 0.45);
            t.anneau(gx, gy + 8, 16, 6, 1, BLEU);
            if (place < 0 || j.phase == 2) ingredient(t, c, j.godets[i], gx - 16, gy - 22);
            if (place >= 0) {
                boolean juste = j.godets[i] == j.ordre[place];
                t.disque(gx, gy - 26, 7, C);
                t.disque(gx, gy - 26, 6, juste ? Dessin.G : Dessin.RG);
                Police.centre(t, String.valueOf(place + 1), gx + 1, gy - 29, 0xFF1A1008, 0, 1);
            }
        }
        // étaler : la spatule passe sur le linge
        if (j.phase == 2) {
            int lx0 = 120, lx1 = 250, ly0 = 60, ly1 = 100;
            t.rect(lx0 + 3, ly0 + 3, lx1 - lx0, ly1 - ly0, 0x44000000);
            t.rect(lx0, ly0, lx1 - lx0, ly1 - ly0, 0xFFF4EEE0);
            for (int y = ly0 + 3; y < ly1; y += 4) t.ligneH(lx0, lx1 - 1, y, 0xFFE8E0CC);
            int[] o = rampe(c.couleur);
            for (int p = 0; p < j.passes; p++) {
                int yy = ly0 + 6 + p * 11;
                for (int x = lx0 + 6; x < lx1 - 6; x++) for (int y = yy; y < yy + 8; y++) t.set(x, y, Dessin.bande(o, 0.45 + 0.3 * ((x + y) % 7) / 7.0, x, y));
            }
            double sx = lx0 + 10 + j.valeur * (lx1 - lx0 - 20);
            int yy = ly0 + 6 + Math.min(2, j.passes) * 11;
            Toile sp = sprite();
            int[] b = Dessin.rampe(BOIS);
            sp.polygone(new double[]{sx - 6, sx + 6, sx + 4, sx - 4}, new double[]{yy + 9, yy + 9, yy - 2, yy - 2}, Dessin.rampe(ACIER)[3]);
            sp.trait(sx, yy - 2, sx + 10, yy - 30, 4, b[2]);
            poser(t, sp);
        }
    }
}
