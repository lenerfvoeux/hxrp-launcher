package fr.lenerfvoeux.hxrp.metiers.virus.client.jeu;

import fr.lenerfvoeux.hxrp.metiers.client.minijeu.Dessin;
import fr.lenerfvoeux.hxrp.metiers.client.minijeu.Police;
import fr.lenerfvoeux.hxrp.metiers.client.minijeu.Toile;
import fr.lenerfvoeux.hxrp.metiers.minijeu.MiniJeu;
import fr.lenerfvoeux.hxrp.metiers.virus.minijeu.JeuxVirus;

import static fr.lenerfvoeux.hxrp.metiers.virus.client.jeu.ScenesVirus.*;

/** Scènes des machines à feu et à broyer : yagen, hachoir à levier, chaudron sur brasero, alambic, jarres. */
final class ScenesOfficine {
    private ScenesOfficine() {}

    // ================================================================== YAGEN
    static void yagen(Toile t, JeuxVirus.Yagen j, ContexteVirus c, double tms) {
        plateau(t, 60, 138, 312, 160);
        int x0 = 84, x1 = 284, cy = 112;
        double cx = (x0 + x1) / 2.0, demi = (x1 - x0) / 2.0;
        // auge en fonte : coque en barque, gorge sombre, pieds
        Toile s = sprite();
        int[] f = Dessin.rampe(FONTE);
        for (int x = x0; x <= x1; x++) {
            double u = (x - cx) / demi;
            int bas = cy + (int) Math.round(24 * Math.sqrt(Math.max(0, 1 - u * u * 0.85)));
            for (int y = cy; y <= bas; y++) {
                double L = 0.72 - u * 0.25 - (y - cy) / 40.0;
                s.set(x, y, Dessin.bande(f, L, x, y));
            }
        }
        for (int k : new int[]{-1, 1}) {
            int px = (int) (cx + k * demi * 0.55);
            s.rect(px - 7, cy + 18, 14, 10, f[1]);
            s.rect(px - 7, cy + 18, 14, 2, f[3]);
        }
        s.ellipse(cx, cy, demi, 7, f[3]);
        s.ellipse(cx, cy + 1, demi - 5, 4.5, f[0]);
        poser(t, s);
        // poudre dans la gorge : grossière au début, fine et claire à la fin
        double fin = j.fait / (double) Math.max(1, j.total);
        int[] p = rampe(Toile.melange(c.couleur, 0xFFF4EEDC, fin * 0.45));
        Dessin.ellipseV(t, cx, cy + 1.5, demi - 10, 3, p, 0.95, 0.45);
        int grains = (int) ((1 - fin) * 70);
        for (int k = 0; k < grains; k++) {
            int h = hash(k * 29L + 5);
            int gx = (int) (cx - (demi - 14) + Math.floorMod(h, (int) (2 * (demi - 14))));
            t.rect(gx, cy + Math.floorMod(h >> 9, 3), 2, 1, p[Math.floorMod(h >> 4, 3)]);
        }
        // extrémité visée : lueur verte à l'approche
        if (j.cible < j.total) {
            boolean droite = j.cible % 2 == 0;
            int ex = droite ? x1 - 14 : x0 + 14;
            double dt = Math.abs(tms - j.temps(j.cible));
            double k = clamp(1 - dt / (j.periode * 0.6));
            t.ellipse(ex, cy, 14, 6, Toile.alpha(dt < j.fenetre ? Dessin.G : 0xFFFFE8A0, 0.25 + 0.5 * k));
            t.polygone(new double[]{ex - 4, ex + 5, ex + 0.5}, new double[]{cy - 30, cy - 30, cy - 22}, C);
            t.polygone(new double[]{ex - 3, ex + 4, ex + 0.5}, new double[]{cy - 29, cy - 29, cy - 23}, dt < j.fenetre ? Dessin.G : Dessin.HL);
        }
        // la roue : disque de fonte à six rayons qui roule, axe de bois tenu à deux mains
        double pos = j.position(tms);
        double wx = x0 + 24 + pos * (x1 - x0 - 48);
        double monte = Math.pow(Math.abs(pos - 0.5) * 2, 3) * 6;
        double wy = cy - 14 - monte, r = 20;
        double ang = wx / r;
        Toile w = sprite();
        Dessin.sphere(w, wx, wy, r, r, f, 0);
        w.anneau(wx, wy, r - 3, r - 3, 2, f[1]);
        w.disque(wx, wy, 6, f[3]);
        for (int k = 0; k < 6; k++) {
            double a = ang + k * Math.PI / 3;
            w.trait(wx + Math.cos(a) * 6, wy + Math.sin(a) * 6, wx + Math.cos(a) * (r - 4), wy + Math.sin(a) * (r - 4), 2, f[k % 2 == 0 ? 3 : 2]);
        }
        w.disque(wx, wy, 3, Dessin.OL);
        w.disque(wx - 0.5, wy - 0.5, 1.5, Dessin.HL);
        poser(t, w);
        Toile a = sprite();
        int[] b = Dessin.rampe(BOIS);
        a.trait(wx - 30, wy + 12, wx + 30, wy - 12, 5, b[2]);
        a.trait(wx - 30, wy + 11, wx + 30, wy - 13, 2, b[3]);
        for (int k : new int[]{-1, 1}) a.ellipse(wx + k * 31, wy - k * 12.4, 4, 4, b[k < 0 ? 2 : 3]);
        poser(t, a);
        // étincelles de poudre au contact
        double age = tms - j.tFrappe;
        if (age >= 0 && age < 320) {
            double q = age / 320.0;
            for (int k = 0; k < 12; k++) {
                double an = Math.PI + k * 0.26, rr = 6 + q * 26;
                t.rect((int) (wx + Math.cos(an) * rr), (int) (cy - 2 + Math.sin(an) * rr * 0.5 - q * 8), 2, 2, Toile.alpha(p[4], 1 - q));
            }
        }
        pastilles(t, j.resultats, j.total, j.cible, 186, 174);
    }

