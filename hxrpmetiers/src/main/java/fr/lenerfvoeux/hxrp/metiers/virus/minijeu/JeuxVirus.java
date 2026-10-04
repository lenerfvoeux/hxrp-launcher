package fr.lenerfvoeux.hxrp.metiers.virus.minijeu;

import fr.lenerfvoeux.hxrp.metiers.minijeu.MiniJeu;

import java.util.Arrays;

/**
 * Les mini-jeux de l'officine du Hunter Virus, sur le moteur du Gourmet (pas de 10 ms, graine tirée par le serveur,
 * journal d'entrées rejoué par le serveur). Tous au clic : flèche, zone verte, jauge de laiton, temps restant.
 * Plus le Virus a d'étoiles, plus c'est exigeant (vitesse, largeur des zones, temps).
 * Aucune dépendance à Minecraft ; tout ce qui touche à l'état passe par StrictMath (même résultat client et serveur).
 * <p>
 * Coordonnées des cibles cliquables en pixels de la scène (320 x 184).
 */
public final class JeuxVirus {
    public static final int W = 320, H = 184;

    private JeuxVirus() {}

    public static MiniJeu creer(String cle, int rang, long graine, int param) {
        switch (cle) {
            case "yagen": return new Yagen(rang, graine, param);
            case "hachoir": return new Hachoir(rang, graine, param);
            case "chaudron": return new Chaudron(rang, graine, param);
            case "alambic": return new Alambic(rang, graine, param);
            case "jarres": return new Verser(rang, graine, param);
            case "balance": return new Balance(rang, graine, param);
            case "pilon": return new Pilon(rang, graine, param);
            case "pilulier": return new Pilulier(rang, graine, param);
            case "table": return new Table(rang, graine, param);
            case "gorgees": return new Gorgees(rang, graine, param);
            case "injection": return new Injection(rang, graine, param);
            case "etaler_soin": return new EtalerSoin(rang, graine, param);
            case "microscope": return new Microscope(rang, graine, param);
            default: return null;
        }
    }

    /** Geste d'administration selon la forme du remède (null : rien à jouer, comme les pilules). */
    public static String gesteAdministration(String forme) {
        switch (forme == null ? "" : forme) {
            case "fiole": case "fumigation": return "gorgees";
            case "seringue": return "injection";
            case "onguent": case "bandage": case "attelle": return "etaler_soin";
            default: return null;
        }
    }

    public static int parametreAdministration(String forme) {
        switch (forme == null ? "" : forme) {
            case "fumigation": return 1;
            case "bandage": case "attelle": return 1;
            default: return 0;
        }
    }

    static double sin(double a) { return StrictMath.sin(a); }

    // ================================================================== RYTHME (yagen, pilon, pilulier)
    /** Base commune : des cibles dans le temps ; clic au bon moment, à une demi-fenêtre près. */
    public abstract static class Rythme extends MiniJeu {
        public int cible;
        public int[] resultats;
        public int fenetre;
        public int tFrappe = -1000, dernierResultat = -1;

        Rythme(int rang, long graine, int param) {
            super(rang, graine, param);
        }

        /** Instant (ms) de la cible k. */
        public abstract int temps(int k);

        /** Écart maximal (ms) au-delà duquel un clic est « trop tôt » (ne compte pas pour la cible). */
        protected abstract int tolerance();

        void initCibles(int n) {
            total = n;
            resultats = new int[n];
            Arrays.fill(resultats, -1);
        }

        @Override protected void tick() {
            if (cible < total && t > temps(cible) + 2 * fenetre) {
                retour(RATE, "TROP TARD");
                resultats[cible] = RATE;
                dernierResultat = RATE;
                cible++;
                fait++;
            }
            if (cible < total) {
                int avant = cible == 0 ? Math.min(0, temps(0) - 900) : temps(cible - 1);
                valeur = clamp((t - avant) / (double) Math.max(1, temps(cible) - avant));
            }
            if (cible >= total) fini = true;
        }

        @Override protected void action(int type, int a, int b) {
            if (!appuiType(type, a) || cible >= total) return;
            int err = Math.abs(t - temps(cible));
            double s;
            if (err > tolerance()) {
                retour(RATE, "TROP TÔT");
                s = 0;
                resultats[cible] = RATE;
            } else {
                s = jugerEcart(err, fenetre);
                resultats[cible] = retours.get(retours.size() - 1).qualite;
            }
            dernierResultat = resultats[cible];
            somme += s;
            tFrappe = t;
            cible++;
            fait++;
            if (cible >= total) fini = true;
        }
    }

    // ================================================================== YAGEN
    /** La roue de fonte roule d'un bout à l'autre de l'auge : cliquer quand elle touche chaque extrémité, 6 allers-retours. */
    public static final class Yagen extends Rythme {
        public final int periode;

        Yagen(int rang, long graine, int param) {
            super(rang, graine, param);
            periode = ri(900, 790, 680, 580);
            fenetre = ri(130, 110, 92, 76);
            initCibles(12);
            zoneLo = 1 - fenetre / (double) periode;
            zoneHi = 1;
        }

