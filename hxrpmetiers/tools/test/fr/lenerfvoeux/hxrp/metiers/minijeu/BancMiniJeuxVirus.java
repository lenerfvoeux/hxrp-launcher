package fr.lenerfvoeux.hxrp.metiers.minijeu;

import fr.lenerfvoeux.hxrp.metiers.virus.minijeu.JeuxVirus;

import java.util.Random;

/**
 * Banc d'essai des mini-jeux du Hunter Virus, hors Minecraft : joueur parfait (≈ 100), joueur au hasard (&lt; 60),
 * joueur absent (≈ 0), à chaque rang et pour chaque mode, puis rejeu serveur identique.
 */
public final class BancMiniJeuxVirus {
    /** geste, paramètres à tester. */
    static final Object[][] JEUX = {
            {"yagen", new int[]{0}}, {"hachoir", new int[]{0}}, {"chaudron", new int[]{0, 1, 2, 3, 4, 5, 6}},
            {"alambic", new int[]{0, 1, 2}}, {"jarres", new int[]{3}}, {"balance", new int[]{0, 1}}, {"pilon", new int[]{0}},
            {"pilulier", new int[]{0}}, {"table", new int[]{0, 3}}, {"gorgees", new int[]{0, 1}}, {"injection", new int[]{0, 1}},
            {"etaler_soin", new int[]{0, 1}}, {"microscope", new int[]{0}}};

    static int erreurs;

    public static void main(String[] args) {
        System.out.printf("%-12s %-5s %-4s %8s %8s %8s   %s%n", "geste", "mode", "rang", "parfait", "hasard", "absent", "durée parfait");
        for (Object[] j : JEUX) {
            String g = (String) j[0];
            for (int param : (int[]) j[1]) {
                for (int rang = 0; rang <= 3; rang++) {
                    double[] notes = new double[3];
                    int dureeParfait = 0;
                    for (int mode = 0; mode < 3; mode++) {
                        int essais = mode == 1 ? 8 : 3;
                        for (int essai = 0; essai < essais; essai++) {
                            long graine = 7654321L * (essai + 1) + rang * 131 + param * 17 + g.hashCode();
                            MiniJeu jeu = Jeux.creer(g, rang, graine, param);
                            if (jeu == null) {
                                err(g + " : jeu introuvable");
                                return;
                            }
                            Journal jl = new Journal();
                            Random r = new Random(graine ^ 77);
                            BancMiniJeux.Bot bot = mode == 0 ? parfait(g) : mode == 1 ? BancMiniJeuxVirus::hasard : (x, y, z) -> {};
                            int garde = 0;
                            while (!jeu.fini && garde++ < 200000) {
                                bot.jouer(jeu, jl, r);
                                if (!jeu.fini) jeu.avancer();
                            }
                            double n = jeu.noteFinale();
                            if (n < 0 || n > 100 || Double.isNaN(n)) err(g + " note hors bornes " + n);
                            MiniJeu rj = jl.rejouer(g, rang, graine, param, jeu.t);
                            if (rj == null) err(g + " rejeu refusé");
                            else if (Math.abs(rj.noteFinale() - n) > 1e-9 || rj.t != jeu.t)
                                err(g + " rejeu différent : " + n + " / " + rj.noteFinale() + " t=" + jeu.t + "/" + rj.t);
                            notes[mode] += n / essais;
                            if (mode == 0) dureeParfait = Math.max(dureeParfait, jeu.t);
                        }
                    }
                    System.out.printf("%-12s %-5d %-4s %8.1f %8.1f %8.1f   %5.1f s%n", g, param, rang + "*", notes[0], notes[1], notes[2], dureeParfait / 1000.0);
                    if (notes[0] < 90) err(g + "/" + param + " rang " + rang + " : le joueur parfait n'atteint que " + notes[0]);
                    if (notes[1] > 60) err(g + "/" + param + " rang " + rang + " : cliquer au hasard rapporte " + notes[1]);
                    if (notes[2] > 5) err(g + "/" + param + " rang " + rang + " : ne rien faire rapporte " + notes[2]);
                }
            }
        }
        System.out.println(erreurs == 0 ? "OK : mini-jeux du Virus cohérents" : erreurs + " ERREUR(S)");
        if (erreurs > 0) System.exit(1);
    }

    static void err(String s) {
        erreurs++;
        System.out.println("  !! " + s);
    }

    static void hasard(MiniJeu j, Journal jl, Random r) {
        if (r.nextInt(20) != 0) return;
        int x = r.nextInt(320), y = r.nextInt(184);
        switch (r.nextInt(8)) {
            case 0: case 1: jl.jouer(j, MiniJeu.CLIC, x, y); break;
            case 2: jl.jouer(j, MiniJeu.RELACHE, x, y); break;
            case 3: jl.jouer(j, MiniJeu.CLIC_DROIT, x, y); break;
            case 4: jl.jouer(j, MiniJeu.TOUCHE, r.nextInt(5), 0); break;
            case 5: jl.jouer(j, MiniJeu.MOLETTE, r.nextBoolean() ? 1 : -1, 0); break;
            case 6: jl.jouer(j, MiniJeu.TOUCHE, MiniJeu.CHIFFRE + 1 + r.nextInt(4), 0); break;
            default: jl.jouer(j, MiniJeu.TOUCHE_RELACHE, MiniJeu.ESPACE, 0); break;
        }
    }

