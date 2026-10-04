package fr.lenerfvoeux.hxrp.metiers.client.minijeu;

import fr.lenerfvoeux.hxrp.metiers.minijeu.Jeux;

/** SECOUER, PRESSER, ASSAISONNER : shaker et métronome, presse-agrumes, doseur. */
final class Bar {
    private Bar() {}

    /** Comptoir en bois sombre (dessus + chant). */
    static void comptoir(Toile t, int y) {
        int[] bois = Dessin.rampe(0xFF6A4226);
        Dessin.degradeV(t, 50, y, 262, 26, bois, 0.85, 0.45);
        for (int k = 0; k < 30; k++) {
            int h = Scenes.hash(k * 17 + 3);
            t.rect(54 + Math.floorMod(h, 240), y + 2 + Math.floorMod(h >> 8, 20), 6 + Math.floorMod(h >> 16, 14), 1, bois[1]);
        }
        t.rect(50, y + 26, 262, 8, bois[0]);
        t.rect(50, y, 262, 1, bois[4]);
        t.rect(49, y - 1, 264, 1, Dessin.CONTOUR);
        t.rect(49, y + 34, 264, 1, Dessin.CONTOUR);
        t.rect(49, y, 1, 34, Dessin.CONTOUR);
        t.rect(312, y, 1, 34, Dessin.CONTOUR);
    }

    // ================================================================== SECOUER
    static void secouer(Toile t, Jeux.Rythme j, Contexte c, double tms) {
        comptoir(t, 132);
        // métronome : le balancier touche un bord à chaque temps
        int mx = 94, my = 132;
        Toile m = new Toile(Jeux.W, Jeux.H);
        int[] bois = Dessin.rampe(0xFF8A5530);
        m.polygone(new double[]{mx - 20, mx + 20, mx + 8, mx - 8}, new double[]{my, my, my - 58, my - 58}, bois[2]);
        m.polygone(new double[]{mx - 20, mx - 8, mx - 8, mx - 14}, new double[]{my, my - 58, my - 58, my}, bois[3]);
        m.rect(mx - 7, my - 50, 14, 38, 0xFFF4ECD8);
        for (int k = 0; k < 6; k++) m.rect(mx - 3, my - 46 + k * 6, 6, 1, 0xFF8A7A6A);
        double phase = ((tms - j.avance) / j.periode);
        double ang = Math.sin(phase * Math.PI + Math.PI / 2) * 0.55;
        double bx = mx + Math.sin(ang) * 46, by = my - 14 - Math.cos(ang) * 46;
        m.trait(mx, my - 14, bx, by, 2.5, 0xFFD4A23A);
        m.rect((int) (mx + Math.sin(ang) * 30) - 3, (int) (my - 14 - Math.cos(ang) * 30) - 2, 7, 5, 0xFFD4A23A);
        m.disque(mx, my - 14, 3, 0xFF6A4020);
        m.contour(Dessin.CONTOUR);
        t.coller(m, 0, 0);
        double depuisTemps = Math.abs(phase - Math.round(phase)) * j.periode;
        if (depuisTemps < 70 && phase > -0.1) t.disque(mx, my - 64, 4, Dessin.OR_TEXTE);
        // shaker : monte et descend à chaque clic
        double age = tms - j.tFrappe, dy = Scenes.bosse(age, 180) * 20;
        int sx = 190, sy = (int) (84 - dy);
        Toile s = new Toile(Jeux.W, Jeux.H);
        int[] inox = Dessin.rampe(0xFFC8CCD2);
        for (int x = sx - 16; x <= sx + 16; x++) {
            double nx = (x - sx) / 16.0;
            double L = 0.6 + 0.4 * Math.sin((nx + 0.4) * 2.6) - 0.1;
            for (int y = sy; y < sy + 46; y++) s.set(x, y, Dessin.bande(inox, L, x, y));
        }
        for (int x = sx - 12; x <= sx + 12; x++) {
            double nx = (x - sx) / 12.0;
            double L = 0.6 + 0.4 * Math.sin((nx + 0.4) * 2.6) - 0.1;
            for (int y = sy - 18; y < sy; y++) s.set(x, y, Dessin.bande(inox, L, x, y));
        }
        s.rect(sx - 5, sy - 26, 10, 8, inox[3]);
        s.rect(sx - 16, sy - 1, 33, 3, inox[1]);
        s.rect(sx - 16, sy + 18, 33, 1, inox[2]);
        s.contour(Dessin.CONTOUR);
        t.coller(s, 0, 0);
        if (age >= 0 && age < 220)
            for (int k = 0; k < 6; k++) t.rect(sx - 20 + k * 8, sy - 30 - (int) (Scenes.bosse(age, 220) * 10) + (k % 2) * 4, 3, 3, 0xFFDFF4FB);
        // verre qui se remplit
        double plein = j.fait / (double) j.total;
        verre(t, 262, 102, 20, 30, c.couleurMelange, plein);
        Bol.rangeeResultats(t, j.resultats, j.cible, 188, 172);
    }