        @Override public int temps(int k) { return periode * (k + 1); }
        @Override protected int tolerance() { return periode / 2; }

        /** Position de la roue dans l'auge (0 : gauche, 1 : droite). */
        public double position(double tms) {
            double p = (tms % (2.0 * periode)) / (2.0 * periode);
            double x = p < 0.5 ? p * 2 : 2 - p * 2;
            // la roue ralentit un peu en remontant les bords de l'auge
            return 0.5 - 0.5 * StrictMath.cos(x * Math.PI);
        }

        @Override public String titre() { return "YAGEN"; }
        @Override public String consigne() { return "CLIQUE QUAND LA ROUE TOUCHE UN BORD"; }
        @Override public String commandes() { return "CLIC OU ESPACE"; }
        @Override public String compteur() { return "PASSES"; }
        @Override public int duree() { return temps(total - 1) + 1500; }
    }

    // ================================================================== HACHOIR
    /** La flèche monte et descend : clic dans la zone verte, le levier tranche. 5 coupes. */
    public static final class Hachoir extends MiniJeu {
        public static final int RECUL = 420;
        public final double periode, largeur;
        public final boolean mobile;
        public final int[] qualite = new int[5];
        public final int[] tCoupe = new int[5];

        Hachoir(int rang, long graine, int param) {
            super(rang, graine, param);
            periode = r(2300, 1850, 1450, 1120);
            largeur = r(0.30, 0.24, 0.18, 0.13);
            mobile = rang >= 2;
            total = 5;
            Arrays.fill(tCoupe, -1);
            majZone();
        }

        @Override public String titre() { return "HACHOIR"; }
        @Override public String consigne() { return "ABATS LE LEVIER DANS LE VERT"; }
        @Override public String commandes() { return "CLIC OU ESPACE"; }
        @Override public String compteur() { return "COUPES"; }
        @Override public int duree() { return 20000; }

        private void majZone() {
            double c = 0.5;
            if (mobile) c += (0.5 - largeur / 2 - 0.05) * sin(t / 4800.0 * 2 * Math.PI) * 0.85;
            zone(c, largeur);
        }

        @Override protected void tick() {
            valeur = tri(t, periode);
            majZone();
        }

        public boolean leve() {
            return fait == 0 || t - tCoupe[fait - 1] >= RECUL;
        }

        @Override protected void action(int type, int a, int b) {
            if (!appuiType(type, a) || fait >= 5 || !leve()) return;
            double s = juger(valeur, zoneLo, zoneHi);
            qualite[fait] = retours.get(retours.size() - 1).qualite;
            tCoupe[fait] = t;
            somme += s;
            fait++;
            if (fait >= 5) fini = true;
        }
    }

    // ================================================================== CHAUDRON
    /**
     * La chaleur du brasero baisse toute seule : clic pour attiser et rester dans la zone (douce, forte, réduction,
     * à sec…). Trop chaud : ça déborde (ou ça brûle, à sec). Des coups de vent font chuter la braise.
     */
    public static final class Chaudron extends MiniJeu {
        public static final String[] MODES = {"FEU DOUX", "FEU FORT", "RÉDUCTION", "À SEC", "FONDRE", "ÉBULLITION", "FEU FORT, LONG"};
        private static final double[] CENTRES = {0.36, 0.68, 0.55, 0.82, 0.45, 0.76, 0.68};
        private static final int GRACE = 1500;
        public final int mode;
        public final double baisse, cran;
        public double compte, bouillon, fumee;
        public int souffles, tSouffle = -1000;
        private final int[] vents;
        private int prochainVent, tAlerte;
        private double secSomme, secN;

        Chaudron(int rang, long graine, int param) {
            super(rang, graine, param);
            mode = param >= 0 && param < CENTRES.length ? param : 1;
            double w = r(0.26, 0.22, 0.18, 0.14) - (mode == 3 ? 0.02 : 0);
            zone(CENTRES[mode], w);
            rouge = zoneHi + (mode == 3 ? 0.08 : 0.16);
            baisse = r(0.15, 0.19, 0.24, 0.29);
            cran = 0.085;
            valeur = 0.15;
            int n = ri(0, 2, 3, 4);
            vents = new int[n];
            for (int i = 0; i < n; i++) vents[i] = 3500 + i * ((duree() - 5000) / Math.max(1, n)) + rng.nextInt(1500);
            total = 100;
        }

        @Override public String titre() { return "CHAUDRON"; }
        @Override public String consigne() { return MODES[mode] + " · CHALEUR DANS LE VERT"; }
        @Override public String commandes() { return "CLIC, ESPACE OU MOLETTE : ATTISER"; }
        @Override public String compteur() { return "CUISSON"; }
        @Override public int duree() { return mode == 6 ? 18000 : 14000; }

