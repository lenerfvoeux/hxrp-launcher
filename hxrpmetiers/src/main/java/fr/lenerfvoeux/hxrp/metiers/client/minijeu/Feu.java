package fr.lenerfvoeux.hxrp.metiers.client.minijeu;

import fr.lenerfvoeux.hxrp.metiers.minijeu.Jeux;
import fr.lenerfvoeux.hxrp.metiers.minijeu.MiniJeu;

/** Tout ce qui chauffe : poêle, casserole, marmite, four, grill et friteuse. */
final class Feu {
    static final int CRU = 0xFFF4E4C8, BRULE = 0xFF2A1810;

    private Feu() {}

    /** Icône 32x32 agrandie x2, teintée selon la cuisson (0 cru pâle, ~0.75 cuit, >1 brûlé). */
    static void aliment(Toile t, int[] ic, int x, int y, double cuisson, boolean miroir) {
        if (ic == null) return;
        Toile s = new Toile(64, 64);
        int teinte;
        double k;
        if (cuisson < 0.75) {
            teinte = CRU;
            k = (0.75 - cuisson) / 0.75 * 0.45;
        } else {
            teinte = BRULE;
            k = Math.max(0, cuisson - 0.85) / 0.25 * 0.85;
        }
        Toile i1 = new Toile(32, 32);
        i1.collerTeinte(ic, 32, 32, 0, 0, teinte, Math.min(0.9, k), miroir);
        s.coller(i1, 0, 0, 2);
        t.coller(s, x, y);
    }

    /** Plaque de cuisson en acier vue de trois-quarts. */
    static void plaque(Toile t, int x, int y, int w, int h) {
        Dessin.ombre(t, x + w / 2.0 + 6, y + h + 8, w / 2.0 + 6, 6);
        int[] acier = Dessin.rampe(0xFF5A5E66);
        for (int j = 0; j < h; j++) {
            int off = (int) Math.round(12.0 * (h - 1 - j) / (h - 1));
            for (int i = 0; i < w; i++) t.set(x + off + i, y + j, Dessin.bande(acier, 0.4 + 0.35 * j / h + ((i + j * 3) % 17 == 0 ? 0.1 : 0), x + i, y + j));
        }
        t.rect(x, y + h, w, 7, acier[1]);
        t.rect(x, y + h, w, 1, acier[4]);
        int C = Dessin.CONTOUR;
        t.ligne(x - 1, y + h, x + 11, y - 1, C);
        t.ligneH(x + 11, x + w + 12, y - 1, C);
        t.ligne(x + w + 12, y - 1, x + w, y + h, C);
        t.ligneH(x - 1, x + w, y + h + 7, C);
        t.ligneV(x - 1, y + h, y + h + 7, C);
        t.ligneV(x + w, y + h, y + h + 7, C);
    }

    static void bruleur(Toile t, double cx, double cy, double r, int feu, int max, int tms, boolean gaz) {
        t.ellipse(cx, cy, r + 3, (r + 3) * 0.36, 0xFF1A1B1E);
        t.anneau(cx, cy, r + 1, (r + 1) * 0.36, 2, 0xFF2E3036);
        t.ellipse(cx, cy, r * 0.35, r * 0.13, 0xFFB8902A);
        if (feu > 0) for (int k = 0; k < 14; k++) {
            double a = k / 14.0 * Math.PI * 2;
            Dessin.flammes(t, cx + Math.cos(a) * r, cy + Math.sin(a) * r * 0.36 + 1, 3, feu / (double) max, tms + k * 37, gaz);
        }
    }

    static final String[] FEU5 = {"OFF", "1", "2", "3", "4"};
    static final String[] FEU7 = {"OFF", "1", "2", "3", "4", "5", "6"};