    static void verre(Toile t, int cx, int y, int w, int h, int liquide, double niveau) {
        Toile s = new Toile(Jeux.W, Jeux.H);
        int x0 = cx - w / 2;
        s.rect(x0, y, w, h, 0xFFCFE6EE);
        int ly = (int) (y + h - 2 - (h - 4) * niveau);
        int[] r = Dessin.rampe(liquide);
        if (niveau > 0.01) Dessin.degradeH(s, x0 + 1, ly, w - 2, y + h - 1 - ly, r, 0.85, 0.35);
        s.rect(x0 + 2, y + 2, 1, h - 4, 0xFFFFFFFF);
        s.rect(x0, y, w, 1, 0xFFF4FBFD);
        s.contour(Dessin.CONTOUR);
        t.coller(s, 0, 0);
    }

    // ================================================================== PRESSER
    static void presser(Toile t, Jeux.Presser j, Contexte c, double tms) {
        comptoir(t, 138);
        int cx = 168;
        // coupelle et cône
        Toile s = new Toile(Jeux.W, Jeux.H);
        int[] email = Dessin.rampe(0xFFE87A22);
        Dessin.degradeH(s, cx - 46, 112, 92, 16, email, 0.9, 0.4);
        s.ellipse(cx, 112, 46, 10, email[3]);
        s.ellipse(cx, 113, 40, 7, Toile.melange(c.chair, 0xFFF8A020, 0.3));
        s.rect(cx + 44, 116, 12, 5, email[2]);
        s.contour(Dessin.CONTOUR);
        t.coller(s, 0, 0);
        // demi-fruit écrasé par la pression
        double p = j.valeur;
        double ry = 20 - p * 9, rx = 26 + p * 5;
        Toile f = new Toile(Jeux.W, Jeux.H);
        int[] peau = Dessin.rampe(c.peau), chair = Dessin.rampe(c.chair);
        for (int y = (int) (106 - ry); y <= 106; y++)
            for (int x = (int) (cx - rx); x <= cx + rx; x++) {
                double nx = (x - cx) / rx, ny = (y - 106) / ry;
                if (nx * nx + ny * ny > 1) continue;
                double nz = Math.sqrt(Math.max(0, 1 - nx * nx - ny * ny));
                f.set(x, y, Dessin.bande(peau, 0.5 - nx * 0.35 - ny * 0.2 + nz * 0.2, x, y));
            }
        f.ellipse(cx, 106, rx, 3, chair[2]);
        for (int k = 0; k < 12; k++) f.set((int) (cx - rx + 4 + k * (rx * 2 - 8) / 11), 106 - (k % 2), chair[4]);
        f.contour(Dessin.CONTOUR);
        t.coller(f, 0, 0);
        // main qui appuie
        int hy = (int) (106 - ry - 18);
        Toile m = new Toile(Jeux.W, Jeux.H);
        int[] ma = Dessin.rampe(0xFFE8B890);
        m.ellipse(cx, hy + 6, 22, 9, ma[2]);
        for (int k = 0; k < 4; k++) m.ellipse(cx - 13 + k * 9, hy + 13, 4.5, 4, ma[3]);
        m.rect(cx - 10, hy - 18, 20, 20, ma[1]);
        m.rect(cx - 12, hy - 24, 24, 8, 0xFF3A6AB0);
        m.contour(Dessin.CONTOUR);
        t.coller(m, 0, 0);
        // filet de jus vers le verre et giclées
        double eff = p >= j.zoneLo && p <= j.zoneHi ? 1 : p > 0.15 && p < j.rouge ? 0.5 : 0;
        int gx = 236;
        verre(t, gx, 100, 26, 44, c.chair, Math.min(1, j.jus));
        if (eff > 0) {
            for (int k = 0; k < 12; k++) {
                double ph = ((tms / 240.0) + k / 12.0) % 1.0;
                double x = cx + 56 + ph * (gx - cx - 56), y = 118 - Math.sin(ph * Math.PI) * 6 + ph * 2;
                t.rect((int) x, (int) y, eff >= 1 ? 2 : 1, 2, chair_(c));
            }
        }
        if (p >= j.rouge) {
            for (int k = 0; k < 14; k++) {
                int h = Scenes.hash(k * 29 + (long) (tms / 60));
                double a = Math.floorMod(h, 314) / 100.0 + 3.14, d = 20 + Math.floorMod(h >> 8, 30);
                t.rect((int) (cx + Math.cos(a) * d), (int) (100 + Math.sin(a) * d * 0.6), 2, 2, chair_(c));
            }
        }
        Dessin.souris(t, 70, 116, 0, j.tenu);
        Police.centre(t, "MAINTENIR", 76, 138, j.tenu ? Dessin.OR_TEXTE : Dessin.CREME, Dessin.CONTOUR, 1);
    }