    // ================================================================== HACHOIR À LEVIER
    static void hachoir(Toile t, JeuxVirus.Hachoir j, ContexteVirus c, double tms) {
        plateau(t, 60, 142, 312, 162);
        int[] b = Dessin.rampe(BOIS), bc = Dessin.rampe(BOIS_CLAIR);
        // billot : dessus clair, face avant
        Toile s = sprite();
        Dessin.degradeV(s, 92, 112, 182, 12, bc, 0.85, 0.6);
        Dessin.degradeV(s, 92, 124, 182, 16, b, 0.55, 0.3);
        for (int x = 98; x < 270; x += 13) s.rect(x, 114 + (x % 3), 8, 1, bc[2]);
        s.rect(92, 124, 182, 1, b[4]);
        poser(t, s);
        // la botte d'ingrédient : raccourcit à chaque coupe, les tranches s'empilent à gauche
        int[] ic = c.icone(0);
        int reste = Math.max(0, 3 - (int) Math.round(j.fait * 3 / 5.0));
        for (int k = 0; k < reste; k++) icone(t, ic, 150 + k * 20, 88, 1);
        int[] tr = rampe(c.couleur);
        for (int k = 0; k < j.fait; k++) {
            int x = 104 + (k % 3) * 11, y = 118 - (k / 3) * 4;
            Dessin.sphere(t, x + 4, y, 6, 3, tr, 1);
        }
        // levier : lame d'acier qui pivote à droite, poignée en bois au bout
        int der = j.fait > 0 ? j.tCoupe[j.fait - 1] : -10000;
        double age = tms - der, haut = 0.55, a;
        if (age >= 0 && age < 90) a = haut * (1 - age / 90.0);
        else if (age >= 90 && age < JeuxVirus.Hachoir.RECUL) a = haut * (age - 90) / (JeuxVirus.Hachoir.RECUL - 90);
        else a = haut;
        double px = 258, py = 106, L = 118;
        double dx = -Math.cos(a), dy = -Math.sin(a), nx = dy, ny = -dx;
        double ex = px + dx * L, ey = py + dy * L;
        Toile l = sprite();
        int[] ac = Dessin.rampe(ACIER);
        l.polygone(new double[]{px, ex, ex + nx * 8, px + nx * 12}, new double[]{py, ey, ey + ny * 8, py + ny * 12}, ac[2]);
        l.trait(px + nx * 2, py + ny * 2, ex + nx * 2, ey + ny * 2, 2, ac[4]);
        l.trait(px + nx * 11, py + ny * 11, ex + nx * 7, ey + ny * 7, 1.5, ac[0]);
        l.trait(ex - dx * 2, ey - dy * 2, ex + dx * 24, ey + dy * 24, 7, b[2]);
        l.trait(ex + dx * 2 - nx * 1.5, ey + dy * 2 - ny * 1.5, ex + dx * 22 - nx * 1.5, ey + dy * 22 - ny * 1.5, 2, b[3]);
        poser(t, l);
        // pivot en laiton et montant
        t.rect((int) px - 4, (int) py, 8, 20, Dessin.OL);
        t.rect((int) px - 3, (int) py, 6, 19, Dessin.OR);
        t.disque(px, py, 5, C);
        t.disque(px, py, 4, Dessin.OR);
        t.disque(px - 1, py - 1, 1.5, Dessin.HL);
        int[] res = new int[5];
        for (int k = 0; k < 5; k++) res[k] = k < j.fait ? j.qualite[k] : -1;
        pastilles(t, res, 5, j.fait, 186, 174);
    }