    // ================================================================== SAISIR
    static void saisir(Toile t, Jeux.Saisir j, Contexte c, double tms) {
        plaque(t, 72, 96, 210, 48);
        double cx = 180, cy = 116;
        bruleur(t, cx, cy + 4, 34, j.feu, 4, (int) tms, true);
        // poêle en fonte
        Toile p = new Toile(Jeux.W, Jeux.H);
        int[] fonte = Dessin.rampe(0xFF3A3C42);
        p.trait(cx + 50, cy - 2, cx + 104, cy - 16, 7, 0xFF26272B);
        p.trait(cx + 52, cy - 4, cx + 102, cy - 17, 2, 0xFF4A4C52);
        p.ellipse(cx, cy + 3, 56, 22, fonte[1]);
        p.ellipse(cx, cy, 56, 21, fonte[3]);
        Dessin.ellipseV(p, cx, cy + 1, 51, 18, fonte, 0.25, 0.7);
        p.contour(Dessin.CONTOUR);
        t.coller(p, 0, 0);
        // huile qui brille
        t.ellipse(cx - 16, cy - 4, 18, 5, Toile.alpha(0xFFE8C040, 0.35));
        // l'aliment : saute et se retourne
        double age = tms - j.tRetourne, saut = Scenes.bosse(age, 300) * 26;
        boolean miroir = age >= 0 && age < 150;
        double cuis = j.fini ? j.couleurFace[1] : j.cuisson;
        if (age >= 0 && age < 150 && j.face > 0) cuis = j.couleurFace[j.face - 1];
        aliment(t, c.icone, (int) cx - 32, (int) (cy - 44 - saut), cuis, miroir);
        // grésillement et fumée
        int n = j.feu * 5;
        for (int k = 0; k < n; k++) {
            int h = Scenes.hash(k * 131 + (long) (tms / 70));
            double a = Math.floorMod(h, 628) / 100.0, d = 30 + Math.floorMod(h >> 8, 16);
            t.rect((int) (cx + Math.cos(a) * d), (int) (cy + Math.sin(a) * d * 0.4 - Math.floorMod(h >> 14, 8)), 1, 1, 0xFFFFF4C0);
        }
        fumee(t, cx, cy - 30, j.fumee + (j.cuisson > 0.95 ? 1 : 0), tms, 0xFF8A8A90);
        Dessin.bandeFeu(t, 5, j.feu, 3, FEU5, Jeux.FEU_X0, Jeux.FEU_X1, Jeux.FEU_Y);
    }

    static void fumee(Toile t, double cx, double cy, double force, double tms, int couleur) {
        if (force <= 0.02) return;
        for (int k = 0; k < 5; k++) {
            double ph = ((tms / 1400.0) + k * 0.2) % 1.0;
            double x = cx - 20 + k * 10 + Math.sin(ph * 6 + k) * 5, y = cy - ph * 40;
            double r = 4 + ph * 7;
            t.ellipse(x, y, r, r * 0.8, Toile.alpha(couleur, Math.min(1, force) * 0.55 * (1 - ph)));
        }
    }

