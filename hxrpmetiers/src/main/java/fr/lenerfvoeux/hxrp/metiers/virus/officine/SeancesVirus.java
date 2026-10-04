package fr.lenerfvoeux.hxrp.metiers.virus.officine;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.minijeu.Journal;
import fr.lenerfvoeux.hxrp.metiers.minijeu.MiniJeu;
import fr.lenerfvoeux.hxrp.metiers.network.Network;
import fr.lenerfvoeux.hxrp.metiers.virus.network.MsgJeuVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.network.MsgResultatVirus;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Séances de mini-jeu du Virus ouvertes côté serveur, comme celles du Gourmet : le serveur tire la graine, note
 * l'heure d'ouverture et de début, puis rejoue le journal envoyé par le client pour calculer la note lui-même.
 * Uniquement manipulé depuis le fil principal du serveur.
 */
public final class SeancesVirus {
    /** Une étape de préparation à l'officine, l'administration d'un remède, une prise de sang, une analyse au microscope. */
    public static final int ETAPE = 0, ADMIN = 1, SANG = 2, MICRO = 3;

    public static final class Seance {
        public final int type, etape, rang, param;
        public final String cle, prep;
        public final long graine, ouvert;
        public final BlockPos pos;
        /** Patient (administration, prise de sang). */
        public final UUID patient;
        public final EnumHand main;
        public long debut;

        Seance(int type, String cle, String prep, int etape, int rang, int param, long graine, BlockPos pos, UUID patient, EnumHand main) {
            this.type = type;
            this.cle = cle;
            this.prep = prep;
            this.etape = etape;
            this.rang = rang;
            this.param = param;
            this.graine = graine;
            this.pos = pos;
            this.patient = patient;
            this.main = main == null ? EnumHand.MAIN_HAND : main;
            this.ouvert = System.currentTimeMillis();
        }
    }

    private static final Map<UUID, Seance> OUVERTES = new HashMap<>();
    private static final SecureRandom RNG = new SecureRandom();

    private SeancesVirus() {}

    /** Ouvre (ou remplace) la séance du Virus et lance le mini-jeu chez lui. */
    public static void ouvrir(EntityPlayerMP p, int type, String cle, String prep, String sujet, int etape, int total, int rang, int param,
                              BlockPos pos, UUID patient, EnumHand main) {
        Seance ancienne = OUVERTES.get(p.getUniqueID());
        if (ancienne != null && ancienne.debut > 0)
            HxrpMetiers.LOG.info("Virus : {} rouvre un mini-jeu sans avoir rendu le précédent ({})", p.getName(), ancienne.cle);
        Seance s = new Seance(type, cle, prep, etape, Math.max(0, Math.min(3, rang)), param, RNG.nextLong(), pos, patient, main);
        OUVERTES.put(p.getUniqueID(), s);
        Network.NET.sendTo(new MsgJeuVirus(type, cle, prep, sujet, etape, total, s.rang, s.graine, param), p);
    }

    public static void commencer(EntityPlayerMP p) {
        Seance s = OUVERTES.get(p.getUniqueID());
        if (s != null && s.debut == 0) s.debut = System.currentTimeMillis();
    }

    public static Seance ouverte(EntityPlayerMP p) {
        return OUVERTES.get(p.getUniqueID());
    }

    public static void oublier(UUID id) {
        OUVERTES.remove(id);
    }

    /** Fin d'un mini-jeu : rejeu, vérification du temps, puis la suite selon le type de séance. */
    public static void terminer(EntityPlayerMP p, MsgResultatVirus msg) {
        Seance s = OUVERTES.remove(p.getUniqueID());
        if (s == null) return;
        if (msg.annule && s.debut == 0) {
            if (s.type == ADMIN || s.type == SANG) Soins.annule(p, s);
            return;
        }
        double note = verifier(p, s, msg.journal, msg.tFin, msg.noteClient);
        switch (s.type) {
            case ETAPE: Officine.etapeJouee(p, s, note); break;
            case ADMIN: Soins.finAdministration(p, s, note); break;
            case SANG: Soins.finPriseDeSang(p, s, note); break;
            case MICRO: Diagnostic.finMicroscope(p, s, note); break;
            default: break;
        }
    }

    /**
     * Rejoue la partie et vérifie qu'elle est cohérente avec le temps réellement écoulé.
     * Une partie simulée plus longue que le temps réel = client accéléré ; beaucoup plus courte = ralenti.
     */
    static double verifier(EntityPlayerMP p, Seance s, Journal journal, int tFin, float noteClient) {
        MiniJeu jeu = journal.rejouer(s.cle, s.rang, s.graine, s.param, tFin);
        if (jeu == null) {
            HxrpMetiers.LOG.warn("Virus : journal incohérent de {} ({})", p.getName(), s.cle);
            return 0;
        }
        double note = jeu.noteFinale();
        long debut = s.debut > 0 ? s.debut : s.ouvert;
        long ecoule = System.currentTimeMillis() - debut;
        if (jeu.t > ecoule + 2500) {
            HxrpMetiers.LOG.warn("Virus : partie de {} plus longue que le temps réel ({} ms / {} ms, {})", p.getName(), jeu.t, ecoule, s.cle);
            return Math.min(note, 50);
        }
        if (ecoule > jeu.t * 1.3 + 8000) {
            HxrpMetiers.LOG.warn("Virus : partie de {} jouée au ralenti ({} ms / {} ms, {})", p.getName(), jeu.t, ecoule, s.cle);
            return Math.min(note, 75);
        }
        if (Math.abs(note - noteClient) > 0.5) HxrpMetiers.LOG.info("Virus : note client {} / serveur {} ({})", noteClient, note, s.cle);
        return note;
    }
}