    static double centre(MiniJeu j) { return (j.zoneLo + j.zoneHi) / 2; }

    static BancMiniJeux.Bot rythme() {
        return (j, jl, r) -> {
            JeuxVirus.Rythme ry = (JeuxVirus.Rythme) j;
            if (ry.cible < ry.total && Math.abs(j.t - ry.temps(ry.cible)) < MiniJeu.PAS) jl.jouer(j, MiniJeu.CLIC, 0, 0);
        };
    }

    static BancMiniJeux.Bot flecheDansLeVert() {
        return (j, jl, r) -> {
            if (Math.abs(j.valeur - centre(j)) < 0.35 * (j.zoneHi - j.zoneLo) / 2 * 0.8) jl.jouer(j, MiniJeu.CLIC, 0, 0);
        };
    }

    public static BancMiniJeux.Bot parfait(String g) {
        switch (g) {
            case "yagen": case "pilon": case "pilulier": return rythme();
            case "hachoir": return flecheDansLeVert();
            case "gorgees": return flecheDansLeVert();
            case "chaudron": return (j, jl, r) -> {
                if (j.valeur < centre(j) - 0.03) jl.jouer(j, MiniJeu.CLIC, 0, 0);
            };
            case "alambic": return (j, jl, r) -> {
                JeuxVirus.Alambic a = (JeuxVirus.Alambic) j;
                if (j.valeur < centre(j) - 0.04) jl.jouer(j, MiniJeu.CLIC, 0, 0);
                else if (a.froid < (a.froidLo + a.froidHi) / 2 - 0.04) jl.jouer(j, MiniJeu.CLIC_DROIT, 0, 0);
            };
            case "jarres": return (j, jl, r) -> {
                JeuxVirus.Verser v = (JeuxVirus.Verser) j;
                if (v.phase == 0) jl.jouer(j, MiniJeu.CLIC, 0, 0);
                else if (v.phase == 1 && j.valeur >= j.trait - 0.004) jl.jouer(j, MiniJeu.RELACHE, 0, 0);
            };
            case "balance": return (j, jl, r) -> {
                JeuxVirus.Balance b = (JeuxVirus.Balance) j;
                if (j.t < 400 || (j.t / MiniJeu.PAS) % 15 != 0) return;
                int reste = b.cible - b.masse;
                for (int k = JeuxVirus.Balance.POIDS.length - 1; k >= 0; k--)
                    if (JeuxVirus.Balance.POIDS[k] <= reste) {
                        jl.jouer(j, MiniJeu.CLIC, JeuxVirus.Balance.boutonX(k), JeuxVirus.Balance.BY);
                        return;
                    }
            };
            case "table": return (j, jl, r) -> {
                JeuxVirus.Table tb = (JeuxVirus.Table) j;
                if (tb.phase == 1 && (j.t / MiniJeu.PAS) % 20 == 0) {
                    int voulu = tb.ordre[tb.place];
                    for (int i = 0; i < tb.nb; i++)
                        if (tb.godets[i] == voulu) jl.jouer(j, MiniJeu.CLIC, JeuxVirus.Table.godetX(i, tb.nb), JeuxVirus.Table.SY);
                } else if (tb.phase == 2 && Math.abs(j.valeur - centre(j)) < 0.35 * (j.zoneHi - j.zoneLo) / 2 * 0.8)
                    jl.jouer(j, MiniJeu.CLIC, 0, 0);
            };
            case "injection": return (j, jl, r) -> {
                JeuxVirus.Injection in = (JeuxVirus.Injection) j;
                if (in.phase == 0) {
                    if (Math.abs(in.aiguille - centre(j)) < 0.35 * (j.zoneHi - j.zoneLo) / 2 * 0.8) jl.jouer(j, MiniJeu.CLIC, 0, 0);
                } else if (!in.appuye && in.piston < centre(j)) jl.jouer(j, MiniJeu.CLIC, 0, 0);
                else if (in.appuye && in.piston > centre(j)) jl.jouer(j, MiniJeu.RELACHE, 0, 0);
            };
            case "etaler_soin": return (j, jl, r) -> {
                JeuxVirus.EtalerSoin e = (JeuxVirus.EtalerSoin) j;
                if (e.phase == JeuxVirus.EtalerSoin.ATTENTE && e.fait < e.total) jl.jouer(j, MiniJeu.CLIC, 0, 0);
                else if (e.phase == JeuxVirus.EtalerSoin.POUSSE && e.valeur >= centre(j) - 0.01) jl.jouer(j, MiniJeu.RELACHE, 0, 0);
            };
            case "microscope": return (j, jl, r) -> {
                JeuxVirus.Microscope m = (JeuxVirus.Microscope) j;
                if (m.etape >= 3 || (j.t / MiniJeu.PAS) % 6 != 0) return;
                if (m.zoom < m.etape + 1) jl.jouer(j, MiniJeu.MOLETTE, 1, 0);
                else if (m.zoom > m.etape + 1) jl.jouer(j, MiniJeu.MOLETTE, -1, 0);
                else if (m.mise > m.nettes[m.etape] + 0.02) jl.jouer(j, MiniJeu.CLIC, 0, 0);
                else if (m.mise < m.nettes[m.etape] - 0.02) jl.jouer(j, MiniJeu.CLIC_DROIT, 0, 0);
            };
            default: throw new IllegalArgumentException(g);
        }
    }
}
