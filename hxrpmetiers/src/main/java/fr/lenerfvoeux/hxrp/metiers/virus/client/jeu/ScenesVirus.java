package fr.lenerfvoeux.hxrp.metiers.virus.client.jeu;

import fr.lenerfvoeux.hxrp.metiers.client.minijeu.Dessin;
import fr.lenerfvoeux.hxrp.metiers.client.minijeu.Police;
import fr.lenerfvoeux.hxrp.metiers.client.minijeu.Rendu;
import fr.lenerfvoeux.hxrp.metiers.client.minijeu.Toile;
import fr.lenerfvoeux.hxrp.metiers.minijeu.MiniJeu;
import fr.lenerfvoeux.hxrp.metiers.virus.minijeu.JeuxVirus;

/**
 * Les scènes des mini-jeux du Virus, dessinées pixel par pixel à chaque image comme celles du Gourmet,
 * posées sur le plateau laqué brun-rouge de l'apothicaire. Aucune dépendance à Minecraft.
 */
public final class ScenesVirus implements Rendu.Scene {
    // matières de l'apothicaire
    static final int LAQUE = 0xFF7A2418, BOIS = 0xFFB07A44, BOIS_CLAIR = 0xFFD8B080, FONTE = 0xFF4E525C, CUIVRE = 0xFFC8703A,
            CELADON = 0xFF8FBFA0, PORCELAINE = 0xFFEEF0F2, BLEU = 0xFF2E5A9A, PAPIER = 0xFFF2E6C8, CORDE_ROUGE = 0xFFB8241C,
            PEAU = 0xFFE8B48C, VERRE = 0x88D8ECF4, ACIER = 0xFFB8C0CC;
    static final int C = Dessin.CONTOUR;

    private final ContexteVirus ctx;

    public ScenesVirus(ContexteVirus ctx) {
        this.ctx = ctx;
    }

    @Override
    public void dessiner(Toile t, MiniJeu j, double tms, int mx, int my, int etat) {
        if (j instanceof JeuxVirus.Yagen) ScenesOfficine.yagen(t, (JeuxVirus.Yagen) j, ctx, tms);
        else if (j instanceof JeuxVirus.Hachoir) ScenesOfficine.hachoir(t, (JeuxVirus.Hachoir) j, ctx, tms);
        else if (j instanceof JeuxVirus.Chaudron) ScenesOfficine.chaudron(t, (JeuxVirus.Chaudron) j, ctx, tms);
        else if (j instanceof JeuxVirus.Alambic) ScenesOfficine.alambic(t, (JeuxVirus.Alambic) j, ctx, tms);
        else if (j instanceof JeuxVirus.Verser) ScenesOfficine.jarres(t, (JeuxVirus.Verser) j, ctx, tms);
        else if (j instanceof JeuxVirus.Balance) ScenesDosage.balance(t, (JeuxVirus.Balance) j, ctx, tms, mx, my);
        else if (j instanceof JeuxVirus.Pilon) ScenesDosage.pilon(t, (JeuxVirus.Pilon) j, ctx, tms);
        else if (j instanceof JeuxVirus.Pilulier) ScenesDosage.pilulier(t, (JeuxVirus.Pilulier) j, ctx, tms);
        else if (j instanceof JeuxVirus.Table) ScenesDosage.table(t, (JeuxVirus.Table) j, ctx, tms, mx, my);
        else if (j instanceof JeuxVirus.Gorgees) ScenesSoins.gorgees(t, (JeuxVirus.Gorgees) j, ctx, tms);
        else if (j instanceof JeuxVirus.Injection) ScenesSoins.injection(t, (JeuxVirus.Injection) j, ctx, tms);
        else if (j instanceof JeuxVirus.EtalerSoin) ScenesSoins.etaler(t, (JeuxVirus.EtalerSoin) j, ctx, tms);
        else if (j instanceof JeuxVirus.Microscope) ScenesSoins.microscope(t, (JeuxVirus.Microscope) j, ctx, tms);
    }

