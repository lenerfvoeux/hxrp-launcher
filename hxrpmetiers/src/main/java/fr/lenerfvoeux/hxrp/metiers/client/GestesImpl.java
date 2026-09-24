package fr.lenerfvoeux.hxrp.metiers.client;

import java.util.Random;

/** Les 17 gestes de cuisine du Gourmet. Plus le rang est élevé, plus c'est exigeant. */
public final class GestesImpl {
    private static final Random R = new Random();

    private GestesImpl() {}

    public static Gestes creer(String geste, int rang) {
        Gestes g;
        switch (geste.toLowerCase().replace("é", "e").replace("ç", "c").replace("î", "i")) {
            case "couper": g = new Couper(); break;
            case "etaler": g = new Etaler(); break;
            case "petrir": g = new Petrir(); break;
            case "faconner": g = new Faconner(); break;
            case "fouetter": g = new Rotation(9, true); break;
            case "melanger": g = new Rotation(4, false); break;
            case "piler": g = new Rythme(true); break;
            case "secouer": g = new Rythme(false); break;
            case "tamiser": g = new Secousse(); break;
            case "saisir": g = new Cuisson(2, "FACE"); break;
            case "four": g = new Cuisson(1, "PLAT"); break;
            case "frire": g = new Cuisson(1, "PANIER"); break;
            case "griller": g = new Griller(); break;
            case "bouillir": g = new Maintien(0.65, "TEMPERATURE", 0); break;
            case "mijoter": g = new Maintien(0.3, "FEU", 2); break;
            case "presser": g = new Presser(); break;
            case "assaisonner": g = new Doser(); break;
            default: g = new Doser(); break;
        }
        g.rang = rang;
        g.init();
        return g;
    }

    // --------------------------------------------------------------- découpe
    public static class Couper extends Gestes {
        public final int[] xs = {134, 150, 166, 182, 198};
        public final boolean[] fait = new boolean[5];
        public final double[] notes = new double[5];
        public int ligne = -1;
        public int knifeX = -99, knifeY = -99;
        private double somme, ech, maxY, ecart;

        @Override public void init() { zoneLo = 1 - 0.6 * largeur(); zoneHi = 1; stat = "COUPES 0/5"; }
        @Override public String consigne() { return "Descends sur chaque pointillé"; }
        @Override public double duree() { return 15; }

        @Override public void down(int x, int y) {
            super.down(x, y);
            for (int i = 0; i < 5; i++) if (!fait[i] && Math.abs(x - xs[i]) < 9 && y < 92) { ligne = i; somme = 0; ech = 0; maxY = y; }
        }

        @Override public void drag(int x, int y) {
            knifeX = x; knifeY = y;
            if (ligne < 0) return;
            ecart = Math.abs(x - xs[ligne]);
            valeur = 1 - Math.min(1, ecart / 10.0);
            if (y >= 86 && y <= 112) { somme += ecart; ech++; }
            maxY = Math.max(maxY, y);
            if (ecart > 14) { fait[ligne] = true; notes[ligne] = 0; ligne = -1; maj(); }
        }

        @Override public void up(int x, int y) {
            if (ligne >= 0 && maxY >= 112 && ech > 0) {
                double tol = new double[]{6, 5, 4, 3}[rang];
                fait[ligne] = true;
                notes[ligne] = Math.max(0, 1 - (somme / ech) / tol);
            }
            ligne = -1;
            valeur = 0;
            maj();
        }

        private void maj() {
            int n = 0;
            for (boolean f : fait) if (f) n++;
            stat = "COUPES " + n + "/5";
            if (n == 5) note = noteFinale();
        }

        @Override public double noteFinale() {
            double s = 0;
            for (double n : notes) s += n;
            return s / 5 * 100;
        }
    }

    // --------------------------------------------------------------- rouleau
    public static class Etaler extends Gestes {
        public final double[] rows = new double[12];
        public double pinY = 170;
        public int dechirures;
        private double lastY, vitesse;
        private long lastT, lastDech;