    // ================================================================== BOUILLIR
    static void bouillir(Toile t, Jeux.Chauffe j, Contexte c, double tms) {
        plaque(t, 72, 110, 210, 36);
        double cx = 180, cy = 70;
        bruleur(t, cx, 128, 36, j.feu, 6, (int) tms, true);
        casserole(t, cx, cy, 50, 16, 58);
        int eau = Toile.melange(0xFF8AB8D8, c.couleurMelange, 0.35);
        double temp = j.temp;
        int[] r = Dessin.rampe(eau);
        Toile s = new Toile(Jeux.W, Jeux.H);
        Dessin.ellipseV(s, cx, cy + 2, 45, 12, r, 0.95, 0.6);
        int bulles = (int) (Math.max(0, temp - 0.25) * 60);
        for (int k = 0; k < bulles; k++) {
            int h = Scenes.hash(k * 97 + (long) (tms / (160 - temp * 100)));
            double a = Math.floorMod(h, 628) / 100.0, d = Math.floorMod(h >> 8, 90) / 100.0;
            double x = cx + Math.cos(a) * 42 * d, y = cy + 2 + Math.sin(a) * 10 * d;
            s.anneau(x, y, 2, 1.5, 1, r[4]);
        }
        for (int k = 0; k < 5; k++) {
            int h = Scenes.hash(k * 3 + 11);
            s.rect((int) cx - 30 + Math.floorMod(h, 60), (int) cy - 2 + Math.floorMod(h >> 8, 8), 3, 1, c.couleursBouts[k % c.couleursBouts.length]);
        }
        t.coller(s, 0, 0);
        bordCasserole(t, cx, cy, 50, 16);
        if (temp > j.rouge - 0.02) {
            for (int k = 0; k < 12; k++) t.disque(cx - 44 + k * 8, cy + 14 + (k % 3), 3, 0xFFF4F8FC);
        }
        fumee(t, cx, cy - 12, (temp - 0.4) * 1.6, tms, 0xFFF0F4F8);
        Dessin.bandeFeu(t, 7, j.feu, -1, FEU7, Jeux.FEU_X0, Jeux.FEU_X1, Jeux.FEU_Y);
    }

    static void casserole(Toile t, double cx, double cy, double rx, double ry, double h) {
        Toile s = new Toile(Jeux.W, Jeux.H);
        int[] inox = Dessin.rampe(0xFFB9BEC6);
        for (int x = (int) (cx - rx); x <= cx + rx; x++) {
            double nx = (x - cx) / rx;
            double L = 0.55 + 0.4 * Math.sin((nx + 0.3) * 2.2) - 0.1;
            int y0 = (int) cy, y1 = (int) (cy + h + ry * Math.sqrt(Math.max(0, 1 - nx * nx)));
            for (int y = y0; y <= y1; y++) s.set(x, y, Dessin.bande(inox, L, x, y));
        }
        for (int sgn = -1; sgn <= 1; sgn += 2) {
            s.rect((int) (cx + sgn * (rx + 2)) - 5, (int) cy + 8, 10, 5, 0xFF26272B);
            s.rect((int) (cx + sgn * (rx + 2)) - 4, (int) cy + 9, 8, 2, 0xFF4A4C52);
        }
        s.ellipse(cx, cy, rx, ry, inox[3]);
        s.ellipse(cx, cy + 1, rx - 3, ry - 2, inox[1]);
        s.contour(Dessin.CONTOUR);
        t.coller(s, 0, 0);
    }

    static void bordCasserole(Toile t, double cx, double cy, double rx, double ry) {
        for (int x = (int) (cx - rx); x <= cx + rx; x++) {
            double nx = (x - cx) / rx;
            if (Math.abs(nx) > 1) continue;
            int y = (int) Math.round(cy + ry * Math.sqrt(1 - nx * nx));
            t.set(x, y - 1, 0xFFE8ECF0);
            t.set(x, y - 2, 0xFFB9BEC6);
        }
    }

