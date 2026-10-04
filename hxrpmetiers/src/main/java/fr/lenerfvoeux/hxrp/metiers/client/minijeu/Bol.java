package fr.lenerfvoeux.hxrp.metiers.client.minijeu;

import fr.lenerfvoeux.hxrp.metiers.minijeu.Jeux;
import fr.lenerfvoeux.hxrp.metiers.minijeu.MiniJeu;

/** FOUETTER, MÉLANGER, PILER, TAMISER : bol, mortier et tamis. */
final class Bol {
    private Bol() {}

    /** Grand bol en céramique vu de trois-quarts. Renvoie rien ; le contenu se dessine dans l'ellipse (cx, cy, rx-4, ry-2). */
    static void bol(Toile t, double cx, double cy, double rx, double ry, double prof, int couleur, int bande) {
        Dessin.ombre(t, cx + 6, cy + prof + ry * 0.6, rx * 0.8, 7);
        Toile s = new Toile(Jeux.W, Jeux.H);
        int[] r = Dessin.rampe(couleur);
        // corps : demi-ellipse basse
        for (int y = (int) cy; y <= (int) (cy + prof); y++) {
            double k = (y - cy) / prof, w = rx * Math.sqrt(Math.max(0, 1 - k * k * 0.85));
            for (int x = (int) (cx - w); x <= (int) (cx + w); x++) {
                double nx = (x - cx) / Math.max(1, w);
                double L = 0.78 - nx * 0.38 - k * 0.25;
                s.set(x, y, Dessin.bande(r, L, x, y));
            }
        }
        s.ellipse(cx, cy + prof - 1, rx * 0.4, 4, r[1]);
        if (bande != 0) {
            for (int dy : new int[]{8, 11})
                for (int x = (int) (cx - rx); x <= cx + rx; x++) {
                    double nx = (x - cx) / rx;
                    if (Math.abs(nx) > 0.98) continue;
                    s.set(x, (int) (cy + dy + ry * 0.35 * Math.sqrt(1 - nx * nx) * 0.3), bande);
                }
        }
        // ouverture
        s.ellipse(cx, cy, rx, ry, r[3]);
        Dessin.ellipseV(s, cx, cy, rx - 3, ry - 2, new int[]{Toile.sombre(r[0], 0.8), r[0], r[1], r[1], r[2]}, 0.1, 0.8);
        s.contour(Dessin.CONTOUR);
        t.coller(s, 0, 0);
    }

    static void bordBol(Toile t, double cx, double cy, double rx, double ry, int couleur) {
        int[] r = Dessin.rampe(couleur);
        for (int x = (int) (cx - rx); x <= cx + rx; x++) {
            double nx = (x - cx) / rx;
            if (Math.abs(nx) > 1) continue;
            int y = (int) Math.round(cy + ry * Math.sqrt(1 - nx * nx));
            t.set(x, y - 1, r[4]);
            t.set(x, y - 2, r[3]);
        }
    }