    private static int chair_(Contexte c) {
        return Toile.melange(c.chair, 0xFFF8B030, 0.2);
    }

    // ================================================================== ASSAISONNER
    static void assaisonner(Toile t, Jeux.Assaisonner j, Contexte c, double tms) {
        // le plat à assaisonner
        Feu.aliment(t, c.iconePlat, 132, 104, 0.75, false);
        String id = c.condiments.get(Math.min(j.dose, j.total - 1) % c.condiments.size());
        boolean verse = j.phase == 1;
        int bx = 176, by = verse ? 58 : 50;
        condiment(t, id, bx, by, verse, tms);
        // ce qui tombe
        if (verse) {
            boolean liquide = liquide(id);
            int col = couleur(id);
            for (int k = 0; k < (liquide ? 16 : 20); k++) {
                double ph = ((tms / (liquide ? 180.0 : 320.0)) + k / 16.0) % 1.0;
                int h = Scenes.hash(k * 7);
                double x = bx - 18 + (liquide ? 0 : Math.floorMod(h, 10) - 5), y = by + 14 + ph * 44;
                t.rect((int) x, (int) y, liquide ? 2 : 1, liquide ? 3 : 1, col);
            }
        }
        // doseur gradué : niveau et trait cible
        int tx = 262, ty = 52, th = 90;
        Toile d = new Toile(Jeux.W, Jeux.H);
        d.rect(tx - 9, ty, 18, th, 0xFFCFE6EE);
        int ly = (int) (ty + th - 2 - (th - 4) * j.valeur);
        if (j.valeur > 0) d.rect(tx - 8, ly, 16, ty + th - 1 - ly, couleur(id));
        for (int k = 0; k <= 10; k++) d.rect(tx - 9, ty + 2 + k * (th - 4) / 10, k % 5 == 0 ? 7 : 4, 1, 0xFF4A5A62);
        int cy = (int) (ty + th - 2 - (th - 4) * j.trait);
        d.rect(tx - 12, cy, 24, 1, 0xFFE0402A);
        d.rect(tx + 2, ty + 3, 1, th - 6, 0xFFFFFFFF);
        d.contour(Dessin.CONTOUR);
        t.coller(d, 0, 0);
        Police.texte(t, "CIBLE", tx + 14, cy - 5, Dessin.ROUGE_TEXTE, Dessin.CONTOUR, 1);
        // doses réalisées
        for (int i = 0; i < j.total; i++) {
            String ci = c.condiments.get(i % c.condiments.size());
            int ix = 64, iy = 58 + i * 26;
            t.disque(ix, iy, 9, Dessin.CONTOUR);
            t.disque(ix, iy, 8, i < j.dose ? 0xFF3A7A2A : i == j.dose ? Dessin.OR : 0xFF5A4A3A);
            t.disque(ix, iy, 4, couleur(ci));
            if (i < j.dose) Police.texte(t, "OK", ix + 12, iy - 5, Dessin.VERT_TEXTE, Dessin.CONTOUR, 1);
        }
        Dessin.souris(t, 70, 142, 0, verse);
        Police.centre(t, "MAINTENIR", 76, 164, verse ? Dessin.OR_TEXTE : Dessin.CREME, Dessin.CONTOUR, 1);
    }