    // ================================================================== MIJOTER
    static void mijoter(Toile t, Jeux.Chauffe j, Contexte c, double tms) {
        plaque(t, 72, 116, 210, 32);
        double cx = 180, cy = 78;
        bruleur(t, cx, 132, 34, j.feu, 6, (int) tms, true);
        // marmite en fonte
        Toile s = new Toile(Jeux.W, Jeux.H);
        int[] fonte = Dessin.rampe(0xFF2E323A);
        for (int x = (int) (cx - 56); x <= cx + 56; x++) {
            double nx = (x - cx) / 56;
            int y1 = (int) (cy + 44 + 16 * Math.sqrt(Math.max(0, 1 - nx * nx)));
            for (int y = (int) cy; y <= y1; y++) s.set(x, y, Dessin.bande(fonte, 0.6 - nx * 0.35 - (y - cy) * 0.004, x, y));
        }
        for (int sgn = -1; sgn <= 1; sgn += 2) s.anneau(cx + sgn * 60, cy + 12, 7, 6, 2.5, fonte[2]);
        s.ellipse(cx, cy, 56, 18, fonte[3]);
        int[] r = Dessin.rampe(c.couleurMelange);
        Dessin.ellipseV(s, cx, cy + 2, 50, 14, r, 0.9, 0.5);
        for (int k = 0; k < 16; k++) {
            int h = Scenes.hash(k * 37 + 5);
            double a = Math.floorMod(h, 628) / 100.0, d = Math.floorMod(h >> 8, 85) / 100.0;
            double x = cx + Math.cos(a + tms / 9000.0) * 46 * d, y = cy + 2 + Math.sin(a + tms / 9000.0) * 12 * d;
            int col = c.couleursBouts[k % c.couleursBouts.length];
            Dessin.sphere(s, x, y, 3, 2, Dessin.rampe(col), 1);
        }
        // bulles lentes qui éclatent
        for (int k = 0; k < 1 + (int) (j.temp * 6); k++) {
            double ph = ((tms / 900.0) + k * 0.37) % 1.0;
            int h = Scenes.hash(k * 7 + (long) ((tms / 900.0) + k * 0.37));
            double x = cx - 40 + Math.floorMod(h, 80), y = cy + Math.floorMod(h >> 8, 10);
            s.anneau(x, y, 1.5 + ph * 3, 1 + ph * 2, 1, r[4]);
        }
        s.contour(Dessin.CONTOUR);
        t.coller(s, 0, 0);
        // cuillère en bois : remue quand on clique
        double remue = 0;
        for (MiniJeu.Retour rt : j.retours) if (rt.texte.startsWith("REMU")) remue = Math.max(remue, Scenes.bosse(tms - rt.t, 500));
        double ang = remue * Math.PI * 2;
        double ux = cx + 10 + Math.cos(ang) * 22 * remue, uy = cy + Math.sin(ang) * 6 * remue;
        Toile cu = new Toile(Jeux.W, Jeux.H);
        cu.trait(ux, uy, ux + 30, uy - 56, 4, 0xFF8A5A2A);
        cu.trait(ux, uy, ux + 30, uy - 56, 1.3, 0xFFB88A52);
        cu.contour(Dessin.CONTOUR);
        t.coller(cu, 0, 0);
        // alerte : ça attache !
        if (j.alerte >= 0) {
            double reste = 1 - (j.t - j.alertes[j.alerte]) / (double) j.reaction;
            fumee(t, cx, cy - 6, 1, tms, 0xFF3A3434);
            for (int x = (int) cx - 50; x < cx + 50; x += 3) t.set(x, (int) (cy + 55 + 8 * Math.sqrt(Math.max(0, 1 - Math.pow((x - cx) / 56.0, 2)))), 0xFFFF6A2A);
            int bx = (int) cx + 52, by = (int) cy - 34;
            t.disque(bx, by, 14, Dessin.CONTOUR);
            t.disque(bx, by, 12.5, 0xFFE0402A);
            for (int a = 0; a < 360; a += 5) {
                if (a / 360.0 > reste) break;
                double an = Math.toRadians(a - 90);
                t.rect((int) (bx + Math.cos(an) * 11), (int) (by + Math.sin(an) * 11), 1, 1, 0xFFFFE0A0);
            }
            Police.centre(t, "!", bx + 1, by - 7, Dessin.BLANC, Dessin.CONTOUR, 2);
            Police.centre(t, "REMUE !", bx, by + 16, Dessin.ROUGE_TEXTE, Dessin.CONTOUR, 1);
        }
        fumee(t, cx - 20, cy - 14, (j.temp - 0.2) * 1.2, tms, 0xFFF0F4F8);
        Dessin.bandeFeu(t, 7, j.feu, 2, FEU7, Jeux.FEU_X0, Jeux.FEU_X1, Jeux.FEU_Y);
    }

