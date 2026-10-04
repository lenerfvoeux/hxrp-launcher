package fr.lenerfvoeux.hxrp.metiers.virus.officine;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Séances de mini-jeu du Virus ouvertes côté serveur. */
public final class SeancesVirus {
    private static final Map<UUID, Object> OUVERTES = new HashMap<>();

    private SeancesVirus() {}

    public static void oublier(UUID id) {
        OUVERTES.remove(id);
    }
}