    // ================================================================== FOUETTER / MÉLANGER
    static void batteur(Toile t, Jeux.Batteur j, Contexte c, double tms) {
        double cx = 172, cy = 96, rx = 66, ry = 22;
        int bolC = j.fouet ? 0xFFF2EEE6 : 0xFFC8603A;
        bol(t, cx, cy, rx, ry, 56, bolC, j.fouet ? 0xFF3A6AB0 : 0);
        double volume = Math.min(1, j.volume / 6.0);
        int pate = Toile.melange(c.couleurMelange, 0xFFF0D080, 0.45);
        int base = j.fouet ? Toile.melange(pate, 0xFFFBF6E8, volume * 0.6) : Toile.melange(c.couleurMelange, 0xFF8AB04A, 0.15);
        int[] r = Dessin.rampe(base);
        double niveau = cy + 4 - volume * 5;
        Toile s = new Toile(Jeux.W, Jeux.H);
        Dessin.ellipseV(s, cx, niveau, rx - 5, ry - 4, r, 0.95, 0.55);
        // tourbillon qui tourne avec la molette
        double phase = j.crans * 0.55 + tms / 1000.0 * (1 + j.valeur * 6);
        for (int bras = 0; bras < 3; bras++)
            for (double a = 0; a < 5.5; a += 0.08) {
                double rr = 3 + a * 8.5, ang = a + phase + bras * 2.09;
                int x = (int) (cx + Math.cos(ang) * rr), y = (int) (niveau + Math.sin(ang) * rr * (ry - 4) / (rx - 5));
                if (rr < rx - 7) s.set(x, y, bras == 1 ? r[1] : r[4]);
            }
        if (!j.fouet) {
            // deux teintes qui se fondent petit à petit
            int autre = c.couleursBouts.length > 0 ? c.couleursBouts[0] : 0xFF8A5A2A;
            double melange = Math.min(1, j.compte / 700.0);
            for (int k = 0; k < 40; k++) {
                int h = Scenes.hash(k * 7 + 1);
                double a = Math.floorMod(h, 628) / 100.0 + phase * 0.5, d = Math.floorMod(h >> 9, 90) / 100.0;
                int x = (int) (cx + Math.cos(a) * (rx - 8) * d), y = (int) (niveau + Math.sin(a) * (ry - 5) * d);
                s.rect(x, y, 3, 2, Toile.melange(autre, base, melange));
            }
        } else {
            for (int k = 0; k < 6 + (int) (volume * 20); k++) {
                int h = Scenes.hash(k * 11 + (int) (tms / 400));
                double a = Math.floorMod(h, 628) / 100.0, d = Math.floorMod(h >> 9, 90) / 100.0;
                s.set((int) (cx + Math.cos(a) * (rx - 8) * d), (int) (niveau + Math.sin(a) * (ry - 5) * d), 0xFFFFFFFF);
            }
        }
        t.coller(s, 0, 0);
        bordBol(t, cx, cy, rx - 1, ry - 1, bolC);
        // éclaboussures quand on va trop vite / débordement
        boolean trop = j.fouet ? j.valeur > j.zoneHi + 0.05 : j.valeur > j.rouge;
        if (trop) {
            for (int k = 0; k < 10; k++) {
                int h = Scenes.hash(k * 5 + (int) (tms / 90));
                double a = Math.floorMod(h, 628) / 100.0;
                double x = cx + Math.cos(a) * (rx + 4 + Math.floorMod(h >> 8, 12)), y = cy - 6 + Math.sin(a) * (ry + 8) - Math.floorMod(h >> 12, 10);
                t.rect((int) x, (int) y, 2, 2, r[3]);
            }
        }
        if (!j.fouet && j.deborde > 0) {
            for (int k = 0; k < Math.min(6, 1 + (int) (j.deborde * 3)); k++) {
                double x = cx - rx + 14 + k * 22;
                double L = 6 + (k * 5) % 11 + Math.min(18, j.deborde * 6);
                t.rect((int) x, (int) (cy + ry - 4), 3, (int) L, r[2]);
                t.rect((int) x, (int) (cy + ry - 4), 1, (int) L, r[4]);
            }
        }
        // l'ustensile tourne dans le bol
        double ang = phase * 1.3;
        double ux = cx + Math.cos(ang) * 22, uy = niveau + Math.sin(ang) * 7;
        if (j.fouet) fouet(t, ux, uy);
        else spatule(t, ux, uy);
        // aide molette
        molette(t, 290, 142, j.crans);
    }

    static void fouet(Toile t, double x, double y) {
        Toile s = new Toile(Jeux.W, Jeux.H);
        for (int k = -2; k <= 2; k++) {
            double w = 3 + Math.abs(k) * 3.5;
            for (double a = 0; a < Math.PI * 2; a += 0.05) {
                double px = x + 12 + Math.cos(a) * w * (k < 0 ? -1 : 1) * (k == 0 ? 0.2 : 1), py = y - 18 + Math.sin(a) * 18;
                if (py > y - 36) s.set((int) px, (int) py, k == 0 ? 0xFFF4F8FB : 0xFFC8CCD2);
            }
        }
        s.trait(x + 12, y - 36, x + 22, y - 62, 5, 0xFF3A3C42);
        s.trait(x + 12, y - 36, x + 22, y - 62, 1.5, 0xFF6E727A);
        s.contour(Dessin.CONTOUR);
        t.coller(s, 0, 0);
    }

    static void spatule(Toile t, double x, double y) {
        Toile s = new Toile(Jeux.W, Jeux.H);
        s.trait(x + 4, y - 10, x + 26, y - 62, 4, 0xFFC8955A);
        s.trait(x + 4, y - 10, x + 26, y - 62, 1.3, 0xFFE4B87A);
        s.ellipse(x + 2, y - 4, 6, 9, 0xFFC8955A);
        s.ellipse(x + 1, y - 5, 3, 6, 0xFFE4B87A);
        s.contour(Dessin.CONTOUR);
        t.coller(s, 0, 0);
    }

    static void molette(Toile t, int x, int y, int crans) {
        Dessin.souris(t, x - 6, y - 10, 2, (crans % 2) == 1);
        int a = crans % 4;
        t.rect(x - 1, y - 8 + a, 1, 1, Dessin.CONTOUR);
        Police.centre(t, "MOLETTE", x, y + 12, Dessin.CREME, Dessin.CONTOUR, 1);
    }

