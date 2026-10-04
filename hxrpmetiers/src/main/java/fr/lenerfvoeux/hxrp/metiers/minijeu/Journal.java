package fr.lenerfvoeux.hxrp.metiers.minijeu;

import java.util.Arrays;

/**
 * Journal des entrées d'une partie, horodatées en temps simulé.
 * Le client le remplit en jouant ; le serveur le rejoue pour recalculer la note.
 */
public final class Journal {
    /** Au-delà, les entrées sont ignorées des deux côtés (paquet borné). */
    public static final int MAX = 2000;

    public int n;
    public int[] temps = new int[64];
    public byte[] types = new byte[64];
    public short[] a = new short[64], b = new short[64];

    /** Enregistre et applique une entrée au jeu, au temps simulé courant. */
    public boolean jouer(MiniJeu jeu, int type, int va, int vb) {
        if (jeu.fini || n >= MAX) return false;
        ajouter(jeu.t, type, va, vb);
        jeu.entree(type, va, vb);
        return true;
    }

    public void ajouter(int t, int type, int va, int vb) {
        if (n == temps.length) {
            int c = n * 2;
            temps = Arrays.copyOf(temps, c);
            types = Arrays.copyOf(types, c);
            a = Arrays.copyOf(a, c);
            b = Arrays.copyOf(b, c);
        }
        temps[n] = t;
        types[n] = (byte) type;
        a[n] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, va));
        b[n] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, vb));
        n++;
    }

    /**
     * Rejoue une partie à l'identique. Renvoie null si le journal est incohérent
     * (temps qui reculent, hors grille, au-delà de la durée, trop d'entrées).
     *
     * Si la partie n'est pas terminée au temps de fin annoncé, c'est un abandon :
     * ce qui n'a pas été fait compte zéro.
     *
     * @param tFin temps simulé auquel le client a terminé
     */
    public MiniJeu rejouer(String geste, int rang, long graine, int param, int tFin) {
        MiniJeu jeu = Jeux.creer(geste, rang, graine, param);
        if (jeu == null || n > MAX || tFin < 0 || tFin > jeu.duree() + MiniJeu.PAS) return null;
        int prec = 0;
        for (int i = 0; i < n; i++) {
            int ti = temps[i];
            if (ti < prec || ti % MiniJeu.PAS != 0 || ti > tFin) return null;
            prec = ti;
            while (jeu.t < ti && !jeu.fini) jeu.avancer();
            if (jeu.fini) break;
            jeu.entree(types[i], a[i], b[i]);
        }
        while (!jeu.fini && jeu.t < tFin) jeu.avancer();
        if (!jeu.fini) jeu.abandonner();
        return jeu;
    }
}