        @Override public void init() { zoneLo = 0; zoneHi = new double[]{0.75, 0.575, 0.425, 0.325}[rang]; stat = "DECHIRURES 0"; }
        @Override public String consigne() { return "Attrape le rouleau, allers-retours"; }
        @Override public double duree() { return new double[]{15, 14, 12, 12}[rang]; }

        @Override public void down(int x, int y) {
            super.down(x, y);
            if (Math.abs(y - pinY) < 11) { lastY = y; lastT = System.currentTimeMillis(); }
        }

        @Override public void drag(int x, int y) {
            long now = System.currentTimeMillis();
            double dt = Math.max(1, now - lastT) / 1000.0, dy = Math.abs(y - lastY);
            vitesse = vitesse * 0.6 + dy / dt * 0.4;
            valeur = Math.min(1, vitesse / 1000.0);
            int passes = new int[]{2, 3, 3, 4}[rang];
            for (int i = 0; i < 12; i++) if (Math.abs(42 + i * 9 + 4.5 - y) < 9) rows[i] += dy / (18.0 * passes);
            if (valeur > zoneHi && now - lastDech > 450) { dechirures++; lastDech = now; stat = "DECHIRURES " + dechirures; }
            pinY = Math.max(30, Math.min(174, y));
            lastY = y;
            lastT = now;
            boolean fini = true;
            for (double r : rows) if (r < 1) fini = false;
            if (fini) note = noteFinale();
        }

        @Override public void tick(double dt) { vitesse *= 0.9; valeur = Math.min(1, vitesse / 1000.0); }

        @Override public double noteFinale() {
            double avg = 0;
            for (double r : rows) avg += Math.min(1, r);
            return Math.max(0, avg / 12 * 100 - dechirures * new int[]{6, 8, 10, 12}[rang]);
        }
    }

    // --------------------------------------------------------------- pétrir
    public static class Petrir extends Gestes {
        public int dir, plis, total;
        public double fenetre, reste;
        private int x0, y0;
        private long t0;
        private double somme;

        @Override public void init() {
            total = new int[]{6, 8, 10, 12}[rang];
            fenetre = new double[]{2.4, 2, 1.6, 1.25}[rang];
            reste = fenetre;
            zone(0.55, 0.16);
            dir = R.nextInt(4);
            stat = "PLIS 0/" + total;
        }

        @Override public String consigne() { return "Glisse dans le sens de la flèche"; }
        @Override public double duree() { return total * fenetre + 1; }

        @Override public void tick(double dt) {
            if (!commence) return;
            reste -= dt;
            if (reste <= 0) suivant(0);
        }

        @Override public void down(int x, int y) { super.down(x, y); x0 = x; y0 = y; t0 = System.currentTimeMillis(); }

        @Override public void drag(int x, int y) {
            double d = Math.hypot(x - x0, y - y0), s = d / Math.max(0.05, (System.currentTimeMillis() - t0) / 1000.0);
            valeur = clamp(s / 400);
        }

        @Override public void up(int x, int y) {
            double dx = x - x0, dy = y - y0, d = Math.hypot(dx, dy);
            if (d < 25) return;
            int dd = Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? 1 : 3) : (dy > 0 ? 2 : 0);
            double force = clamp(d / Math.max(0.05, (System.currentTimeMillis() - t0) / 1000.0) / 400);
            suivant(dd == dir ? proche(force, zoneLo, zoneHi, 2) : 0);
        }

        private void suivant(double n) {
            somme += n;
            plis++;
            stat = "PLIS " + plis + "/" + total;
            if (plis >= total) { note = noteFinale(); return; }
            int nd;
            do { nd = R.nextInt(4); } while (nd == dir);
            dir = nd;
            reste = fenetre;
        }