    // ================================================================== PILER
    static void piler(Toile t, Jeux.Rythme j, Contexte c, double tms) {
        double cx = 172, cy = 104, rx = 50, ry = 17;
        // mortier en pierre
        Dessin.ombre(t, cx + 6, cy + 50, rx * 0.9, 7);
        Toile s = new Toile(Jeux.W, Jeux.H);
        int[] pierre = Dessin.rampe(0xFF8A8C90);
        for (int y = (int) cy; y <= cy + 44; y++) {
            double k = (y - cy) / 44.0, w = rx * (1 - k * k * 0.45);
            for (int x = (int) (cx - w); x <= cx + w; x++) {
                double nx = (x - cx) / w;
                s.set(x, y, Dessin.bande(pierre, 0.8 - nx * 0.4 - k * 0.2 + ((Scenes.hash(x * 31 + y * 17) & 7) - 3) * 0.02, x, y));
            }
        }
        s.rect((int) (cx - rx * 0.6), (int) (cy + 44), (int) (rx * 1.2), 6, pierre[1]);
        s.ellipse(cx, cy, rx, ry, pierre[3]);
        s.ellipse(cx, cy + 1, rx - 6, ry - 4, pierre[0]);
        // contenu : grains au début, poudre à la fin
        double fin = j.fait / (double) j.total;
        int[] g = Dessin.rampe(c.peau), p = Dessin.rampe(Toile.melange(c.peau, c.chair, 0.5));
        Dessin.ellipseV(s, cx, cy + 2, rx - 9, ry - 6, p, 0.9, 0.5);
        int grains = (int) ((1 - fin) * 60);
        for (int k = 0; k < grains; k++) {
            int h = Scenes.hash(k * 13 + 77);
            double a = Math.floorMod(h, 628) / 100.0, d = Math.floorMod(h >> 9, 90) / 100.0;
            int x = (int) (cx + Math.cos(a) * (rx - 11) * d), y = (int) (cy + 2 + Math.sin(a) * (ry - 7) * d);
            s.rect(x, y, 2, 2, g[1 + (k % 3)]);
            s.set(x, y, g[4]);
        }
        s.contour(Dessin.CONTOUR);
        t.coller(s, 0, 0);
        // cercle d'approche et cible
        if (j.cible < j.total) {
            double dt = j.temps(j.cible) - tms;
            double k = Math.max(0, Math.min(1.4, dt / j.periode));
            double r0 = 9, r = r0 + 42 * k;
            t.anneau(cx, cy + 2, r0 + 1.5, (r0 + 1.5) * 0.5, 3, Dessin.CONTOUR);
            t.anneau(cx, cy + 2, r0, r0 * 0.5, 2, Dessin.OR);
            if (k < 1.3) {
                t.anneau(cx, cy + 2, r + 1, (r + 1) * 0.5, 3, Toile.alpha(Dessin.CONTOUR, 0.8));
                t.anneau(cx, cy + 2, r, r * 0.5, 1.5, dt < j.fenetre ? Dessin.G : 0xFFFFF4D0);
            }
        }
        // pilon : frappe franchement à chaque clic
        double age = tms - j.tFrappe;
        double d = age < 60 ? age / 60.0 : age < 220 ? 1 - (age - 60) / 160.0 : 0;
        pilon(t, cx + 8, cy - 40 + d * 34);
        // poudre soulevée
        if (age >= 0 && age < 350) {
            double a = age / 350.0;
            for (int k = 0; k < 14; k++) {
                double ang = k * 0.45 + 3.3, rr = 8 + a * 30;
                t.rect((int) (cx + Math.cos(ang) * rr), (int) (cy + Math.sin(ang) * rr * 0.4 - a * 12), 2, 2, Toile.alpha(p[4], 1 - a));
            }
        }
        rangeeResultats(t, j.resultats, j.cible, 176, 172);
    }

    static void pilon(Toile t, double x, double y) {
        Toile s = new Toile(Jeux.W, Jeux.H);
        int[] r = Dessin.rampe(0xFF9A9CA0);
        s.trait(x, y, x + 18, y - 44, 9, r[2]);
        s.trait(x - 1, y - 1, x + 17, y - 45, 3, r[3]);
        Dessin.sphere(s, x, y + 2, 7, 6, r, 2);
        Dessin.sphere(s, x + 18, y - 45, 5.5, 5, r, 1);
        s.contour(Dessin.CONTOUR);
        t.coller(s, 0, 0);
    }

