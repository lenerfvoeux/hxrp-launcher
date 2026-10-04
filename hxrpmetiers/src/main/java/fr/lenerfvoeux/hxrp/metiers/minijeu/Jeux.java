package fr.lenerfvoeux.hxrp.metiers.minijeu;

import java.util.Arrays;
import java.util.Locale;

/**
 * Les 17 gestes du Gourmet. Chaque geste ne repose que sur des entrées fiables, et la difficulté
 * ne joue que sur la vitesse, la largeur de zone, le nombre de répétitions et le délai de réaction.
 * <p>
 * Les coordonnées (cibles, crans du feu, pièces du grill) sont en pixels de la scène (320 x 184).
 */
public final class Jeux {
    public static final int W = 320, H = 184;

    private Jeux() {}

    /** Identifiant de geste sans accents ni majuscules : « Pétrir » -> « petrir ». */
    public static String cle(String geste) {
        String g = geste == null ? "" : geste.toLowerCase(Locale.ROOT);
        g = g.replace('é', 'e').replace('è', 'e').replace('ê', 'e').replace('ç', 'c').replace('î', 'i').replace('â', 'a');
        return g.trim();
    }

    public static MiniJeu creer(String geste, int rang, long graine, int param) {
        switch (cle(geste)) {
            case "couper": return new Couper(rang, graine, param);
            case "etaler": return new Etaler(rang, graine, param);
            case "petrir": return new Petrir(rang, graine, param);
            case "faconner": return new Faconner(rang, graine, param);
            case "fouetter": return new Batteur(rang, graine, param, true);
            case "melanger": return new Batteur(rang, graine, param, false);
            case "piler": return new Rythme(rang, graine, param, true);
            case "secouer": return new Rythme(rang, graine, param, false);
            case "tamiser": return new Tamiser(rang, graine, param);
            case "saisir": return new Saisir(rang, graine, param);
            case "bouillir": return new Chauffe(rang, graine, param, false);
            case "mijoter": return new Chauffe(rang, graine, param, true);
            case "four": return new Four(rang, graine, param);
            case "griller": return new Griller(rang, graine, param);
            case "frire": return new Frire(rang, graine, param);
            case "presser": return new Presser(rang, graine, param);
            case "assaisonner": return new Assaisonner(rang, graine, param);
            default: return null;
        }
    }

    /** Importance d'une étape dans la note du plat : la cuisson compte plus que la découpe. */
    public static double poids(String geste) {
        switch (cle(geste)) {
            case "saisir": case "four": case "griller": return 1.5;
            case "frire": case "mijoter": return 1.3;
            case "bouillir": return 1.2;
            case "tamiser": case "assaisonner": return 0.8;
            default: return 1.0;
        }
    }

    // ================================================================== feu à crans (commun)
    /** Bande de réglage du feu en bas de la scène : n crans cliquables. */
    public static final int FEU_Y = 168, FEU_X0 = 124, FEU_X1 = 276;

    public static int feuCranX(int i, int n) {
        return FEU_X0 + (FEU_X1 - FEU_X0) * i / (n - 1);
    }

    /** Cran visé par un clic, ou -1 si le clic n'est pas sur la bande du feu. */
    public static int feuClic(int x, int y, int n) {
        if (y < FEU_Y - 12 || y > FEU_Y + 14) return -1;
        for (int i = 0; i < n; i++) if (Math.abs(x - feuCranX(i, n)) <= 11) return i;
        return -1;
    }

    /** Gère molette / flèches / clic sur la bande pour un réglage de feu ; renvoie le nouveau cran. */
    static int reglerFeu(int feu, int n, int type, int a, int b) {
        if (type == MiniJeu.MOLETTE) return Math.max(0, Math.min(n - 1, feu + (a > 0 ? 1 : -1)));
        if (type == MiniJeu.TOUCHE && (a == MiniJeu.DROITE || a == MiniJeu.HAUT)) return Math.min(n - 1, feu + 1);
        if (type == MiniJeu.TOUCHE && (a == MiniJeu.GAUCHE || a == MiniJeu.BAS)) return Math.max(0, feu - 1);
        if (type == MiniJeu.CLIC) {
            int c = feuClic(a, b, n);
            if (c >= 0) return c;
        }
        return feu;
    }

    static boolean estReglageFeu(int type, int a, int b, int n) {
        if (type == MiniJeu.MOLETTE) return true;
        if (type == MiniJeu.TOUCHE && a != MiniJeu.ESPACE && a < MiniJeu.CHIFFRE) return true;
        return type == MiniJeu.CLIC && feuClic(a, b, n) >= 0;
    }

    // ================================================================== COUPER (référence validée)
    public static final class Couper extends MiniJeu {
        /** Le couteau doit remonter avant la coupe suivante (ms) : les clics pendant ce temps sont ignorés. */
        public static final int RECUL = 380;
        public final double periode, largeur;
        public final boolean mobile;
        public final int[] qualite = new int[5];
        public final int[] tCoupe = new int[5];