    static boolean liquide(String id) {
        return id.contains("huile") || id.contains("vinaigr") || id.contains("sauce") || id.contains("miel") || id.contains("ketchup")
                || id.contains("mayonnaise") || id.contains("moutarde") || id.contains("pesto");
    }

    static int couleur(String id) {
        switch (id) {
            case "sel": case "sucre": return 0xFFFBFBF8;
            case "poivre_noir": return 0xFF2A2420;
            case "huile_d_olive": return 0xFFD8C840;
            case "vinaigre": return 0xFF8A1A2A;
            case "sauce_soja": return 0xFF3A1A0A;
            case "paprika": case "piment": return 0xFFC8321A;
            case "cumin": return 0xFF9A7038;
            case "curcuma": case "safran": return 0xFFF0A818;
            case "miel": return 0xFFE8A018;
            case "ketchup": case "sauce_tomate": return 0xFFD01A1A;
            case "mayonnaise": return 0xFFF8E8A8;
            case "moutarde": return 0xFFE8C030;
            case "vinaigrette": return 0xFFE0C030;
            case "pesto": return 0xFF4A8A22;
            default: return 0xFFC8A070;
        }
    }

    /** Salière, moulin à poivre, bouteille ou pot, penché quand on verse. */
    static void condiment(Toile t, String id, int x, int y, boolean verse, double tms) {
        Toile s = new Toile(Jeux.W, Jeux.H);
        double ang = verse ? -0.9 : 0;
        int col = couleur(id);
        if ("poivre_noir".equals(id)) {
            int[] b = Dessin.rampe(0xFF5A3420);
            Dessin.degradeH(s, x - 8, y - 20, 16, 34, b, 0.9, 0.3);
            s.rect(x - 8, y - 14, 16, 2, 0xFFD4A23A);
            Dessin.sphere(s, x, y - 24, 7, 5, b, 1);
            s.disque(x, y - 30, 3, 0xFFD4A23A);
            if (verse) {
                double a = tms / 120.0;
                s.ligne(x, y - 30, x + Math.cos(a) * 8, y - 30 + Math.sin(a) * 3, 0xFFD4A23A);
            }
        } else {
            boolean liq = liquide(id);
            int[] verreR = Dessin.rampe(liq ? col : 0xFFDCECF2);
            double cx = x, cy = y;
            // corps incliné : dessiné point par point dans le repère de l'objet
            double w = liq ? 9 : 10, h = liq ? 30 : 24;
            for (int yy = -40; yy <= 40; yy++)
                for (int xx = -40; xx <= 40; xx++) {
                    double lx = xx * Math.cos(-ang) - yy * Math.sin(-ang), ly = xx * Math.sin(-ang) + yy * Math.cos(-ang);
                    boolean corps = Math.abs(lx) <= w && ly >= -h / 2 && ly <= h / 2;
                    boolean goulot = liq && Math.abs(lx) <= 3 && ly >= -h / 2 - 9 && ly < -h / 2;
                    boolean bouchon = liq ? Math.abs(lx) <= 4 && ly >= -h / 2 - 13 && ly < -h / 2 - 9 : Math.abs(lx) <= w + 1 && ly >= -h / 2 - 6 && ly < -h / 2;
                    if (bouchon) s.set((int) (cx + xx), (int) (cy + yy), liq ? 0xFFC8A040 : 0xFFC0C4CC);
                    else if (corps || goulot) {
                        double L = 0.85 - (lx + w) / (2 * w) * 0.5;
                        int base = Dessin.bande(verreR, L, xx, yy);
                        if (!liq && ly > -h / 2 + 5) base = Dessin.bande(Dessin.rampe(col), L, xx, yy);
                        s.set((int) (cx + xx), (int) (cy + yy), base);
                    }
                }
            if (!liq) for (int k = -1; k <= 1; k++) {
                double lx = k * 4, ly = -h / 2 - 3;
                s.set((int) (cx + lx * Math.cos(ang) - ly * Math.sin(ang)), (int) (cy + lx * Math.sin(ang) + ly * Math.cos(ang)), 0xFF4A4A50);
            }
        }
        s.contour(Dessin.CONTOUR);
        t.coller(s, 0, 0);
    }
}