    // ================================================================== FOUR
    static void four(Toile t, Jeux.Four j, Contexte c, double tms) {
        int x0 = 84, x1 = 272, y0 = 38, y1 = 152;
        Dessin.ombre(t, (x0 + x1) / 2.0 + 6, y1 + 4, (x1 - x0) / 2.0 + 6, 6);
        Toile s = new Toile(Jeux.W, Jeux.H);
        int[] acier = Dessin.rampe(0xFFB9BEC6);
        Dessin.degradeH(s, x0, y0, x1 - x0, y1 - y0, acier, 0.9, 0.5);
        for (int k = 0; k < 40; k++) {
            int h = Scenes.hash(k * 5 + 1);
            s.rect(x0 + Math.floorMod(h, x1 - x0 - 20), y0 + Math.floorMod(h >> 8, y1 - y0), 10 + Math.floorMod(h >> 16, 16), 1, acier[3]);
        }
        // bandeau : afficheur de température
        s.rect(x0 + 6, y0 + 5, x1 - x0 - 12, 20, 0xFF16181C);
        s.rect(x0 + 6, y0 + 5, x1 - x0 - 12, 1, 0xFF3A3D44);
        String deg = Jeux.Four.DEGRES[j.thermostat];
        s.rect((x0 + x1) / 2 - 24, y0 + 8, 48, 14, 0xFF0A0506);
        Police.centre(s, deg, (x0 + x1) / 2, y0 + 8, 0xFFFF8A1A, 0, 1);
        s.disque(x0 + 22, y0 + 15, 7, 0xFF26272B);
        s.disque(x1 - 22, y0 + 15, 7, 0xFF26272B);
        double ang = -2.4 + j.thermostat * 0.8;
        s.ligne(x0 + 22, y0 + 15, x0 + 22 + Math.cos(ang) * 5, y0 + 15 + Math.sin(ang) * 5, 0xFFFFFFFF);
        s.rect(x0 + 46, y0 + 14, 3, 3, j.thermostat > 0 ? 0xFFFF3A2A : 0xFF4A1A1A);
        // porte vitrée
        int dx0 = x0 + 10, dx1 = x1 - 10, dy0 = y0 + 32, dy1 = y1 - 8;
        s.rect(dx0, dy0, dx1 - dx0, dy1 - dy0, acier[4]);
        s.rect(dx0 + 1, dy0 + 1, dx1 - dx0 - 2, dy1 - dy0 - 2, acier[2]);
        int wx0 = dx0 + 12, wx1 = dx1 - 12, wy0 = dy0 + 14, wy1 = dy1 - 8;
        double ch = j.chaleur;
        for (int y = wy0; y < wy1; y++) {
            double k = (y - wy0) / (double) (wy1 - wy0);
            s.rect(wx0, y, wx1 - wx0, 1, Toile.melange(0xFF120A08, Toile.melange(0xFF3A1A0C, 0xFFB8501A, ch), 0.25 + k * 0.75));
        }
        s.rect(wx0 + 4, wy1 - 5, wx1 - wx0 - 8, 2, Toile.melange(0xFF3A1A12, 0xFFFF7A2A, ch));
        s.rect(wx0 + 4, wy0 + 3, wx1 - wx0 - 8, 1, Toile.melange(0xFF3A1A12, 0xFFFF7A2A, ch * 0.8));
        int gy = wy1 - 16;
        s.rect(wx0 + 2, gy, wx1 - wx0 - 4, 1, 0xFF6A5A52);
        for (int x = wx0 + 6; x < wx1 - 4; x += 8) s.rect(x, gy - 1, 1, 3, 0xFF6A5A52);
        s.rect(wx0, wy0, wx1 - wx0, 1, 0xFF050303);
        s.rect(wx0, wy0, 1, wy1 - wy0, 0xFF050303);
        s.contour(Dessin.CONTOUR);
        t.coller(s, 0, 0);
        // le plat sur la grille
        aliment(t, c.iconePlat, (x0 + x1) / 2 - 32, gy - 50, j.sorti ? j.cuisson : j.cuisson, false);
        // reflets du hublot
        t.ligne(wx0 + 6, wy1 - 2, wx0 + 26, wy0 + 2, 0x40FFFFFF);
        t.ligne(wx0 + 10, wy1 - 2, wx0 + 30, wy0 + 2, 0x30FFFFFF);
        // poignée
        Toile po = new Toile(Jeux.W, Jeux.H);
        po.rect(dx0 + 14, dy0 + 5, dx1 - dx0 - 28, 4, 0xFFE8ECF0);
        po.rect(dx0 + 14, dy0 + 5, dx1 - dx0 - 28, 1, 0xFFFFFFFF);
        po.rect(dx0 + 16, dy0 + 8, 3, 3, 0xFFC0C4CC);
        po.rect(dx1 - 19, dy0 + 8, 3, 3, 0xFFC0C4CC);
        po.contour(Dessin.CONTOUR);
        t.coller(po, 0, 0);
        fumee(t, (x0 + x1) / 2.0, y0 - 2, j.cuisson > 0.95 ? (j.cuisson - 0.9) * 5 : 0, tms, 0xFF4A4448);
        Dessin.bandeFeu(t, 7, j.thermostat, j.ideal, Jeux.Four.DEGRES, Jeux.FEU_X0, Jeux.FEU_X1, Jeux.FEU_Y);
    }