    // ================================================================== CHAUDRON SUR BRASERO
    static void chaudron(Toile t, JeuxVirus.Chaudron j, ContexteVirus c, double tms) {
        plateau(t, 64, 150, 304, 168);
        double cx = 186, v = j.valeur;
        int[] f = Dessin.rampe(FONTE);
        // brasero : vasque sur trois pieds, braises qui rougeoient selon la chaleur
        Toile s = sprite();
        for (int k : new int[]{-1, 0, 1}) s.trait(cx + k * 22, 140, cx + k * 30, 156, 3, f[1]);
        s.ellipse(cx, 136, 36, 9, f[2]);
        s.rect((int) cx - 34, 130, 68, 7, f[1]);
        s.ellipse(cx, 130, 34, 7, f[3]);
        poser(t, s);
        int braise = Toile.melange(0xFF4A1208, 0xFFFFA030, clamp(v * 1.1));
        t.ellipse(cx, 130, 30, 5, braise);
        for (int k = 0; k < 18; k++) {
            int h = hash(k * 31L);
            double bx = cx - 26 + Math.floorMod(h, 52), by = 129 + Math.floorMod(h >> 8, 3);
            double scint = 0.5 + 0.5 * Math.sin(tms / 140.0 + k);
            t.rect((int) bx, (int) by, 3, 2, Toile.melange(braise, 0xFFFFF0A0, v * scint * 0.7));
        }
        Dessin.flammes(t, cx, 128, 54, v * 1.5, (int) tms, false);
        // chaudron de fonte
        Toile p = sprite();
        Dessin.sphere(p, cx, 98, 42, 30, f, 2);
        p.ellipse(cx, 76, 36, 8, f[3]);
        p.ellipse(cx, 77, 32, 6, f[0]);
        for (int k : new int[]{-1, 1}) p.anneau(cx + k * 44, 84, 6, 6, 2, f[2]);
        poser(t, p);
        // contenu : liquide qui bout (ou herbes qui torréfient, à sec)
        boolean sec = j.mode == 3;
        int[] liq = rampe(sec ? Toile.melange(c.couleur, 0xFF5A3010, clamp(j.compte / 1500.0)) : c.couleur);
        Dessin.ellipseV(t, cx, 77.5, 31, 5, liq, 0.8, 0.45);
        int bulles = (int) (j.bouillon * 14);
        for (int k = 0; k < bulles && !sec; k++) {
            int h = hash(k * 97L + (long) (tms / 260) * 13);
            double bx = cx - 26 + Math.floorMod(h, 52), by = 76 + Math.floorMod(h >> 7, 4);
            t.anneau(bx, by, 2, 1.4, 1, liq[4]);
        }
        if (sec) for (int k = 0; k < 16; k++) {
            int h = hash(k * 53L);
            t.rect((int) (cx - 24 + Math.floorMod(h, 48)), 75 + Math.floorMod(h >> 6, 4), 3, 1, liq[Math.floorMod(h >> 3, 3) + 1]);
        }
        // vapeur, débordement ou fumée noire
        for (int k = 0; k < 4; k++) {
            double age = ((tms / 1000.0) + k * 0.25) % 1.0;
            volute(t, cx - 18 + k * 12 + Math.sin(tms / 400.0 + k) * 4, 70 - age * 34, 4 + age * 5, 0xFFF0F0F0, v * (1 - age) * 0.8);
        }
        if (j.valeur > j.rouge) {
            if (sec) for (int k = 0; k < 5; k++) volute(t, cx - 16 + k * 8, 62 - ((tms / 6 + k * 20) % 40), 6, 0xFF2A2420, 0.7);
            else for (int k = 0; k < 7; k++) {
                double ox = cx - 34 + k * 11;
                double coule = (tms / 8 + k * 13) % 26;
                t.ellipse(ox, 79 + coule * 0.6, 4, 3, Toile.alpha(Toile.clair(c.couleur, 0.6), 0.9));
            }
        }
        // soufflet : se comprime à chaque coup
        double comp = bosse(tms - j.tSouffle, 220);
        soufflet(t, 112, 148, comp);
        if (comp > 0) for (int k = 0; k < 3; k++) t.ligneH(128, 140 + (int) (comp * 8), 142 + k * 3, Toile.alpha(0xFFFFFFFF, comp * 0.6));
        etiquette(t, 262, 92, JeuxVirus.Chaudron.MODES[j.mode]);
    }