        @Override protected void tick() {
            valeur = Math.max(0, valeur - baisse * DT * (0.6 + 0.6 * valeur));
            if (prochainVent < vents.length && t >= vents[prochainVent]) {
                prochainVent++;
                valeur = Math.max(0, valeur - 0.13);
                retour(INFO, "COUP DE VENT");
            }
            bouillon = valeur;
            fumee = valeur > rouge ? Math.min(1, fumee + DT) : Math.max(0, fumee - DT * 0.5);
            if (t <= GRACE) return;
            double credit;
            if (valeur >= zoneLo && valeur <= zoneHi) credit = 1;
            else {
                double e = valeur < zoneLo ? zoneLo - valeur : valeur - zoneHi;
                credit = Math.max(0, 1 - e / 0.12) * 0.5;
            }
            if (valeur > rouge) {
                credit = 0;
                if (t - tAlerte >= 700) {
                    tAlerte = t;
                    retour(RATE, mode == 3 ? "ÇA BRÛLE !" : "ÇA DÉBORDE !");
                }
            }
            compte += credit;
            secSomme += credit;
            secN++;
            if ((t - GRACE) % 1000 == 0) {
                double q = secSomme / Math.max(1, secN);
                if (q >= 0.9) retour(PARFAIT, "PARFAIT");
                else if (q >= 0.5) retour(BIEN, "BIEN");
                else if (valeur <= rouge) retour(RATE, valeur < zoneLo ? "PLUS CHAUD !" : "TROP CHAUD !");
                secSomme = 0;
                secN = 0;
            }
            fait = (int) Math.round(note());
        }

        @Override protected void action(int type, int a, int b) {
            int n = 0;
            if (type == MOLETTE && a > 0) n = 1;
            else if (appuiType(type, a)) n = 1;
            if (n == 0) return;
            souffles++;
            tSouffle = t;
            valeur = Math.min(1, valeur + cran);
        }

        @Override public double note() {
            return clamp(compte / ((duree() - GRACE) / (double) PAS)) * 100;
        }
    }

    // ================================================================== ALAMBIC
    /**
     * Deux jauges : le feu (clic gauche) et le refroidissement du serpentin (clic droit) baissent tout seuls ;
     * il faut garder les deux dans leur zone. Trop de feu sans assez de froid : la vapeur siffle et se perd.
     */
    public static final class Alambic extends MiniJeu {
        public static final String[] MODES = {"EAU FLORALE", "DISTILLATION", "CONCENTRER"};
        private static final double[] CENTRES = {0.52, 0.62, 0.70};
        private static final int GRACE = 1500;
        public final int mode;
        public final double baisseFeu, baisseFroid;
        public double froid = 0.3, froidLo, froidHi, compte, gouttes;
        public int tFeu = -1000, tFroid = -1000, tSiffle;
        private double secSomme, secN;

        Alambic(int rang, long graine, int param) {
            super(rang, graine, param);
            mode = param >= 0 && param < CENTRES.length ? param : 0;
            zone(CENTRES[mode], r(0.26, 0.22, 0.18, 0.14));
            double wf = r(0.30, 0.26, 0.22, 0.18);
            froidLo = 0.5 - wf / 2;
            froidHi = 0.5 + wf / 2;
            baisseFeu = r(0.13, 0.16, 0.20, 0.24);
            baisseFroid = r(0.11, 0.14, 0.18, 0.22);
            valeur = 0.2;
            total = 100;
        }

        @Override public String titre() { return "ALAMBIC"; }
        @Override public String consigne() { return "GARDE LE FEU ET LE FROID DANS LEUR ZONE"; }
        @Override public String commandes() { return "CLIC GAUCHE : FEU   CLIC DROIT : FROID"; }
        @Override public String compteur() { return "DISTILLÉ"; }
        @Override public int duree() { return 16000; }

        public boolean feuOk() { return valeur >= zoneLo && valeur <= zoneHi; }
        public boolean froidOk() { return froid >= froidLo && froid <= froidHi; }
        public boolean siffle() { return valeur > zoneHi + 0.05 && froid < froidLo; }

        @Override protected void tick() {
            valeur = Math.max(0, valeur - baisseFeu * DT);
            froid = Math.max(0, froid - baisseFroid * DT * (0.6 + valeur));
            if (t <= GRACE) return;
            double credit = (feuOk() ? 0.5 : 0) + (froidOk() ? 0.5 : 0);
            if (siffle()) {
                credit = 0;
                if (t - tSiffle >= 800) {
                    tSiffle = t;
                    retour(RATE, "ÇA SIFFLE !");
                }
            }
            compte += credit;
            gouttes += credit * DT;
            secSomme += credit;
            secN++;
            if ((t - GRACE) % 1000 == 0) {
                double q = secSomme / Math.max(1, secN);
                if (q >= 0.9) retour(PARFAIT, "PARFAIT");
                else if (q >= 0.5) retour(BIEN, "BIEN");
                else if (!siffle()) retour(RATE, !feuOk() ? (valeur < zoneLo ? "PLUS DE FEU" : "MOINS DE FEU") : "REFROIDIS !");
                secSomme = 0;
                secN = 0;
            }
            fait = (int) Math.round(note());
        }

        @Override protected void action(int type, int a, int b) {
            if (type == CLIC || (type == TOUCHE && (a == GAUCHE || a == ESPACE))) {
                valeur = Math.min(1, valeur + 0.1);
                tFeu = t;
            } else if (type == CLIC_DROIT || (type == TOUCHE && a == DROITE)) {
                froid = Math.min(1, froid + 0.1);
                tFroid = t;
            }
        }

