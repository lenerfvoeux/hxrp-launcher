package fr.lenerfvoeux.hxrp.metiers.client.minijeu;

import fr.lenerfvoeux.hxrp.metiers.minijeu.Jeux;
import fr.lenerfvoeux.hxrp.metiers.minijeu.MiniJeu;

/**
 * Les scènes des 17 mini-jeux, dessinées pixel par pixel à chaque image, en vue trois-quarts,
 * sur fond transparent. Chaque scène lit l'état public du mini-jeu et le contexte de la recette.
 */
public final class Scenes {
    private Scenes() {}

    /** Où afficher les retours « PARFAIT / BIEN / RATÉ » quand ils n'ont pas de position propre. */
    public static int[] ancreRetour(MiniJeu j) {
        if (j instanceof Jeux.Couper) return new int[]{126, 42};
        if (j instanceof Jeux.Petrir) return new int[]{96, 56};
        if (j instanceof Jeux.Four) return new int[]{176, 150};
        if (j instanceof Jeux.Frire || j instanceof Jeux.Saisir) return new int[]{110, 58};
        if (j instanceof Jeux.Batteur || j instanceof Jeux.Rythme || j instanceof Jeux.Tamiser) return new int[]{250, 58};
        return new int[]{176, 42};
    }

    public static void dessiner(Toile t, MiniJeu j, Contexte c, double tms, int mx, int my, int etat) {
        if (j instanceof Jeux.Couper) Decoupe.couper(t, (Jeux.Couper) j, c, tms);
        else if (j instanceof Jeux.Etaler) Pate.etaler(t, (Jeux.Etaler) j, c, tms);
        else if (j instanceof Jeux.Petrir) Pate.petrir(t, (Jeux.Petrir) j, c, tms);
        else if (j instanceof Jeux.Faconner) Pate.faconner(t, (Jeux.Faconner) j, c, tms, mx, my);
        else if (j instanceof Jeux.Batteur) Bol.batteur(t, (Jeux.Batteur) j, c, tms);
        else if (j instanceof Jeux.Rythme) {
            Jeux.Rythme r = (Jeux.Rythme) j;
            if (r.piler) Bol.piler(t, r, c, tms);
            else Bar.secouer(t, r, c, tms);
        } else if (j instanceof Jeux.Tamiser) Bol.tamiser(t, (Jeux.Tamiser) j, c, tms);
        else if (j instanceof Jeux.Saisir) Feu.saisir(t, (Jeux.Saisir) j, c, tms);
        else if (j instanceof Jeux.Chauffe) {
            Jeux.Chauffe ch = (Jeux.Chauffe) j;
            if (ch.mijoter) Feu.mijoter(t, ch, c, tms);
            else Feu.bouillir(t, ch, c, tms);
        } else if (j instanceof Jeux.Four) Feu.four(t, (Jeux.Four) j, c, tms);
        else if (j instanceof Jeux.Griller) Feu.griller(t, (Jeux.Griller) j, c, tms, mx, my);
        else if (j instanceof Jeux.Frire) Feu.frire(t, (Jeux.Frire) j, c, tms);
        else if (j instanceof Jeux.Presser) Bar.presser(t, (Jeux.Presser) j, c, tms);
        else if (j instanceof Jeux.Assaisonner) Bar.assaisonner(t, (Jeux.Assaisonner) j, c, tms);
    }