    private static void soufflet(Toile t, double x, double y, double comp) {
        int[] b = Dessin.rampe(BOIS), cuir = Dessin.rampe(0xFF7A4A2A);
        double ouv = 9 * (1 - comp * 0.8);
        Toile s = sprite();
        s.polygone(new double[]{x - 20, x + 12, x + 18, x - 18}, new double[]{y - ouv - 4, y - 3, y - 1, y - ouv}, b[3]);
        s.polygone(new double[]{x - 18, x + 18, x + 12, x - 20}, new double[]{y + ouv, y + 1, y + 3, y + ouv + 4}, b[1]);
        s.polygone(new double[]{x - 18, x + 16, x - 18}, new double[]{y - ouv, y, y + ouv}, cuir[2]);
        for (int k = 1; k < 4; k++) s.ligne(x - 18 + k * 8, y - ouv * (1 - k / 4.5), x - 18 + k * 8, y + ouv * (1 - k / 4.5), cuir[1]);
        s.rect((int) x + 16, (int) y - 2, 12, 4, Dessin.OL);
        s.rect((int) x + 16, (int) y - 1, 12, 2, Dessin.OR);
        s.trait(x - 20, y - ouv - 4, x - 30, y - ouv - 10, 3, b[2]);
        s.trait(x - 20, y + ouv + 4, x - 30, y + ouv + 10, 3, b[2]);
        poser(t, s);
    }