        @Override public double note() {
            return clamp(compte / ((duree() - GRACE) / (double) PAS)) * 100;
        }
    }

    // ================================================================== JARRES : VERSER
    /** Verser dans la jarre : maintenir le clic, relâcher pile sur le trait. Puis la macération se fait en temps réel. */
    public static final class Verser extends MiniJeu {
        public final double vitesse, tol;
        public final double[] cibles, doses;
        public int phase, dose, tFin;
        private int tAppui;
        private boolean souris, espace;

        Verser(int rang, long graine, int param) {
            super(rang, graine, param);
            total = ri(2, 2, 3, 3);
            vitesse = r(0.34, 0.41, 0.49, 0.58);
            tol = r(0.05, 0.04, 0.03, 0.022);
            cibles = new double[total];
            doses = new double[total];
            for (int i = 0; i < total; i++) cibles[i] = 0.42 + rng.nextDouble() * 0.42;
            viser();
        }

        private void viser() {
            if (dose < total) {
                trait = cibles[dose];
                zoneLo = trait - tol;
                zoneHi = trait + tol;
            }
        }

        @Override public String titre() { return "JARRES"; }
        @Override public String consigne() { return "MAINTIENS POUR VERSER, LÂCHE SUR LE TRAIT"; }
        @Override public String commandes() { return "MAINTENIR CLIC OU ESPACE"; }
        @Override public String compteur() { return "VERSÉS"; }
        @Override public int duree() { return 24000; }

        @Override protected void tick() {
            if (phase == 1) {
                double tenu = (t - tAppui) / 1000.0;
                valeur += vitesse * DT * (1 + 0.5 * tenu);
                if (valeur >= 1) {
                    valeur = 1;
                    retour(RATE, "ÇA DÉBORDE !");
                    finirDose(0);
                }
            } else if (phase == 2 && t - tFin >= 600) {
                if (dose >= total) fini = true;
                else {
                    phase = 0;
                    valeur = 0;
                    viser();
                }
            }
        }

        @Override protected void action(int type, int a, int b) {
            if (appuiType(type, a)) {
                if (type == CLIC) souris = true; else espace = true;
                if (phase == 0) {
                    phase = 1;
                    tAppui = t;
                }
            } else if (relacheType(type, a)) {
                if (type == RELACHE) souris = false; else espace = false;
                if (phase == 1 && !souris && !espace) {
                    double e = Math.abs(valeur - trait), s;
                    if (e <= tol * 0.4) {
                        retour(PARFAIT, "PARFAIT");
                        s = 100;
                    } else if (e <= tol) {
                        retour(BIEN, "BIEN");
                        s = 95 - 25 * (e - 0.4 * tol) / (0.6 * tol);
                    } else {
                        retour(RATE, valeur < trait ? "PAS ASSEZ" : "TROP");
                        s = e <= 3 * tol ? 40 * (1 - (e - tol) / (2 * tol)) : 0;
                    }
                    finirDose(s);
                }
            }
        }

        private void finirDose(double s) {
            somme += s;
            doses[dose] = valeur;
            dose++;
            fait = dose;
            phase = 2;
            tFin = t;
        }
    }

    // ================================================================== BALANCE
    /**
     * Atteindre la masse cible avec des poids de 1, 2, 5 et 10 g en un nombre limité de coups (poser ou retirer
     * un poids = un coup). Dose stricte : dépasser la masse gâche tout.
     */
    public static final class Balance extends MiniJeu {
        public static final int[] POIDS = {1, 2, 5, 10};
        public static final int BX0 = 132, BDX = 40, BY = 158, BR = 13;
        public final boolean stricte;
        public final int cible, optimal, coupsMax;
        public int masse, coups, tCoup = -1000;
        public final int[] plateau = new int[64];
        public int n;
        private int resultat = -1;

        Balance(int rang, long graine, int param) {
            super(rang, graine, param);
            stricte = param == 1;
            cible = stricte ? 11 + rng.nextInt(19) : 7 + rng.nextInt(26);
            optimal = optimal(cible);
            coupsMax = optimal + (stricte ? ri(2, 2, 1, 1) : ri(4, 3, 2, 1));
            total = coupsMax;
            zone(0.8, 0.035);
            rouge = stricte ? 0.83 : 2;
        }

        public static int optimal(int m) {
            int c = 0;
            for (int k = POIDS.length - 1; k >= 0; k--) {
                c += m / POIDS[k];
                m %= POIDS[k];
            }
            return c;
        }

        public static int boutonX(int k) { return BX0 + k * BDX; }

        @Override public String titre() { return "BALANCE"; }
        @Override public String consigne() { return stricte ? "DOSE STRICTE : " + cible + " G EN " + coupsMax + " COUPS" : "PÈSE " + cible + " G EN " + coupsMax + " COUPS AU PLUS"; }
        @Override public String commandes() { return "CLIC : POSER UN POIDS   CLIC DROIT : RETIRER"; }
        @Override public String compteur() { return "COUPS"; }
        @Override public int duree() { return ri(22000, 19000, 16000, 13000); }