    // ================================================================== GRILLER
    static void griller(Toile t, Jeux.Griller j, Contexte c, double tms, int mx, int my) {
        int x0 = 88, x1 = 292, y0 = 62, y1 = 150;
        Dessin.ombre(t, (x0 + x1) / 2.0 + 6, y1 + 10, (x1 - x0) / 2.0 + 6, 6);
        Toile s = new Toile(Jeux.W, Jeux.H);
        // cuve et braises
        s.rect(x0 - 4, y0 - 4, x1 - x0 + 8, y1 - y0 + 16, 0xFF26282C);
        s.rect(x0 - 4, y1 + 6, x1 - x0 + 8, 6, 0xFF1A1B1E);
        for (int y = y0; y < y1; y += 3)
            for (int x = x0; x < x1; x += 4) {
                int h = Scenes.hash(x * 31 + y * 7);
                double fl = 0.5 + 0.5 * Math.sin(tms / 300.0 + (h & 63));
                int base = Toile.melange(0xFF2A1410, 0xFF5A2418, (h & 7) / 8.0);
                s.rect(x + Math.floorMod(h >> 4, 2), y, 3, 2, base);
                if ((h & 3) == 0) s.rect(x + 1, y, 2, 1, Toile.melange(0xFFE8501A, 0xFFFFB040, fl));
            }
        // grille
        for (int x = x0; x < x1; x += 7) {
            s.rect(x, y0, 2, y1 - y0, 0xFF3A3C42);
            s.rect(x, y0, 1, y1 - y0, 0xFF6E727A);
        }
        s.rect(x0, y0, x1 - x0, 2, 0xFF6E727A);
        s.rect(x0, y1 - 2, x1 - x0, 2, 0xFF3A3C42);
        s.contour(Dessin.CONTOUR);
        t.coller(s, 0, 0);
        for (int i = 0; i < j.n; i++) {
            if (j.t < j.depart[i]) continue;
            int[] ic = c.iconesGrill.isEmpty() ? null : c.iconesGrill.get(i % c.iconesGrill.size());
            int px = j.x[i], py = j.y[i];
            boolean fini = j.faces[i] >= 2;
            double cuis = fini ? j.marque[i][1] : j.cuisson[i];
            Toile pc = new Toile(40, 40);
            if (ic != null) {
                int teinte = cuis < 0.75 ? CRU : BRULE;
                double k = cuis < 0.75 ? (0.75 - cuis) / 0.75 * 0.4 : Math.max(0, cuis - 0.85) / 0.25 * 0.85;
                pc.collerTeinte(ic, 32, 32, 4, 4, teinte, Math.min(0.9, k), false);
            }
            // marques de grill après le premier retournement
            if (j.faces[i] >= 1 || fini)
                for (int m = 0; m < 3; m++)
                    for (int d = 0; d < 20; d++) {
                        int xx = 10 + m * 8 + d / 3, yy = 10 + d;
                        if ((pc.get(xx, yy) >>> 24) != 0) pc.poser(xx, yy, 0xFF2A140A);
                    }
            t.coller(pc, px - 20, py - 20);
            // mini-jauge
            if (!fini) {
                int bx = px - 16, by = py - 26;
                t.rect(bx - 1, by - 1, 34, 6, Dessin.CONTOUR);
                t.rect(bx, by, 32, 4, 0xFF2A1508);
                int lo = (int) (j.zoneLo / 1.08 * 32), hi = (int) (j.zoneHi / 1.08 * 32);
                t.rect(bx + lo, by, hi - lo, 4, Dessin.GD);
                int f = (int) Math.min(32, j.cuisson[i] / 1.08 * 32);
                boolean pret = j.cuisson[i] >= j.zoneLo && j.cuisson[i] <= j.zoneHi;
                t.rect(bx, by + 1, f, 2, pret ? Dessin.GH : j.cuisson[i] > j.zoneHi ? Dessin.RG : Dessin.OR);
                Police.texte(t, String.valueOf(i + 1), bx - 8, by - 3, Dessin.CREME, Dessin.CONTOUR, 1);
                if (pret && ((int) (tms / 150)) % 2 == 0) t.anneau(px, py, 21, 14, 1, Dessin.GH);
            } else {
                Police.centre(t, "OK", px, py - 30, Dessin.VERT_TEXTE, Dessin.CONTOUR, 1);
            }
            if (!fini && j.cuisson[i] > 0.3) fumee(t, px, py - 14, (j.cuisson[i] - 0.3) * 0.8, tms + i * 300, 0xFFB8B8BE);
        }
    }