    // ================================================================== ALAMBIC DE CUIVRE
    static void alambic(Toile t, JeuxVirus.Alambic j, ContexteVirus c, double tms) {
        plateau(t, 60, 152, 312, 168);
        int[] cu = Dessin.rampe(CUIVRE), br = Dessin.rampe(0xFF9A4A32), b = Dessin.rampe(BOIS);
        // foyer de briques et son feu
        Toile s = sprite();
        Dessin.degradeV(s, 82, 120, 60, 34, br, 0.6, 0.35);
        for (int y = 124; y < 154; y += 6) {
            s.ligneH(82, 141, y, br[0]);
            for (int x = 84 + ((y / 6) % 2) * 6; x < 140; x += 12) s.ligneV(x, y - 5, y, br[0]);
        }
        s.rect(100, 134, 24, 16, 0xFF1A0A06);
        poser(t, s);
        double flare = bosse(tms - j.tFeu, 260);
        Dessin.flammes(t, 112, 149, 20, j.valeur * 1.2 + flare * 0.4, (int) tms, false);
        // chaudière en cuivre, chapiteau et col de cygne
        Toile a = sprite();
        Dessin.sphere(a, 112, 100, 30, 24, cu, 3);
        a.rect(96, 72, 32, 8, cu[2]);
        Dessin.sphere(a, 112, 66, 18, 12, cu, 2);
        for (int k = 0; k <= 30; k++) {
            double u = k / 30.0;
            double x = 124 + u * 104, y = 60 - Math.sin(u * Math.PI) * 10 + u * 36;
            a.disque(x, y, 4 - u * 1.5, cu[3 - (k % 2)]);
        }
        poser(t, a);
        // tonneau du serpentin : douves, cerclages, eau froide
        Toile tn = sprite();
        Dessin.degradeH(tn, 214, 90, 52, 62, b, 0.75, 0.3);
        for (int x = 222; x < 266; x += 9) tn.ligneV(x, 92, 150, b[1]);
        for (int y : new int[]{98, 140}) {
            tn.rect(212, y, 56, 4, 0xFF3A3C44);
            tn.rect(212, y, 56, 1, 0xFF7A7E88);
        }
        tn.ellipse(240, 90, 26, 5, 0xFF2A6A9A);
        poser(t, tn);
        // eau versée quand on refroidit
        double eau = bosse(tms - j.tFroid, 300);
        if (eau > 0) for (int k = 0; k < 6; k++) t.rect(236 + k % 3 * 3, (int) (70 + k * 3 + eau * 6), 2, 3, Toile.alpha(0xFF8ACBF0, eau));
        // thermomètre du froid sur le tonneau
        int gx = 274, gy = 84, gl = 60;
        t.rect(gx - 1, gy - 1, 10, gl + 2, C);
        t.rect(gx, gy, 8, gl, Dessin.OR);
        t.rect(gx + 2, gy + 2, 4, gl - 4, 0xFF1A2A3A);
        int bas = gy + gl - 2, H = gl - 4;
        int za = (int) (bas - j.froidHi * H), zb = (int) (bas - j.froidLo * H);
        t.rect(gx + 2, za, 4, zb - za, j.froidOk() ? Dessin.GH : Dessin.G);
        int fy = (int) (bas - clamp(j.froid) * H);
        t.rect(gx + 3, fy, 2, bas - fy, 0xFF6AC8FF);
        t.polygone(new double[]{gx + 10, gx + 15, gx + 15}, new double[]{fy, fy - 3, fy + 3}, C);
        Police.centre(t, "FROID", gx + 4, gy + gl + 3, Dessin.CREME, C, 1);
        // sortie : goutte à goutte dans la fiole
        int[] lq = rampe(c.couleur);
        double rempli = clamp(j.fait / 100.0);
        Toile fl = sprite();
        fl.ellipse(292, 140, 11, 11, 0xFFDCECF2);
        fl.rect(288, 120, 8, 12, 0xFFDCECF2);
        poser(t, fl);
        for (int y = (int) (151 - rempli * 20); y < 151; y++)
            for (int x = 282; x < 303; x++) {
                double dx = (x + 0.5 - 292) / 10.0, dy = (y + 0.5 - 140) / 10.0;
                if (dx * dx + dy * dy <= 1) t.set(x, y, Dessin.bande(lq, 0.4 + 0.4 * (151 - y) / 20.0, x, y));
            }
        t.rect(289, 121, 2, 10, 0x88FFFFFF);
        t.rect(266, 132, 26, 3, cu[2]);
        if (j.feuOk() && j.froidOk()) {
            double g = (tms % 500) / 500.0;
            t.rect(291, (int) (134 + g * 14), 2, 3, lq[3]);
        }
        // sifflement : jets de vapeur perdue
        if (j.siffle()) for (int k = 0; k < 4; k++) {
            double q = ((tms / 300.0) + k * 0.25) % 1;
            volute(t, 128 + q * 20, 58 - q * 22, 3 + q * 4, 0xFFFFFFFF, 1 - q);
        }
        etiquette(t, 150, 166, JeuxVirus.Alambic.MODES[j.mode]);
    }