        @Override protected void tick() {
            valeur = clamp(masse / (cible * 1.25));
            fait = coups;
            if (!fini && t >= duree() - PAS) conclure(false);
        }

        @Override protected void action(int type, int a, int b) {
            if (resultat >= 0) return;
            int k = -1;
            if (type == CLIC) {
                for (int i = 0; i < POIDS.length; i++) if (Math.hypot(a - boutonX(i), b - BY) <= BR + 2) k = i;
                if (k < 0) {
                    if (a > 150 && a < 290 && b > 40 && b < 120) retirer();
                    return;
                }
            } else if (type == TOUCHE && a >= CHIFFRE + 1 && a <= CHIFFRE + 4) k = a - CHIFFRE - 1;
            else if (type == CLIC_DROIT || (type == TOUCHE && a == BAS)) {
                retirer();
                return;
            }
            if (k < 0 || n >= plateau.length) return;
            plateau[n++] = POIDS[k];
            masse += POIDS[k];
            coup();
        }

        private void retirer() {
            if (n == 0) return;
            masse -= plateau[--n];
            coup();
        }

        private void coup() {
            coups++;
            tCoup = t;
            valeur = clamp(masse / (cible * 1.25));
            if (stricte && masse > cible) {
                retour(RATE, "SURDOSE !");
                somme = 0;
                resultat = 0;
                fini = true;
                return;
            }
            if (masse == cible) conclure(true);
            else if (coups >= coupsMax) conclure(false);
        }

        private void conclure(boolean juste) {
            if (resultat >= 0) return;
            if (juste) {
                int extra = Math.max(0, coups - optimal);
                somme = Math.max(70, 100 - 6 * extra);
                retour(extra == 0 ? PARFAIT : BIEN, extra == 0 ? "PARFAIT" : "JUSTE");
            } else {
                int e = Math.abs(masse - cible);
                somme = Math.max(0, 55 - 15 * e);
                retour(RATE, e == 0 ? "JUSTE" : masse < cible ? "PAS ASSEZ" : "TROP LOURD");
            }
            resultat = (int) somme;
            fini = true;
        }

        @Override public double note() {
            return resultat < 0 ? 0 : somme;
        }

        /** Inclinaison du fléau (-1 : poids trop légers, 1 : trop lourds). */
        public double inclinaison() {
            return Math.max(-1, Math.min(1, (masse - cible) / 8.0));
        }
    }

    // ================================================================== MORTIER
    /** Le cercle se referme sur le pilon : frappe au moment où il touche la cible. */
    public static final class Pilon extends Rythme {
        public final int periode, avance = 900;

        Pilon(int rang, long graine, int param) {
            super(rang, graine, param);
            initCibles(ri(8, 10, 12, 14));
            periode = ri(1100, 950, 800, 680);
            fenetre = ri(120, 100, 85, 70);
            zoneLo = 1 - fenetre / (double) periode;
            zoneHi = 1;
        }

        @Override public int temps(int k) { return avance + k * periode; }
        @Override protected int tolerance() { return periode / 2; }
        @Override public String titre() { return "MORTIER"; }
        @Override public String consigne() { return "FRAPPE QUAND LE CERCLE SE REFERME"; }
        @Override public String commandes() { return "CLIC OU ESPACE"; }
        @Override public String compteur() { return "COUPS"; }
        @Override public int duree() { return avance + total * periode + 1200; }
    }

    // ================================================================== PILULIER
    /** La lame passe sur six rainures inégalement espacées : clic quand elle est sur chacune. */
    public static final class Pilulier extends Rythme {
        public static final int DEPART = 900;
        public final int passage;
        public final double[] rainures = new double[6];

        Pilulier(int rang, long graine, int param) {
            super(rang, graine, param);
            initCibles(6);
            passage = ri(4200, 3600, 3000, 2500);
            fenetre = ri(115, 98, 82, 68);
            // positions irrégulières, au moins 9 % d'écart (pas de rythme régulier à suivre)
            double x = 0.10;
            for (int i = 0; i < 6; i++) {
                x += 0.09 + rng.nextDouble() * 0.05;
                rainures[i] = x;
            }
            double k = 0.86 / x;
            for (int i = 0; i < 6; i++) rainures[i] = 0.07 + (rainures[i] - 0.10) * k;
            zoneLo = 1 - fenetre / 600.0;
            zoneHi = 1;
        }

        @Override public int temps(int k) { return DEPART + (int) Math.round(rainures[k] * passage); }
        @Override protected int tolerance() { return Math.max(2 * fenetre, (int) (0.045 * passage)); }

        /** Position de la lame (0-1). */
        public double lame(double tms) { return clamp((tms - DEPART) / passage); }

        @Override public String titre() { return "PILULIER"; }
        @Override public String consigne() { return "CLIQUE QUAND LA LAME PASSE UNE RAINURE"; }
        @Override public String commandes() { return "CLIC OU ESPACE"; }
        @Override public String compteur() { return "PILULES"; }
        @Override public int duree() { return DEPART + passage + 1500; }
    }