        @Override public double noteFinale() { return somme / total * 100; }
    }

    // --------------------------------------------------------------- façonner
    public static class Faconner extends Gestes {
        public final boolean[] couvert = new boolean[48];
        public int trace, bons;
        private long lastT;
        private int lx, ly;

        @Override public void init() { zone(0.45, 0.22); stat = "MOTIF 0 %"; }
        @Override public String consigne() { return "Suis le cercle en pointillés"; }

        @Override public void down(int x, int y) { super.down(x, y); lx = x; ly = y; lastT = System.currentTimeMillis(); }

        @Override public void drag(int x, int y) {
            long now = System.currentTimeMillis();
            double d = Math.hypot(x - lx, y - ly), dt = Math.max(1, now - lastT) / 1000.0;
            valeur = clamp(d / dt / 300);
            lx = x; ly = y; lastT = now;
            double dist = Math.hypot(x - 172, y - 100);
            double tol = new double[]{7, 6, 5, 4}[rang];
            trace++;
            if (Math.abs(dist - 30) <= tol) {
                int seg = (int) (((Math.atan2(y - 100, x - 172) + Math.PI * 2.5) % (Math.PI * 2)) / (Math.PI * 2) * 48);
                couvert[seg % 48] = true;
                if (valeur >= zoneLo && valeur <= zoneHi) bons++;
            }
            int n = 0;
            for (boolean b : couvert) if (b) n++;
            stat = "MOTIF " + (n * 100 / 48) + " %";
            if (n >= 47) note = noteFinale();
        }

        @Override public double noteFinale() {
            int n = 0;
            for (boolean b : couvert) if (b) n++;
            double couv = n / 48.0, prec = trace == 0 ? 0 : bons / (double) trace;
            return couv * 100 * (0.4 + 0.6 * Math.min(1, prec * 1.6));
        }
    }

    // --------------------------------------------------------------- rotation (fouet, spatule)
    public static class Rotation extends Gestes {
        private final double centre;
        private final boolean rapide;
        public double angle, acc, vitesse, tenu, deborde;
        public int mx = 172, my = 106;
        private long lastT;
        private boolean drag;

        public Rotation(double centre, boolean rapide) { this.centre = centre; this.rapide = rapide; }

        @Override public void init() { zone(centre / 20, (rapide ? 4 : 2) / 20.0); stat = "MAINTIEN"; }
        @Override public String consigne() { return rapide ? "Tourne vite dans le bol" : "Tourne lentement, sans déborder"; }
        @Override public double duree() { return rapide ? 9 : 12; }

        @Override public void down(int x, int y) { super.down(x, y); drag = true; mx = x; my = y; angle = Math.atan2(y - 106, x - 172); lastT = System.currentTimeMillis(); }
        @Override public void up(int x, int y) { drag = false; }

        @Override public void drag(int x, int y) {
            long now = System.currentTimeMillis();
            mx = x; my = y;
            double a = Math.atan2(y - 106, x - 172), d = a - angle;
            if (d > Math.PI) d -= Math.PI * 2;
            if (d < -Math.PI) d += Math.PI * 2;
            double dt = Math.max(1, now - lastT) / 1000.0;
            vitesse = vitesse * 0.8 + Math.abs(d) / dt * 0.2;
            acc += d;
            angle = a;
            lastT = now;
            valeur = clamp(vitesse / 20);
        }

        @Override public void tick(double dt) {
            if (!drag) { vitesse *= 0.94; valeur = clamp(vitesse / 20); }
            if (!commence) return;
            if (valeur >= zoneLo && valeur <= zoneHi) tenu += dt;
            else if (valeur > zoneHi + 0.12) deborde += dt;
            stat = "MAINTIEN " + (int) (tenu / duree() * 100) + " %";
        }

        @Override public double noteFinale() { return Math.max(0, tenu / duree() * 100 - deborde * 12); }
    }

    // --------------------------------------------------------------- rythme (mortier, shaker)
    public static class Rythme extends Gestes {
        private final boolean clic;
        public int n, total, combo;
        public double periode, fenetre, phase;
        private double somme;
        private int lastY;
        private int sens;
        private double travel;

        public Rythme(boolean clic) { this.clic = clic; }

        @Override public void init() {
            total = clic ? new int[]{8, 10, 12, 14}[rang] : new int[]{8, 10, 12, 14}[rang];
            periode = clic ? new double[]{1, .85, .72, .6}[rang] : new double[]{.7, .6, .5, .42}[rang];
            fenetre = new double[]{.14, .11, .09, .07}[rang];
            zoneLo = 1 - fenetre / periode;
            zoneHi = 1;
            stat = (clic ? "FRAPPES 0/" : "BATTEMENTS 0/") + total;
        }

        @Override public String consigne() { return clic ? "Frappe quand le cercle touche la cible" : "Secoue de haut en bas sur le tempo"; }
        @Override public double duree() { return total * periode + 1.5; }

        @Override public void tick(double dt) {
            if (!commence) return;
            phase += dt / periode;
            if (phase >= 1 + fenetre / periode) { resultat(0); phase = 0; }
            valeur = clamp(phase);
        }

        @Override public void down(int x, int y) { super.down(x, y); lastY = y; if (clic) frappe(); }

        @Override public void drag(int x, int y) {
            if (clic) return;
            int d = Integer.compare(y - lastY, 0);
            if (d != 0) {
                if (sens > 0 && d < 0 && travel >= 10) { frappe(); travel = 0; }
                travel = d == sens ? travel + Math.abs(y - lastY) : Math.abs(y - lastY);
                sens = d;
            }
            lastY = y;
        }

        private void frappe() {
            double e = Math.abs(1 - phase) * periode;
            resultat(e <= fenetre / 2 ? 1 : e <= fenetre ? 0.6 : 0);
            phase = 0;
        }

        private void resultat(double v) {
            somme += v;
            n++;
            combo = v > 0 ? combo + 1 : 0;
            stat = (clic ? "FRAPPES " : "BATTEMENTS ") + n + "/" + total + (combo > 1 ? "  x" + combo : "");
            if (n >= total) note = noteFinale();
        }

        @Override public double noteFinale() { return somme / total * 100; }
    }

    // --------------------------------------------------------------- tamis
    public static class Secousse extends Gestes {
        public double offset, tamise, besoin;
        public int envols;
        private int lastX;
        private double vitesse;
        private long lastT, lastEnvol;
        private boolean drag;

        @Override public void init() {
            besoin = new double[]{3.5, 4, 4.5, 5}[rang];
            zone(0.5, 0.2);
            stat = "TAMISE 0 %";
        }

        @Override public String consigne() { return "Secoue le tamis gauche-droite"; }
        @Override public double duree() { return new double[]{15, 14, 13, 12}[rang]; }

        @Override public void down(int x, int y) { super.down(x, y); drag = true; lastX = x; lastT = System.currentTimeMillis(); }
        @Override public void up(int x, int y) { drag = false; }

        @Override public void drag(int x, int y) {
            long now = System.currentTimeMillis();
            double dt = Math.max(1, now - lastT) / 1000.0, dx = Math.abs(x - lastX);
            vitesse = vitesse * 0.7 + dx / dt * 0.3;
            offset = Math.max(-24, Math.min(24, offset + (x - lastX)));
            lastX = x;
            lastT = now;
            valeur = clamp(vitesse / 500);
        }

        @Override public void tick(double dt) {
            if (!drag) { vitesse *= 0.93; valeur = clamp(vitesse / 500); }
            if (!commence || !drag) return;
            if (valeur >= zoneLo && valeur <= zoneHi) tamise += dt;
            else if (valeur > zoneHi + 0.2 && System.currentTimeMillis() - lastEnvol > 500) { envols++; lastEnvol = System.currentTimeMillis(); }
            stat = "TAMISE " + (int) Math.min(100, tamise / besoin * 100) + " %";
            if (tamise >= besoin) note = noteFinale();
        }

        @Override public double noteFinale() { return Math.max(0, Math.min(1, tamise / besoin) * 100 - envols * new int[]{6, 8, 10, 12}[rang]); }
    }

    // --------------------------------------------------------------- cuisson à retourner / sortir
    public static class Cuisson extends Gestes {
        private final int faces;
        private final String label;
        public double feu, cuisson;
        public int face;
        private double somme;
        private boolean slider;

        public Cuisson(int faces, String label) { this.faces = faces; this.label = label; }

        @Override public void init() { zone(0.72, new double[]{.1, .08, .06, .045}[rang] / largeur()); stat = label + " 1/" + faces; }
        @Override public String consigne() { return "Règle le feu en bas, puis clique au bon moment"; }
        @Override public double duree() { return 26; }

        @Override public void down(int x, int y) {
            super.down(x, y);
            if (y > 152) { slider = true; feu = clamp((x - 110) / 140.0); return; }
            valider();
        }

        @Override public void drag(int x, int y) { if (slider) feu = clamp((x - 110) / 140.0); }
        @Override public void up(int x, int y) { slider = false; }

        @Override public void tick(double dt) {
            if (!commence) return;
            cuisson += feu * 0.22 * dt;
            valeur = clamp(cuisson);
            if (cuisson >= 1.12) valider0(0);
        }

        private void valider() { valider0(proche(cuisson, zoneLo, zoneHi, 4)); }

        private void valider0(double n) {
            somme += n;
            face++;
            cuisson = 0;
            stat = label + " " + Math.min(face + 1, faces) + "/" + faces;
            if (face >= faces) note = noteFinale();
        }

        @Override public double noteFinale() { return somme / faces * 100; }
    }

    // --------------------------------------------------------------- grill
    public static class Griller extends Gestes {
        public double[] cuisson;
        public int[] faces;
        public double[] rates;
        private double somme;
        private int total;

        @Override public void init() {
            int n = new int[]{2, 3, 4, 5}[rang];
            cuisson = new double[n];
            faces = new int[n];
            rates = new double[n];
            for (int i = 0; i < n; i++) rates[i] = (0.07 + (i % 5) * 0.01) * new double[]{1, 1.15, 1.3, 1.45}[rang];
            total = n * 2;
            zone(0.72, new double[]{.1, .085, .07, .055}[rang] / largeur());
            stat = "PIECES 0/" + n;
        }

        @Override public String consigne() { return "Clique une pièce quand sa jauge est verte"; }
        @Override public double duree() { return 34; }

        @Override public void tick(double dt) {
            if (!commence) return;
            double max = 0;
            for (int i = 0; i < cuisson.length; i++) {
                if (faces[i] >= 2) continue;
                cuisson[i] += rates[i] * dt;
                if (cuisson[i] >= 1.12) retourner(i, 0);
                max = Math.max(max, cuisson[i]);
            }
            valeur = clamp(max);
        }

        @Override public void down(int x, int y) {
            super.down(x, y);
            int n = cuisson.length;
            for (int i = 0; i < n; i++) {
                int px = 138 + (i % 2) * 68, py = 72 + (i / 2) * 34;
                if (faces[i] < 2 && Math.abs(x - px) < 26 && Math.abs(y - py) < 12) retourner(i, proche(cuisson[i], zoneLo, zoneHi, 4));
            }
        }

        private void retourner(int i, double n) {
            somme += n;
            faces[i]++;
            cuisson[i] = 0;
            int finis = 0;
            for (int f : faces) if (f >= 2) finis++;
            stat = "PIECES " + finis + "/" + cuisson.length;
            if (finis == cuisson.length) note = noteFinale();
        }

        @Override public double noteFinale() { return somme / total * 100; }
    }

    // --------------------------------------------------------------- maintien de température
    public static class Maintien extends Gestes {
        private final double cible;
        private final String label;
        private final int alertes;
        public double feu, temp = 0.1, tenu;
        public boolean alerte;
        public double alerteReste, tour;
        public int rates;
        private double prochaine = 4;
        private int posees;
        private boolean slider, stir;
        private double angle;

        public Maintien(double cible, String label, int alertes) { this.cible = cible; this.label = label; this.alertes = alertes; }

        @Override public void init() { zone(cible, new double[]{.12, .1, .08, .06}[rang] / largeur()); stat = label; }
        @Override public String consigne() { return alertes > 0 ? "Feu doux, et remue dès que ça alerte" : "Garde la température dans le vert"; }
        @Override public double duree() { return alertes > 0 ? 25 : 20; }

        @Override public void down(int x, int y) {
            super.down(x, y);
            if (y > 152) { slider = true; feu = clamp((x - 110) / 140.0); }
            else if (alerte) { stir = true; angle = Math.atan2(y - 100, x - 172); }
        }

        @Override public void drag(int x, int y) {
            if (slider) { feu = clamp((x - 110) / 140.0); return; }
            if (!stir || !alerte) return;
            double a = Math.atan2(y - 100, x - 172), d = a - angle;
            if (d > Math.PI) d -= Math.PI * 2;
            if (d < -Math.PI) d += Math.PI * 2;
            angle = a;
            tour += Math.abs(d);
            if (tour >= Math.PI * 2) { alerte = false; tour = 0; }
        }

        @Override public void up(int x, int y) { slider = false; stir = false; }

        @Override public void tick(double dt) {
            if (!commence) return;
            temp += (feu - temp) * 0.45 * dt;
            valeur = clamp(temp);
            if (valeur >= zoneLo && valeur <= zoneHi) tenu += dt;
            if (alertes > 0) {
                prochaine -= dt;
                if (!alerte && posees < alertes * new int[]{1, 2, 2, 3}[rang] && prochaine <= 0) {
                    alerte = true;
                    tour = 0;
                    alerteReste = new double[]{3, 2.6, 2.2, 1.8}[rang];
                    posees++;
                    prochaine = 5;
                }
                if (alerte) {
                    alerteReste -= dt;
                    if (alerteReste <= 0) { alerte = false; rates++; }
                }
                stat = "ATTACHE " + rates;
            } else {
                stat = label + " " + (int) (tenu / duree() * 100) + " %";
            }
        }

        @Override public double noteFinale() { return Math.max(0, tenu / duree() * 100 - rates * new int[]{10, 12, 14, 16}[rang]); }
    }

    // --------------------------------------------------------------- presse-agrumes
    public static class Presser extends Gestes {
        public double pression, jus, besoin;
        public int gicles;
        private boolean hold;
        private long last;

        @Override public void init() {
            besoin = new double[]{3, 3.5, 4, 4.5}[rang];
            zone(0.55, new double[]{.14, .11, .08, .06}[rang] / largeur());
            stat = "JUS 0 %";
        }

        @Override public String consigne() { return "Maintiens le clic, pression régulière"; }
        @Override public double duree() { return 14; }

        @Override public void down(int x, int y) { super.down(x, y); hold = true; }
        @Override public void up(int x, int y) { hold = false; }

        @Override public void tick(double dt) {
            pression = clamp(pression + (hold ? new double[]{.35, .45, .55, .65}[rang] : -0.5) * dt);
            valeur = pression;
            if (!commence) return;
            if (valeur >= zoneLo && valeur <= zoneHi) jus += dt;
            else if (valeur > zoneHi + 0.15 && System.currentTimeMillis() - last > 500) { gicles++; last = System.currentTimeMillis(); }
            stat = "JUS " + (int) Math.min(100, jus / besoin * 100) + " %";
            if (jus >= besoin) note = noteFinale();
        }

        @Override public double noteFinale() { return Math.max(0, Math.min(1, jus / besoin) * 100 - gicles * 8); }
    }

    // --------------------------------------------------------------- doseur
    public static class Doser extends Gestes {
        public double dose, cible;
        private boolean verse;

        @Override public void init() {
            cible = 0.5 + R.nextDouble() * 0.3;
            zone(cible, new double[]{.07, .055, .04, .03}[rang] / largeur());
            stat = "DOSE 0 %";
        }

        @Override public String consigne() { return "Verse, puis relâche sur le trait"; }
        @Override public double duree() { return 12; }

        @Override public void down(int x, int y) {
            super.down(x, y);
            if (dose > 0.05 && x > 250 && y > 140) { note = noteFinale(); return; }
            verse = true;
        }

        @Override public void up(int x, int y) { verse = false; }

        @Override public void tick(double dt) {
            if (verse) dose = clamp(dose + new double[]{.25, .3, .36, .42}[rang] * dt);
            valeur = dose;
            stat = "DOSE " + (int) (dose * 100) + " %";
            if (dose >= 1) note = noteFinale();
        }

        @Override public double noteFinale() { return proche(dose, zoneLo, zoneHi, 3) * 100; }
    }
}