    // ================================================================== JARRES : VERSER
    static void jarres(Toile t, JeuxVirus.Verser j, ContexteVirus c, double tms) {
        plateau(t, 60, 150, 312, 168);
        double cx = 186, bas = 148, haut = 74;
        int[] ce = Dessin.rampe(CELADON), lq = rampe(c.couleur);
        // jarre en céladon : col étroit, épaule, panse qui s'affine vers le pied
        Toile s = sprite();
        for (int y = (int) haut; y <= bas; y++) {
            double u = (y - haut) / (bas - haut);
            double r = u < 0.1 ? 14 : u < 0.42 ? 14 + 25 * Math.sin((u - 0.1) / 0.32 * Math.PI / 2) : 39 - 13 * Math.pow((u - 0.42) / 0.58, 2);
            for (int x = (int) (cx - r); x <= cx + r; x++) {
                double nx = (x - cx) / r;
                s.set(x, y, Dessin.bande(ce, 0.78 - nx * 0.38 - u * 0.12, x, y));
            }
        }
        s.ellipse(cx, haut, 15, 4, ce[3]);
        s.ellipse(cx, haut, 11, 2.5, 0xFF1E2A26);
        poser(t, s);
        t.rect((int) cx - 30, (int) haut + 30, 60, 2, Toile.alpha(ce[4], 0.5));
        // intérieur visible par une fenêtre de verre
        int fx0 = (int) cx - 14, fx1 = (int) cx + 14, fy0 = (int) haut + 10, fy1 = (int) bas - 6;
        t.rect(fx0 - 1, fy0 - 1, fx1 - fx0 + 2, fy1 - fy0 + 2, C);
        t.rect(fx0, fy0, fx1 - fx0, fy1 - fy0, 0xFF1E2A26);
        double niv = clamp(j.valeur);
        int ly = (int) (fy1 - niv * (fy1 - fy0));
        for (int y = ly; y < fy1; y++) for (int x = fx0; x < fx1; x++) t.set(x, y, Dessin.bande(lq, 0.4 + 0.35 * (x - fx0) / 28.0, x, y));
        if (ly < fy1) t.ligneH(fx0, fx1 - 1, ly, lq[4]);
        t.rect(fx0 + 2, fy0 + 2, 2, fy1 - fy0 - 4, 0x55FFFFFF);
        // graduations et trait cible (cordelette rouge)
        for (int k = 1; k < 10; k++) t.ligneH(fx1 - 4, fx1 - 1, (int) (fy1 - k / 10.0 * (fy1 - fy0)), 0xAAF2E6C8);
        if (j.trait >= 0) {
            int ty = (int) (fy1 - j.trait * (fy1 - fy0));
            t.rect(fx0 - 8, ty, fx1 - fx0 + 16, 1, CORDE_ROUGE);
            t.disque(fx1 + 9, ty, 2, CORDE_ROUGE);
        }
        // pichet de grès qui verse : bec à droite, anse à gauche
        boolean verse = j.phase == 1;
        double inc = verse ? 0.85 : 0.2;
        double px = 140, py = 54;
        double ca = Math.cos(inc), sa = Math.sin(inc);
        int[] pp = Dessin.rampe(0xFFB89A78);
        Toile p = sprite();
        double[][] forme = {{-10, -15}, {10, -15}, {13, -6}, {13, 12}, {9, 16}, {-9, 16}, {-13, 12}, {-13, -6}};
        double[] xs = new double[forme.length], ys = new double[forme.length];
        for (int k = 0; k < forme.length; k++) {
            xs[k] = px + forme[k][0] * ca - forme[k][1] * sa;
            ys[k] = py + forme[k][0] * sa + forme[k][1] * ca;
        }
        p.polygone(xs, ys, pp[2]);
        // bec
        double bx = px + 14 * ca + 13 * sa, by = py + 14 * sa - 13 * ca;
        p.polygone(new double[]{px + 8 * ca + 14 * sa, bx + 4 * ca, px + 12 * ca + 9 * sa}, new double[]{py + 8 * sa - 14 * ca, by + 4 * sa, py + 12 * sa - 9 * ca}, pp[3]);
        // anse
        p.anneau(px - 16 * ca, py - 16 * sa, 6, 7, 2, pp[1]);
        // bandeau décoratif
        p.trait(px - 12 * ca + 2 * sa, py - 12 * sa - 2 * ca, px + 12 * ca + 2 * sa, py + 12 * sa - 2 * ca, 3, BLEU);
        poser(t, p);
        if (verse) {
            for (int y = (int) by + 2; y < ly; y++) {
                double ox = Math.sin((y + tms / 20) * 0.5) * 0.8;
                double xx = bx + 4 + (cx - 4 - bx - 4) * clamp((y - by) / 18.0);
                t.rect((int) (xx + ox), y, 3, 1, lq[3]);
            }
            for (int k = 0; k < 4; k++) t.rect((int) cx - 9 + k * 4, ly - 2 - (int) ((tms / 40 + k * 3) % 4), 2, 2, lq[4]);
        }
        // doses déjà versées : petites jarres scellées
        for (int k = 0; k < j.total; k++) {
            int x = 256 + k * 18, y = 124;
            boolean faite = k < j.dose;
            t.ellipse(x, y + 8, 7, 9, faite ? ce[2] : 0x55000000);
            if (faite) {
                t.rect(x - 5, y - 2, 10, 3, CORDE_ROUGE);
                t.ellipse(x, y - 2, 6, 2, PAPIER);
            }
        }
    }

    static int noteAffichee(MiniJeu j) {
        return (int) Math.round(j.noteFinale());
    }
}