    // ================================================================== TABLE DE PRÉPARATION
    /**
     * Mémoriser l'ordre des ingrédients montré un instant, les replacer dans l'ordre (clic sur les godets),
     * puis étaler (flèche + zone, trois passes).
     */
    public static final class Table extends MiniJeu {
        public static final int DEBUT = 400, SY = 142, SR = 15;
        public final int nb, montre;
        /** Ordre à retenir (indices d'ingrédients) et godets présentés (indice d'ingrédient par case). */
        public final int[] ordre, godets;
        public int phase, place, justes, passes, tPasse = -1000, tPlace = -1000;
        public final int[] places;
        public final double periode, largeur;
        private double scoreEtaler;

        Table(int rang, long graine, int param) {
            super(rang, graine, param);
            nb = ri(3, 4, 5, 6);
            montre = ri(3000, 2600, 2200, 1800);
            ordre = melange(nb);
            godets = melange(nb);
            places = new int[nb];
            Arrays.fill(places, -1);
            periode = r(2000, 1700, 1400, 1120);
            largeur = r(0.28, 0.23, 0.18, 0.14);
            total = nb + 3;
            zone(0.5, largeur);
        }

        private int[] melange(int n) {
            int[] p = new int[n];
            for (int i = 0; i < n; i++) p[i] = i;
            for (int i = n - 1; i > 0; i--) {
                int j = rng.nextInt(i + 1);
                int x = p[i];
                p[i] = p[j];
                p[j] = x;
            }
            return p;
        }

        public static int godetX(int j, int n) { return 176 - (n - 1) * 20 + j * 40; }

        public boolean montre(double tms) { return phase == 0 && tms < DEBUT + montre; }

        @Override public String titre() { return "TABLE"; }
        @Override public String consigne() { return "RETIENS L'ORDRE, REPLACE-LE, PUIS ÉTALE"; }
        @Override public String commandes() { return "CLIC SUR LES GODETS, PUIS CLIC OU ESPACE"; }
        @Override public String compteur() { return "GESTES"; }
        @Override public int duree() { return 30000; }

        @Override protected void tick() {
            if (phase == 0 && t >= DEBUT + montre) phase = 1;
            if (phase == 2) valeur = tri(t, periode);
            else valeur = phase == 0 ? clamp((t - DEBUT) / (double) montre) : place / (double) nb;
        }

        @Override protected void action(int type, int a, int b) {
            if (phase == 1 && type == CLIC) {
                int j = -1;
                for (int i = 0; i < nb; i++) if (Math.abs(a - godetX(i, nb)) <= SR && Math.abs(b - SY) <= SR + 4) j = i;
                if (j < 0) return;
                for (int p : places) if (p == j) return;   // godet déjà posé
                places[place] = j;
                tPlace = t;
                if (godets[j] == ordre[place]) {
                    justes++;
                    retour(BIEN, "BIEN", godetX(j, nb), SY);
                } else retour(RATE, "PAS ÇA", godetX(j, nb), SY);
                place++;
                fait = place;
                if (place >= nb) {
                    phase = 2;
                    retour(INFO, "ÉTALE !");
                }
            } else if (phase == 2 && appuiType(type, a) && t - tPasse >= 350) {
                scoreEtaler += juger(valeur, zoneLo, zoneHi);
                tPasse = t;
                passes++;
                fait = nb + passes;
                if (passes >= 3) fini = true;
            }
        }

        @Override public double note() {
            return 50.0 * justes / nb + 0.5 * scoreEtaler / 3;
        }
    }

    // ================================================================== ADMINISTRATION : GORGÉES
    /** Faire boire trois gorgées (ou respirer trois fois la fumée) : clic quand la flèche est dans le vert. */
    public static final class Gorgees extends MiniJeu {
        public static final int RECUL = 650;
        public final boolean fumee;
        public final double periode;
        public final int[] tGorgee = new int[3];
        public final int[] qualite = new int[3];

        Gorgees(int rang, long graine, int param) {
            super(rang, graine, param);
            fumee = param == 1;
            periode = r(2200, 1800, 1450, 1150);
            zone(0.55, r(0.30, 0.25, 0.20, 0.15));
            total = 3;
            Arrays.fill(tGorgee, -1);
        }

        @Override public String titre() { return fumee ? "FUMIGATION" : "GORGÉES"; }
        @Override public String consigne() { return fumee ? "FAIS RESPIRER LA FUMÉE DANS LE VERT" : "INCLINE LA FIOLE DANS LE VERT"; }
        @Override public String commandes() { return "CLIC OU ESPACE"; }
        @Override public String compteur() { return fumee ? "INSPIRATIONS" : "GORGÉES"; }
        @Override public int duree() { return 15000; }

        @Override protected void tick() {
            valeur = tri(t + periode / 4, periode);
        }

        @Override protected void action(int type, int a, int b) {
            if (!appuiType(type, a) || fait >= 3 || (fait > 0 && t - tGorgee[fait - 1] < RECUL)) return;
            double s = juger(valeur, zoneLo, zoneHi);
            qualite[fait] = retours.get(retours.size() - 1).qualite;
            tGorgee[fait] = t;
            somme += s;
            fait++;
            if (fait >= 3) fini = true;
        }
    }