    @Override
    public int[] ancre(MiniJeu j) {
        if (j instanceof JeuxVirus.Balance) return new int[]{200, 40};
        if (j instanceof JeuxVirus.Microscope) return new int[]{190, 36};
        if (j instanceof JeuxVirus.Table) return new int[]{180, 84};
        return new int[]{180, 44};
    }

    // ================================================================== décor commun
    /**
     * Plateau laqué brun-rouge vu de trois quarts : dessus brillant (reflet en biais), chant avant,
     * coins de laiton, ombre portée. Le dessus va de y0 à y1.
     */
    static void plateau(Toile t, int x0, int y0, int x1, int y1) {
        int ep = 6, dx = 8;
        Dessin.ombre(t, (x0 + x1) / 2.0 + 6, y1 + ep + 3, (x1 - x0) / 2.0 + 6, 6);
        int[] l = Dessin.rampe(LAQUE);
        int h = y1 - y0;
        for (int y = y0; y < y1; y++) {
            int off = (int) Math.round(dx * (y1 - 1 - y) / (double) Math.max(1, h - 1));
            double L = 0.5 + 0.22 * (y - y0) / Math.max(1, h);
            for (int x = x0 + off; x < x1 + off - dx; x++) {
                double reflet = Math.abs(((x - x0) - (y - y0) * 2.2) - (x1 - x0) * 0.35) < 9 ? 0.32 : 0;
                double reflet2 = Math.abs(((x - x0) - (y - y0) * 2.2) - (x1 - x0) * 0.42) < 3 ? 0.2 : 0;
                t.set(x, y, Dessin.bande(l, L + reflet + reflet2, x, y));
            }
        }
        // chant avant et filet doré
        t.rect(x0, y1, x1 - x0 - dx, ep, l[1]);
        t.rect(x0, y1, x1 - x0 - dx, 1, l[3]);
        t.rect(x0, y1 + ep - 1, x1 - x0 - dx, 1, l[0]);
        t.rect(x0 + 3, y1 + 2, x1 - x0 - dx - 6, 1, Dessin.SH);
        // chant droit
        for (int i = 0; i < dx; i++) {
            int yy = y1 - (int) Math.round(i * (h - 1) / (double) dx);
            t.rect(x1 - dx + i, yy, 1, ep, l[0]);
        }
        // coins en laiton
        coin(t, x0, y1 - 1);
        coin(t, x1 - dx - 6, y1 - 1);
        coin(t, x0 + dx, y0);
        coin(t, x1 - 6, y0);
        // contour
        t.ligne(x0 - 1, y1, x0 + dx - 1, y0 - 1, C);
        t.ligneH(x0 + dx - 1, x1, y0 - 1, C);
        t.ligne(x1, y0 - 1, x1, y0 + ep, C);
        t.ligne(x1, y0 + ep, x1 - dx, y1 + ep, C);
        t.ligneH(x0 - 1, x1 - dx, y1 + ep, C);
        t.ligneV(x0 - 1, y1, y1 + ep, C);
    }

    private static void coin(Toile t, int x, int y) {
        t.rect(x, y, 6, 2, Dessin.OL);
        t.rect(x + 1, y, 4, 1, Dessin.HL);
    }

    /** Étiquette de papier attachée par une cordelette rouge (nom du mode, cible…). */
    static void etiquette(Toile t, int cx, int y, String texte) {
        int w = Police.largeur(texte, 1) + 10;
        int x = cx - w / 2;
        t.rect(x + 2, y + 2, w, 11, 0x44000000);
        t.rect(x - 1, y - 1, w + 2, 13, C);
        t.rect(x, y, w, 11, PAPIER);
        t.rect(x, y, w, 1, 0xFFFFF8E4);
        t.rect(x, y + 10, w, 1, 0xFFD8C8A0);
        t.disque(x + 3.5, y + 5.5, 1.6, CORDE_ROUGE);
        t.ligne(x + 3, y + 5, x - 4, y - 3, CORDE_ROUGE);
        Police.texte(t, texte, x + 7, y + 2, 0xFF3A2418, 0, 1);
    }