    // ================================================================== décor commun
    /** Planche à découper épaisse en perspective : dessus bois chaud deux tons (fuyant vers le fond),
     * chant avant et chant droit visibles, trou de suspension, ombre portée. */
    static void planche(Toile t, int x, int y, int w, int h, int ep, boolean farine, long graine) {
        int dx = 10;
        Dessin.ombre(t, x + w / 2.0 + 8, y + h + ep + 3, w / 2.0 + 10, 7);
        int[] dessus = Dessin.rampe(0xFFD9A566), chant = Dessin.rampe(0xFFA86A34);
        // dessus : parallélogramme (bord arrière décalé à droite)
        for (int j = 0; j < h; j++) {
            int off = (int) Math.round(dx * (h - 1 - j) / (double) (h - 1));
            double L = 0.62 + 0.3 * (j / (double) h);
            for (int i = 0; i < w; i++) t.set(x + off + i, y + j, Dessin.bande(dessus, L, x + off + i, y + j));
        }
        long s = graine;
        for (int g = 0; g < w * h / 55; g++) {
            s = (s * 6364136223846793005L + 1442695040888963407L);
            int gy = y + 3 + (int) ((s >>> 13) % (h - 6));
            int off = (int) Math.round(dx * (h - 1 - (gy - y)) / (double) (h - 1));
            int gx = x + off + 3 + (int) ((s >>> 33) % (w - 14));
            int gl = 4 + (int) ((s >>> 50) % 10);
            t.rect(gx, gy, gl, 1, (s & 1) == 0 ? dessus[1] : dessus[3]);
        }
        // rigole à jus
        for (int j = 4; j < h - 4; j++) {
            int off = (int) Math.round(dx * (h - 1 - j) / (double) (h - 1));
            t.set(x + off + 4, y + j, dessus[1]);
            t.set(x + off + w - 6, y + j, dessus[1]);
        }
        t.ligneH(x + dx + 4 - 1, x + dx + w - 7, y + 4, dessus[1]);
        t.ligneH(x + 5, x + w - 6, y + h - 5, dessus[1]);
        // trou de suspension
        int tx = x + w - 15 + dx / 2, ty = y + h / 2;
        t.ellipse(tx, ty, 5, 4, 0xFF4A2A12);
        t.ellipse(tx, ty + 1, 4, 3, 0xFF7A4A26);
        // chant avant
        t.rect(x, y + h, w, ep, chant[2]);
        t.rect(x, y + h, w, 1, chant[4]);
        t.rect(x, y + h + ep - 2, w, 2, chant[0]);
        for (int k = x + 6; k < x + w - 4; k += 11) t.rect(k, y + h + 3, 5, 1, chant[3]);
        // chant droit (fuyant)
        for (int i = 0; i <= dx; i++) {
            int yy = y + h - (int) Math.round(i * (h - 1) / (double) dx);
            t.rect(x + w + i, yy, 1, ep, i < 2 ? chant[1] : chant[0]);
        }
        // contours
        int C = Dessin.CONTOUR;
        t.ligne(x - 1, y + h, x + dx - 1, y - 1, C);
        t.ligneH(x + dx - 1, x + dx + w, y - 1, C);
        t.ligne(x + dx + w, y - 1, x + w, y + h, C);
        t.ligne(x + dx + w + 1, y - 1, x + dx + w + 1, y + ep - 1, C);
        t.ligne(x + dx + w + 1, y + ep - 1, x + w, y + h + ep, C);
        t.ligneH(x - 1, x + w, y + h + ep, C);
        t.ligneV(x - 1, y + h, y + h + ep, C);
        t.ligneH(x, x + w - 1, y + h, Toile.melange(C, chant[2], 0.5));
        if (farine) {
            long f = graine * 31;
            for (int i = 0; i < w * h / 12; i++) {
                f = f * 6364136223846793005L + 1442695040888963407L;
                double a = ((f >>> 20) % 6283) / 1000.0, r = Math.sqrt(((f >>> 40) % 1000) / 1000.0);
                int py = (int) (y + h / 2.0 + Math.sin(a) * r * h * 0.42);
                int px = (int) (x + dx / 2.0 + w / 2.0 + Math.cos(a) * r * w * 0.45);
                t.set(px, py, (f & 3) == 0 ? 0xFFF8F4EA : 0xCCE8DCC4);
            }
        }
    }

    /** Petit rebond (0 -> 1 -> 0) sur une durée. */
    static double bosse(double age, double duree) {
        if (age < 0 || age > duree) return 0;
        return Math.sin(age / duree * Math.PI);
    }

    static double clamp(double v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }

    static int hash(long a) {
        a = (a ^ (a >>> 33)) * 0xff51afd7ed558ccdL;
        a = (a ^ (a >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return (int) (a ^ (a >>> 33));
    }
}