    // ================================================================== ADMINISTRATION / PRISE DE SANG : SERINGUE
    /**
     * Trouver la veine (la zone verte roule le long du bras : clic quand l'aiguille est dessus), puis maintenir
     * l'aspiration (ou l'injection) dans la zone pendant 3 secondes, sans forcer.
     * param 0 : injecter un remède ; 1 : prise de sang.
     */
    public static final class Injection extends MiniJeu {
        public static final int TENIR = 3000;
        public final boolean prise;
        public final double periodeAiguille, periodeVeine, largeurVeine, monte;
        public int phase, tPique = -1, essais;
        public double scoreVeine, tenu, aiguille, piston;
        public int forts, tFort;
        private final double dephasage;
        private boolean souris, espace;
        public boolean appuye;

        Injection(int rang, long graine, int param) {
            super(rang, graine, param);
            prise = param == 1;
            periodeAiguille = r(1800, 1500, 1250, 1000);
            periodeVeine = r(3400, 2900, 2400, 2000);
            largeurVeine = r(0.20, 0.16, 0.13, 0.10);
            monte = r(0.50, 0.60, 0.72, 0.85);
            dephasage = rng.nextDouble() * Math.PI * 2;
            total = 100;
            majVeine();
        }

        private void majVeine() {
            double c = 0.5 + (0.5 - largeurVeine / 2 - 0.06) * sin(t / periodeVeine * 2 * Math.PI + dephasage);
            zone(c, largeurVeine);
        }

        @Override public String titre() { return prise ? "PRISE DE SANG" : "INJECTION"; }
        @Override public String consigne() { return phase == 0 ? "PIQUE QUAND L'AIGUILLE EST SUR LA VEINE" : prise ? "ASPIRE DOUCEMENT : GARDE LE PISTON DANS LE VERT" : "INJECTE DOUCEMENT : GARDE LE PISTON DANS LE VERT"; }
        @Override public String commandes() { return "CLIC POUR PIQUER, PUIS MAINTENIR"; }
        @Override public String compteur() { return prise ? "SANG" : "INJECTÉ"; }
        @Override public int duree() { return 20000; }

        @Override protected void tick() {
            if (phase == 0) {
                majVeine();
                aiguille = tri(t, periodeAiguille);
                valeur = aiguille;
            } else {
                appuye = souris || espace;
                piston = clamp(piston + (appuye ? monte : -0.7) * DT);
                valeur = piston;
                if (piston >= rouge) {
                    if (t - tFort >= 600) {
                        tFort = t;
                        forts++;
                        retour(RATE, "TROP FORT !");
                    }
                } else if (piston >= zoneLo && piston <= zoneHi) {
                    tenu += PAS;
                }
                fait = (int) Math.min(100, Math.round(100 * tenu / TENIR));
                if (tenu >= TENIR) {
                    retour(PARFAIT, prise ? "FLACON PLEIN" : "INJECTÉ");
                    fini = true;
                }
            }
        }

        @Override protected void action(int type, int a, int b) {
            if (phase == 0) {
                if (!appuiType(type, a)) return;
                scoreVeine = juger(aiguille, zoneLo, zoneHi);
                tPique = t;
                essais++;
                phase = 1;
                zone(0.55, r(0.24, 0.20, 0.16, 0.13));
                rouge = 0.9;
                if (type == CLIC) souris = true; else espace = true;
                return;
            }
            if (appuiType(type, a)) {
                if (type == CLIC) souris = true; else espace = true;
            } else if (relacheType(type, a)) {
                if (type == RELACHE) souris = false; else espace = false;
            }
        }

        @Override public double note() {
            double b = Math.max(0, 100 * Math.min(1, tenu / TENIR) - 8 * forts);
            return phase == 0 ? 0 : 0.35 * scoreVeine + 0.65 * b;
        }
    }

    // ================================================================== ADMINISTRATION : ÉTALER / SERRER
    /** Étaler l'onguent (ou serrer le bandage) en trois passages : maintenir, relâcher avant le rouge. */
    public static final class EtalerSoin extends MiniJeu {
        public static final int ATTENTE = 0, POUSSE = 1, RETOUR = 2;
        public final boolean serrer;
        public int phase, dechirures;
        public final double vitesse;
        public final double[] etendue = new double[3];
        private boolean souris, espace;

        EtalerSoin(int rang, long graine, int param) {
            super(rang, graine, param);
            serrer = param == 1;
            total = 3;
            vitesse = r(0.42, 0.52, 0.64, 0.78);
            zone(0.72, r(0.26, 0.21, 0.17, 0.13));
            rouge = 0.9;
        }

        @Override public String titre() { return serrer ? "SERRER" : "ÉTALER"; }
        @Override public String consigne() { return serrer ? "SERRE : MAINTIENS, LÂCHE AVANT LE ROUGE" : "ÉTALE : MAINTIENS, LÂCHE AVANT LE ROUGE"; }
        @Override public String commandes() { return "MAINTENIR CLIC OU ESPACE"; }
        @Override public String compteur() { return "PASSAGES"; }
        @Override public int duree() { return 20000; }