    /** Rangée de pastilles : résultat de chaque temps (doré parfait, vert bien, rouge raté). */
    static void rangeeResultats(Toile t, int[] res, int courant, int cx, int y) {
        int n = res.length, w = Math.min(10, 180 / n);
        int x0 = cx - n * w / 2;
        for (int i = 0; i < n; i++) {
            int c = res[i] == MiniJeu.PARFAIT ? Dessin.OR_TEXTE : res[i] == MiniJeu.BIEN ? Dessin.VERT_TEXTE : res[i] == MiniJeu.RATE ? Dessin.ROUGE_TEXTE : 0xFF6A5A4A;
            t.disque(x0 + i * w + w / 2.0, y, i == courant ? 4 : 3, Dessin.CONTOUR);
            t.disque(x0 + i * w + w / 2.0, y, i == courant ? 3 : 2, c);
        }
    }

    // ================================================================== TAMISER
    static void tamiser(Toile t, Jeux.Tamiser j, Contexte c, double tms) {
        double cx = 172;
        // bol dessous et farine accumulée
        bol(t, cx, 132, 56, 16, 34, 0xFF3A6AB0, 0xFFF2EEE6);
        double tas = Math.min(1, j.tamise / Math.max(1, j.total));
        int farine = Toile.melange(c.chair, 0xFFFBF8F0, 0.5);
        int[] f = Dessin.rampe(farine);
        Toile s = new Toile(Jeux.W, Jeux.H);
        Dessin.sphere(s, cx, 134 - tas * 6, 16 + tas * 26, 5 + tas * 7, f, 1);
        t.coller(s, 0, 0);
        bordBol(t, cx, 132, 55, 15, 0xFF3A6AB0);
        // tamis : se décale vers le côté cliqué puis revient
        double age = tms - j.tDernier, dx = 0;
        if (j.cote >= 0 && age < j.tempo) dx = (j.cote == 0 ? -1 : 1) * 12 * Math.cos(age / j.tempo * Math.PI / 2);
        double sx = cx + dx, sy = 70;
        // pluie de farine
        int dens = j.cote < 0 ? 0 : (int) (40 * Math.max(0, 1 - age / (j.tempo * 1.5)));
        for (int k = 0; k < dens; k++) {
            int h = Scenes.hash(k * 7919 + (long) (tms / 30));
            double x = sx - 36 + Math.floorMod(h, 72), y = sy + 10 + Math.floorMod(h >> 8, 50);
            t.set((int) x, (int) y, (h & 3) == 0 ? 0xFFFFFFFF : Toile.alpha(farine, 0.8));
        }
        Toile tm = new Toile(Jeux.W, Jeux.H);
        int[] bois = Dessin.rampe(0xFFC8955A);
        // cerclage en bois (arrière, maille, farine, avant)
        tm.ellipse(sx, sy, 48, 17, bois[2]);
        tm.ellipse(sx, sy + 1, 45, 14, 0xFF9AA0A8);
        for (int x = (int) sx - 44; x < sx + 44; x += 3) tm.ligne(x, sy - 13, x, sy + 15, 0xFFC0C6CE);
        for (int y = (int) sy - 13; y < sy + 15; y += 3) tm.ligneH((int) sx - 44, (int) sx + 44, y, 0xFFB0B6BE);
        int reste = (int) ((1 - tas) * 26);
        Dessin.sphere(tm, sx, sy, 10 + reste, 4 + reste * 0.3, f, 1);
        for (int x = (int) (sx - 48); x <= sx + 48; x++) {
            double nx = (x - sx) / 48.0;
            if (Math.abs(nx) > 1) continue;
            int y0 = (int) Math.round(sy + 17 * Math.sqrt(1 - nx * nx));
            for (int k = 0; k < 7; k++) tm.set(x, y0 - 3 + k, Dessin.bande(bois, 0.8 - k * 0.07, x, y0 + k));
        }
        tm.contour(Dessin.CONTOUR);
        t.coller(tm, 0, 0);
        // côté attendu
        int attendu = j.cote < 0 ? -1 : 1 - j.cote;
        Dessin.souris(t, 70, 60, 0, attendu == 0 || attendu < 0);
        Dessin.souris(t, 262, 60, 1, attendu == 1 || attendu < 0);
        Police.centre(t, "GAUCHE", 76, 82, attendu == 0 ? Dessin.OR_TEXTE : Dessin.CREME, Dessin.CONTOUR, 1);
        Police.centre(t, "DROIT", 268, 82, attendu == 1 ? Dessin.OR_TEXTE : Dessin.CREME, Dessin.CONTOUR, 1);
    }
}