        Couper(int rang, long graine, int param) {
            super(rang, graine, param);
            periode = r(2400, 1900, 1500, 1150);
            largeur = r(0.30, 0.24, 0.18, 0.12);
            mobile = rang >= 2;
            total = 5;
            Arrays.fill(tCoupe, -1);
            majZone();
        }

        @Override public String titre() { return "COUPER"; }
        @Override public String consigne() { return "CLIQUE QUAND LA FLÈCHE EST DANS LE VERT"; }
        @Override public String commandes() { return "CLIC OU ESPACE"; }
        @Override public String compteur() { return "COUPES"; }
        @Override public int duree() { return 20000; }

        private void majZone() {
            double c = 0.5;
            if (mobile) c += (0.5 - largeur / 2 - 0.04) * StrictMath.sin(t / 5200.0 * 2 * Math.PI) * 0.9;
            zone(c, largeur);
        }

        @Override protected void tick() {
            valeur = tri(t, periode);
            majZone();
        }

        public boolean couteauLeve() {
            return fait == 0 || t - tCoupe[fait - 1] >= RECUL;
        }

        @Override protected void action(int type, int a, int b) {
            if (!appuiType(type, a) || fait >= 5 || !couteauLeve()) return;
            double s = juger(valeur, zoneLo, zoneHi);
            Retour r = retours.get(retours.size() - 1);
            qualite[fait] = r.qualite;
            tCoupe[fait] = t;
            somme += s;
            fait++;
            if (fait >= 5) fini = true;
        }
    }

    // ================================================================== ÉTALER
    public static final class Etaler extends MiniJeu {
        public static final int ATTENTE = 0, POUSSE = 1, RETOUR = 2;
        public int phase, dechirures;
        public final double vitesse;
        public final double[] etendue;   // étalement obtenu à chaque passe (0-1)
        private boolean souris, espace;

        Etaler(int rang, long graine, int param) {
            super(rang, graine, param);
            total = ri(4, 5, 5, 6);
            vitesse = r(0.42, 0.52, 0.64, 0.78);
            zone(0.73, r(0.26, 0.21, 0.17, 0.13));
            rouge = 0.9;
            etendue = new double[total];
        }

        @Override public String titre() { return "ÉTALER"; }
        @Override public String consigne() { return "MAINTIENS POUR ROULER, RELÂCHE AVANT LE ROUGE"; }
        @Override public String commandes() { return "MAINTENIR CLIC OU ESPACE"; }
        @Override public String compteur() { return "PASSES"; }
        @Override public int duree() { return 26000; }

        private boolean tenu() { return souris || espace; }