        private boolean tenu() { return souris || espace; }

        @Override protected void tick() {
            if (phase == POUSSE) {
                valeur += vitesse * DT * (1 + 0.7 * valeur);
                if (valeur >= rouge) {
                    retour(RATE, serrer ? "TROP SERRÉ !" : "ÇA DÉBORDE !");
                    dechirures++;
                    etendue[fait] = -1;
                    fait++;
                    phase = RETOUR;
                }
            } else if (phase == RETOUR) {
                valeur -= 2.4 * DT;
                if (valeur <= 0) {
                    valeur = 0;
                    phase = ATTENTE;
                    if (fait >= total) fini = true;
                }
            }
        }

        @Override protected void action(int type, int a, int b) {
            if (appuiType(type, a)) {
                if (type == CLIC) souris = true; else espace = true;
                if (phase == ATTENTE && fait < total) phase = POUSSE;
            } else if (relacheType(type, a)) {
                if (type == RELACHE) souris = false; else espace = false;
                if (phase == POUSSE && !tenu()) {
                    double s = juger(valeur, zoneLo, zoneHi);
                    somme += s;
                    etendue[fait] = s / 100;
                    fait++;
                    phase = RETOUR;
                }
            }
        }
    }

    // ================================================================== MICROSCOPE
    /**
     * Rendre l'image nette à ×10, puis ×40, puis ×100 avant la fin du temps : molette pour le grossissement,
     * clic gauche / droit pour la mise au point (qui dérive toute seule, et se dérègle à chaque changement d'objectif).
     */
    public static final class Microscope extends MiniJeu {
        public static final int[] ZOOMS = {4, 10, 40, 100};
        public final double tol, derive, pas;
        public final int tenir;
        public int zoom, etape, tEtape, tNet = -1, tReglage = -1000;
        public double mise = 0.5, vitesse;
        public final double[] nettes = new double[3];
        public final double[] scores = new double[3];
        private int prochainChangement;

        Microscope(int rang, long graine, int param) {
            super(rang, graine, param);
            tol = r(0.30, 0.25, 0.20, 0.15);
            derive = r(0.03, 0.045, 0.06, 0.075);
            pas = 0.035;
            tenir = ri(600, 700, 800, 900);
            for (int i = 0; i < 3; i++) nettes[i] = 0.2 + rng.nextDouble() * 0.6;
            mise = 0.5 + (rng.nextBoolean() ? 0.3 : -0.3);
            total = 3;
            zone(1 - tol / 2, tol);
        }

        public double nettete() {
            if (etape >= 3) return 1;
            return clamp(1 - Math.abs(mise - nettes[etape]) / 0.5);
        }

        public boolean bonZoom() { return etape < 3 && zoom == etape + 1; }

        @Override public String titre() { return "MICROSCOPE"; }
        @Override public String consigne() { return "ZOOME ×10, ×40, ×100 ET FAIS LE POINT"; }
        @Override public String commandes() { return "MOLETTE · CLIC GAUCHE / DROIT · Q / D"; }
        @Override public String compteur() { return "NET"; }
        @Override public int duree() { return ri(36000, 32000, 28000, 24000); }

        @Override protected void tick() {
            if (t >= prochainChangement) {
                vitesse = (rng.nextDouble() * 2 - 1) * derive;
                prochainChangement = t + 500 + rng.nextInt(700);
            }
            mise = clamp(mise + vitesse * DT);
            valeur = bonZoom() ? nettete() : nettete() * 0.4;
            if (etape >= 3) return;
            if (bonZoom() && valeur >= zoneLo) {
                if (tNet < 0) tNet = t;
                if (t - tNet >= tenir) {
                    int duree = t - tEtape;
                    double s = duree <= 5000 ? 100 : Math.max(70, 100 - (duree - 5000) / 250.0);
                    scores[etape] = s;
                    somme += s;
                    retour(s >= 95 ? PARFAIT : BIEN, "×" + ZOOMS[etape + 1] + " NET");
                    etape++;
                    fait = etape;
                    tEtape = t;
                    tNet = -1;
                    if (etape >= 3) fini = true;
                }
            } else tNet = -1;
        }

        @Override protected void action(int type, int a, int b) {
            if (etape >= 3) return;
            if (type == MOLETTE) {
                int z = Math.max(0, Math.min(3, zoom + (a > 0 ? 1 : -1)));
                if (z != zoom) {
                    zoom = z;
                    mise = clamp(mise + (rng.nextBoolean() ? 0.18 : -0.18));
                    tNet = -1;
                }
            } else if (type == CLIC || (type == TOUCHE && a == GAUCHE)) {
                mise = clamp(mise - pas);
                tReglage = t;
            } else if (type == CLIC_DROIT || (type == TOUCHE && a == DROITE)) {
                mise = clamp(mise + pas);
                tReglage = t;
            } else if (type == TOUCHE && (a == HAUT || a == BAS)) {
                action(MOLETTE, a == HAUT ? 1 : -1, 0);
            }
        }
    }
}