    // ================================================================== FRIRE
    static void frire(Toile t, Jeux.Frire j, Contexte c, double tms) {
        int x0 = 96, x1 = 264, y0 = 96, y1 = 158;
        Dessin.ombre(t, (x0 + x1) / 2.0 + 6, y1 + 4, (x1 - x0) / 2.0 + 6, 6);
        // arrière de la cuve et bain d'huile
        Toile fond = new Toile(Jeux.W, Jeux.H);
        int[] acier = Dessin.rampe(0xFFB9BEC6);
        fond.rect(x0 + 4, y0 - 12, x1 - x0 - 8, 14, acier[1]);
        fond.contour(Dessin.CONTOUR);
        t.coller(fond, 0, 0);
        double oy = y0 - 4;
        for (int x = x0 + 8; x < x1 - 8; x++)
            for (int y = (int) oy - 5; y < oy + 4; y++) t.set(x, y, Toile.melange(0xFFF4C848, 0xFFC8801A, (y - oy + 5) / 9.0));
        // panier : en haut, plongé, puis relevé et secoué
        double basketY;
        if (j.phase == 0) basketY = 34;
        else if (j.phase == 1) basketY = 34 + Math.min(1, (j.t) / 300.0) * 42;
        else {
            double age = tms - j.tEgoutte + 300;
            basketY = 76 - Math.min(1, age / 250.0) * 42;
            basketY -= Scenes.bosse(tms - j.tSecousse, 160) * 6;
        }
        double cuis = j.phase == 2 ? j.dorureFinale : j.dorure;
        int bx0 = 138, bx1 = 222, by0 = (int) basketY, by1 = (int) basketY + 44;
        Toile p = new Toile(Jeux.W, Jeux.H);
        p.rect(bx0, by0, bx1 - bx0, by1 - by0, 0x55303238);
        p.contour(Dessin.CONTOUR);
        t.coller(p, 0, 0);
        aliment(t, c.iconePlat, (bx0 + bx1) / 2 - 32, by1 - 60, 0.3 + cuis * 0.95, false);
        Toile m = new Toile(Jeux.W, Jeux.H);
        for (int x = bx0; x <= bx1; x += 4) m.ligne(x, by0, x, by1, 0xFFC0C6CE);
        for (int y = by0; y <= by1; y += 4) m.ligneH(bx0, bx1, y, 0xFF9AA0A8);
        m.rect(bx0 - 1, by0 - 2, bx1 - bx0 + 3, 3, 0xFFE8ECF0);
        m.trait(bx1, by0, bx1 + 50, by0 - 16, 4, 0xFF26272B);
        m.trait(bx1, by0 - 1, bx1 + 50, by0 - 17, 1.3, 0xFF4A4C52);
        m.contour(Dessin.CONTOUR);
        t.coller(m, 0, 0);
        // huile par-dessus quand le panier est plongé, bulles qui crépitent
        if (j.phase == 1 && basketY > 60) {
            for (int x = bx0 - 2; x <= bx1 + 2; x++) for (int y = (int) oy - 5; y < oy + 4; y++) t.set(x, y, Toile.alpha(0xFFE8B030, 0.8));
            for (int k = 0; k < 30; k++) {
                int h = Scenes.hash(k * 13 + (long) (tms / 80));
                t.anneau(bx0 + Math.floorMod(h, bx1 - bx0), oy - 4 + Math.floorMod(h >> 8, 6), 2, 1.5, 1, 0xFFFFF0A0);
            }
            fumee(t, 180, oy - 8, 0.6, tms, 0xFFF0F4F8);
        }
        // façade de la friteuse, devant le panier plongé
        Toile s = new Toile(Jeux.W, Jeux.H);
        Dessin.degradeH(s, x0, y0, x1 - x0, y1 - y0, acier, 0.9, 0.45);
        s.rect(x0, y0, x1 - x0, 2, acier[4]);
        s.rect(x0 + 10, y0 + 22, 60, 22, 0xFF2A2C32);
        s.rect(x0 + 14, y0 + 26, 4, 4, j.phase == 1 ? 0xFFFF3A2A : 0xFF5A1A1A);
        s.rect(x0 + 14, y0 + 34, 4, 4, 0xFF3AFF6A);
        Police.texte(s, "180°", x0 + 24, y0 + 27, 0xFFFF8A1A, 0, 1);
        for (int k = 0; k < 6; k++) s.rect(x0 + 90 + k * 10, y0 + 30, 6, 1, acier[1]);
        s.contour(Dessin.CONTOUR);
        t.coller(s, 0, 0);
        // égouttage : gouttes d'huile
        if (j.phase == 2) {
            for (int k = 0; k < 10; k++) {
                double ph = ((tms / 500.0) + k * 0.13) % 1.0;
                t.rect(bx0 + 6 + k * 8, (int) (by1 + ph * 16), 1, 2, Toile.alpha(0xFFE8B030, 1 - ph));
            }
            Dessin.souris(t, 272, 60, 0, ((int) (tms / 150)) % 2 == 0);
            Police.centre(t, "SECOUE !", 278, 82, Dessin.OR_TEXTE, Dessin.CONTOUR, 1);
            Police.texte(t, j.egoutte + "/" + j.secousses, 266, 92, Dessin.CREME, Dessin.CONTOUR, 1);
        } else if (j.phase == 0) {
            Police.centre(t, "CLIC : PLONGER", 180, 20 + 150, Dessin.CREME, Dessin.CONTOUR, 1);
        }
    }
}
