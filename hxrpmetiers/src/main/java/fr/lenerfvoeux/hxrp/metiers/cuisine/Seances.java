package fr.lenerfvoeux.hxrp.metiers.cuisine;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.data.FoodEntry;
import fr.lenerfvoeux.hxrp.metiers.minijeu.Jeux;
import fr.lenerfvoeux.hxrp.metiers.minijeu.Journal;
import fr.lenerfvoeux.hxrp.metiers.minijeu.MiniJeu;
import fr.lenerfvoeux.hxrp.metiers.network.MsgMiniJeu;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Séances de mini-jeu ouvertes côté serveur : c'est le serveur qui tire la graine, note l'heure
 * d'ouverture et de début, puis rejoue le journal envoyé par le client pour calculer la note.
 * Uniquement manipulé depuis le fil principal du serveur.
 */
public final class Seances {
    public static final class Seance {
        public final String geste, recette;
        public final int etape, rang, param;
        public final long graine, ouvert;
        public final BlockPos pos;
        public long debut;

        Seance(String geste, String recette, int etape, int rang, int param, long graine, BlockPos pos) {
            this.geste = geste;
            this.recette = recette;
            this.etape = etape;
            this.rang = rang;
            this.param = param;
            this.graine = graine;
            this.pos = pos;
            this.ouvert = System.currentTimeMillis();
        }
    }

    /** Résultat vérifié d'une étape. */
    public static final class Verdict {
        public final double note;
        public final boolean suspect;
        public final String raison;

        Verdict(double note, boolean suspect, String raison) {
            this.note = note;
            this.suspect = suspect;
            this.raison = raison;
        }
    }

    private static final Map<UUID, Seance> OUVERTES = new HashMap<>();
    private static final SecureRandom RNG = new SecureRandom();

    private Seances() {}

    /** Ouvre (ou remplace) la séance du joueur et prépare le message qui lance le mini-jeu chez lui. */
    public static MsgMiniJeu ouvrir(EntityPlayerMP p, BlockPos pos, FoodEntry r, int etape, String geste, int rang) {
        Seance ancienne = OUVERTES.get(p.getUniqueID());
        if (ancienne != null && ancienne.debut > 0)
            HxrpMetiers.LOG.info("Gourmet : {} rouvre un mini-jeu sans avoir rendu le précédent ({} étape {})", p.getName(), ancienne.recette, ancienne.etape + 1);
        Seance s = new Seance(geste, r.id, etape, rang, parametre(geste, r), RNG.nextLong(), pos);
        OUVERTES.put(p.getUniqueID(), s);
        return new MsgMiniJeu(geste, r.id, etape, r.steps.size(), rang, s.graine, s.param);
    }

    /** Le joueur a cliqué pour commencer : l'horloge murale démarre. */
    public static void commencer(EntityPlayerMP p) {
        Seance s = OUVERTES.get(p.getUniqueID());
        if (s != null && s.debut == 0) s.debut = System.currentTimeMillis();
    }

    public static Seance prendre(EntityPlayerMP p) {
        return OUVERTES.remove(p.getUniqueID());
    }

    public static void oublier(UUID id) {
        OUVERTES.remove(id);
    }

    /**
     * Rejoue la partie et vérifie qu'elle est cohérente avec le temps réellement écoulé.
     * Une partie simulée plus longue que le temps réel = client accéléré ; beaucoup plus courte = ralenti.
     */
    public static Verdict verifier(Seance s, Journal journal, int tFin, float noteClient) {
        MiniJeu jeu = journal.rejouer(s.geste, s.rang, s.graine, s.param, tFin);
        if (jeu == null) return new Verdict(0, true, "journal incohérent");
        double note = jeu.noteFinale();
        long debut = s.debut > 0 ? s.debut : s.ouvert;
        long ecoule = System.currentTimeMillis() - debut;
        if (jeu.t > ecoule + 2500) return new Verdict(Math.min(note, 50), true, "partie plus longue que le temps réel (" + jeu.t + " ms / " + ecoule + " ms)");
        if (ecoule > jeu.t * 1.3 + 8000) return new Verdict(Math.min(note, 75), true, "partie jouée au ralenti (" + jeu.t + " ms / " + ecoule + " ms)");
        if (Math.abs(note - noteClient) > 0.5) HxrpMetiers.LOG.info("Gourmet : note client {} / serveur {} ({})", noteClient, note, s.geste);
        return new Verdict(note, false, "");
    }

    /** Paramètre propre au geste : pour le four, la bonne température selon le plat. */
    static int parametre(String geste, FoodEntry r) {
        if (!"four".equals(Jeux.cle(geste))) return 0;
        String id = r.id;
        if (id.contains("pizza")) return 6;
        if (id.startsWith("pain") || id.equals("croissant") || id.contains("baguette")) return 5;
        if ("Desserts".equals(r.cat)) return 3;
        return 4;
    }
}
