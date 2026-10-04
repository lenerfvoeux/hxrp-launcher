package fr.lenerfvoeux.hxrp.metiers.client;

import fr.lenerfvoeux.hxrp.metiers.ModConfig;
import fr.lenerfvoeux.hxrp.metiers.network.MsgSync;

import java.util.HashMap;
import java.util.Map;

/** Copie côté client de la nutrition du joueur local. */
public final class ClientData {
    public static float faim = 60, soif = 60;
    public static int rang, xp;
    public static long offset;      // heure serveur - heure client
    public static long lastSync;    // heure client de la dernière synchro
    public static final Map<String, Long> COOLDOWNS = new HashMap<>();

    private ClientData() {}

    public static void apply(MsgSync m) {
        faim = m.faim;
        soif = m.soif;
        rang = m.rang;
        xp = m.xp;
        lastSync = System.currentTimeMillis();
        offset = m.serverTime - lastSync;
        COOLDOWNS.clear();
        COOLDOWNS.putAll(m.cooldowns);
    }

    public static long now() { return System.currentTimeMillis() + offset; }

    /** Valeurs affichées entre deux synchros (on prolonge la baisse régulière). */
    public static double faimVue(boolean drains) {
        return drains ? Math.max(0, faim - elapsedTicks() * 60.0 / (ModConfig.dureeFaimMinutes * 1200.0)) : faim;
    }

    public static double soifVue(boolean drains) {
        return drains ? Math.max(0, soif - elapsedTicks() * 60.0 / (ModConfig.dureeSoifMinutes * 1200.0)) : soif;
    }

    private static double elapsedTicks() { return Math.min(200, (System.currentTimeMillis() - lastSync) / 50.0); }
}
