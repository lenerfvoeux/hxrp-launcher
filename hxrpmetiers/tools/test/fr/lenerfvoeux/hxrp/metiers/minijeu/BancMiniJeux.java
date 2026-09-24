package fr.lenerfvoeux.hxrp.metiers.minijeu;

import java.util.Random;

/**
 * Banc d'essai hors Minecraft : fait jouer chaque mini-jeu par un joueur parfait, un joueur
 * au hasard et un joueur absent, à chaque rang, puis vérifie que le rejeu serveur donne
 * exactement la même note.
 *
 *   javac -d build/banc src/main/java/fr/lenerfvoeux/hxrp/metiers/minijeu/*.java tools/test/fr/lenerfvoeux/hxrp/metiers/minijeu/BancMiniJeux.java
 *   java -cp build/banc fr.lenerfvoeux.hxrp.metiers.minijeu.BancMiniJeux
 */
public final class BancMiniJeux {
    static final String[] GESTES = {"Couper", "Étaler", "Pétrir", "Façonner", "Fouetter", "Mélanger", "Piler", "Tamiser", "Saisir",
            "Bouillir", "Four", "Griller", "Frire", "Mijoter", "Presser", "Secouer", "Assaisonner"};

    public interface Bot { void jouer(MiniJeu j, Journal jl, Random r); }

    static int erreurs;

    public static void main(String[] args) {
        System.out.printf("%-12s %-4s %8s %8s %8s   %s%n", "geste", "rang", "parfait", "hasard", "absent", "durée parfait");
        for (String g : GESTES) {
            for (int rang = 0; rang <= 3; rang++) {
                double[] notes = new double[3];
                int dureeParfait = 0;
                for (int mode = 0; mode < 3; mode++) {
                    for (int essai = 0; essai < 3; essai++) {
                        long graine = 1234567L * (essai + 1) + rang * 31 + g.hashCode();
                        MiniJeu j = Jeux.creer(g, rang, graine, 4);
                        Journal jl = new Journal();
                        Random r = new Random(graine ^ 99);
                        Bot bot = mode == 0 ? parfait(g) : mode == 1 ? BancMiniJeux::hasard : (x, y, z) -> {};
                        int garde = 0;
                        while (!j.fini && garde++ < 100000) {
                            bot.jouer(j, jl, r);
                            if (!j.fini) j.avancer();
                        }
                        double n = j.noteFinale();
                        if (n < 0 || n > 100 || Double.isNaN(n)) err(g + " note hors bornes " + n);
                        MiniJeu rj = jl.rejouer(g, rang, graine, 4, j.t);
                        if (rj == null) err(g + " rejeu refusé");
                        else if (Math.abs(rj.noteFinale() - n) > 1e-9 || rj.t != j.t)
                            err(g + " rejeu différent : " + n + " / " + rj.noteFinale() + " t=" + j.t + "/" + rj.t);
                        notes[mode] += n / 3;
                        if (mode == 0) dureeParfait = Math.max(dureeParfait, j.t);
                    }
                }
                System.out.printf("%-12s %-4s %8.1f %8.1f %8.1f   %5.1f s%n", g, rang + "*", notes[0], notes[1], notes[2], dureeParfait / 1000.0);
                if (notes[0] < 90) err(g + " rang " + rang + " : le joueur parfait n'atteint que " + notes[0]);
                if (notes[2] > 5) err(g + " rang " + rang + " : ne rien faire rapporte " + notes[2]);
            }
        }
        // anti-triche : un journal truqué est refusé
        Journal faux = new Journal();
        faux.ajouter(500, MiniJeu.CLIC, 0, 0);
        faux.ajouter(400, MiniJeu.CLIC, 0, 0);
        if (faux.rejouer("Couper", 0, 1, 4, 1000) != null) err("journal aux temps décroissants accepté");
        Journal horsGrille = new Journal();
        horsGrille.ajouter(505, MiniJeu.CLIC, 0, 0);
        if (horsGrille.rejouer("Couper", 0, 1, 4, 1000) != null) err("journal hors grille accepté");
        System.out.println(erreurs == 0 ? "OK : tout est cohérent" : erreurs + " ERREUR(S)");
        if (erreurs > 0) System.exit(1);
    }

    static void err(String s) {
        erreurs++;
        System.out.println("  !! " + s);
    }

    static void hasard(MiniJeu j, Journal jl, Random r) {
        if (r.nextInt(25) != 0) return;
        int k = r.nextInt(7);
        int x = r.nextInt(320), y = r.nextInt(184);
        switch (k) {
            case 0: jl.jouer(j, MiniJeu.CLIC, x, y); break;
            case 1: jl.jouer(j, MiniJeu.RELACHE, x, y); break;
            case 2: jl.jouer(j, MiniJeu.CLIC_DROIT, x, y); break;
            case 3: jl.jouer(j, MiniJeu.TOUCHE, r.nextInt(5), 0); break;
            case 4: jl.jouer(j, MiniJeu.TOUCHE_RELACHE, MiniJeu.ESPACE, 0); break;
            case 5: jl.jouer(j, MiniJeu.MOLETTE, r.nextBoolean() ? 1 : -1, 0); break;
            default: jl.jouer(j, MiniJeu.TOUCHE, MiniJeu.CHIFFRE + 1 + r.nextInt(5), 0); break;
        }
    }

    static double centre(MiniJeu j) { return (j.zoneLo + j.zoneHi) / 2; }