    /** Couleurs bien distinctes pour les ingrédients sans icône (table de préparation). */
    static final int[] PALETTE = {0xFFD8443A, 0xFF3A6AD8, 0xFFE8C83A, 0xFF4AB84A, 0xFF9A4AC8, 0xFFE8862A, 0xFFF0F0E8, 0xFF7A4A2A};

    /** L'ingrédient k dans une case de 32x32 : son icône, ou un petit tas de poudre d'une couleur propre. */
    static void ingredient(Toile t, ContexteVirus c, int k, int x, int y) {
        int[] ic = k < c.icones.size() ? c.icones.get(k) : null;
        if (ic != null) {
            icone(t, ic, x, y, 1);
            return;
        }
        int col = PALETTE[Math.floorMod(k, PALETTE.length)];
        Toile s = sprite();
        Dessin.ellipseV(s, x + 16, y + 26, 12, 4, Dessin.rampe(0xFFD8D0C0), 0.9, 0.5);
        Dessin.sphere(s, x + 16, y + 21, 10, 7, Dessin.rampe(col), 2);
        for (int g = 0; g < 6; g++) s.set(x + 10 + (g * 5) % 13, y + 17 + (g * 3) % 6, Toile.clair(col, 0.4));
        poser(t, s);
    }

    /** Colle une icône 32x32 à l'échelle 1 ou 2. */
    static void icone(Toile t, int[] ic, int x, int y, int echelle) {
        if (ic == null) return;
        Toile s = new Toile(32, 32, ic);
        if (echelle <= 1) t.coller(s, x, y);
        else t.coller(s, x, y, echelle);
    }

    /** Rangée de pastilles : résultat de chaque coup (doré parfait, vert bien, rouge raté, gris à venir). */
    static void pastilles(Toile t, int[] res, int n, int courant, int cx, int y) {
        if (n <= 0) return;
        int w = Math.min(10, 200 / n);
        int x0 = cx - n * w / 2;
        for (int i = 0; i < n; i++) {
            int x = x0 + i * w + w / 2;
            int r = res == null || i >= res.length ? -1 : res[i];
            int c = r == MiniJeu.PARFAIT ? 0xFFFFC93A : r == MiniJeu.BIEN ? Dessin.G : r == MiniJeu.RATE ? Dessin.RG : 0xFF6A5040;
            t.disque(x + 0.5, y + 0.5, 3.5, C);
            t.disque(x + 0.5, y + 0.5, 2.5, c);
            if (r >= 0) t.set(x, y - 1, Toile.clair(c, 0.5));
            if (i == courant) t.anneau(x + 0.5, y + 0.5, 5.5, 5.5, 1.2, 0xFFFFF4D0);
        }
    }

    /** Petit nuage de vapeur ou de fumée, transparent. */
    static void volute(Toile t, double x, double y, double r, int couleur, double a) {
        if (a <= 0.01) return;
        t.ellipse(x, y, r, r * 0.8, Toile.alpha(couleur, a * 0.55));
        t.ellipse(x - r * 0.3, y - r * 0.2, r * 0.6, r * 0.5, Toile.alpha(Toile.clair(couleur, 0.3), a * 0.5));
    }

    /** Liquide ou poudre : rampe de la couleur de la préparation. */
    static int[] rampe(int c) {
        return Dessin.rampe(c == 0 ? 0xFF8FB860 : c);
    }

    static int hash(long a) {
        a = (a ^ (a >>> 33)) * 0xff51afd7ed558ccdL;
        a = (a ^ (a >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return (int) (a ^ (a >>> 33));
    }

    static double bosse(double age, double duree) {
        if (age < 0 || age > duree) return 0;
        return Math.sin(age / duree * Math.PI);
    }

    static double clamp(double v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }

    /** Dessine un sprite (toile dédiée), pose un contour sombre, puis le colle. */
    static Toile sprite() {
        return new Toile(JeuxVirus.W, JeuxVirus.H);
    }

    static void poser(Toile t, Toile s) {
        s.contour(C);
        t.coller(s, 0, 0);
    }
}