        @Override protected void tick() {
            if (phase == POUSSE) {
                valeur += vitesse * DT * (1 + 0.7 * valeur);
                if (valeur >= rouge) {
                    retour(RATE, "DÉCHIRÉE !");
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
            boolean appui = appuiType(type, a), relache = relacheType(type, a);
            if (appui) {
                if (type == CLIC) souris = true; else espace = true;
                if (phase == ATTENTE && fait < total) phase = POUSSE;
            } else if (relache) {
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

    // ================================================================== PÉTRIR
    public static final class Petrir extends MiniJeu {
        public int dir = -1, phase, tDebut;
        public double fenetre;
        public int dernierSens = -1, tDernier = -1, dernierQualite;
        private int tPause;

        Petrir(int rang, long graine, int param) {
            super(rang, graine, param);
            total = ri(8, 10, 12, 14);
            fenetre = r(1600, 1350, 1100, 900);
            zone(0.775, 0.45);
            valeur = 1;
        }

        @Override public String titre() { return "PÉTRIR"; }
        @Override public String consigne() { return "APPUIE SUR LA FLÈCHE AFFICHÉE AVANT LA FIN"; }
        @Override public String commandes() { return "Z Q S D OU FLÈCHES"; }
        @Override public String compteur() { return "PLIS"; }
        @Override public int duree() { return 32000; }

        @Override protected void tick() {
            if (phase == 0) {
                valeur = 1;
                if (t - tPause >= 260 && fait < total) {
                    int nd;
                    do { nd = rng.nextInt(4); } while (nd == dir);
                    dir = nd;
                    phase = 1;
                    tDebut = t;
                }
            } else {
                valeur = clamp(1 - (t - tDebut) / fenetre);
                if (t - tDebut >= fenetre) {
                    retour(RATE, "TROP TARD");
                    suivant(RATE);
                }
            }
        }

        @Override protected void action(int type, int a, int b) {
            if (type != TOUCHE || a > DROITE || phase != 1) return;
            if (a == dir) {
                double rr = (t - tDebut) / fenetre;
                if (rr <= 0.45) {
                    retour(PARFAIT, "PARFAIT");
                    somme += 100;
                    suivant(PARFAIT);
                } else {
                    retour(BIEN, "BIEN");
                    somme += 100 - 30 * (rr - 0.45) / 0.55;
                    suivant(BIEN);
                }
            } else {
                retour(RATE, "MAUVAIS SENS");
                suivant(RATE);
            }
        }

        private void suivant(int q) {
            dernierSens = dir;
            tDernier = t;
            dernierQualite = q;
            fait++;
            fenetre = Math.max(450, fenetre * 0.94);
            phase = 0;
            tPause = t;
            if (fait >= total) fini = true;
        }
    }

    // ================================================================== FAÇONNER
    public static final class Faconner extends MiniJeu {
        public static final int CX = 178, CY = 110, RX = 48, RY = 24;
        public final int vie;
        public final double rayon;
        public int phase, px, py, tDebut;
        public final int[] faitsX, faitsY, faitsQ;
        private int tPause;

        Faconner(int rang, long graine, int param) {
            super(rang, graine, param);
            total = ri(6, 7, 8, 10);
            vie = ri(1500, 1300, 1100, 900);
            rayon = r(9, 8, 7, 6);
            zone(0.65, 0.7);
            faitsX = new int[total];
            faitsY = new int[total];
            faitsQ = new int[total];
            tPause = -300;
        }

        @Override public String titre() { return "FAÇONNER"; }
        @Override public String consigne() { return "CLIQUE AU CENTRE DE CHAQUE POINT D'APPUI"; }
        @Override public String commandes() { return "CLIC SUR LES CIBLES"; }
        @Override public String compteur() { return "APPUIS"; }
        @Override public int duree() { return 26000; }

        @Override protected void tick() {
            if (phase == 0) {
                valeur = 1;
                if (t - tPause >= 220 && fait < total) {
                    double ang = rng.nextDouble() * Math.PI * 2, rr = Math.sqrt(rng.nextDouble()) * 0.8;
                    px = (int) Math.round(CX + StrictMath.cos(ang) * RX * rr);
                    py = (int) Math.round(CY + StrictMath.sin(ang) * RY * rr);
                    phase = 1;
                    tDebut = t;
                }
            } else {
                valeur = clamp(1 - (t - tDebut) / (double) vie);
                if (t - tDebut >= vie) {
                    retour(RATE, "TROP TARD", px, py);
                    suivant(RATE);
                }
            }
        }

        @Override protected void action(int type, int a, int b) {
            if (type != CLIC || phase != 1) return;
            double d = Math.hypot(a - px, b - py);
            if (d <= rayon * 0.4) {
                retour(PARFAIT, "PARFAIT", px, py);
                somme += 100;
                suivant(PARFAIT);
            } else if (d <= rayon) {
                retour(BIEN, "BIEN", px, py);
                somme += 95 - 25 * (d - 0.4 * rayon) / (0.6 * rayon);
                suivant(BIEN);
            } else {
                retour(RATE, "RATÉ", px, py);
                somme += d <= rayon * 2 ? 30 * (1 - (d - rayon) / rayon) : 0;
                suivant(RATE);
            }
        }

        private void suivant(int q) {
            faitsX[fait] = px;
            faitsY[fait] = py;
            faitsQ[fait] = q;
            fait++;
            phase = 0;
            tPause = t;
            if (fait >= total) fini = true;
        }
    }

    // ================================================================== FOUETTER / MÉLANGER (molette)
    public static final class Batteur extends MiniJeu {
        public final boolean fouet;
        public final double baisse, cran;
        public double compte, qualiteSeconde, volume, deborde;
        public int crans;
        private static final int GRACE = 1500;
        private double secSomme, secN;
        private int tDebord;

        Batteur(int rang, long graine, int param, boolean fouet) {
            super(rang, graine, param);
            this.fouet = fouet;
            if (fouet) {
                zone(0.72, r(0.26, 0.22, 0.18, 0.14));
                baisse = r(0.30, 0.38, 0.46, 0.55);
                cran = 0.075;
            } else {
                zone(0.30, r(0.24, 0.20, 0.16, 0.12));
                baisse = r(0.22, 0.27, 0.33, 0.40);
                cran = 0.06;
                rouge = zoneHi + 0.12;
            }
            total = 100;
        }

        @Override public String titre() { return fouet ? "FOUETTER" : "MÉLANGER"; }
        @Override public String consigne() { return fouet ? "TOURNE LA MOLETTE, GARDE LA JAUGE DANS LE VERT" : "TOURNE DOUCEMENT, TROP VITE ÇA DÉBORDE"; }
        @Override public String commandes() { return "MOLETTE OU ESPACE"; }
        @Override public String compteur() { return fouet ? "FOUETTÉ" : "MÉLANGÉ"; }
        @Override public int duree() { return 12000; }

        @Override protected void tick() {
            valeur = Math.max(0, valeur - baisse * DT);
            if (t <= GRACE) return;
            double credit;
            if (valeur >= zoneLo && valeur <= zoneHi) credit = 1;
            else {
                double e = valeur < zoneLo ? zoneLo - valeur : valeur - zoneHi;
                credit = Math.max(0, 1 - e / 0.12) * 0.5;
            }
            if (!fouet && valeur > rouge) {
                credit = 0;
                deborde += DT;
                if (t - tDebord >= 600) {
                    tDebord = t;
                    retour(RATE, "ÇA DÉBORDE !");
                }
            }
            compte += credit;
            volume += credit * DT;
            secSomme += credit;
            secN++;
            if ((t - GRACE) % 1000 == 0) {
                double q = secSomme / Math.max(1, secN);
                qualiteSeconde = q;
                if (q >= 0.9) retour(PARFAIT, "PARFAIT");
                else if (q >= 0.5) retour(BIEN, "BIEN");
                else if (fouet || valeur <= rouge) retour(RATE, valeur < zoneLo ? "PLUS VITE !" : "TROP VITE !");
                secSomme = 0;
                secN = 0;
            }
            fait = (int) Math.round(100 * compte / ((duree() - GRACE) / (double) PAS));
        }

        @Override protected void action(int type, int a, int b) {
            int n = 0;
            if (type == MOLETTE) n = Math.max(1, Math.min(3, Math.abs(a)));
            else if (type == TOUCHE && a == ESPACE) n = 1;
            if (n == 0) return;
            crans += n;
            valeur = Math.min(1, valeur + cran * n);
        }

        @Override public double note() {
            return clamp(compte / ((duree() - GRACE) / (double) PAS)) * 100;
        }
    }

    // ================================================================== PILER / SECOUER (rythme)
    public static final class Rythme extends MiniJeu {
        public final boolean piler;
        public final int periode, fenetre, avance;
        public int cible;
        public final int[] resultats;
        public int tFrappe = -1000;

        Rythme(int rang, long graine, int param, boolean piler) {
            super(rang, graine, param);
            this.piler = piler;
            total = piler ? ri(8, 10, 12, 14) : ri(8, 10, 12, 16);
            periode = piler ? ri(1100, 950, 800, 680) : ri(600, 520, 450, 380);
            fenetre = piler ? ri(120, 100, 85, 70) : ri(110, 95, 80, 65);
            avance = piler ? 900 : 2 * periode;
            resultats = new int[total];
            Arrays.fill(resultats, -1);
            zoneLo = 1 - fenetre / (double) periode;
            zoneHi = 1;
        }

        public int temps(int k) { return avance + k * periode; }

        @Override public String titre() { return piler ? "PILER" : "SECOUER"; }
        @Override public String consigne() { return piler ? "FRAPPE QUAND LE CERCLE TOUCHE LA CIBLE" : "CLIQUE SUR CHAQUE TEMPS DU MÉTRONOME"; }
        @Override public String commandes() { return "CLIC OU ESPACE"; }
        @Override public String compteur() { return piler ? "COUPS" : "TEMPS"; }
        @Override public int duree() { return avance + total * periode + 1200; }

        @Override protected void tick() {
            if (cible < total && t > temps(cible) + 2 * fenetre) {
                retour(RATE, "TROP TARD");
                resultats[cible] = RATE;
                cible++;
                fait++;
            }
            if (cible < total) {
                int avant = cible == 0 ? Math.min(0, temps(0) - periode) : temps(cible - 1);
                valeur = clamp((t - avant) / (double) (temps(cible) - avant));
            }
            if (cible >= total) fini = true;
        }

        @Override protected void action(int type, int a, int b) {
            if (!appuiType(type, a) || cible >= total) return;
            int err = Math.abs(t - temps(cible));
            double s;
            if (err > periode / 2) {
                retour(RATE, "TROP TÔT");
                s = 0;
                resultats[cible] = RATE;
            } else {
                s = jugerEcart(err, fenetre);
                resultats[cible] = retours.get(retours.size() - 1).qualite;
            }
            somme += s;
            tFrappe = t;
            cible++;
            fait++;
            if (cible >= total) fini = true;
        }
    }

    // ================================================================== TAMISER
    public static final class Tamiser extends MiniJeu {
        public final int tempo, tol;
        public int cote = -1, tDernier = -1;
        public double tamise;

        Tamiser(int rang, long graine, int param) {
            super(rang, graine, param);
            total = ri(12, 14, 16, 18);
            tempo = ri(500, 450, 400, 360);
            tol = ri(90, 75, 60, 50);
            double k = 1.25 * tempo;
            zoneLo = (tempo - tol) / k;
            zoneHi = (tempo + tol) / k;
        }

        @Override public String titre() { return "TAMISER"; }
        @Override public String consigne() { return "ALTERNE CLIC GAUCHE ET CLIC DROIT, EN RYTHME"; }
        @Override public String commandes() { return "CLIC GAUCHE / DROIT OU Q / D"; }
        @Override public String compteur() { return "SECOUSSES"; }
        @Override public int duree() { return (int) (total * tempo * 1.8) + 4000; }

        @Override protected void tick() {
            if (cote < 0) {
                valeur = 0;
                return;
            }
            valeur = clamp((t - tDernier) / (1.25 * tempo));
            if (t - tDernier > 2.2 * tempo) {
                retour(RATE, "TROP LENT");
                fait++;
                cote = -1;
                if (fait >= total) fini = true;
            }
        }

        @Override protected void action(int type, int a, int b) {
            int c;
            if (type == CLIC || (type == TOUCHE && a == GAUCHE)) c = 0;
            else if (type == CLIC_DROIT || (type == TOUCHE && a == DROITE)) c = 1;
            else return;
            if (cote < 0) {
                cote = c;
                tDernier = t;
                retour(INFO, "EN RYTHME !");
                return;
            }
            double s;
            if (c == cote) {
                retour(RATE, "ALTERNE !");
                s = 0;
            } else {
                s = jugerEcart(Math.abs((t - tDernier) - tempo), tol);
            }
            somme += s;
            tamise += s / 100;
            cote = c;
            tDernier = t;
            fait++;
            if (fait >= total) fini = true;
        }
    }

    // ================================================================== SAISIR (poêle, deux faces)
    public static final class Saisir extends MiniJeu {
        public static final int CRANS = 5;
        private static final double[] VITESSE = {0, 0.05, 0.10, 0.16, 0.24};
        private static final double[] QUALITE_FEU = {0, 0.7, 0.92, 1.0, 0.85};
        public int feu, face, tRetourne = -1000;
        public double cuisson, fumee;
        public final double facteur;
        public final double[] couleurFace = new double[2];
        private double qfSomme, qfTemps;
        private int tFumee;

        Saisir(int rang, long graine, int param) {
            super(rang, graine, param);
            total = 2;
            facteur = r(1, 1.15, 1.3, 1.45);
            zone(0.75, r(0.24, 0.20, 0.16, 0.12));
            rouge = zoneHi + 0.1;
        }

        @Override public String titre() { return "SAISIR"; }
        @Override public String consigne() { return "RÈGLE LE FEU, PUIS RETOURNE AU BON MOMENT"; }
        @Override public String commandes() { return "MOLETTE / Q D : FEU   CLIC OU ESPACE : RETOURNER"; }
        @Override public String compteur() { return "FACE"; }
        @Override public int duree() { return 32000; }

        @Override protected void tick() {
            cuisson += VITESSE[feu] * facteur * DT;
            if (feu > 0) {
                qfSomme += QUALITE_FEU[feu] * DT;
                qfTemps += DT;
            }
            fumee = feu == CRANS - 1 ? Math.min(1, fumee + DT * 0.6) : Math.max(0, fumee - DT);
            if (feu == CRANS - 1 && fumee >= 1 && t - tFumee > 2500) {
                tFumee = t;
                retour(INFO, "ÇA FUME !");
            }
            valeur = Math.min(1, cuisson);
            if (cuisson >= 1.08) {
                retour(RATE, "BRÛLÉ !");
                retourner(0);
            }
        }

        @Override protected void action(int type, int a, int b) {
            if (estReglageFeu(type, a, b, CRANS)) {
                feu = reglerFeu(feu, CRANS, type, a, b);
                return;
            }
            if (appuiType(type, a)) {
                double s = juger(cuisson, zoneLo, zoneHi);
                double f = qfTemps > 0 ? qfSomme / qfTemps : 0;
                if (f < 0.8 && cuisson > 0.3) retour(INFO, "FEU MAL RÉGLÉ");
                retourner(s * f);
            }
        }

        private void retourner(double s) {
            somme += s;
            couleurFace[face] = cuisson;
            face++;
            fait = face;
            cuisson = 0;
            qfSomme = 0;
            qfTemps = 0;
            tRetourne = t;
            if (face >= 2) fini = true;
        }
    }

    // ================================================================== BOUILLIR / MIJOTER (température)
    public static final class Chauffe extends MiniJeu {
        public static final int CRANS = 7;
        public final boolean mijoter;
        public int feu;
        public double temp = 0.12, compte;
        public final int[] alertes;
        public final int reaction;
        public final boolean[] traitees;
        public int alerte = -1;
        private final double tau;
        private final int[] froid;
        private int prochainFroid, nbAlertesTraitees;
        private double scoreAlertes, secSomme, secN;
        private static final int GRACE = 2500;

        Chauffe(int rang, long graine, int param, boolean mijoter) {
            super(rang, graine, param);
            this.mijoter = mijoter;
            if (mijoter) {
                zone(0.35, r(0.24, 0.20, 0.16, 0.12));
                tau = 2.0;
                int n = ri(2, 3, 4, 5);
                alertes = new int[n];
                int t0 = 3500, span = duree() - 2500 - t0;
                int pas = span / n;
                for (int i = 0; i < n; i++) alertes[i] = t0 + i * pas + rng.nextInt(Math.max(1, pas - 2500));
                reaction = ri(2000, 1600, 1300, 1000);
                froid = new int[0];
            } else {
                zone(0.75, r(0.24, 0.20, 0.16, 0.12));
                tau = 1.6;
                alertes = new int[0];
                reaction = 0;
                int ecart = ri(0, 5000, 3800, 3000);
                int n = ecart == 0 ? 0 : (duree() - 4000) / ecart;
                froid = new int[n];
                for (int i = 0; i < n; i++) froid[i] = 4000 + i * ecart + rng.nextInt(1600) - 800;
            }
            traitees = new boolean[alertes.length];
            rouge = zoneHi + (mijoter ? 0.25 : 0.14);
            total = 100;
        }

        @Override public String titre() { return mijoter ? "MIJOTER" : "BOUILLIR"; }
        @Override public String consigne() { return mijoter ? "FEU DOUX, ET REMUE DÈS QUE ÇA ATTACHE" : "GARDE LA TEMPÉRATURE DANS LE VERT"; }
        @Override public String commandes() { return mijoter ? "MOLETTE / Q D : FEU   CLIC OU ESPACE : REMUER" : "MOLETTE / Q D OU CLIC SUR LES CRANS"; }
        @Override public String compteur() { return mijoter ? "MIJOTÉ" : "CUISSON"; }
        @Override public int duree() { return mijoter ? 22000 : 15000; }

        @Override protected void tick() {
            temp += (feu / (double) (CRANS - 1) - temp) * DT / tau;
            if (prochainFroid < froid.length && t >= froid[prochainFroid]) {
                prochainFroid++;
                temp = Math.max(0, temp - 0.14);
                retour(INFO, "AJOUT À FROID");
            }
            valeur = clamp(temp);
            if (t > GRACE) {
                double credit;
                if (valeur >= zoneLo && valeur <= zoneHi) credit = 1;
                else {
                    double e = valeur < zoneLo ? zoneLo - valeur : valeur - zoneHi;
                    credit = Math.max(0, 1 - e / 0.12) * 0.5;
                }
                compte += credit;
                secSomme += credit;
                secN++;
                if ((t - GRACE) % 1000 == 0) {
                    double q = secSomme / Math.max(1, secN);
                    if (q >= 0.9) retour(PARFAIT, "PARFAIT");
                    else if (q >= 0.5) retour(BIEN, "BIEN");
                    else retour(RATE, valeur < zoneLo ? "PAS ASSEZ CHAUD" : "TROP CHAUD");
                    secSomme = 0;
                    secN = 0;
                }
            }
            alerte = -1;
            for (int i = 0; i < alertes.length; i++) {
                if (traitees[i]) continue;
                if (t >= alertes[i] && t < alertes[i] + reaction) alerte = i;
                else if (t >= alertes[i] + reaction) {
                    traitees[i] = true;
                    retour(RATE, "ÇA ATTACHE !");
                }
            }
            fait = (int) Math.round(100 * note() / 100);
        }

        @Override protected void action(int type, int a, int b) {
            if (estReglageFeu(type, a, b, CRANS)) {
                feu = reglerFeu(feu, CRANS, type, a, b);
                return;
            }
            if (mijoter && appuiType(type, a) && alerte >= 0) {
                double rr = (t - alertes[alerte]) / (double) reaction;
                if (rr <= 0.45) {
                    retour(PARFAIT, "REMUÉ !");
                    scoreAlertes += 100;
                } else {
                    retour(BIEN, "REMUÉ");
                    scoreAlertes += 100 - 30 * (rr - 0.45) / 0.55;
                }
                traitees[alerte] = true;
                nbAlertesTraitees++;
                alerte = -1;
            }
        }

        private double noteTemp() {
            return clamp(compte / ((duree() - GRACE) / (double) PAS)) * 100;
        }

        @Override public double note() {
            if (!mijoter) return noteTemp();
            double al = alertes.length == 0 ? 100 : scoreAlertes / alertes.length;
            return 0.6 * noteTemp() + 0.4 * al;
        }
    }

    // ================================================================== FOUR
    public static final class Four extends MiniJeu {
        public static final int CRANS = 7;
        public static final String[] DEGRES = {"OFF", "100°", "130°", "160°", "180°", "210°", "250°"};
        public final int ideal;
        public int thermostat;
        public double chaleur, cuisson;
        public final double facteur;
        private double qfSomme, qfTemps;
        public boolean sorti;

        Four(int rang, long graine, int param) {
            super(rang, graine, param);
            ideal = param >= 1 && param < CRANS ? param : 4;
            facteur = r(1, 1.15, 1.3, 1.45);
            zone(0.78, r(0.22, 0.18, 0.14, 0.10));
            rouge = zoneHi + 0.12;
            total = 100;
        }

        @Override public double note() {
            return somme;
        }

        @Override public String titre() { return "CUIRE AU FOUR"; }
        @Override public String consigne() { return "RÈGLE LA CHALEUR SUR " + DEGRES[ideal] + ", SORS LE PLAT DANS LE VERT"; }
        @Override public String commandes() { return "MOLETTE / Q D : CHALEUR   CLIC OU ESPACE : SORTIR"; }
        @Override public String compteur() { return "CUISSON"; }
        @Override public int duree() { return 40000; }

        @Override protected void tick() {
            chaleur += (thermostat / (double) (CRANS - 1) - chaleur) * DT / 1.2;
            double vit = 0.11 * StrictMath.pow(Math.max(0, chaleur), 1.2) * facteur;
            cuisson += vit * DT;
            if (vit > 0.005) {
                int ecart = Math.abs(thermostat - ideal);
                qfSomme += (ecart == 0 ? 1 : ecart == 1 ? 0.85 : ecart == 2 ? 0.65 : 0.45) * DT;
                qfTemps += DT;
            }
            valeur = Math.min(1, cuisson);
            fait = (int) Math.min(100, Math.round(cuisson * 100));
            if (cuisson >= 1.05) {
                retour(RATE, "CARBONISÉ !");
                sortir(0);
            }
        }

        @Override protected void action(int type, int a, int b) {
            if (estReglageFeu(type, a, b, CRANS)) {
                thermostat = reglerFeu(thermostat, CRANS, type, a, b);
                return;
            }
            if (appuiType(type, a) && cuisson > 0.05) {
                double s = juger(cuisson, zoneLo, zoneHi);
                double f = qfTemps > 0 ? qfSomme / qfTemps : 0.4;
                if (f < 0.8) retour(INFO, "CHALEUR MAL RÉGLÉE");
                sortir(s * f);
            }
        }

        private void sortir(double s) {
            somme = s;
            sorti = true;
            fini = true;
        }
    }

    // ================================================================== GRILLER
    public static final class Griller extends MiniJeu {
        public static final int DX = 21, DY = 12;
        public final int n;
        public final int[] x, y, faces, depart;
        public final double[] cuisson, vitesse;
        public final double[][] marque;
        private static final int[][][] SLOTS = {
                {{150, 105}, {230, 105}},
                {{130, 105}, {190, 105}, {250, 105}},
                {{150, 90}, {230, 90}, {150, 128}, {230, 128}},
                {{130, 90}, {190, 90}, {250, 90}, {160, 128}, {220, 128}}};

        Griller(int rang, long graine, int param) {
            super(rang, graine, param);
            n = ri(2, 3, 4, 5);
            x = new int[n];
            y = new int[n];
            faces = new int[n];
            depart = new int[n];
            cuisson = new double[n];
            vitesse = new double[n];
            marque = new double[n][2];
            double base = r(0.13, 0.15, 0.17, 0.19);
            for (int i = 0; i < n; i++) {
                x[i] = SLOTS[n - 2][i][0];
                y[i] = SLOTS[n - 2][i][1];
                vitesse[i] = base * (0.8 + rng.nextDouble() * 0.45);
                depart[i] = i * 900;
            }
            total = n * 2;
            zone(0.74, r(0.24, 0.20, 0.16, 0.13));
            rouge = zoneHi + 0.1;
        }

        @Override public String titre() { return "GRILLER"; }
        @Override public String consigne() { return "CLIQUE UNE PIÈCE QUAND SA JAUGE EST VERTE"; }
        @Override public String commandes() { return "CLIC SUR LA PIÈCE OU TOUCHES 1 À " + n; }
        @Override public String compteur() { return "FACES"; }
        @Override public int duree() { return 45000; }

        public boolean active(int i) { return t >= depart[i] && faces[i] < 2; }

        @Override protected void tick() {
            double max = 0;
            for (int i = 0; i < n; i++) {
                if (!active(i)) continue;
                cuisson[i] += vitesse[i] * DT;
                if (cuisson[i] >= 1.08) {
                    retour(RATE, "BRÛLÉ !", x[i], y[i]);
                    retourner(i, 0);
                    continue;
                }
                max = Math.max(max, cuisson[i]);
            }
            valeur = Math.min(1, max);
        }

        @Override protected void action(int type, int a, int b) {
            int i = -1;
            if (type == CLIC) {
                for (int k = 0; k < n; k++) if (Math.abs(a - x[k]) <= DX && Math.abs(b - y[k]) <= DY + 4) i = k;
            } else if (type == TOUCHE && a >= CHIFFRE + 1 && a <= CHIFFRE + n) i = a - CHIFFRE - 1;
            if (i < 0 || !active(i)) return;
            double s = juger(cuisson[i], zoneLo, zoneHi);
            Retour r = retours.remove(retours.size() - 1);
            retour(r.qualite, r.texte, x[i], y[i]);
            retourner(i, s);
        }

        private void retourner(int i, double s) {
            somme += s;
            marque[i][faces[i]] = cuisson[i];
            faces[i]++;
            cuisson[i] = 0;
            fait++;
            if (fait >= total) fini = true;
        }
    }

    // ================================================================== FRIRE
    public static final class Frire extends MiniJeu {
        public int phase, egoutte, tEgoutte, tSecousse = -1000;
        public double dorure, dorureFinale;
        public final double vitesse;
        public final int secousses, fenetreEgoutte = 2200;
        private double scoreA, scoreB;

        Frire(int rang, long graine, int param) {
            super(rang, graine, param);
            vitesse = r(0.11, 0.13, 0.155, 0.18);
            secousses = ri(4, 5, 6, 7);
            zone(0.72, r(0.22, 0.18, 0.15, 0.12));
            rouge = zoneHi + 0.1;
            total = 2;
        }

        @Override public String titre() { return "FRIRE"; }
        @Override public String consigne() { return "PLONGE, SORS À LA BONNE DORURE, PUIS ÉGOUTTE"; }
        @Override public String commandes() { return "CLIC OU ESPACE"; }
        @Override public String compteur() { return "ÉTAPE"; }
        @Override public int duree() { return 36000; }

        @Override protected void tick() {
            if (phase == 1) {
                dorure += vitesse * DT;
                valeur = Math.min(1, dorure);
                if (dorure >= 1.05) {
                    retour(RATE, "BRÛLÉ !");
                    sortir(0);
                }
            } else if (phase == 2) {
                valeur = egoutte / (double) secousses;
                if (t > tEgoutte + fenetreEgoutte) finir();
            }
        }

        @Override protected void action(int type, int a, int b) {
            if (!appuiType(type, a)) return;
            if (phase == 0) {
                phase = 1;
                retour(INFO, "PLOUF !");
            } else if (phase == 1) {
                sortir(juger(dorure, zoneLo, zoneHi));
            } else if (phase == 2 && t >= tEgoutte) {
                egoutte++;
                tSecousse = t;
                valeur = egoutte / (double) secousses;
                if (egoutte >= secousses) {
                    retour(PARFAIT, "BIEN ÉGOUTTÉ");
                    finir();
                }
            }
        }

        private void sortir(double s) {
            scoreA = s;
            dorureFinale = dorure;
            phase = 2;
            fait = 1;
            tEgoutte = t + 300;
            zone(0.975, 0.05);
            rouge = 2;
            valeur = 0;
        }

        private void finir() {
            scoreB = 100.0 * Math.min(egoutte, secousses) / secousses;
            if (egoutte < secousses) retour(BIEN, "ÇA DÉGOULINE");
            fait = 2;
            fini = true;
        }

        @Override public double note() {
            return 0.75 * scoreA + 0.25 * scoreB;
        }
    }

    // ================================================================== PRESSER
    public static final class Presser extends MiniJeu {
        public double jus, qualite, gicles;
        public boolean tenu;
        public final double monte;
        private boolean souris, espace;
        private int tGicle;
        private double secSomme, secN;

        Presser(int rang, long graine, int param) {
            super(rang, graine, param);
            monte = r(0.55, 0.65, 0.78, 0.92);
            zone(0.55, r(0.26, 0.22, 0.17, 0.13));
            rouge = 0.9;
            total = 100;
        }

        @Override public String titre() { return "PRESSER"; }
        @Override public String consigne() { return "MAINTIENS LA PRESSION DANS LE VERT, SANS FAIRE GICLER"; }
        @Override public String commandes() { return "MAINTENIR CLIC OU ESPACE"; }
        @Override public String compteur() { return "JUS"; }
        @Override public int duree() { return 16000; }

        @Override protected void tick() {
            tenu = souris || espace;
            valeur = clamp(valeur + (tenu ? monte : -0.7) * DT);
            double eff;
            if (valeur >= rouge) {
                eff = 0;
                if (t - tGicle >= 500) {
                    tGicle = t;
                    gicles++;
                    retour(RATE, "ÇA GICLE !");
                }
            } else if (valeur >= zoneLo && valeur <= zoneHi) eff = 1;
            else if (valeur > zoneHi) eff = 0.5;
            else eff = valeur > 0.15 ? 0.35 * valeur / zoneLo : 0;
            double d = eff * DT / 4.0;
            jus += d;
            qualite += d * (eff >= 1 ? 1 : 0.6);
            if (tenu) {
                secSomme += eff;
                secN++;
                if (secN >= 100) {
                    double q = secSomme / secN;
                    if (q >= 0.9) retour(PARFAIT, "PARFAIT");
                    else if (q >= 0.5) retour(BIEN, "BIEN");
                    secSomme = 0;
                    secN = 0;
                }
            }
            fait = (int) Math.min(100, Math.round(jus * 100));
            if (jus >= 1) {
                retour(PARFAIT, "VERRE PLEIN !");
                fini = true;
            }
        }

        @Override protected void action(int type, int a, int b) {
            if (appuiType(type, a)) {
                if (type == CLIC) souris = true; else espace = true;
            } else if (relacheType(type, a)) {
                if (type == RELACHE) souris = false; else espace = false;
            }
        }

        @Override public double note() {
            double q = jus <= 0 ? 0 : qualite / jus;
            return Math.max(0, q * Math.min(1, jus) * 100 - 8 * gicles);
        }
    }

    // ================================================================== ASSAISONNER
    public static final class Assaisonner extends MiniJeu {
        public final double vitesse, tol;
        public final double[] cibles, doses;
        public int phase, dose, tFin;
        private int tAppui;
        private boolean souris, espace;

        Assaisonner(int rang, long graine, int param) {
            super(rang, graine, param);
            total = ri(2, 2, 3, 3);
            vitesse = r(0.35, 0.42, 0.50, 0.60);
            tol = r(0.05, 0.04, 0.03, 0.022);
            cibles = new double[total];
            doses = new double[total];
            for (int i = 0; i < total; i++) cibles[i] = 0.45 + rng.nextDouble() * 0.4;
            viser();
        }

        private void viser() {
            if (dose < total) {
                trait = cibles[dose];
                zoneLo = trait - tol;
                zoneHi = trait + tol;
            }
        }

        @Override public String titre() { return "ASSAISONNER"; }
        @Override public String consigne() { return "MAINTIENS POUR VERSER, RELÂCHE PILE SUR LE TRAIT"; }
        @Override public String commandes() { return "MAINTENIR CLIC OU ESPACE"; }
        @Override public String compteur() { return "DOSES"; }
        @Override public int duree() { return 26000; }

        @Override protected void tick() {
            if (phase == 1) {
                double tenu = (t - tAppui) / 1000.0;
                valeur += vitesse * DT * (1 + 0.5 * tenu);
                if (valeur >= 1) {
                    valeur = 1;
                    retour(RATE, "TROP !");
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
}