    public static Bot parfait(String g) {
        switch (Jeux.cle(g)) {
            case "couper": return (j, jl, r) -> {
                if (Math.abs(j.valeur - centre(j)) < 0.35 * (j.zoneHi - j.zoneLo) / 2 * 0.8) jl.jouer(j, MiniJeu.CLIC, 0, 0);
            };
            case "etaler": return (j, jl, r) -> {
                Jeux.Etaler e = (Jeux.Etaler) j;
                if (e.phase == Jeux.Etaler.ATTENTE && e.fait < e.total) jl.jouer(j, MiniJeu.CLIC, 0, 0);
                else if (e.phase == Jeux.Etaler.POUSSE && e.valeur >= centre(j) - 0.01) jl.jouer(j, MiniJeu.RELACHE, 0, 0);
            };
            case "petrir": return (j, jl, r) -> {
                Jeux.Petrir p = (Jeux.Petrir) j;
                if (p.phase == 1) jl.jouer(j, MiniJeu.TOUCHE, p.dir, 0);
            };
            case "faconner": return (j, jl, r) -> {
                Jeux.Faconner f = (Jeux.Faconner) j;
                if (f.phase == 1) jl.jouer(j, MiniJeu.CLIC, f.px, f.py);
            };
            case "fouetter": case "melanger": return (j, jl, r) -> {
                if (j.valeur < centre(j)) jl.jouer(j, MiniJeu.MOLETTE, 1, 0);
            };
            case "piler": case "secouer": return (j, jl, r) -> {
                Jeux.Rythme ry = (Jeux.Rythme) j;
                if (ry.cible < ry.total && Math.abs(j.t - ry.temps(ry.cible)) < MiniJeu.PAS) jl.jouer(j, MiniJeu.CLIC, 0, 0);
            };
            case "tamiser": return (j, jl, r) -> {
                Jeux.Tamiser tm = (Jeux.Tamiser) j;
                if (tm.cote < 0) jl.jouer(j, MiniJeu.CLIC, 0, 0);
                else if (j.t - tm.tDernier >= tm.tempo) jl.jouer(j, tm.cote == 0 ? MiniJeu.CLIC_DROIT : MiniJeu.CLIC, 0, 0);
            };
            case "saisir": return (j, jl, r) -> {
                Jeux.Saisir s = (Jeux.Saisir) j;
                if (s.feu < 3) jl.jouer(j, MiniJeu.TOUCHE, MiniJeu.DROITE, 0);
                else if (s.cuisson >= centre(j)) jl.jouer(j, MiniJeu.TOUCHE, MiniJeu.ESPACE, 0);
            };
            case "bouillir": case "mijoter": return (j, jl, r) -> {
                Jeux.Chauffe c = (Jeux.Chauffe) j;
                int base = (int) Math.floor(centre(j) * 6);
                int voulu = c.temp < centre(j) ? Math.min(6, base + 2) : Math.max(0, base - 1);
                if (c.feu < voulu) jl.jouer(j, MiniJeu.TOUCHE, MiniJeu.DROITE, 0);
                else if (c.feu > voulu) jl.jouer(j, MiniJeu.TOUCHE, MiniJeu.GAUCHE, 0);
                if (c.alerte >= 0) jl.jouer(j, MiniJeu.TOUCHE, MiniJeu.ESPACE, 0);
            };
            case "four": return (j, jl, r) -> {
                Jeux.Four f = (Jeux.Four) j;
                if (f.thermostat < f.ideal) jl.jouer(j, MiniJeu.TOUCHE, MiniJeu.DROITE, 0);
                else if (f.cuisson >= centre(j)) jl.jouer(j, MiniJeu.CLIC, 20, 60);
            };
            case "griller": return (j, jl, r) -> {
                Jeux.Griller gr = (Jeux.Griller) j;
                for (int i = 0; i < gr.n; i++)
                    if (gr.active(i) && gr.cuisson[i] >= centre(j)) jl.jouer(j, MiniJeu.CLIC, gr.x[i], gr.y[i]);
            };
            case "frire": return (j, jl, r) -> {
                Jeux.Frire f = (Jeux.Frire) j;
                if (f.phase == 0) jl.jouer(j, MiniJeu.CLIC, 0, 0);
                else if (f.phase == 1 && f.dorure >= centre(j)) jl.jouer(j, MiniJeu.CLIC, 0, 0);
                else if (f.phase == 2 && j.t >= f.tEgoutte && (j.t / MiniJeu.PAS) % 12 == 0) jl.jouer(j, MiniJeu.CLIC, 0, 0);
            };
            case "presser": return (j, jl, r) -> {
                Jeux.Presser p = (Jeux.Presser) j;
                if (!p.tenu && j.valeur < centre(j)) jl.jouer(j, MiniJeu.CLIC, 0, 0);
                else if (p.tenu && j.valeur > centre(j)) jl.jouer(j, MiniJeu.RELACHE, 0, 0);
            };
            case "assaisonner": return (j, jl, r) -> {
                Jeux.Assaisonner a = (Jeux.Assaisonner) j;
                if (a.phase == 0) jl.jouer(j, MiniJeu.CLIC, 0, 0);
                else if (a.phase == 1 && j.valeur >= j.trait - 0.004) jl.jouer(j, MiniJeu.RELACHE, 0, 0);
            };
            default: throw new IllegalArgumentException(g);
        }
    }
}
