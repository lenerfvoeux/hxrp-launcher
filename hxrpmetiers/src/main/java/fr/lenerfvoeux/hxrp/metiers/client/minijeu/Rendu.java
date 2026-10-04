package fr.lenerfvoeux.hxrp.metiers.client.minijeu;

import fr.lenerfvoeux.hxrp.metiers.minijeu.Jeux;
import fr.lenerfvoeux.hxrp.metiers.minijeu.MiniJeu;

/**
 * Compose une image complète du mini-jeu sur la toile : la scène, la jauge en laiton commune,
 * le HUD (temps, rang, compteur), les retours immédiats, et les plaques d'intro et de résultat.
 * Fond transparent : la scène se pose sur le monde.
 */
public final class Rendu {
    public static final int INTRO = 0, JEU = 1, FIN = 2;
    public static final int GX = 10, GY = 28, GL = 132;

    private Rendu() {}

    public static void image(Toile t, MiniJeu j, Contexte c, double tms, int etat, int mx, int my, double note) {
        t.vider();
        Scenes.dessiner(t, j, c, tms, mx, my, etat);
        boolean dansVert = j.valeur >= j.zoneLo && j.valeur <= j.zoneHi;
        Dessin.jauge(t, GX, GY, GL, j.valeur, j.zoneLo, j.zoneHi, j.rouge, j.trait, dansVert && ((int) (tms / 120)) % 2 == 0 && etat == JEU);
        hud(t, j, c, etat);
        if (etat == JEU || etat == FIN) retours(t, j, tms);
        if (etat == INTRO) intro(t, j, c);
        if (etat == FIN) fin(t, j, note);
    }

    private static void hud(Toile t, MiniJeu j, Contexte c, int etat) {
        // bandeau titre en haut à gauche
        String titre = c.recette.isEmpty() ? j.titre() : c.recette;
        Police.texte(t, titre, 46, 4, Dessin.OR_TEXTE, Dessin.CONTOUR, 1);
        Police.texte(t, "ÉTAPE " + c.etape + "/" + c.etapes + " · " + j.titre(), 46, 15, Dessin.CREME, Dessin.CONTOUR, 1);
        // temps, rang et compteur en haut à droite
        int xr = Jeux.W - 5;
        String tps = String.format(java.util.Locale.ROOT, "%.1f S", etat == Rendu.INTRO ? j.duree() / 1000.0 : j.reste());
        Police.droite(t, tps, xr, 4, j.reste() < 3 && etat == JEU ? Dessin.ROUGE_TEXTE : Dessin.BLANC, Dessin.CONTOUR, 1);
        Dessin.horloge(t, xr - Police.largeur(tps, 1) - 11, 5);
        for (int i = 0; i < 3; i++) Dessin.etoile(t, xr - 27 + i * 9, 16, i < j.rang);
        String cpt = j.compteur() + " " + (j.total == 100 ? j.fait + " %" : j.fait + "/" + j.total);
        Police.droite(t, cpt, xr, 26, Dessin.CREME, Dessin.CONTOUR, 1);
    }

    private static void retours(Toile t, MiniJeu j, double tms) {
        int[] ancre = Scenes.ancreRetour(j);
        for (MiniJeu.Retour r : j.retours) {
            double age = (tms - r.t) / 850.0;
            if (age < 0 || age > 1) continue;
            boolean place = r.x >= 0;
            int x = place ? r.x : ancre[0], y = place ? r.y - 36 : ancre[1];
            Dessin.retour(t, r.texte, r.qualite, x, y, age, !place);
        }
    }

    private static void intro(Toile t, MiniJeu j, Contexte c) {
        int w = Math.max(200, Police.largeur(j.consigne(), 1) + 24);
        Dessin.plaque(t, 176, 58, w, 62);
        Police.centre(t, j.titre(), 176, 63, Dessin.OR_TEXTE, Dessin.CONTOUR, 2);
        Police.centre(t, j.consigne(), 176, 86, Dessin.BLANC, Dessin.CONTOUR, 1);
        Police.centre(t, j.commandes(), 176, 97, Dessin.CREME, Dessin.CONTOUR, 1);
        Police.centre(t, "CLIC POUR COMMENCER", 176, 107, Dessin.VERT_TEXTE, Dessin.CONTOUR, 1);
    }

    private static void fin(Toile t, MiniJeu j, double note) {
        int n = (int) Math.round(note);
        String q = n >= 98 ? "EXCEPTIONNEL !" : n >= 90 ? "TRÈS BIEN !" : n >= 80 ? "BIEN" : n >= 60 ? "PEUT MIEUX FAIRE" : "RATÉ…";
        int col = n >= 90 ? Dessin.OR_TEXTE : n >= 80 ? Dessin.VERT_TEXTE : n >= 60 ? Dessin.CREME : Dessin.ROUGE_TEXTE;
        Dessin.plaque(t, 176, 56, 180, 50);
        Police.centre(t, j.abandon ? "ÉTAPE ABANDONNÉE" : "ÉTAPE TERMINÉE", 176, 61, Dessin.CREME, Dessin.CONTOUR, 1);
        Police.centre(t, n + " %", 176, 72, col, Dessin.CONTOUR, 2);
        Police.centre(t, q, 176, 93, col, Dessin.CONTOUR, 1);
    }
}
